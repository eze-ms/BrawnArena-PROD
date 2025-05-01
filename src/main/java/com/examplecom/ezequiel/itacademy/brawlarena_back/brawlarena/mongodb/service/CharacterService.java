package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterUpdateRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;

import java.util.List;

public interface CharacterService {

    // Flux para múltiples valores o listas de datos.
    Flux<Character> getAllCharacters();
    Flux<Character> getUnlockedCharacters(String playerId);
    Mono<Boolean> unlockCharacter(String playerId, String characterId);
    Mono<Character> getCharacterDetail(String characterId);
    Mono<Character> updateCharacter(String characterId, CharacterUpdateRequest request);
    Mono<Character> assignPieces(String characterId, List<String> pieceIds);
    Mono<Character> assignPiecesWithPowers(String characterId, List<Piece> pieces);
}
