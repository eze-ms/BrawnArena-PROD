package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.router;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler.AuthHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class AuthRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> authRoutes(AuthHandler handler) {
        return route()
                .POST("/auth/register", handler::registerUser)
                .POST("/auth/login", handler::loginUser)
                .GET("/auth/validate", handler::validateToken)
                .build();
    }
}

