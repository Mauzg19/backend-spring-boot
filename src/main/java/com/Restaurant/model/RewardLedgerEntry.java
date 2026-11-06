package com.Restaurant.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reward_ledger_entry")
@Getter
@Setter
@NoArgsConstructor
public class RewardLedgerEntry {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	private RewardAccount account;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Order order;

	@Column(nullable = false, unique = true, length = 120)
	private String referenceKey;

	@Column(nullable = false, length = 20)
	private String type;

	@Column(nullable = false)
	private int pointsDelta;

	@Column(nullable = false, length = 240)
	private String description;

	@Column(nullable = false)
	private LocalDateTime createdAt;
}
