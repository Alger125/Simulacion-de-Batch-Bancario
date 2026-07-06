package com.prueba.lox.batch.model_1;

public class MovimientoDTO {
    private String nombre;
    private String numeroCuenta;
    private Double monto;
    private String estatus; // <--- 1. Agregamos la variable

    public MovimientoDTO() {}

    // Getters y Setters previos...
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    // --- 2. AGREGAR ESTOS MÉTODOS (Crucial para que la Impl no truene) ---
    public String getEstatus() {
        return estatus;
    }

    public void setEstatus(String estatus) {
        this.estatus = estatus;
    }

    // 3. Recomendado para ver el resultado en el archivo .txt
    @Override
    public String toString() {
        return nombre + "," + numeroCuenta + "," + monto + "," + estatus;
    }
}