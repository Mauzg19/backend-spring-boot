package com.Restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Restaurant.model.Payment;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Payment> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT payment FROM Payment payment WHERE payment.id = :paymentId")
	Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);
}
