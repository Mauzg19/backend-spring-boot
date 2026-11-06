package com.Restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Restaurant.Exception.UserException;
import com.Restaurant.model.User;
import com.Restaurant.model.Address;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private UserService userService;
	private UserRepository userRepository;

	public UserController(UserService userService, UserRepository userRepository) {
		this.userService = userService;
		this.userRepository = userRepository;
	}

	@GetMapping("/profile")
	public ResponseEntity<User> getUserProfileHandler(@RequestHeader("Authorization") String jwt) throws UserException {

		User user = userService.findUserProfileByJwt(jwt);
		user.setPassword(null);

		return new ResponseEntity<>(user, HttpStatus.ACCEPTED);
	}

	@PostMapping("/address")
	public ResponseEntity<Address> saveDeliveryAddress(
			@RequestHeader("Authorization") String jwt,
			@RequestBody DeliveryAddressRequest request) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		String clientOperationId = request.clientOperationId();
		if (clientOperationId != null && !clientOperationId.isBlank()
				&& user.getAddresses() != null) {
			Address existing = user.getAddresses().stream()
					.filter(address -> clientOperationId.equals(address.getClientOperationId()))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				existing.setFullName(user.getFullName());
				existing.setStreetAddress(request.street());
				existing.setCity(request.city());
				existing.setPostalCode(request.postalCode());
				existing.setNotes(request.notes());
				userRepository.save(user);
				return ResponseEntity.ok(existing);
			}
		}
		Address address = new Address();
		address.setFullName(user.getFullName());
		address.setStreetAddress(request.street());
		address.setCity(request.city());
		address.setPostalCode(request.postalCode());
		address.setNotes(request.notes());
		address.setClientOperationId(clientOperationId);
		if (user.getAddresses() == null) {
			user.setAddresses(new java.util.ArrayList<>());
		}
		user.getAddresses().add(address);
		userRepository.save(user);
		return ResponseEntity.ok(address);
	}

	@PutMapping("/settings")
	public ResponseEntity<UserSettingsResponse> saveUserSettings(
			@RequestHeader("Authorization") String jwt,
			@RequestBody UserSettingsRequest request) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		user.setPreferredPaymentType(request.paymentType());
		user.setPreferredPaymentDetails(request.paymentDetails());
		userRepository.save(user);
		return ResponseEntity.ok(new UserSettingsResponse(
				user.getPreferredPaymentType(),
				user.getPreferredPaymentDetails()));
	}

	@GetMapping("/settings")
	public ResponseEntity<UserSettingsResponse> getUserSettings(
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		return ResponseEntity.ok(new UserSettingsResponse(
				user.getPreferredPaymentType(),
				user.getPreferredPaymentDetails()));
	}

	public record DeliveryAddressRequest(
			String street, String city, String postalCode, String notes, String clientOperationId) {}
	public record UserSettingsRequest(String paymentType, String paymentDetails) {}
	public record UserSettingsResponse(String paymentType, String paymentDetails) {}

}
