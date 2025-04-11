package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserService {
    Mono<User> findById(Long id);
    Mono<User> findByNickname(String nickname);
    Mono<User> save(User user);
    Mono<User> updateTokens(String nickname, int newTokens);
    Mono<User> addCharacterId(String nickname, Long characterId);
    Mono<List<Long>> getCharacterIds(String nickname);
}