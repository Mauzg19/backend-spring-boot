package com.Restaurant.service;

import com.Restaurant.model.Category;
import com.Restaurant.model.Food;
import com.Restaurant.model.Order;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.foodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplementationTest {

    @Mock OrderRepository orderRepository;
    @Mock foodRepository foodRepository;

    @TempDir Path tmp;

    ReportServiceImplementation service;

    @BeforeEach
    void setUp() throws IOException {
        service = new ReportServiceImplementation(tmp.toString(), orderRepository, foodRepository);
    }

    private Order order(Long id, Long restId, Long total, String status) {
        Order o = new Order();
        o.setId(id);
        Restaurant r = new Restaurant(); r.setId(restId); o.setRestaurant(r);
        User u = new User(); u.setEmail("c@test.com"); o.setCustomer(u);
        o.setTotalAmount(total);
        o.setOrderStatus(status);
        o.setCreatedAt(new Date());
        o.setTotalItem(2);
        return o;
    }

    @Test
    void exportSalesCsv_writesHeaderAndRows() throws IOException {
        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(
                order(1L, 5L, 1000L, "DELIVERED"),
                order(2L, 5L, 2000L, "PENDING"),
                order(3L, 99L, 500L, "DELIVERED")  // distinto restaurante - debe filtrarse
        ));

        String fileName = service.exportSalesCsv(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), 5L);

        Path file = tmp.resolve(fileName);
        assertThat(Files.exists(file)).isTrue();
        String content = Files.readString(file);
        assertThat(content)
                .contains("\"id\"").contains("\"customer\"").contains("\"total\"")
                .contains("\"1\"").contains("\"2\"")
                .doesNotContain("\"3\"");  // filtrado
        assertThat(fileName).startsWith("sales_5_").endsWith(".csv");
    }

    @Test
    void exportSalesCsv_emptyOrders_writesHeaderOnly() throws IOException {
        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());
        String fileName = service.exportSalesCsv(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), 1L);
        String content = Files.readString(tmp.resolve(fileName));
        assertThat(content.lines().count()).isEqualTo(1L);  // solo header
    }

    @Test
    void exportSalesTxt_writesHeaderAndOrderLines() throws IOException {
        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(
                order(10L, 1L, 5000L, "DELIVERED")));
        String fileName = service.exportSalesTxt(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"), 1L);
        String content = Files.readString(tmp.resolve(fileName));
        assertThat(content)
                .contains("REPORTE DE VENTAS")
                .contains("Order #10")
                .contains("5000")
                .contains("DELIVERED");
        assertThat(fileName).endsWith(".txt");
    }

    @Test
    void exportMenuCsv_writesAllFoods() throws IOException {
        Category cat = new Category(); cat.setName("Pizzas");
        Food f1 = new Food(); f1.setId(1L); f1.setName("Margherita"); f1.setPrice(20000L);
        f1.setAvailable(true); f1.setVegetarian(true); f1.setSeasonal(false); f1.setFoodCategory(cat);
        Food f2 = new Food(); f2.setId(2L); f2.setName("Pepperoni"); f2.setPrice(25000L);
        f2.setAvailable(false); f2.setVegetarian(false); f2.setSeasonal(false); f2.setFoodCategory(cat);
        when(foodRepository.findByRestaurantId(1L)).thenReturn(List.of(f1, f2));

        String fileName = service.exportMenuCsv(1L);
        String content = Files.readString(tmp.resolve(fileName));
        assertThat(content)
                .contains("Margherita").contains("Pepperoni")
                .contains("Pizzas").contains("\"true\"").contains("\"false\"");
    }

    @Test
    void exportMenuTxt_humanReadable() throws IOException {
        Food f = new Food(); f.setId(1L); f.setName("Sushi"); f.setPrice(45000L);
        f.setAvailable(true);
        when(foodRepository.findByRestaurantId(2L)).thenReturn(List.of(f));

        String fileName = service.exportMenuTxt(2L);
        String content = Files.readString(tmp.resolve(fileName));
        assertThat(content)
                .contains("REPORTE DE MENÚ")
                .contains("Sushi")
                .contains("Disponible");
    }

    @Test
    void listAvailableReports_returnsFilesInDir() throws IOException {
        Files.writeString(tmp.resolve("sales_1_a.csv"), "x");
        Files.writeString(tmp.resolve("menu_2_b.txt"), "y");
        List<String> files = service.listAvailableReports();
        assertThat(files).contains("sales_1_a.csv", "menu_2_b.txt");
    }

    @Test
    void listAvailableReports_emptyDir_returnsEmpty() {
        assertThat(service.listAvailableReports()).isEmpty();
    }
}
