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
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

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
}