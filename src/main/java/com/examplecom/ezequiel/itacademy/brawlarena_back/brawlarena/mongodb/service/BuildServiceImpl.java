package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.exception.*;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Character;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.logic.ScoreCalculator;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.BuildRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.repository.CharacterRepository;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.entity.User;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mysql.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.Optional;
import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;

@Service
public class BuildServiceImpl implements BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildServiceImpl.class);


    private final UserRepository userRepository;
    private final BuildRepository buildRepository;
    private final CharacterRepository characterRepository;
    private final Map<String, List<Piece>> piezasCache = new ConcurrentHashMap<>();
    private final ScoreCalculator scoreCalculator;

    public BuildServiceImpl(UserRepository userRepository, BuildRepository buildRepository, CharacterRepository characterRepository, ScoreCalculator scoreCalculator) {
        this.userRepository = userRepository;
        this.buildRepository = buildRepository;
        this.characterRepository = characterRepository;
        this.scoreCalculator = scoreCalculator;
    }

    private static class EvaluacionPiezas {
        private final List<Piece> piezasColocadas;
        private final long errores;

        public EvaluacionPiezas(List<Piece> piezasColocadas, long errores) {
            this.piezasColocadas = piezasColocadas;
            this.errores = errores;
        }

        public List<Piece> getPiezasColocadas() {
            return piezasColocadas;
        }

        public long getErrores() {
            return errores;
        }
    }

    private Mono<Character> verificarAcceso(String playerId, String characterId) {
        Mono<User> userMono = userRepository.findByNickname(playerId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado")));

        Mono<Character> characterMono = characterRepository.findById(characterId)
                .switchIfEmpty(Mono.error(new CharacterNotFoundException("Personaje no encontrado")));

        return Mono.zip(userMono, characterMono)
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Character character = tuple.getT2();

                    List<String> idsDesbloqueados = Optional.ofNullable(user.getCharacterIds())
                            .map(ids -> Arrays.stream(ids.replace("[", "").replace("]", "").split(","))
                                    .map(String::trim)
                                    .map(id -> id.replace("\"", ""))
                                    .filter(s -> !s.isBlank())
                                    .toList())
                            .orElse(List.of());

                    if (!idsDesbloqueados.contains(characterId)) {
                        logger.warn("Acceso denegado: personaje {} no desbloqueado por jugador {}", characterId, playerId);
                        return Mono.error(new CharacterAccessDeniedException("No puedes validar un build de un personaje que no has desbloqueado"));
                    }

                    return Mono.just(character);
                });
    }

    private Mono<Build> obtenerBuildPendiente(String playerId, String characterId) {
        return buildRepository.findByPlayerIdAndCharacterIdAndValidFalse(playerId, characterId)
                .next()
                .doOnNext(build -> logger.debug("Build pendiente encontrado para playerId={}, characterId={}: {}", playerId, characterId, build.getId()))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("No se encontró build pendiente para playerId={} y characterId={}", playerId, characterId);
                    return Mono.error(new NoPendingBuildException("No hay un build pendiente para este personaje"));
                }));
    }

    private EvaluacionPiezas evaluarPiezas(List<String> piezasColocadasIds, List<Piece> piezasCorrectas) {
        List<Piece> piezasColocadas = piezasCorrectas.stream()
                .filter(p -> piezasColocadasIds.contains(p.getId()))
                .toList();

        long errores = piezasColocadasIds.stream()
                .filter(id -> piezasCorrectas.stream().noneMatch(p -> p.getId().equals(id)))
                .count();

        return new EvaluacionPiezas(piezasColocadas, errores);
    }

    private Mono<Build> completarYGuardarBuild(Build build, List<String> piezasColocadasIds, long errores, int duration, boolean primeraVez, List<Piece> piezasColocadas, List<Piece> piezasCorrectas) {
        int score = scoreCalculator.calculateScore(
                piezasColocadas,
                piezasCorrectas,
                (int) errores,
                duration,
                primeraVez
        );

        build.setValid(true);
        build.setScore(score);
        build.setDuration(duration);
        build.setErrors((int) errores);
        build.setPiecesPlaced(piezasColocadasIds);

        return buildRepository.save(build)
                .doOnSuccess(saved -> logger.info(
                        "Build validado: {} | Score: {} | Duration: {}s | Errores: {}",
                        saved.getId(), score, duration, errores
                ));
    }

    @Override
    public Mono<Build> startBuild(String playerId, String characterId) {
        Mono<User> userMono = userRepository.findByNickname(playerId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Usuario no encontrado")));

        Mono<Character> characterMono = characterRepository.findById(characterId)
                .switchIfEmpty(Mono.error(new CharacterNotFoundException("Personaje no encontrado")));

        return Mono.zip(userMono, characterMono)
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Character character = tuple.getT2();

                    List<String> idsDesbloqueados = Optional.ofNullable(user.getCharacterIds())
                            .map(ids -> Arrays.stream(ids.replace("[", "").replace("]", "").split(","))
                                    .map(String::trim)
                                    .map(id -> id.replace("\"", ""))
                                    .filter(s -> !s.isBlank())
                                    .toList())
                            .orElse(List.of());

                    if (!idsDesbloqueados.contains(characterId)) {
                        logger.warn("Acceso denegado: personaje {} no desbloqueado por jugador {}", characterId, playerId);
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
                                    return Mono.error(new BuildAlreadyExistsException("Ya tienes un build activo para este personaje"));
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

        Objects.requireNonNull(playerId, "playerId no puede ser null");
        Objects.requireNonNull(buildData, "buildData no puede ser null");
        Objects.requireNonNull(buildData.getCharacterId(), "characterId no puede ser null");
        Objects.requireNonNull(buildData.getPiecesPlaced(), "piecesPlaced no puede ser null");
        if (buildData.getDuration() <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor que 0");
        }


        String characterId = buildData.getCharacterId();
        List<String> piezasColocadasIds = buildData.getPiecesPlaced();
        long duration = buildData.getDuration();

        return verificarAcceso(playerId, characterId)
                .flatMap(character -> {
                    List<Piece> piezasCorrectas = piezasCache.computeIfAbsent(characterId, id -> {
                        List<Piece> piezas = character.getPieces();
                        if (piezas == null) {
                            logger.warn("El personaje {} no tiene piezas asignadas", characterId);
                            return List.of();
                        }
                        return piezas.stream()
                                .filter(p -> !p.isFake())
                                .toList();
                    });


                    return obtenerBuildPendiente(playerId, characterId)
                            .flatMap(buildExistente -> {
                                EvaluacionPiezas evaluacion = evaluarPiezas(piezasColocadasIds, piezasCorrectas);
                                List<Piece> piezasColocadas = evaluacion.getPiezasColocadas();
                                long errores = evaluacion.getErrores();

                                return buildRepository.countByPlayerIdAndCharacterIdAndValidTrue(playerId, characterId)
                                        .map(count -> count == 0)
                                        .flatMap(primeraVezCompletado ->
                                                completarYGuardarBuild(
                                                        buildExistente,
                                                        piezasColocadasIds,
                                                        errores,
                                                        (int) duration,
                                                        primeraVezCompletado,
                                                        piezasColocadas,
                                                        piezasCorrectas
                                                )
                                        );
                            });
                })
                .doOnError(error -> {
                    logger.error("Error durante la validación de build: {}", error.getMessage());
                    clearPiecesCache(buildData.getCharacterId());
                });

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

    @Override
    public Mono<Build> getPendingBuild(String playerId, String characterId) {
        if (!StringUtils.hasText(playerId) || !StringUtils.hasText(characterId)) {
            throw new IllegalArgumentException("playerId y characterId no pueden ser nulos o vacíos");
        }

        return buildRepository.findByPlayerIdAndCharacterIdAndValidFalse(playerId, characterId)
                .next()
                .switchIfEmpty(Mono.error(new NoPendingBuildException("No hay build pendiente para este personaje")));
    }
}
