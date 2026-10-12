package ma.mizaj.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ma.mizaj.domain.Customer;
import ma.mizaj.domain.Redemption;
import ma.mizaj.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

	Optional<Redemption> findFirstBySubscriptionAndCancelledAtIsNullOrderByRedeemedAtDesc(Subscription subscription);

	List<Redemption> findTop10BySubscriptionCustomerAndCancelledAtIsNullOrderByRedeemedAtDesc(Customer customer);

	long countByRedeemedAtGreaterThanEqualAndCancelledAtIsNull(Instant from);

	long countByRedeemedAtGreaterThanEqualAndShopAndCancelledAtIsNull(Instant from, String shop);
}
