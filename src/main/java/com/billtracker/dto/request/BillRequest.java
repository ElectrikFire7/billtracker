package com.billtracker.dto.request;

import com.billtracker.enums.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BillRequest {

    @NotNull
    private String paidByUserId;

    @NotNull
    private BigDecimal finalAmount;

    @NotBlank
    private String description;

    @NotNull
    private SplitType splitType;

    @Valid
    private List<BillItemRequest> items;

    @Valid
    @NotNull
    private List<BillSplitRequest> splits;

    @Data
    public static class BillItemRequest {
        @NotBlank
        private String itemName;
        @NotNull
        private Integer quantity;
        @NotNull
        private BigDecimal unitPrice;
        @NotNull
        private BigDecimal totalPrice;

        @Valid
        private List<BillItemMemberRequest> members;
    }

    @Data
    public static class BillItemMemberRequest {
        @NotNull
        private String userId;
        @NotNull
        private BigDecimal shareAmount;
    }

    @Data
    public static class BillSplitRequest {
        @NotNull
        private String userId;
        @NotNull
        private BigDecimal amountOwed;
        private BigDecimal percentage;
    }
}
