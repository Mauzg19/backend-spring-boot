package com.Restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.NotificationDeviceToken;

public interface NotificationDeviceTokenRepository
        extends JpaRepository<NotificationDeviceToken, Long> {

    List<NotificationDeviceToken> findByUserId(Long userId);

    Optional<NotificationDeviceToken> findByToken(String token);

    void deleteByTokenAndUserId(String token, Long userId);
}
