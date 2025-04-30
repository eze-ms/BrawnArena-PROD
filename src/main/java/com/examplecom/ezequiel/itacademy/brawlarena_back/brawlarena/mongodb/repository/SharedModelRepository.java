package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository;


import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface SharedModelRepository extends ReactiveMongoRepository<SharedModel, String> {
    Flux<SharedModel> findByCharacterId(String characterId);
    Flux<SharedModel> findByPlayerId(String playerId);
    Flux<SharedModel> findByHighlightedTrue();

}

