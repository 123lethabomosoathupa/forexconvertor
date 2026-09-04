package com.forex.transaction.repository;

import com.forex.transaction.document.RateAlert;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RateAlertRepository extends MongoRepository<RateAlert, String> {

    List<RateAlert> findByUserIdOrderByCreatedAtDesc(String userId);

    List<RateAlert> findByTriggeredFalse();
}
