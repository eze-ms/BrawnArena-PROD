package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.CharacterService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;



import java.util.*;

import static com.mongodb.assertions.Assertions.assertNull;
import static com.mongodb.internal.connection.tlschannel.util.Util.assertTrue;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterHandlerTest {

    @Mock
    private CharacterService characterService;

    @Mock
    private ServerRequest request;

    @InjectMocks
    private CharacterHandler characterHandler;

    //* Helper para crear Characters de prueba
    private Character createTestCharacter(String id) {
        return new Character(
                id,
                "TestName",
                "TestDescription",
                "Medium",
                new ArrayList<>(),
                new ArrayList<>(),
                "test.png",
                0
        );
    }

    //* Helper para mockear query params
    private void mockQueryParams(String key, String value) {
        when(request.queryParam(key)).thenReturn(Optional.ofNullable(value));
    }
    //* Helper para path variables
    private void mockPathVariables(String key, String value) {
        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put(key, value);
        when(request.pathVariables()).thenReturn(pathVariables);
    }


    //! getAllCharacters
    // Respuesta 200 OK con personajes
    @Test
    void getAllCharacters_ReturnsOkWithValidHeaders() {
        Character testChar = createTestCharacter("1");
        when(characterService.getAllCharacters())
                .thenReturn(Flux.just(testChar));

        Mono<ServerResponse> response = characterHandler.getAllCharacters(
                ServerRequest.create(
                        MockServerWebExchange.from(MockServerHttpRequest.get("/")),
                        Collections.emptyList()
                )
        );

        StepVerifier.create(response)
                .assertNext(res -> {

                    assertEquals(HttpStatus.OK, res.statusCode());

                    HttpHeaders headers = res.headers();
                    assertNotNull(headers.getContentType(), "Content-Type no debe ser nulo");
                    assertEquals(
                            MediaType.APPLICATION_JSON,
                            headers.getContentType(),
                            "Debe ser application/json"
                    );

                    assertEquals("1.0", headers.getFirst("X-API-Version"));
                })
                .verifyComplete();
    }

    // Respuesta 204 No Content
    @Test
    void getAllCharacters_ReturnsNoContentWhenEmpty() {

        when(characterService.getAllCharacters())
                .thenReturn(Flux.empty());


        Mono<ServerResponse> response = characterHandler.getAllCharacters(
                ServerRequest.create(
                        MockServerWebExchange.from(MockServerHttpRequest.get("/")),
                        Collections.emptyList()
                )
        );


        StepVerifier.create(response)
                .assertNext(res -> {
                    assertEquals(HttpStatus.NO_CONTENT, res.statusCode());
                    assertNull(res.headers().getContentType());
                    assertEquals("1.0", res.headers().getFirst("X-API-Version"));
                })
                .verifyComplete();
    }

    // Manejo de errores
    @Test
    void getAllCharacters_PropagatesServiceError() {

        when(characterService.getAllCharacters())
                .thenReturn(Flux.error(new RuntimeException("Error en base de datos")));


        StepVerifier.create(
                        characterHandler.getAllCharacters(
                                ServerRequest.create(
                                        MockServerWebExchange.from(MockServerHttpRequest.get("/")),
                                        Collections.emptyList()
                                )
                        )
                )
                .expectErrorMatches(ex -> {
                    assertTrue(ex instanceof RuntimeException);
                    assertEquals("Error en base de datos", ex.getMessage());
                    return true;
                })
                .verify();
    }

    // validación de headers (sin Accept)
    @Test
    void getAllCharacters_ForceJsonResponseEvenWithoutAcceptHeader() {

        Character testChar = createTestCharacter("1");
        when(characterService.getAllCharacters())
                .thenReturn(Flux.just(testChar));


        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();


        Mono<ServerResponse> response = characterHandler.getAllCharacters(
                ServerRequest.create(
                        MockServerWebExchange.from(request),
                        Collections.emptyList()
                )
        );


        StepVerifier.create(response)
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());
                    assertEquals(MediaType.APPLICATION_JSON, res.headers().getContentType()); // ¡Debe forzar JSON!
                })
                .verifyComplete();
    }

    //! getCharacterId
    // test para personajes desbloqueados
    @Test
    void getCharacterId_ReturnsUnlockedCharacters() {

        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(characterService.getUnlockedCharacters("player1"))
                .thenReturn(Flux.just(createTestCharacter("1")));


        StepVerifier.create(characterHandler.getCharacterId(request))
                .expectNextMatches(r -> r.statusCode() == HttpStatus.OK)
                .verifyComplete();
    }

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


    //! unlockCharacter
    @Test
    void unlockCharacter_MissingCharacterId_ReturnsBadRequest() {

        mockQueryParams("characterId", null);

        StepVerifier.create(characterHandler.unlockCharacter(request))
                .expectNextMatches(response ->
                        response.statusCode() == HttpStatus.BAD_REQUEST)
                .verifyComplete();
    }

    @Test
    void unlockCharacter_SuccessfullyUnlocked_ReturnsOk() {

        mockQueryParams("characterId", "char1");
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(characterService.unlockCharacter("player1", "char1"))
                .thenReturn(Mono.just(true));


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

        mockQueryParams("characterId", "char1");
        Authentication auth = new UsernamePasswordAuthenticationToken("player1", "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(characterService.unlockCharacter("player1", "char1"))
                .thenReturn(Mono.just(false));

        StepVerifier.create(characterHandler.unlockCharacter(request))
                .expectNextMatches(response -> {
                    if (response.statusCode() != HttpStatus.OK) return false;
                    String body = (String) ((EntityResponse) response).entity();
                    return body.equals("El personaje ya estaba desbloqueado");
                })
                .verifyComplete();
    }


    //! getCharacterDetail
    @Test
    void getCharacterDetail_ReturnsCharacterDetails() {
        when(request.pathVariable("id"))
                .thenReturn("char1");

        Character testChar = createTestCharacter("char1");
        when(characterService.getCharacterDetail("char1"))
                .thenReturn(Mono.just(testChar));

        StepVerifier.create(characterHandler.getCharacterDetail(request))
                .expectNextMatches(response ->
                        response.statusCode() == HttpStatus.OK)
                .verifyComplete();
    }

    @Test
    void getCharacterDetail_NotFound_ThrowsException() {
        when(request.pathVariable("id")).thenReturn("char1");
        when(characterService.getCharacterDetail("char1"))
                .thenReturn(Mono.error(new CharacterNotFoundException("char1")));

        StepVerifier.create(characterHandler.getCharacterDetail(request))
                .expectError(CharacterNotFoundException.class)
                .verify();
    }

    @Test
    void getCharacterDetail_ServiceError_PropagatesException() {
        when(request.pathVariable("id"))
                .thenReturn("char1");

        when(characterService.getCharacterDetail("char1"))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        StepVerifier.create(characterHandler.getCharacterDetail(request))
                .expectError(RuntimeException.class)
                .verify();
    }

}
