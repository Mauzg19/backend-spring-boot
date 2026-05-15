package com.Restaurant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DataWarehouseService {

    private static final Logger logger = LoggerFactory.getLogger(DataWarehouseService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void initializeDataWarehouse() {
        logger.info("Inicializando Data Warehouse...");

        try {
            createDatabase();
            truncateTables();
            populateDimCustomer();
            populateDimRestaurant();
            populateDimCategory();
            populateDimFood();
            populateDimDate();
            populateDimTime();
            populateFactOrderItems();

            logger.info("Data Warehouse inicializado exitosamente");
        } catch (Exception e) {
            logger.error("Error al inicializar Data Warehouse", e);
        }
    }

    private void createDatabase() {
        try {
            String[] createTableStatements = {
                "CREATE DATABASE IF NOT EXISTS restaurant_dw",
                "CREATE TABLE IF NOT EXISTS restaurant_dw.dim_customer (" +
                "  customer_key BIGINT PRIMARY KEY," +
                "  customer_id BIGINT," +
                "  full_name VARCHAR(255)," +
                "  email VARCHAR(255)," +
                "  role VARCHAR(50)," +
                "  status VARCHAR(50)" +
                ")",
                "CREATE TABLE IF NOT EXISTS restaurant_dw.dim_restaurant (" +
                "  restaurant_key BIGINT PRIMARY KEY," +
                "  restaurant_id BIGINT," +
                "  restaurant_name VARCHAR(255)," +
                "  cuisine_type VARCHAR(100)," +
                "  owner_name VARCHAR(255)," +
                "  restaurant_open BOOLEAN," +
                "  num_rating INT," +
                "  registration_date DATE," +
                "  city VARCHAR(100)" +
                ")",
                "CREATE TABLE IF NOT EXISTS restaurant_dw.dim_category (" +
                "  category_key BIGINT PRIMARY KEY," +
                "  category_id BIGINT," +
                "  category_name VARCHAR(255)," +
                "  restaurant_id BIGINT" +
                ")",
                "CREATE TABLE IF NOT EXISTS restaurant_dw.dim_food (" +
                "  food_key BIGINT PRIMARY KEY," +
                "  food_id BIGINT," +
                "  food_name VARCHAR(255)," +
                "  description VARCHAR(1000)," +
                "  price BIGINT," +
                "  available BOOLEAN," +
                "  is_vegetarian BOOLEAN," +
                "  is_seasonal BOOLEAN," +
                "  restaurant_id BIGINT," +
                "  category_id BIGINT," +
                "  category_name VARCHAR(255)" +
                ")",
                "CREATE TABLE IF NOT EXISTS restaurant_dw.dim_date (" +
                "  date_key INT PRIMARY KEY," +
                "  date_value DATE," +
                "  year INT," +
                "  month INT," +
                "  month_name VARCHAR(20)," +
                "  quarter INT," +
                "  day_of_week INT," +
                "  day_name VARCHAR(20)," +
                "  is_weekend BOOLEAN" +
                ")",
                "CREATE TABLE IF NOT EXISTS restaurant_dw.dim_time (" +
                "  time_key INT PRIMARY KEY," +
                "  time_value TIME," +
                "  hour INT," +
                "  minute INT," +
                "  time_period VARCHAR(20)" +
                ")",
                "CREATE TABLE IF NOT EXISTS restaurant_dw.fact_order_items (" +
                "  order_item_key BIGINT PRIMARY KEY," +
                "  order_id BIGINT," +
                "  order_status VARCHAR(100)," +
                "  customer_key BIGINT," +
                "  restaurant_key BIGINT," +
                "  food_key BIGINT," +
                "  category_key BIGINT," +
                "  date_key INT," +
                "  time_key INT," +
                "  quantity INT," +
                "  food_price BIGINT," +
                "  item_total_price BIGINT," +
                "  order_total_amount BIGINT," +
                "  total_item_count INT," +
                "  delivery_city VARCHAR(100)," +
                "  payment_method VARCHAR(100)" +
                ")",
                "CREATE INDEX IF NOT EXISTS idx_fact_customer ON restaurant_dw.fact_order_items (customer_key)",
                "CREATE INDEX IF NOT EXISTS idx_fact_restaurant ON restaurant_dw.fact_order_items (restaurant_key)",
                "CREATE INDEX IF NOT EXISTS idx_fact_food ON restaurant_dw.fact_order_items (food_key)",
                "CREATE INDEX IF NOT EXISTS idx_fact_category ON restaurant_dw.fact_order_items (category_key)",
                "CREATE INDEX IF NOT EXISTS idx_fact_date ON restaurant_dw.fact_order_items (date_key)",
                "CREATE INDEX IF NOT EXISTS idx_fact_time ON restaurant_dw.fact_order_items (time_key)"
            };

            for (String stmt : createTableStatements) {
                try {
                    jdbcTemplate.execute(stmt);
                    logger.debug("Ejecutado: " + stmt.substring(0, Math.min(50, stmt.length())));
                } catch (Exception e) {
                    logger.debug("Skipped: " + e.getMessage());
                }
            }
            logger.info("Database schema creado exitosamente");
        } catch (Exception e) {
            logger.error("Error al crear Database Schema", e);
        }
    }

    private void truncateTables() {
        String[] tables = {
            "restaurant_dw.fact_order_items",
            "restaurant_dw.dim_time",
            "restaurant_dw.dim_date",
            "restaurant_dw.dim_food",
            "restaurant_dw.dim_category",
            "restaurant_dw.dim_restaurant",
            "restaurant_dw.dim_customer"
        };

        for (String table : tables) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
                logger.debug("Truncated: " + table);
            } catch (Exception e) {
                logger.warn("No se pudo truncar " + table + ": " + e.getMessage());
            }
        }
    }

    private void populateDimCustomer() {
        String sql = "INSERT INTO restaurant_dw.dim_customer (customer_key, customer_id, full_name, email, role, status) " +
                "SELECT u.id, u.id, u.full_name, u.email, CAST(u.role AS CHAR), u.status " +
                "FROM restaurante_ing.user u";
        int count = jdbcTemplate.update(sql);
        logger.info("dim_customer cargado: {} registros", count);
    }

    private void populateDimRestaurant() {
        String sql = "INSERT INTO restaurant_dw.dim_restaurant (restaurant_key, restaurant_id, restaurant_name, " +
                "cuisine_type, owner_name, restaurant_open, num_rating, registration_date, city) " +
                "SELECT r.id, r.id, r.name, r.cuisine_type, CONCAT(COALESCE(o.full_name, 'Unknown')), " +
                "r.open, r.num_rating, DATE(r.registration_date), COALESCE(a.city, 'Unknown') " +
                "FROM restaurante_ing.restaurant r " +
                "LEFT JOIN restaurante_ing.user o ON r.owner_id = o.id " +
                "LEFT JOIN restaurante_ing.address a ON r.address_id = a.id";
        int count = jdbcTemplate.update(sql);
        logger.info("dim_restaurant cargado: {} registros", count);
    }

    private void populateDimCategory() {
        String sql = "INSERT INTO restaurant_dw.dim_category (category_key, category_id, category_name, restaurant_id) " +
                "SELECT c.id, c.id, c.name, c.restaurant_id " +
                "FROM restaurante_ing.category c";
        int count = jdbcTemplate.update(sql);
        logger.info("dim_category cargado: {} registros", count);
    }

    private void populateDimFood() {
        String sql = "INSERT INTO restaurant_dw.dim_food (food_key, food_id, food_name, description, price, " +
                "available, is_vegetarian, is_seasonal, restaurant_id, category_id, category_name) " +
                "SELECT f.id, f.id, f.name, f.description, f.price, f.available, f.is_vegetarian, f.is_seasonal, " +
                "f.restaurant_id, f.food_category_id, COALESCE(c.name, 'Unknown') " +
                "FROM restaurante_ing.food f " +
                "LEFT JOIN restaurante_ing.category c ON f.food_category_id = c.id";
        int count = jdbcTemplate.update(sql);
        logger.info("dim_food cargado: {} registros", count);
    }

    private void populateDimDate() {
        String sql = "INSERT INTO restaurant_dw.dim_date (date_key, date_value, year, month, month_name, quarter, " +
                "day_of_week, day_name, is_weekend) " +
                "SELECT DISTINCT " +
                "DATE_FORMAT(DATE(o.created_at), '%Y%m%d') + 0, " +
                "DATE(o.created_at), " +
                "YEAR(o.created_at), " +
                "MONTH(o.created_at), " +
                "DATE_FORMAT(o.created_at, '%M'), " +
                "QUARTER(o.created_at), " +
                "DAYOFWEEK(o.created_at), " +
                "DATE_FORMAT(o.created_at, '%W'), " +
                "CASE WHEN DAYOFWEEK(o.created_at) IN (1,7) THEN TRUE ELSE FALSE END " +
                "FROM restaurante_ing.orders o";
        int count = jdbcTemplate.update(sql);
        logger.info("dim_date cargado: {} registros", count);
    }

    private void populateDimTime() {
        String sql = "INSERT INTO restaurant_dw.dim_time (time_key, time_value, hour, minute, time_period) " +
                "SELECT DISTINCT " +
                "HOUR(o.created_at) * 10000 + MINUTE(o.created_at) * 100 + SECOND(o.created_at), " +
                "TIME(o.created_at), " +
                "HOUR(o.created_at), " +
                "MINUTE(o.created_at), " +
                "CASE " +
                "  WHEN HOUR(o.created_at) BETWEEN 6 AND 11 THEN 'Morning' " +
                "  WHEN HOUR(o.created_at) BETWEEN 12 AND 17 THEN 'Afternoon' " +
                "  WHEN HOUR(o.created_at) BETWEEN 18 AND 21 THEN 'Evening' " +
                "  ELSE 'Night' " +
                "END " +
                "FROM restaurante_ing.orders o";
        int count = jdbcTemplate.update(sql);
        logger.info("dim_time cargado: {} registros", count);
    }

    private void populateFactOrderItems() {
        String sql = "INSERT INTO restaurant_dw.fact_order_items (order_item_key, order_id, order_status, " +
                "customer_key, restaurant_key, food_key, category_key, date_key, time_key, quantity, " +
                "food_price, item_total_price, order_total_amount, total_item_count, delivery_city, payment_method) " +
                "SELECT " +
                "oi.id, o.id, o.order_status, o.customer_id, o.restaurant_id, oi.food_id, f.food_category_id, " +
                "DATE_FORMAT(DATE(o.created_at), '%Y%m%d') + 0, " +
                "HOUR(o.created_at) * 10000 + MINUTE(o.created_at) * 100 + SECOND(o.created_at), " +
                "oi.quantity, f.price, oi.total_price, o.total_amount, o.total_item, " +
                "COALESCE(a.city, 'Unknown'), COALESCE(p.payment_method, 'Unknown') " +
                "FROM restaurante_ing.order_item oi " +
                "JOIN restaurante_ing.orders o ON oi.order_id = o.id " +
                "LEFT JOIN restaurante_ing.food f ON oi.food_id = f.id " +
                "LEFT JOIN restaurante_ing.address a ON o.delivery_address_id = a.id " +
                "LEFT JOIN restaurante_ing.payment p ON o.payment_id = p.id";
        int count = jdbcTemplate.update(sql);
        logger.info("fact_order_items cargado: {} registros", count);
    }
}
