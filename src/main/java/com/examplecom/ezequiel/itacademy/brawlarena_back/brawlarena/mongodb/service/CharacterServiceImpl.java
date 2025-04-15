package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
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


    @Override
    public Mono<Boolean> unlockCharacter(String playerId, String characterId) {
        return characterRepository.findById(characterId)
                .doOnSubscribe(sub -> logger.info("Intentando desbloquear personaje {} para playerId: {}", characterId, playerId))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Personaje con id {} no encontrado", characterId);
                    return Mono.error(new CharacterNotFoundException("Personaje no encontrado"));
                }))
                .flatMap(character -> {
                    if (character.isUnlocked()) {
                        logger.warn("El personaje {} ya está desbloqueado", character.getName());
                        return Mono.just(false); // Ya estaba desbloqueado
                    }
                    character.setUnlocked(true);
                    character.setPlayerId(playerId);
                    return characterRepository.save(character)
                            .doOnSuccess(saved -> logger.info("Personaje {} desbloqueado para playerId: {}", saved.getName(), playerId))
                            .thenReturn(true);
                })
                .doOnError(error -> logger.error("Error al desbloquear personaje: {}", error.getMessage()));
    }

    //! PENDIENTE
    @Override
    public Mono<Character> getCharacterDetail(String characterId) {
        // Implementación pendiente
        return Mono.empty();
    }
}