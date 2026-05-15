package com.Restaurant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio para insertar datos genéricamente en cualquier tabla.
 * 
 * Usa JdbcTemplate para:
 * - Construir INSERT dinámicamente
 * - Validar tabla y columnas
 * - Ejecutar inserción segura
 */
@Service
public class DatabaseInsertServiceImplementation implements DatabaseInsertService {
    
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseInsertServiceImplementation.class);
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseInsertServiceImplementation(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public String insertData(String tableName, Map<String, Object> data) throws Exception {
        LOG.info("MCP: Insertar en tabla '{}' con datos: {}", tableName, data);
        
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de tabla no puede estar vacío");
        }
        
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Datos a insertar no pueden estar vacíos");
        }
        
        // Validar que la tabla existe
        List<String> tables = listAvailableTables();
        String tableNameLower = tableName.toLowerCase();
        boolean tableExists = tables.stream()
                .anyMatch(t -> t.toLowerCase().equals(tableNameLower));
        
        if (!tableExists) {
            throw new Exception("Tabla '" + tableName + "' no existe. Tablas disponibles: " + tables);
        }
        
        // Validar que las columnas existen
        Map<String, String> columns = getTableColumns(tableName);
        for (String columnName : data.keySet()) {
            if (!columns.containsKey(columnName.toLowerCase())) {
                throw new Exception("Columna '" + columnName + "' no existe en tabla '" + tableName 
                        + "'. Columnas disponibles: " + columns.keySet());
            }
        }
        
        // Construir INSERT dinámicamente
        String columnsList = String.join(",", data.keySet());
        String placeholders = data.keySet().stream().map(k -> "?").collect(Collectors.joining(","));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnsList, placeholders);
        
        LOG.info("MCP: SQL generado: {}", sql);
        
        try {
            // Ejecutar insert
            int rowsAffected = jdbcTemplate.update(sql, data.values().toArray());
            
            String result = String.format("✓ Registro insertado exitosamente en tabla '%s'. "
                    + "Filas afectadas: %d", tableName, rowsAffected);
            LOG.info("MCP: {}", result);
            return result;
            
        } catch (Exception e) {
            String error = String.format("✗ Error al insertar en tabla '%s': %s", tableName, e.getMessage());
            LOG.error("MCP: {}", error, e);
            throw new Exception(error, e);
        }
    }
    
    @Override
    public List<String> listAvailableTables() throws Exception {
        LOG.info("MCP: Listando tablas disponibles en BD");
        
        try {
            DatabaseMetaData dbMetaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
            ResultSet tables = dbMetaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            List<String> tableList = new ArrayList<>();
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                // Filtrar tablas del sistema (información_schema, mysql, etc)
                if (!tableName.toLowerCase().contains("information_schema") 
                        && !tableName.toLowerCase().contains("mysql")
                        && !tableName.toLowerCase().contains("performance_schema")) {
                    tableList.add(tableName);
                }
            }
            
            LOG.info("MCP: {} tablas encontradas: {}", tableList.size(), tableList);
            return tableList;
            
        } catch (Exception e) {
            LOG.error("MCP: Error al listar tablas", e);
            throw new Exception("Error al obtener lista de tablas: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, String> getTableColumns(String tableName) throws Exception {
        LOG.info("MCP: Obteniendo columnas de tabla '{}'", tableName);
        
        try {
            DatabaseMetaData dbMetaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
            ResultSet columns = dbMetaData.getColumns(null, null, tableName, null);
            
            Map<String, String> columnMap = new LinkedHashMap<>();
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                columnMap.put(columnName.toLowerCase(), columnType);
            }
            
            if (columnMap.isEmpty()) {
                throw new Exception("Tabla '" + tableName + "' no encontrada o no tiene columnas");
            }
            
            LOG.info("MCP: Tabla '{}' tiene {} columnas: {}", tableName, columnMap.size(), columnMap.keySet());
            return columnMap;
            
        } catch (Exception e) {
            LOG.error("MCP: Error al obtener columnas de tabla '{}'", tableName, e);
            throw new Exception("Error al obtener columnas de '" + tableName + "': " + e.getMessage(), e);
        }
    }
}
