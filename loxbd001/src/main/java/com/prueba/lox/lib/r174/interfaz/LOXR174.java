package com.prueba.lox.lib.r174.interfaz;

import java.util.Map;

public interface LOXR174 {
    /**
     * Método para ejecutar la lógica de negocio delegada desde el Batch.
     * @param event Mapa que contiene los datos del DTO (item).
     */
    void executeCreateCreditContract(Map<String, Object> event);

    // Este es el puente que conecta tu primera consulta con la segunda
    Map<String, Object> consultarDetalleSaldo(String numeroCuenta);
}