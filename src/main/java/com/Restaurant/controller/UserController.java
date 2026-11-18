package com.Restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
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
import com.Restaurant.repository.AddressRepository;
import com.Restaurant.repository.OrderRepository;
import com.Restaurant.repository.RestaurantRepository;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private UserService userService;
	private UserRepository userRepository;
	private final AddressRepository addressRepository;
	private final OrderRepository orderRepository;
	private final RestaurantRepository restaurantRepository;

	public UserController(UserService userService, UserRepository userRepository,
			AddressRepository addressRepository, OrderRepository orderRepository, RestaurantRepository restaurantRepository) {
		this.userService = userService;
		this.userRepository = userRepository;
		this.addressRepository = addressRepository;
		this.orderRepository = orderRepository;
		this.restaurantRepository = restaurantRepository;
	}

	@GetMapping("/profile")
	public ResponseEntity<User> getUserProfileHandler(@RequestHeader("Authorization") String jwt) throws UserException {

		User user = userService.findUserProfileByJwt(jwt);
		user.setPassword(null);

		return new ResponseEntity<>(user, HttpStatus.ACCEPTED);
	}

	@PostMapping("/address")
	@Transactional
	public ResponseEntity<Address> saveDeliveryAddress(
			@RequestHeader("Authorization") String jwt,
			@RequestBody DeliveryAddressRequest request) throws UserException {
		validateAddress(request);
		User user = lockUser(jwt);
		String clientOperationId = request.clientOperationId();
		if (clientOperationId != null && !clientOperationId.isBlank()
				&& user.getAddresses() != null) {
			Address existing = user.getAddresses().stream()
					.filter(address -> clientOperationId.equals(address.getClientOperationId()))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				return ResponseEntity.ok(existing);
			}
		}
		Address address = new Address();
		applyAddress(address, user, request);
		address.setClientOperationId(StringUtils.hasText(clientOperationId) ? clientOperationId : null);
		user.getAddresses().add(address);
		userRepository.saveAndFlush(user);
		return ResponseEntity.ok(address);
	}

	@PutMapping("/address/{id}")
	@Transactional
	public Address updateAddress(@PathVariable Long id, @RequestHeader("Authorization") String jwt,
			@RequestBody DeliveryAddressRequest request) throws UserException {
		validateAddress(request);
		User user = lockUser(jwt);
		Address address = findAddress(user, id);
		if (usedOutsideProfile(id)) {
			// Older checkouts shared the address with the profile. Preserve their snapshot.
			Address edited = new Address();
			edited.setState(address.getState());
			edited.setCountry(address.getCountry());
			user.getAddresses().set(user.getAddresses().indexOf(address), edited);
			address = edited;
		}
		applyAddress(address, user, request);
		userRepository.saveAndFlush(user);
		return address;
	}

	@DeleteMapping("/address/{id}")
	@Transactional
	public ResponseEntity<Void> deleteAddress(@PathVariable Long id, @RequestHeader("Authorization") String jwt)
			throws UserException {
		User user = lockUser(jwt);
		Address address = findAddress(user, id);
		user.getAddresses().remove(address);
		userRepository.flush();
		if (!usedOutsideProfile(id)) addressRepository.delete(address);
		return ResponseEntity.noContent().build();
	}

	private boolean usedOutsideProfile(Long addressId) {
		return orderRepository.existsByDeliveryAddressId(addressId) || restaurantRepository.existsByAddressId(addressId);
	}

	private User lockUser(String jwt) throws UserException {
		Long id = userService.findUserProfileByJwt(jwt).getId();
		return userRepository.findByIdForUpdate(id).orElseThrow(() -> new UserException("Usuario no encontrado."));
	}

	private Address findAddress(User user, Long id) {
		return user.getAddresses().stream().filter(address -> id.equals(address.getId())).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dirección no encontrada."));
	}

	private void validateAddress(DeliveryAddressRequest request) {
		if (!StringUtils.hasText(request.street()) || !StringUtils.hasText(request.city())
				|| !StringUtils.hasText(request.postalCode())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa la dirección, ciudad y código postal.");
		}
	}

	private void applyAddress(Address address, User user, DeliveryAddressRequest request) {
		address.setFullName(user.getFullName());
		address.setStreetAddress(request.street().trim());
		address.setCity(request.city().trim());
		address.setPostalCode(request.postalCode().trim());
		address.setNotes(request.notes());
		if (request.state() != null) address.setState(request.state().trim());
		if (request.country() != null) address.setCountry(request.country().trim());
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
			String street, String city, String postalCode, String notes, String clientOperationId,
			String state, String country) {}
	public record UserSettingsRequest(String paymentType, String paymentDetails) {}
	public record UserSettingsResponse(String paymentType, String paymentDetails) {}

}
