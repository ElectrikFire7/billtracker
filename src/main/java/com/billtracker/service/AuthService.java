package com.billtracker.service;

import com.billtracker.dto.request.GoogleOAuthRequest;
import com.billtracker.dto.response.AuthResponse;
import com.billtracker.dto.response.UserResponse;
import com.billtracker.entity.User;
import com.billtracker.repository.UserRepository;
import com.billtracker.security.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthResponse googleLogin(GoogleOAuthRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getCredential());

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String googleId = payload.getSubject();

        User user = userRepository.findByOauthIdAndOauthProvider(googleId, "google")
                .orElseGet(() -> {
                    // Check if a user with this email already exists (edge case)
                    User existingUser = userRepository.findByEmail(email).orElse(null);
                    if (existingUser != null) {
                        // Link existing account to Google OAuth
                        existingUser.setOauthProvider("google");
                        existingUser.setOauthId(googleId);
                        existingUser.setName(name != null ? name : existingUser.getName());
                        return userRepository.save(existingUser);
                    }

                    // Create new user
                    User newUser = User.builder()
                            .name(name != null ? name : email.split("@")[0])
                            .email(email)
                            .oauthProvider("google")
                            .oauthId(googleId)
                            .build();
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.from(user))
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String credential) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google token");
            }
            return idToken.getPayload();
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Google token: " + e.getMessage());
        }
    }
}
