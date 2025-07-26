/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.techsolutions.task;

// Librerias estandar de Java
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Librerias de Jackson para JSON
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * @author Luis
 */


/*
 * Programa principal:
 * 1) Consume el API generadora (GET)
 * 2) Procesa recursivamente el JSON para obtener las metricas pedidas
 * 3) Envia los resultados a la API evaluadora (POST) junto con el payload original
 *
 * Todas las variables y comentarios estan en espanol y sin acentos.
 */
public class Main {

    // Endpoints proporcionados en la tarea
    private static final String API_GENERADORA = "https://58o1y6qyic.execute-api.us-east-1.amazonaws.com/default/taskReport";
    private static final String API_EVALUADORA = "https://t199qr74fg.execute-api.us-east-1.amazonaws.com/default/taskReportVerification";

    // Datos del estudiante (reemplaza por los tuyos)
    private static final String NOMBRE  = "Luis Alfredo Tejeda Hernandez";
    private static final String CARNET  = "6590-22-20215";
    private static final String SECCION = "Seccion: 5";

    // Jackson
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try {
            // 1) Consumir API generadora
            String payload = consumirApiGeneradora();

            // 2) Procesar recursivamente
            ResultadoBusqueda resultado = procesarPayload(payload);

            // 3) Construir y enviar POST a API evaluadora
            String respuestaEvaluadora = enviarResultados(resultado, payload);

            // 4) Mostrar por consola todo para verificar
            System.out.println("==== METRICAS CALCULADAS ====");
            System.out.println("totalProcesos: " + resultado.totalProcesos);
            System.out.println("procesosCompletos: " + resultado.procesosCompletos);
            System.out.println("procesosPendientes: " + resultado.procesosPendientes);
            System.out.println("recursosTipoHerramienta: " + resultado.recursosTipoHerramienta);
            System.out.println("eficienciaPromedio: " + resultado.getEficienciaPromedio());
            System.out.println("procesoMasAntiguo.id: " + resultado.procesoMasAntiguoId);
            System.out.println("procesoMasAntiguo.nombre: " + resultado.procesoMasAntiguoNombre);
            System.out.println("procesoMasAntiguo.fechaInicio: " + resultado.procesoMasAntiguoFechaInicio);

            System.out.println("\n==== RESPUESTA API EVALUADORA ====");
            System.out.println(respuestaEvaluadora);

        } catch (Exception e) {
            System.err.println("Error ejecutando el programa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Paso 1: Consumir el API generadora
    // -------------------------------------------------------------------------
    private static String consumirApiGeneradora() throws IOException, InterruptedException {
        // Creamos el cliente HTTP
        HttpClient client = HttpClient.newHttpClient();

        // Construimos el request GET
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_GENERADORA))
                .GET()
                .build();

        // Ejecutamos y obtenemos el body como string
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Validamos codigo 200
        if (response.statusCode() != 200) {
            throw new RuntimeException("La API generadora respondio con codigo: " + response.statusCode());
        }

        String body = response.body();
        if (body == null || body.isEmpty()) {
            throw new RuntimeException("La API generadora devolvio un body vacio");
        }

        return body;
    }

    // -------------------------------------------------------------------------
    // Paso 2: Procesar recursivamente el JSON
    // -------------------------------------------------------------------------
    private static ResultadoBusqueda procesarPayload(String payload) throws IOException {
        // Parseamos a arbol JsonNode
        JsonNode root = MAPPER.readTree(payload);

        ResultadoBusqueda res = new ResultadoBusqueda();

        // Llamamos al recorrido recursivo generico que camina por todo el arbol
        recorrerNodo(root, res);

        return res;
    }

    /**
     * Recorre cualquier nodo Json (objeto, arreglo, valor)
     * y actualiza las metricas en ResultadoBusqueda.
     *
     * Este metodo es deliberadamente generico y defensivo, para soportar
     * estructuras jerarquicas que puedan cambiar nombre de campos.
     */
    private static void recorrerNodo(JsonNode node, ResultadoBusqueda res) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            // Procesar como objeto
            procesarObjeto(node, res);

