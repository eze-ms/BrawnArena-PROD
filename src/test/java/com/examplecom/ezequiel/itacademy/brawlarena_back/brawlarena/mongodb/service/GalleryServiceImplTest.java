package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.*;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.BuildRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.SharedModelRepository;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
class GalleryServiceImplTest {

    @InjectMocks
    private GalleryServiceImpl galleryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private BuildRepository buildRepository;

    @Mock
    private SharedModelRepository sharedModelRepository;


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

    @Test
    void shareModel_conPlayerIdOVacioOLNull_lanzaIllegalArgumentException() {
        String validCharacterId = "char1";

        assertThrows(IllegalArgumentException.class,
                () -> galleryService.shareModel(null, validCharacterId).block());

        assertThrows(IllegalArgumentException.class,
                () -> galleryService.shareModel("", validCharacterId).block());

        assertThrows(IllegalArgumentException.class,
                () -> galleryService.shareModel("player1", null).block());

        assertThrows(IllegalArgumentException.class,
                () -> galleryService.shareModel("player1", "").block());
    }

    @Test
    void shareModel_jugadorNoExiste_lanzaUserNotFoundException() {
        String playerId = "player1";
        String characterId = "char1";

        when(userRepository.findByNickname(playerId))
                .thenReturn(Mono.empty());

        when(characterRepository.findById(characterId))
                .thenReturn(Mono.just(createTestCharacter(characterId)));

        StepVerifier.create(galleryService.shareModel(playerId, characterId))
                .expectErrorMatches(ex -> ex instanceof UserNotFoundException &&
                        ex.getMessage().equals("Jugador no encontrado"))
                .verify();
    }

    @Test
    void shareModel_personajeNoExiste_lanzaCharacterNotFoundException() {
        String playerId = "player1";
        String characterId = "char1";

        when(userRepository.findByNickname(playerId))
                .thenReturn(Mono.just(User.builder().nickname(playerId).build()));

        when(characterRepository.findById(characterId))
                .thenReturn(Mono.empty());

        StepVerifier.create(galleryService.shareModel(playerId, characterId))
                .expectErrorMatches(ex -> ex instanceof CharacterNotFoundException &&
                        ex.getMessage().equals("Personaje no encontrado"))
                .verify();
    }

    @Test
    void shareModel_sinBuildsValidos_lanzaBuildNotFoundException() {
        String playerId = "player1";
        String characterId = "char1";

        User mockUser = User.builder().nickname(playerId).build();
        Character mockCharacter = createTestCharacter(characterId);

        when(userRepository.findByNickname(playerId))
                .thenReturn(Mono.just(mockUser));

        when(characterRepository.findById(characterId))
                .thenReturn(Mono.just(mockCharacter));

        when(buildRepository.findByPlayerIdAndCharacterIdAndValidTrue(playerId, characterId))
                .thenReturn(Flux.empty());

        StepVerifier.create(galleryService.shareModel(playerId, characterId))
                .expectErrorMatches(ex -> ex instanceof BuildNotFoundException &&
                        ex.getMessage().equals("No has completado ningún montaje válido para este personaje"))
                .verify();
    }

