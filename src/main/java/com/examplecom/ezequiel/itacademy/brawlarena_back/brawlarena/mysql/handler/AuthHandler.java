package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.handler;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.Role;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.dto.LoginRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service.UserService;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class AuthHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CharacterRepository characterRepository;


    public AuthHandler(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService, CharacterRepository characterRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.characterRepository = characterRepository;
    }

    public Mono<ServerResponse> registerUser(ServerRequest request) {
        return request.bodyToMono(User.class)
                .flatMap(user ->
                        characterRepository.findAll()
                                .retryWhen(reactor.util.retry.Retry.fixedDelay(3, Duration.ofMillis(300)))
                                .filter(character -> character.getCost() == 0)
                                .map(Character::getId)
                                .collectList()
                                .flatMap(freeIds -> {
                                    try {
                                        ObjectMapper objectMapper = new ObjectMapper();
                                        user.setCharacterIds(objectMapper.writeValueAsString(freeIds));
                                    } catch (Exception e) {
                                        return Mono.error(new RuntimeException("Error al preparar el registro del usuario"));
                                    }
                                    return userService.save(user);
                                })
                )
                .flatMap(savedUser -> ServerResponse.status(HttpStatus.CREATED).bodyValue(savedUser))
                .onErrorResume(e -> ServerResponse.status(HttpStatus.CONFLICT)
                        .bodyValue("El nickname ya está en uso."));
    }

    public Mono<ServerResponse> loginUser(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .doOnNext(login -> logger.info("Intento de login: {}", login.nickname()))
                .flatMap(login -> {
                    // Lógica especial para el admin
                    if ("admin".equals(login.nickname()) && "12345678".equals(login.password())) {
                        logger.info("Login exitoso como admin");
                        return ServerResponse.ok()
                                .bodyValue(jwtService.generateToken("admin", Role.ADMIN)); // Asignar rol ADMIN explícitamente
                    }

                    return userService.findByNickname(login.nickname())
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
                                        .bodyValue(jwtService.generateToken(user.getNickname(), Role.valueOf(user.getRole())));
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                logger.warn("Usuario no encontrado: {}", login.nickname());
                                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
                            }));
                })
                .onErrorResume(e -> {
                    logger.error("Error durante login: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
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