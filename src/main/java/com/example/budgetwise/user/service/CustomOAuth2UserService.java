package com.example.budgetwise.user.service;

import com.example.budgetwise.user.entity.User;
import com.example.budgetwise.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Load user from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Extract Details
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        String providerId = oAuth2User.getAttribute("sub");

        if (providerId == null) {
            providerId = oAuth2User.getAttribute("id");
        }

        // 3. Save or Update in Database
        saveOrUpdateUser(email, name ,providerId);

        return oAuth2User;
    }

    private void saveOrUpdateUser(String email, String name , String providerId) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .authProvider(User.AuthProvider.GOOGLE)
                    .role(User.Role.USER)
                    .status(User.Status.ACTIVE)
                    .password(null)
                    .providerId(providerId)
                    .accountNonLocked(true)
                    .accountNonExpired(true)
                    .credentialsNonExpired(true)
                    .enabled(true)
                    .build();
            userRepository.save(newUser);
            System.out.println("New Google User Created: " + email);
        } else {
            User existingUser = userOptional.get();
            existingUser.setName(name);
            existingUser.setAuthProvider(User.AuthProvider.GOOGLE);
            userRepository.save(existingUser);
            System.out.println("Existing User Updated: " + email);
        }
    }
}