package com.Restaurant.service;

import java.util.List;
import com.Restaurant.model.ContactInformation;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.dto.RestaurantDto;
import com.Restaurant.model.Address;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;
import com.Restaurant.repository.AddressRepository;
import com.Restaurant.repository.RestaurantRepository;
import com.Restaurant.repository.UserRepository;
import com.Restaurant.request.CreateRestaurantRequest;

@Service
public class RestaurantServiceImplementation implements RestaurantService {
	@Autowired
	private RestaurantRepository restaurantRepository;
	@Autowired
	private AddressRepository addressRepository;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private UserRepository userRepository;
	

	@Override
	public Restaurant createRestaurant(CreateRestaurantRequest req,User user) {
		Address address=new Address();
		address.setCity(req.getAddress().getCity());
		address.setCountry(req.getAddress().getCountry());
		address.setFullName(req.getAddress().getFullName());
		address.setPostalCode(req.getAddress().getPostalCode());
		address.setState(req.getAddress().getState());
		address.setStreetAddress(req.getAddress().getStreetAddress());
		Address savedAddress = addressRepository.save(address);
		
		Restaurant restaurant = new Restaurant();
		
		restaurant.setAddress(savedAddress);
		restaurant.setContactInformation(req.getContactInformation());
		restaurant.setCuisineType(req.getCuisineType());
		restaurant.setDescription(req.getDescription());
		restaurant.setImages(req.getImages());
		restaurant.setName(req.getName());
		restaurant.setOpeningHours(req.getOpeningHours());
		restaurant.setRegistrationDate(req.getRegistrationDate());
		restaurant.setOwner(user);
		Restaurant savedRestaurant = restaurantRepository.save(restaurant);

		return savedRestaurant;
	}

	@Override
	public Restaurant updateRestaurant(Long restaurantId, CreateRestaurantRequest updatedReq)
			throws RestaurantException {
		Restaurant restaurant = findRestaurantById(restaurantId);
		
		// Actualizar todos los campos si vienen en la solicitud
		if (updatedReq.getName() != null) {
			restaurant.setName(updatedReq.getName());
		}
		if (updatedReq.getCuisineType() != null) {
			restaurant.setCuisineType(updatedReq.getCuisineType());
		}
		if (updatedReq.getDescription() != null) {
			restaurant.setDescription(updatedReq.getDescription());
		}
		if (updatedReq.getOpeningHours() != null) {
			restaurant.setOpeningHours(updatedReq.getOpeningHours());
		}
		if (updatedReq.getContactInformation() != null) {
			if (restaurant.getContactInformation() == null) {
				restaurant.setContactInformation(new ContactInformation());
			}
			ContactInformation contactInfo = restaurant.getContactInformation();
			ContactInformation updatedContact = updatedReq.getContactInformation();
			
			if (updatedContact.getEmail() != null) {
				contactInfo.setEmail(updatedContact.getEmail());
			}
			if (updatedContact.getMobile() != null) {
				contactInfo.setMobile(updatedContact.getMobile());
			}
			if (updatedContact.getInstagram() != null) {
				contactInfo.setInstagram(updatedContact.getInstagram());
			}
			if (updatedContact.getFacebook() != null) {
				contactInfo.setFacebook(updatedContact.getFacebook());
			}
			if (updatedContact.getTwitter() != null) {
				contactInfo.setTwitter(updatedContact.getTwitter());
			}
		}
		if (updatedReq.getImages() != null) {
			restaurant.setImages(updatedReq.getImages());
		}
		
		return restaurantRepository.save(restaurant);
	}
	
	@Override
	public Restaurant findRestaurantById(Long restaurantId) throws RestaurantException {
		Optional<Restaurant> restaurant = restaurantRepository.findById(restaurantId);
		if (restaurant.isPresent()) {
			return restaurant.get();
		} else {
			throw new RestaurantException("Restaurant with id " + restaurantId + "not found");
		}
	}

	@Override
	public void deleteRestaurant(Long restaurantId) throws RestaurantException {
		Restaurant restaurant = findRestaurantById(restaurantId);
		if (restaurant != null) {
			restaurantRepository.delete(restaurant);
			return;
		}
		throw new RestaurantException("Restaurant with id " + restaurantId + " Not found");

	}

	@Override
	public List<Restaurant> getAllRestaurant() {
		return restaurantRepository.findAll();
	}


	@Override
	public Restaurant getRestaurantsByUserId(Long userId) throws RestaurantException {
		Restaurant restaurants=restaurantRepository.findByOwnerId(userId);
		return restaurants;
	}



	@Override
	public List<Restaurant> searchRestaurant(String keyword) {
		return restaurantRepository.findBySearchQuery(keyword);
	}

	@Override
	public RestaurantDto addToFavorites(Long restaurantId,User user) throws RestaurantException {
		Restaurant restaurant=findRestaurantById(restaurantId);
		
		RestaurantDto dto=new RestaurantDto();
		dto.setTitle(restaurant.getName());
		dto.setImages(restaurant.getImages());
		dto.setId(restaurant.getId());
		dto.setDescription(restaurant.getDescription());

		boolean isFavorited = false;
		List<RestaurantDto> favorites = user.getFavorites();
		for (RestaurantDto favorite : favorites) {
			if (favorite.getId().equals(restaurantId)) {
				isFavorited = true;
				break;
			}
		}

		if (isFavorited) {
			favorites.removeIf(favorite -> favorite.getId().equals(restaurantId));
		} else {
			favorites.add(dto);
		}
		
		User updatedUser = userRepository.save(user);
		return dto;
	}

	@Override
	public Restaurant updateRestaurantStatus(Long id) throws RestaurantException {
		Restaurant restaurant=findRestaurantById(id);
		restaurant.setOpen(!restaurant.isOpen());
		return restaurantRepository.save(restaurant);
	}

}
