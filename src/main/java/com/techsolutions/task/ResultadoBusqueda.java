/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.techsolutions.task;

/**
 *
 * @author Luis
 */

/*
 * Clase simple para acumular las metricas solicitadas.
 * Todas las variables estan en espanol y sin acentos.
 */

public class ResultadoBusqueda {

    // cantidad total de procesos encontrados
    public long totalProcesos = 0;

    // procesos con estado "COMPLETADO" (o equivalente) vs pendientes
    public long procesosCompletos = 0;
    public long procesosPendientes = 0;

    // cantidad de recursos cuyo tipo sea "herramienta"
    public long recursosTipoHerramienta = 0;

    // para calcular el promedio de eficiencia
    public double sumaEficiencia = 0.0;
    public long contadorEficiencia = 0;

    // proceso mas antiguo (guardamos su id, nombre y fechaInicio)
    public String procesoMasAntiguoId = "";
    public String procesoMasAntiguoNombre = "";
    public String procesoMasAntiguoFechaInicio = null; // mantenemos el string original
    public long procesoMasAntiguoEpochMs = Long.MAX_VALUE; // para comparar

    // retorna el promedio si hay valores, de lo contrario 0.0
    public double getEficienciaPromedio() {
        if (contadorEficiencia == 0) return 0.0;
        return sumaEficiencia / contadorEficiencia;
    }
}

