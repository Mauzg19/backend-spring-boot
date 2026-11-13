package com.Restaurant.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.Restaurant.domain.USER_ROLE;
import com.Restaurant.dto.RestaurantDto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Data
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@NotBlank
	private String fullName;
	@NotBlank
	private String email;
	@NotBlank
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String password;

	@NotNull
	private USER_ROLE role;

	@JsonIgnore
	@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
	private List<Order> orders;

	@ElementCollection
	private List<RestaurantDto> favorites=new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL)
	private List<Address> addresses = new ArrayList<>();

	private String status;

	private String preferredPaymentType;

	private String preferredPaymentDetails;

}
