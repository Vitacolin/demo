package com.llmledger.controller;

import com.llmledger.dto.FamilyCreateRequest;
import com.llmledger.dto.FamilyMemberAddRequest;
import com.llmledger.dto.FamilyMemberUpdateRequest;
import com.llmledger.dto.FamilyResponse;
import com.llmledger.entity.User;
import com.llmledger.service.FamilyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/families")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    private Long getUserId(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }

    @PostMapping
    public ResponseEntity<FamilyResponse> createFamily(@RequestBody FamilyCreateRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        FamilyResponse response = familyService.createFamily(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FamilyResponse>> getMyFamilies(Authentication authentication) {
        Long userId = getUserId(authentication);
        List<FamilyResponse> families = familyService.getFamiliesByUser(userId);
        return ResponseEntity.ok(families);
    }

    @GetMapping("/{familyId}")
    public ResponseEntity<FamilyResponse> getFamilyById(@PathVariable Long familyId, Authentication authentication) {
        Long userId = getUserId(authentication);
        if (!familyService.hasAccess(familyId, userId, com.llmledger.entity.FamilyMember.Role.VIEWER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        FamilyResponse family = familyService.getFamilyById(familyId);
        return ResponseEntity.ok(family);
    }

    @PostMapping("/{familyId}/members")
    public ResponseEntity<FamilyResponse> addMember(@PathVariable Long familyId,
            @RequestBody FamilyMemberAddRequest request, Authentication authentication) {
        Long userId = getUserId(authentication);
        FamilyResponse response = familyService.addMember(familyId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{familyId}/members/{memberId}")
    public ResponseEntity<FamilyResponse> updateMemberRole(@PathVariable Long familyId, @PathVariable Long memberId,
            @RequestBody FamilyMemberUpdateRequest request, Authentication authentication) {
        Long userId = getUserId(authentication);
        FamilyResponse response = familyService.updateMemberRole(familyId, memberId, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{familyId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long familyId, @PathVariable Long memberId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        familyService.removeMember(familyId, memberId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{familyId}")
    public ResponseEntity<Void> deleteFamily(@PathVariable Long familyId, Authentication authentication) {
        Long userId = getUserId(authentication);
        familyService.deleteFamily(familyId, userId);
        return ResponseEntity.noContent().build();
    }
}