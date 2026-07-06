package com.prueba.lox.batch.Utility;

import com.prueba.lox.batch.model_1.MovimientoDTO;
import com.prueba.lox.lib.r174.interfaz.LOXR174;
import org.junit.Before;
import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class UtilityTest {

    private Utility utility;
    private String odateHoy;
    private final String ODATE_FIJA = "20260212";
    private final String ODATE_FORMATEADA_ESPERADA = "2026/02/12";

    @Before
    public void setUp() {
        utility = new Utility();
        // Obtenemos la fecha del sistema para validar la generación automática
        odateHoy = new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    @Test
    public void testGetOdate_AutoGeneration() {
        // Escenario: No se asigna fecha, debe generar la de hoy automáticamente (formato plano YYYYMMDD)
        utility.setOdate(null);
        assertEquals("Debe generar la fecha plana si es null", odateHoy, utility.getOdate());

        utility.setOdate("");
        assertEquals("Debe generar la fecha plana si está vacía", odateHoy, utility.getOdate());
    }

    @Test
    public void testGettersAndSetters() {
        LOXR174 mockLox = mock(LOXR174.class);
        utility.setLoxR174(mockLox);
        assertEquals(mockLox, utility.getLoxR174());

        utility.setOdate(ODATE_FIJA);
        assertEquals(ODATE_FIJA, utility.getOdate());
    }

    @Test
    public void testComplementDataContracts_FormatoFechaConDiagonales() {
        // Prueba específica para validar que el archivo de salida use diagonales
        utility.setOdate(ODATE_FIJA);
        MovimientoDTO dto = new MovimientoDTO();
        dto.setNombre("TEST");
        dto.setNumeroCuenta("123");
        dto.setMonto(100.0);

        String result = utility.complementDataContracts(dto);

        // Validamos que NO contenga la fecha plana y SÍ contenga las diagonales
        assertFalse("El reporte no debe usar formato plano", result.contains(ODATE_FIJA));
        assertTrue("El reporte debe usar formato YYYY/MM/DD", result.contains(ODATE_FORMATEADA_ESPERADA));
    }

    @Test
    public void testComplementDataContracts_WithDTO() {
        utility.setOdate(ODATE_FIJA);

        MovimientoDTO dto = new MovimientoDTO();
        dto.setNombre("JUAN PEREZ");
        dto.setNumeroCuenta("CTA-001");
        dto.setMonto(15000.00);

        String result = utility.complementDataContracts(dto);

        assertTrue("Contiene nombre", result.contains("JUAN PEREZ"));
        assertTrue("Contiene cuenta", result.contains("CTA-001"));
        assertTrue("Contiene fecha con diagonales", result.contains(ODATE_FORMATEADA_ESPERADA));

        // Caso null
        assertEquals("", utility.complementDataContracts((MovimientoDTO) null));
    }

    @Test
    public void testComplementDataContracts_WithList() {
        utility.setOdate(ODATE_FIJA);

        List<Map<String, Object>> lista = new ArrayList<>();
        Map<String, Object> contrato = new HashMap<>();
        contrato.put("CONTRATO_ID", "999");
        lista.add(contrato);

        String result = utility.complementDataContracts(lista);
        assertTrue(result.contains("CONTRATO: 999"));
        assertTrue("La lista también debe usar diagonales", result.contains(ODATE_FORMATEADA_ESPERADA));

        // Casos vacíos
        assertEquals("", utility.complementDataContracts((List<Map<String, Object>>) null));
    }

    @Test
    public void testGetMapEvent_MantieneFechaPlanaParaLibreria() {
        // IMPORTANTE: La comunicación con la librería/DB debe seguir siendo YYYYMMDD
        utility.setOdate(ODATE_FIJA);

        Map<String, Object> eventMsg = utility.getMapEvent("OK", "Mensaje");
        Map<String, Object> eventObj = utility.getMapEvent("PROCESS", new Object());

        assertEquals("La librería necesita la fecha sin diagonales", ODATE_FIJA, eventMsg.get("ODATE"));
        assertEquals("El evento de objeto necesita fecha sin diagonales", ODATE_FIJA, eventObj.get("ODATE"));
    }

    @Test
    public void testObtenerFormatoSalida() {
        assertEquals("DatoPrueba", utility.obtenerFormatoSalida("DatoPrueba"));
        assertEquals("", utility.obtenerFormatoSalida(null));
    }
}