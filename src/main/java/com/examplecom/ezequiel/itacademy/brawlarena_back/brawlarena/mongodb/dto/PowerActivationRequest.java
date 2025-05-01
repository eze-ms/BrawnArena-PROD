package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PowerActivationRequest {

    @NotBlank(message = "El characterId no puede estar vacío")
    private String characterId;
}