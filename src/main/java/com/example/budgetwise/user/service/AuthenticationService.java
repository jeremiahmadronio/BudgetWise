package com.example.budgetwise.user.service;

import com.example.budgetwise.user.dto.RegisterRequest;
import com.example.budgetwise.user.entity.User;
import com.example.budgetwise.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email is already in use");
        }
        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .status(User.Status.ACTIVE)
                .build();
        userRepository.save(user);
    }


    @Transactional(readOnly = true)
    public boolean isEmailAlreadyTaken(String email) {
        return userRepository.existsByEmail(email);
    }
}
