package com.prueba.lox.batch;

import com.prueba.lox.lib.r174.interfaz.LOXR174;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

public class FailedTest {

    @InjectMocks
    private Failed failed;

    @Mock
    private LOXR174 loxR174;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void execute_shouldCallServiceWithRealErrorMessage() throws Exception {
        // 1. Arrange: Simulamos la cadena de objetos de Spring Batch
        ChunkContext chunkContext = mock(ChunkContext.class);
        StepContext stepContext = mock(StepContext.class);
        StepExecution stepExecution = mock(StepExecution.class);
        JobExecution jobExecution = mock(JobExecution.class);

        // Simulamos una excepción en la lista de fallos
        List<Throwable> exceptions = new ArrayList<>();
        exceptions.add(new RuntimeException("Error de conexión a Oracle"));

        // Encadenamos los mocks
        when(chunkContext.getStepContext()).thenReturn(stepContext);
        when(stepContext.getStepExecution()).thenReturn(stepExecution);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getAllFailureExceptions()).thenReturn(exceptions);

        // 2. Act
        RepeatStatus status = failed.execute(null, chunkContext);

        // 3. Assert
        assertEquals(RepeatStatus.FINISHED, status);
        // Verificamos que se llamó a la librería (el mensaje contendrá "Error de conexión")
        verify(loxR174, times(1)).executeCreateCreditContract(anyMap());
    }

    @Test
    public void execute_shouldHandleExceptionInExtractionAndEnterCatch() throws Exception {
        // 1. Arrange: Mandamos un ChunkContext nulo para forzar el NullPointerException y entrar al CATCH

        // 2. Act
        RepeatStatus status = failed.execute(null, null);

        // 3. Assert
        assertEquals(RepeatStatus.FINISHED, status);
        // Verificamos que aunque falló la extracción, el Tasklet terminó y notificó el error genérico
        verify(loxR174, times(1)).executeCreateCreditContract(anyMap());
    }
}