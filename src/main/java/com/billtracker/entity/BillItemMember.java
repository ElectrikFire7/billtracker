package com.billtracker.entity;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemMember {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String userId;

    private String userName;

    private BigDecimal shareAmount;
}
