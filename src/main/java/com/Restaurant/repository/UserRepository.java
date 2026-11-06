package com.Restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Restaurant.model.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {
	
	
	
	@Query("SELECT u FROM User u Where u.status='PENDING'")
	public List<User> getPenddingRestaurantOwners();
	
	public User findByEmail(String username);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT u FROM User u WHERE u.id = :userId")
	Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
