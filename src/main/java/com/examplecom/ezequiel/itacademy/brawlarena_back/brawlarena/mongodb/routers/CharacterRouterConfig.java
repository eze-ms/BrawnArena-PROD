package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.CharacterHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@Configuration
@Tag(name = "Characters", description = "Endpoints para gestionar los personajes")
public class CharacterRouterConfig {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/characters/all",
                    method = RequestMethod.GET,
                    beanClass = CharacterHandler.class,
                    beanMethod = "getAllCharacters"
            ),

            @RouterOperation(
                    path = "/characters/unlocked",
                    method = RequestMethod.GET,
                    beanClass = CharacterHandler.class,
                    beanMethod = "getCharacterId"
            ),

            @RouterOperation(
                    path = "/characters/unlock",
                    method = RequestMethod.POST,
                    beanClass = CharacterHandler.class,
                    beanMethod = "unlockCharacter"
            ),

            @RouterOperation(
                    path = "/characters/{id}",
                    method = RequestMethod.GET,
                    beanClass = CharacterHandler.class,
                    beanMethod = "getCharacterDetail"
            ),
            @RouterOperation(
                    path = "/characters/{id}",
                    method = RequestMethod.PUT,
                    beanClass = CharacterHandler.class,
                    beanMethod = "updateCharacter"
            ),
            @RouterOperation(
                    path = "/characters/{id}/pieces",
                    method = RequestMethod.POST,
                    beanClass = CharacterHandler.class,
                    beanMethod = "assignPiecesToCharacter"
            ),
            @RouterOperation(
                    path = "/characters/{id}/pieces-with-powers",
                    method = RequestMethod.POST,
                    beanClass = CharacterHandler.class,
                    beanMethod = "assignPiecesWithPowers"
            ),

    })

    public RouterFunction<ServerResponse> characterRoutes(CharacterHandler characterHandler) {
        return route()
                .GET("/characters/all", characterHandler::getAllCharacters)
                .GET("/characters/unlocked", characterHandler::getCharacterId)
                .POST("/characters/unlock", characterHandler::unlockCharacter)
                .GET("/characters/{id}", characterHandler::getCharacterDetail)
                .PUT("/characters/{id}", characterHandler::updateCharacter)
                .POST("/characters/{id}/pieces", characterHandler::assignPiecesToCharacter)
                .POST("/characters/{id}/pieces-with-powers", characterHandler::assignPiecesWithPowers)
                .build();
    }
}

