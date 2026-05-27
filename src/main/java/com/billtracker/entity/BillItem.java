package com.billtracker.entity;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItem {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String itemName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    @Builder.Default
    private List<BillItemMember> billItemMembers = new ArrayList<>();
}
