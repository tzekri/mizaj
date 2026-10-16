package ma.mizaje.repo;

import java.time.Instant;
import java.util.Optional;
import ma.mizaje.domain.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {

	Optional<OtpChallenge> findFirstByPhoneOrderByCreatedAtDesc(String phone);

	long countByPhoneAndCreatedAtAfter(String phone, Instant after);

	@Modifying
	@Transactional
	@Query("delete from OtpChallenge o where o.createdAt < :before")
	int deleteOlderThan(Instant before);
}
