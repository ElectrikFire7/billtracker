package com.billtracker.repository;

import com.billtracker.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByGroupIdOrderByCreatedAtDesc(String groupId);

    List<Payment> findByGroupId(String groupId);
}
