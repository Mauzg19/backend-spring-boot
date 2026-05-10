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

@RestController
@RequestMapping("/api/admin/ingredients")
public class IngredientsController {
	
	@Autowired
	private IngredientsService ingredientService;

	@PostMapping("/category")
	public ResponseEntity<IngredientCategory> createIngredientCategory(
			@RequestBody CreateIngredientCategoryRequest req) throws Exception{
		IngredientCategory items=ingredientService.createIngredientsCategory(req.getName(), req.getRestaurantId());
		return new ResponseEntity<>(items,HttpStatus.OK);
	}

	@PostMapping()
	public ResponseEntity<IngredientsItem> createIngredient(
			@RequestBody CreateIngredientRequest req) throws Exception{

		IngredientsItem item=ingredientService.createIngredientsItem(req.getRestaurantId(),req.getName(),req.getIngredientCategoryId());
		return new ResponseEntity<>(item,HttpStatus.OK);
	}
	
	@PutMapping("/{id}/stoke")
	public ResponseEntity<IngredientsItem> updateStoke(@PathVariable Long id) throws Exception{
		IngredientsItem item=ingredientService.updateStoke(id);
		return new ResponseEntity<IngredientsItem>(item,HttpStatus.OK);
	}
	
	@GetMapping("/restaurant/{id}")
	public ResponseEntity<List<IngredientsItem>> restaurantsIngredient(
			@PathVariable Long id) throws Exception{
		List<IngredientsItem> items=ingredientService.findRestaurantsIngredients(id);
		return new ResponseEntity<>(items,HttpStatus.OK);
	}

	@GetMapping("/restaurant/{id}/category")
	public ResponseEntity<List<IngredientCategory>> restaurantsIngredientCategory(
			@PathVariable Long id) throws Exception{
		List<IngredientCategory> items=ingredientService.findIngredientsCategoryByRestaurantId(id);
		return new ResponseEntity<>(items,HttpStatus.OK);
	}

	// Update ingredient (name / category)
	@PutMapping("/{id}")
	public ResponseEntity<IngredientsItem> updateIngredient(@PathVariable Long id, @RequestBody CreateIngredientRequest req) throws Exception{
		IngredientsItem item = ingredientService.updateIngredient(id, req.getRestaurantId(), req.getName(), req.getIngredientCategoryId());
		return new ResponseEntity<>(item, HttpStatus.OK);
	}

	// Delete ingredient
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteIngredient(@PathVariable Long id) throws Exception{
		ingredientService.deleteIngredient(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// Update ingredient category
	@PutMapping("/category/{id}")
	public ResponseEntity<IngredientCategory> updateIngredientCategory(@PathVariable Long id, @RequestBody CreateIngredientCategoryRequest req) throws Exception{
		IngredientCategory cat = ingredientService.updateIngredientsCategory(id, req.getName());
		return new ResponseEntity<>(cat, HttpStatus.OK);
	}

	// Delete ingredient category
	@DeleteMapping("/category/{id}")
	public ResponseEntity<?> deleteIngredientCategory(@PathVariable Long id) throws Exception{
		ingredientService.deleteIngredientsCategory(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}
