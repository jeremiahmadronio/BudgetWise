package com.example.budgetwise.user.service;

import com.example.budgetwise.security.JwtService;
import com.example.budgetwise.user.dto.AuthResponse;
import com.example.budgetwise.user.dto.LoginRequest;
import com.example.budgetwise.user.dto.RegisterRequest;
import com.example.budgetwise.user.entity.User;
import com.example.budgetwise.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email is already in use");
        }

        // 2. Map and Save new User
        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .status(User.Status.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        return generateAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAuthProvider() == User.AuthProvider.GOOGLE && user.getPassword() == null) {
            throw new RuntimeException("This account is linked with Google. Please login using Google.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return generateAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public boolean isEmailAlreadyTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    private AuthResponse generateAuthResponse(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", user.getRole().name());

        String token = jwtService.generateToken(user, claims);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .id(user.getId())
                .build();
    }
}