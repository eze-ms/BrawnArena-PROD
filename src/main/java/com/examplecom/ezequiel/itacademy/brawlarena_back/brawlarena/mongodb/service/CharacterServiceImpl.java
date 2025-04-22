package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.InsufficientTokensException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.UserNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto.CharacterUpdateRequest;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CharacterServiceImpl implements CharacterService {
    private static final Logger logger = LoggerFactory.getLogger(CharacterServiceImpl.class);
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final BuildService buildService;

    @Autowired
    private ObjectMapper objectMapper;


    public CharacterServiceImpl(CharacterRepository characterRepository, UserRepository userRepository, BuildService buildService) {
        this.characterRepository = characterRepository;
        this.userRepository = userRepository;
        this.buildService = buildService;
    }

    @Override
    public Flux<Character> getAllCharacters() {
        return characterRepository.findAll()
                .doOnSubscribe(sub -> logger.info("Buscando todos los personajes..."))
                .doOnNext(character -> logger.debug("Personaje encontrado: {}", character.getName()))
                .switchIfEmpty(Flux.defer(() -> {
                    logger.warn("No se encontraron personajes.");
                    return Flux.empty();
                }))
                .doOnError(error -> logger.error("Error al obtener personajes: {}", error.getMessage()));
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
        Mono<User> userMono = userRepository.findByNickname(playerId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado")));

        Mono<Character> characterMono = characterRepository.findById(characterId)
                .switchIfEmpty(Mono.error(new CharacterNotFoundException("Personaje no encontrado")));

        return Mono.zip(userMono, characterMono)
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Character character = tuple.getT2();

                    if (character.isUnlocked()) {
                        logger.warn("El personaje {} ya está desbloqueado", character.getName());
                        return Mono.just(false);
                    }

                    if (user.getTokens() < character.getCost()) {
                        logger.warn("Usuario {} no tiene tokens suficientes", user.getNickname());
                        return Mono.error(new InsufficientTokensException("No tienes tokens suficientes"));
                    }

                    user.setTokens(user.getTokens() - character.getCost());

                    List<String> idsList = Optional.ofNullable(user.getCharacterIds())
                            .map(ids -> new ArrayList<>(Arrays.asList(ids.replace("[", "").replace("]", "").split(","))))
                            .orElse(new ArrayList<>());

                    if (!idsList.contains(character.getId())) {
                        idsList.add(character.getId());

                        try {
                            String formatted = objectMapper.writeValueAsString(idsList);
                            user.setCharacterIds(formatted);
                        } catch (JsonProcessingException e) {
                            logger.error("Error al serializar characterIds: {}", e.getMessage());
                            return Mono.error(new RuntimeException("Error al guardar los personajes desbloqueados"));
                        }
                    }

                    character.setUnlocked(true);
                    character.setPlayerId(user.getNickname());

                    return Mono.when(
                            userRepository.save(user),
                            characterRepository.save(character)
                    ).thenReturn(true);

                })
                .doOnError(error -> logger.error("Error al desbloquear personaje: {}", error.getMessage()));
    }

    @Override
    public Mono<Character> getCharacterDetail(String characterId) {
        return characterRepository.findById(characterId)
                .doOnSubscribe(sub -> logger.info("Buscando detalles del personaje con ID: {}", characterId))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Personaje con ID {} no encontrado", characterId);
                    return Mono.error(new CharacterNotFoundException("Personaje no encontrado"));
                }))
                .doOnSuccess(character -> logger.info("Detalles del personaje obtenidos: {}", character.getName()))
                .doOnError(error -> logger.error("Error al obtener detalles del personaje: {}", error.getMessage()));
    }

    @Override
    public Mono<Character> updateCharacter(String characterId, CharacterUpdateRequest request) {
        return characterRepository.findById(characterId)
                .doOnSubscribe(sub -> logger.info("Actualizando personaje con ID {}", characterId))
                .switchIfEmpty(Mono.error(new CharacterNotFoundException("Personaje no encontrado")))
                .flatMap(character -> {
                    character.setName(request.getName());
                    character.setDescription(request.getDescription());
                    character.setDifficulty(request.getDifficulty());
                    character.setImageUrl(request.getImageUrl());
                    character.setPowers(request.getPowers());

                    if (request.getPieces() != null) {
                        character.setPieces(request.getPieces());
                        buildService.clearPiecesCache(characterId);
                    }

                    return characterRepository.save(character)
                            .doOnSuccess(updated -> logger.info("Personaje actualizado correctamente: {}", updated.getId()));
                })
                .doOnError(error -> logger.error("Error al actualizar personaje: {}", error.getMessage()));
    }

}