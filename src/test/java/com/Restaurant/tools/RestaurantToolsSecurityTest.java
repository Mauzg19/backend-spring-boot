package com.Restaurant.tools;

import com.Restaurant.model.Food;
import com.Restaurant.repository.CategoryRepository;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.repository.foodRepository;
import com.Restaurant.service.FoodService;
import com.Restaurant.service.OrderService;
import com.Restaurant.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifica que @PreAuthorize en RestaurantTools bloquea por rol.
 * Levanta sólo Spring Security (mínimo) + RestaurantTools.
 */
@SpringJUnitConfig
@ContextConfiguration(classes = RestaurantToolsSecurityTest.TestConfig.class)
@Import(RestaurantTools.class)
class RestaurantToolsSecurityTest {

    @EnableMethodSecurity(prePostEnabled = true)
    @TestConfiguration
    static class TestConfig {
        // RestaurantTools tiene @Transactional → stub mínimo de TransactionManager
        @Bean org.springframework.transaction.PlatformTransactionManager txManager() {
            return new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(Object o, org.springframework.transaction.TransactionDefinition d) {}
                @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus s) {}
                @Override protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus s) {}
            };
        }
        @Bean FoodService foodService() { return mock(FoodService.class); }
        @Bean OrderService orderService() { return mock(OrderService.class); }
        @Bean RestaurantService restaurantService() { return mock(RestaurantService.class); }
        @Bean foodRepository foodRepo() { return mock(foodRepository.class); }
        @Bean OrderRepository orderRepo() { return mock(OrderRepository.class); }
        @Bean UserRepository userRepo() { return mock(UserRepository.class); }
        @Bean CategoryRepository categoryRepo() { return mock(CategoryRepository.class); }
    }

    @Autowired RestaurantTools tools;
    @Autowired FoodService foodService;

    // ----- CUSTOMER -----

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_createFood_denied() {
        assertThatThrownBy(() -> tools.createFood(
                "X", 1L, "d", 1L, 1L, false, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_updateFoodPrice_denied() {
        assertThatThrownBy(() -> tools.updateFoodPrice(
                1L, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_deleteFood_denied() {
        assertThatThrownBy(() -> tools.deleteFood(
                1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_updateOrderStatus_denied() {
        assertThatThrownBy(() -> tools.updateOrderStatus(
                1L, "DELIVERED"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ----- RESTAURANT_OWNER -----

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void owner_deleteFood_denied() {
        assertThatThrownBy(() -> tools.deleteFood(
                1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void owner_updateFoodPrice_allowed() throws Exception {
        Food f = new Food(); f.setId(1L);
        when(foodService.findFoodById(1L)).thenReturn(f);
        String out = tools.updateFoodPrice(1L, 5000L);
        assertThat(out).contains("Precio actualizado");
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void owner_updateFoodAvailability_allowed() throws Exception {
        Food f = new Food(); f.setId(1L);
        when(foodService.findFoodById(1L)).thenReturn(f);
        String out = tools.updateFoodAvailability(
                1L, false);
        assertThat(out).contains("no disponible");
    }

    // ----- ADMIN -----

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_deleteFood_allowed() throws Exception {
        doNothing().when(foodService).deleteFood(any());
        String out = tools.deleteFood(5L);
        assertThat(out).contains("eliminado").contains("5");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_updateOrderStatus_allowed_butValidatesEnum() {
        String out = tools.updateOrderStatus(
                1L, "INVALID_STATUS");
        assertThat(out).contains("inválido");
    }

    // ----- Sin auth -----

    @Test
    @WithAnonymousUser
    void anonymous_listMyOrders_denied() {
        assertThatThrownBy(() -> tools.listMyOrders(
                null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithAnonymousUser
    void anonymous_getMenu_denied() {
        assertThatThrownBy(() -> tools.getMenu(
                1L, null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
