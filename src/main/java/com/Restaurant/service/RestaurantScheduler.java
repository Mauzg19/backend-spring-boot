package com.Restaurant.service;

import com.Restaurant.model.Restaurant;
import com.Restaurant.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class RestaurantScheduler {
    @Autowired
    private RestaurantRepository restaurantRepository;

    // Formato esperado: "Mon: 9:00 AM - 9:00 PM; Tue: 10:00 AM - 8:00 PM; ..."
    @Scheduled(cron = "0 * * * * *") // Cada minuto
    public void updateRestaurantOpenStatus() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        for (Restaurant restaurant : restaurants) {
            boolean shouldBeOpen = isOpenNow(restaurant.getOpeningHours());
            if (restaurant.isOpen() != shouldBeOpen) {
                restaurant.setOpen(shouldBeOpen);
                restaurantRepository.save(restaurant);
            }
        }
    }

    private boolean isOpenNow(String openingHours) {
        if (openingHours == null || openingHours.isEmpty()) return false;
        String day = java.time.LocalDate.now().getDayOfWeek().toString().substring(0,3);
        String[] days = openingHours.split(";");
        for (String d : days) {
            d = d.trim();
            if (d.startsWith(day)) {
                String[] parts = d.split(":");
                if (parts.length < 2) return false;
                String[] hours = parts[1].split("-");
                if (hours.length < 2) return false;
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
                    LocalTime open = LocalTime.parse(hours[0].trim(), formatter);
                    LocalTime close = LocalTime.parse(hours[1].trim(), formatter);
                    LocalTime now = LocalTime.now();
                    return !now.isBefore(open) && !now.isAfter(close);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }
}
