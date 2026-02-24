package com.billtracker.controller;

import com.billtracker.dto.request.PaymentRequest;
import com.billtracker.dto.response.PaymentResponse;
import com.billtracker.entity.User;
import com.billtracker.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments(@PathVariable Long groupId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(paymentService.getPaymentsByGroup(groupId, user));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@PathVariable Long groupId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.createPayment(groupId, user, request));
    }

    @PutMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> updatePayment(@PathVariable Long groupId,
            @PathVariable Long paymentId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.updatePayment(groupId, paymentId, user, request));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long groupId,
            @PathVariable Long paymentId,
            @AuthenticationPrincipal User user) {
        paymentService.deletePayment(groupId, paymentId, user);
        return ResponseEntity.noContent().build();
    }
}
