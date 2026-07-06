package com.prueba.lox.batch;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ReaderTest {

    private Reader reader;

    @Before
    public void setUp() {
        reader = new Reader();
    }

    @Test
    public void read_shouldReturnListOnFirstCallAndNullOnlyAfterTotal() throws Exception {
        // 1. Primera llamada: Debe devolver el primer bloque de 100
        List<Map<String, Object>> firstResult = reader.read();

        assertNotNull("La primera lectura no debe ser nula", firstResult);
        assertEquals("El primer bloque debe tener 100 contratos", 100, firstResult.size());

        // CORRECCIÓN: El ID ahora sigue el formato "CONT-1" (según el nuevo Reader)
        assertEquals("El ID del primer contrato debe ser CONT-1", "CONT-1", firstResult.get(0).get("CONTRATO_ID"));

        // 2. Verificamos la progresión (Segunda llamada)
        List<Map<String, Object>> secondResult = reader.read();
        assertNotNull("La segunda lectura debe traer el siguiente bloque", secondResult);
        assertEquals("El ID del primer contrato del segundo bloque debe ser CONT-101", "CONT-101", secondResult.get(0).get("CONTRATO_ID"));

        // 3. Simulamos que llegamos al final (opcional, para probar el null)
        // Para probar el null rápidamente, podrías usar Reflection para mover el índiceActual al final
        ReflectionTestUtils.setField(reader, "indiceActual", 1000);
        List<Map<String, Object>> finalResult = reader.read();
        assertNull("Después de 1000 registros, debe devolver null", finalResult);
    }
}