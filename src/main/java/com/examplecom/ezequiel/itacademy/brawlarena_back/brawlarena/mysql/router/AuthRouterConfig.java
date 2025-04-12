package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.router;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler.AuthHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;


import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@Tag(name = "Braw Arena API", description = "Endpoints para gestionar el acceso a Brawl Arena")
public class AuthRouterConfig {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/auth/login",
                    beanClass = AuthHandler.class,
                    beanMethod = "loginUser",
                    operation = @Operation(
                            summary = "Login de usuario",
                            description = "Autentica un usuario por nickname y contraseña. Devuelve un JWT si las credenciales son correctas."
                    )
            ),
            @RouterOperation(
                    path = "/auth/register",
                    beanClass = AuthHandler.class,
                    beanMethod = "registerUser",
                    operation = @Operation(
                            summary = "Registro de usuario",
                            description = "Registra un nuevo usuario con nickname y contraseña. Devuelve el usuario creado si el registro es exitoso. Falla si el nickname ya existe."

                    )
            ),
            @RouterOperation(
                    path = "/auth/validate",
                    beanClass = AuthHandler.class,
                    beanMethod = "validateToken",
                    operation = @Operation(
                            summary = "Verificación de token",
                            description = "Verifica la validez del token JWT enviado en la cabecera Authorization. Devuelve los datos mínimos del usuario autenticado si el token es válido.",
                            security = @SecurityRequirement(name = "bearerAuth")
                    )
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

