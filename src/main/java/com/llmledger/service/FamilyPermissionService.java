package com.llmledger.service;

import com.llmledger.entity.Family;
import com.llmledger.entity.FamilyMember;
import com.llmledger.entity.Transaction;
import com.llmledger.repository.FamilyMemberRepository;
import com.llmledger.repository.FamilyRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FamilyPermissionService {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRepository familyRepository;

    public FamilyPermissionService(FamilyMemberRepository familyMemberRepository,
            FamilyRepository familyRepository) {
        this.familyMemberRepository = familyMemberRepository;
        this.familyRepository = familyRepository;
    }

    /**
     * 检查用户是否有权限查看家庭账本的交易
     */
    public boolean canViewTransactions(Long familyId, Long userId) {
        // 所有者、管理员、成员、查看者都可以查看
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId).isPresent();
    }

    /**
     * 检查用户是否有权限在家庭账本中创建交易
     */
    public boolean canCreateTransaction(Long familyId, Long userId) {
        Optional<FamilyMember> member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId);
        if (member.isEmpty()) {
            return false;
        }

        FamilyMember.Role role = member.get().getRole();
        // 所有者、管理员、成员可以创建交易
        return role == FamilyMember.Role.OWNER ||
                role == FamilyMember.Role.ADMIN ||
                role == FamilyMember.Role.MEMBER;
    }

    /**
     * 检查用户是否有权限删除家庭账本中的交易
     * 规则：谁创建谁可删
     */
    public boolean canDeleteTransaction(Transaction transaction, Long userId) {
        // 如果交易不属于任何家庭账本，使用普通权限
        if (transaction.getFamilyId() == null) {
            return transaction.getOwner().getId().equals(userId);
        }

        Optional<FamilyMember> member = familyMemberRepository.findByFamilyIdAndUserId(
                transaction.getFamilyId(), userId);

        if (member.isEmpty()) {
            return false;
        }

        FamilyMember.Role role = member.get().getRole();

        // 所有者、管理员可以删除任何交易
        if (role == FamilyMember.Role.OWNER || role == FamilyMember.Role.ADMIN) {
            return true;
        }

        // 成员和查看者只能删除自己创建的交易
        return transaction.getOwner().getId().equals(userId);
    }

    /**
     * 检查用户是否有权限修改家庭账本中的交易
     * 规则：谁创建谁可改
     */
    public boolean canUpdateTransaction(Transaction transaction, Long userId) {
        // 如果交易不属于任何家庭账本，使用普通权限
        if (transaction.getFamilyId() == null) {
            return transaction.getOwner().getId().equals(userId);
        }

        Optional<FamilyMember> member = familyMemberRepository.findByFamilyIdAndUserId(
                transaction.getFamilyId(), userId);

        if (member.isEmpty()) {
            return false;
        }

        FamilyMember.Role role = member.get().getRole();

        // 所有者、管理者可以修改任何交易
        if (role == FamilyMember.Role.OWNER || role == FamilyMember.Role.ADMIN) {
            return true;
        }

        // 成员只能修改自己创建的交易
        // 查看者不能修改任何交易
        return role == FamilyMember.Role.MEMBER && transaction.getOwner().getId().equals(userId);
    }

    /**
     * 检查用户是否有权限管理家庭成员
     */
    public boolean canManageMembers(Long familyId, Long userId) {
        Optional<FamilyMember> member = familyMemberRepository.findByFamilyIdAndUserId(familyId, userId);
        if (member.isEmpty()) {
            return false;
        }

        FamilyMember.Role role = member.get().getRole();
        // 只有所有者和管理员可以管理成员
        return role == FamilyMember.Role.OWNER || role == FamilyMember.Role.ADMIN;
    }

    /**
     * 检查用户是否是家庭的所有者
     */
    public boolean isOwner(Long familyId, Long userId) {
        Optional<Family> family = familyRepository.findById(familyId);
        return family.isPresent() && family.get().getCreatedBy().getId().equals(userId);
    }

    /**
     * 检查用户是否有权限删除家庭账本
     */
    public boolean canDeleteFamily(Long familyId, Long userId) {
        // 只有所有者可以删除家庭账本
        return isOwner(familyId, userId);
    }

    /**
     * 获取用户在家庭中的角色
     */
    public Optional<FamilyMember.Role> getUserRole(Long familyId, Long userId) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId)
                .map(FamilyMember::getRole);
    }

    /**
     * 检查用户是否是家庭成员（包括任何角色）
     */
    public boolean isFamilyMember(Long familyId, Long userId) {
        return familyMemberRepository.findByFamilyIdAndUserId(familyId, userId).isPresent();
    }
}
