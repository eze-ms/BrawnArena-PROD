package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CharacterServiceImpl implements CharacterService {
    private static final Logger logger = LoggerFactory.getLogger(CharacterServiceImpl.class);
    private final CharacterRepository characterRepository;

    public CharacterServiceImpl(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    @Override
    public Flux<Character> getAllFreeCharacters() {
        return characterRepository.findAll()
                .doOnSubscribe(sub -> logger.info("Buscando personajes gratuitos..."))
                .filter(character -> !character.isUnlocked())
                .doOnNext(character -> logger.debug("Personaje gratuito encontrado: {}", character.getName()))
                .switchIfEmpty(Flux.defer(() -> {
                    logger.warn("No se encontraron personajes gratuitos.");
                    return Flux.empty();
                }))
                .doOnError(error -> logger.error("Error al obtener personajes gratuitos: {}", error.getMessage()));
    }


    @Override
    public Flux<Character> getUnlockedCharacters(String playerId) {
        return characterRepository.findAll()
                .doOnSubscribe(sub -> logger.info("Buscando personajes desbloqueados para playerId: {}", playerId))
                .filter(character -> character.isUnlocked() && playerId.equals(character.getPlayerId()))
                .doOnNext(character -> logger.debug("Personaje desbloqueado encontrado: {}", character.getName()))
                .switchIfEmpty(Flux.defer(() -> {
                    logger.warn("No se encontraron personajes desbloqueados para playerId: {}", playerId);
                    return Flux.empty();
                }))
                .doOnError(error -> logger.error("Error al obtener personajes desbloqueados: {}", error.getMessage()));
    }

    
    //! PENDIENTE
    @Override
    public Mono<Boolean> unlockCharacter(String playerId, String characterId) {
        // Implementación pendiente
        return Mono.empty();
    }

    //! PENDIENTE
    @Override
    public Mono<Character> getCharacterDetail(String characterId) {
        // Implementación pendiente
        return Mono.empty();
    }
}