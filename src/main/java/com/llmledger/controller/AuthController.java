package com.llmledger.controller;

import com.llmledger.dto.*;
import com.llmledger.entity.User;
import com.llmledger.security.JwtService;
import com.llmledger.service.AuthService;
import com.llmledger.service.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final DtoMapper mapper;

    public AuthController(AuthService authService, JwtService jwtService, DtoMapper mapper) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody UserCreateRequest request) {
        User user = authService.register(request);
        return mapper.toUserResponse(user);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestParam("username") String username,
                               @RequestParam("password") String password) {
        User user = authService.login(username, password);
        String token = jwtService.generateToken(user.getUsername());
        return new TokenResponse(token);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return mapper.toUserResponse(user);
    }

    @PutMapping("/password")
    public MessageResponse updatePassword(@Valid @RequestBody PasswordChangeRequest request,
                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        authService.updatePassword(user, request);
        return new MessageResponse("success", "Password updated");
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AvatarResponse updateAvatar(@RequestPart("file") MultipartFile file,
                                       Authentication authentication) throws IOException {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        User user = (User) authentication.getPrincipal();
        String ext = "png";
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            ext = name.substring(name.lastIndexOf('.') + 1);
        }

        Path uploadDir = Paths.get("uploads", "avatars");
        Files.createDirectories(uploadDir);
        String filename = "user_" + user.getId() + "_" + Instant.now().getEpochSecond() + "." + ext;
        Path target = uploadDir.resolve(filename);
        file.transferTo(target.toFile());

        String avatarUrl = "http://localhost:8000/uploads/avatars/" + filename;
        user.setAvatarUrl(avatarUrl);
        authService.save(user);

        return new AvatarResponse("success", "Avatar updated", avatarUrl);
    }
}
