package ma.mizaj.repo;

import java.util.List;
import java.util.Optional;
import ma.mizaj.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

	Optional<Plan> findByCode(String code);

	Optional<Plan> findByCodeAndActiveTrue(String code);

	boolean existsByCode(String code);

	List<Plan> findAllByOrderBySortOrderAscIdAsc();

	List<Plan> findByActiveTrueOrderBySortOrderAscIdAsc();
}
