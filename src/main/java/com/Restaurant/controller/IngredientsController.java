package com.Restaurant.controller;

import java.util.List;

import com.Restaurant.request.CreateIngredientCategoryRequest;
import com.Restaurant.request.CreateIngredientRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Restaurant.model.IngredientCategory;
import com.Restaurant.model.IngredientsItem;
import com.Restaurant.service.IngredientsService;
import com.Restaurant.service.RestaurantAccess;
import com.Restaurant.service.RestaurantService;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api/admin/ingredients")
public class IngredientsController {
	
	@Autowired
	private IngredientsService ingredientService;
	@Autowired
	private RestaurantService restaurantService;
	@Autowired
	private UserService userService;

	@PostMapping("/category")
	public ResponseEntity<IngredientCategory> createIngredientCategory(
			@RequestBody CreateIngredientCategoryRequest req, @RequestHeader("Authorization") String jwt) throws Exception {
		requireRestaurant(jwt, req.getRestaurantId());
		IngredientCategory items=ingredientService.createIngredientsCategory(req.getName(), req.getRestaurantId());
		return new ResponseEntity<>(items,HttpStatus.OK);
	}

	@PostMapping()
	public ResponseEntity<IngredientsItem> createIngredient(
			@RequestBody CreateIngredientRequest req, @RequestHeader("Authorization") String jwt) throws Exception {
		requireRestaurant(jwt, req.getRestaurantId());

		IngredientsItem item=ingredientService.createIngredientsItem(req.getRestaurantId(),req.getName(),req.getIngredientCategoryId());
		return new ResponseEntity<>(item,HttpStatus.OK);
	}
	
	@PutMapping("/{id}/stoke")
	public ResponseEntity<IngredientsItem> updateStoke(@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
		requireIngredient(jwt, id);
		IngredientsItem item=ingredientService.updateStoke(id);
		return new ResponseEntity<IngredientsItem>(item,HttpStatus.OK);
	}
	
	@GetMapping("/restaurant/{id}")
	public ResponseEntity<List<IngredientsItem>> restaurantsIngredient(
			@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
		requireRestaurant(jwt, id);
		List<IngredientsItem> items=ingredientService.findRestaurantsIngredients(id);
		return new ResponseEntity<>(items,HttpStatus.OK);
	}

	@GetMapping("/restaurant/{id}/category")
	public ResponseEntity<List<IngredientCategory>> restaurantsIngredientCategory(
			@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
		requireRestaurant(jwt, id);
		List<IngredientCategory> items=ingredientService.findIngredientsCategoryByRestaurantId(id);
		return new ResponseEntity<>(items,HttpStatus.OK);
	}

	// Update ingredient (name / category)
	@PutMapping("/{id}")
	public ResponseEntity<IngredientsItem> updateIngredient(@PathVariable Long id, @RequestBody CreateIngredientRequest req,
			@RequestHeader("Authorization") String jwt) throws Exception {
		requireIngredient(jwt, id);
		IngredientsItem item = ingredientService.updateIngredient(id, req.getRestaurantId(), req.getName(), req.getIngredientCategoryId());
		return new ResponseEntity<>(item, HttpStatus.OK);
	}

	// Delete ingredient
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteIngredient(@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
		requireIngredient(jwt, id);
		ingredientService.deleteIngredient(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// Update ingredient category
	@PutMapping("/category/{id}")
	public ResponseEntity<IngredientCategory> updateIngredientCategory(@PathVariable Long id, @RequestBody CreateIngredientCategoryRequest req,
			@RequestHeader("Authorization") String jwt) throws Exception {
		requireCategory(jwt, id);
		IngredientCategory cat = ingredientService.updateIngredientsCategory(id, req.getName());
		return new ResponseEntity<>(cat, HttpStatus.OK);
	}

	// Delete ingredient category
	@DeleteMapping("/category/{id}")
	public ResponseEntity<?> deleteIngredientCategory(@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws Exception {
		requireCategory(jwt, id);
		ingredientService.deleteIngredientsCategory(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private void requireRestaurant(String jwt, Long id) throws Exception {
		RestaurantAccess.require(userService.findUserProfileByJwt(jwt), restaurantService.findRestaurantById(id));
	}

	private void requireIngredient(String jwt, Long id) throws Exception {
		RestaurantAccess.require(userService.findUserProfileByJwt(jwt), ingredientService.findIngredientsItemById(id).getRestaurant());
	}

	private void requireCategory(String jwt, Long id) throws Exception {
		RestaurantAccess.require(userService.findUserProfileByJwt(jwt), ingredientService.findIngredientsCategoryById(id).getRestaurant());
	}
}
