package com.billtracker.repository;

import com.billtracker.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN GroupMember gm ON gm.group = g WHERE gm.user.id = :userId ORDER BY g.createdAt DESC")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);
}
