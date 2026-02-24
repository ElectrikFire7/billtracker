package com.billtracker.dto.response;

import com.billtracker.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long groupId;
    private UserResponse fromUser;
    private UserResponse toUser;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .groupId(payment.getGroup().getId())
                .fromUser(UserResponse.from(payment.getFromUser()))
                .toUser(UserResponse.from(payment.getToUser()))
                .amount(payment.getAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
