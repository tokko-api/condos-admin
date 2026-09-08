package com.condos.billing.repository;

import com.condos.billing.model.UnitCredit;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UnitCreditRepository extends MongoRepository<UnitCredit, String> {
}
