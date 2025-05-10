package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.router;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler.UserHandler;
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
@Tag(name = "Users", description = "Endpoints para gestionar el registro")
public class UserRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserHandler handler) {
        return route()
                .GET("/users/me", handler::getCurrentUser)
                .PUT("/users/me/tokens", handler::updateUserTokens)
                .GET("/users/me/gallery", handler::getUserGallery)
                .POST("/users/me/gallery", handler::addCharacterId)
                .build();
    }
}
