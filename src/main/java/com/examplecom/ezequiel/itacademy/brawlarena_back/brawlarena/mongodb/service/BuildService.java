package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BuildService {

    Mono<Build> startBuild(String playerId, String characterId);
    Mono<Build> validateBuild(String playerId, Build buildData);
    Flux<Build> getBuildHistory(String playerId);
    void clearPiecesCache(String characterId);
}