            // Por cada campo del objeto, recorrer recursivamente
            node.fields().forEachRemaining(entry -> {
                JsonNode hijo = entry.getValue();
                recorrerNodo(hijo, res);
            });

        } else if (node.isArray()) {
            // Si es arreglo, recorrer cada elemento
            for (JsonNode elemento : node) {
                recorrerNodo(elemento, res);
            }

        } else {
            // Es un valor primitivo (string, numero, boolean, etc).
            // No hacemos nada aqui directamente, el conteo real ocurre en procesarObjeto.
        }
    }

    /**
     * Intenta interpretar que este objeto representa un "proceso" si contiene
     * ciertos campos tipicos: id, nombre, estado, fechaInicio, eficiencia, etc.
     * Aun asi, no dependemos de que existan todos, contamos flexible.
     */
    private static void procesarObjeto(JsonNode obj, ResultadoBusqueda res) {
        // Detectamos si parece un proceso. Criterios flexibles:
        // - Tiene un "id" y/o "nombre" y/o "estado"
        // - o contiene "hijos" o "subprocesos"
        boolean pareceProceso = false;

        if (obj.has("id") || obj.has("nombre") || obj.has("estado")
                || obj.has("hijos") || obj.has("subprocesos") || obj.has("children")) {
            pareceProceso = true;
        }

        if (pareceProceso) {
            res.totalProcesos++;

            // estado: completo vs pendiente
            String estado = text(obj, "estado");
            if (estado != null) {
                String e = estado.toLowerCase();
                if (e.contains("completo") || e.contains("finalizado") || e.equals("done")) {
                    res.procesosCompletos++;
                } else if (e.contains("pendiente") || e.contains("incompleto") || e.contains("pending")) {
                    res.procesosPendientes++;
                }
            }

            // eficiencia (si existe y es numero)
            if (obj.has("eficiencia") && obj.get("eficiencia").isNumber()) {
                res.sumaEficiencia += obj.get("eficiencia").asDouble();
                res.contadorEficiencia++;
            }

            // fechaInicio: para detectar el proceso mas antiguo
            String fechaInicio = text(obj, "fechaInicio");
            if (fechaInicio != null) {
                Long epoch = UtilFecha.parseToEpochMillis(fechaInicio);
                if (epoch != null && epoch < res.procesoMasAntiguoEpochMs) {
                    res.procesoMasAntiguoEpochMs = epoch;
                    res.procesoMasAntiguoFechaInicio = fechaInicio;
                    res.procesoMasAntiguoId = text(obj, "id");
                    res.procesoMasAntiguoNombre = text(obj, "nombre");
                }
            }

            // recursos: buscamos arrays o campos que contengan recursos con tipo == "herramienta"
            // Caso 1: campo "recursos"
            contarRecursosHerramienta(obj.get("recursos"), res);

            // Caso 2: a veces los recursos estan con otros nombres ("resources", etc)
            contarRecursosHerramienta(obj.get("resources"), res);
        }

        // Adicional: si el objeto (que puede o no ser un proceso) tiene algun campo
        // cuyo nombre sea "tipo" y su valor sea "herramienta", tambien lo contamos
        // (esto cubre estructuras diferentes).
        if (obj.has("tipo") && "herramienta".equalsIgnoreCase(text(obj, "tipo"))) {
            res.recursosTipoHerramienta++;
        }
    }

    private static void contarRecursosHerramienta(JsonNode recursosNode, ResultadoBusqueda res) {
        if (recursosNode == null || recursosNode.isNull()) return;

        if (recursosNode.isArray()) {
            for (JsonNode recurso : recursosNode) {
                if (recurso != null && recurso.isObject()) {
                    String tipo = text(recurso, "tipo");
                    if (tipo != null && tipo.equalsIgnoreCase("herramienta")) {
                        res.recursosTipoHerramienta++;
                    }
                }
            }
        } else if (recursosNode.isObject()) {
            // por si viniera como objeto
            String tipo = text(recursosNode, "tipo");
            if (tipo != null && tipo.equalsIgnoreCase("herramienta")) {
                res.recursosTipoHerramienta++;
            }
        }
    }

    private static String text(JsonNode obj, String field) {
        if (obj == null || field == null) return null;
        JsonNode v = obj.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isTextual()) return v.asText();
        return v.toString();
    }

    // -------------------------------------------------------------------------
    // Paso 3: Enviar resultados a la API evaluadora
    // -------------------------------------------------------------------------
    private static String enviarResultados(ResultadoBusqueda r, String payloadOriginal)
            throws IOException, InterruptedException {

        // Construimos el JSON exactamente como lo piden
        ObjectNode root = MAPPER.createObjectNode();
        root.put("nombre", NOMBRE);
        root.put("carnet", CARNET);
        root.put("seccion", SECCION);

        ObjectNode resultadoBusqueda = MAPPER.createObjectNode();
        resultadoBusqueda.put("totalProcesos", r.totalProcesos);
        resultadoBusqueda.put("procesosCompletos", r.procesosCompletos);
        resultadoBusqueda.put("procesosPendientes", r.procesosPendientes);
        resultadoBusqueda.put("recursosTipoHerramienta", r.recursosTipoHerramienta);
        resultadoBusqueda.put("eficienciaPromedio", r.getEficienciaPromedio());

        ObjectNode procesoMasAntiguo = MAPPER.createObjectNode();
        procesoMasAntiguo.put("id", r.procesoMasAntiguoId == null ? "" : r.procesoMasAntiguoId);
        procesoMasAntiguo.put("nombre", r.procesoMasAntiguoNombre == null ? "" : r.procesoMasAntiguoNombre);
        procesoMasAntiguo.put("fechaInicio", r.procesoMasAntiguoFechaInicio == null ? "" : r.procesoMasAntiguoFechaInicio);
        resultadoBusqueda.set("procesoMasAntiguo", procesoMasAntiguo);

        // payload: lo que devolvio el API generadora (tal y como lo recibimos)
        // Aqui lo insertamos como un nodo JSON real, no como string.
        JsonNode payloadJson = MAPPER.readTree(payloadOriginal);
        root.set("resultadoBusqueda", resultadoBusqueda);
        root.set("payload", payloadJson); // el enunciado lo pone dentro de resultadoBusqueda, pero la imagen lo muestra fuera.
                                         // Si tu docente exige EXACTAMENTE dentro de "resultadoBusqueda", mueve esta linea:

        // ---> resultadoBusqueda.set("payload", payloadJson);
        // y elimina root.set("payload"...).
        // Revisa la especificacion final que te pidieron. Aqui dejo ambas opciones explicadas.

        // Si estrictamente debe ir dentro de resultadoBusqueda, hazlo asi:
        // resultadoBusqueda.set("payload", payloadJson);
        // root.set("resultadoBusqueda", resultadoBusqueda);

        // Construimos el request POST
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_EVALUADORA))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return "codigo: " + response.statusCode() + "\nbody:\n" + response.body();
    }
}
