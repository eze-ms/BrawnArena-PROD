package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.router;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler.AuthHandler;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Braw Arena API", description = "Endpoints para gestionar el acceso a Brawl Arena")
public class AuthRouterConfig {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/auth/register",
                    method = RequestMethod.POST,
                    beanClass = AuthHandler.class,
                    beanMethod = "registerUser"
            ),
            @RouterOperation(
                    path = "/auth/login",
                    method = RequestMethod.POST,
                    beanClass = AuthHandler.class,
                    beanMethod = "loginUser"
            ),
            @RouterOperation(
                    path = "/auth/validate",
                    method = RequestMethod.GET,
                    beanClass = AuthHandler.class,
                    beanMethod = "validateToken"
            )
    })

    public RouterFunction<ServerResponse> authRoutes(AuthHandler handler) {
        return route()
                .POST("/auth/register", handler::registerUser)
                .POST("/auth/login", handler::loginUser)
                .GET("/auth/validate", handler::validateToken)
                .build();
    }
}

