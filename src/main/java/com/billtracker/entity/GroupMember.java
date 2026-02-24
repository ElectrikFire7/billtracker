package com.billtracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "group_id", "user_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "date_of_joining", nullable = false)
    private LocalDateTime dateOfJoining;

    @PrePersist
    protected void onCreate() {
        if (dateOfJoining == null) {
            dateOfJoining = LocalDateTime.now();
        }
    }
}
