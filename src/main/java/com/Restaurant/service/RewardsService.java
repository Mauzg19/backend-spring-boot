package com.Restaurant.service;

import java.util.List;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.dto.RewardCatalogItemResponse;
import com.Restaurant.dto.RewardLedgerEntryResponse;
import com.Restaurant.dto.RewardsSummaryResponse;
import com.Restaurant.model.Order;
import com.Restaurant.model.RewardCatalogItem;
import com.Restaurant.model.User;

public interface RewardsService {
	RewardsSummaryResponse getSummary(User user);

	RewardsSummaryResponse join(User user);

	List<RewardCatalogItemResponse> getCatalog();

	List<RewardLedgerEntryResponse> getHistory(User user);

	RewardCatalogItem reserveReward(User user, Long rewardId, Order order, long subtotal) throws OrderException;

	void creditPaidOrder(Order order);

	void releaseReservedPoints(Order order);
}
