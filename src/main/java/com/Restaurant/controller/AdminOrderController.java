package com.Restaurant.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.model.Order;
import com.Restaurant.service.OrderService;
import com.Restaurant.service.UserService;
import com.Restaurant.Exception.UserException;
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.model.Restaurant;
import com.Restaurant.repository.RestaurantRepository;
import com.Restaurant.model.User;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private UserService userService;

	@Autowired
	private RestaurantRepository restaurantRepository;
	
	
    @DeleteMapping("/order/{orderId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteOrder(@PathVariable Long orderId) throws OrderException{
    	if(orderId!=null) {
    		orderService.cancelOrder(orderId);
    	return ResponseEntity.ok("Order deleted with id)"+orderId);
    }else return new ResponseEntity<String>(HttpStatus.BAD_REQUEST) ;
    }
    
    
    @GetMapping("/order/restaurant/{restaurantId}")
    public ResponseEntity<List<Order>> getAllRestaurantOrders(
    		@PathVariable Long restaurantId,
    		@RequestParam(required = false) String order_status,
    		@RequestHeader("Authorization") String jwt)
			throws OrderException, RestaurantException, UserException {
		User actor = userService.findUserProfileByJwt(jwt);
		if (!canAccessRestaurant(actor, restaurantId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
    	
    		List<Order> orders = orderService.
    				getOrdersOfRestaurant(restaurantId,order_status);
    		
//    		System.out.println("ORDER STATUS----- "+orderStatus);
    		return ResponseEntity.ok(orders);
    		
    	
    	
    }
    
    @PutMapping("/orders/{orderId}/{orderStatus}")
    public ResponseEntity<Order> updateOrders(
    		@PathVariable Long orderId,
    		@PathVariable String orderStatus,
    		@RequestHeader("Authorization") String jwt) throws OrderException, RestaurantException, UserException{
		User actor = userService.findUserProfileByJwt(jwt);
    	
    		Order orders = orderService.updateOrder(orderId, orderStatus, actor);
    		return ResponseEntity.ok(orders);
    		
    }

	private boolean canAccessRestaurant(User actor, Long restaurantId) {
		if (actor.getRole() == USER_ROLE.ROLE_ADMIN) {
			return true;
		}
		if (actor.getRole() != USER_ROLE.ROLE_RESTAURANT_OWNER) {
			return false;
		}
		Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);
		return restaurant != null
				&& restaurant.getOwner() != null
				&& actor.getId().equals(restaurant.getOwner().getId());
	}

}
