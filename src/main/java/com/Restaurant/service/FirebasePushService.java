package com.Restaurant.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.Restaurant.model.Notification;
import com.Restaurant.model.NotificationDeviceToken;
import com.Restaurant.repository.NotificationDeviceTokenRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

import jakarta.annotation.PostConstruct;

@Service
public class FirebasePushService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FirebasePushService.class);

	private final NotificationDeviceTokenRepository deviceTokenRepository;
	private final String credentialsPath;
	private FirebaseMessaging messaging;

	public FirebasePushService(
			NotificationDeviceTokenRepository deviceTokenRepository,
			@Value("${GOOGLE_APPLICATION_CREDENTIALS:}") String credentialsPath) {
		this.deviceTokenRepository = deviceTokenRepository;
		this.credentialsPath = credentialsPath;
	}

	@PostConstruct
	void initialize() {
		if (!FirebaseApp.getApps().isEmpty()) {
			messaging = FirebaseMessaging.getInstance();
			return;
		}
		if (!StringUtils.hasText(credentialsPath)) {
			LOGGER.warn("Firebase push deshabilitado: configure GOOGLE_APPLICATION_CREDENTIALS.");
			return;
		}

		try (FileInputStream credentials = new FileInputStream(credentialsPath)) {
			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(credentials))
					.build();
			messaging = FirebaseMessaging.getInstance(FirebaseApp.initializeApp(options));
			LOGGER.info("Firebase push habilitado.");
		} catch (IOException exception) {
			throw new IllegalStateException(
					"No se pudo cargar el archivo de credenciales de Firebase configurado.", exception);
		}
	}

	public void send(Notification notification) {
		if (messaging == null || notification.getCustomer() == null) {
			return;
		}

		for (NotificationDeviceToken device : deviceTokenRepository.findByUserId(
				notification.getCustomer().getId())) {
			try {
				messaging.send(createMessage(notification, device.getToken()));
			} catch (FirebaseMessagingException exception) {
				LOGGER.warn("No se pudo enviar push para la notificación {} al dispositivo {}: {}",
						notification.getId(), device.getId(), exception.getMessagingErrorCode(), exception);
				if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
					deviceTokenRepository.delete(device);
				}
			}
		}
	}

	private Message createMessage(Notification notification, String token) {
		Map<String, String> data = new HashMap<>();
		data.put("notificationId", notification.getId().toString());
		data.put("type", notification.getType());
		if (notification.getOrderId() != null) {
			data.put("orderId", notification.getOrderId().toString());
		}
		return Message.builder()
				.setToken(token)
				.setNotification(com.google.firebase.messaging.Notification.builder()
						.setTitle(notification.getTitle())
						.setBody(notification.getMessage())
						.build())
				.putAllData(data)
				.build();
	}
}
