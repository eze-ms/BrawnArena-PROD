package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NicknameAlreadyExistsException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();



    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado.")))
                .doOnNext(user -> logger.info("Usuario encontrado: {}", user))
                .doOnError(e -> logger.error("Error al buscar usuario: {}", e.getMessage()));
    }

    @Override
    public Mono<User> findByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado.")))
                .doOnNext(user -> logger.info("Usuario encontrado: {}", user))
                .doOnError(e -> logger.error("Error al buscar usuario: {}", e.getMessage()));
    }

    @Override
    public Mono<User> save(User user) {
        return userRepository.findByNickname(user.getNickname())
                .flatMap(existingUser -> {
                    logger.warn("El nickname {} ya está en uso", user.getNickname());
                    return Mono.<User>error(new NicknameAlreadyExistsException("El nickname ya está en uso"));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    logger.info("Registrando nuevo usuario: {}", user.getNickname());
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    if (user.getCharacterIds() == null) {
                        user.setCharacterIds("[]");
                    }
                    user.setRole("USER");
                    return userRepository.save(user);
                }))
                .doOnNext(savedUser -> logger.info("Usuario guardado: {}", savedUser))
                .doOnError(e -> logger.error("Error al registrar el usuario: {}", e.getMessage()));
    }


    @Override
    public Mono<User> updateTokens(String nickname, int newTokens) {
        return userRepository.findByNickname(nickname)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado.")))
                .flatMap(user -> {
                    logger.info("Actualizando tokens para usuario {}: {} → {}", nickname, user.getTokens(), newTokens);
                    user.setTokens(newTokens);
                    return userRepository.save(user);
                })
                .doOnNext(updatedUser -> logger.info("Tokens actualizados para usuario: {}", updatedUser.getNickname()))
                .doOnError(e -> logger.error("Error al actualizar tokens: {}", e.getMessage()));
    }

    @Override
    @Transactional
    public Mono<User> addCharacterId(String nickname, Long characterId) {
        return userRepository.findByNickname(nickname)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado.")))
                .flatMap(user -> {
                    List<Long> list;
                    try {
                        String json = user.getCharacterIds();
                        list = (json != null)
                                ? objectMapper.readValue(json, new TypeReference<List<Long>>() {})
                                : new ArrayList<>();
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Error al leer la galería del usuario."));
                    }

                    if (!list.contains(characterId)) {
                        list.add(characterId);
                        try {
                            user.setCharacterIds(objectMapper.writeValueAsString(list));
                        } catch (Exception e) {
                            return Mono.error(new RuntimeException("Error al guardar la galería."));
                        }
                        return userRepository.save(user);
                    }
                    return Mono.just(user);
                });
    }

    @Override
    public Mono<List<Long>> getCharacterIds(String nickname) {
        return userRepository.findByNickname(nickname)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado.")))
                .map(user -> {
                    try {
                        String json = user.getCharacterIds();
                        return (json != null)
                                ? objectMapper.readValue(json, new TypeReference<List<Long>>() {})
                                : new ArrayList<>();
                    } catch (Exception e) {
                        throw new RuntimeException("Error al leer la galería.");
                    }
                });
    }


}