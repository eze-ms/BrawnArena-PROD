package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterResponse;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterUpdateRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.CharacterService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class CharacterHandler {

    private static final Logger logger = LoggerFactory.getLogger(CharacterHandler.class);

    private final JwtService jwtService;
    private final CharacterService characterService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;


    public CharacterHandler(
            CharacterService characterService,
            JwtService jwtService,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.characterService = characterService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }


    private CharacterResponse mapToResponse(Character character, Set<String> unlockedIds) {
        boolean unlocked = unlockedIds.contains(character.getId());

        return new CharacterResponse(
                character.getId(),
                character.getName(),
                character.getDescription(),
                character.getDifficulty(),
                character.getImageUrl(),
                character.getCost(),
                character.getPowers(),
                character.getPieces(),
                unlocked
        );
    }

    //! Devuelve la lista completa de personajes, sin filtrar por desbloqueo o usuario
    @Operation(
            summary = "Obtener todos los personajes",
            description = "Devuelve la lista completa de personajes disponibles en el juego, marcando cuáles están desbloqueados por el jugador autenticado.",
            security = @SecurityRequirement(name = "bearerAuth"),
            operationId = "getAllCharacters"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Personajes obtenidos correctamente.",
                            content = @Content(schema = @Schema(implementation = CharacterResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "204",
                            description = "No hay personajes disponibles."
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al recuperar personajes."
                    )
            }
    )
    public Mono<ServerResponse> getAllCharacters(ServerRequest request) {
        logger.info("Solicitud recibida: obtener todos los personajes");

        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMapMany(playerId ->
                        userRepository.findByNickname(playerId)
                                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado")))
                                .flatMapMany(user -> {
                                    Set<String> unlockedIds;
                                    try {
                                        String characterIdsJson = Optional.ofNullable(user.getCharacterIds()).orElse("[]");
                                        List<String> ids = objectMapper.readValue(characterIdsJson, new TypeReference<>() {});
                                        unlockedIds = new HashSet<>(ids);
                                    } catch (JsonProcessingException e) {
                                        logger.error("Error al deserializar characterIds: {}", e.getMessage());
                                        return Flux.error(new RuntimeException("Error al procesar personajes desbloqueados"));
                                    }

                                    return characterService.getAllCharacters()
                                            .map(character -> mapToResponse(character, unlockedIds));
                                })
                )
                .collectList()
                .flatMap(characterResponses -> {
                    if (characterResponses.isEmpty()) {
                        return ServerResponse.noContent()
                                .header("X-API-Version", "1.0")
                                .build();
                    }
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-API-Version", "1.0")
                            .bodyValue(characterResponses);
                })
                .doOnError(e -> logger.error("Error al recuperar personajes", e));
    }

    //! Devuelve los personajes que el jugador ha desbloqueado
    @Operation(
            summary = "Desbloquear personaje",
            description = "Permite desbloquear un personaje específico para el jugador autenticado mediante su ID.",
            operationId = "unlockCharacter",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Personaje desbloqueado correctamente o ya estaba desbloqueado"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Falta el parámetro 'characterId'"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Personaje no encontrado"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al intentar desbloquear el personaje"
                    )
            }
    )
    public Mono<ServerResponse> getCharacterId(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId -> {
                    logger.info("Solicitud recibida: obtener personajes desbloqueados para playerId: {}", playerId);
                    return characterService.getUnlockedCharacters(playerId)
                            .collectList()
                            .flatMap(characters -> {
                                if (characters.isEmpty()) {
                                    return ServerResponse.noContent().build();
                                }
                                return ServerResponse.ok().bodyValue(characters);
                            });
                });
    }

    //! Permite desbloquear un personaje utilizando tokens, asociándolo con el jugador
    @Operation(
            summary = "Desbloquear personaje",
            description = "Permite desbloquear un personaje específico para el jugador autenticado mediante su ID.",
            operationId = "unlockCharacter",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "characterId",
                            in = ParameterIn.QUERY,
                            required = true,
                            description = "ID del personaje que se desea desbloquear"
                    )
            }
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Personaje desbloqueado correctamente o ya estaba desbloqueado"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Falta el parámetro 'characterId'"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Personaje no encontrado"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al intentar desbloquear el personaje"
                    )
            }
    )
    public Mono<ServerResponse> unlockCharacter(ServerRequest request) {
        String characterId = request.queryParam("characterId")
                .orElse(null);

        if (characterId == null) {
            logger.warn("Falta el parámetro 'characterId' en la solicitud");
            return ServerResponse.badRequest().bodyValue("Parámetro 'characterId' requerido");
        }

        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId -> {
                    logger.info("Solicitud de desbloqueo de personaje {} por parte de playerId: {}", characterId, playerId);
                    return characterService.unlockCharacter(playerId, characterId)
                            .flatMap(unlocked -> {
                                if (unlocked) {
                                    return ServerResponse.ok().bodyValue("Personaje desbloqueado con éxito");
                                } else {
                                    return ServerResponse.ok().bodyValue("El personaje ya estaba desbloqueado");
                                }
                            });
                });
    }

    //! Devuelve la información detallada de un personaje específico, como poderes y dificultad
    @Operation(
            summary = "Obtener detalles de un personaje",
            description = "Devuelve los detalles completos de un personaje específico a partir de su ID.",
            operationId = "getCharacterDetail",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            in = ParameterIn.PATH,
                            name = "id",
                            required = true,
                            description = "ID del personaje a consultar"
                    )

            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalles del personaje encontrados",
                    content = @Content(schema = @Schema(implementation = Character.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Personaje no encontrado"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al recuperar detalles del personaje"
            )
    })
    public Mono<ServerResponse> getCharacterDetail(ServerRequest request) {
        String characterId = request.pathVariable("id");
        logger.info("Solicitud recibida: detalles del personaje con ID {}", characterId);

        return characterService.getCharacterDetail(characterId)
                .flatMap(character -> ServerResponse.ok().bodyValue(character));
    }

    //! Devuelve la lista de los personajes actualizados
    @Operation(
            summary = "Actualizar personaje",
            description = "Actualiza los datos de un personaje existente mediante su ID.",
            operationId = "updateCharacter",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Personaje actualizado correctamente"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Personaje no encontrado"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al actualizar personaje"
                    )
    })
    public Mono<ServerResponse> updateCharacter(ServerRequest request) {
        String characterId = request.pathVariable("id");

        return request.bodyToMono(CharacterUpdateRequest.class)
                .flatMap(updateRequest ->
                        characterService.updateCharacter(characterId, updateRequest)
                                .flatMap(updated ->
                                        ServerResponse.ok().bodyValue(updated)
                                )
                )
                .doOnSubscribe(sub -> logger.info("Solicitud de actualización recibida para personaje {}", characterId))
                .doOnError(error -> logger.error("Error al actualizar personaje {}: {}", characterId, error.getMessage()));
    }

}