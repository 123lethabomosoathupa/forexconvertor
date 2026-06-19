package com.forex.forexapp.repository;

import com.forex.forexapp.model.AppUser;
import com.forex.forexapp.model.RateAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RateAlertRepository extends JpaRepository<RateAlert, Long> {

    List<RateAlert> findByUserAndTriggeredFalse(AppUser user);

    List<RateAlert> findByUser(AppUser user);

    List<RateAlert> findByFromCurrencyAndToCurrencyAndTriggeredFalse(
        String fromCurrency, String toCurrency);
}