    @Test
    void shareModel_conBuildValido_guardaYDevuelveSharedModel() {
        String playerId = "player1";
        String characterId = "char1";

        User mockUser = User.builder().nickname(playerId).build();
        Character mockCharacter = createTestCharacter(characterId);

        Build validBuild = new Build();
        validBuild.setPlayerId(playerId);
        validBuild.setCharacterId(characterId);
        validBuild.setValid(true);
        validBuild.setScore(85);
        validBuild.setCreatedAt(Instant.now());

        SharedModel expectedModel = new SharedModel();
        expectedModel.setPlayerId(playerId);
        expectedModel.setCharacterId(characterId);
        expectedModel.setScore(85);
        expectedModel.setPowers(List.of());
        expectedModel.setSharedAt(Instant.now());

        when(userRepository.findByNickname(playerId))
                .thenReturn(Mono.just(mockUser));

        when(characterRepository.findById(characterId))
                .thenReturn(Mono.just(mockCharacter));

        when(buildRepository.findByPlayerIdAndCharacterIdAndValidTrue(playerId, characterId))
                .thenReturn(Flux.just(validBuild));

        when(sharedModelRepository.save(any(SharedModel.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(galleryService.shareModel(playerId, characterId))
                .expectNextMatches(shared ->
                        shared.getPlayerId().equals(playerId) &&
                                shared.getCharacterId().equals(characterId) &&
                                shared.getScore() == 85 &&
                                shared.getPowers().isEmpty() &&
                                shared.getSharedAt() != null
                )
                .verifyComplete();
    }

    @Test
    void shareModel_errorAlGuardarSharedModel_propagaExcepcion() {
        String playerId = "player1";
        String characterId = "char1";

        User mockUser = User.builder().nickname(playerId).build();
        Character mockCharacter = createTestCharacter(characterId);

        Build validBuild = new Build();
        validBuild.setPlayerId(playerId);
        validBuild.setCharacterId(characterId);
        validBuild.setValid(true);
        validBuild.setScore(90);
        validBuild.setCreatedAt(Instant.now());

        when(userRepository.findByNickname(playerId))
                .thenReturn(Mono.just(mockUser));

        when(characterRepository.findById(characterId))
                .thenReturn(Mono.just(mockCharacter));

        when(buildRepository.findByPlayerIdAndCharacterIdAndValidTrue(playerId, characterId))
                .thenReturn(Flux.just(validBuild));

        when(sharedModelRepository.save(any(SharedModel.class)))
                .thenReturn(Mono.error(new RuntimeException("Error al guardar modelo")));

        StepVerifier.create(galleryService.shareModel(playerId, characterId))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Error al guardar modelo"))
                .verify();
    }

    @Test
    void shareModel_seleccionaBuildMasReciente_correctamente() {
        String playerId = "player1";
        String characterId = "char1";

        User mockUser = User.builder().nickname(playerId).build();
        Character mockCharacter = createTestCharacter(characterId);

        Build buildAntiguo = new Build();
        buildAntiguo.setPlayerId(playerId);
        buildAntiguo.setCharacterId(characterId);
        buildAntiguo.setValid(true);
        buildAntiguo.setScore(70);
        buildAntiguo.setCreatedAt(Instant.parse("2024-01-01T10:00:00Z"));

        Build buildReciente = new Build();
        buildReciente.setPlayerId(playerId);
        buildReciente.setCharacterId(characterId);
        buildReciente.setValid(true);
        buildReciente.setScore(95);
        buildReciente.setCreatedAt(Instant.parse("2024-04-01T10:00:00Z"));

        when(userRepository.findByNickname(playerId))
                .thenReturn(Mono.just(mockUser));

        when(characterRepository.findById(characterId))
                .thenReturn(Mono.just(mockCharacter));

        when(buildRepository.findByPlayerIdAndCharacterIdAndValidTrue(playerId, characterId))
                .thenReturn(Flux.just(buildAntiguo, buildReciente));

        when(sharedModelRepository.save(any(SharedModel.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(galleryService.shareModel(playerId, characterId))
                .expectNextMatches(shared ->
                        shared.getScore() == 95 &&
                                shared.getPlayerId().equals(playerId) &&
                                shared.getCharacterId().equals(characterId)
                )
                .verifyComplete();
    }

    @Test
    void getPublicGallery_conModelosCompartidos_retornaFluxOrdenado() {
        SharedModel modelo1 = new SharedModel();
        modelo1.setPlayerId("player1");
        modelo1.setCharacterId("char1");
        modelo1.setSharedAt(Instant.parse("2024-01-01T10:00:00Z"));

        SharedModel modelo2 = new SharedModel();
        modelo2.setPlayerId("player2");
        modelo2.setCharacterId("char2");
        modelo2.setSharedAt(Instant.parse("2024-03-01T10:00:00Z"));

        when(sharedModelRepository.findAll())
                .thenReturn(Flux.just(modelo1, modelo2));

        StepVerifier.create(galleryService.getPublicGallery())
                .expectNext(modelo2) // más reciente
                .expectNext(modelo1)
                .verifyComplete();
    }

    @Test
    void getPublicGallery_sinModelosCompartidos_retornaFluxVacio() {
        when(sharedModelRepository.findAll())
                .thenReturn(Flux.empty());

        StepVerifier.create(galleryService.getPublicGallery())
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void getPublicGallery_errorEnBaseDeDatos_propagaExcepcion() {
        when(sharedModelRepository.findAll())
                .thenReturn(Flux.error(new RuntimeException("Error en la base de datos")));

        StepVerifier.create(galleryService.getPublicGallery())
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Error en la base de datos"))
                .verify();
    }

    @Test
    void getPublicGallery_verificaOrdenPorFecha_descendente() {
        SharedModel modeloAntiguo = new SharedModel();
        modeloAntiguo.setPlayerId("playerA");
        modeloAntiguo.setCharacterId("charA");
        modeloAntiguo.setSharedAt(Instant.parse("2023-05-01T10:00:00Z"));

        SharedModel modeloReciente = new SharedModel();
        modeloReciente.setPlayerId("playerB");
        modeloReciente.setCharacterId("charB");
        modeloReciente.setSharedAt(Instant.parse("2024-02-01T10:00:00Z"));

        SharedModel modeloIntermedio = new SharedModel();
        modeloIntermedio.setPlayerId("playerC");
        modeloIntermedio.setCharacterId("charC");
        modeloIntermedio.setSharedAt(Instant.parse("2023-12-01T10:00:00Z"));

        when(sharedModelRepository.findAll())
                .thenReturn(Flux.just(modeloAntiguo, modeloReciente, modeloIntermedio));

        StepVerifier.create(galleryService.getPublicGallery())
                .expectNext(modeloReciente)
                .expectNext(modeloIntermedio)
                .expectNext(modeloAntiguo)
                .verifyComplete();
    }

    @Test
    void getHighlightedModel_conModeloDestacado_retornaSharedModel() {
        SharedModel destacado = new SharedModel();
        destacado.setPlayerId("player1");
        destacado.setCharacterId("char1");
        destacado.setHighlighted(true);
        destacado.setSharedAt(Instant.now());

        when(sharedModelRepository.findByHighlightedTrue())
                .thenReturn(Flux.just(destacado));

        StepVerifier.create(galleryService.getHighlightedModel())
                .expectNextMatches(model ->
                        model.getPlayerId().equals("player1") &&
                                model.getCharacterId().equals("char1") &&
                                model.isHighlighted()
                )
                .verifyComplete();
    }

    @Test
    void getHighlightedModel_sinModeloDestacado_lanzaExcepcion() {
        when(sharedModelRepository.findByHighlightedTrue())
                .thenReturn(Flux.empty());

        StepVerifier.create(galleryService.getHighlightedModel())
                .expectErrorMatches(ex ->
                        ex instanceof HighlightedModelNotFoundException &&
                                ex.getMessage().equals("No hay jugador destacado actualmente"))
                .verify();
    }

    @Test
    void getHighlightedModel_errorEnBaseDeDatos_propagaExcepcion() {
        when(sharedModelRepository.findByHighlightedTrue())
                .thenReturn(Flux.error(new RuntimeException("Error en MongoDB")));

        StepVerifier.create(galleryService.getHighlightedModel())
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Error en MongoDB"))
                .verify();
    }

    @Test
    void getHighlightedModel_multiplesDestacados_retornaSoloPrimero() {
        SharedModel destacado1 = new SharedModel();
        destacado1.setPlayerId("player1");
        destacado1.setCharacterId("char1");
        destacado1.setHighlighted(true);
        destacado1.setSharedAt(Instant.parse("2024-03-01T10:00:00Z"));

        SharedModel destacado2 = new SharedModel();
        destacado2.setPlayerId("player2");
        destacado2.setCharacterId("char2");
        destacado2.setHighlighted(true);
        destacado2.setSharedAt(Instant.parse("2024-04-01T10:00:00Z"));

        when(sharedModelRepository.findByHighlightedTrue())
                .thenReturn(Flux.just(destacado1, destacado2));

        StepVerifier.create(galleryService.getHighlightedModel())
                .expectNext(destacado1)
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_conCharacterIdVacioOLNull_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> galleryService.getSharedUsersByCharacter(null).collectList().block());

        assertThrows(IllegalArgumentException.class,
                () -> galleryService.getSharedUsersByCharacter("").collectList().block());
    }

    @Test
    void getSharedUsersByCharacter_conModelosCompartidos_retornaPlayerIdsUnicos() {
        String characterId = "char1";

        SharedModel modelo1 = new SharedModel();
        modelo1.setPlayerId("player1");
        modelo1.setCharacterId(characterId);

        SharedModel modelo2 = new SharedModel();
        modelo2.setPlayerId("player2");
        modelo2.setCharacterId(characterId);

        SharedModel modelo3 = new SharedModel();
        modelo3.setPlayerId("player1"); // repetido
        modelo3.setCharacterId(characterId);

        when(sharedModelRepository.findByCharacterId(characterId))
                .thenReturn(Flux.just(modelo1, modelo2, modelo3));

        StepVerifier.create(galleryService.getSharedUsersByCharacter(characterId).sort())
                .expectNext("player1", "player2")
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_sinModelosCompartidos_retornaFluxVacio() {
        String characterId = "char1";

        when(sharedModelRepository.findByCharacterId(characterId))
                .thenReturn(Flux.empty());

        StepVerifier.create(galleryService.getSharedUsersByCharacter(characterId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_conModelosDuplicados_seFiltranCorrectamente() {
        String characterId = "char1";

        SharedModel modelo1 = new SharedModel();
        modelo1.setPlayerId("player1");
        modelo1.setCharacterId(characterId);

        SharedModel modelo2 = new SharedModel();
        modelo2.setPlayerId("player2");
        modelo2.setCharacterId(characterId);

        SharedModel modelo3 = new SharedModel();
        modelo3.setPlayerId("player1"); // duplicado
        modelo3.setCharacterId(characterId);

        when(sharedModelRepository.findByCharacterId(characterId))
                .thenReturn(Flux.just(modelo1, modelo2, modelo3));

        StepVerifier.create(galleryService.getSharedUsersByCharacter(characterId).sort())
                .expectNext("player1", "player2") // solo player1 y player2, sin duplicados
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_errorEnBaseDeDatos_propagaExcepcion() {
        String characterId = "char1";

        when(sharedModelRepository.findByCharacterId(characterId))
                .thenReturn(Flux.error(new RuntimeException("Error en la base de datos")));

        StepVerifier.create(galleryService.getSharedUsersByCharacter(characterId))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Error en la base de datos"))
                .verify();
    }

    @Test
    void highlightModel_conSharedModelIdVacioOLNull_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> galleryService.highlightModel(null).block());

        assertThrows(IllegalArgumentException.class,
                () -> galleryService.highlightModel("").block());
    }

    @Test
    void highlightModel_modeloNoExiste_lanzaModelNotFoundException() {
        String sharedModelId = "model1";

        when(sharedModelRepository.findById(sharedModelId))
                .thenReturn(Mono.empty());

        StepVerifier.create(galleryService.highlightModel(sharedModelId))
                .expectErrorMatches(ex ->
                        ex instanceof ModelNotFoundException &&
                                ex.getMessage().equals("Modelo compartido no encontrado"))
                .verify();
    }

    @Test
    void highlightModel_modeloExiste_markAsHighlighted_yDesmarcaOtros() {
        String sharedModelId = "model1";

        SharedModel modeloExistente = new SharedModel();
        modeloExistente.setId(sharedModelId);
        modeloExistente.setHighlighted(false);

        SharedModel modeloDestacado = new SharedModel();
        modeloDestacado.setId("model2");
        modeloDestacado.setHighlighted(true);

        when(sharedModelRepository.findById(sharedModelId))
                .thenReturn(Mono.just(modeloExistente));

        when(sharedModelRepository.findByHighlightedTrue())
                .thenReturn(Flux.just(modeloDestacado));

        when(sharedModelRepository.save(any(SharedModel.class)))
                .thenReturn(Mono.just(modeloExistente));

        StepVerifier.create(galleryService.highlightModel(sharedModelId))
                .expectNextMatches(updatedModel ->
                        updatedModel.isHighlighted() &&
                                updatedModel.getId().equals(sharedModelId) // modelo marcado correctamente
                )
                .verifyComplete();

        verify(sharedModelRepository, times(1)).save(modeloDestacado); // se desmarca el modelo anterior
    }

    @Test
    void highlightModel_errorAlDesmarcarModelosDestacados_propagaExcepcion() {
        String sharedModelId = "model1";

        SharedModel modeloExistente = new SharedModel();
        modeloExistente.setId(sharedModelId);
        modeloExistente.setHighlighted(false);

        SharedModel modeloDestacado = new SharedModel();
        modeloDestacado.setId("model2");
        modeloDestacado.setHighlighted(true);

        when(sharedModelRepository.findById(sharedModelId))
                .thenReturn(Mono.just(modeloExistente));

        when(sharedModelRepository.findByHighlightedTrue())
                .thenReturn(Flux.just(modeloDestacado));

        when(sharedModelRepository.save(any(SharedModel.class)))
                .thenReturn(Mono.just(modeloExistente));

        when(sharedModelRepository.save(modeloDestacado))
                .thenReturn(Mono.error(new RuntimeException("Error al desmarcar modelo")));

        StepVerifier.create(galleryService.highlightModel(sharedModelId))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Error al desmarcar modelo"))
                .verify();
    }

    @Test
    void highlightModel_errorAlGuardarModeloDestacado_propagaExcepcion() {
        String sharedModelId = "model1";

        SharedModel modeloExistente = new SharedModel();
        modeloExistente.setId(sharedModelId);
        modeloExistente.setHighlighted(false);

        SharedModel modeloDestacado = new SharedModel();
        modeloDestacado.setId("model2");
        modeloDestacado.setHighlighted(true);

        when(sharedModelRepository.findById(sharedModelId))
                .thenReturn(Mono.just(modeloExistente));

        when(sharedModelRepository.findByHighlightedTrue())
                .thenReturn(Flux.just(modeloDestacado));

        when(sharedModelRepository.save(any(SharedModel.class)))
                .thenReturn(Mono.just(modeloExistente));

        when(sharedModelRepository.save(modeloDestacado))
                .thenReturn(Mono.error(new RuntimeException("Error al guardar modelo destacado")));

        StepVerifier.create(galleryService.highlightModel(sharedModelId))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException &&
                                ex.getMessage().equals("Error al guardar modelo destacado"))
                .verify();
    }


}