package com.Restaurant.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.model.Events;
import com.Restaurant.response.ApiResponse;
import com.Restaurant.service.EventsService;
import com.Restaurant.service.RestaurantAccess;
import com.Restaurant.service.RestaurantService;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api")
public class EventController {
	
	@Autowired
	public EventsService eventService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private UserService userService;
	
	@PostMapping("/admin/events/restaurant/{restaurantId}")
	public ResponseEntity<Events> createEvents(@RequestBody Events event,
			@PathVariable Long restaurantId, @RequestHeader("Authorization") String jwt) throws Exception {
		RestaurantAccess.require(userService.findUserProfileByJwt(jwt), restaurantService.findRestaurantById(restaurantId));
		Events createdEvents=eventService.createEvent(event, restaurantId);
		return new ResponseEntity<>(createdEvents,HttpStatus.ACCEPTED);
	}
	
	@GetMapping("/events")
	public ResponseEntity<List<Events>> findAllEvents() throws RestaurantException{
		List<Events> events=eventService.findAllEvent();
		return new ResponseEntity<>(events,HttpStatus.ACCEPTED);
	}
	
	@GetMapping("/admin/events/restaurant/{restaurantId}")
	public ResponseEntity<List<Events>> findRestaurantsEvents(
			@PathVariable Long restaurantId, @RequestHeader("Authorization") String jwt) throws Exception {
		RestaurantAccess.require(userService.findUserProfileByJwt(jwt), restaurantService.findRestaurantById(restaurantId));
		List<Events> events=eventService.findRestaurantsEvent(restaurantId);
		return new ResponseEntity<>(events,HttpStatus.ACCEPTED);
	}
	
	@DeleteMapping("/admin/events/{id}")
	public ResponseEntity<ApiResponse> deleteEvents(
			@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
		RestaurantAccess.require(userService.findUserProfileByJwt(jwt), eventService.findById(id).getRestaurant());
		eventService.deleteEvent(id);
		ApiResponse res=new ApiResponse("Events Deleted",true);
		return new ResponseEntity<>(res,HttpStatus.ACCEPTED);
	}

}
