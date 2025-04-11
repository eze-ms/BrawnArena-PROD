package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<User> findById(Long id);
    Mono<User> findByNickname(String nickname);
    Mono<User> save(User user);
}