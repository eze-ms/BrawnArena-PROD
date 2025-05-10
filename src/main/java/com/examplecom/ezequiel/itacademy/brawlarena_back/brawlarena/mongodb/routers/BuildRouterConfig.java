package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.BuildHandler;
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
@Tag(name = "Builds", description = "Endpoints para gestionar el montaje de los personajes")

public class BuildRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> buildRoutes(BuildHandler handler) {
        return route()
                .POST("/builds/start", handler::startBuild)
                .POST("/builds/validate", handler::validateBuild)
                .GET("/builds/history", handler::getBuildHistory)
                .GET("/builds/pending", handler::getPendingBuild)
                .build();
    }
}
