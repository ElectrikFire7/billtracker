package com.billtracker.service;

import com.billtracker.dto.request.BillRequest;
import com.billtracker.dto.response.BillResponse;
import com.billtracker.entity.*;
import com.billtracker.enums.SplitType;
import com.billtracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public List<BillResponse> getBillsByGroup(Long groupId, User user) {
        ensureMember(groupId, user.getId());
        List<Bill> bills = billRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        return bills.stream().map(BillResponse::from).toList();
    }

    public BillResponse getBill(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        return BillResponse.from(bill);
    }

    @Transactional
    public BillResponse createBill(Long groupId, User user, BillRequest request) {
        ensureMember(groupId, user.getId());

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User payer = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        // Validate splits sum to finalAmount
        validateSplits(request);

        Bill bill = Bill.builder()
                .group(group)
                .paidBy(payer)
                .finalAmount(request.getFinalAmount())
                .description(request.getDescription())
                .splitType(request.getSplitType())
                .build();

        // Add items
        if (request.getItems() != null) {
            for (BillRequest.BillItemRequest itemReq : request.getItems()) {
                BillItem item = BillItem.builder()
                        .bill(bill)
                        .itemName(itemReq.getItemName())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .totalPrice(itemReq.getTotalPrice())
                        .build();

                if (itemReq.getMembers() != null) {
                    for (BillRequest.BillItemMemberRequest memberReq : itemReq.getMembers()) {
                        User memberUser = userRepository.findById(memberReq.getUserId())
                                .orElseThrow(() -> new RuntimeException("User not found"));
                        BillItemMember bim = BillItemMember.builder()
                                .billItem(item)
                                .user(memberUser)
                                .shareAmount(memberReq.getShareAmount())
                                .build();
                        item.getBillItemMembers().add(bim);
                    }
                }

                bill.getBillItems().add(item);
            }
        }

        // Add splits
        for (BillRequest.BillSplitRequest splitReq : request.getSplits()) {
            User splitUser = userRepository.findById(splitReq.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            BillSplit split = BillSplit.builder()
                    .bill(bill)
                    .user(splitUser)
                    .amountOwed(splitReq.getAmountOwed())
                    .percentage(splitReq.getPercentage())
                    .build();
            bill.getBillSplits().add(split);
        }

        bill = billRepository.save(bill);
        return BillResponse.from(bill);
    }

    @Transactional
    public BillResponse updateBill(Long billId, User user, BillRequest request) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        ensureMember(bill.getGroup().getId(), user.getId());

        User payer = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        validateSplits(request);

        bill.setPaidBy(payer);
        bill.setFinalAmount(request.getFinalAmount());
        bill.setDescription(request.getDescription());
        bill.setSplitType(request.getSplitType());

        // Clear collections and flush to execute deletes before inserts (prevents
        // unique constraint violations)
        bill.getBillItems().clear();
        bill.getBillSplits().clear();
        billRepository.flush();

        // Re-add items
        if (request.getItems() != null) {
            for (BillRequest.BillItemRequest itemReq : request.getItems()) {
                BillItem item = BillItem.builder()
                        .bill(bill)
                        .itemName(itemReq.getItemName())
                        .quantity(itemReq.getQuantity())
                        .unitPrice(itemReq.getUnitPrice())
                        .totalPrice(itemReq.getTotalPrice())
                        .build();

                if (itemReq.getMembers() != null) {
                    for (BillRequest.BillItemMemberRequest memberReq : itemReq.getMembers()) {
                        User memberUser = userRepository.findById(memberReq.getUserId())
                                .orElseThrow(() -> new RuntimeException("User not found"));
                        BillItemMember bim = BillItemMember.builder()
                                .billItem(item)
                                .user(memberUser)
                                .shareAmount(memberReq.getShareAmount())
                                .build();
                        item.getBillItemMembers().add(bim);
                    }
                }

                bill.getBillItems().add(item);
            }
        }

        // Re-add splits
        for (BillRequest.BillSplitRequest splitReq : request.getSplits()) {
            User splitUser = userRepository.findById(splitReq.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            BillSplit split = BillSplit.builder()
                    .bill(bill)
                    .user(splitUser)
                    .amountOwed(splitReq.getAmountOwed())
                    .percentage(splitReq.getPercentage())
                    .build();
            bill.getBillSplits().add(split);
        }

        bill = billRepository.save(bill);
        return BillResponse.from(bill);
    }

    @Transactional
    public void deleteBill(Long billId, User user) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        ensureMember(bill.getGroup().getId(), user.getId());
        billRepository.delete(bill);
    }

    private void validateSplits(BillRequest request) {
        BigDecimal totalSplits = request.getSplits().stream()
                .map(BillRequest.BillSplitRequest::getAmountOwed)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSplits.compareTo(request.getFinalAmount()) != 0) {
            throw new RuntimeException(
                    "Split amounts (" + totalSplits + ") do not add up to the final amount (" + request.getFinalAmount()
                            + ")");
        }

        if (request.getSplitType() == SplitType.PERCENTAGE) {
            BigDecimal totalPercentage = request.getSplits().stream()
                    .map(s -> s.getPercentage() != null ? s.getPercentage() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new RuntimeException("Percentages must add up to 100%");
            }
        }
    }

    private void ensureMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("You are not a member of this group");
        }
    }
}
