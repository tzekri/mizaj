package ma.mizaje.repo;

import java.time.LocalDate;
import java.util.Optional;
import ma.mizaje.domain.Partner;
import ma.mizaje.domain.PartnerDisplay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PartnerDisplayRepository extends JpaRepository<PartnerDisplay, Long> {

	Optional<PartnerDisplay> findByPartnerAndDayAndShop(Partner partner, LocalDate day, String shop);

	@Query("select coalesce(sum(d.shown), 0) from PartnerDisplay d where d.partner = :partner and d.day >= :since")
	long countShownSince(Partner partner, LocalDate since);

	@Modifying
	@Query("delete from PartnerDisplay d where d.partner = :partner")
	void deleteByPartner(Partner partner);
}
