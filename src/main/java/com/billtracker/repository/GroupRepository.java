package com.billtracker.repository;

import com.billtracker.entity.Group;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GroupRepository extends MongoRepository<Group, String> {

    List<Group> findAllByIdInOrderByCreatedAtDesc(List<String> ids);
}
