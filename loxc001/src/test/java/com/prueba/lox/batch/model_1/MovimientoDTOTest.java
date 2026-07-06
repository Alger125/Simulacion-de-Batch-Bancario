package com.prueba.lox.batch.model_1;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests unitarios para MovimientoDTO
 * Cobertura de getters, setters y toString
 */
public class MovimientoDTOTest {

    @Test
    public void shouldCreateEmptyObject() {
        MovimientoDTO dto = new MovimientoDTO();

        assertNotNull(dto);
    }

    @Test
    public void shouldSetAndGetNombre() {
        MovimientoDTO dto = new MovimientoDTO();

        dto.setNombre("Jonathan");

        assertEquals("Jonathan", dto.getNombre());
    }

    @Test
    public void shouldSetAndGetNumeroCuenta() {
        MovimientoDTO dto = new MovimientoDTO();

        dto.setNumeroCuenta("123456789");

        assertEquals("123456789", dto.getNumeroCuenta());
    }

    @Test
    public void shouldSetAndGetMonto() {
        MovimientoDTO dto = new MovimientoDTO();

        dto.setMonto(2500.50);

        assertEquals(Double.valueOf(2500.50), dto.getMonto());
    }

    @Test
    public void shouldSetAndGetEstatus() {
        MovimientoDTO dto = new MovimientoDTO();

        dto.setEstatus("PROCESS");

        assertEquals("PROCESS", dto.getEstatus());
    }

    @Test
    public void toString_shouldReturnExpectedFormat() {
        MovimientoDTO dto = new MovimientoDTO();

        dto.setNombre("Juan");
        dto.setNumeroCuenta("999999");
        dto.setMonto(10000.0);
        dto.setEstatus("APROBADO");

        String result = dto.toString();

        assertEquals("Juan,999999,10000.0,APROBADO", result);
    }
}
