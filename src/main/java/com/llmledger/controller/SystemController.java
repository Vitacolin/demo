package com.llmledger.controller;

import com.llmledger.dto.UserResponse;
import com.llmledger.entity.SystemNotice;
import com.llmledger.entity.User;
import com.llmledger.repository.SystemNoticeRepository;
import com.llmledger.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final UserRepository userRepository;
    private final SystemNoticeRepository noticeRepository;

    public SystemController(UserRepository userRepository, SystemNoticeRepository noticeRepository) {
        this.userRepository = userRepository;
        this.noticeRepository = noticeRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> responses = users.stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getAvatarUrl(), u.getCreatedAt(),
                        u.getRole().name()))
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        String role = body.get("role");
        if ("ADMIN".equals(role)) {
            user.setRole(User.Role.ADMIN);
        } else {
            user.setRole(User.Role.USER);
        }

        userRepository.save(user);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getAvatarUrl(),
                user.getCreatedAt(), user.getRole().name()));
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notices")
    public ResponseEntity<List<SystemNotice>> getActiveNotices() {
        List<SystemNotice> notices = noticeRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        return ResponseEntity.ok(notices);
    }

    @GetMapping("/notices/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SystemNotice>> getAllNotices() {
        List<SystemNotice> notices = noticeRepository.findAll();
        notices.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return ResponseEntity.ok(notices);
    }

    @PostMapping("/notices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemNotice> createNotice(@RequestBody Map<String, String> body) {
        SystemNotice notice = new SystemNotice();
        notice.setTitle(body.get("title"));
        notice.setContent(body.get("content"));
        notice.setIsActive(true);

        SystemNotice saved = noticeRepository.save(notice);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/notices/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemNotice> updateNotice(@PathVariable Long noticeId,
            @RequestBody Map<String, Object> body) {
        SystemNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("公告不存在"));

        if (body.containsKey("title")) {
            notice.setTitle((String) body.get("title"));
        }
        if (body.containsKey("content")) {
            notice.setContent((String) body.get("content"));
        }
        if (body.containsKey("isActive")) {
            notice.setIsActive((Boolean) body.get("isActive"));
        }

        SystemNotice saved = noticeRepository.save(notice);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/notices/{noticeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new RuntimeException("公告不存在");
        }
        noticeRepository.deleteById(noticeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        long userCount = userRepository.count();
        long activeNoticeCount = noticeRepository.findByIsActiveTrueOrderByCreatedAtDesc().size();

        return ResponseEntity.ok(Map.of(
                "totalUsers", userCount,
                "activeNotices", activeNoticeCount));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "isAdmin", user.isAdmin()));
    }
}