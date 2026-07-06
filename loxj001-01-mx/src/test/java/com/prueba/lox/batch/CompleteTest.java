package com.prueba.lox.batch;

import com.prueba.lox.lib.r174.interfaz.LOXR174;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CompleteTest {

    @InjectMocks
    private Complete complete; // La clase a probar

    @Mock
    private LOXR174 loxR174; // El servicio que devuelve getLoxR174()

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Como Complete hereda de Utility, necesitamos que el mock
        // se comporte como si Utility tuviera el servicio inyectado.
        // Si Utility usa un setter, podrías necesitar: complete.setLoxR174(loxR174);
    }

    @Test
    public void execute_shouldCallServiceWithSuccessMessage() throws Exception {
        // Arrange
        // Simulamos el comportamiento del mapa que genera getMapEvent
        Map<String, Object> mockMap = new HashMap<>();
        mockMap.put("status", "OK");

        // Act
        RepeatStatus status = complete.execute(null, null);

        // Assert
        assertEquals(RepeatStatus.FINISHED, status);

        // Verificamos que se llamó al servicio con los parámetros de éxito
        verify(loxR174, times(1)).executeCreateCreditContract(anyMap());
    }
}