package com.Restaurant.service;

import java.util.List;

import com.Restaurant.model.Notification;
import com.Restaurant.model.Order;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;

public interface NotificationService {
	
	public Notification sendOrderStatusNotification(Order order);
	public void sendRestaurantNotification(Restaurant restaurant, String message);
	public void sendNewOrderNotification(Order order);
	public void sendPromotionalNotification(User user, String message);
	
	public List<Notification> findUsersNotification(Long userId);
	public boolean markAsRead(Long notificationId, Long userId);
	public int markAllAsRead(Long userId);
	public void registerDeviceToken(User user, String token, String platform);
	public void unregisterDeviceToken(User user, String token);

}
