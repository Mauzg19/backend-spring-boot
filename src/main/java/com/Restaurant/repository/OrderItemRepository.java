package com.Restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}
