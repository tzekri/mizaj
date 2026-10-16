package ma.mizaje.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.JuiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JuiceOrderRepository extends JpaRepository<JuiceOrder, Long> {

	Optional<JuiceOrder> findByCode(String code);

	boolean existsByCode(String code);

	List<JuiceOrder> findByServedAtGreaterThanEqualAndServedAtLessThan(Instant from, Instant to);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
}
