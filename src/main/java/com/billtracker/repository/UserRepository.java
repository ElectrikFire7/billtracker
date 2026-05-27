package com.billtracker.repository;

import com.billtracker.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByOauthIdAndOauthProvider(String oauthId, String oauthProvider);
}
