package com.Restaurant.controller;

import java.util.List;

import com.Restaurant.Exception.UserException;
import com.Restaurant.model.User;
import com.Restaurant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.model.Category;
import com.Restaurant.service.CategoryService;

@RestController
@RequestMapping("/api")
public class CategoryController {
	
	@Autowired
	public CategoryService categoryService;

	@Autowired
	public UserService userService;
	
	@PostMapping("/admin/category")
	public ResponseEntity<Category> createdCategory(
			@RequestHeader("Authorization")String jwt,
			@RequestBody Category category) throws RestaurantException, UserException {
		User user=userService.findUserProfileByJwt(jwt);
		
		Category createdCategory=categoryService.createCategory(category.getName(), user.getId());
		return new ResponseEntity<Category>(createdCategory,HttpStatus.OK);
	}

	@PutMapping("/admin/category/{id}")
	public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestHeader("Authorization") String jwt, @RequestBody Category category) throws RestaurantException, UserException {
		User user = userService.findUserProfileByJwt(jwt);
		Category updated = categoryService.updateCategory(id, category.getName());
		return new ResponseEntity<>(updated, HttpStatus.OK);
	}

	@DeleteMapping("/admin/category/{id}")
	public ResponseEntity<?> deleteCategory(@PathVariable Long id, @RequestHeader("Authorization") String jwt) throws RestaurantException, UserException {
		User user = userService.findUserProfileByJwt(jwt);
		categoryService.deleteCategory(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	@GetMapping("/category/restaurant/{id}")
	public ResponseEntity<List<Category>> getRestaurantsCategory(
			@PathVariable Long id,
			@RequestHeader("Authorization")String jwt) throws RestaurantException, UserException {
		User user=userService.findUserProfileByJwt(jwt);
		List<Category> categories=categoryService.findCategoryByRestaurantId(id);
		return new ResponseEntity<>(categories,HttpStatus.OK);
	}

}
