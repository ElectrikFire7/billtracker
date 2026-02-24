package com.billtracker.dto.response;

import com.billtracker.entity.Bill;
import com.billtracker.entity.BillItem;
import com.billtracker.entity.BillItemMember;
import com.billtracker.entity.BillSplit;
import com.billtracker.enums.SplitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class BillResponse {
    private Long id;
    private Long groupId;
    private UserResponse paidBy;
    private BigDecimal finalAmount;
    private String description;
    private SplitType splitType;
    private List<BillItemResponse> items;
    private List<BillSplitResponse> splits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    public static class BillItemResponse {
        private Long id;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private List<BillItemMemberResponse> members;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class BillItemMemberResponse {
        private Long id;
        private Long userId;
        private String userName;
        private BigDecimal shareAmount;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class BillSplitResponse {
        private Long id;
        private Long userId;
        private String userName;
        private BigDecimal amountOwed;
        private BigDecimal percentage;
    }

    public static BillResponse from(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .groupId(bill.getGroup().getId())
                .paidBy(UserResponse.from(bill.getPaidBy()))
                .finalAmount(bill.getFinalAmount())
                .description(bill.getDescription())
                .splitType(bill.getSplitType())
                .items(bill.getBillItems().stream().map(BillResponse::mapItem).toList())
                .splits(bill.getBillSplits().stream().map(BillResponse::mapSplit).toList())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }

    private static BillItemResponse mapItem(BillItem item) {
        return BillItemResponse.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .members(item.getBillItemMembers().stream().map(m -> BillItemMemberResponse.builder()
                        .id(m.getId())
                        .userId(m.getUser().getId())
                        .userName(m.getUser().getName())
                        .shareAmount(m.getShareAmount())
                        .build()).toList())
                .build();
    }

    private static BillSplitResponse mapSplit(BillSplit split) {
        return BillSplitResponse.builder()
                .id(split.getId())
                .userId(split.getUser().getId())
                .userName(split.getUser().getName())
                .amountOwed(split.getAmountOwed())
                .percentage(split.getPercentage())
                .build();
    }
}
