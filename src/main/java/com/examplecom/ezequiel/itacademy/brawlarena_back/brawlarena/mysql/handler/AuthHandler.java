package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.dto.LoginRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service.UserService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class AuthHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthHandler(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<ServerResponse> registerUser(ServerRequest request) {
        return request.bodyToMono(User.class)
                .doOnNext(user -> logger.info("Registrando nuevo usuario: {}", user)) // Log antes de guardar
                .flatMap(userService::save) // Delegamos al servicio para guardar el usuario
                .doOnNext(savedUser -> logger.info("Usuario registrado exitosamente: {}", savedUser)) // Log de éxito
                .flatMap(savedUser -> ServerResponse.status(HttpStatus.CREATED).bodyValue(savedUser))
                .doOnError(e -> logger.error("Error al registrar el usuario: {}", e.getMessage())) // Log de error
                .onErrorResume(e -> ServerResponse.status(HttpStatus.CONFLICT)
                        .bodyValue("El nickname ya está en uso."));
    }

    public Mono<ServerResponse> loginUser(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .doOnNext(login -> logger.info("Intento de login: {}", login.nickname()))
                .flatMap(login -> userService.findByNickname(login.nickname())
                        .filter(user -> {
                            boolean matches = passwordEncoder.matches(login.password(), user.getPassword());
                            if (!matches) {
                                logger.warn("Contraseña incorrecta para usuario: {}", login.nickname());
                            }
                            return matches;
                        })
                        .flatMap(user -> {
                            logger.info("Login exitoso: {}", user.getNickname());
                            return ServerResponse.ok()
                                    .bodyValue(jwtService.generateToken(user.getNickname(), user.getRole()));
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            logger.warn("Usuario no encontrado: {}", login.nickname());
                            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                        }))
                        .onErrorResume(e -> {
                            logger.error("Error durante login: {}", e.getMessage());
                            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        })
                );
    }

    public Mono<ServerResponse> validateToken(ServerRequest request) {
        return Mono.justOrEmpty(request.headers().firstHeader("Authorization"))
                .doOnNext(header -> logger.info("Validando token recibido"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .doOnNext(token -> logger.debug("Token extraído para validación"))
                .flatMap(token -> {
                    if (!jwtService.validateToken(token)) {
                        logger.warn("Token inválido");
                        return ServerResponse.ok()
                                .bodyValue(Map.of("valid", false));
                    }
                    Claims claims = jwtService.getClaims(token);
                    logger.info("Token válido para usuario: {}", claims.getSubject());
                    return ServerResponse.ok()
                            .bodyValue(Map.of(
                                    "valid", true,
                                    "nickname", claims.getSubject(),
                                    "role", claims.get("role", String.class)
                            ));
                })
                .doOnError(e -> logger.error("Error validando token: {}", e.getMessage()))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Intento de validación sin token Bearer");
                    return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                }));
    }
}