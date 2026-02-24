package com.billtracker.service;

import com.billtracker.dto.request.PaymentRequest;
import com.billtracker.dto.response.PaymentResponse;
import com.billtracker.entity.Group;
import com.billtracker.entity.Payment;
import com.billtracker.entity.User;
import com.billtracker.repository.GroupMemberRepository;
import com.billtracker.repository.GroupRepository;
import com.billtracker.repository.PaymentRepository;
import com.billtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public List<PaymentResponse> getPaymentsByGroup(Long groupId, User user) {
        ensureMember(groupId, user.getId());
        return paymentRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse createPayment(Long groupId, User user, PaymentRequest request) {
        ensureMember(groupId, user.getId());

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Recipient user not found"));

        ensureMember(groupId, toUser.getId());

        Payment payment = Payment.builder()
                .group(group)
                .fromUser(user)
                .toUser(toUser)
                .amount(request.getAmount())
                .build();

        payment = paymentRepository.save(payment);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse updatePayment(Long groupId, Long paymentId, User user, PaymentRequest request) {
        ensureMember(groupId, user.getId());
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Payment does not belong to this group");
        }

        if (!payment.getFromUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only edit payments that you recorded");
        }

        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new RuntimeException("Recipient user not found"));
        ensureMember(groupId, toUser.getId());

        payment.setToUser(toUser);
        payment.setAmount(request.getAmount());

        payment = paymentRepository.save(payment);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void deletePayment(Long groupId, Long paymentId, User user) {
        ensureMember(groupId, user.getId());
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Payment does not belong to this group");
        }

        if (!payment.getFromUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete payments that you recorded");
        }

        paymentRepository.delete(payment);
    }

    private void ensureMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("You are not a member of this group");
        }
    }
}
