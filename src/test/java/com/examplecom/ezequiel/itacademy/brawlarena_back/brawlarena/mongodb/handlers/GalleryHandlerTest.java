package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.BuildNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.HighlightedModelNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.ModelNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.GalleryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryHandlerTest {

    @Mock
    private GalleryService galleryService;

    @Mock
    private ServerRequest request;

    @InjectMocks
    private GalleryHandler galleryHandler;

    @Test
    void getPublicGallery_ReturnsOkWithModels() {
        SharedModel model1 = new SharedModel();
        model1.setId("model1");

        SharedModel model2 = new SharedModel();
        model2.setId("model2");

        when(galleryService.getPublicGallery())
                .thenReturn(Flux.just(model1, model2));

        StepVerifier.create(galleryHandler.getPublicGallery(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    assertInstanceOf(EntityResponse.class, response);

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(Publisher.class, body);

                    @SuppressWarnings("unchecked")
                    Flux<SharedModel> flux = (Flux<SharedModel>) body;

                    StepVerifier.create(flux)
                            .expectNextMatches(m -> "model1".equals(m.getId()))
                            .expectNextMatches(m -> "model2".equals(m.getId()))
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    void getPublicGallery_EmptyGallery_ReturnsOkWithEmptyFlux() {
        when(galleryService.getPublicGallery())
                .thenReturn(Flux.empty());

        StepVerifier.create(galleryHandler.getPublicGallery(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    assertInstanceOf(EntityResponse.class, response);

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(Publisher.class, body);

                    @SuppressWarnings("unchecked")
                    Flux<SharedModel> flux = (Flux<SharedModel>) body;

                    StepVerifier.create(flux)
                            .verifyComplete(); // Confirma que está vacío
                })
                .verifyComplete();
    }

    @Test
    void getPublicGallery_ServiceError_PropagatesErrorFromFlux() {
        when(galleryService.getPublicGallery())
                .thenReturn(Flux.error(new RuntimeException("Error interno de la galería")));

        StepVerifier.create(galleryHandler.getPublicGallery(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    assertInstanceOf(EntityResponse.class, response);

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(Publisher.class, body);

                    @SuppressWarnings("unchecked")
                    Flux<SharedModel> flux = (Flux<SharedModel>) body;

                    StepVerifier.create(flux)
                            .expectErrorMatches(error ->
                                    error instanceof RuntimeException &&
                                            error.getMessage().contains("Error interno de la galería"))
                            .verify();
                })
                .verifyComplete();
    }

    @Test
    void getPublicGallery_ReturnsResponseWithJsonContentType() {
        SharedModel model = new SharedModel();
        model.setId("model1");

        when(galleryService.getPublicGallery())
                .thenReturn(Flux.just(model));

        StepVerifier.create(galleryHandler.getPublicGallery(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());
                    assertEquals(MediaType.APPLICATION_JSON, response.headers().getContentType());
                })
                .verifyComplete();
    }

    @Test
    void shareModel_Success_ReturnsOkWithSharedModel() {
        String playerId = "player123";
        String characterId = "char456";

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        SharedModel shared = new SharedModel();
        shared.setId("shared789");
        shared.setCharacterId(characterId);
        shared.setPlayerId(playerId);

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just(characterId));

        when(galleryService.shareModel(playerId, characterId))
                .thenReturn(Mono.just(shared));

        StepVerifier.create(galleryHandler.shareModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(SharedModel.class, body);

                    SharedModel result = (SharedModel) body;
                    assertEquals("shared789", result.getId());
                    assertEquals(characterId, result.getCharacterId());
                    assertEquals(playerId, result.getPlayerId());
                })
                .verifyComplete();
    }

    @Test
    void shareModel_InvalidBody_ReturnsBadRequest() {
        String playerId = "player123";
        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just("")); // ahora el filtro detecta vacío o solo espacios

        StepVerifier.create(galleryHandler.shareModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("characterId no puede estar vacío"));
                })
                .verifyComplete();
    }

    @Test
    void shareModel_MissingAuthentication_ReturnsUnauthorized() {
        when(request.principal())
                .thenReturn(Mono.empty());

        StepVerifier.create(galleryHandler.shareModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("Autenticación requerida"));
                })
                .verifyComplete();
    }

    @Test
    void shareModel_BuildNotFound_ReturnsNotFound() {
        String playerId = "player123";
        String characterId = "char456";

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just(characterId));

        when(galleryService.shareModel(playerId, characterId))
                .thenReturn(Mono.error(new BuildNotFoundException("No se encontró build válido")));

        StepVerifier.create(galleryHandler.shareModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("build válido"));
                })
                .verifyComplete();
    }

    @Test
    void shareModel_UnexpectedError_ReturnsInternalServerError() {
        String playerId = "player123";
        String characterId = "char456";

        Authentication auth = new UsernamePasswordAuthenticationToken(playerId, "");

        when(request.principal())
                .thenAnswer(inv -> Mono.just(auth));

        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just(characterId));

        when(galleryService.shareModel(playerId, characterId))
                .thenReturn(Mono.error(new RuntimeException("Error inesperado")));

        StepVerifier.create(galleryHandler.shareModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("Error interno", body);
                })
                .verifyComplete();
    }

    @Test
    void getHighlightedModel_Success_ReturnsOkWithSharedModel() {
        SharedModel highlighted = new SharedModel();
        highlighted.setId("highlighted123");
        highlighted.setPlayerId("player123");
        highlighted.setCharacterId("char456");

        when(galleryService.getHighlightedModel())
                .thenReturn(Mono.just(highlighted));

        StepVerifier.create(galleryHandler.getHighlightedModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(SharedModel.class, body);

                    SharedModel result = (SharedModel) body;
                    assertEquals("highlighted123", result.getId());
                    assertEquals("player123", result.getPlayerId());
                    assertEquals("char456", result.getCharacterId());
                })
                .verifyComplete();
    }

    @Test
    void getHighlightedModel_NotFound_ReturnsNotFound() {
        when(galleryService.getHighlightedModel())
                .thenReturn(Mono.error(new HighlightedModelNotFoundException("No hay modelo destacado")));

        StepVerifier.create(galleryHandler.getHighlightedModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("modelo destacado"));
                })
                .verifyComplete();
    }

    @Test
    void getHighlightedModel_UnexpectedError_ReturnsInternalServerError() {
        when(galleryService.getHighlightedModel())
                .thenReturn(Mono.error(new RuntimeException("Error inesperado")));

        StepVerifier.create(galleryHandler.getHighlightedModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("Error interno", body);
                })
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_MissingCharacterId_ReturnsBadRequest() {
        when(request.pathVariable("characterId"))
                .thenReturn("");

        StepVerifier.create(galleryHandler.getSharedUsersByCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("Parámetro 'characterId' requerido"));
                })
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_WithUsers_ReturnsOkWithList() {
        String characterId = "char123";
        List<String> users = List.of("player1", "player2");

        when(request.pathVariable("characterId"))
                .thenReturn(characterId);

        when(galleryService.getSharedUsersByCharacter(characterId))
                .thenReturn(Flux.fromIterable(users));

        StepVerifier.create(galleryHandler.getSharedUsersByCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(List.class, body);

                    @SuppressWarnings("unchecked")
                    List<String> result = (List<String>) body;

                    assertEquals(2, result.size());
                    assertEquals("player1", result.get(0));
                    assertEquals("player2", result.get(1));
                })
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_NoUsers_ReturnsOkWithEmptyList() {
        String characterId = "char123";

        when(request.pathVariable("characterId"))
                .thenReturn(characterId);

        when(galleryService.getSharedUsersByCharacter(characterId))
                .thenReturn(Flux.empty());

        StepVerifier.create(galleryHandler.getSharedUsersByCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(List.class, body);

                    @SuppressWarnings("unchecked")
                    List<String> result = (List<String>) body;

                    assertTrue(result.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void getSharedUsersByCharacter_ServiceError_ReturnsInternalServerError() {
        String characterId = "char123";

        when(request.pathVariable("characterId"))
                .thenReturn(characterId);

        when(galleryService.getSharedUsersByCharacter(characterId))
                .thenReturn(Flux.error(new RuntimeException("Error inesperado")));

        StepVerifier.create(galleryHandler.getSharedUsersByCharacter(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("Error interno", body);
                })
                .verifyComplete();
    }

    @Test
    void highlightModel_Success_ReturnsOkWithSharedModel() {
        String sharedModelId = "shared123";

        SharedModel updated = new SharedModel();
        updated.setId(sharedModelId);
        updated.setCharacterId("char456");
        updated.setPlayerId("player123");

        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just(sharedModelId));

        when(galleryService.highlightModel(sharedModelId))
                .thenReturn(Mono.just(updated));

        StepVerifier.create(galleryHandler.highlightModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(SharedModel.class, body);

                    SharedModel result = (SharedModel) body;
                    assertEquals(sharedModelId, result.getId());
                    assertEquals("char456", result.getCharacterId());
                    assertEquals("player123", result.getPlayerId());
                })
                .verifyComplete();
    }

    @Test
    void highlightModel_EmptyBody_ReturnsBadRequest() {
        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just("")); // cuerpo vacío

        StepVerifier.create(galleryHandler.highlightModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("ID del modelo no puede estar vacío"));
                })
                .verifyComplete();
    }

    @Test
    void highlightModel_ModelNotFound_ReturnsNotFound() {
        String sharedModelId = "shared123";

        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just(sharedModelId));

        when(galleryService.highlightModel(sharedModelId))
                .thenReturn(Mono.error(new ModelNotFoundException("Modelo no encontrado")));

        StepVerifier.create(galleryHandler.highlightModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertTrue(((String) body).contains("Modelo no encontrado"));
                })
                .verifyComplete();
    }

    @Test
    void highlightModel_UnexpectedError_ReturnsInternalServerError() {
        String sharedModelId = "shared123";

        when(request.bodyToMono(String.class))
                .thenReturn(Mono.just(sharedModelId));

        when(galleryService.highlightModel(sharedModelId))
                .thenReturn(Mono.error(new RuntimeException("Fallo interno")));

        StepVerifier.create(galleryHandler.highlightModel(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode());

                    Object body = ((EntityResponse<?>) response).entity();
                    assertInstanceOf(String.class, body);
                    assertEquals("Error interno", body);
                })
                .verifyComplete();
    }

}