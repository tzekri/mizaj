package ma.mizaj.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	/** Fuseau qui définit « aujourd'hui » pour les statistiques des coffee shops. */
	@Bean
	ZoneId appZone(@Value("${app.timezone}") String zone) {
		return ZoneId.of(zone);
	}
}
