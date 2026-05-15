package com.Restaurant.service;

import java.util.Map;

/**
 * Servicio para insertar datos genéricamente en cualquier tabla de la BD.
 * 
 * Permite que Ollama inserte registros especificando:
 * - tableName: nombre de la tabla (users, restaurants, food, etc)
 * - data: Map con pares columna:valor
 * 
 * Ejemplo:
 * insertData("users", {
 *   "name": "Juan",
 *   "email": "juan@test.com",
 *   "phone": "3001234567"
 * })
 */
public interface DatabaseInsertService {
    
    /**
     * Inserta un registro en cualquier tabla.
     * 
     * @param tableName Nombre de la tabla (ej: users, restaurants, food)
     * @param data Map con columna:valor (ej: {"name": "Juan", "email": "juan@test.com"})
     * @return Mensaje de éxito con cantidad de registros insertados
     * @throws Exception Si la tabla no existe, columnas inválidas, o error de inserción
     */
    String insertData(String tableName, Map<String, Object> data) throws Exception;
    
    /**
     * Lista todas las tablas disponibles en la BD.
     * 
     * @return Lista de nombres de tablas
     * @throws Exception Si hay error al consultar metadatos
     */
    java.util.List<String> listAvailableTables() throws Exception;
    
    /**
     * Obtiene información sobre las columnas de una tabla.
     * 
     * @param tableName Nombre de la tabla
     * @return Map con columna:tipo para cada columna
     * @throws Exception Si la tabla no existe
     */
    Map<String, String> getTableColumns(String tableName) throws Exception;
}
