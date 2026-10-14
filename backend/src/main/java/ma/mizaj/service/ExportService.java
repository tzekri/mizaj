package ma.mizaj.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import ma.mizaj.domain.Redemption;
import ma.mizaj.domain.Subscription;
import ma.mizaj.repo.RedemptionRepository;
import ma.mizaj.repo.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports CSV pour Excel (séparateur « ; », BOM UTF-8 pour les accents, dates au format français).
 */
@Service
public class ExportService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private final RedemptionRepository redemptions;
	private final SubscriptionRepository subscriptions;
	private final Clock clock;
	private final ZoneId zone;

	public ExportService(RedemptionRepository redemptions, SubscriptionRepository subscriptions, Clock clock, ZoneId appZone) {
		this.redemptions = redemptions;
		this.subscriptions = subscriptions;
		this.clock = clock;
		this.zone = appZone;
	}

	@Transactional(readOnly = true)
	public String redemptions(LocalDate from, LocalDate to, String shop) {
		StringBuilder csv = header("Date;Heure;Coffee shop;Client;Téléphone;Formule;Barista");
		for (Redemption r : redemptions.findInPeriod(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant())) {
			if (shop != null && !shop.isBlank() && !shop.equals(r.getShop())) {
				continue;
			}
			var t = r.getRedeemedAt().atZone(zone);
			Subscription s = r.getSubscription();
			row(csv, DATE.format(t), TIME.format(t), r.getShop(), s.getCustomer().getFirstName(),
					PhoneNumbers.pretty(s.getCustomer().getPhone()), s.getPlan() == null ? "" : s.getPlan().getName(),
					r.getStaff().getFullName());
		}
		return csv.toString();
	}

	/** Toutes les périodes d'abonnement (une ligne par activation), la plus récente d'abord. */
	@Transactional(readOnly = true)
	public String subscriptions() {
		Instant now = Instant.now(clock);
		StringBuilder csv = header("Client;Téléphone;Formule;Activé le;Fin;État;Boissons restantes;Boissons achetées;Prix (DH);Coffee shop;Barista");
		List<Subscription> all = subscriptions.findAllWithCustomerAndPlan().stream()
				.sorted(Comparator.comparing(Subscription::getActivatedAt).reversed()).toList();
		for (Subscription s : all) {
			row(csv, s.getCustomer().getFirstName(), PhoneNumbers.pretty(s.getCustomer().getPhone()),
					s.getPlan() == null ? "" : s.getPlan().getName(),
					DATE.format(s.getActivatedAt().atZone(zone)), DATE.format(s.getExpiresAt().atZone(zone)),
					switch (s.statusAt(now)) {
						case ACTIVE -> "Actif";
						case EMPTY -> "Épuisé";
						case EXPIRED -> "Terminé";
						case NONE -> "";
					},
					String.valueOf(s.getDrinksLeft()), String.valueOf(s.getDrinksTotal()), String.valueOf(s.getPricePaid()),
					s.getShop(), s.getActivatedBy().getFullName());
		}
		return csv.toString();
	}

	private static StringBuilder header(String columns) {
		return new StringBuilder("﻿").append(columns).append("\r\n");
	}

	private static void row(StringBuilder csv, String... values) {
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				csv.append(';');
			}
			csv.append(cell(values[i]));
		}
		csv.append("\r\n");
	}

	/** Échappement CSV, et neutralisation des formules (=, +, -, @) qu'Excel exécuterait. */
	static String cell(String v) {
		if (v == null) {
			return "";
		}
		String s = v;
		if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
			s = "'" + s;
		}
		if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
			s = "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}
}
