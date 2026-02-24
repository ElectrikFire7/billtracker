package com.billtracker.repository;

import com.billtracker.entity.BillItemMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillItemMemberRepository extends JpaRepository<BillItemMember, Long> {
}
