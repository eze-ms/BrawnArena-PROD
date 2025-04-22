package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.dto;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;
import lombok.Data;

import java.util.List;

@Data
public class CharacterUpdateRequest {
    private String name;
    private String description;
    private String difficulty;
    private String imageUrl;
    private List<Piece> pieces;
    private List<Power> powers;
}