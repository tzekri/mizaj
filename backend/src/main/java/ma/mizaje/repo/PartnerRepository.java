package ma.mizaje.repo;

import java.util.List;
import ma.mizaje.domain.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

	List<Partner> findAllByOrderBySortOrderAscIdAsc();
}
