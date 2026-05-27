package com.billtracker.controller;

import com.billtracker.dto.request.AddMemberRequest;
import com.billtracker.dto.request.GroupRequest;
import com.billtracker.dto.response.BalanceResponse;
import com.billtracker.dto.response.GroupResponse;
import com.billtracker.dto.response.UserResponse;
import com.billtracker.entity.User;
import com.billtracker.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getUserGroups(user));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@AuthenticationPrincipal User user,
            @Valid @RequestBody GroupRequest request) {
        return ResponseEntity.ok(groupService.createGroup(user, request));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable String groupId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getGroup(groupId, user));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupId,
            @AuthenticationPrincipal User user) {
        groupService.deleteGroup(groupId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable String groupId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.ok(groupService.addMember(groupId, user, request));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<UserResponse>> getMembers(@PathVariable String groupId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getMembers(groupId, user));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable String groupId,
            @PathVariable String memberId,
            @AuthenticationPrincipal User user) {
        groupService.removeMember(groupId, memberId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/balances")
    public ResponseEntity<List<BalanceResponse>> getBalances(@PathVariable String groupId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.calculateBalances(groupId, user));
    }
}
