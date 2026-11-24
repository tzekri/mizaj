package ma.mizaje.repo;

import java.util.List;
import ma.mizaje.domain.WeeklyAd;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyAdRepository extends JpaRepository<WeeklyAd, Long> {

	/** De la plus récente à la plus ancienne. */
	List<WeeklyAd> findAllByOrderByStartsOnDescIdDesc();
}
