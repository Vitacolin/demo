package com.llmledger.service;

import com.llmledger.dto.FamilyCreateRequest;
import com.llmledger.dto.FamilyMemberAddRequest;
import com.llmledger.dto.FamilyMemberUpdateRequest;
import com.llmledger.dto.FamilyResponse;
import com.llmledger.entity.Family;
import com.llmledger.entity.FamilyMember;
import com.llmledger.entity.User;
import com.llmledger.repository.FamilyMemberRepository;
import com.llmledger.repository.FamilyRepository;
import com.llmledger.repository.TransactionRepository;
import com.llmledger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public FamilyService(FamilyRepository familyRepository, FamilyMemberRepository familyMemberRepository,
            UserRepository userRepository, TransactionRepository transactionRepository) {
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public FamilyResponse createFamily(FamilyCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Family family = new Family();
        family.setName(request.getName());
        family.setCreatedBy(user);

        FamilyMember owner = new FamilyMember();
        owner.setUser(user);
        owner.setRole(FamilyMember.Role.OWNER);
        family.addMember(owner);

        Family savedFamily = familyRepository.save(family);
        return toFamilyResponse(savedFamily);
    }

    public List<FamilyResponse> getFamiliesByUser(Long userId) {
        List<FamilyMember> memberships = familyMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(member -> familyRepository.findById(member.getFamily().getId()).orElse(null))
                .filter(family -> family != null)
                .map(this::toFamilyResponse)
                .collect(Collectors.toList());
    }

    public FamilyResponse getFamilyById(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NoSuchElementException("Family not found"));
        return toFamilyResponse(family);
    }

    @Transactional
    public FamilyResponse addMember(Long familyId, FamilyMemberAddRequest request, Long requesterId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NoSuchElementException("Family not found"));

        FamilyMember requester = familyMemberRepository.findByFamilyIdAndUserId(familyId, requesterId)
                .orElseThrow(() -> new SecurityException("You are not a member of this family"));

        if (!hasPermission(requester, FamilyMember.Role.ADMIN)) {
            throw new SecurityException("Only ADMIN can add members");
        }

        User newMember = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (familyMemberRepository.existsByFamilyIdAndUserId(familyId, newMember.getId())) {
            throw new IllegalArgumentException("User is already a member of this family");
        }

        FamilyMember member = new FamilyMember();
        member.setUser(newMember);
        member.setRole(FamilyMember.Role.valueOf(request.getRole().toUpperCase()));
        family.addMember(member);

        Family savedFamily = familyRepository.save(family);
        return toFamilyResponse(savedFamily);
    }

    @Transactional
    public FamilyResponse updateMemberRole(Long familyId, Long memberId, FamilyMemberUpdateRequest request,
            Long requesterId) {
        FamilyMember requester = familyMemberRepository.findByFamilyIdAndUserId(familyId, requesterId)
                .orElseThrow(() -> new SecurityException("You are not a member of this family"));

        if (!hasPermission(requester, FamilyMember.Role.ADMIN)) {
            throw new SecurityException("Only ADMIN can update member roles");
        }

        FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found"));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new IllegalArgumentException("Member does not belong to this family");
        }

        member.setRole(FamilyMember.Role.valueOf(request.getRole().toUpperCase()));
        familyMemberRepository.save(member);

        return toFamilyResponse(familyRepository.findById(familyId).orElseThrow());
    }

    @Transactional
    public void removeMember(Long familyId, Long memberId, Long requesterId) {
        FamilyMember requester = familyMemberRepository.findByFamilyIdAndUserId(familyId, requesterId)
                .orElseThrow(() -> new SecurityException("You are not a member of this family"));

        if (!hasPermission(requester, FamilyMember.Role.ADMIN)) {
            throw new SecurityException("Only ADMIN can remove members");
        }

        FamilyMember member = familyMemberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found"));

        if (!member.getFamily().getId().equals(familyId)) {
            throw new IllegalArgumentException("Member does not belong to this family");
        }

        familyMemberRepository.delete(member);
    }

    @Transactional
    public void deleteFamily(Long familyId, Long requesterId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NoSuchElementException("Family not found"));

        if (!family.getCreatedBy().getId().equals(requesterId)) {
            throw new SecurityException("Only owner can delete family");
        }

        familyRepository.delete(family);
    }

    public boolean hasAccess(Long familyId, Long userId, FamilyMember.Role requiredRole) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .map(member -> hasPermission(member, requiredRole))
                .orElse(false);
    }

    private boolean hasPermission(FamilyMember member, FamilyMember.Role requiredRole) {
        FamilyMember.Role memberRole = member.getRole();
        return memberRole.ordinal() <= requiredRole.ordinal();
    }

    private FamilyResponse toFamilyResponse(Family family) {
        List<FamilyResponse.MemberResponse> members = family.getMembers().stream()
                .map(member -> new FamilyResponse.MemberResponse(
                        member.getUser().getId(),
                        member.getUser().getUsername(),
                        member.getRole().name()))
                .collect(Collectors.toList());

        // 计算家庭账单数
        Long transactionCount = transactionRepository.countByFamilyId(family.getId());

        return new FamilyResponse(
                family.getId(),
                family.getName(),
                family.getCreatedAt(),
                family.getCreatedBy().getId(),
                members,
                transactionCount);
    }
}