package ma.mizaje.repo;

import java.time.Instant;
import java.util.Optional;
import ma.mizaje.domain.ChronoPrize;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChronoPrizeRepository extends JpaRepository<ChronoPrize, Long> {

	Optional<ChronoPrize> findByCode(String code);

	boolean existsByCode(String code);

	long countByCreatedAtGreaterThanEqual(Instant since);

	long countByRedeemedAtGreaterThanEqual(Instant since);
}
