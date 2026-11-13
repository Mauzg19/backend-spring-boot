package com.Restaurant.service;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.model.Restaurant;
import com.Restaurant.model.User;

public final class RestaurantAccess {
    private RestaurantAccess() {}

    public static void require(User actor, Restaurant restaurant) {
        if (actor.getRole() == USER_ROLE.ROLE_ADMIN) return;
        if (actor.getRole() != USER_ROLE.ROLE_RESTAURANT_OWNER || restaurant == null
                || restaurant.getOwner() == null
                || !Objects.equals(actor.getId(), restaurant.getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El recurso pertenece a otro restaurante.");
        }
    }

    public static void requireSameRestaurant(Restaurant expected, Restaurant actual) {
        if (actual == null || !Objects.equals(expected.getId(), actual.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los recursos deben pertenecer al mismo restaurante.");
        }
    }
}
