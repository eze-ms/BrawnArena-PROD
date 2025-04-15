package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.CharacterHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;


@Configuration
public class CharacterRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> characterRoutes(CharacterHandler handler) {
        return route()
                .GET("/characters/free", handler::getCharacterAllId)
                .GET("/characters/unlocked", handler::getCharacterId)
                .POST("/characters/unlock", handler::unlockCharacter)
                .build();
    }
}

