package com.prueba.lox.lib.r174.impl;

import com.prueba.lox.batch.model_1.MovimientoDTO;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class LOXR174ImplTest {

    /* =====================================================
       TESTS extractSaldo
       ===================================================== */

    @Test
    public void extractSaldo_whenMapIsNull_shouldReturnZero() {
        LOXR174Impl impl = new LOXR174Impl();
        assertEquals(0.0, impl.extractSaldo(null), 0.0);
    }

    @Test
    public void extractSaldo_whenKeyDoesNotExist_shouldReturnZero() {
        LOXR174Impl impl = new LOXR174Impl();
        Map<String, Object> infoSaldo = new HashMap<>();
        infoSaldo.put("OTRA_LLAVE", 9999.0);
        assertEquals(0.0, impl.extractSaldo(infoSaldo), 0.0);
    }

    @Test
    public void extractSaldo_whenValueIsNumber_shouldReturnValue() {
        LOXR174Impl impl = new LOXR174Impl();
        Map<String, Object> infoSaldo = new HashMap<>();
        infoSaldo.put("SALDO_DISPONIBLE", 15000.75);
        assertEquals(15000.75, impl.extractSaldo(infoSaldo), 0.0);
    }

    /* =====================================================
       TESTS executeCreateCreditContract
       ===================================================== */

    @Test
    public void executeCreateCreditContract_whenStatusIsNotProcess_shouldDoNothing() {
        LOXR174Impl impl = new LOXR174Impl();
        Map<String, Object> event = new HashMap<>();
        // CORRECCIÓN: Usamos la llave correcta "STATUS" pero con valor incorrecto
        event.put("STATUS", "OTHER");

        impl.executeCreateCreditContract(event);
        // Pasa si no hay excepción
    }

    @Test
    public void executeCreateCreditContract_whenValidEvent_shouldCallConsultarDetalleSaldo() {
        LOXR174Impl impl = spy(new LOXR174Impl());
        MovimientoDTO item = new MovimientoDTO();
        item.setNumeroCuenta("123456");

        Map<String, Object> event = new HashMap<>();
        // CORRECCIÓN: Llave "STATUS" en mayúsculas
        event.put("STATUS", "PROCESS");
        event.put("item", item);

        // Evitamos que intente ir a la base de datos real
        doReturn(new HashMap<String, Object>())
                .when(impl)
                .consultarDetalleSaldo(anyString());

        impl.executeCreateCreditContract(event);

        verify(impl, times(1)).consultarDetalleSaldo("123456");
    }

    /* =====================================================
       TESTS consultarDetalleSaldo
       ===================================================== */

    @Test
    public void consultarDetalleSaldo_whenJdbcReturnsData_shouldReturnPopulatedMap() {
        // 1. Arrange
        LOXR174Impl impl = new LOXR174Impl();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        // Seteamos los campos necesarios
        String sqlPrueba = "SELECT * FROM SALDOS WHERE CUENTA = ?";
        ReflectionTestUtils.setField(impl, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(impl, "sqlDetalleSaldos", sqlPrueba);

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("SALDO_DISPONIBLE", 1000.50);

        // CORRECCIÓN: Usamos any() para el segundo parámetro para que coincida
        // con el 'new Object[]{numeroCuenta}' que creas en la implementación.
        when(jdbcTemplate.queryForMap(eq(sqlPrueba), any()))
                .thenReturn(mockResponse);

        // 2. Act
        HashMap<String, Object> result = impl.consultarDetalleSaldo("123456");

        // 3. Assert
        assertNotNull("El resultado no debe ser nulo", result);
        assertFalse("El mapa debe contener datos", result.isEmpty());
        // Usamos Double para asegurar que la comparación de objetos sea correcta
        assertEquals(Double.valueOf(1000.50), (Double) result.get("SALDO_DISPONIBLE"));
    }

    @Test
    public void executeCreateCreditContract_whenItemIsNull_shouldReturnEarly() {
        // 1. Arrange (Preparar)
        // Usamos spy para poder vigilar si se llaman métodos internos después del return
        LOXR174Impl impl = spy(new LOXR174Impl());

        Map<String, Object> event = new HashMap<>();
        event.put("STATUS", "PROCESS"); // Pasamos el primer IF (el de status)
        event.put("item", null);        // <--- Aquí forzamos entrar al segundo RETURN

        // 2. Act (Ejecutar)
        impl.executeCreateCreditContract(event);

        // 3. Assert (Verificar)
        // Verificamos que el código se detuvo y NUNCA llegó a consultar el saldo
        verify(impl, never()).consultarDetalleSaldo(anyString());

        // Si llegas aquí sin errores, el return funcionó correctamente
    }

    @Test
    public void consultarDetalleSaldo_whenExceptionOccurs_shouldReturnEmptyMap() {
        // 1. Arrange (Preparar)
        LOXR174Impl impl = new LOXR174Impl();
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        // Inyectamos el mock de jdbcTemplate
        ReflectionTestUtils.setField(impl, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(impl, "sqlDetalleSaldos", "SELECT * FROM SALDOS");

        // PROGRAMAMOS EL ERROR: Cuando llame a queryForMap, lanzamos una RuntimeException
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("Error simulado de base de datos"));

        // 2. Act (Ejecutar)
        HashMap<String, Object> result = impl.consultarDetalleSaldo("123456");

        // 3. Assert (Verificar)
        // El método no debe propagar la excepción, debe atraparla y devolver un mapa vacío
        assertNotNull("El resultado no debe ser null", result);
        assertTrue("El mapa debe estar vacío porque entró al catch", result.isEmpty());
    }

    @Test
    public void testSettersInAbstractClass() {
        LOXR174Impl impl = new LOXR174Impl();
        JdbcTemplate mockJdbc = mock(JdbcTemplate.class);
        String sql = "SELECT 1 FROM DUAL";

        impl.setJdbcTemplate(mockJdbc);
        impl.setSqlDetalleSaldos(sql);

        assertEquals(mockJdbc, ReflectionTestUtils.getField(impl, "jdbcTemplate"));
        assertEquals(sql, ReflectionTestUtils.getField(impl, "sqlDetalleSaldos"));
    }
}