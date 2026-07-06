package com.prueba.lox.batch;

import com.prueba.lox.batch.Utility.Utility;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class Complete extends Utility implements Tasklet {
    @Override
    public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
        this.getLoxR174().executeCreateCreditContract(this.getMapEvent("OK", "Ejecucion Correcta del JOB LOX Local"));
        return RepeatStatus.FINISHED;
    }
}