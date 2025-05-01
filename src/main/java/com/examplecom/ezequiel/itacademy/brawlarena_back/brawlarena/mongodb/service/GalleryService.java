package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.service;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.SharedModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GalleryService {
    Mono<SharedModel> shareModel(String playerId, String characterId);
    Flux<SharedModel> getPublicGallery();
    Mono<SharedModel> getHighlightedModel();
    Flux<String> getSharedUsersByCharacter(String characterId);
    Mono<SharedModel> highlightModel(String sharedModelId);
    Mono<Void> deleteSharedModel(String sharedModelId, String requesterId, String role);
}
