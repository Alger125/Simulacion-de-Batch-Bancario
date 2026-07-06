package com.prueba.lox.batch;

import com.prueba.lox.batch.model_1.MovimientoDTO;
import com.prueba.lox.lib.r174.interfaz.LOXR174;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

public class ProcessTest {

    @InjectMocks
    private Process process;

    @Mock
    private LOXR174 loxR174;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Inicializamos el contador en 0
        ReflectionTestUtils.setField(process, "totalProcesados", 0);
    }

    @Test
    public void process_shouldInvokeLibraryForSingleItem() throws Exception {
        // 1. Arrange: Preparamos UN solo DTO (que es lo que el Processor recibe ahora)
        MovimientoDTO contrato = new MovimientoDTO();
        contrato.setNumeroCuenta("987654321");

        // 2. Act: Llamamos al método process con un solo objeto
        String result = process.process(contrato);

        // 3. Assert
        // Verificamos que la librería LOX se llamó exactamente 1 vez
        verify(loxR174, times(1)).executeCreateCreditContract(anyMap());

        // Verificamos que el contador interno subió a 1
        Integer currentCount = (Integer) ReflectionTestUtils.getField(process, "totalProcesados");
        assertEquals("Debería haber procesado 1 contrato", Integer.valueOf(1), currentCount);

        // Verificamos que el resultado no sea nulo y contenga datos
        assertNotNull("El String resultante no debe ser nulo", result);
    }
}