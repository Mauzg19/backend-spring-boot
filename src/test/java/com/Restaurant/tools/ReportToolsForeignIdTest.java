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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifica que mensajes de error son distintos entre ADMIN sin id y OWNER pasando id ajeno (N2).
 */
@ExtendWith(MockitoExtension.class)
class ReportToolsForeignIdTest {

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
    void owner_withForeignRestaurantId_returnsAccessDeniedMessage_notAdminPrompt() throws Exception {
        authAs("owner@test.com", "ROLE_RESTAURANT_OWNER");
        User owner = new User(); owner.setId(5L); owner.setEmail("owner@test.com");
        Restaurant own = new Restaurant(); own.setId(7L);  // OWNER restaurante=7
        when(userRepository.findByEmail("owner@test.com")).thenReturn(owner);
        when(restaurantService.getRestaurantsByUserId(5L)).thenReturn(own);

        // OWNER intenta restaurantId=99 (ajeno)
        String out = tools.generateSalesReport(new ReportTools.GenerateSalesReportInput(
                "2026-01-01", "2026-01-31", "csv", 99L));

        assertThat(out)
                .contains("DENEGADO")
                .contains("99")
                .contains("7")
                .doesNotContain("ADMIN: debes");
        verifyNoInteractions(reportService);
    }

    @Test
    void admin_noExplicitId_returnsAdminPrompt_notDenegado() {
        authAs("admin@test.com", "ROLE_ADMIN");
        User admin = new User(); admin.setId(1L); admin.setEmail("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(admin);

        String out = tools.generateSalesReport(new ReportTools.GenerateSalesReportInput(
                "2026-01-01", "2026-01-31", "csv", null));

        assertThat(out)
                .contains("ADMIN")
                .contains("restaurantId")
                .doesNotContain("DENEGADO");
        verifyNoInteractions(reportService);
    }

    @Test
    void owner_noRestaurantAssigned_returnsNoRestaurantMessage() throws Exception {
        authAs("owner@test.com", "ROLE_RESTAURANT_OWNER");
        User owner = new User(); owner.setId(5L); owner.setEmail("owner@test.com");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(owner);
        when(restaurantService.getRestaurantsByUserId(5L)).thenReturn(null);

        String out = tools.generateMenuReport(null, "csv");

        assertThat(out).contains("No tienes un restaurante asociado");
    }
}
