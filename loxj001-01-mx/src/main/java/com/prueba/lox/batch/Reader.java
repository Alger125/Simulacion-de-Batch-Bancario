package com.prueba.lox.batch;

import org.springframework.batch.item.ItemReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Reader implements ItemReader<List<Map<String, Object>>> {

    // Cambiamos la bandera por un índice para saber dónde vamos
    private int indiceActual = 0;
    private final int TAMANO_PAGINA = 100;
    private final int TOTAL_REGISTROS = 1000; // Simulemos que hay 1000 registros en total

    @Override
    public List<Map<String, Object>> read() throws Exception {

        // 1. CONDICIÓN DE PARADA
        // Si nuestro índice llegó al total, devolvemos null para terminar el Job
        if (indiceActual >= TOTAL_REGISTROS) {
            System.out.println(">>> READER: Se han procesado todos los registros. Finalizando...");
            return null;
        }

        // 2. CREACIÓN DE LA PÁGINA (BLOQUE)
        List<Map<String, Object>> pagina = new ArrayList<>();

        // Llenamos la página con 100 elementos
        for (int i = 0; i < TAMANO_PAGINA && (indiceActual + i) < TOTAL_REGISTROS; i++) {
            Map<String, Object> contrato = new HashMap<>();

            // Datos simulados dinámicos basándonos en el índice
            int idSimulado = indiceActual + i + 1;
            contrato.put("CONTRATO_ID", "CONT-" + idSimulado);
            contrato.put("ESTADO", "ACTIVO");
            contrato.put("IMPORTE", 1000.0 + idSimulado);

            pagina.add(contrato);
        }

        // 3. ACTUALIZAR EL PUNTERO
        // Avisamos que la próxima vez que entre, empiece 100 registros después
        System.out.println(">>> READER: Enviando bloque desde registro " + (indiceActual + 1) +
                " hasta " + (indiceActual + pagina.size()));

        indiceActual += TAMANO_PAGINA;

        return pagina;
    }
}