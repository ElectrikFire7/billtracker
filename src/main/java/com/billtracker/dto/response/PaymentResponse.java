package com.billtracker.dto.response;

import com.billtracker.entity.Payment;
import com.billtracker.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private String groupId;
    private UserResponse fromUser;
    private UserResponse toUser;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment payment, User fromUser, User toUser) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .groupId(payment.getGroupId())
                .fromUser(UserResponse.from(fromUser))
                .toUser(UserResponse.from(toUser))
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
