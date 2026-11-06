package com.Restaurant.config;

import com.Restaurant.controller.ProductImageStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProductImageWebConfig implements WebMvcConfigurer {
	private final ProductImageStorage productImageStorage;

	public ProductImageWebConfig(ProductImageStorage productImageStorage) {
		this.productImageStorage = productImageStorage;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/product-images/**")
				.addResourceLocations(productImageStorage.getImageDirectoryLocation());
	}
}
