package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PieceAssignmentDTO {

    @NotBlank(message = "El ID de la pieza no puede estar vacío")
    private String pieceId;

    @NotNull(message = "El poder no puede ser nulo")
    private Power power;
}