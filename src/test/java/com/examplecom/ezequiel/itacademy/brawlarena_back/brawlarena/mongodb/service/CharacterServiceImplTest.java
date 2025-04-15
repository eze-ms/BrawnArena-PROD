package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterServiceImplTest {

    @Mock
    private CharacterRepository characterRepository;

    @InjectMocks
    private CharacterServiceImpl characterService;

    // Método helper para crear Characters de prueba
    private Character createTestCharacter(String id, String playerId, boolean unlocked) {
        return new Character(
                id,
                "Test-" + id,
                "Descripción",
                "Medium",
                new ArrayList<>(),
                new ArrayList<>(),
                unlocked,
                "image.png",
                playerId
        );
    }

    //! Test getAllFreeCharacters
    @Test
    void getAllFreeCharacters_ReturnsFilteredCharacters() {
        Character lockedChar1 = new Character("1", "Locked", "desc", "easy", null, null, false, "img1", "player1");
        Character lockedChar2 = new Character("2", "Locked", "desc", "easy", null, null, false, "img1", "player2");
        Character unlockedChar = new Character("Unlocked", "Locked", "desc", "easy", null, null, true, "img1", "player2");


        when(characterRepository.findAll())
                .thenReturn(Flux.just(lockedChar1, unlockedChar, lockedChar2));

        StepVerifier.create(characterService.getAllFreeCharacters())
                .expectNextMatches(c -> c.getId().equals("1") && !c.isUnlocked())
                .expectNextMatches(c -> c.getId().equals("2") && !c.isUnlocked())
                .verifyComplete();
    }

    @Test
    void getAllFreeCharacters_EmptyWhenNoResults() {
        // Arrange
        when(characterRepository.findAll())
                .thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(characterService.getAllFreeCharacters())
                .verifyComplete();
    }

    @Test
    void getAllFreeCharacters_LogsErrorOnFailure() {
        // Arrange
        RuntimeException simulatedError = new RuntimeException("Error de base de datos");
        when(characterRepository.findAll())
                .thenReturn(Flux.error(simulatedError)); // Simula error en el repositorio

        // Act & Assert
        StepVerifier.create(characterService.getAllFreeCharacters())
                .expectErrorMatches(error -> {

                    return error.equals(simulatedError);
                })
                .verify();

    }

    //! Test getUnlockedCharacters
    @Test
    void getUnlockedCharacters_ReturnsFilteredCharacters() {
        // Datos de prueba
        Character unlockedChar = createTestCharacter("1", "player1", true);
        Character lockedChar = createTestCharacter("2", "player1", false);
        Character otherPlayerChar = createTestCharacter("3", "player2", true);

        // Mock del repositorio
        when(characterRepository.findAll())
                .thenReturn(Flux.just(unlockedChar, lockedChar, otherPlayerChar));

        // Ejecución y verificación
        StepVerifier.create(characterService.getUnlockedCharacters("player1"))
                .expectNextMatches(character ->
                        character.getId().equals("1") &&
                                character.isUnlocked() &&
                                character.getPlayerId().equals("player1")
                )
                .expectComplete()
                .verify();
    }

    @Test
    void getUnlockedCharacters_ReturnsEmptyWhenNoMatches() {
        when(characterRepository.findAll())
                .thenReturn(Flux.empty());

        StepVerifier.create(characterService.getUnlockedCharacters("player1"))
                .verifyComplete();
    }

    @Test
    void getUnlockedCharacters_PropagatesErrors() {
        when(characterRepository.findAll())
                .thenReturn(Flux.error(new RuntimeException("DB Error")));

        StepVerifier.create(characterService.getUnlockedCharacters("player1"))
                .expectError(RuntimeException.class)
                .verify();
    }

    //! Test unlockCharacter
    // Desbloqueo exitoso
    @Test
    void unlockCharacter_SuccessfullyUnlocks_ReturnsTrue() {
        Character lockedChar = createTestCharacter("char1", null, false);
        Character savedChar = createTestCharacter("char1", "player1", true);

        when(characterRepository.findById("char1"))
                .thenReturn(Mono.just(lockedChar));
        when(characterRepository.save(any(Character.class)))
                .thenReturn(Mono.just(savedChar));

        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectNext(true)
                .verifyComplete();
    }

    // Test 2: Personaje ya desbloqueado
    @Test
    void unlockCharacter_AlreadyUnlocked_ReturnsFalse() {
        Character unlockedChar = createTestCharacter("char1", "player1", true);

        when(characterRepository.findById("char1"))
                .thenReturn(Mono.just(unlockedChar));

        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectNext(false)
                .verifyComplete();
    }

    // Test 3: Personaje no encontrado
    @Test
    void unlockCharacter_NotFound_ThrowsException() {
        when(characterRepository.findById("char1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectErrorMatches(ex -> ex instanceof CharacterNotFoundException &&
                        ex.getMessage().equals("Personaje no encontrado"))
                .verify();
    }

    // Test 4: Error en repositorio
    @Test
    void unlockCharacter_RepositoryError_PropagatesError() {
        when(characterRepository.findById("char1"))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));

        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectError(RuntimeException.class)
                .verify();
    }


}