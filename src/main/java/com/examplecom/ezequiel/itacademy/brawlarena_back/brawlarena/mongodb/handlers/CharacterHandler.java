package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterResponse;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterUpdateRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.PieceAssignmentDTO;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.PieceRepository;
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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    private final PieceRepository pieceRepository;


    public CharacterHandler(
            CharacterService characterService,
            JwtService jwtService,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            PieceRepository pieceRepository
    ) {
        this.characterService = characterService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.pieceRepository = pieceRepository;
    }


    private CharacterResponse mapToResponse(Character character, Set<String> unlockedIds) {
        boolean unlocked = unlockedIds.contains(character.getId());

        return new CharacterResponse(
                character.getId(),
                character.getName(),
                character.getDescription(),
                character.getDifficulty(),
                character.getImageUrl(),
                character.getGameImageUrl(),
                character.getCost(),
                character.getPowers(),
                character.getPieces(),
                unlocked
        );
    }

    @Operation(
            summary = "Obtener todos los personajes",
            description = "Devuelve la lista completa de personajes disponibles en el juego, marcando cuáles están desbloqueados por el jugador autenticado.",
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
        return characterService.getAllCharacters()
                .map(this::mapToPublicResponse)
                .collectList()
                .flatMap(list -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(list));
    }

    private CharacterResponse mapToPublicResponse(Character character) {
        return new CharacterResponse(
                character.getId(),
                character.getName(),
                character.getDescription(),
                character.getDifficulty(),
                character.getImageUrl(),
                character.getGameImageUrl(),
                character.getCost(),
                character.getPowers(),
                character.getPieces(),
                false // todos como no desbloqueados
        );
    }

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

    @Operation(
            summary = "Asignar piezas a un personaje",
            description = "Embebe una lista de piezas existentes al personaje indicado por ID.",
            operationId = "assignPiecesToCharacter",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            in = ParameterIn.PATH,
                            name = "id",
                            required = true,
                            description = "ID del personaje a consultar"
                    )

            },
            requestBody = @RequestBody(
                    description = "Lista de IDs de piezas a asignar",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = String.class, type = "array")
                    )
            )
    )
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Piezas asignadas correctamente",
                            content = @Content(schema = @Schema(implementation = Piece.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Lista vacía o error de entrada"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Personaje no encontrado"
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error interno al asignar piezas"
                    )
            }
    )
    public Mono<ServerResponse> assignPiecesToCharacter(ServerRequest request) {
        String characterId = request.pathVariable("id");
        logger.info("Solicitud recibida: asignar piezas al personaje con ID {}", characterId);

        return request.bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .flatMap(pieceIds -> characterService.assignPieces(characterId, pieceIds))
                .flatMap(updated -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updated))
                .onErrorResume(error -> {
                    logger.error("Error al asignar piezas: {}", error.getMessage());
                    if (error instanceof IllegalArgumentException) {
                        return ServerResponse.badRequest().bodyValue(error.getMessage());
                    } else if (error instanceof CharacterNotFoundException) {
                        return ServerResponse.status(404).bodyValue(error.getMessage());
                    } else {
                        return ServerResponse.status(500).bodyValue("Error interno al asignar piezas");
                    }
                });
    }

    @Operation(
            summary = "Asignar piezas con poderes a un personaje",
            description = "Asocia una lista de piezas a un personaje, cada una con su poder correspondiente.",
            operationId = "assignPiecesWithPowers",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            in = ParameterIn.PATH,
                            name = "id",
                            required = true,
                            description = "ID del personaje"
                    )
            },
            requestBody = @RequestBody(
                    required = true,
                    description = "Lista de piezas con poderes",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PieceAssignmentDTO.class))
                    )
            )

    )
    public Mono<ServerResponse> assignPiecesWithPowers(ServerRequest request) {
        String characterId = request.pathVariable("id");

        if (!StringUtils.hasText(characterId)) {
            return ServerResponse.badRequest().bodyValue("El characterId no puede estar vacío");
        }

        return request.bodyToFlux(PieceAssignmentDTO.class)
                .doOnNext(dto -> {
                    if (!StringUtils.hasText(dto.getPieceId())) {
                        throw new IllegalArgumentException("Cada pieceId debe ser válido y no estar vacío");
                    }
                    if (dto.getPower() == null) {
                        throw new IllegalArgumentException("Cada pieza debe tener un poder asignado");
                    }
                })
                .collectList()
                .flatMap(assignments -> {
                    List<String> pieceIds = assignments.stream()
                            .map(PieceAssignmentDTO::getPieceId)
                            .toList();

                    return pieceRepository.findByIdIn(pieceIds)
                            .collectList()
                            .flatMap(piezas -> {
                                if (piezas.size() != assignments.size()) {
                                    return ServerResponse.badRequest().bodyValue("Una o más piezas no existen en la base de datos");
                                }

                                // Asociar poderes
                                for (Piece piece : piezas) {
                                    assignments.stream()
                                            .filter(dto -> dto.getPieceId().equals(piece.getId()))
                                            .findFirst()
                                            .ifPresent(dto -> piece.setPower(dto.getPower()));
                                }

                                return characterService.assignPiecesWithPowers(characterId, piezas)
                                        .flatMap(updated -> ServerResponse.ok().bodyValue(updated));
                            });
                })
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest().bodyValue(e.getMessage()))
                .doOnError(error -> logger.error("Error al asignar piezas con poderes: {}", error.getMessage()));
    }

}