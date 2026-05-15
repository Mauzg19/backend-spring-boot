package com.Restaurant.service;

import com.Restaurant.dto.analytics.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== DASHBOARD SUMMARY ====================
    public DashboardSummaryDTO getDashboardSummary() {
        logger.info("Obteniendo resumen del dashboard");

        DashboardSummaryDTO summary = new DashboardSummaryDTO();

        try {
            // Total Revenue
            Long totalRevenue = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(order_total_amount), 0) FROM restaurant_dw.fact_order_items",
                    Long.class);
            summary.setTotalRevenue(totalRevenue);

            // Total Orders
            Integer totalOrders = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT order_id) FROM restaurant_dw.fact_order_items",
                    Integer.class);
            summary.setTotalOrders(totalOrders);

            // Total Customers
            Integer totalCustomers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM restaurant_dw.dim_customer",
                    Integer.class);
            summary.setTotalCustomers(totalCustomers);

            // Total Restaurants
            Integer totalRestaurants = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM restaurant_dw.dim_restaurant",
                    Integer.class);
            summary.setTotalRestaurants(totalRestaurants);

            // Average Order Value
            Double avgOrderValue = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(AVG(order_total_amount), 0) FROM restaurant_dw.fact_order_items",
                    Double.class);
            summary.setAverageOrderValue(avgOrderValue);

            // Unique Cities
            Integer uniqueCities = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT delivery_city) FROM restaurant_dw.fact_order_items",
                    Integer.class);
            summary.setUniqueCities(uniqueCities);

            // Top Performing Restaurants
            Integer topRestaurants = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM (" +
                    "  SELECT restaurant_key FROM restaurant_dw.fact_order_items " +
                    "  GROUP BY restaurant_key " +
                    "  ORDER BY SUM(order_total_amount) DESC LIMIT 5" +
                    ") t",
                    Integer.class);
            summary.setTopPerformerRestaurants(topRestaurants);

            // Best Selling Category
            String bestCategory = jdbcTemplate.queryForObject(
                    "SELECT category_name FROM restaurant_dw.dim_category c " +
                    "JOIN restaurant_dw.fact_order_items f ON c.category_key = f.category_key " +
                    "GROUP BY c.category_name ORDER BY SUM(f.quantity) DESC LIMIT 1",
                    String.class);
            summary.setBestSellingCategory(bestCategory);

        } catch (Exception e) {
            logger.error("Error obteniendo resumen del dashboard", e);
        }

        return summary;
    }

    // ==================== SALES ANALYTICS ====================
    public List<SalesAnalyticsDTO> getSalesAnalyticsByDay() {
        String sql = "SELECT DATE(d.date_value) as date, " +
                "SUM(f.order_total_amount) as totalRevenue, " +
                "COUNT(DISTINCT f.order_id) as totalOrders, " +
                "SUM(f.quantity) as totalItems, " +
                "AVG(f.order_total_amount) as averageOrderValue, " +
                "'Daily' as timePeriod " +
                "FROM restaurant_dw.fact_order_items f " +
                "JOIN restaurant_dw.dim_date d ON f.date_key = d.date_key " +
                "GROUP BY DATE(d.date_value) " +
                "ORDER BY DATE(d.date_value) DESC LIMIT 30";

        return jdbcTemplate.query(sql, new SalesAnalyticsRowMapper());
    }

    public List<SalesAnalyticsDTO> getSalesAnalyticsByTimePeriod() {
        String sql = "SELECT NULL as date, " +
                "SUM(f.order_total_amount) as totalRevenue, " +
                "COUNT(DISTINCT f.order_id) as totalOrders, " +
                "SUM(f.quantity) as totalItems, " +
                "AVG(f.order_total_amount) as averageOrderValue, " +
                "t.time_period as timePeriod " +
                "FROM restaurant_dw.fact_order_items f " +
                "JOIN restaurant_dw.dim_time t ON f.time_key = t.time_key " +
                "GROUP BY t.time_period " +
                "ORDER BY FIELD(t.time_period, 'Morning', 'Afternoon', 'Evening', 'Night')";

        return jdbcTemplate.query(sql, new SalesAnalyticsRowMapper());
    }

    // ==================== RESTAURANT PERFORMANCE ====================
    public List<RestaurantPerformanceDTO> getRestaurantPerformance() {
        String sql = "SELECT r.restaurant_id, r.restaurant_name, " +
                "SUM(f.order_total_amount) as totalRevenue, " +
                "COUNT(DISTINCT f.order_id) as totalOrders, " +
                "r.num_rating as averageRating, " +
                "SUM(f.quantity) as totalItems, " +
                "ROUND(SUM(f.order_total_amount) / (SELECT SUM(order_total_amount) FROM restaurant_dw.fact_order_items) * 100, 2) as revenuePercentage, " +
                "r.cuisine_type " +
                "FROM restaurant_dw.fact_order_items f " +
                "JOIN restaurant_dw.dim_restaurant r ON f.restaurant_key = r.restaurant_key " +
                "GROUP BY r.restaurant_id, r.restaurant_name, r.num_rating, r.cuisine_type " +
                "ORDER BY totalRevenue DESC";

        return jdbcTemplate.query(sql, new RestaurantPerformanceRowMapper());
    }

    // ==================== FOOD PERFORMANCE ====================
    public List<FoodPerformanceDTO> getTopFoodItems(int limit) {
        String sql = "SELECT fo.food_id, fo.food_name, " +
                "SUM(f.quantity) as quantitySold, " +
                "SUM(f.item_total_price) as totalRevenue, " +
                "fo.price, " +
                "fo.category_name, " +
                "fo.is_vegetarian, " +
                "fo.is_seasonal, " +
                "ROUND(SUM(f.item_total_price) / (SELECT SUM(item_total_price) FROM restaurant_dw.fact_order_items) * 100, 2) as revenuePercentage " +
                "FROM restaurant_dw.fact_order_items f " +
                "JOIN restaurant_dw.dim_food fo ON f.food_key = fo.food_key " +
                "GROUP BY fo.food_id, fo.food_name, fo.price, fo.category_name, fo.is_vegetarian, fo.is_seasonal " +
                "ORDER BY quantitySold DESC LIMIT ?";

        return jdbcTemplate.query(sql, new FoodPerformanceRowMapper(), limit);
    }

    // ==================== CATEGORY PERFORMANCE ====================
    public List<CategoryPerformanceDTO> getCategoryPerformance() {
        String sql = "SELECT c.category_id, c.category_name, " +
                "SUM(f.quantity) as quantitySold, " +
                "SUM(f.item_total_price) as totalRevenue, " +
                "COUNT(DISTINCT f.food_key) as numberOfDishes, " +
                "ROUND(SUM(f.item_total_price) / (SELECT SUM(item_total_price) FROM restaurant_dw.fact_order_items) * 100, 2) as revenuePercentage, " +
                "(SELECT fo.food_name FROM restaurant_dw.fact_order_items f2 " +
                "JOIN restaurant_dw.dim_food fo ON f2.food_key = fo.food_key " +
                "WHERE f2.category_key = c.category_key " +
                "GROUP BY fo.food_name ORDER BY SUM(f2.quantity) DESC LIMIT 1) as topDish " +
                "FROM restaurant_dw.fact_order_items f " +
                "JOIN restaurant_dw.dim_category c ON f.category_key = c.category_key " +
                "GROUP BY c.category_id, c.category_name " +
                "ORDER BY totalRevenue DESC";

        return jdbcTemplate.query(sql, new CategoryPerformanceRowMapper());
    }

    // ==================== GEOGRAPHIC ANALYTICS ====================
    public List<GeographicAnalyticsDTO> getGeographicAnalytics() {
        String sql = "SELECT delivery_city as city, " +
                "SUM(order_total_amount) as totalRevenue, " +
                "COUNT(DISTINCT order_id) as totalOrders, " +
                "COUNT(DISTINCT customer_key) as totalCustomers, " +
                "AVG(order_total_amount) as averageOrderValue " +
                "FROM restaurant_dw.fact_order_items " +
                "GROUP BY delivery_city " +
                "ORDER BY totalRevenue DESC";

        return jdbcTemplate.query(sql, new GeographicAnalyticsRowMapper());
    }

    // ==================== CUSTOMER ANALYTICS ====================
    public List<CustomerAnalyticsDTO> getTopCustomers(int limit) {
        String sql = "SELECT c.customer_id, c.full_name, " +
                "COUNT(DISTINCT f.order_id) as totalOrders, " +
                "SUM(f.order_total_amount) as totalSpent, " +
                "AVG(f.order_total_amount) as averageOrderValue, " +
                "f.payment_method, " +
                "MAX(d.date_value) as lastOrderDate " +
                "FROM restaurant_dw.fact_order_items f " +
                "JOIN restaurant_dw.dim_customer c ON f.customer_key = c.customer_key " +
                "JOIN restaurant_dw.dim_date d ON f.date_key = d.date_key " +
                "GROUP BY c.customer_id, c.full_name, f.payment_method " +
                "ORDER BY totalSpent DESC LIMIT ?";

        return jdbcTemplate.query(sql, new CustomerAnalyticsRowMapper(), limit);
    }

    // ==================== ROW MAPPERS ====================
    private static class SalesAnalyticsRowMapper implements RowMapper<SalesAnalyticsDTO> {
        @Override
        public SalesAnalyticsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            SalesAnalyticsDTO dto = new SalesAnalyticsDTO();
            try {
                dto.setDate(rs.getDate("date") != null ? rs.getDate("date").toLocalDate() : null);
            } catch (Exception e) {
                dto.setDate(null);
            }
            dto.setTotalRevenue(rs.getLong("totalRevenue"));
            dto.setTotalOrders(rs.getInt("totalOrders"));
            dto.setTotalItems(rs.getInt("totalItems"));
            dto.setAverageOrderValue(rs.getDouble("averageOrderValue"));
            dto.setTimePeriod(rs.getString("timePeriod"));
            return dto;
        }
    }

    private static class RestaurantPerformanceRowMapper implements RowMapper<RestaurantPerformanceDTO> {
        @Override
        public RestaurantPerformanceDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            RestaurantPerformanceDTO dto = new RestaurantPerformanceDTO();
            dto.setRestaurantId(rs.getLong("restaurant_id"));
            dto.setRestaurantName(rs.getString("restaurant_name"));
            dto.setTotalRevenue(rs.getLong("totalRevenue"));
            dto.setTotalOrders(rs.getInt("totalOrders"));
            dto.setAverageRating(rs.getInt("averageRating"));
            dto.setTotalItems(rs.getInt("totalItems"));
            dto.setRevenuePercentage(rs.getDouble("revenuePercentage"));
            dto.setCuisineType(rs.getString("cuisine_type"));
            return dto;
        }
    }

    private static class FoodPerformanceRowMapper implements RowMapper<FoodPerformanceDTO> {
        @Override
        public FoodPerformanceDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            FoodPerformanceDTO dto = new FoodPerformanceDTO();
            dto.setFoodId(rs.getLong("food_id"));
            dto.setFoodName(rs.getString("food_name"));
            dto.setQuantitySold(rs.getInt("quantitySold"));
            dto.setTotalRevenue(rs.getLong("totalRevenue"));
            dto.setPrice(rs.getLong("price"));
            dto.setCategoryName(rs.getString("category_name"));
            dto.setVegetarian(rs.getBoolean("is_vegetarian"));
            dto.setSeasonal(rs.getBoolean("is_seasonal"));
            dto.setRevenuePercentage(rs.getDouble("revenuePercentage"));
            return dto;
        }
    }

    private static class CategoryPerformanceRowMapper implements RowMapper<CategoryPerformanceDTO> {
        @Override
        public CategoryPerformanceDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            CategoryPerformanceDTO dto = new CategoryPerformanceDTO();
            dto.setCategoryId(rs.getLong("category_id"));
            dto.setCategoryName(rs.getString("category_name"));
            dto.setQuantitySold(rs.getInt("quantitySold"));
            dto.setTotalRevenue(rs.getLong("totalRevenue"));
            dto.setNumberOfDishes(rs.getInt("numberOfDishes"));
            dto.setRevenuePercentage(rs.getDouble("revenuePercentage"));
            dto.setTopDish(rs.getString("topDish"));
            return dto;
        }
    }

    private static class GeographicAnalyticsRowMapper implements RowMapper<GeographicAnalyticsDTO> {
        @Override
        public GeographicAnalyticsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            GeographicAnalyticsDTO dto = new GeographicAnalyticsDTO();
            dto.setCity(rs.getString("city"));
            dto.setTotalRevenue(rs.getLong("totalRevenue"));
            dto.setTotalOrders(rs.getInt("totalOrders"));
            dto.setTotalCustomers(rs.getInt("totalCustomers"));
            dto.setAverageOrderValue(rs.getDouble("averageOrderValue"));
            return dto;
        }
    }

    private static class CustomerAnalyticsRowMapper implements RowMapper<CustomerAnalyticsDTO> {
        @Override
        public CustomerAnalyticsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            CustomerAnalyticsDTO dto = new CustomerAnalyticsDTO();
            dto.setCustomerId(rs.getLong("customer_id"));
            dto.setCustomerName(rs.getString("full_name"));
            dto.setTotalOrders(rs.getInt("totalOrders"));
            dto.setTotalSpent(rs.getLong("totalSpent"));
            dto.setAverageOrderValue(rs.getDouble("averageOrderValue"));
            dto.setPaymentMethod(rs.getString("payment_method"));
            try {
                dto.setLastOrderDate(rs.getDate("lastOrderDate") != null ?
                        rs.getDate("lastOrderDate").toLocalDate().toString() : null);
            } catch (Exception e) {
                dto.setLastOrderDate(null);
            }
            return dto;
        }
    }
}
