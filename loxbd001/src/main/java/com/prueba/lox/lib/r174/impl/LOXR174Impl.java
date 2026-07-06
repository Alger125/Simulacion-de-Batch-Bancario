package com.prueba.lox.lib.r174.impl;


import com.prueba.lox.batch.model_1.MovimientoDTO;
import com.prueba.lox.lib.r174.Variables.VariablesSQL;

import java.util.Map;
import java.util.HashMap;

public class LOXR174Impl extends LOXR174Abstract {

    private static final String EVENT_STATUS_PROCESS = "PROCESS";
    private static final double MONTO_MINIMO_APTO = 10000.0;
    private static final double SALDO_MINIMO_REQUERIDO = 5000.0;

    @Override
    public void executeCreateCreditContract(Map<String, Object> event) {
        // 1. Cambia "status" por "STATUS" para que coincida con Utility
        if (event == null || !EVENT_STATUS_PROCESS.equals(event.get("STATUS"))) {
            return;
        }

        // 2. Extraemos el item (aquí sí está bien "item" porque así lo pusiste en Utility)
        MovimientoDTO item = (MovimientoDTO) event.get("item");
        if (item == null) {
            return;
        }

        processContractLogic(item);
    }

    private void processContractLogic(MovimientoDTO item) {
        HashMap<String, Object> infoSaldo = this.consultarDetalleSaldo(item.getNumeroCuenta());
        double saldoDisponible = extractSaldo(infoSaldo);

    }

    @Override
    @SuppressWarnings("unchecked")
    public HashMap<String, Object> consultarDetalleSaldo(String numeroCuenta) {
        try {
            // Asegúrate que tu Enum tenga el método getDescription() y no getKey()
            String queryKey = VariablesSQL.SELECT_DETALLE_SALDOS.getDescription();


            Map<String, Object> resultado = jdbcTemplate.queryForMap(this.sqlDetalleSaldos, new Object[]{numeroCuenta});

            return (resultado != null) ? new HashMap<>(resultado) : new HashMap<>();
        } catch (Exception e) {

            return new HashMap<>();
        }
    }

    protected  double extractSaldo(Map<String, Object> infoSaldo) {

        // Caso 1: mapa nulo o sin la clave esperada
        if (infoSaldo == null || !infoSaldo.containsKey("SALDO_DISPONIBLE")) {
            return 0.0;
        }

        // Caso 2: obtenemos el valor del saldo
        Object saldo = infoSaldo.get("SALDO_DISPONIBLE");

        // Caso 3: validamos que sea numérico
        if (saldo instanceof Number) {
            return ((Number) saldo).doubleValue();
        }

        // Caso 4: cualquier otro tipo no válido
        return 0.0;
    }
}