package com.gmeo.finance_tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gmeo.finance_tracker.auth.dto.ChangePasswordRequest;
import com.gmeo.finance_tracker.auth.dto.LoginRequest;
import com.gmeo.finance_tracker.auth.dto.LoginResponse;
import com.gmeo.finance_tracker.auth.dto.RegisterRequest;
import com.gmeo.finance_tracker.auth.dto.UserResponse;
import com.gmeo.finance_tracker.common.exception.DuplicateResourceException;
import com.gmeo.finance_tracker.common.exception.InvalidCredentialsException;
import com.gmeo.finance_tracker.security.CurrentUserService;
import com.gmeo.finance_tracker.user.User;
import com.gmeo.finance_tracker.user.UserRepository;
import com.gmeo.finance_tracker.user.enums.UserRole;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTests {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private CurrentUserService currentUserService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = Mockito.mock(JwtService.class);
        currentUserService = Mockito.mock(CurrentUserService.class);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, currentUserService);
    }

    @Test
    void registerSavesUserWithHashedPassword() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(LocalDateTime.of(2026, 5, 31, 10, 0));
            user.setUpdatedAt(LocalDateTime.of(2026, 5, 31, 10, 0));
            return user;
        });

        UserResponse response = authService.register(createRequest("test@example.com", "password123", "Test User"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getFullName()).isEqualTo("Test User");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash())).isTrue();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void registerSavesPasswordHashThatIsNotEqualToRawPassword() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(createRequest("test@example.com", "password123", "Test User"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("password123");
    }

    @Test
    void registerThrowsDuplicateResourceExceptionForDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(createRequest("test@example.com", "password123", "Test User")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerNormalizesEmailToLowercase() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(createRequest("  TEST@Example.COM  ", "password123", "Test User"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void loginReturnsUserForValidCredentials() {
        User user = createUser("test@example.com", passwordEncoder.encode("password123"));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("test@example.com")).thenReturn("jwt-token");

        LoginResponse response = authService.login(createLoginRequest("test@example.com", "password123"));

        assertThat(response.getUser().getId()).isEqualTo(1L);
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getFullName()).isEqualTo("Test User");
        assertThat(response.getUser().getRole()).isEqualTo(UserRole.USER);
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginNormalizesEmailBeforeLookup() {
        User user = createUser("test@example.com", passwordEncoder.encode("password123"));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken("test@example.com")).thenReturn("jwt-token");

        authService.login(createLoginRequest("  TEST@Example.COM  ", "password123"));

        verify(userRepository).findByEmail("test@example.com");
        verify(jwtService).generateAccessToken("test@example.com");
    }

    @Test
    void loginThrowsInvalidCredentialsExceptionForWrongPassword() {
        User user = createUser("test@example.com", passwordEncoder.encode("password123"));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(createLoginRequest("test@example.com", "wrongpass123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void loginThrowsInvalidCredentialsExceptionForUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(createLoginRequest("missing@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void changePasswordHashesNewPasswordWhenCurrentPasswordMatches() {
        User user = createUser("test@example.com", passwordEncoder.encode("password123"));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.changePassword(createChangePasswordRequest("password123", "newpass123"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(passwordEncoder.matches("newpass123", userCaptor.getValue().getPasswordHash())).isTrue();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void changePasswordThrowsInvalidCredentialsExceptionForWrongCurrentPassword() {
        User user = createUser("test@example.com", passwordEncoder.encode("password123"));
        when(currentUserService.getCurrentUser()).thenReturn(user);

        assertThatThrownBy(() -> authService.changePassword(createChangePasswordRequest("wrongpass123", "newpass123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Current password is incorrect");

        verify(userRepository, never()).save(any(User.class));
    }

    private RegisterRequest createRequest(String email, String password, String fullName) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setFullName(fullName);
        return request;
    }

    private LoginRequest createLoginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private ChangePasswordRequest createChangePasswordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    private User createUser(String email, String passwordHash) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFullName("Test User");
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.of(2026, 5, 31, 10, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 5, 31, 10, 0));
        return user;
    }
}
