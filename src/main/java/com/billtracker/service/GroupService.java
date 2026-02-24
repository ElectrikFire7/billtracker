package com.billtracker.service;

import com.billtracker.dto.request.AddMemberRequest;
import com.billtracker.dto.request.GroupRequest;
import com.billtracker.dto.response.BalanceResponse;
import com.billtracker.dto.response.GroupResponse;
import com.billtracker.dto.response.UserResponse;
import com.billtracker.entity.*;
import com.billtracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final BillRepository billRepository;
    private final BillSplitRepository billSplitRepository;
    private final PaymentRepository paymentRepository;

    public List<GroupResponse> getUserGroups(User user) {
        List<Group> groups = groupRepository.findGroupsByUserId(user.getId());
        return groups.stream().map(g -> {
            int count = groupMemberRepository.findByGroupId(g.getId()).size();
            return GroupResponse.from(g, count);
        }).toList();
    }

    @Transactional
    public GroupResponse createGroup(User user, GroupRequest request) {
        Group group = Group.builder()
                .name(request.getName())
                .createdBy(user)
                .build();
        group = groupRepository.save(group);

        // Add creator as member
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(user)
                .dateOfJoining(LocalDateTime.now())
                .build();
        groupMemberRepository.save(member);

        return GroupResponse.from(group, 1);
    }

    public GroupResponse getGroup(Long groupId, User user) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        ensureMember(groupId, user.getId());
        int count = groupMemberRepository.findByGroupId(groupId).size();
        return GroupResponse.from(group, count);
    }

    @Transactional
    public GroupResponse addMember(Long groupId, User user, AddMemberRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        ensureMember(groupId, user.getId());

        User newUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, newUser.getId())) {
            throw new RuntimeException("User is already a member of this group");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(newUser)
                .dateOfJoining(LocalDateTime.now())
                .build();
        groupMemberRepository.save(member);

        int count = groupMemberRepository.findByGroupId(groupId).size();
        return GroupResponse.from(group, count);
    }

    @Transactional
    public void removeMember(Long groupId, Long memberId, User user) {
        ensureMember(groupId, user.getId());
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (!member.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Member does not belong to this group");
        }
        groupMemberRepository.delete(member);
    }

    public List<UserResponse> getMembers(Long groupId, User user) {
        ensureMember(groupId, user.getId());
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(gm -> UserResponse.from(gm.getUser()))
                .toList();
    }

    @Transactional
    public void deleteGroup(Long groupId, User user) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreatedBy().getId().equals(user.getId())) {
            throw new RuntimeException("Only the group creator can delete the group");
        }

        // Check unsettled balances
        List<BalanceResponse> balances = calculateBalances(groupId, user);
        boolean hasUnsettled = balances.stream()
                .anyMatch(b -> b.getNetBalance().abs().compareTo(BigDecimal.valueOf(0.01)) > 0);

        if (hasUnsettled) {
            throw new RuntimeException(
                    "Cannot delete group: there are unsettled balances. All debts must be settled first.");
        }

        groupRepository.delete(group);
    }

    public List<BalanceResponse> calculateBalances(Long groupId, User user) {
        ensureMember(groupId, user.getId());

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        List<Bill> bills = billRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        List<Payment> payments = paymentRepository.findByGroupId(groupId);

        // Map: userId -> net amount (positive = they are owed, negative = they owe)
        Map<Long, BigDecimal> netMap = new HashMap<>();
        for (GroupMember m : members) {
            netMap.put(m.getUser().getId(), BigDecimal.ZERO);
        }

        // Process bills
        for (Bill bill : bills) {
            Long paidById = bill.getPaidBy().getId();
            // The payer paid the full amount
            netMap.merge(paidById, bill.getFinalAmount(), BigDecimal::add);

            // Each split user owes their share
            for (var split : bill.getBillSplits()) {
                netMap.merge(split.getUser().getId(), split.getAmountOwed().negate(), BigDecimal::add);
            }
        }

        // Process payments
        for (Payment payment : payments) {
            netMap.merge(payment.getFromUser().getId(), payment.getAmount(), BigDecimal::add);
            netMap.merge(payment.getToUser().getId(), payment.getAmount().negate(), BigDecimal::add);
        }

        // Build per-user balance relative to the requesting user
        // For the requesting user, show balance with each other member
        // positive = other user owes you, negative = you owe other user
        BigDecimal myNet = netMap.getOrDefault(user.getId(), BigDecimal.ZERO);

        List<BalanceResponse> result = new ArrayList<>();
        Map<Long, String> nameMap = new HashMap<>();
        for (GroupMember m : members) {
            nameMap.put(m.getUser().getId(), m.getUser().getName());
        }

        // Compute pairwise balances from bills and payments
        // Simpler approach: for each other member, compute net between current user and
        // that member
        Map<Long, BigDecimal> pairwise = new HashMap<>();
        for (Bill bill : bills) {
            Long paidById = bill.getPaidBy().getId();
            for (var split : bill.getBillSplits()) {
                Long owerId = split.getUser().getId();
                if (paidById.equals(user.getId()) && !owerId.equals(user.getId())) {
                    // Other user owes me
                    pairwise.merge(owerId, split.getAmountOwed(), BigDecimal::add);
                } else if (owerId.equals(user.getId()) && !paidById.equals(user.getId())) {
                    // I owe other user
                    pairwise.merge(paidById, split.getAmountOwed().negate(), BigDecimal::add);
                }
            }
        }

        for (Payment payment : payments) {
            if (payment.getFromUser().getId().equals(user.getId())) {
                // I paid someone => increases what they owe me (or reduces what I owe them)
                pairwise.merge(payment.getToUser().getId(), payment.getAmount(), BigDecimal::add);
            } else if (payment.getToUser().getId().equals(user.getId())) {
                // Someone paid me => reduce what they owe me
                pairwise.merge(payment.getFromUser().getId(), payment.getAmount().negate(), BigDecimal::add);
            }
        }

        for (GroupMember m : members) {
            if (m.getUser().getId().equals(user.getId()))
                continue;
            BigDecimal net = pairwise.getOrDefault(m.getUser().getId(), BigDecimal.ZERO);
            result.add(BalanceResponse.builder()
                    .userId(m.getUser().getId())
                    .userName(m.getUser().getName())
                    .netBalance(net)
                    .build());
        }

        return result;
    }

    private void ensureMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("You are not a member of this group");
        }
    }
}
