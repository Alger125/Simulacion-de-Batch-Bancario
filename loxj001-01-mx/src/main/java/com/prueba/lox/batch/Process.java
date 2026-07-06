package com.prueba.lox.batch;

import com.prueba.lox.batch.Utility.Utility;
import com.prueba.lox.batch.model_1.MovimientoDTO;
import org.springframework.batch.item.ItemProcessor;
import java.util.Map;

public class Process extends Utility implements ItemProcessor<MovimientoDTO, String> {

    private int totalProcesados = 0;

    @Override
    public String process(MovimientoDTO item) throws Exception {
        // Lógica de librería
        Map<String, Object> event = this.getMapEvent("PROCESS", item);
        this.getLoxR174().executeCreateCreditContract(event);

        totalProcesados++;

        // CADA 1000 REGISTROS imprimimos en consola para ver el progreso
        if (totalProcesados % 1000 == 0) {
            System.out.println(">>> PROGRESO: Se han procesado " + totalProcesados + " registros...");
        }

        return this.complementDataContracts(item) + "\n";
    }
}