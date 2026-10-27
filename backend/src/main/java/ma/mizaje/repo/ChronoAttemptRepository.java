package ma.mizaje.repo;

import java.time.Instant;
import java.util.Optional;
import ma.mizaje.domain.ChronoAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChronoAttemptRepository extends JpaRepository<ChronoAttempt, Long> {

	Optional<ChronoAttempt> findByToken(String token);

	long countByDeviceAndStartedAtGreaterThanEqual(String device, Instant since);

	long countByIpAndStartedAtGreaterThanEqual(String ip, Instant since);

	long countByStartedAtGreaterThanEqual(Instant since);
}
