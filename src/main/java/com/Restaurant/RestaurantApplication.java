package com.Restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
public class RestaurantApplication {

	public static void main(String[] args) {
		// Asegurar que el contexto de seguridad se herede a hilos secundarios (como los que usa Spring AI)
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
		SpringApplication.run(RestaurantApplication.class, args);
	}

}
