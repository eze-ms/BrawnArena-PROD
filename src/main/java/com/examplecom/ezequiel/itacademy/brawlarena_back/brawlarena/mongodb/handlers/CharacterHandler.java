package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service.CharacterService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        logger.info("Solicitud recibida: obtener personajes gratuitos");
        return characterService.getAllFreeCharacters()
                .collectList()
                .flatMap(characters -> {
                    if (characters.isEmpty()) {
                        return ServerResponse.noContent().build(); // 204 No Content
                    }
                    return ServerResponse.ok().bodyValue(characters);
                });
    }

    //! Devuelve los personajes que el jugador ha desbloqueado
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
    public Mono<ServerResponse> getCharacterDetail(ServerRequest request) {
        String characterId = request.pathVariable("id");
        logger.info("Solicitud recibida: detalles del personaje con ID {}", characterId);

        return characterService.getCharacterDetail(characterId)
                .flatMap(character -> ServerResponse.ok().bodyValue(character));
    }

}