package com.Restaurant.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Restaurant.Exception.UserException;
import com.Restaurant.model.Notification;
import com.Restaurant.model.User;
import com.Restaurant.service.NotificationService;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api")
public class NotificationController {
	
	@Autowired
	private NotificationService notificationSerivce;
	@Autowired
	private UserService userService;
	
	@GetMapping("/notifications")
	public ResponseEntity<List<Notification>> findUsersNotification(
			@RequestHeader("Authorization") String jwt) throws UserException{
		User user=userService.findUserProfileByJwt(jwt);
		
		List<Notification> notifications=notificationSerivce.findUsersNotification(user.getId());
		return new ResponseEntity<List<Notification>>(notifications,HttpStatus.ACCEPTED);
	}

}
