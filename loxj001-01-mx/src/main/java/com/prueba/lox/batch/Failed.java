package com.prueba.lox.batch;

import com.prueba.lox.batch.Utility.Utility;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class Failed extends Utility implements Tasklet {

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        // 1. Extraemos el mensaje de error que detuvo el Job
        String causaError = extraerMensajeDeError(chunkContext);

        // 2. Notificamos a la librería con información detallada
        this.getLoxR174().executeCreateCreditContract(
                this.getMapEvent("KO", "JOB_FAILED: " + causaError)
        );

        System.err.println(">>> ALERTA: El Job falló. Motivo: " + causaError);

        return RepeatStatus.FINISHED;
    }

    /**
     * Intenta obtener la excepción que causó el fallo desde el contexto de ejecución.
     */
    private String extraerMensajeDeError(ChunkContext chunkContext) {
        try {
            // Spring Batch guarda las excepciones en el StepContext
            Object exception = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getAllFailureExceptions();
            return exception.toString();
        } catch (Exception e) {
            return "Error desconocido en la ejecución del Job";
        }
    }
}