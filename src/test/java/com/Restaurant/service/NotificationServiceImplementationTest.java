package com.Restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.Restaurant.model.Notification;
import com.Restaurant.model.Order;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.NotificationDeviceTokenRepository;
import com.Restaurant.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplementationTest {

	@Mock private NotificationRepository notificationRepository;
	@Mock private NotificationDeviceTokenRepository deviceTokenRepository;
	@Mock private FirebasePushService firebasePushService;
	@InjectMocks private NotificationServiceImplementation notificationService;

	@Test
	void sendsOrderStatusNotificationToCustomerAndPersistsIt() {
		User customer = user(11L);
		Order order = new Order();
		order.setId(42L);
		order.setCustomer(customer);
		order.setOrderStatus("READY");
		order.setRestaurant(new Restaurant());
		when(notificationRepository.existsByCustomerIdAndOrderIdAndType(
				11L, 42L, "ORDER_STATUS_READY")).thenReturn(false);
		when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
			Notification notification = invocation.getArgument(0);
			notification.setId(7L);
			return notification;
		});

		Notification saved = notificationService.sendOrderStatusNotification(order);

		assertEquals("Actualización de tu pedido", saved.getTitle());
		assertEquals("Tu pedido #42 ahora está: listo para recoger.", saved.getMessage());
		assertEquals("ORDER_STATUS_READY", saved.getType());
		assertEquals(42L, saved.getOrderId());
		assertEquals(11L, saved.getCustomer().getId());
		assertFalse(saved.isReadStatus());
		assertTrue(saved.getSentAt() instanceof Date);
		verify(firebasePushService).send(saved);
	}

	@Test
	void sendsNewPaidOrderNotificationToRestaurantOwnerOnce() {
		User owner = user(12L);
		Restaurant restaurant = new Restaurant();
		restaurant.setOwner(owner);
		Order order = new Order();
		order.setId(43L);
		order.setRestaurant(restaurant);
		when(notificationRepository.existsByCustomerIdAndOrderIdAndType(
				12L, 43L, "NEW_ORDER")).thenReturn(true);

		notificationService.sendNewOrderNotification(order);

		verify(notificationRepository, never()).save(any(Notification.class));
		verify(firebasePushService, never()).send(any(Notification.class));
	}

	@Test
	void sendsNewPaidOrderNotificationToRestaurantOwner() {
		User owner = user(12L);
		Restaurant restaurant = new Restaurant();
		restaurant.setOwner(owner);
		Order order = new Order();
		order.setId(43L);
		order.setRestaurant(restaurant);
		when(notificationRepository.existsByCustomerIdAndOrderIdAndType(
				12L, 43L, "NEW_ORDER")).thenReturn(false);
		when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
			Notification notification = invocation.getArgument(0);
			notification.setId(8L);
			return notification;
		});

		notificationService.sendNewOrderNotification(order);

		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(saved.capture());
		assertEquals(12L, saved.getValue().getCustomer().getId());
		assertEquals("NEW_ORDER", saved.getValue().getType());
		verify(firebasePushService).send(saved.getValue());
	}

	@Test
	void sendsPushOnlyAfterTheNotificationTransactionCommits() {
		User owner = user(12L);
		Restaurant restaurant = new Restaurant();
		restaurant.setOwner(owner);
		Notification saved = new Notification();
		saved.setId(8L);
		when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

		TransactionSynchronizationManager.initSynchronization();
		try {
			notificationService.sendRestaurantNotification(restaurant, "Pedido actualizado");

			verify(firebasePushService, never()).send(any(Notification.class));
			for (TransactionSynchronization synchronization
					: TransactionSynchronizationManager.getSynchronizations()) {
				synchronization.afterCommit();
			}
			verify(firebasePushService).send(saved);
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void missingRestaurantOwnerDoesNotInterruptThePaidOrderFlow() {
		Order order = new Order();
		order.setId(43L);
		order.setRestaurant(new Restaurant());

		assertDoesNotThrow(() -> notificationService.sendNewOrderNotification(order));

		verify(notificationRepository, never()).save(any(Notification.class));
		verify(firebasePushService, never()).send(any(Notification.class));
	}

	@Test
	void onlyMarksNotificationsOwnedByTheCurrentUserAsRead() {
		when(notificationRepository.findByIdAndCustomerId(4L, 11L)).thenReturn(Optional.empty());

		assertFalse(notificationService.markAsRead(4L, 11L));

		verify(notificationRepository, never()).save(any(Notification.class));
	}

	@Test
	void marksAllUnreadNotificationsAsReadForTheCurrentUser() {
		Notification notification = new Notification();
		notification.setReadStatus(false);
		when(notificationRepository.findByCustomerIdAndReadStatusFalse(11L))
				.thenReturn(List.of(notification));
		int updatedCount = notificationService.markAllAsRead(11L);

		assertEquals(1, updatedCount);
		assertTrue(notification.isReadStatus());
		verify(notificationRepository).saveAll(anyList());
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		return user;
	}
}
