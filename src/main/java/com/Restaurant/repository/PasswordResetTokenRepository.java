package com.Restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
	PasswordResetToken findByToken(String token);
}
