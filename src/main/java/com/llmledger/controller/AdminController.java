package com.llmledger.controller;

import com.llmledger.entity.Admin;
import com.llmledger.entity.User;
import com.llmledger.repository.UserRepository;
import com.llmledger.service.AdminService;
import com.llmledger.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;

    public AdminController(AdminService adminService,
            UserRepository userRepository,
            AnalyticsService analyticsService) {
        this.adminService = adminService;
        this.userRepository = userRepository;
        this.analyticsService = analyticsService;
    }

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            Admin admin = adminService.login(username, password);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("username", admin.getUsername());
            response.put("role", admin.getRole());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取系统统计数据（管理员专用）
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);

        // 活跃用户（近30天有操作的用户）
        // 简化版本：暂时使用总用户数
        stats.put("activeUsers", totalUsers);

        // 交易统计
        stats.put("totalTransactions", analyticsService.getTotalTransactionCount());

        // 系统健康状态
        stats.put("systemStatus", "healthy");

        return ResponseEntity.ok(stats);
    }

    /**
     * 获取所有用户列表（管理员专用）
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("avatarUrl", user.getAvatarUrl());
                    userMap.put("createdAt", user.getCreatedAt());
                    return userMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    /**
     * 获取用户详情（管理员专用）
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", user.getId());
                    detail.put("username", user.getUsername());
                    detail.put("avatarUrl", user.getAvatarUrl());
                    detail.put("createdAt", user.getCreatedAt());
                    detail.put("transactionCount", analyticsService.getUserTransactionCount(userId));
                    detail.put("totalExpense", analyticsService.getUserTotalExpense(userId));
                    detail.put("totalIncome", analyticsService.getUserTotalIncome(userId));
                    return ResponseEntity.ok(detail);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建管理员（仅超级管理员）
     */
    @PostMapping("/create")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String role = request.get("role");

            Admin admin = adminService.createAdmin(username, password, role);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("adminId", admin.getId());
            response.put("username", admin.getUsername());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取所有管理员列表
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAdminList() {
        List<Admin> admins = adminService.getAllAdmins();

        List<Map<String, Object>> adminList = admins.stream()
                .map(admin -> {
                    Map<String, Object> adminMap = new HashMap<>();
                    adminMap.put("id", admin.getId());
                    adminMap.put("username", admin.getUsername());
                    adminMap.put("role", admin.getRole());
                    adminMap.put("active", admin.isActive());
                    adminMap.put("createdAt", admin.getCreatedAt());
                    adminMap.put("lastLogin", admin.getLastLogin());
                    return adminMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(adminList);
    }

    /**
     * 禁用管理员
     */
    @PostMapping("/disable/{adminId}")
    public ResponseEntity<?> disableAdmin(@PathVariable Long adminId) {
        try {
            adminService.disableAdmin(adminId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "管理员已禁用");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 启用管理员
     */
    @PostMapping("/enable/{adminId}")
    public ResponseEntity<?> enableAdmin(@PathVariable Long adminId) {
        try {
            adminService.enableAdmin(adminId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "管理员已启用");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
