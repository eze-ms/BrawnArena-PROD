package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface BuildRepository extends ReactiveMongoRepository<Build, String> {
    Mono<Long> countByPlayerIdAndCharacterIdAndValidTrue(String playerId, String characterId);
    Flux<Build> findByPlayerIdAndValidTrueOrderByCreatedAtDesc(String playerId);


}
