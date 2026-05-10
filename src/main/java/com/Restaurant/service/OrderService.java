package com.Restaurant.service;

import java.util.List;

import com.stripe.exception.StripeException;
import com.Restaurant.Exception.CartException;
import com.Restaurant.Exception.OrderException;
import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.Exception.UserException;
import com.Restaurant.model.Order;
import com.Restaurant.model.PaymentResponse;
import com.Restaurant.model.User;
import com.Restaurant.request.CreateOrderRequest;

public interface OrderService {
	
	 public PaymentResponse createOrder(CreateOrderRequest order, User user) throws UserException, RestaurantException, CartException, StripeException;
	 
	 public Order updateOrder(Long orderId, String orderStatus) throws OrderException;
	 
	 public void cancelOrder(Long orderId) throws OrderException;
	 
	 public List<Order> getUserOrders(Long userId) throws OrderException;
	 
	 public List<Order> getOrdersOfRestaurant(Long restaurantId,String orderStatus) throws OrderException, RestaurantException;
	 

}
