package ma.mizaj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Formule d'abonnement : prix en dirhams, nombre d'espressos, durée de validité en jours. */
@ConfigurationProperties(prefix = "app.plan")
public record PlanProperties(int price, int drinks, int days) {
}
