package com.Restaurant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Restaurant.model.RewardLedgerEntry;

public interface RewardLedgerEntryRepository extends JpaRepository<RewardLedgerEntry, Long> {
	boolean existsByReferenceKey(String referenceKey);

	Optional<RewardLedgerEntry> findByReferenceKey(String referenceKey);

	List<RewardLedgerEntry> findTop100ByAccount_IdOrderByCreatedAtDesc(Long accountId);
}
