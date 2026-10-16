package ma.mizaje.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ma.mizaje.domain.Customer;
import ma.mizaje.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	/** Période la plus récente du client (c'est elle qui détermine l'état de sa carte). */
	Optional<Subscription> findFirstByCustomerOrderByActivatedAtDesc(Customer customer);

	@Query("select count(s) from Subscription s where s.expiresAt > :now and s.drinksLeft > 0")
	long countActive(Instant now);

	@Query("select coalesce(sum(s.pricePaid), 0) from Subscription s where s.activatedAt >= :from")
	long revenueSince(Instant from);

	long countByActivatedAtGreaterThanEqual(Instant from);

	long countByActivatedAtGreaterThanEqualAndShop(Instant from, String shop);

	@Query("select s from Subscription s join fetch s.customer where s.activatedAt = "
			+ "(select max(s2.activatedAt) from Subscription s2 where s2.customer = s.customer)")
	List<Subscription> findLatestPerCustomer();

	/** Toutes les périodes avec client et formule (tableau de bord : volumes modestes, agrégés en mémoire). */
	@Query("select s from Subscription s join fetch s.customer left join fetch s.plan")
	List<Subscription> findAllWithCustomerAndPlan();
}
