package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterUpdateRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.PieceAssignmentDTO;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.CharacterService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.PieceRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.ParameterizedTypeReference;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterHandlerTest {

    @Mock
    private CharacterService characterService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PieceRepository pieceRepository;

    @Mock
    private ServerRequest request;

    private CharacterHandler characterHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        characterHandler = new CharacterHandler(characterService, jwtService, userRepository, objectMapper, pieceRepository);
    }

    private Character createTestCharacter(String id) {
        return new Character(
                id,
                "TestName",
                "TestDescription",
                "Medium",
                new ArrayList<>(),
                new ArrayList<>(),
                "test.png",
                0,
                "test-game.png"
        );
    }

    private void mockQueryParams(String key, String value) {
        when(request.queryParam(key)).thenReturn(Optional.ofNullable(value));
    }


    @Test
    void getAllCharacters_ReturnsOkWithValidHeaders() throws Exception {

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("player1");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        User mockUser = new User();
        mockUser.setNickname("player1");
        mockUser.setCharacterIds("[\"1\"]");
        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(mockUser));

        when(objectMapper.readValue(eq("[\"1\"]"), any(TypeReference.class)))
                .thenReturn(List.of("1"));

        Character testChar = createTestCharacter("1");
        when(characterService.getAllCharacters()).thenReturn(Flux.just(testChar));

        Mono<ServerResponse> response = characterHandler.getAllCharacters(request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());
                    HttpHeaders headers = res.headers();
                    assertNotNull(headers.getContentType());
                    assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
                    assertEquals("1.0", headers.getFirst("X-API-Version"));
                })
                .verifyComplete();
    }

    @Test
    void getAllCharacters_ReturnsNoContentWhenEmpty() throws Exception {

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("player1");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        User mockUser = new User();
        mockUser.setNickname("player1");
        mockUser.setCharacterIds("[]");
        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(mockUser));

        when(objectMapper.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of());

        when(characterService.getAllCharacters()).thenReturn(Flux.empty());

        Mono<ServerResponse> response = characterHandler.getAllCharacters(request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertEquals(HttpStatus.NO_CONTENT, res.statusCode());
                    assertNull(res.headers().getContentType());
                    assertEquals("1.0", res.headers().getFirst("X-API-Version"));
                })
                .verifyComplete();
    }

    @Test
    void getAllCharacters_PropagatesServiceError() throws Exception {

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("player1");
        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        User mockUser = new User();
        mockUser.setNickname("player1");
        mockUser.setCharacterIds("[\"1\"]");
        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(mockUser));

        when(objectMapper.readValue(eq("[\"1\"]"), any(TypeReference.class)))
                .thenReturn(List.of("1"));

        when(characterService.getAllCharacters())
                .thenReturn(Flux.error(new RuntimeException("Error en base de datos")));

        StepVerifier.create(characterHandler.getAllCharacters(request))
                .expectErrorMatches(ex -> {
                    assertTrue(ex instanceof RuntimeException);
                    assertEquals("Error en base de datos", ex.getMessage());
                    return true;
                })
                .verify();
    }

    @Test
    void getAllCharacters_ForceJsonResponseEvenWithoutAcceptHeader() throws Exception {

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("player1");

        ServerRequest request = ServerRequest.create(
                MockServerWebExchange.from(MockServerHttpRequest.get("/characters/all")),
                Collections.emptyList()
        );
        when(this.request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        User mockUser = new User();
        mockUser.setNickname("player1");
        mockUser.setCharacterIds("[\"1\"]");
        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(mockUser));

        when(objectMapper.readValue(eq("[\"1\"]"), any(TypeReference.class)))
                .thenReturn(List.of("1"));

        Character testChar = createTestCharacter("1");
        when(characterService.getAllCharacters()).thenReturn(Flux.just(testChar));

        Mono<ServerResponse> response = characterHandler.getAllCharacters(this.request);

        StepVerifier.create(response)
                .assertNext(res -> {
                    assertEquals(HttpStatus.OK, res.statusCode());
                    assertEquals(MediaType.APPLICATION_JSON, res.headers().getContentType());
                })
                .verifyComplete();
    }

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

    @Test
    void getCharacterId_UsuarioNoAutenticado_NoHaceNada() {
        when(request.principal())
                .thenReturn(Mono.empty());

        StepVerifier.create(characterHandler.getCharacterId(request))
                .verifyComplete();
    }

    @Test
    void getCharacterId_ErrorEnServicio_PropagaExcepcion() {
        String playerId = "player1";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));
        when(characterService.getUnlockedCharacters(playerId))
                .thenReturn(Flux.error(new RuntimeException("Fallo en base de datos")));

        StepVerifier.create(characterHandler.getCharacterId(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Fallo en base de datos"))
                .verify();
    }

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

    @Test
    void updateCharacter_CharacterIdYBodyValidos_RetornaOkConPersonajeActualizado() {
        String characterId = "char123";

        CharacterUpdateRequest updateRequest = new CharacterUpdateRequest();
        updateRequest.setName("NuevoNombre");
        updateRequest.setDescription("Descripción actualizada");

        Character personajeActualizado = new Character();
        personajeActualizado.setId(characterId);
        personajeActualizado.setName("NuevoNombre");
        personajeActualizado.setDescription("Descripción actualizada");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(CharacterUpdateRequest.class))
                .thenReturn(Mono.just(updateRequest));
        when(characterService.updateCharacter(characterId, updateRequest))
                .thenReturn(Mono.just(personajeActualizado));

        StepVerifier.create(characterHandler.updateCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(Character.class, body);

                    Character result = (Character) body;
                    assertEquals(characterId, result.getId());
                    assertEquals("NuevoNombre", result.getName());
                    assertEquals("Descripción actualizada", result.getDescription());
                })
                .verifyComplete();
    }

    @Test
    void updateCharacter_BodyMalFormado_RetornaBadRequest() {
        String characterId = "char123";

        when(request.pathVariable("id"))
                .thenReturn(characterId);

        when(request.bodyToMono(CharacterUpdateRequest.class))
                .thenReturn(Mono.error(new RuntimeException("Fallo al deserializar el cuerpo")));

        StepVerifier.create(characterHandler.updateCharacter(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Fallo al deserializar"))
                .verify();
    }

    @Test
    void updateCharacter_CharacterNoEncontrado_RetornaNotFound() {
        String characterId = "char123";

        CharacterUpdateRequest updateRequest = new CharacterUpdateRequest();
        updateRequest.setName("Nombre");
        updateRequest.setDescription("Desc");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(CharacterUpdateRequest.class))
                .thenReturn(Mono.just(updateRequest));
        when(characterService.updateCharacter(characterId, updateRequest))
                .thenReturn(Mono.error(new CharacterNotFoundException("Personaje no encontrado")));

        StepVerifier.create(characterHandler.updateCharacter(request))
                .expectErrorMatches(error ->
                        error instanceof CharacterNotFoundException &&
                                error.getMessage().contains("Personaje no encontrado"))
                .verify();
    }

    @Test
    void updateCharacter_ErrorGenericoEnServicio_PropagaExcepcion500() {
        String characterId = "char123";

        CharacterUpdateRequest updateRequest = new CharacterUpdateRequest();
        updateRequest.setName("Nombre");
        updateRequest.setDescription("Desc");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(CharacterUpdateRequest.class))
                .thenReturn(Mono.just(updateRequest));
        when(characterService.updateCharacter(characterId, updateRequest))
                .thenReturn(Mono.error(new RuntimeException("Fallo interno en el servicio")));

        StepVerifier.create(characterHandler.updateCharacter(request))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Fallo interno en el servicio"))
                .verify();
    }

    @Test
    void updateCharacter_ErrorDeserializacionCuerpo_RetornaBadRequest() {
        String characterId = "char123";

        when(request.pathVariable("id"))
                .thenReturn(characterId);

        when(request.bodyToMono(CharacterUpdateRequest.class))
                .thenReturn(Mono.error(new IllegalArgumentException("Cuerpo inválido: deserialización fallida")));

        StepVerifier.create(characterHandler.updateCharacter(request))
                .expectErrorMatches(error ->
                        error instanceof IllegalArgumentException &&
                                error.getMessage().contains("deserialización fallida"))
                .verify();
    }

    @Test
    void assignPiecesToCharacter_DatosValidos_RetornaOkConPersonajeActualizado() {
        String characterId = "char123";
        List<String> pieceIds = List.of("pieza1", "pieza2");

        Character personajeActualizado = new Character();
        personajeActualizado.setId(characterId);
        personajeActualizado.setName("Personaje Test");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<String>>>any()))
                .thenReturn(Mono.just(pieceIds));
        when(characterService.assignPieces(characterId, pieceIds))
                .thenReturn(Mono.just(personajeActualizado));

        StepVerifier.create(characterHandler.assignPiecesToCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    assertEquals(MediaType.APPLICATION_JSON, response.headers().getContentType());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(Character.class, body);

                    Character result = (Character) body;
                    assertEquals(characterId, result.getId());
                    assertEquals("Personaje Test", result.getName());
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesToCharacter_ListaVacia_RetornaOkConPersonajeActualizado() {
        String characterId = "char123";
        List<String> pieceIds = List.of(); // Lista vacía

        Character personajeActualizado = new Character();
        personajeActualizado.setId(characterId);
        personajeActualizado.setName("Personaje Sin Piezas");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<String>>>any()))
                .thenReturn(Mono.just(pieceIds));
        when(characterService.assignPieces(characterId, pieceIds))
                .thenReturn(Mono.just(personajeActualizado));

        StepVerifier.create(characterHandler.assignPiecesToCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(Character.class, body);
                    Character result = (Character) body;
                    assertEquals(characterId, result.getId());
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesToCharacter_CuerpoMalFormado_RetornaBadRequest() {
        String characterId = "char123";

        when(request.pathVariable("id"))
                .thenReturn(characterId);

        when(request.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<String>>>any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Formato del cuerpo no válido")));

        StepVerifier.create(characterHandler.assignPiecesToCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("Formato del cuerpo no válido"));
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesToCharacter_IllegalArgumentException_RetornaBadRequest() {
        String characterId = "char123";
        List<String> pieceIds = List.of("pieza1", "pieza2");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<String>>>any()))
                .thenReturn(Mono.just(pieceIds));
        when(characterService.assignPieces(characterId, pieceIds))
                .thenReturn(Mono.error(new IllegalArgumentException("IDs de piezas inválidos")));

        StepVerifier.create(characterHandler.assignPiecesToCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("IDs de piezas inválidos", body);
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesToCharacter_CharacterNoEncontrado_RetornaNotFound() {
        String characterId = "char123";
        List<String> pieceIds = List.of("pieza1", "pieza2");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<String>>>any()))
                .thenReturn(Mono.just(pieceIds));
        when(characterService.assignPieces(characterId, pieceIds))
                .thenReturn(Mono.error(new CharacterNotFoundException("Personaje no encontrado")));

        StepVerifier.create(characterHandler.assignPiecesToCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("Personaje no encontrado", body);
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesToCharacter_ErrorInterno_RetornaError500() {
        String characterId = "char123";
        List<String> pieceIds = List.of("pieza1", "pieza2");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<String>>>any()))
                .thenReturn(Mono.just(pieceIds));
        when(characterService.assignPieces(characterId, pieceIds))
                .thenReturn(Mono.error(new RuntimeException("Fallo inesperado en el servicio")));

        StepVerifier.create(characterHandler.assignPiecesToCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("Error interno al asignar piezas", body);
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesWithPowers_CharacterIdVacio_RetornaBadRequest() {
        // Simula que el path variable 'id' está vacío
        when(request.pathVariable("id"))
                .thenReturn("");

        StepVerifier.create(characterHandler.assignPiecesWithPowers(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("El characterId no puede estar vacío", body);
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesWithPowers_PieceIdVacio_RetornaBadRequest() {
        String characterId = "char123";
        PieceAssignmentDTO dto = new PieceAssignmentDTO();
        dto.setPieceId("");
        dto.setPower(Power.LLAVE_IMPOSIBLE);

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToFlux(PieceAssignmentDTO.class))
                .thenReturn(Flux.just(dto));

        StepVerifier.create(characterHandler.assignPiecesWithPowers(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("pieceId debe ser válido"));
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesWithPowers_PowerNulo_RetornaBadRequest() {
        String characterId = "char123";
        PieceAssignmentDTO dto = new PieceAssignmentDTO();
        dto.setPieceId("piece1");
        dto.setPower(null);

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToFlux(PieceAssignmentDTO.class))
                .thenReturn(Flux.just(dto));

        StepVerifier.create(characterHandler.assignPiecesWithPowers(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("poder asignado"));
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesWithPowers_AlgunasPiezasNoExisten_RetornaBadRequest() {
        String characterId = "char123";

        PieceAssignmentDTO dto1 = new PieceAssignmentDTO();
        dto1.setPieceId("pieza1");
        dto1.setPower(Power.AIR_JUMP);

        PieceAssignmentDTO dto2 = new PieceAssignmentDTO();
        dto2.setPieceId("piezaInexistente");
        dto2.setPower(Power.CAMPO_MAGNETICO);

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToFlux(PieceAssignmentDTO.class))
                .thenReturn(Flux.just(dto1, dto2));

        Piece piezaEncontrada = new Piece();
        piezaEncontrada.setId("pieza1");

        when(pieceRepository.findByIdIn(List.of("pieza1", "piezaInexistente")))
                .thenReturn(Flux.just(piezaEncontrada));

        StepVerifier.create(characterHandler.assignPiecesWithPowers(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("Una o más piezas no existen en la base de datos", body);
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesWithPowers_TodasLasPiezasValidas_RetornaOkConPersonajeActualizado() {
        String characterId = "char123";

        PieceAssignmentDTO dto1 = new PieceAssignmentDTO();
        dto1.setPieceId("pieza1");
        dto1.setPower(Power.COMBO_KICKS);

        PieceAssignmentDTO dto2 = new PieceAssignmentDTO();
        dto2.setPieceId("pieza2");
        dto2.setPower(Power.CONCENTRACION_ZEN);

        // Piezas encontradas
        Piece pieza1 = new Piece();
        pieza1.setId("pieza1");

        Piece pieza2 = new Piece();
        pieza2.setId("pieza2");

        // Personaje actualizado
        Character personajeActualizado = new Character();
        personajeActualizado.setId(characterId);
        personajeActualizado.setName("TestCharacter");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToFlux(PieceAssignmentDTO.class))
                .thenReturn(Flux.just(dto1, dto2));
        when(pieceRepository.findByIdIn(List.of("pieza1", "pieza2")))
                .thenReturn(Flux.just(pieza1, pieza2));
        when(characterService.assignPiecesWithPowers(characterId, List.of(pieza1, pieza2)))
                .thenReturn(Mono.just(personajeActualizado));

        StepVerifier.create(characterHandler.assignPiecesWithPowers(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(Character.class, body);

                    Character result = (Character) body;
                    assertEquals(characterId, result.getId());
                    assertEquals("TestCharacter", result.getName());
                })
                .verifyComplete();
    }

    @Test
    void assignPiecesWithPowers_ErrorEnServicio_RetornaError500() {
        String characterId = "char123";

        PieceAssignmentDTO dto = new PieceAssignmentDTO();
        dto.setPieceId("pieza1");
        dto.setPower(Power.CORTE_TSUNAMI);

        Piece pieza = new Piece();
        pieza.setId("pieza1");

        when(request.pathVariable("id"))
                .thenReturn(characterId);
        when(request.bodyToFlux(PieceAssignmentDTO.class))
                .thenReturn(Flux.just(dto));
        when(pieceRepository.findByIdIn(List.of("pieza1")))
                .thenReturn(Flux.just(pieza));
        when(characterService.assignPiecesWithPowers(characterId, List.of(pieza)))
                .thenReturn(Mono.error(new RuntimeException("Fallo interno")));

        StepVerifier.create(characterHandler.assignPiecesWithPowers(request))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Fallo interno"))
                .verify();
    }
}
