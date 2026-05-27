package com.billtracker.controller;

import com.billtracker.dto.request.BillRequest;
import com.billtracker.dto.response.BillResponse;
import com.billtracker.entity.User;
import com.billtracker.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @GetMapping
    public ResponseEntity<List<BillResponse>> getBills(@PathVariable String groupId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(billService.getBillsByGroup(groupId, user));
    }

    @GetMapping("/{billId}")
    public ResponseEntity<BillResponse> getBill(@PathVariable String groupId,
            @PathVariable String billId) {
        return ResponseEntity.ok(billService.getBill(billId));
    }

    @PostMapping
    public ResponseEntity<BillResponse> createBill(@PathVariable String groupId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BillRequest request) {
        return ResponseEntity.ok(billService.createBill(groupId, user, request));
    }

    @PutMapping("/{billId}")
    public ResponseEntity<BillResponse> updateBill(@PathVariable String groupId,
            @PathVariable String billId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BillRequest request) {
        return ResponseEntity.ok(billService.updateBill(billId, user, request));
    }

    @DeleteMapping("/{billId}")
    public ResponseEntity<Void> deleteBill(@PathVariable String groupId,
            @PathVariable String billId,
            @AuthenticationPrincipal User user) {
        billService.deleteBill(billId, user);
        return ResponseEntity.noContent().build();
    }
}
