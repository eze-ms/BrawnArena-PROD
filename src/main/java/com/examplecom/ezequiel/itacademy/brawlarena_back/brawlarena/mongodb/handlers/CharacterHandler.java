package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.CharacterService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CharacterHandler {

    private static final Logger logger = LoggerFactory.getLogger(CharacterHandler.class);
    private final JwtService jwtService;
    private final CharacterService characterService;

    public CharacterHandler(CharacterService characterService, JwtService jwtService) {
        this.characterService = characterService;
        this.jwtService = jwtService;
    }

    //! Devuelve la lista de personajes gratuitos
    public Mono<ServerResponse> getCharacterAllId(ServerRequest request) {
        return characterService.getAllFreeCharacters()
                .doOnSubscribe(sub -> logger.info("Solicitud recibida: obtener personajes gratuitos"))
                .collectList()
                .flatMap(characters -> {
                    if (characters.isEmpty()) {
                        logger.warn("No hay personajes gratuitos disponibles.");
                        return ServerResponse.noContent().build(); // 204 No Content
                    }
                    logger.info("Personajes gratuitos encontrados: {}", characters.size());
                    return ServerResponse.ok().bodyValue(characters);
                })
                .doOnError(e -> logger.error("Error al obtener personajes gratuitos: {}", e.getMessage()))
                .onErrorResume(e -> ServerResponse
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .bodyValue("Error al recuperar personajes gratuitos"));
    }


    //! Devuelve los personajes que el jugador ha desbloqueado
    public Mono<ServerResponse> getCharacterId(ServerRequest request) {
        return request.principal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .flatMap(playerId -> {
                    logger.info("Solicitud recibida: personajes desbloqueados para playerId: {}", playerId);
                    return characterService.getUnlockedCharacters(playerId)
                            .collectList()
                            .flatMap(characters -> {
                                if (characters.isEmpty()) {
                                    logger.warn("No hay personajes desbloqueados para este jugador.");
                                    return ServerResponse.noContent().build();
                                }
                                logger.info("Personajes desbloqueados encontrados: {}", characters.size());
                                return ServerResponse.ok().bodyValue(characters);
                            });
                })
                .doOnError(e -> logger.error("Error al obtener personajes desbloqueados: {}", e.getMessage()))
                .onErrorResume(e -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .bodyValue("Error al recuperar personajes desbloqueados"));
    }


    //! Permite desbloquear un personaje utilizando tokens, asociándolo con el jugador
//    public Mono<ServerResponse> unlockCharacter(ServerRequest request) {
//        String playerId = request.pathVariable("playerId");
//        String characterId = request.queryParam("characterId").orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Character ID is required"));
//        return characterService.unlockCharacter(playerId, characterId)
//                .flatMap(unlocked -> ServerResponse.ok().bodyValue("Character unlocked successfully"))
//                .onErrorResume(e -> ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue("Not enough tokens or error unlocking character"));
//    }

    //! Devuelve la información detallada de un personaje específico, como poderes y dificultad
//    public Mono<ServerResponse> getCharacterDetail(ServerRequest request) {
//        String characterId = request.pathVariable("characterId");
//        return characterService.getCharacterDetail(characterId)
//                .flatMap(character -> ServerResponse.ok().bodyValue(character))
//                .onErrorResume(e -> ServerResponse.status(HttpStatus.NOT_FOUND).bodyValue("Character not found"));
//    }

}