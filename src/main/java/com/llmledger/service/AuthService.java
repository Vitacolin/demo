package com.llmledger.service;

import com.llmledger.dto.PasswordChangeRequest;
import com.llmledger.dto.UserCreateRequest;
import com.llmledger.entity.User;
import com.llmledger.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already registered");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + request.getUsername() + "&backgroundColor=ff6b35");
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Incorrect username or password");
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            throw new IllegalArgumentException("Incorrect username or password");
        }
        return user;
    }

    public void updatePassword(User user, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.getOld_password(), user.getHashedPassword())) {
            throw new IllegalArgumentException("Incorrect old password");
        }
        user.setHashedPassword(passwordEncoder.encode(request.getNew_password()));
        userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
