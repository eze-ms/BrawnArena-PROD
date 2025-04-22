package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.BuildHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class BuildRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> buildRoutes(BuildHandler handler) {
        return route()
                .POST("/builds/start", handler::startBuild)
                .POST("/builds/validate", handler::validateBuild)
                .build();
    }
}

