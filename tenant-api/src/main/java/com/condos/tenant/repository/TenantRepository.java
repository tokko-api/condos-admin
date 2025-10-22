package com.condos.tenant.repository;

import com.condos.tenant.model.Tenant;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TenantRepository extends MongoRepository<Tenant, ObjectId> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
}