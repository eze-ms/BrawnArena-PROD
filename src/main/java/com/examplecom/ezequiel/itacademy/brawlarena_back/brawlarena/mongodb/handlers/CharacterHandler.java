package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterResponse;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterUpdateRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.PieceAssignmentDTO;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.PieceRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.CharacterService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
public class CharacterHandler {

    private static final Logger logger = LoggerFactory.getLogger(CharacterHandler.class);

    private final JwtService jwtService;
    private final CharacterService characterService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PieceRepository pieceRepository;
    private final CharacterRepository characterRepository;


    public CharacterHandler(
            CharacterService characterService,
            JwtService jwtService,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            PieceRepository pieceRepository,
            CharacterRepository characterRepository
    ) {
        this.characterService = characterService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.pieceRepository = pieceRepository;
        this.characterRepository = characterRepository;
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

    public Mono<ServerResponse> getCharacterDetail(ServerRequest request) {
        String characterId = request.pathVariable("id");
        logger.info("Solicitud recibida: detalles del personaje con ID {}", characterId);

        return characterService.getCharacterDetail(characterId)
                .flatMap(character -> ServerResponse.ok().bodyValue(character));
    }

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

    public Mono<ServerResponse> testMongoConnection(ServerRequest request) {
        return characterRepository.findAll()
                .collectList()
                .flatMap(characters -> ServerResponse.ok().bodyValue(characters))
                .onErrorResume(e -> {
                    logger.error("Error al conectar con Mongo: {}", e.getMessage());
                    return ServerResponse.status(500).bodyValue("Error al conectar con Mongo");
                });
    }

    public Mono<ServerResponse> getFreeCharacters(ServerRequest request) {
        return characterRepository.findAll()
                .filter(c -> c.getCost() == 0)
                .collectList()
                .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }

}