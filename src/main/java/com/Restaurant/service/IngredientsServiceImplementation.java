package com.Restaurant.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.Restaurant.Exception.RestaurantException;
import com.Restaurant.model.IngredientCategory;
import com.Restaurant.model.IngredientsItem;
import com.Restaurant.model.Restaurant;
import com.Restaurant.repository.IngredientsCategoryRepository;
import com.Restaurant.repository.IngredientsItemRepository;

@Service
public class IngredientsServiceImplementation implements IngredientsService {

    @Autowired
    private IngredientsCategoryRepository ingredientsCategoryRepo;

    @Autowired
    private IngredientsItemRepository ingredientsItemRepository;

    @Autowired
    private RestaurantService restaurantService;

    @Override
    public IngredientCategory createIngredientsCategory(String name, Long restaurantId) throws RestaurantException {
        IngredientCategory isExist = ingredientsCategoryRepo
                .findByRestaurantIdAndNameIgnoreCase(restaurantId, name);

        if (isExist != null) {
            return isExist;
        }

        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);

        IngredientCategory ingredientCategory = new IngredientCategory();
        ingredientCategory.setRestaurant(restaurant);
        ingredientCategory.setName(name);

        IngredientCategory createdCategory = ingredientsCategoryRepo.save(ingredientCategory);

        return createdCategory;
    }

    @Override
    public IngredientCategory findIngredientsCategoryById(Long id) throws Exception {
        Optional<IngredientCategory> opt = ingredientsCategoryRepo.findById(id);
        if (opt.isEmpty()) {
            throw new Exception("Ingredient category not found");
        }
        return opt.get();
    }

    @Override
    public List<IngredientCategory> findIngredientsCategoryByRestaurantId(Long id) throws Exception {
        return ingredientsCategoryRepo.findByRestaurantId(id);
    }

    @Override
    public List<IngredientsItem> findRestaurantsIngredients(Long restaurantId) {
        return ingredientsItemRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public IngredientsItem createIngredientsItem(Long restaurantId, String ingredientName, Long ingredientCategoryId)
            throws Exception {

        IngredientCategory category = findIngredientsCategoryById(ingredientCategoryId);

        IngredientsItem isExist = ingredientsItemRepository
                .findByRestaurantIdAndNameIngoreCase(restaurantId, ingredientName, category.getName());
        if (isExist != null) {
            System.out.println("Item already exists");
            return isExist;
        }

        Restaurant restaurant = restaurantService.findRestaurantById(restaurantId);
        IngredientsItem item = new IngredientsItem();
        item.setName(ingredientName);
        item.setRestaurant(restaurant);
        item.setCategory(category);
        item.setStock(0); // Inicializar el stock en 0 por defecto

        IngredientsItem savedIngredients = ingredientsItemRepository.save(item);
        category.getIngredients().add(savedIngredients);

        return savedIngredients;
    }

    @Override
    public IngredientsItem updateStoke(Long id) throws Exception {
        // Corregir el nombre del método y su lógica
        Optional<IngredientsItem> item = ingredientsItemRepository.findById(id);
        if (item.isEmpty()) {
            throw new Exception("Ingredient not found with id " + id);
        }
        IngredientsItem ingredient = item.get();
        ingredient.setInStoke(!ingredient.isInStoke());
        return ingredientsItemRepository.save(ingredient);
    }

    @Override
    public IngredientsItem updateStock(Long id, int quantity) throws Exception {
        IngredientsItem ingredient = findIngredientsItemById(id);
        ingredient.setStock(quantity);
        return ingredientsItemRepository.save(ingredient);
    }

    // Nuevo método para encontrar un ingrediente por ID
    private IngredientsItem findIngredientsItemById(Long id) throws Exception {
        Optional<IngredientsItem> item = ingredientsItemRepository.findById(id);
        if (item.isEmpty()) {
            throw new Exception("Ingredient not found with id " + id);
        }
        return item.get();
    }

    @Override
    public IngredientsItem updateIngredient(Long id, Long restaurantId, String ingredientName, Long ingredientCategoryId) throws Exception {
        IngredientsItem ingredient = findIngredientsItemById(id);
        if (ingredientName != null && !ingredientName.isBlank()) {
            ingredient.setName(ingredientName);
        }
        if (ingredientCategoryId != null) {
            IngredientCategory category = findIngredientsCategoryById(ingredientCategoryId);
            ingredient.setCategory(category);
        }
        return ingredientsItemRepository.save(ingredient);
    }

    @Override
    public void deleteIngredient(Long id) throws Exception {
        Optional<IngredientsItem> item = ingredientsItemRepository.findById(id);
        if (item.isEmpty()) {
            throw new Exception("Ingredient not found with id " + id);
        }
        ingredientsItemRepository.deleteById(id);
    }

    @Override
    public IngredientCategory updateIngredientsCategory(Long id, String name) throws Exception {
        Optional<IngredientCategory> opt = ingredientsCategoryRepo.findById(id);
        if (opt.isEmpty()) {
            throw new Exception("Ingredient category not found with id " + id);
        }
        IngredientCategory cat = opt.get();
        if (name != null && !name.isBlank()) {
            cat.setName(name);
        }
        return ingredientsCategoryRepo.save(cat);
    }

    @Override
    public void deleteIngredientsCategory(Long id) throws Exception {
        Optional<IngredientCategory> opt = ingredientsCategoryRepo.findById(id);
        if (opt.isEmpty()) {
            throw new Exception("Ingredient category not found with id " + id);
        }
        // Note: this will cascade-delete ingredients if cascade is configured
        ingredientsCategoryRepo.deleteById(id);
    }
}
