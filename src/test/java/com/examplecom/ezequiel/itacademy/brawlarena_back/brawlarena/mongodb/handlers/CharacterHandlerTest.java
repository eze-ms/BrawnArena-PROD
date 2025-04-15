package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.CharacterService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterHandlerTest {

    @Mock
    private CharacterService characterService;

    @Mock
    private JwtService jwtService;

    @Mock
    private ServerRequest request;

    @InjectMocks
    private CharacterHandler characterHandler;

    // Método helper para crear Characters de prueba
    private Character createTestCharacter(String id, String playerId, boolean unlocked) {
        return new Character(
                id,                        // @Id
                "TestName",                // name
                "TestDescription",         // description
                "Medium",                  // difficulty
                new ArrayList<>(),         // pieces
                new ArrayList<>(),         // powers
                unlocked,                  // unlocked
                "test.png",                // imageUrl
                playerId                   // playerId
        );
    }

    // Helper para mockear query params
    private void mockQueryParams(String key, String value) {
        when(request.queryParam(key)).thenReturn(Optional.ofNullable(value));
    }

    //*** getCharacterAllId ***//
    @Test
    void getCharacterAllId_ReturnsOkWithFreeCharacters() {
        // Arrange - Usa el FQN implícito gracias al import
        List<Character> mockCharacters = List.of(
                new Character("1", "Free1", "desc", "easy", null, null, false, "img1", "player1"),
                new Character("2", "Free2", "desc", "easy", null, null, false, "img2", "player2")
        );

        when(characterService.getAllFreeCharacters())
                .thenReturn(Flux.fromIterable(mockCharacters));

        // Act & Assert (igual que antes)
        StepVerifier.create(characterHandler.getCharacterAllId(request))
                .expectNextMatches(response -> {
                    // Para bodyValue():
                    Object body = ((EntityResponse) response).entity();
                    return response.statusCode() == HttpStatus.OK
                            && ((List<Character>) body).size() == 2;
                })
                .verifyComplete();
    }

    @Test
    void getCharacterAllId_Returns500OnError() {
        // Arrange
        when(characterService.getAllFreeCharacters())
                .thenReturn(Flux.error(new RuntimeException("DB Error")));

        // Act & Assert
        StepVerifier.create(characterHandler.getCharacterAllId(request))
                .expectNextMatches(response -> {
                    // Verify status code
                    boolean statusMatches = response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR;

                    // Verify error message body
                    String body = (String) ((EntityResponse) response).entity();
                    boolean bodyMatches = body.equals("Error al recuperar personajes gratuitos");

                    return statusMatches && bodyMatches;
                })
                .verifyComplete();
    }

    //*** getCharacterId ***//
    //! Test para personajes desbloqueados
    @Test
    void getCharacterId_ReturnsUnlockedCharacters() {
        // Configura
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth)); // ← Solución clave

        when(characterService.getUnlockedCharacters("player1"))
                .thenReturn(Flux.just(createTestCharacter("1", "player1", true)));

        // Ejecuta y verifica
        StepVerifier.create(characterHandler.getCharacterId(request))
                .expectNextMatches(r -> r.statusCode() == HttpStatus.OK)
                .verifyComplete();
    }

    //! Test para personajes desbloqueados
    @Test
    void getCharacterId_ReturnsNoContentWhenEmpty() {
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth)); // ← Mock consistente
        when(characterService.getUnlockedCharacters("player1"))
                .thenReturn(Flux.empty());

        StepVerifier.create(characterHandler.getCharacterId(request))
                .expectNextMatches(response ->
                        response.statusCode() == HttpStatus.NO_CONTENT) // 204
                .verifyComplete();
    }

    //! Test para errores
    @Test
    void getCharacterId_Returns500OnError() {
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(characterService.getUnlockedCharacters("player1"))
                .thenReturn(Flux.error(new RuntimeException("DB Error")));

        StepVerifier.create(characterHandler.getCharacterId(request))
                .expectNextMatches(response ->
                        response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .verifyComplete();
    }

    //*** unlockCharacter ***//
    @Test
    void unlockCharacter_MissingCharacterId_ReturnsBadRequest() {
        // Arrange
        mockQueryParams("characterId", null);

        // Act & Assert
        StepVerifier.create(characterHandler.unlockCharacter(request))
                .expectNextMatches(response ->
                        response.statusCode() == HttpStatus.BAD_REQUEST)
                .verifyComplete();
    }

    @Test
    void unlockCharacter_SuccessfullyUnlocked_ReturnsOk() {
        // Arrange
        mockQueryParams("characterId", "char1");
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(characterService.unlockCharacter("player1", "char1"))
                .thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(characterHandler.unlockCharacter(request))
                .expectNextMatches(response -> {
                    if (response.statusCode() != HttpStatus.OK) return false;
                    String body = (String) ((EntityResponse) response).entity();
                    return body.equals("Personaje desbloqueado con éxito");
                })
                .verifyComplete();
    }

    @Test
    void unlockCharacter_AlreadyUnlocked_ReturnsOk() {
        // Arrange
        mockQueryParams("characterId", "char1");
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(characterService.unlockCharacter("player1", "char1"))
                .thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(characterHandler.unlockCharacter(request))
                .expectNextMatches(response -> {
                    if (response.statusCode() != HttpStatus.OK) return false;
                    String body = (String) ((EntityResponse) response).entity();
                    return body.equals("El personaje ya estaba desbloqueado");
                })
                .verifyComplete();
    }

    @Test
    void unlockCharacter_ServiceError_Returns500() {
        // Arrange
        mockQueryParams("characterId", "char1");
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(characterService.unlockCharacter("player1", "char1"))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));

        // Act & Assert
        StepVerifier.create(characterHandler.unlockCharacter(request))
                .expectNextMatches(response ->
                        response.statusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                .verifyComplete();
    }
}
