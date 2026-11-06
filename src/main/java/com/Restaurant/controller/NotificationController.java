package com.Restaurant.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Restaurant.Exception.UserException;
import com.Restaurant.dto.NotificationResponse;
import com.Restaurant.model.Notification;
import com.Restaurant.model.User;
import com.Restaurant.request.NotificationDeviceTokenRequest;
import com.Restaurant.service.NotificationService;
import com.Restaurant.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
	private final NotificationService notificationService;
	private final UserService userService;

	public NotificationController(NotificationService notificationService, UserService userService) {
		this.notificationService = notificationService;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<List<NotificationResponse>> getNotifications(
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		List<NotificationResponse> notifications = notificationService
				.findUsersNotification(user.getId())
				.stream()
				.map(NotificationController::toResponse)
				.toList();
		return ResponseEntity.ok(notifications);
	}

	@PutMapping("/{notificationId}/read")
	public ResponseEntity<Void> markAsRead(
			@PathVariable Long notificationId,
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		if (!notificationService.markAsRead(notificationId, user.getId())) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead(
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		notificationService.markAllAsRead(user.getId());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/device-token")
	public ResponseEntity<Void> registerDeviceToken(
			@Valid @RequestBody NotificationDeviceTokenRequest request,
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		notificationService.registerDeviceToken(user, request.token().trim(), request.platform().trim());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/device-token")
	public ResponseEntity<Void> unregisterDeviceToken(
			@RequestParam String token,
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		notificationService.unregisterDeviceToken(user, token);
		return ResponseEntity.noContent().build();
	}

	private static NotificationResponse toResponse(Notification notification) {
		Instant sentAt = notification.getSentAt() == null
				? Instant.EPOCH
				: notification.getSentAt().toInstant();
		return new NotificationResponse(
				notification.getId(),
				notification.getTitle() == null ? "Notificación" : notification.getTitle(),
				notification.getMessage(),
				notification.getType() == null ? "GENERAL" : notification.getType(),
				sentAt,
				notification.isReadStatus(),
				notification.getOrderId());
	}
}
