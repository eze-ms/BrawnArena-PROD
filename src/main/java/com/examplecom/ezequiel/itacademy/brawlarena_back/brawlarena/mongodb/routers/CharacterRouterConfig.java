package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.CharacterHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@Configuration
@Tag(name = "Characters", description = "Endpoints para gestionar los personajes")
public class CharacterRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> characterRoutes(CharacterHandler characterHandler) {
        return route()
                .GET("/characters/all", characterHandler::getAllCharacters)
                .GET("/characters/unlocked", characterHandler::getCharacterId)
                .POST("/characters/unlock", characterHandler::unlockCharacter)
                .GET("/characters/{id}", characterHandler::getCharacterDetail)
                .PUT("/characters/{id}", characterHandler::updateCharacter)
                .POST("/characters/{id}/pieces", characterHandler::assignPiecesToCharacter)
                .POST("/characters/{id}/pieces-with-powers", characterHandler::assignPiecesWithPowers)
                .GET("/mongo/test", characterHandler::testMongoConnection)
                .GET("/mongo/free-characters", characterHandler::getFreeCharacters)

                .build();
    }
}

