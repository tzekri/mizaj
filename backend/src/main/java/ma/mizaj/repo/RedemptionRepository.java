package ma.mizaj.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.Redemption;
import ma.mizaj.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

	Optional<Redemption> findFirstBySubscriptionAndCancelledAtIsNullOrderByRedeemedAtDesc(Subscription subscription);

	List<Redemption> findTop10BySubscriptionCustomerAndCancelledAtIsNullOrderByRedeemedAtDesc(Customer customer);

	long countByRedeemedAtGreaterThanEqualAndCancelledAtIsNull(Instant from);

	long countByRedeemedAtGreaterThanEqualAndShopAndCancelledAtIsNull(Instant from, String shop);

	/** Validations non annulées sur [from, to), avec abonnement, client, formule et barista (tableau de bord, export). */
	@Query("select r from Redemption r join fetch r.subscription s join fetch s.customer left join fetch s.plan join fetch r.staff "
			+ "where r.redeemedAt >= :from and r.redeemedAt < :to and r.cancelledAt is null order by r.redeemedAt")
	List<Redemption> findInPeriod(Instant from, Instant to);
}
