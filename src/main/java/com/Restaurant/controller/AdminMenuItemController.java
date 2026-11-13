package com.Restaurant.controller;

import java.util.List;
import java.util.Map;

import com.Restaurant.model.Restaurant;
import com.Restaurant.service.RestaurantAccess;
import com.Restaurant.model.Category;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.Restaurant.service.CategoryService;
import com.Restaurant.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Restaurant.Exception.FoodException;
import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.Exception.UserException;
import com.Restaurant.model.Food;
import com.Restaurant.model.User;
import com.Restaurant.request.CreateFoodRequest;
import com.Restaurant.service.FoodService;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api/admin/food")
public class AdminMenuItemController {
	
	@Autowired
	private FoodService menuItemService;
	@Autowired
	private UserService userService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private CategoryService categoryService;
	@Autowired
	private ProductImageStorage productImageStorage;

	@GetMapping("/all")
	public ResponseEntity<List<Food>> getAllMenuItems() {
		return ResponseEntity.ok(menuItemService.findAllFoods());
	}

	@PostMapping()
	public ResponseEntity<Food> createItem(
			@RequestBody CreateFoodRequest item, 
			@RequestHeader("Authorization") String jwt)
			throws FoodException, UserException, RestaurantException {
		User user = userService.findUserProfileByJwt(jwt);
		if (item.getClientOperationId() != null && !item.getClientOperationId().isBlank()) {
			Food existing = menuItemService.findFoodByClientOperationId(item.getClientOperationId());
			if (existing != null) {
				RestaurantAccess.require(user, existing.getRestaurant());
				Category category = item.getCategory() == null
						? existing.getFoodCategory()
						: categoryForRestaurant(item.getCategory().getId(), existing.getRestaurant());
				Food updated = menuItemService.updateFood(
						existing.getId(), item.getName(), item.getDescription(),
						item.getPrice(), item.getImages(), category);
				if (updated.isAvailable() != item.isAvailable()) {
					updated = menuItemService.updateAvailability(
							updated.getId(), item.isAvailable());
				}
				return ResponseEntity.ok(updated);
			}
		}
//		Category category=categoryService.findCategoryById(item.getCategoryId());
		Restaurant restaurant=restaurantService.findRestaurantById(item.getRestaurantId());
		RestaurantAccess.require(user, restaurant);
			Category category = item.getCategory() == null ? null
					: categoryForRestaurant(item.getCategory().getId(), restaurant);
			Food menuItem = menuItemService.createFood(item,category,restaurant);
			return ResponseEntity.ok(menuItem);

	}

	@PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, String>> uploadProductImage(
			@PathVariable Long id,
			@RequestParam("image") MultipartFile image,
			@RequestHeader("Authorization") String jwt) throws java.io.IOException, FoodException, UserException {
		try {
			requireFoodAccess(jwt, id);
			String imageUrl = productImageStorage.save(image);
			menuItemService.updateFood(id, null, null, null, List.of(imageUrl), null);
			return ResponseEntity.ok(Map.of("url", imageUrl));
		} catch (IllegalArgumentException exception) {
			return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
		}
	}


	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteItem(@PathVariable Long id, @RequestHeader("Authorization") String jwt)
			throws UserException, FoodException {
		requireFoodAccess(jwt, id);
			menuItemService.deleteFood(id);
			return ResponseEntity.ok("Menu item deleted");
		
	
	}

	

	@GetMapping("/search")
	public ResponseEntity<List<Food>> getMenuItemByName(@RequestParam String name)  {
		List<Food> menuItem = menuItemService.searchFood(name);
		return ResponseEntity.ok(menuItem);
	}
	
	
	@PutMapping("/{id}")
	public ResponseEntity<Food> updateAvilibilityStatus(
			@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws FoodException, UserException {
		requireFoodAccess(jwt, id);
		Food menuItems= menuItemService.updateAvailibilityStatus(id);
		return ResponseEntity.ok(menuItems);
	}

	@PutMapping("/{id}/availability")
	public ResponseEntity<Food> updateAvailability(
			@PathVariable Long id,
			@RequestBody AvailabilityRequest request,
			@RequestHeader("Authorization") String jwt) throws FoodException, UserException {
		requireFoodAccess(jwt, id);
		Food menuItem = menuItemService.updateAvailability(id, request.available());
		return ResponseEntity.ok(menuItem);
	}

	@PutMapping("/{id}/details")
	public ResponseEntity<Food> updateFood(
			@PathVariable Long id,
			@RequestBody FoodUpdateRequest request,
			@RequestHeader("Authorization") String jwt) throws FoodException, RestaurantException, UserException {
		Food food = requireFoodAccess(jwt, id);
		Category category = request.categoryId() == null
				? null
				: categoryForRestaurant(request.categoryId(), food.getRestaurant());
		Food menuItem = menuItemService.updateFood(
				id, request.name(), request.description(), request.price(),
				request.images(), category);
		return ResponseEntity.ok(menuItem);
	}

	private Food requireFoodAccess(String jwt, Long id) throws FoodException, UserException {
		User actor = userService.findUserProfileByJwt(jwt);
		Food food = menuItemService.findFoodById(id);
		RestaurantAccess.require(actor, food.getRestaurant());
		return food;
	}

	private Category categoryForRestaurant(Long id, Restaurant restaurant) throws RestaurantException {
		if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una categoría válida.");
		Category category = categoryService.findCategoryById(id);
		RestaurantAccess.requireSameRestaurant(restaurant, category.getRestaurant());
		return category;
	}

	public record AvailabilityRequest(boolean available) {}
	public record FoodUpdateRequest(String name, String description, Long price,
			List<String> images, Long categoryId) {}
	
	

}
