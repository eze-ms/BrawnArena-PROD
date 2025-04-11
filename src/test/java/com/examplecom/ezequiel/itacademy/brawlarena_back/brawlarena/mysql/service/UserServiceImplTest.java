package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NicknameAlreadyExistsException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    //! Tests para findById
    @Test
    void findById_WhenUserExists_ReturnsUser() {
        // Arrange
        Long userId = 1L;
        User expectedUser = new User(userId, "testUser", "password", 100, "USER", "[]");
        when(userRepository.findById(userId)).thenReturn(Mono.just(expectedUser));

        // Act & Assert
        StepVerifier.create(userService.findById(userId))
                .expectNext(expectedUser)
                .verifyComplete();
    }

    @Test
    void findById_WhenUserNotExists_ThrowsException() {
        // Arrange
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(userService.findById(userId))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    //! Tests para findByNickname
    @Test
    void findByNickname_WhenUserExists_ReturnsUser() {
        // Arrange
        String nickname = "testUser";
        User expectedUser = new User(1L, nickname, "password", 100, "USER", "[]");
        when(userRepository.findByNickname(nickname)).thenReturn(Mono.just(expectedUser));

        // Act & Assert
        StepVerifier.create(userService.findByNickname(nickname))
                .expectNext(expectedUser)
                .verifyComplete();
    }

    @Test
    void findByNickname_WhenUserNotExists_ThrowsException() {
        // Arrange
        String nickname = "unknownUser";
        when(userRepository.findByNickname(nickname)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(userService.findByNickname(nickname))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    //! Tests para save
    @Test
    void save_WhenNewUser_ReturnsSavedUser() {
        // Arrange
        User newUser = new User(null, "newUser", "rawPassword", 100, "USER", "[]");
        User savedUser = new User(1L, "newUser", "encodedPassword", 100, "USER", "[]");

        when(userRepository.findByNickname("newUser")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));

        // Act & Assert
        StepVerifier.create(userService.save(newUser))
                .expectNextMatches(user ->
                        user.getId() == 1L &&
                                user.getPassword().equals("encodedPassword")
                )
                .verifyComplete();
    }

    @Test
    void save_WhenNicknameExists_ThrowsException() {
        // Arrange
        User existingUser = new User(1L, "existingUser", "encodedPassword", 100, "USER", "[]");
        User newUser = new User(null, "existingUser", "rawPassword", 100, "USER", "[]");

        when(userRepository.findByNickname("existingUser"))
                .thenReturn(Mono.just(existingUser));

        // Act & Assert
        StepVerifier.create(userService.save(newUser))
                .expectErrorMatches(ex ->
                        ex instanceof NicknameAlreadyExistsException &&
                                ex.getMessage().equals("El nickname ya está en uso")
                )
                .verify();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    //! Tests para updateTokens
    @Test
    void updateTokens_WhenUserExists_ReturnsUpdatedUser() {
        // Arrange
        String nickname = "testUser";
        int newTokens = 200;
        User originalUser = new User(1L, nickname, "password", 100, "USER", "[]");
        User updatedUser = new User(1L, nickname, "password", newTokens, "USER", "[]");

        when(userRepository.findByNickname(nickname)).thenReturn(Mono.just(originalUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        // Act & Assert
        StepVerifier.create(userService.updateTokens(nickname, newTokens))
                .expectNextMatches(user ->
                        user.getNickname().equals(nickname) &&
                                user.getTokens() == newTokens
                )
                .verifyComplete();
    }

    @Test
    void updateTokens_WhenUserNotExists_ThrowsException() {
        // Arrange
        String nickname = "unknownUser";
        when(userRepository.findByNickname(nickname)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(userService.updateTokens(nickname, 100))
                .expectError(UserNotFoundException.class)
                .verify();
    }

}