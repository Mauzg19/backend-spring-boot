package com.Restaurant.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Restaurant.Exception.OrderException;
import com.Restaurant.dto.RewardCatalogItemResponse;
import com.Restaurant.dto.RewardLedgerEntryResponse;
import com.Restaurant.dto.RewardsSummaryResponse;
import com.Restaurant.model.Order;
import com.Restaurant.model.RewardAccount;
import com.Restaurant.model.RewardCatalogItem;
import com.Restaurant.model.RewardLedgerEntry;
import com.Restaurant.model.User;
import com.Restaurant.repository.RewardAccountRepository;
import com.Restaurant.repository.RewardCatalogItemRepository;
import com.Restaurant.repository.RewardLedgerEntryRepository;
import com.Restaurant.repository.UserRepository;

@Service
@Transactional
public class RewardsServiceImplementation implements RewardsService {
	private static final int COP_PER_POINT = 1000;

	private final RewardAccountRepository accountRepository;
	private final RewardCatalogItemRepository catalogRepository;
	private final RewardLedgerEntryRepository ledgerRepository;
	private final UserRepository userRepository;

	public RewardsServiceImplementation(
			RewardAccountRepository accountRepository,
			RewardCatalogItemRepository catalogRepository,
			RewardLedgerEntryRepository ledgerRepository,
			UserRepository userRepository) {
		this.accountRepository = accountRepository;
		this.catalogRepository = catalogRepository;
		this.ledgerRepository = ledgerRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public RewardsSummaryResponse getSummary(User user) {
		Optional<RewardAccount> account = accountRepository.findByUserId(user.getId());
		return new RewardsSummaryResponse(
				account.isPresent(),
				account.map(RewardAccount::getPointsBalance).orElse(0),
				account.map(RewardAccount::getJoinedAt).orElse(null),
				getCatalog());
	}

	@Override
	public RewardsSummaryResponse join(User user) {
		User lockedUser = userRepository.findByIdForUpdate(user.getId())
				.orElseThrow(() -> new IllegalStateException("El usuario de recompensas ya no existe."));
		RewardAccount account = accountRepository.findByUserIdForUpdate(lockedUser.getId())
				.orElseGet(() -> {
					RewardAccount created = new RewardAccount();
					created.setUser(lockedUser);
					created.setPointsBalance(0);
					created.setJoinedAt(LocalDateTime.now());
					return accountRepository.save(created);
				});
		return new RewardsSummaryResponse(
				true,
				account.getPointsBalance(),
				account.getJoinedAt(),
				getCatalog());
	}

	@Override
	@Transactional(readOnly = true)
	public List<RewardCatalogItemResponse> getCatalog() {
		return catalogRepository.findByActiveTrueOrderByRequiredPointsAsc()
				.stream()
				.map(RewardsServiceImplementation::toCatalogResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<RewardLedgerEntryResponse> getHistory(User user) {
		return accountRepository.findByUserId(user.getId())
				.map(account -> ledgerRepository
						.findTop100ByAccount_IdOrderByCreatedAtDesc(account.getId())
						.stream()
						.map(RewardsServiceImplementation::toLedgerResponse)
						.toList())
				.orElseGet(List::of);
	}

	@Override
	public RewardCatalogItem reserveReward(User user, Long rewardId, Order order, long subtotal)
			throws OrderException {
		RewardAccount account = accountRepository.findByUserIdForUpdate(user.getId())
				.orElseThrow(() -> new OrderException("Debes unirte a Restaurant Rewards antes de canjear puntos."));
		RewardCatalogItem reward = catalogRepository.findById(rewardId)
				.filter(RewardCatalogItem::isActive)
				.orElseThrow(() -> new OrderException("El premio seleccionado no está disponible."));

		if (account.getPointsBalance() < reward.getRequiredPoints()) {
			throw new OrderException("No tienes suficientes puntos para canjear este premio.");
		}
		if (subtotal < reward.getMinimumOrderAmount()) {
			throw new OrderException("El pedido no alcanza el mínimo requerido para este descuento.");
		}
		if (reward.getDiscountAmount() <= 0 || reward.getDiscountAmount() >= subtotal) {
			throw new OrderException("El descuento del premio no es válido para este pedido.");
		}

		String referenceKey = redemptionReference(order.getId());
		if (ledgerRepository.existsByReferenceKey(referenceKey)) {
			throw new OrderException("Este premio ya fue reservado para el pedido.");
		}

		account.setPointsBalance(account.getPointsBalance() - reward.getRequiredPoints());
		accountRepository.save(account);
		RewardLedgerEntry entry = new RewardLedgerEntry();
		entry.setAccount(account);
		entry.setOrder(order);
		entry.setReferenceKey(referenceKey);
		entry.setType("REDEMPTION");
		entry.setPointsDelta(-reward.getRequiredPoints());
		entry.setDescription("Reserva de premio: " + reward.getName());
		entry.setCreatedAt(LocalDateTime.now());
		ledgerRepository.save(entry);
		return reward;
	}

	@Override
	public void creditPaidOrder(Order order) {
		if (order.getCustomer() == null || order.getId() == null || order.getTotalAmount() == null) {
			return;
		}
		int points = Math.toIntExact(order.getTotalAmount() / COP_PER_POINT);
		if (points <= 0) {
			return;
		}

		Optional<RewardAccount> account = accountRepository.findByUserIdForUpdate(order.getCustomer().getId());
		if (account.isEmpty()) {
			return;
		}
		String referenceKey = earningReference(order.getId());
		if (ledgerRepository.existsByReferenceKey(referenceKey)) {
			return;
		}

		RewardAccount rewardAccount = account.get();
		rewardAccount.setPointsBalance(Math.addExact(rewardAccount.getPointsBalance(), points));
		accountRepository.save(rewardAccount);
		RewardLedgerEntry entry = new RewardLedgerEntry();
		entry.setAccount(rewardAccount);
		entry.setOrder(order);
		entry.setReferenceKey(referenceKey);
		entry.setType("EARN");
		entry.setPointsDelta(points);
		entry.setDescription("Puntos por pedido pagado #" + order.getId());
		entry.setCreatedAt(LocalDateTime.now());
		ledgerRepository.save(entry);
	}

	@Override
	public void releaseReservedPoints(Order order) {
		if (order.getCustomer() == null || order.getId() == null || order.getRewardPointsReserved() <= 0) {
			return;
		}
		String reservationKey = redemptionReference(order.getId());
		Optional<RewardLedgerEntry> reservation = ledgerRepository.findByReferenceKey(reservationKey);
		if (reservation.isEmpty()) {
			return;
		}

		RewardAccount account = accountRepository.findByUserIdForUpdate(order.getCustomer().getId())
				.orElseThrow(() -> new IllegalStateException("No se encontró la cuenta de puntos reservados."));
		String refundKey = "redemption-refund-order-" + order.getId();
		if (ledgerRepository.existsByReferenceKey(refundKey)) {
			return;
		}

		int points = Math.negateExact(reservation.get().getPointsDelta());
		account.setPointsBalance(Math.addExact(account.getPointsBalance(), points));
		accountRepository.save(account);
		RewardLedgerEntry refund = new RewardLedgerEntry();
		refund.setAccount(account);
		refund.setOrder(order);
		refund.setReferenceKey(refundKey);
		refund.setType("REFUND");
		refund.setPointsDelta(points);
		refund.setDescription("Reintegro de puntos por cancelación del pedido #" + order.getId());
		refund.setCreatedAt(LocalDateTime.now());
		ledgerRepository.save(refund);
	}

	private static String redemptionReference(Long orderId) {
		return "redemption-order-" + orderId;
	}

	private static String earningReference(Long orderId) {
		return "earning-order-" + orderId;
	}

	private static RewardCatalogItemResponse toCatalogResponse(RewardCatalogItem item) {
		return new RewardCatalogItemResponse(
				item.getId(),
				item.getCode(),
				item.getName(),
				item.getDescription(),
				item.getRequiredPoints(),
				item.getDiscountAmount(),
				item.getMinimumOrderAmount(),
				item.isActive());
	}

	private static RewardLedgerEntryResponse toLedgerResponse(RewardLedgerEntry entry) {
		return new RewardLedgerEntryResponse(
				entry.getId(),
				entry.getType(),
				entry.getPointsDelta(),
				entry.getDescription(),
				entry.getOrder() == null ? null : entry.getOrder().getId(),
				entry.getCreatedAt());
	}
}
