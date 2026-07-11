package com.example.expensetracker.repository.mapper;

import com.example.expensetracker.repository.model.ImportRowFailure;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ImportRowFailureMapper implements RowMapper<ImportRowFailure> {

    @Override
    public ImportRowFailure mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ImportRowFailure(rs.getInt("row_number"), rs.getString("error_message"));
    }
}
