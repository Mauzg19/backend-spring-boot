package com.Restaurant.service;

import java.util.List;

import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.model.IngredientCategory;
import com.Restaurant.model.IngredientsItem;

public interface IngredientsService {
    
    public IngredientCategory createIngredientsCategory(
            String name, Long restaurantId) throws RestaurantException;

    public IngredientCategory findIngredientsCategoryById(Long id) throws Exception;

    public List<IngredientCategory> findIngredientsCategoryByRestaurantId(Long id) throws Exception;
    
    public List<IngredientsItem> findRestaurantsIngredients(Long restaurantId);

    public IngredientsItem createIngredientsItem(Long restaurantId, 
            String ingredientName, Long ingredientCategoryId) throws Exception;

    public IngredientsItem updateStoke(Long id) throws Exception;

    // Agregar el método updateStock a la interfaz
    public IngredientsItem updateStock(Long id, int quantity) throws Exception;

        // Nuevo: actualizar un ingrediente (nombre y categoría)
        public IngredientsItem updateIngredient(Long id, Long restaurantId, String ingredientName, Long ingredientCategoryId) throws Exception;

        // Nuevo: eliminar un ingrediente por id
        public void deleteIngredient(Long id) throws Exception;

        // Nuevo: actualizar categoría de ingrediente
        public IngredientCategory updateIngredientsCategory(Long id, String name) throws Exception;

        // Nuevo: eliminar categoría de ingrediente
        public void deleteIngredientsCategory(Long id) throws Exception;
}
