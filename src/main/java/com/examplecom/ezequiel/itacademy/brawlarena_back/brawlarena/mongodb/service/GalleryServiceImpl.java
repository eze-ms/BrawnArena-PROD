package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.*;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.BuildRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.SharedModelRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class GalleryServiceImpl implements GalleryService{
    private static final Logger logger = LoggerFactory.getLogger(GalleryServiceImpl.class);
    private final SharedModelRepository sharedModelRepository;
    private final BuildRepository buildRepository;
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;

    public GalleryServiceImpl(SharedModelRepository sharedModelRepository, BuildRepository buildRepository, UserRepository userRepository, CharacterRepository characterRepository) {
        this.sharedModelRepository = sharedModelRepository;
        this.buildRepository = buildRepository;
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
    }

    @Override
    public Mono<SharedModel> shareModel(String playerId, String characterId) {
        if (!StringUtils.hasText(playerId) || !StringUtils.hasText(characterId)) {
            return Mono.error(new IllegalArgumentException("playerId y characterId son obligatorios"));
        }

        return Mono.zip(
                        userRepository.findByNickname(playerId)
                                .switchIfEmpty(Mono.error(new UserNotFoundException("Jugador no encontrado"))),
                        characterRepository.findById(characterId)
                                .switchIfEmpty(Mono.error(new CharacterNotFoundException("Personaje no encontrado")))
                )
                .doOnSubscribe(sub -> logger.info("Validando existencia de jugador {} y personaje {}", playerId, characterId))
                .flatMapMany(tuple -> buildRepository.findByPlayerIdAndCharacterIdAndValidTrue(playerId, characterId)
                        .sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                        .limitRequest(1)
                )
                .next()
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("No se encontró build válido para jugador {} y personaje {}", playerId, characterId);
                    return Mono.error(new BuildNotFoundException("No has completado ningún montaje válido para este personaje"));
                }))
                .flatMap(build -> {
                    SharedModel sharedModel = new SharedModel();
                    sharedModel.setPlayerId(playerId);
                    sharedModel.setCharacterId(characterId);
                    sharedModel.setScore(build.getScore());
                    sharedModel.setPowers(List.of()); // A completar si se guardan poderes
                    sharedModel.setSharedAt(Instant.now());

                    return sharedModelRepository.save(sharedModel)
                            .doOnSuccess(saved -> logger.info("Modelo compartido correctamente por jugador {} con personaje {}", playerId, characterId));
                })
                .doOnError(error -> logger.error("Error al compartir modelo para jugador {}: {}", playerId, error.getMessage()));
    }

    @Override
    public Flux<SharedModel> getPublicGallery() {
        return sharedModelRepository.findAll()
                .sort((a, b) -> b.getSharedAt().compareTo(a.getSharedAt()))
                .doOnSubscribe(sub -> logger.info("Recuperando galería pública de modelos compartidos"))
                .doOnNext(model -> logger.debug("Modelo compartido encontrado: playerId={}, characterId={}", model.getPlayerId(), model.getCharacterId()))
                .doOnComplete(() -> logger.info("Galería pública recuperada correctamente"))
                .doOnError(error -> logger.error("Error al recuperar galería pública: {}", error.getMessage()));
    }

    @Override
    public Mono<SharedModel> getHighlightedModel() {
        return sharedModelRepository.findByHighlightedTrue()
                .next()
                .doOnSubscribe(sub -> logger.info("Buscando modelo destacado en la galería"))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("No hay modelo destacado definido");
                    return Mono.error(new HighlightedModelNotFoundException("No hay jugador destacado actualmente"));
                }))
                .doOnSuccess(model -> logger.info("Modelo destacado encontrado: playerId={}, characterId={}", model.getPlayerId(), model.getCharacterId()))
                .doOnError(error -> logger.error("Error al obtener modelo destacado: {}", error.getMessage()));
    }

    @Override
    public Flux<String> getSharedUsersByCharacter(String characterId) {
        if (!StringUtils.hasText(characterId)) {
            return Flux.error(new IllegalArgumentException("characterId no puede estar vacío"));
        }

        return sharedModelRepository.findByCharacterId(characterId)
                .doOnSubscribe(sub -> logger.info("Buscando usuarios que compartieron el personaje {}", characterId))
                .map(SharedModel::getPlayerId)
                .distinct()
                .doOnComplete(() -> logger.info("Usuarios recuperados correctamente para el personaje {}", characterId))
                .doOnError(error -> logger.error("Error al recuperar usuarios del personaje {}: {}", characterId, error.getMessage()));
    }

    @Override
    public Mono<SharedModel> highlightModel(String sharedModelId) {
        if (!StringUtils.hasText(sharedModelId)) {
            return Mono.error(new IllegalArgumentException("sharedModelId no puede estar vacío"));
        }

        return sharedModelRepository.findById(sharedModelId)
                .switchIfEmpty(Mono.error(new ModelNotFoundException("Modelo compartido no encontrado")))
                .flatMap(model -> {
                    Mono<Void> desmarcarTodos = sharedModelRepository.findByHighlightedTrue()
                            .flatMap(existing -> {
                                existing.setHighlighted(false);
                                return sharedModelRepository.save(existing);
                            })
                            .then();

                    model.setHighlighted(true);
                    Mono<SharedModel> guardarNuevo = sharedModelRepository.save(model);

                    return desmarcarTodos.then(guardarNuevo);
                })
                .doOnSuccess(updated -> logger.info("Modelo destacado correctamente: {}", updated.getId()))
                .doOnError(error -> logger.error("Error al destacar modelo: {}", error.getMessage()));
    }

}