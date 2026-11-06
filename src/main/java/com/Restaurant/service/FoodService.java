package com.Restaurant.service;

import java.util.List;

import com.Restaurant.Exception.FoodException;
import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.model.Category;
import com.Restaurant.model.Food;
import com.Restaurant.model.Restaurant;
import com.Restaurant.request.CreateFoodRequest;

public interface FoodService {

	public Food createFood(CreateFoodRequest req,Category category,
						   Restaurant restaurant) throws FoodException, RestaurantException;

	void deleteFood(Long foodId) throws FoodException;
	
	public List<Food> getRestaurantsFood(Long restaurantId,
			boolean isVegetarian, boolean isNonveg, boolean isSeasonal,String foodCategory) throws FoodException;
	
	public List<Food> searchFood(String keyword);

	public List<Food> findAllFoods();

	public Food findFoodById(Long foodId) throws FoodException;

	public Food findFoodByClientOperationId(String clientOperationId);

	public Food updateAvailibilityStatus(Long foodId) throws FoodException;

	public Food updateAvailability(Long foodId, boolean available) throws FoodException;

	public Food updateFood(Long foodId, String name, String description, Long price,
			List<String> images, Category category) throws FoodException;
}
