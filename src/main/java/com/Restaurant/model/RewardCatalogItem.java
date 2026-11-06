package com.Restaurant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reward_catalog_item")
@Getter
@Setter
@NoArgsConstructor
public class RewardCatalogItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(nullable = false)
	private int requiredPoints;

	@Column(nullable = false)
	private long discountAmount;

	@Column(nullable = false)
	private long minimumOrderAmount;

	@Column(nullable = false)
	private boolean active;
}
