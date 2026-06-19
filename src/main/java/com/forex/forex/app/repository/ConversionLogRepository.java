package com.forex.forexapp.repository;

import com.forex.forexapp.model.AppUser;
import com.forex.forexapp.model.ConversionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConversionLogRepository extends JpaRepository<ConversionLog, Long> {

    // Existing queries
    List<ConversionLog> findTop10ByUserOrderByCreatedAtDesc(AppUser user);

    List<ConversionLog> findTop30ByUserAndFromCurrencyAndToCurrencyOrderByCreatedAtAsc(
        AppUser user, String fromCurrency, String toCurrency);

    List<ConversionLog> findByUserAndFromCurrencyAndToCurrencyOrderByCreatedAtDesc(
        AppUser user, String fromCurrency, String toCurrency);

    List<ConversionLog> findTop50ByFromCurrencyAndToCurrencyOrderByCreatedAtAsc(
        String fromCurrency, String toCurrency);

    // Dashboard queries
    long countByUser(AppUser user);

    @Query("SELECT c.fromCurrency, c.toCurrency, COUNT(c) as cnt " +
           "FROM ConversionLog c WHERE c.user = :user " +
           "GROUP BY c.fromCurrency, c.toCurrency ORDER BY cnt DESC")
    List<Object[]> findTopPairsByUser(@Param("user") AppUser user);

    @Query("SELECT MAX(c.rate) FROM ConversionLog c WHERE c.user = :user")
    Double findMaxRateByUser(@Param("user") AppUser user);

    @Query("SELECT MIN(c.rate) FROM ConversionLog c WHERE c.user = :user")
    Double findMinRateByUser(@Param("user") AppUser user);

    @Query("SELECT c FROM ConversionLog c WHERE c.user = :user " +
           "AND c.createdAt >= :since ORDER BY c.createdAt ASC")
    List<ConversionLog> findByUserSince(@Param("user") AppUser user,
                                         @Param("since") LocalDateTime since);
}