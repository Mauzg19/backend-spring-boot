package com.Restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.RewardCatalogItem;

public interface RewardCatalogItemRepository extends JpaRepository<RewardCatalogItem, Long> {
	List<RewardCatalogItem> findByActiveTrueOrderByRequiredPointsAsc();

	Optional<RewardCatalogItem> findByCode(String code);
}
