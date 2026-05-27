package com.billtracker.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "group_members")
@CompoundIndex(name = "group_user_idx", def = "{'groupId': 1, 'userId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    private String id;

    private String groupId;

    private String userId;

    private LocalDateTime dateOfJoining;

    public void onCreate() {
        if (dateOfJoining == null) {
            dateOfJoining = LocalDateTime.now();
        }
    }
}
