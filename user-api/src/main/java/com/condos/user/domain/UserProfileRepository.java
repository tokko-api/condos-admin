package com.condos.user.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    boolean existsByEmail(String email);
    List<UserProfile> findByEmailContainingIgnoreCase(String q);
    Optional<UserProfile> findByEmail(String email);
}