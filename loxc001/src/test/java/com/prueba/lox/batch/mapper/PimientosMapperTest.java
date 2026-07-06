package com.prueba.lox.batch.mapper;

import com.prueba.lox.batch.model_1.MovimientoDTO;
import org.junit.Test;
import java.sql.ResultSet;

// Importaciones explícitas para evitar conflictos
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PimientosMapperTest {

    @Test
    public void mapRow_whenResultSetHasData_shouldMapCorrectly() throws Exception {
        // 1. Preparación (Arrange)
        PimientosMapper mapper = new PimientosMapper();
        ResultSet rs = mock(ResultSet.class);

        // Corregimos el problema del 'symbol' asegurando que Mockito reconozca el método
        when(rs.getString("NOMBRE")).thenReturn("Pedro");
        when(rs.getString("NUMERO_CUENTA")).thenReturn("123456");

        // Para tipos primitivos como double, a veces Mockito prefiere el valor exacto
        when(rs.getDouble("MONTO")).thenReturn(5000.0);

        // 2. Ejecución (Act)
        MovimientoDTO result = mapper.mapRow(rs, 1);

        // 3. Verificación (Assert)
        assertNotNull(result);
        assertEquals("Pedro", result.getNombre());
        assertEquals("123456", result.getNumeroCuenta());

        // Para assertEquals con doubles, se recomienda usar un delta (margen de error)
        // o comparar Objetos Double.
        assertEquals(Double.valueOf(5000.0), result.getMonto());
    }
}