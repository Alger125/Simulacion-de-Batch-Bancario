package com.prueba.lox.lib.r174.Variables;

public enum VariablesSQL {
    SELECT_VERIFICACION_DATOS("sql.select.verificacion_de_datos"),
    SELECT_DETALLE_SALDOS("sql.select.detalle_saldos_cuenta");

    private final String description; // Renombrado para coincidir con la Impl

    VariablesSQL(String description) {
        this.description = description;
    }

    // Ahora este método sí lo encontrará la Impl
    public String getDescription() {
        return description;
    }
}