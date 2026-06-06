package com.gmeo.finance_tracker.auth;

import com.gmeo.finance_tracker.auth.dto.LoginRequest;
import com.gmeo.finance_tracker.auth.dto.LoginResponse;
import com.gmeo.finance_tracker.auth.dto.RegisterRequest;
import com.gmeo.finance_tracker.auth.dto.UserResponse;
import com.gmeo.finance_tracker.common.exception.DuplicateResourceException;
import com.gmeo.finance_tracker.common.exception.InvalidCredentialsException;
import com.gmeo.finance_tracker.user.User;
import com.gmeo.finance_tracker.user.UserRepository;
import com.gmeo.finance_tracker.user.enums.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        LoginResponse response = new LoginResponse();
        response.setUser(mapToResponse(user));
        response.setAccessToken(jwtService.generateAccessToken(user.getEmail()));
        response.setTokenType("Bearer");
        return response;
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
