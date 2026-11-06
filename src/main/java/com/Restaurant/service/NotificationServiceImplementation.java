package com.Restaurant.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.Restaurant.model.Notification;
import com.Restaurant.model.NotificationDeviceToken;
import com.Restaurant.model.Order;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.NotificationDeviceTokenRepository;
import com.Restaurant.repository.NotificationRepository;

@Service
public class NotificationServiceImplementation implements NotificationService {
	private static final Logger LOGGER =
			LoggerFactory.getLogger(NotificationServiceImplementation.class);

	private final NotificationRepository notificationRepository;
	private final NotificationDeviceTokenRepository deviceTokenRepository;
	private final FirebasePushService firebasePushService;

	public NotificationServiceImplementation(
			NotificationRepository notificationRepository,
			NotificationDeviceTokenRepository deviceTokenRepository,
			FirebasePushService firebasePushService) {
		this.notificationRepository = notificationRepository;
		this.deviceTokenRepository = deviceTokenRepository;
		this.firebasePushService = firebasePushService;
	}

	@Override
	@Transactional
	public Notification sendOrderStatusNotification(Order order) {
		String type = "ORDER_STATUS_" + order.getOrderStatus();
		User customer = requireRecipient(order.getCustomer());
		if (notificationRepository.existsByCustomerIdAndOrderIdAndType(
				customer.getId(), order.getId(), type)) {
			return notificationRepository.findByCustomerIdAndOrderIdAndType(
							customer.getId(), order.getId(), type)
					.orElseThrow(() -> new IllegalStateException("El aviso duplicado no se pudo recuperar."));
		}

		Notification notification = newNotification(
				customer,
				"Actualización de tu pedido",
				"Tu pedido #" + order.getId() + " ahora está: "
						+ orderStatusLabel(order.getOrderStatus()) + ".",
				type,
				order.getId());
		notification.setRestaurant(order.getRestaurant());
		return saveAndPush(notification);
	}

	@Override
	@Transactional
	public void sendRestaurantNotification(Restaurant restaurant, String message) {
		if (restaurant == null || restaurant.getOwner() == null) {
			throw new IllegalArgumentException("El restaurante no tiene un usuario destinatario.");
		}
		saveAndPush(newNotification(
				restaurant.getOwner(),
				"Notificación del restaurante",
				message,
				"RESTAURANT",
				null));
	}

	@Override
	@Transactional
	public void sendNewOrderNotification(Order order) {
		Restaurant restaurant = order.getRestaurant();
		if (restaurant == null || restaurant.getOwner() == null) {
			LOGGER.error("No se pudo notificar el pedido pagado {}: no tiene propietario asociado.",
					order.getId());
			return;
		}
		if (notificationRepository.existsByCustomerIdAndOrderIdAndType(
				restaurant.getOwner().getId(), order.getId(), "NEW_ORDER")) {
			return;
		}

		saveAndPush(newNotification(
				restaurant.getOwner(),
				"Nuevo pedido pagado",
				"El pedido #" + order.getId() + " está listo para ser atendido.",
				"NEW_ORDER",
				order.getId()));
	}

	@Override
	@Transactional
	public void sendPromotionalNotification(User user, String message) {
		saveAndPush(newNotification(
				requireRecipient(user),
				"Promoción",
				message,
				"PROMOTION",
				null));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> findUsersNotification(Long userId) {
		return notificationRepository.findByCustomerIdOrderBySentAtDesc(userId);
	}

	@Override
	@Transactional
	public boolean markAsRead(Long notificationId, Long userId) {
		Optional<Notification> notification =
				notificationRepository.findByIdAndCustomerId(notificationId, userId);
		if (notification.isEmpty()) {
			return false;
		}
		notification.get().setReadStatus(true);
		notificationRepository.save(notification.get());
		return true;
	}

	@Override
	@Transactional
	public int markAllAsRead(Long userId) {
		List<Notification> unread = notificationRepository.findByCustomerIdAndReadStatusFalse(userId);
		unread.forEach(notification -> notification.setReadStatus(true));
		notificationRepository.saveAll(unread);
		return unread.size();
	}

	@Override
	@Transactional
	public void registerDeviceToken(User user, String token, String platform) {
		User recipient = requireRecipient(user);
		NotificationDeviceToken deviceToken = deviceTokenRepository.findByToken(token)
				.orElseGet(NotificationDeviceToken::new);
		deviceToken.setUser(recipient);
		deviceToken.setToken(token);
		deviceToken.setPlatform(platform);
		deviceToken.setUpdatedAt(new Date());
		deviceTokenRepository.save(deviceToken);
	}

	@Override
	@Transactional
	public void unregisterDeviceToken(User user, String token) {
		deviceTokenRepository.deleteByTokenAndUserId(token, requireRecipient(user).getId());
	}

	private Notification newNotification(
			User recipient,
			String title,
			String message,
			String type,
			Long orderId) {
		Notification notification = new Notification();
		notification.setCustomer(requireRecipient(recipient));
		notification.setTitle(title);
		notification.setMessage(message);
		notification.setType(type);
		notification.setOrderId(orderId);
		notification.setSentAt(new Date());
		notification.setReadStatus(false);
		return notification;
	}

	private Notification saveAndPush(Notification notification) {
		Notification saved = notificationRepository.save(notification);
		Runnable sendPush = () -> {
			try {
				firebasePushService.send(saved);
			} catch (RuntimeException exception) {
				LOGGER.error("La notificación {} quedó guardada, pero no se pudo enviar el push.",
						saved.getId(), exception);
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					sendPush.run();
				}
			});
		} else {
			sendPush.run();
		}
		return saved;
	}

	private User requireRecipient(User user) {
		if (user == null || user.getId() == null) {
			throw new IllegalArgumentException("La notificación requiere un usuario destinatario guardado.");
		}
		return user;
	}

	private String orderStatusLabel(String status) {
		return switch (status) {
			case "PENDING" -> "pendiente";
			case "PREPARING" -> "en preparación";
			case "READY" -> "listo para recoger";
			case "OUT_FOR_DELIVERY" -> "en camino";
			case "DELIVERED", "COMPLETED" -> "entregado";
			default -> status.toLowerCase();
		};
	}
}
