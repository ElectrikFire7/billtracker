package com.billtracker.repository;

import com.billtracker.entity.Bill;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BillRepository extends MongoRepository<Bill, String> {
    List<Bill> findByGroupIdOrderByCreatedAtDesc(String groupId);
}
