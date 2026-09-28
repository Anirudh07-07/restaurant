package com.kaveri.service;

import com.kaveri.dto.request.LoginRequest;
import com.kaveri.dto.request.RegisterRequest;
import com.kaveri.dto.response.AuthResponse;
import com.kaveri.entity.Role;
import com.kaveri.entity.User;
import com.kaveri.enums.RoleName;
import com.kaveri.exception.BadRequestException;
import com.kaveri.repository.RoleRepository;
import com.kaveri.repository.UserRepository;
import com.kaveri.security.service.JwtService;
import com.kaveri.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role customerRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder().id(1L).name(RoleName.ROLE_CUSTOMER).build();
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encoded_password")
                .roles(Set.of(customerRole))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Register - Success with new email")
    void register_withNewEmail_shouldSucceed() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .password("Test@123")
                .phone("9876543210")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(
            org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com").password("encoded_password")
                .authorities("ROLE_CUSTOMER").build()
        );
        when(jwtService.generateToken(any())).thenReturn("mock_jwt_token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock_jwt_token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getName()).isEqualTo("Test User");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Fails when email already exists")
    void register_withExistingEmail_shouldThrowBadRequestException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .email("existing@example.com")
                .password("Test@123")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login - Success with valid credentials")
    void login_withValidCredentials_shouldReturnToken() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Test@123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(
            org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com").password("encoded_password")
                .authorities("ROLE_CUSTOMER").build()
        );
        when(jwtService.generateToken(any())).thenReturn("mock_token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock_token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Login - Fails with invalid credentials")
    void login_withInvalidCredentials_shouldThrow() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
