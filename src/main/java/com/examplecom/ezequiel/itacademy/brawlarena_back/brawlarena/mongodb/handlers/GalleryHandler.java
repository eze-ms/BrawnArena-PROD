package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.BuildNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.HighlightedModelNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.GalleryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class GalleryHandler {

    private static final Logger logger = LoggerFactory.getLogger(SharedModel.class);
    private final GalleryService galleryService;

    public GalleryHandler(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @Operation(
            summary = "Obtener galería pública",
            description = "Devuelve la lista de todos los modelos compartidos públicamente por los jugadores.",
            security = @SecurityRequirement(name = "bearerAuth"),
            operationId = "getPublicGallery"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Modelos compartidos obtenidos correctamente",
                    content = @Content(schema = @Schema(implementation = SharedModel.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al recuperar la galería"
            )
    })
    public Mono<ServerResponse> getPublicGallery(ServerRequest request) {
        logger.info("Solicitud recibida: obtener galería pública de modelos compartidos");

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(galleryService.getPublicGallery(), SharedModel.class)
                .doOnError(error -> logger.error("Error al procesar galería pública: {}", error.getMessage()));
    }

    @Operation(
            summary = "Compartir modelo completado",
            description = "Permite al jugador compartir un modelo completado en la galería pública. Requiere autenticación.",
            operationId = "shareModel",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(
                    description = "ID del personaje completado que se quiere compartir",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(
                                    name = "characterId",
                                    value = "\"680743b8485a1c9f6c909003\""
                            )
                    )
            )
    )
    @ApiResponses(
            value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Modelo compartido correctamente",
                    content = @Content(schema = @Schema(implementation = SharedModel.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o modelo no completado"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Autenticación requerida"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Build válido no encontrado"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al compartir modelo"
            )
    })
    public Mono<ServerResponse> shareModel(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(Mono.error(new UserNotFoundException("Autenticación requerida")))
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId ->
                        request.bodyToMono(String.class)
                                .flatMap(characterId -> {
                                    logger.info("Solicitud para compartir modelo: playerId={}, characterId={}", playerId, characterId);
                                    return galleryService.shareModel(playerId, characterId)
                                            .doOnSuccess(shared -> logger.info("Modelo compartido exitosamente por {}", playerId))
                                            .flatMap(shared -> ServerResponse.ok().bodyValue(shared));
                                })
                )
                .onErrorResume(UserNotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.UNAUTHORIZED).bodyValue(e.getMessage()))
                .onErrorResume(BuildNotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(e.getMessage()))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest().bodyValue(e.getMessage()))
                .onErrorResume(e -> {
                    logger.error("Error inesperado al compartir modelo: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("Error interno");
                });
    }

    @Operation(
            summary = "Obtener modelo destacado",
            description = "Devuelve el modelo compartido que ha sido marcado como destacado (Jugador de la Semana).",
            operationId = "getHighlightedModel"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Modelo destacado obtenido correctamente",
                    content = @Content(schema = @Schema(implementation = SharedModel.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No hay modelo destacado actualmente"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al obtener el modelo destacado"
            )
    })
    public Mono<ServerResponse> getHighlightedModel(ServerRequest request) {
        logger.info("Solicitud recibida: obtener modelo destacado");

        return galleryService.getHighlightedModel()
                .doOnSuccess(model -> logger.info("Modelo destacado encontrado: playerId={}, characterId={}", model.getPlayerId(), model.getCharacterId()))
                .flatMap(model -> ServerResponse.ok().bodyValue(model))
                .onErrorResume(HighlightedModelNotFoundException.class, e ->
                        ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue(e.getMessage()))
                .onErrorResume(e -> {
                    logger.error("Error al obtener modelo destacado: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).bodyValue("Error interno");
                });
    }

    @Operation(
            summary = "Obtener usuarios que compartieron un personaje",
            description = "Devuelve los IDs de jugadores que han compartido públicamente el modelo de un personaje específico.",
            operationId = "getSharedUsersByCharacter",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "characterId",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "ID del personaje compartido"
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de usuarios obtenida correctamente",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetro 'characterId' faltante o inválido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado: se requiere rol ADMIN"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al obtener los usuarios"
            )
    })
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

}