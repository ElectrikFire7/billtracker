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
    private final PaymentRepository paymentRepository;

    public List<GroupResponse> getUserGroups(User user) {
        List<String> groupIds = groupMemberRepository.findByUserId(user.getId()).stream()
                .map(GroupMember::getGroupId).toList();
        List<Group> groups = groupRepository.findAllByIdInOrderByCreatedAtDesc(groupIds);
        return groups.stream().map(g -> {
            int count = groupMemberRepository.findByGroupId(g.getId()).size();
            User creator = userRepository.findById(g.getCreatedById())
                    .orElseThrow(() -> new RuntimeException("Creator not found"));
            return GroupResponse.from(g, creator, count);
        }).toList();
    }

    @Transactional
    public GroupResponse createGroup(User user, GroupRequest request) {
        Group group = Group.builder()
                .name(request.getName())
                .createdById(user.getId())
                .build();
        group = groupRepository.save(group);

        // Add creator as member
        GroupMember member = GroupMember.builder()
                .groupId(group.getId())
                .userId(user.getId())
                .dateOfJoining(LocalDateTime.now())
                .build();
        groupMemberRepository.save(member);

        return GroupResponse.from(group, user, 1);
    }

    public GroupResponse getGroup(String groupId, User user) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        ensureMember(groupId, user.getId());
        int count = groupMemberRepository.findByGroupId(groupId).size();
        User creator = userRepository.findById(group.getCreatedById())
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        return GroupResponse.from(group, creator, count);
    }

    @Transactional
    public GroupResponse addMember(String groupId, User user, AddMemberRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        ensureMember(groupId, user.getId());

        User newUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, newUser.getId())) {
            throw new RuntimeException("User is already a member of this group");
        }

        GroupMember member = GroupMember.builder()
                .groupId(group.getId())
                .userId(newUser.getId())
                .dateOfJoining(LocalDateTime.now())
                .build();
        groupMemberRepository.save(member);

        int count = groupMemberRepository.findByGroupId(groupId).size();
        User creator = userRepository.findById(group.getCreatedById())
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        return GroupResponse.from(group, creator, count);
    }

    @Transactional
    public void removeMember(String groupId, String memberId, User user) {
        ensureMember(groupId, user.getId());
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (!member.getGroupId().equals(groupId)) {
            throw new RuntimeException("Member does not belong to this group");
        }
        groupMemberRepository.delete(member);
    }

    public List<UserResponse> getMembers(String groupId, User user) {
        ensureMember(groupId, user.getId());
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(gm -> userRepository.findById(gm.getUserId()).orElseThrow())
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public void deleteGroup(String groupId, User user) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreatedById().equals(user.getId())) {
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

    public List<BalanceResponse> calculateBalances(String groupId, User user) {
        ensureMember(groupId, user.getId());

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        List<Bill> bills = billRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        List<Payment> payments = paymentRepository.findByGroupId(groupId);

        // Map: userId -> net amount (positive = they are owed, negative = they owe)
        Map<String, BigDecimal> netMap = new HashMap<>();
        for (GroupMember m : members) {
            netMap.put(m.getUserId(), BigDecimal.ZERO);
        }

        // Process bills
        for (Bill bill : bills) {
            String paidById = bill.getPaidById();
            // The payer paid the full amount
            netMap.merge(paidById, bill.getFinalAmount(), BigDecimal::add);

            // Each split user owes their share
            for (var split : bill.getBillSplits()) {
                netMap.merge(split.getUserId(), split.getAmountOwed().negate(), BigDecimal::add);
            }
        }

        // Process payments
        for (Payment payment : payments) {
            netMap.merge(payment.getFromUserId(), payment.getAmount(), BigDecimal::add);
            netMap.merge(payment.getToUserId(), payment.getAmount().negate(), BigDecimal::add);
        }

        // Build per-user balance relative to the requesting user
        List<BalanceResponse> result = new ArrayList<>();
        Map<String, String> nameMap = new HashMap<>();
        for (GroupMember m : members) {
            User u = userRepository.findById(m.getUserId()).orElseThrow();
            nameMap.put(u.getId(), u.getName());
        }

        // Compute pairwise balances from bills and payments
        Map<String, BigDecimal> pairwise = new HashMap<>();
        for (Bill bill : bills) {
            String paidById = bill.getPaidById();
            for (var split : bill.getBillSplits()) {
                String owerId = split.getUserId();
                if (paidById.equals(user.getId()) && !owerId.equals(user.getId())) {
                    pairwise.merge(owerId, split.getAmountOwed(), BigDecimal::add);
                } else if (owerId.equals(user.getId()) && !paidById.equals(user.getId())) {
                    pairwise.merge(paidById, split.getAmountOwed().negate(), BigDecimal::add);
                }
            }
        }

        for (Payment payment : payments) {
            if (payment.getFromUserId().equals(user.getId())) {
                pairwise.merge(payment.getToUserId(), payment.getAmount(), BigDecimal::add);
            } else if (payment.getToUserId().equals(user.getId())) {
                pairwise.merge(payment.getFromUserId(), payment.getAmount().negate(), BigDecimal::add);
            }
        }

        for (GroupMember m : members) {
            if (m.getUserId().equals(user.getId()))
                continue;
            BigDecimal net = pairwise.getOrDefault(m.getUserId(), BigDecimal.ZERO);
            result.add(BalanceResponse.builder()
                    .userId(m.getUserId())
                    .userName(nameMap.get(m.getUserId()))
                    .netBalance(net)
                    .build());
        }

        return result;
    }

    private void ensureMember(String groupId, String userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("You are not a member of this group");
        }
    }
}
