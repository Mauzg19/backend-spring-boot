package com.Restaurant.prediccion.model;

public record CategoriaPrediccionResponse(
        String predictedCategory,
        double confidence
) {}
