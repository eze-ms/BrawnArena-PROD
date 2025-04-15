package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity;


import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "characters")
public class Character {

    @Id
    private String id;

    private String name;
    private String description;
    private String difficulty;
    private List<Piece> pieces;
    private List<Power> powers;
    private boolean unlocked;
    private String imageUrl;
    private String playerId;
}

