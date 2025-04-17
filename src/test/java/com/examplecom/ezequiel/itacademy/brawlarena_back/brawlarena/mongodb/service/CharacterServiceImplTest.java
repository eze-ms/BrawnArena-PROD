package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterServiceImplTest {

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private UserRepository userRepository;

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
                playerId,
                0
        );
    }

    //! Test getAllFreeCharacters
//    @Test
//    void getAllFreeCharacters_ReturnsFilteredCharacters() {
//
//        Character lockedChar1 = new Character("1", "Locked", "desc", "easy", null, null, false, "img1", "player1", 100);
//        Character lockedChar2 = new Character("2", "Locked", "desc", "easy", null, null, false, "img1", "player2", 100);
//        Character unlockedChar = new Character("Unlocked", "Locked", "desc", "easy", null, null, true, "img1", "player2", 100);
//
//        when(characterRepository.findAll())
//                .thenReturn(Flux.just(lockedChar1, unlockedChar, lockedChar2));
//
//        StepVerifier.create(characterService.getAllFreeCharacters())
//                .expectNextMatches(c -> c.getId().equals("1") && !c.isUnlocked())
//                .expectNextMatches(c -> c.getId().equals("2") && !c.isUnlocked())
//                .verifyComplete();
//    }
//
//    @Test
//    void getAllFreeCharacters_EmptyWhenNoResults() {
//
//        when(characterRepository.findAll())
//                .thenReturn(Flux.empty());
//
//        StepVerifier.create(characterService.getAllFreeCharacters())
//                .verifyComplete();
//    }
//
//    @Test
//    void getAllFreeCharacters_LogsErrorOnFailure() {
//
//        RuntimeException simulatedError = new RuntimeException("Error de base de datos");
//        when(characterRepository.findAll())
//                .thenReturn(Flux.error(simulatedError)); // Simula error en el repositorio
//
//
//        StepVerifier.create(characterService.getAllFreeCharacters())
//                .expectErrorMatches(error -> {
//
//                    return error.equals(simulatedError);
//                })
//                .verify();
//
//    }

    // ! Test para getAllCharacters
    @Test
    void getAllCharacters_ReturnsFluxOfCharacters() {
        // Configuración
        Character char1 = createTestCharacter("1", null, false);
        Character char2 = createTestCharacter("2", null, true);

        when(characterRepository.findAll())
                .thenReturn(Flux.just(char1, char2));

        // Ejecución y validación
        StepVerifier.create(characterService.getAllCharacters())
                .expectNext(char1)
                .expectNext(char2)
                .verifyComplete();
    }

    @Test
    void getAllCharacters_ReturnsEmptyFluxWhenNoCharacters() {
        // Configuración
        when(characterRepository.findAll())
                .thenReturn(Flux.empty());

        // Ejecución y validación
        StepVerifier.create(characterService.getAllCharacters())
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void getAllCharacters_PropagatesRepositoryError() {
        // Configuración
        RuntimeException simulatedError = new RuntimeException("Error en MongoDB");
        when(characterRepository.findAll())
                .thenReturn(Flux.error(simulatedError));

        // Ejecución y validación
        StepVerifier.create(characterService.getAllCharacters())
                .expectErrorMatches(ex -> {
                    assertThat(ex).isInstanceOf(RuntimeException.class);
                    assertThat(ex.getMessage()).isEqualTo("Error en MongoDB");
                    return true;
                })
                .verify();
    }


    //! Test para getUnlockedCharacters
    @Test
    void getUnlockedCharacters_ReturnsFilteredCharacters() {

        Character unlockedChar = createTestCharacter("1", "player1", true);
        Character lockedChar = createTestCharacter("2", "player1", false);
        Character otherPlayerChar = createTestCharacter("3", "player2", true);


        when(characterRepository.findAll())
                .thenReturn(Flux.just(unlockedChar, lockedChar, otherPlayerChar));


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

    //! Test para unlockCharacter
    // Desbloqueo exitoso
    @Test
    void unlockCharacter_SuccessfullyUnlocks_ReturnsTrue() {
        // Configura mocks
        Character lockedChar = createTestCharacter("char1", null, false);
        Character savedChar = createTestCharacter("char1", "player1", true);
        User testUser = User.builder()
                .nickname("player1")
                .tokens(100)
                .characterIds("[]") // ¡Importante! Lista inicial vacía como string
                .build();

        when(characterRepository.findById("char1"))
                .thenReturn(Mono.just(lockedChar));
        when(characterRepository.save(any(Character.class)))
                .thenReturn(Mono.just(savedChar));
        when(userRepository.findByNickname("player1")) // Mockear UserRepository
                .thenReturn(Mono.just(testUser));
        when(userRepository.save(any(User.class))) // Mockear actualización de usuario
                .thenReturn(Mono.just(testUser));

        // Ejecuta y verifica
        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectNext(true)
                .verifyComplete();
    }

    // Personaje ya desbloqueado
    @Test
    void unlockCharacter_AlreadyUnlocked_ReturnsFalse() {
        // Configura mocks
        Character unlockedChar = createTestCharacter("char1", "player1", true);
        User testUser = User.builder() // Añade mock de User
                .nickname("player1")
                .tokens(100)
                .characterIds("[]")
                .build();

        when(characterRepository.findById("char1"))
                .thenReturn(Mono.just(unlockedChar));
        when(userRepository.findByNickname("player1")) // Mock necesario aunque no se use
                .thenReturn(Mono.just(testUser));

        // Ejecuta y verifica
        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectNext(false)
                .verifyComplete();
    }

    // Personaje no encontrado
    @Test
    void unlockCharacter_NotFound_ThrowsException() {
        when(characterRepository.findById("char1"))
                .thenReturn(Mono.empty());
        when(userRepository.findByNickname("player1")) // Mock necesario para evitar NPE
                .thenReturn(Mono.just(User.builder().build())); // User dummy

        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectErrorMatches(ex -> ex instanceof CharacterNotFoundException &&
                        ex.getMessage().equals("Personaje no encontrado"))
                .verify();
    }

    // Error en repositorio
    @Test
    void unlockCharacter_RepositoryError_PropagatesError() {
        // Mock de UserRepository para evitar NPE
        when(userRepository.findByNickname("player1"))
                .thenReturn(Mono.just(User.builder().build())); // User dummy

        // Mock del error en CharacterRepository
        when(characterRepository.findById("char1"))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));

        StepVerifier.create(characterService.unlockCharacter("player1", "char1"))
                .expectError(RuntimeException.class)
                .verify();
    }

    //! Tests para getCharacterDetail
    @Test
    void getCharacterDetail_ReturnsCharacterWhenExists() {

        Character testChar = createTestCharacter("char1", "player1", true);
        when(characterRepository.findById("char1"))
                .thenReturn(Mono.just(testChar));

        StepVerifier.create(characterService.getCharacterDetail("char1"))
                .expectNextMatches(character ->
                        character.getId().equals("char1") &&
                                character.getName().equals("Test-char1"))
                .verifyComplete();
    }

    @Test
    void getCharacterDetail_ThrowsWhenCharacterNotFound() {

        when(characterRepository.findById("char1"))
                .thenReturn(Mono.empty());

        StepVerifier.create(characterService.getCharacterDetail("char1"))
                .expectErrorMatches(ex ->
                        ex instanceof CharacterNotFoundException &&
                                ex.getMessage().equals("Personaje no encontrado"))
                .verify();
    }

    @Test
    void getCharacterDetail_PropagatesRepositoryErrors() {

        when(characterRepository.findById("char1"))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));

        StepVerifier.create(characterService.getCharacterDetail("char1"))
                .expectError(RuntimeException.class)
                .verify();
    }

}