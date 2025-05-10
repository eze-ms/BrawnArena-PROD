package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.*;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.GalleryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class GalleryHandler {

    private static final Logger logger = LoggerFactory.getLogger(GalleryHandler.class);
    private final GalleryService galleryService;

    public GalleryHandler(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    public Mono<ServerResponse> getPublicGallery(ServerRequest request) {
        logger.info("Solicitud recibida: obtener galería pública de modelos compartidos");

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(galleryService.getPublicGallery(), SharedModel.class)
                .doOnError(error -> logger.error("Error al procesar galería pública: {}", error.getMessage()));
    }

    public Mono<ServerResponse> shareModel(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new UserNotFoundException("Autenticación requerida")))
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId -> {
                    String characterId = request.queryParam("characterId").orElse(null);
                    if (!StringUtils.hasText(characterId)) {
                        return Mono.error(new IllegalArgumentException("characterId no puede estar vacío"));
                    }

                    logger.info("Solicitud para compartir modelo: playerId={}, characterId={}", playerId, characterId);
                    return galleryService.shareModel(playerId, characterId);
                })
                .flatMap(shared -> {
                    logger.info("Modelo compartido exitosamente");
                    return ServerResponse.ok().bodyValue(shared);
                });
    }

    public Mono<ServerResponse> getHighlightedModel(ServerRequest request) {
        return galleryService.getHighlightedModel()
                .switchIfEmpty(Mono.error(new ModelNotFoundException("No hay jugador destacado actualmente"))) // Lanzar error si no se encuentra el modelo destacado
                .flatMap(model -> {
                    logger.info("Modelo destacado encontrado: playerId={}, characterId={}", model.getPlayerId(), model.getCharacterId());
                    return ServerResponse.ok().bodyValue(model);
                });
    }

    public Mono<ServerResponse> getSharedUsersByCharacter(ServerRequest request) {
        String characterId = request.pathVariable("characterId");

        if (!StringUtils.hasText(characterId)) {
            logger.warn("Falta el parámetro 'characterId' en la solicitud");
            return ServerResponse.badRequest().bodyValue("Parámetro 'characterId' requerido");
        }

        logger.info("Solicitud ADMIN recibida para obtener usuarios que compartieron el personaje {}", characterId);

        return galleryService.getSharedUsersByCharacter(characterId)
                .collectList()
                .doOnSuccess(list -> logger.info("Usuarios que compartieron el personaje {}: {}", characterId, list.size()))
                .flatMap(list -> ServerResponse.ok().bodyValue(list))
                .onErrorResume(e -> {
                    logger.error("Error al obtener usuarios por personaje: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("Error interno");
                });
    }

    public Mono<ServerResponse> highlightModel(ServerRequest request) {
        return request.bodyToMono(String.class)
                .filter(StringUtils::hasText)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("El ID del modelo no puede estar vacío")))
                .doOnNext(id -> logger.info("Solicitud para destacar modelo: sharedModelId={}", id))
                .flatMap(galleryService::highlightModel)
                .flatMap(updated -> {
                    logger.info("🔔Modelo destacado correctamente: {}", updated.getId());
                    return ServerResponse.ok().bodyValue(updated);
                })
                .onErrorResume(ModelNotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(e.getMessage()))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest().bodyValue(e.getMessage()))
                .onErrorResume(e -> {
                    logger.error("Error inesperado al destacar modelo: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("Error interno");
                });
    }

    public Mono<ServerResponse> deleteSharedModel(ServerRequest request) {
        String sharedModelId = request.pathVariable("sharedModelId");

        if (!StringUtils.hasText(sharedModelId)) {
            return ServerResponse.badRequest().bodyValue("sharedModelId no puede estar vacío");
        }

        return request.principal()
                .switchIfEmpty(Mono.error(new UserNotFoundException("Autenticación requerida")))
                .cast(Authentication.class)
                .flatMap(auth -> {
                    String requesterId = auth.getName();
                    String role = auth.getAuthorities().iterator().next().getAuthority();

                    logger.info("🔍 ID del requester: {}", requesterId);
                    logger.info("🔍 Rol recibido: {}", role);


                    return galleryService.deleteSharedModel(sharedModelId, requesterId, role);
                })
                .then(ServerResponse.noContent().build())
                .onErrorResume(UserNotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(e.getMessage()))
                .onErrorResume(AccessDeniedException.class, e ->
                        ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue(e.getMessage()));
    }
}