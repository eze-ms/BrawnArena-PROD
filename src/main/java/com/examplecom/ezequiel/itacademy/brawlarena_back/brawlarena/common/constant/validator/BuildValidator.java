package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.validator;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Build;

public class BuildValidator {

    private BuildValidator() {
    }

    public static void validateBuildData(Build build) {
        if (build == null) {
            throw new IllegalArgumentException("El objeto Build no puede ser nulo");
        }
        if (build.getCharacterId() == null || build.getCharacterId().isBlank()) {
            throw new IllegalArgumentException("El campo characterId es obligatorio");
        }
        if (build.getPiecesPlaced() == null || build.getPiecesPlaced().isEmpty()) {
            throw new IllegalArgumentException("La lista de piezas colocadas no puede estar vacía");
        }
        if (build.getDuration() <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor que cero");
        }
    }
}