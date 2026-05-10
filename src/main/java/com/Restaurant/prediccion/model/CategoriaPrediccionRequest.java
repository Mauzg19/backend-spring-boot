package com.Restaurant.prediccion.model;

import jakarta.validation.constraints.PositiveOrZero;

public record CategoriaPrediccionRequest(
        @PositiveOrZero(message = "El precio no puede ser negativo")
        double price,

        boolean isVegetarian,
        boolean isSeasonal,

        @PositiveOrZero(message = "La cantidad no puede ser negativa")
        int quantity
) {}
