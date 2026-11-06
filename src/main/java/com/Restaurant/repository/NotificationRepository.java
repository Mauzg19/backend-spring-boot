package com.Restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByCustomerIdOrderBySentAtDesc(Long userId);

	Optional<Notification> findByIdAndCustomerId(Long id, Long userId);

	Optional<Notification> findByCustomerIdAndOrderIdAndType(
			Long userId, Long orderId, String type);

	boolean existsByCustomerIdAndOrderIdAndType(Long userId, Long orderId, String type);

	List<Notification> findByCustomerIdAndReadStatusFalse(Long userId);
}
