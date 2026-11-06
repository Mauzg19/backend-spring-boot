package com.Restaurant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.Restaurant.model.RewardCatalogItem;
import com.Restaurant.repository.RewardCatalogItemRepository;

@Component
public class RewardCatalogInitializer implements ApplicationRunner {
	private final RewardCatalogItemRepository catalogRepository;
	private final int starterPoints;
	private final long starterDiscount;
	private final long starterMinimum;
	private final int plusPoints;
	private final long plusDiscount;
	private final long plusMinimum;

	public RewardCatalogInitializer(
			RewardCatalogItemRepository catalogRepository,
			@Value("${rewards.catalog.starter.points:100}") int starterPoints,
			@Value("${rewards.catalog.starter.discount-cop:5000}") long starterDiscount,
			@Value("${rewards.catalog.starter.minimum-order-cop:25000}") long starterMinimum,
			@Value("${rewards.catalog.plus.points:200}") int plusPoints,
			@Value("${rewards.catalog.plus.discount-cop:12000}") long plusDiscount,
			@Value("${rewards.catalog.plus.minimum-order-cop:50000}") long plusMinimum) {
		this.catalogRepository = catalogRepository;
		this.starterPoints = starterPoints;
		this.starterDiscount = starterDiscount;
		this.starterMinimum = starterMinimum;
		this.plusPoints = plusPoints;
		this.plusDiscount = plusDiscount;
		this.plusMinimum = plusMinimum;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seed("starter-discount", "Descuento $5.000", "Canjea puntos por $5.000 COP de descuento.", starterPoints,
				starterDiscount, starterMinimum);
		seed("plus-discount", "Descuento $12.000", "Canjea puntos por $12.000 COP de descuento.", plusPoints,
				plusDiscount, plusMinimum);
	}

	private void seed(String code, String name, String description, int points, long discount, long minimum) {
		if (points <= 0 || discount <= 0 || minimum <= discount) {
			throw new IllegalStateException("La configuración del catálogo de recompensas no es válida: " + code);
		}
		RewardCatalogItem item = catalogRepository.findByCode(code).orElseGet(RewardCatalogItem::new);
		item.setCode(code);
		item.setName(name);
		item.setDescription(description);
		item.setRequiredPoints(points);
		item.setDiscountAmount(discount);
		item.setMinimumOrderAmount(minimum);
		if (item.getId() == null) {
			item.setActive(true);
		}
		catalogRepository.save(item);
	}
}
