package com.Restaurant.tools;

import com.Restaurant.model.*;
import com.Restaurant.repository.CategoryRepository;
import com.Restaurant.repository.foodRepository;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.request.CreateFoodRequest;
import com.Restaurant.service.FoodService;
import com.Restaurant.service.OrderService;
import com.Restaurant.service.RestaurantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class RestaurantTools {

    private static final Logger LOG = LoggerFactory.getLogger(RestaurantTools.class);
    private static final Set<String> VALID_ORDER_STATUS = Set.of(
            "PENDING", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED", "COMPLETED"
    );

    private final FoodService foodService;
    private final OrderService orderService;
    private final RestaurantService restaurantService;
    private final foodRepository foodRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public RestaurantTools(FoodService foodService, OrderService orderService,
                           RestaurantService restaurantService, foodRepository foodRepository,
                           OrderRepository orderRepository, UserRepository userRepository,
                           CategoryRepository categoryRepository) {
        this.foodService = foodService;
        this.orderService = orderService;
        this.restaurantService = restaurantService;
        this.foodRepository = foodRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    // ---- Tool methods ----

    @PreAuthorize("isAuthenticated()")
    @Tool(description = "Consulta el menú de un restaurante. Retorna nombre, precio, disponibilidad y categoría de cada plato.")
    public String getMenu(
            @ToolParam(description = "ID del restaurante (default=1)") Long restaurantId,
            @ToolParam(description = "Filtrar solo disponibles") Boolean availableOnly) {
        LOG.info("tool=getMenu restaurantId={}", restaurantId);
        try {
            List<Food> foods = foodRepository.findByRestaurantId(
                    restaurantId != null ? restaurantId : 1L);
            if (availableOnly != null && availableOnly) {
                foods = foods.stream().filter(Food::isAvailable).collect(Collectors.toList());
            }
            if (foods.isEmpty()) return "No hay platos disponibles.";
            StringBuilder sb = new StringBuilder("Menú:\n");
            for (Food f : foods) {
                sb.append(String.format("- #%d %s | $%d | %s | %s | %s | %s\n",
                        f.getId(), f.getName(), f.getPrice(),
                        f.isAvailable() ? "OK" : "AGOTADO",
                        f.getFoodCategory() != null ? f.getFoodCategory().getName() : "",
                        f.isVegetarian() ? "Veg" : "",
                        f.isSeasonal() ? "Temp" : ""));
            }
            return sb.toString();
        } catch (AccessDeniedException e) {
            LOG.warn("DENIED getMenu user={}", getCurrentUserEmail());
            return "DENEGADO: debes iniciar sesión para consultar el menú.";
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Tool(description = "Obtiene información de un restaurante: nombre, descripción, tipo de cocina, horario, estado.")
    public String getRestaurantInfo(
            @ToolParam(description = "ID del restaurante") Long id) {
        LOG.info("tool=getRestaurantInfo id={}", id);
        try {
            Restaurant r = restaurantService.findRestaurantById(id);
            return String.format("Restaurante #%d: %s | %s | Cocina: %s | Abierto: %s | Horario: %s",
                    r.getId(), r.getName(), r.getDescription(),
                    r.getCuisineType(), r.isOpen() ? "Sí" : "No",
                    r.getOpeningHours() != null ? r.getOpeningHours() : "N/D");
        } catch (AccessDeniedException e) {
            return "DENEGADO: no tienes acceso a esta información.";
        } catch (Exception e) {
            return "Restaurante no encontrado: " + e.getMessage();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Tool(description = "Lista todos los restaurantes disponibles en la plataforma.")
    public String listAllRestaurants() {
        LOG.info("tool=listAllRestaurants");
        try {
            List<Restaurant> restaurants = restaurantService.getAllRestaurant();
            if (restaurants.isEmpty()) return "No hay restaurantes registrados.";
            StringBuilder sb = new StringBuilder("Restaurantes disponibles:\n");
            for (Restaurant r : restaurants) {
                sb.append(String.format("- #%d %s | %s | Cocina: %s | Abierto: %s\n",
                        r.getId(), r.getName(),
                        r.getDescription() != null ? r.getDescription() : "",
                        r.getCuisineType() != null ? r.getCuisineType() : "",
                        r.isOpen() ? "Sí" : "No"));
            }
            return sb.toString();
        } catch (AccessDeniedException e) {
            return "DENEGADO: debes iniciar sesión para ver los restaurantes.";
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Tool(description = "Lista las órdenes del usuario autenticado. Permite filtrar por status.")
    public String listMyOrders(
            @ToolParam(description = "Filtrar por status (opcional): PENDING, COMPLETED, etc.") String status) {
        LOG.info("tool=listMyOrders status={}", status);
        try {
            User user = getCurrentUser();
            if (user == null) return "No se encontró el usuario autenticado.";
            List<Order> orders = orderService.getUserOrders(user.getId());
            if (status != null && !status.isBlank()) {
                String filterStatus = status.toUpperCase();
                orders = orders.stream()
                        .filter(o -> o.getOrderStatus() != null
                                && filterStatus.equalsIgnoreCase(o.getOrderStatus()))
                        .collect(Collectors.toList());
            }
            if (orders.isEmpty()) return "No tienes órdenes aún.";
            StringBuilder sb = new StringBuilder("Tus órdenes:\n");
            for (Order o : orders) {
                sb.append(String.format("- #%d | %s | $%d | %s | %s\n",
                        o.getId(), o.getOrderStatus(),
                        o.getTotalAmount(), o.getCreatedAt(),
                        o.getRestaurant() != null ? o.getRestaurant().getName() : ""));
            }
            return sb.toString();
        } catch (AccessDeniedException e) {
            return "DENEGADO: debes iniciar sesión para ver tus órdenes.";
        } catch (Exception e) {
            LOG.error("listMyOrders error", e);
            return "Error al listar órdenes: " + e.getMessage();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    @Transactional
    @Tool(description = "Crea un nuevo plato en el menú. Solo ADMIN o RESTAURANT_OWNER.")
    public String createFood(
            @ToolParam(description = "Nombre del plato") String name,
            @ToolParam(description = "Precio en centavos (ej. 25000 = $250.00)") Long price,
            @ToolParam(description = "Descripción del plato") String description,
            @ToolParam(description = "ID del restaurante") Long restaurantId,
            @ToolParam(description = "ID de la categoría") Long categoryId,
            @ToolParam(description = "¿Es vegetariano? (opcional)") Boolean vegetarian,
            @ToolParam(description = "¿Es de temporada? (opcional)") Boolean seasonal) {
        LOG.info("tool=createFood name={} restaurantId={}", name, restaurantId);
        try {
            Category cat = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
            Restaurant rest = restaurantService.findRestaurantById(restaurantId);

            CreateFoodRequest req = new CreateFoodRequest();
            req.setName(name);
            req.setPrice(price);
            req.setDescription(description != null ? description : "");
            req.setRestaurantId(restaurantId);
            req.setCategory(cat);
            req.setVegetarian(vegetarian != null && vegetarian);
            req.setSeasonal(seasonal != null && seasonal);
            req.setIngredients(new ArrayList<>());
            req.setImages(new ArrayList<>());

            Food food = foodService.createFood(req, cat, rest);
            return String.format("Plato creado. ID=%d | %s | $%d | %s | %s",
                    food.getId(), food.getName(), food.getPrice(),
                    food.getFoodCategory() != null ? food.getFoodCategory().getName() : "",
                    food.isVegetarian() ? "Vegetariano" : "Normal");
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN o RESTAURANT_OWNER pueden crear platos.";
        } catch (Exception e) {
            LOG.error("createFood error", e);
            return "Error al crear plato: " + e.getMessage();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    @Transactional
    @Tool(description = "Cambia la disponibilidad de un plato (disponible/agotado). Solo ADMIN o RESTAURANT_OWNER.")
    public String updateFoodAvailability(
            @ToolParam(description = "ID del plato") Long id,
            @ToolParam(description = "¿Disponible? (true/false)") Boolean available) {
        LOG.info("tool=updateFoodAvailability id={} available={}", id, available);
        try {
            Food food = foodService.findFoodById(id);
            food.setAvailable(available);
            foodRepository.save(food);
            return String.format("Disponibilidad actualizada. Plato #%d ahora %s",
                    food.getId(), available ? "disponible" : "no disponible");
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN o RESTAURANT_OWNER pueden modificar platos.";
        } catch (Exception e) {
            LOG.error("updateFoodAvailability error", e);
            return "Error: " + e.getMessage();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    @Transactional
    @Tool(description = "Actualiza el precio de un plato. Solo ADMIN o RESTAURANT_OWNER.")
    public String updateFoodPrice(
            @ToolParam(description = "ID del plato") Long id,
            @ToolParam(description = "Nuevo precio en centavos") Long price) {
        LOG.info("tool=updateFoodPrice id={} price={}", id, price);
        if (price == null || price < 0) {
            return "Error: el precio debe ser un número positivo.";
        }
        try {
            Food food = foodService.findFoodById(id);
            food.setPrice(price);
            foodRepository.save(food);
            return String.format("Precio actualizado. Plato #%d ahora $%d", food.getId(), price);
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN o RESTAURANT_OWNER pueden modificar precios.";
        } catch (Exception e) {
            LOG.error("updateFoodPrice error", e);
            return "Error: " + e.getMessage();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','RESTAURANT_OWNER')")
    @Transactional
    @Tool(description = "Cambia el estado de una orden. Solo ADMIN o RESTAURANT_OWNER.")
    public String updateOrderStatus(
            @ToolParam(description = "ID de la orden") Long orderId,
            @ToolParam(description = "Nuevo estado: PENDING, PREPARING, OUT_FOR_DELIVERY, DELIVERED, COMPLETED") String status) {
        LOG.info("tool=updateOrderStatus orderId={} status={}", orderId, status);
        if (status == null || !VALID_ORDER_STATUS.contains(status.toUpperCase())) {
            return "Status inválido: " + status
                    + ". Válidos: " + String.join(", ", VALID_ORDER_STATUS);
        }
        try {
            Order order = orderService.updateOrder(orderId, status.toUpperCase());
            return String.format("Orden #%d actualizada a estado: %s", order.getId(), order.getOrderStatus());
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN o RESTAURANT_OWNER pueden cambiar estados de órdenes.";
        } catch (Exception e) {
            LOG.error("updateOrderStatus error", e);
            return "Error al actualizar orden: " + e.getMessage();
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Tool(description = "Elimina un plato permanentemente. Solo ADMIN.")
    public String deleteFood(
            @ToolParam(description = "ID del plato a eliminar") Long id) {
        LOG.info("tool=deleteFood id={}", id);
        try {
            foodService.deleteFood(id);
            return String.format("Plato #%d eliminado correctamente.", id);
        } catch (AccessDeniedException e) {
            return "DENEGADO: solo ADMIN puede eliminar platos.";
        } catch (Exception e) {
            LOG.error("deleteFood error", e);
            return "Error al eliminar: " + e.getMessage();
        }
    }

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName());
    }

    private String getCurrentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anon";
    }
}
