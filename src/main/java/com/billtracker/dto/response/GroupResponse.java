package com.billtracker.dto.response;

import com.billtracker.entity.Group;
import com.billtracker.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class GroupResponse {
    private String id;
    private String name;
    private UserResponse createdBy;
    private LocalDateTime createdAt;
    private int memberCount;

    public static GroupResponse from(Group group, User createdBy, int memberCount) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .createdBy(UserResponse.from(createdBy))
                .createdAt(group.getCreatedAt())
                .memberCount(memberCount)
                .build();
    }
}
