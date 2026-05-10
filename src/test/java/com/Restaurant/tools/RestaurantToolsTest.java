package com.Restaurant.tools;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.model.Category;
import com.Restaurant.model.Food;
import com.Restaurant.model.Order;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.CategoryRepository;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.repository.foodRepository;
import com.Restaurant.service.FoodService;
import com.Restaurant.service.OrderService;
import com.Restaurant.service.RestaurantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantToolsTest {

    @Mock FoodService foodService;
    @Mock OrderService orderService;
    @Mock RestaurantService restaurantService;
    @Mock foodRepository foodRepository;
    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;

    @InjectMocks RestaurantTools tools;

    @BeforeEach
    void auth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "owner@test.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT_OWNER"))));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void getMenu_emptyRestaurant_returnsNoItemsMessage() {
        when(foodRepository.findByRestaurantId(1L)).thenReturn(List.of());
        String out = tools.getMenu(1L, null);
        assertThat(out).contains("No hay platos");
    }

    @Test
    void getMenu_withItems_formatsLines() {
        Food f = new Food();
        f.setId(10L); f.setName("Pizza"); f.setPrice(25000L); f.setAvailable(true);
        when(foodRepository.findByRestaurantId(1L)).thenReturn(List.of(f));
        String out = tools.getMenu(1L, null);
        assertThat(out).contains("Pizza").contains("25000").contains("OK");
    }

    @Test
    void getMenu_availableOnlyTrue_filtersOut() {
        Food f1 = new Food(); f1.setId(1L); f1.setName("A"); f1.setPrice(1L); f1.setAvailable(true);
        Food f2 = new Food(); f2.setId(2L); f2.setName("B"); f2.setPrice(2L); f2.setAvailable(false);
        when(foodRepository.findByRestaurantId(1L)).thenReturn(List.of(f1, f2));
        String out = tools.getMenu(1L, true);
        assertThat(out).contains("A").doesNotContain("B |");
    }

    @Test
    void createFood_categoryNotFound_returnsError() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        String out = tools.createFood(new RestaurantTools.CreateFoodInput(
                "Pizza", 1L, "desc", 1L, 99L, false, false));
        assertThat(out).contains("Error").contains("Categoría");
    }

    @Test
    void createFood_happyPath_returnsCreatedMessage() throws Exception {
        Category cat = new Category(); cat.setId(1L); cat.setName("Pizza");
        Restaurant rest = new Restaurant(); rest.setId(1L);
        Food food = new Food(); food.setId(42L); food.setName("Hawaiana");
        food.setPrice(30000L); food.setFoodCategory(cat);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(restaurantService.findRestaurantById(1L)).thenReturn(rest);
        when(foodService.createFood(any(), any(), any())).thenReturn(food);
        String out = tools.createFood(new RestaurantTools.CreateFoodInput(
                "Hawaiana", 30000L, "Piña", 1L, 1L, false, false));
        assertThat(out).contains("Plato creado").contains("42").contains("Hawaiana");
    }

    @Test
    void updateFoodPrice_negative_rejects() {
        String out = tools.updateFoodPrice(1L, -10L);
        assertThat(out).contains("precio").contains("positivo");
        verifyNoInteractions(foodService);
    }

    @Test
    void updateFoodPrice_null_rejects() {
        String out = tools.updateFoodPrice(1L, null);
        assertThat(out).contains("precio");
        verifyNoInteractions(foodService);
    }

    @Test
    void updateFoodPrice_valid_updates() throws Exception {
        Food f = new Food(); f.setId(1L); f.setPrice(100L);
        when(foodService.findFoodById(1L)).thenReturn(f);
        when(foodRepository.save(any())).thenReturn(f);
        String out = tools.updateFoodPrice(1L, 5000L);
        assertThat(out).contains("Precio actualizado").contains("5000");
    }

    @Test
    void updateOrderStatus_invalidStatus_rejects() {
        String out = tools.updateOrderStatus(
                1L, "DELIVERED_NOW");
        assertThat(out).contains("inválido").contains("PENDING");
        verifyNoInteractions(orderService);
    }

    @Test
    void updateOrderStatus_caseInsensitive_accepts() throws Exception {
        Order o = new Order(); o.setId(1L); o.setOrderStatus("DELIVERED");
        when(orderService.updateOrder(1L, "DELIVERED")).thenReturn(o);
        String out = tools.updateOrderStatus(
                1L, "delivered");
        assertThat(out).contains("actualizada").contains("DELIVERED");
    }

    @Test
    void listMyOrders_filterStatus_skipsNullStatusOrders() throws Exception {
        User u = new User(); u.setId(1L); u.setEmail("owner@test.com");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(u);
        Order o1 = new Order(); o1.setId(1L); o1.setOrderStatus(null);
        o1.setTotalAmount(100L); o1.setCreatedAt(new Date());
        Order o2 = new Order(); o2.setId(2L); o2.setOrderStatus("DELIVERED");
        o2.setTotalAmount(200L); o2.setCreatedAt(new Date());
        when(orderService.getUserOrders(1L)).thenReturn(List.of(o1, o2));
        String out = tools.listMyOrders("DELIVERED");
        assertThat(out).contains("#2").doesNotContain("#1");
    }

    @Test
    void listMyOrders_noUser_returnsErrorMessage() {
        SecurityContextHolder.clearContext();
        String out = tools.listMyOrders(null);
        assertThat(out).containsAnyOf("usuario", "iniciar sesión");
    }

    @Test
    void listMyOrders_orderServiceFails_catchesException() throws Exception {
        User u = new User(); u.setId(1L); u.setEmail("owner@test.com");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(u);
        when(orderService.getUserOrders(1L)).thenThrow(new OrderException("DB down"));
        String out = tools.listMyOrders(null);
        assertThat(out).contains("Error");
    }

    @Test
    void deleteFood_happyPath() throws Exception {
        doNothing().when(foodService).deleteFood(5L);
        String out = tools.deleteFood(5L);
        assertThat(out).contains("eliminado").contains("5");
        verify(foodService).deleteFood(5L);
    }
}
