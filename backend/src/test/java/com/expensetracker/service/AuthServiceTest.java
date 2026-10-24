package com.expensetracker.service;

import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ConflictException;
import com.expensetracker.exception.InvalidRequestException;
import com.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerStoresAHashedPasswordAndANormalisedEmail() {
        when(userRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User user = authService.register(new RegisterRequest("  Asha ", "  Asha@Example.com ", "secret123"));

        assertThat(user.getEmail()).isEqualTo("asha@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Asha");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-value").isNotEqualTo("secret123");
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyUsed() {
        when(userRepository.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("Asha", "asha@example.com", "secret123")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsAPasswordLongerThan72Bytes() {
        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("Asha", "asha@example.com", "a".repeat(73))))
                .isInstanceOf(InvalidRequestException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}