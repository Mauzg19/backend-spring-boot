package com.Restaurant.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class AppConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider) throws Exception {

        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(Authorize -> Authorize
                		.requestMatchers("/api/payments/stripe/webhook", "/api/payments/stripe/return").permitAll()
                		.requestMatchers("/api/admin/**").hasAnyRole("RESTAURANT_OWNER","ADMIN")
                        .requestMatchers("/api/customers", "/api/pending-customers", "/api/analytics/**").hasRole("ADMIN")
                		.requestMatchers(HttpMethod.GET, "/api/food", "/api/food/**",
                				"/api/restaurants", "/api/restaurants/**").permitAll()
                                .requestMatchers("/api/chat").authenticated()
                                .requestMatchers("/api/reports/**").authenticated()
                                .requestMatchers("/mcp/**").authenticated()
                                .requestMatchers("/api/**").authenticated()

                                .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtTokenValidator(jwtProvider), BasicAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));


		return http.build();

	}

    // CORS Configuration
    private CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOriginPatterns(Arrays.asList(
                    "http://localhost:[*]",
                    "http://127.0.0.1:[*]",
                    "http://10.*.*.*:[*]",
                    "http://192.168.*.*:[*]",
                    "http://172.*.*.*:[*]",
                    "https://proyecto-restaurante-six.vercel.app"
                ));
                cfg.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                cfg.setAllowCredentials(true);
                cfg.setAllowedHeaders(Arrays.asList("*"));
                cfg.setExposedHeaders(Arrays.asList("Authorization"));
                cfg.setMaxAge(3600L);
                return cfg;
            }
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
