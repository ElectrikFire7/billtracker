package com.billtracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class BalanceResponse {
    private String userId;
    private String userName;
    private BigDecimal netBalance; // positive = owed to you, negative = you owe
}
