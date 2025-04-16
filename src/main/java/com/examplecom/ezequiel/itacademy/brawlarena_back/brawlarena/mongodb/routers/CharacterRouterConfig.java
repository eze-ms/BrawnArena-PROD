package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.CharacterHandler;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler.UserHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
                    path = "/characters/free",
                    method = RequestMethod.GET,
                    beanClass = CharacterHandler.class,
                    beanMethod = "getCharacterAllId"
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
            )
    })


    public RouterFunction<ServerResponse> characterRoutes(CharacterHandler handler) {
        return route()
                .GET("/characters/free", handler::getCharacterAllId)
                .GET("/characters/unlocked", handler::getCharacterId)
                .POST("/characters/unlock", handler::unlockCharacter)
                .GET("/characters/{id}", handler::getCharacterDetail)
                .build();
    }
}
