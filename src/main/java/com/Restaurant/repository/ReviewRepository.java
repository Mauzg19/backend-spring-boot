package com.Restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

}
