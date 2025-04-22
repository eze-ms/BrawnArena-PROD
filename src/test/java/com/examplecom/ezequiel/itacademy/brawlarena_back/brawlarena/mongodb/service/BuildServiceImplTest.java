package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.validator.BuildValidator;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterAccessDeniedException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NoPendingBuildException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.BuildRepository;
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
import static org.mockito.Mockito.any;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.internal.connection.tlschannel.util.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BuildServiceImplTest {

    @Mock
    private BuildRepository buildRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CharacterRepository characterRepository;

    @InjectMocks
    private BuildServiceImpl buildService;


    // Helper
    private Character createTestCharacter(String id) {
        Character character = new Character();
        character.setId(id);
        character.setName("Test-" + id);
        character.setDescription("Desc");
        character.setDifficulty("Medium");
        character.setPieces(new ArrayList<>());
        character.setPowers(new ArrayList<>());
        character.setImageUrl("image.png");
        character.setCost(0);
        return character;
    }

    // Helper
    private Build createTestBuild(String playerId, String characterId, boolean isValid) {
        Build build = new Build();
        build.setPlayerId(playerId);
        build.setCharacterId(characterId);
        build.setValid(isValid);
        build.setCreatedAt(Instant.now());
        return build;
    }

    @Test
    void startBuild_conPersonajeDesbloqueado_creaBuildNoValidado() {
        // Usuario con personaje desbloqueado
        User mockUser = User.builder()
                .nickname("player1")
                .characterIds("[\"char1\"]")
                .build();

        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(mockUser));

        // Personaje correspondiente
        Character character = createTestCharacter("char1");
        when(characterRepository.findById("char1")).thenReturn(Mono.just(character));

        when(buildRepository.findAll()).thenReturn(Flux.empty());
        when(buildRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // Ejecución y validación
        StepVerifier.create(buildService.startBuild("player1", "char1"))
                .expectNextMatches(build ->
                        build.getPlayerId().equals("player1") &&
                                build.getCharacterId().equals("char1") &&
                                !build.isValid()
                )
                .verifyComplete();
    }

    @Test
    void startBuild_conBuildExistenteNoValidado_lanzaExcepcion() {
        Character character = createTestCharacter("char1");
        Build existingBuild = createTestBuild("player1", "char1", false);

        User user = User.builder()
                .nickname("player1")
                .characterIds("[\"char1\"]")
                .build();

        when(userRepository.findByNickname("player1")).thenReturn(Mono.just(user));
        when(characterRepository.findById("char1")).thenReturn(Mono.just(character));
        when(buildRepository.findAll()).thenReturn(Flux.just(existingBuild));

        StepVerifier.create(buildService.startBuild("player1", "char1"))
                .expectErrorMatches(ex ->
                        ex instanceof IllegalStateException &&
                                ex.getMessage().equals("Ya tienes un build activo para este personaje")
                )
                .verify();
    }

    @Test
    void validateBuildData_conPiezasNull_lanzaExcepcion() {
        Build build = new Build();
        build.setCharacterId("char1");
        build.setPiecesPlaced(null);
        build.setDuration(60L);

        assertThrows(IllegalArgumentException.class, () -> {
            BuildValidator.validateBuildData(build);
        });
    }

    @Test
    void validateBuild_ValidBuild_ReturnsSuccessResult() {
        Build mockBuild = new Build();
        mockBuild.setId("build123");
        mockBuild.setPlayerId("player123");
        mockBuild.setCharacterId("char123");
        mockBuild.setPiecesPlaced(List.of("piezaCorrecta1", "piezaCorrecta2"));
        mockBuild.setDuration(120);

        Piece piece1 = new Piece();
        piece1.setId("piezaCorrecta1");
        piece1.setName("Pieza 1");

        Piece piece2 = new Piece();
        piece2.setId("piezaCorrecta2");
        piece2.setName("Pieza 2");

        Character mockCharacter = createTestCharacter("char123");
        mockCharacter.setPieces(List.of(piece1, piece2));

        User mockUser = new User();
        mockUser.setNickname("player123");
        mockUser.setCharacterIds("[\"char123\"]");

        Build existingBuild = new Build();
        existingBuild.setId("build123");
        existingBuild.setPlayerId("player123");
        existingBuild.setCharacterId("char123");
        existingBuild.setValid(false);

        when(userRepository.findByNickname("player123"))
                .thenReturn(Mono.just(mockUser));
        when(characterRepository.findById("char123"))
                .thenReturn(Mono.just(mockCharacter));
        when(buildRepository.findAll())
                .thenReturn(Flux.just(existingBuild));
        when(buildRepository.countByPlayerIdAndCharacterIdAndValidTrue("player123", "char123"))
                .thenReturn(Mono.just(0L));
        when(buildRepository.save(any(Build.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(buildService.validateBuild("player123", mockBuild))
                .assertNext(result -> {
                    assertTrue(result.isValid());
                    assertTrue(result.getScore() > 0);
                })
                .verifyComplete();
    }

    @Test
    void validateBuild_BuildNotFound_ReturnsError() {
        Build mockBuild = new Build();
        mockBuild.setId("buildX");
        mockBuild.setPlayerId("player123");
        mockBuild.setCharacterId("char123");
        mockBuild.setPiecesPlaced(List.of("pieza1"));
        mockBuild.setDuration(100);

        Piece piece = new Piece();
        piece.setId("pieza1");

        Character mockCharacter = createTestCharacter("char123");
        mockCharacter.setPieces(List.of(piece));

        User mockUser = new User();
        mockUser.setNickname("player123");
        mockUser.setCharacterIds("[\"char123\"]");

        when(userRepository.findByNickname("player123")).thenReturn(Mono.just(mockUser));
        when(characterRepository.findById("char123")).thenReturn(Mono.just(mockCharacter));
        when(buildRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(buildService.validateBuild("player123", mockBuild))
                .expectErrorMatches(error ->
                        error instanceof NoPendingBuildException &&
                                error.getMessage().contains("build pendiente"))
                .verify();
    }

    @Test
    void validateBuild_InternalServiceError_PropagatesException() {
        Build mockBuild = new Build();
        mockBuild.setPlayerId("player123");
        mockBuild.setCharacterId("char123");
        mockBuild.setPiecesPlaced(List.of("pieza1"));
        mockBuild.setDuration(100);

        when(userRepository.findByNickname("player123"))
                .thenReturn(Mono.just(new User()));

        when(characterRepository.findById("char123"))
                .thenReturn(Mono.error(new RuntimeException("Error inesperado en la base de datos")));

        StepVerifier.create(buildService.validateBuild("player123", mockBuild))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error inesperado"))
                .verify();
    }

    @Test
    void validateBuild_PersonajeNoExiste_LanzaCharacterNotFoundException() {
        Build mockBuild = new Build();
        mockBuild.setPlayerId("player123");
        mockBuild.setCharacterId("char123");
        mockBuild.setPiecesPlaced(List.of("pieza1"));
        mockBuild.setDuration(100);

        User mockUser = new User();
        mockUser.setNickname("player123");
        mockUser.setCharacterIds("[\"char123\"]");

        when(userRepository.findByNickname("player123"))
                .thenReturn(Mono.just(mockUser));

        when(characterRepository.findById("char123"))
                .thenReturn(Mono.empty());

        StepVerifier.create(buildService.validateBuild("player123", mockBuild))
                .expectErrorMatches(error ->
                        error instanceof CharacterNotFoundException &&
                                error.getMessage().contains("Personaje no encontrado"))
                .verify();
    }

    @Test
    void validateBuild_PersonajeNoDesbloqueado_LanzaAccessDeniedException() {

        Build mockBuild = new Build();
        mockBuild.setPlayerId("player123");
        mockBuild.setCharacterId("char123");
        mockBuild.setPiecesPlaced(List.of("pieza1"));
        mockBuild.setDuration(100);


        Piece piece = new Piece();
        piece.setId("pieza1");

        Character mockCharacter = createTestCharacter("char123");
        mockCharacter.setPieces(List.of(piece));


        User mockUser = new User();
        mockUser.setNickname("player123");
        mockUser.setCharacterIds("[\"otroPersonaje\"]");

        when(userRepository.findByNickname("player123")).thenReturn(Mono.just(mockUser));
        when(characterRepository.findById("char123")).thenReturn(Mono.just(mockCharacter));

        StepVerifier.create(buildService.validateBuild("player123", mockBuild))
                .expectErrorMatches(error ->
                        error instanceof CharacterAccessDeniedException &&
                                error.getMessage().contains("no has desbloqueado"))
                .verify();
    }

    @Test
    void validateBuild_NoHayBuildPendiente_LanzaNoPendingBuildException() {
        Build mockBuild = new Build();
        mockBuild.setPlayerId("player123");
        mockBuild.setCharacterId("char123");
        mockBuild.setPiecesPlaced(List.of("pieza1"));
        mockBuild.setDuration(100);

        Piece piece = new Piece();
        piece.setId("pieza1");

        Character mockCharacter = createTestCharacter("char123");
        mockCharacter.setPieces(List.of(piece));

        User mockUser = new User();
        mockUser.setNickname("player123");
        mockUser.setCharacterIds("[\"char123\"]");

        // Stubs
        when(userRepository.findByNickname("player123")).thenReturn(Mono.just(mockUser));
        when(characterRepository.findById("char123")).thenReturn(Mono.just(mockCharacter));
        when(buildRepository.findAll()).thenReturn(Flux.empty()); // No hay build pendiente

        StepVerifier.create(buildService.validateBuild("player123", mockBuild))
                .expectErrorMatches(error ->
                        error instanceof NoPendingBuildException &&
                                error.getMessage().contains("build pendiente"))
                .verify();
    }

    @Test
    void getBuildHistory_HistorialConBuilds_ReturnsFluxOrdenado() {
        String playerId = "player123";

        Build build1 = new Build();
        build1.setId("b1");
        build1.setValid(true);
        build1.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));

        Build build2 = new Build();
        build2.setId("b2");
        build2.setValid(true);
        build2.setCreatedAt(Instant.parse("2024-02-01T10:00:00Z"));

        when(buildRepository.findByPlayerIdAndValidTrueOrderByCreatedAtDesc(playerId))
                .thenReturn(Flux.just(build2, build1));

        StepVerifier.create(buildService.getBuildHistory(playerId))
                .expectNext(build2)
                .expectNext(build1)
                .verifyComplete();
    }

    @Test
    void getBuildHistory_HistorialVacio_ReturnsEmptyFlux() {
        String playerId = "player123";

        when(buildRepository.findByPlayerIdAndValidTrueOrderByCreatedAtDesc(playerId))
                .thenReturn(Flux.empty());

        StepVerifier.create(buildService.getBuildHistory(playerId))
                .verifyComplete();
    }

    @Test
    void getBuildHistory_ErrorEnBaseDeDatos_PropagaError() {

        String playerId = "player123";

        when(buildRepository.findByPlayerIdAndValidTrueOrderByCreatedAtDesc(playerId))
                .thenReturn(Flux.error(new RuntimeException("Error en la base de datos")));

        StepVerifier.create(buildService.getBuildHistory(playerId))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException &&
                                error.getMessage().contains("Error en la base de datos"))
                .verify();
    }

    @Test
    void getBuildHistory_FiltraBuildsInvalidos_RetornaSoloValidos() {
        Build valido = createTestBuild("player1", "char1", true);
        Build invalido = createTestBuild("player1", "char1", false);

        when(buildRepository.findByPlayerIdAndValidTrueOrderByCreatedAtDesc("player1"))
                .thenReturn(Flux.just(valido));

        StepVerifier.create(buildService.getBuildHistory("player1"))
                .expectNext(valido)
                .verifyComplete();
    }
}

