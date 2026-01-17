package com.example.budgetwise.user.service;

import com.example.budgetwise.user.entity.User;
import com.example.budgetwise.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String role = user.getRole().name();
            Long id = user.getId();



            String targetUrl = "http://localhost:5173/auth/callback";

            String finalUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("id", id)
                    .queryParam("role", role)
                    .queryParam("email", email)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, finalUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}