package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.logic.ScoreCalculator;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterAccessDeniedException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.CharacterNotFoundException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.NoPendingBuildException;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.BuildRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BuildServiceImpl implements BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildServiceImpl.class);


    private final BuildRepository buildRepository;
    private final CharacterRepository characterRepository;
    private final Map<String, List<Piece>> piezasCache = new ConcurrentHashMap<>();
    private final ScoreCalculator scoreCalculator;

    public BuildServiceImpl(BuildRepository buildRepository, CharacterRepository characterRepository, ScoreCalculator scoreCalculator) {
        this.buildRepository = buildRepository;
        this.characterRepository = characterRepository;
        this.scoreCalculator = scoreCalculator;

    }

    @Override
    public Mono<Build> startBuild(String playerId, String characterId) {
        return characterRepository.findById(characterId)
                .doOnSubscribe(sub -> logger.info("Buscando personaje {} para jugador {}", characterId, playerId))
                .switchIfEmpty(Mono.error(new CharacterNotFoundException("Personaje no encontrado")))
                .flatMap(character -> {
                    if (!character.isUnlocked() || !playerId.equals(character.getPlayerId())) {
                        logger.warn("Acceso denegado: personaje {} no pertenece a jugador {}", characterId, playerId);
                        return Mono.error(new CharacterAccessDeniedException("No puedes iniciar un build con este personaje"));
                    }

                    return buildRepository.findAll()
                            .filter(build -> build.getPlayerId().equals(playerId)
                                    && build.getCharacterId().equals(characterId)
                                    && !build.isValid())
                            .hasElements()
                            .flatMap(exists -> {
                                if (exists) {
                                    logger.warn("Ya existe un build no validado para jugador {} y personaje {}", playerId, characterId);
                                    return Mono.error(new IllegalStateException("Ya tienes un build activo para este personaje"));
                                }

                                Build newBuild = new Build();
                                newBuild.setPlayerId(playerId);
                                newBuild.setCharacterId(characterId);
                                newBuild.setValid(false);
                                newBuild.setCreatedAt(Instant.now());

                                return buildRepository.save(newBuild)
                                        .doOnSuccess(saved -> logger.info("Nuevo build iniciado: {}", saved.getId()));
                            });
                })
                .doOnError(error -> logger.error("Error en startBuild: {}", error.getMessage()));
    }

    @Override
    @Transactional
    public Mono<Build> validateBuild(String playerId, Build buildData) {
        String characterId = buildData.getCharacterId();
        List<String> piezasColocadasIds = buildData.getPiecesPlaced();
        long duration = buildData.getDuration();

        return characterRepository.findById(characterId)
                .doOnSubscribe(sub -> logger.info("Validando build para jugador {} y personaje {}", playerId, characterId))
                .switchIfEmpty(Mono.error(new CharacterNotFoundException("Personaje no encontrado")))
                .flatMap(character -> {
                    if (!character.isUnlocked() || !playerId.equals(character.getPlayerId())) {
                        return Mono.error(new CharacterAccessDeniedException("No puedes validar un build de un personaje que no te pertenece"));
                    }

                    List<Piece> piezasCorrectas = piezasCache.computeIfAbsent(characterId, id -> {
                        List<Piece> piezas = character.getPieces();
                        if (piezas == null) {
                            logger.warn("El personaje {} no tiene piezas asignadas", characterId);
                            return List.of(); // evita NPE
                        }
                        return piezas;
                    });

                    return buildRepository.findAll()
                            .filter(build -> build.getPlayerId().equals(playerId)
                                    && build.getCharacterId().equals(characterId)
                                    && !build.isValid())
                            .next()
                            .switchIfEmpty(Mono.error(new NoPendingBuildException("No hay un build pendiente para este personaje")))
                            .flatMap(buildExistente -> {

                                List<Piece> piezasColocadas = piezasCorrectas.stream()
                                        .filter(p -> piezasColocadasIds.contains(p.getId()))
                                        .toList();

                                long errores = piezasColocadasIds.stream()
                                        .filter(id -> piezasCorrectas.stream().noneMatch(p -> p.getId().equals(id)))
                                        .count();

                                return buildRepository.countByPlayerIdAndCharacterIdAndValidTrue(playerId, characterId)
                                        .map(count -> count == 0)
                                        .flatMap(primeraVezCompletado -> {
                                            int score = scoreCalculator.calculateScore(
                                                    piezasColocadas,
                                                    piezasCorrectas,
                                                    (int) errores,
                                                    duration,
                                                    primeraVezCompletado
                                            );

                                            buildExistente.setValid(true);
                                            buildExistente.setScore(score);
                                            buildExistente.setDuration(duration);
                                            buildExistente.setErrors((int) errores);
                                            buildExistente.setPiecesPlaced(piezasColocadasIds);

                                            return buildRepository.save(buildExistente)
                                                    .doOnSuccess(saved -> logger.info(
                                                            "Build validado: {} | Score: {} | Duration: {}s | Errores: {}",
                                                            saved.getId(), score, duration, errores
                                                    ));
                                        });
                            });
                })
                .doOnError(error -> logger.error("Error durante la validación de build: {}", error.getMessage()));
    }

    @Override
    public Flux<Build> getBuildHistory(String playerId) {
        return buildRepository.findByPlayerIdAndValidTrueOrderByCreatedAtDesc(playerId)
                .doOnSubscribe(sub -> logger.info("Recuperando historial de builds para jugador {}", playerId))
                .doOnError(error -> logger.error("Error al recuperar historial de builds: {}", error.getMessage()));
    }

    @Override
    public void clearPiecesCache(String characterId) {
        piezasCache.remove(characterId);
        logger.info("Caché de piezas eliminada para personaje {}", characterId);
    }


}
