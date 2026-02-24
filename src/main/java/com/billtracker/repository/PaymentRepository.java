package com.billtracker.repository;

import com.billtracker.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<Payment> findByGroupId(Long groupId);
}
