package com.Restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {


//    CartItem findByFoodIsContaining

}
