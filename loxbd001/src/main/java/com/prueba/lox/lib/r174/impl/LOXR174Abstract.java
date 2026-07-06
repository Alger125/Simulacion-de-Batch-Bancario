package com.prueba.lox.lib.r174.impl;

import com.prueba.lox.lib.r174.interfaz.LOXR174;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class LOXR174Abstract implements LOXR174 {

    protected JdbcTemplate jdbcTemplate;
    protected String sqlDetalleSaldos;

    // Los Setters se quedan aquí para siempre
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setSqlDetalleSaldos(String sqlDetalleSaldos) {
        this.sqlDetalleSaldos = sqlDetalleSaldos;
    }
}