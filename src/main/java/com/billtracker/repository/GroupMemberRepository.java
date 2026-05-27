package com.billtracker.repository;

import com.billtracker.entity.GroupMember;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends MongoRepository<GroupMember, String> {
    List<GroupMember> findByGroupId(String groupId);

    List<GroupMember> findByUserId(String userId);

    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);

    boolean existsByGroupIdAndUserId(String groupId, String userId);

    void deleteByGroupIdAndUserId(String groupId, String userId);
}
