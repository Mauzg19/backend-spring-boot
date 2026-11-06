package com.Restaurant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Restaurant.Exception.UserException;
import com.Restaurant.dto.RewardCatalogItemResponse;
import com.Restaurant.dto.RewardLedgerEntryResponse;
import com.Restaurant.dto.RewardsSummaryResponse;
import com.Restaurant.model.User;
import com.Restaurant.service.RewardsService;
import com.Restaurant.service.UserService;

@RestController
@RequestMapping("/api/rewards")
public class RewardsController {
	private final RewardsService rewardsService;
	private final UserService userService;

	public RewardsController(RewardsService rewardsService, UserService userService) {
		this.rewardsService = rewardsService;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<RewardsSummaryResponse> getRewards(
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		return ResponseEntity.ok(rewardsService.getSummary(user));
	}

	@PostMapping("/join")
	public ResponseEntity<RewardsSummaryResponse> joinRewards(
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		return ResponseEntity.ok(rewardsService.join(user));
	}

	@GetMapping("/catalog")
	public ResponseEntity<List<RewardCatalogItemResponse>> getCatalog() {
		return ResponseEntity.ok(rewardsService.getCatalog());
	}

	@GetMapping("/history")
	public ResponseEntity<List<RewardLedgerEntryResponse>> getHistory(
			@RequestHeader("Authorization") String jwt) throws UserException {
		User user = userService.findUserProfileByJwt(jwt);
		return ResponseEntity.ok(rewardsService.getHistory(user));
	}
}
