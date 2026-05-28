package com.llmledger.service;

import com.llmledger.entity.Admin;
import com.llmledger.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 管理员登录
     */
    public Admin login(String username, String password) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("管理员账号不存在"));

        if (!passwordEncoder.matches(password, admin.getHashedPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (!admin.isActive()) {
            throw new RuntimeException("账号已被禁用");
        }

        // 更新最后登录时间
        admin.setLastLogin(LocalDateTime.now());
        adminRepository.save(admin);

        return admin;
    }

    /**
     * 创建管理员
     */
    public Admin createAdmin(String username, String password, String role) {
        if (adminRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        Admin admin = new Admin(
                username,
                passwordEncoder.encode(password),
                role != null ? role : "SYSTEM_ADMIN");

        return adminRepository.save(admin);
    }

    /**
     * 获取所有管理员
     */
    public java.util.List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    /**
     * 根据ID获取管理员
     */
    public Optional<Admin> getAdminById(Long id) {
        return adminRepository.findById(id);
    }

    /**
     * 禁用管理员
     */
    public void disableAdmin(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        admin.setActive(false);
        adminRepository.save(admin);
    }

    /**
     * 启用管理员
     */
    public void enableAdmin(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        admin.setActive(true);
        adminRepository.save(admin);
    }

    /**
     * 修改密码
     */
    public void changePassword(Long id, String oldPassword, String newPassword) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        if (!passwordEncoder.matches(oldPassword, admin.getHashedPassword())) {
            throw new RuntimeException("原密码错误");
        }

        admin.setHashedPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(admin);
    }
}
