package com.Restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	public List<Category> findByRestaurantId(Long id);
}
