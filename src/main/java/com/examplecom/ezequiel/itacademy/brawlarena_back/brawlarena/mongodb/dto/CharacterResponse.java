package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;

import java.util.List;

public record CharacterResponse(
        String id,
        String name,
        String description,
        String difficulty,
        String imageUrl,
        int cost,
        List<Power> powers,
        List<Piece> pieces,
        boolean unlocked
) {}
