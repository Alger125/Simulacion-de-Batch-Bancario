package com.prueba.lox.batch.Utility;

import com.prueba.lox.batch.model_1.MovimientoDTO;
import com.prueba.lox.lib.r174.interfaz.LOXR174;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase de soporte que centraliza herramientas comunes para el Batch.
 */
public class Utility {

    private String odate;
    private LOXR174 loxR174;

    // --- SECCIÓN 1: MÉTODOS DE ACCESO (ENCAPSULAMIENTO) ---

    public String getOdate() {
        if (this.odate == null || this.odate.trim().isEmpty()) {
            this.odate = new SimpleDateFormat("yyyyMMdd").format(new Date());
        }
        return odate;
    }

    public void setOdate(String odate) { this.odate = odate; }

    public LOXR174 getLoxR174() { return loxR174; }
    public void setLoxR174(LOXR174 loxR174) { this.loxR174 = loxR174; }

    // --- SECCIÓN 2: LÓGICA DE FORMATEO DE FECHA ---

    /**
     * Transforma la fecha de YYYYMMDD a YYYY/MM/DD para el reporte.
     */
    private String getFechaFormateadaConDiagonales() {
        String fechaOriginal = this.getOdate(); // Ej: 20260217

        if (fechaOriginal != null && fechaOriginal.length() == 8) {
            return fechaOriginal.substring(0, 4) + "/" +
                    fechaOriginal.substring(4, 6) + "/" +
                    fechaOriginal.substring(6, 8);
        }
        return fechaOriginal;
    }

    // --- SECCIÓN 3: FORMATEO PARA ARCHIVO DE SALIDA (MÉTODOS SEPARADOS) ---

    /**
     * SOBRECARGA 1: Formatea un objeto individual (DTO).
     */
    public String complementDataContracts(MovimientoDTO item) {
        if (item == null) return "";

        // Usamos el nuevo método de fecha con diagonales
        String fechaSalida = this.getFechaFormateadaConDiagonales();

        return String.format("NOMBRE: %-20s | CUENTA: %-15s | MONTO: %10.2f | FECHA: %s",
                item.getNombre(),
                item.getNumeroCuenta(),
                item.getMonto(),
                fechaSalida);
    }

    /**
     * SOBRECARGA 2: Formateo para listas de contratos.
     */
    public String complementDataContracts(List<Map<String, Object>> listcontract) {
        if (listcontract == null || listcontract.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        String fechaSalida = this.getFechaFormateadaConDiagonales();

        for (Map<String, Object> contract : listcontract) {
            sb.append("CONTRATO: ").append(contract.getOrDefault("CONTRATO_ID", "N/A"))
                    .append(" | FECHA: ").append(fechaSalida)
                    .append("\n");
        }
        return sb.toString();
    }

    // --- SECCIÓN 4: GESTIÓN DE EVENTOS (COMUNICACIÓN CON LIBRERÍA) ---

    public Map<String, Object> getMapEvent(String status, String message) {
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("STATUS", status);
        mapEvent.put("MESSAGE", message);
        mapEvent.put("ODATE", this.getOdate()); // Aquí se queda sin diagonales para la DB
        return mapEvent;
    }

    public Map<String, Object> getMapEvent(String status, Object item) {
        Map<String, Object> mapEvent = new HashMap<>();
        mapEvent.put("STATUS", status);
        mapEvent.put("item", item);
        mapEvent.put("ODATE", this.getOdate());
        return mapEvent;
    }

    // --- SECCIÓN 5: UTILIDADES VARIAS ---

    public String obtenerFormatoSalida(Object data) {
        return (data != null) ? data.toString() : "";
    }
}