package com.billtracker.entity;

import com.billtracker.enums.SplitType;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    private String id;

    private String groupId;

    private String paidById;

    private BigDecimal finalAmount;

    private String description;

    private SplitType splitType;

    @Builder.Default
    private List<BillItem> billItems = new ArrayList<>();

    @Builder.Default
    private List<BillSplit> billSplits = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
