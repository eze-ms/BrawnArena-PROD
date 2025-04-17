package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;

public interface CharacterService {

    // Flux para múltiples valores o listas de datos.
//    Flux<Character> getAllFreeCharacters();
    Flux<Character> getAllCharacters();
    Flux<Character> getUnlockedCharacters(String playerId);
    Mono<Boolean> unlockCharacter(String playerId, String characterId);
    Mono<Character> getCharacterDetail(String characterId);
}
