package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.routers;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.handlers.GalleryHandler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@Tag(name = "gallery", description = "Endpoints para gestionar los personajes compartidos")
public class GalleryRouterConfig {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/gallery",
                    method = RequestMethod.GET,
                    beanClass = GalleryHandler.class,
                    beanMethod = "getPublicGallery"
            ),
            @RouterOperation(
                    path = "/gallery/share",
                    method = RequestMethod.POST,
                    beanClass = GalleryHandler.class,
                    beanMethod = "shareModel"
            ),
            @RouterOperation(
                    path = "/gallery/highlighted",
                    method = RequestMethod.GET,
                    beanClass = GalleryHandler.class,
                    beanMethod = "getHighlightedModel"
            ),
            @RouterOperation(
                    path = "/gallery/character/{characterId}",
                    method = RequestMethod.GET,
                    beanClass = GalleryHandler.class,
                    beanMethod = "getSharedUsersByCharacter"
            ),
            @RouterOperation(
                    path = "/gallery/highlighted",
                    method = RequestMethod.PUT,
                    beanClass = GalleryHandler.class,
                    beanMethod = "highlightModel"
            ),
            @RouterOperation(
                    path = "/gallery/{sharedModelId}",
                    method = RequestMethod.DELETE,
                    beanClass = GalleryHandler.class,
                    beanMethod = "deleteSharedModel"
            ),

    })
    public RouterFunction<ServerResponse> galleryRoutes(GalleryHandler galleryHandler) {
        return RouterFunctions.route()
                .GET("/gallery", galleryHandler::getPublicGallery)
                .POST("/gallery/share", galleryHandler::shareModel)
                .GET("/gallery/highlighted", galleryHandler::getHighlightedModel)
                .GET("/gallery/character/{characterId}", galleryHandler::getSharedUsersByCharacter)
                .PUT("/gallery/highlighted", galleryHandler::highlightModel)
//                .DELETE("/gallery/{sharedModelId}", galleryHandler::deleteSharedModel)
                .build();
    }
}