package com.Restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

}
