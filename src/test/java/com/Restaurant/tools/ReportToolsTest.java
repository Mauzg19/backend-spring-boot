package com.Restaurant.tools;

import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.service.ReportService;
import com.Restaurant.service.RestaurantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportToolsTest {

    @Mock ReportService reportService;
    @Mock RestaurantService restaurantService;
    @Mock UserRepository userRepository;

    @InjectMocks ReportTools tools;

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void authAs(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null,
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void generateSalesReport_adminWithoutRestaurantId_promptsToProvide() {
        authAs("admin@test.com", "ROLE_ADMIN");
        User admin = new User(); admin.setId(99L); admin.setEmail("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(admin);

        String out = tools.generateSalesReport(new ReportTools.GenerateSalesReportInput(
                "2026-01-01", "2026-01-31", "csv", null));
        assertThat(out).contains("ADMIN").contains("restaurantId");
    }

    @Test
    void generateSalesReport_ownerWithoutId_usesOwnerRestaurant() throws Exception {
        authAs("owner@test.com", "ROLE_RESTAURANT_OWNER");
        User owner = new User(); owner.setId(5L); owner.setEmail("owner@test.com");
        Restaurant r = new Restaurant(); r.setId(7L);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(owner);
        when(restaurantService.getRestaurantsByUserId(5L)).thenReturn(r);
        when(reportService.exportSalesCsv(any(), any(), eq(7L))).thenReturn("sales_7_123.csv");

        String out = tools.generateSalesReport(new ReportTools.GenerateSalesReportInput(
                "2026-01-01", "2026-01-31", "csv", null));
        assertThat(out).contains("sales_7_123.csv").contains("/api/reports/sales_7_123.csv");
    }

    @Test
    void generateSalesReport_adminWithExplicitId_generates() throws Exception {
        authAs("admin@test.com", "ROLE_ADMIN");
        User admin = new User(); admin.setId(1L); admin.setEmail("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(admin);
        when(reportService.exportSalesCsv(any(), any(), eq(3L))).thenReturn("sales_3_x.csv");

        String out = tools.generateSalesReport(new ReportTools.GenerateSalesReportInput(
                "2026-01-01", "2026-01-31", "csv", 3L));
        assertThat(out).contains("sales_3_x.csv");
    }

    @Test
    void generateSalesReport_invalidDateFormat_returnsError() {
        authAs("admin@test.com", "ROLE_ADMIN");
        User admin = new User(); admin.setId(1L); admin.setEmail("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(admin);

        String out = tools.generateSalesReport(new ReportTools.GenerateSalesReportInput(
                "01/01/2026", "31/01/2026", "csv", 3L));
        assertThat(out).contains("Error");
    }

    @Test
    void generateSalesReport_txtFormat_callsTxtExport() throws Exception {
        authAs("admin@test.com", "ROLE_ADMIN");
        User admin = new User(); admin.setId(1L); admin.setEmail("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(admin);
        when(reportService.exportSalesTxt(any(), any(), eq(2L))).thenReturn("sales_2_x.txt");

        String out = tools.generateSalesReport(new ReportTools.GenerateSalesReportInput(
                "2026-01-01", "2026-01-31", "TXT", 2L));
        assertThat(out).contains("sales_2_x.txt");
    }

    @Test
    void generateMenuReport_owner_usesOwnRestaurant() throws Exception {
        authAs("owner@test.com", "ROLE_RESTAURANT_OWNER");
        User owner = new User(); owner.setId(5L); owner.setEmail("owner@test.com");
        Restaurant r = new Restaurant(); r.setId(7L);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(owner);
        when(restaurantService.getRestaurantsByUserId(5L)).thenReturn(r);
        when(reportService.exportMenuCsv(7L)).thenReturn("menu_7_x.csv");

        String out = tools.generateMenuReport(null, "csv");
        assertThat(out).contains("menu_7_x.csv");
    }

    @Test
    void listAvailableReports_empty_returnsEmptyMessage() {
        authAs("user@test.com", "ROLE_CUSTOMER");
        when(reportService.listAvailableReports()).thenReturn(List.of());
        String out = tools.listAvailableReports();
        assertThat(out).contains("No hay reportes");
    }

    @Test
    void listAvailableReports_withFiles_listsAll() {
        authAs("user@test.com", "ROLE_CUSTOMER");
        when(reportService.listAvailableReports()).thenReturn(
                List.of("sales_1_a.csv", "menu_2_b.txt"));
        String out = tools.listAvailableReports();
        assertThat(out).contains("sales_1_a.csv").contains("menu_2_b.txt")
                .contains("/api/reports/");
    }

}
