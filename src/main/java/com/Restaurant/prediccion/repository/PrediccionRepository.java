package com.Restaurant.prediccion.repository;

import com.Restaurant.prediccion.model.PrediccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrediccionRepository extends JpaRepository<PrediccionEntity, Long> {

    List<PrediccionEntity> findByPrice(double price);

    List<PrediccionEntity> findByPredictedCategory(String predictedCategory);

    List<PrediccionEntity> findByIsVegetarian(boolean isVegetarian);

    List<PrediccionEntity> findByIsSeasonal(boolean isSeasonal);

    List<PrediccionEntity> findByQuantity(int quantity);

    @Query("SELECT p FROM PrediccionEntity p WHERE " +
           "(:price IS NULL OR p.price = :price) AND " +
           "(:isVegetarian IS NULL OR p.isVegetarian = :isVegetarian) AND " +
           "(:isSeasonal IS NULL OR p.isSeasonal = :isSeasonal) AND " +
           "(:quantity IS NULL OR p.quantity = :quantity) AND " +
           "(:predictedCategory IS NULL OR p.predictedCategory = :predictedCategory)")
    List<PrediccionEntity> findByCriteria(@Param("price") Double price,
                                          @Param("isVegetarian") Boolean isVegetarian,
                                          @Param("isSeasonal") Boolean isSeasonal,
                                          @Param("quantity") Integer quantity,
                                          @Param("predictedCategory") String predictedCategory);
}
