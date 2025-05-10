package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.GalleryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class GalleryRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> galleryRoutes(GalleryHandler galleryHandler) {
        return RouterFunctions.route()
                .GET("/gallery", galleryHandler::getPublicGallery)
                .POST("/gallery/share", galleryHandler::shareModel)
                .GET("/gallery/highlighted", galleryHandler::getHighlightedModel)
                .GET("/gallery/character/{characterId}", galleryHandler::getSharedUsersByCharacter)
                .PUT("/gallery/highlighted", galleryHandler::highlightModel)
                .DELETE("/gallery/{sharedModelId}", galleryHandler::deleteSharedModel)
                .build();
    }
}