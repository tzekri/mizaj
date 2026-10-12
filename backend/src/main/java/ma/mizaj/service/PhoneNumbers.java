package ma.mizaj.service;

import ma.mizaj.web.ApiException;

/** Normalisation des numéros saisis par les clients vers le format E.164. */
public final class PhoneNumbers {

	private PhoneNumbers() {
	}

	/**
	 * Accepte « 06 12 34 56 78 », « 0612345678 », « +212 6 12 34 56 78 », « 00212612345678 »…
	 * Les numéros marocains doivent être des mobiles (06 / 07), seuls à avoir WhatsApp.
	 * Les numéros étrangers sont acceptés s'ils sont saisis avec l'indicatif (+33…).
	 */
	public static String normalize(String raw) {
		if (raw == null) {
			throw invalid();
		}
		String s = raw.replaceAll("[\\s.\\-()]", "");
		if (s.startsWith("00")) {
			s = "+" + s.substring(2);
		}
		if (s.matches("0[67]\\d{8}")) {
			s = "+212" + s.substring(1);
		} else if (s.matches("212[67]\\d{8}")) {
			s = "+" + s;
		}
		if (s.startsWith("+212")) {
			if (!s.matches("\\+212[67]\\d{8}")) {
				throw invalid();
			}
			return s;
		}
		if (!s.matches("\\+[1-9]\\d{7,14}")) {
			throw invalid();
		}
		return s;
	}

	/** +212612345678 → +212 6 •• •• •5 78 (affichage au barista). */
	public static String mask(String e164) {
		if (e164.startsWith("+212") && e164.length() == 13) {
			return "+212 " + e164.charAt(4) + " •• •• •" + e164.charAt(10) + " " + e164.substring(11);
		}
		return e164.substring(0, Math.min(4, e164.length())) + " •••• " + e164.substring(e164.length() - 2);
	}

	/** +212612345678 → 06 12 34 56 78 (affichage au client). */
	public static String pretty(String e164) {
		if (e164.startsWith("+212") && e164.length() == 13) {
			String local = "0" + e164.substring(4);
			return local.replaceAll("(\\d{2})(?=\\d)", "$1 ");
		}
		return e164;
	}

	private static ApiException invalid() {
		return ApiException.badRequest("Numéro invalide. Saisissez un mobile marocain (06… ou 07…) ou un numéro avec indicatif (+33…).");
	}
}
