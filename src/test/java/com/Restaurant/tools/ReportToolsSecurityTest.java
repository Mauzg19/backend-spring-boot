package com.Restaurant.tools;

import com.Restaurant.repository.UserRepository;
import com.Restaurant.service.ReportService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig
@ContextConfiguration(classes = ReportToolsSecurityTest.TestConfig.class)
@Import(ReportTools.class)
class ReportToolsSecurityTest {

    @EnableMethodSecurity(prePostEnabled = true)
    @TestConfiguration
    static class TestConfig {
        @Bean ReportService reportService() { return mock(ReportService.class); }
        @Bean RestaurantService restaurantService() { return mock(RestaurantService.class); }
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
    }

    @Autowired ReportTools tools;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_generateSalesReport_denied() {
        assertThatThrownBy(() -> tools.generateSalesReport(
                "2026-01-01", "2026-01-31", "csv", 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_generateMenuReport_denied() {
        assertThatThrownBy(() -> tools.generateMenuReport(
                1L, "csv"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithAnonymousUser
    void anonymous_listAvailableReports_denied() {
        assertThatThrownBy(() -> tools.listAvailableReports(
                ))
                .isInstanceOf(AccessDeniedException.class);
    }
}
