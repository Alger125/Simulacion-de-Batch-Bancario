package com.prueba.lox.batch.mapper;

import com.prueba.lox.batch.model_1.MovimientoDTO;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PimientosMapper implements RowMapper<MovimientoDTO> {
    @Override
    public MovimientoDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        MovimientoDTO dto = new MovimientoDTO();
        dto.setNombre(rs.getString("NOMBRE"));
        dto.setNumeroCuenta(rs.getString("NUMERO_CUENTA"));
        dto.setMonto(rs.getDouble("MONTO"));
        return dto;
    }
}