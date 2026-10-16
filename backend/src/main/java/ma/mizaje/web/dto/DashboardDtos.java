package ma.mizaje.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Réponse du tableau de bord (abonnements et consommation) pour une période et, éventuellement, un coffee shop. */
public final class DashboardDtos {

	private DashboardDtos() {
	}

	/**
	 * @param from  premier jour inclus
	 * @param to    dernier jour inclus
	 * @param shop  coffee shop filtré, null = tous
	 * @param shops coffee shops connus (pour le filtre)
	 */
	public record Dashboard(LocalDate from, LocalDate to, String shop, List<String> shops, Kpis kpis, Kpis previous,
			List<Day> daily, List<Month> monthly, List<PlanStat> plans, List<ShopStat> shopStats, int[][] heatmap,
			List<FollowUp> followUp, List<TopCustomer> topCustomers) {
	}

	/**
	 * Indicateurs de la période. consumptionRate : boissons consommées / boissons achetées, pour les abonnements
	 * terminés pendant la période (null s'il n'y en a pas). renewalRate : part de ces abonnements suivis d'une
	 * nouvelle période.
	 */
	public record Kpis(long revenue, long activations, long firstActivations, long renewals, long redemptions,
			long newCustomers, long activeNow, Double consumptionRate, Double renewalRate, double redemptionsPerDay) {
	}

	public record Day(LocalDate date, long redemptions, long activations, long revenue) {
	}

	/** revenueByPlan : code de formule → montant encaissé (DH). */
	public record Month(String month, long revenue, Map<String, Long> revenueByPlan) {
	}

	public record PlanStat(String code, String name, int price, long activeNow, long activations, long revenue,
			long redemptions, Double consumptionRate) {
	}

	public record ShopStat(String shop, long redemptions, long activations, long revenue) {
	}

	/** reason : EXPIRING (fin dans 5 jours ou moins), LOW (3 boissons ou moins). */
	public record FollowUp(long customerId, String firstName, String phone, String planName, int drinksLeft,
			int drinksTotal, Instant expiresAt, String reason) {
	}

	public record TopCustomer(long customerId, String firstName, String phone, String planName, long redemptions) {
	}
}
