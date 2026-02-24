package com.billtracker.repository;

import com.billtracker.entity.BillSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillSplitRepository extends JpaRepository<BillSplit, Long> {
    List<BillSplit> findByBillId(Long billId);
}
