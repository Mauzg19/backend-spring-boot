-- STAR_SCHEMA_POWERBI.sql
-- Data Warehouse Schema para Power BI Integration
-- Ejecutado automáticamente por Spring Boot al iniciar

CREATE DATABASE IF NOT EXISTS restaurant_dw;
USE restaurant_dw;

-- Dimensión Cliente
CREATE TABLE IF NOT EXISTS dim_customer (
  customer_key BIGINT PRIMARY KEY,
  customer_id BIGINT,
  full_name VARCHAR(255),
  email VARCHAR(255),
  role VARCHAR(50),
  status VARCHAR(50)
);

-- Dimensión Restaurante
CREATE TABLE IF NOT EXISTS dim_restaurant (
  restaurant_key BIGINT PRIMARY KEY,
  restaurant_id BIGINT,
  restaurant_name VARCHAR(255),
  cuisine_type VARCHAR(100),
  owner_name VARCHAR(255),
  restaurant_open BOOLEAN,
  num_rating INT,
  registration_date DATE,
  city VARCHAR(100)
);

-- Dimensión Categoría de Platillo
CREATE TABLE IF NOT EXISTS dim_category (
  category_key BIGINT PRIMARY KEY,
  category_id BIGINT,
  category_name VARCHAR(255),
  restaurant_id BIGINT
);

-- Dimensión Platillo / Producto
CREATE TABLE IF NOT EXISTS dim_food (
  food_key BIGINT PRIMARY KEY,
  food_id BIGINT,
  food_name VARCHAR(255),
  description VARCHAR(1000),
  price BIGINT,
  available BOOLEAN,
  is_vegetarian BOOLEAN,
  is_seasonal BOOLEAN,
  restaurant_id BIGINT,
  category_id BIGINT,
  category_name VARCHAR(255)
);

-- Dimensión Fecha
CREATE TABLE IF NOT EXISTS dim_date (
  date_key INT PRIMARY KEY,
  date_value DATE,
  year INT,
  month INT,
  month_name VARCHAR(20),
  quarter INT,
  day_of_week INT,
  day_name VARCHAR(20),
  is_weekend BOOLEAN
);

-- Dimensión Hora
CREATE TABLE IF NOT EXISTS dim_time (
  time_key INT PRIMARY KEY,
  time_value TIME,
  hour INT,
  minute INT,
  time_period VARCHAR(20)
);

-- Tabla de Hechos: órdenes a nivel de item
CREATE TABLE IF NOT EXISTS fact_order_items (
  order_item_key BIGINT PRIMARY KEY,
  order_id BIGINT,
  order_status VARCHAR(100),
  customer_key BIGINT,
  restaurant_key BIGINT,
  food_key BIGINT,
  category_key BIGINT,
  date_key INT,
  time_key INT,
  quantity INT,
  food_price BIGINT,
  item_total_price BIGINT,
  order_total_amount BIGINT,
  total_item_count INT,
  delivery_city VARCHAR(100),
  payment_method VARCHAR(100)
);

-- Índices para Power BI
CREATE INDEX IF NOT EXISTS idx_fact_customer ON fact_order_items (customer_key);
CREATE INDEX IF NOT EXISTS idx_fact_restaurant ON fact_order_items (restaurant_key);
CREATE INDEX IF NOT EXISTS idx_fact_food ON fact_order_items (food_key);
CREATE INDEX IF NOT EXISTS idx_fact_category ON fact_order_items (category_key);
CREATE INDEX IF NOT EXISTS idx_fact_date ON fact_order_items (date_key);
CREATE INDEX IF NOT EXISTS idx_fact_time ON fact_order_items (time_key);
