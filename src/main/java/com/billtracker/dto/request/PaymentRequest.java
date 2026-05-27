package com.billtracker.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull
    private String toUserId;

    @NotNull
    private BigDecimal amount;
}
