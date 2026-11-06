package com.Restaurant.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Restaurant.model.RewardAccount;

import jakarta.persistence.LockModeType;

public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {
	Optional<RewardAccount> findByUserId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT account FROM RewardAccount account WHERE account.user.id = :userId")
	Optional<RewardAccount> findByUserIdForUpdate(@Param("userId") Long userId);
}
