package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pieces")
public class Piece {

    @Id
    private String id;
    private String name;
    private String type;
    private String color;
    private String description;
    private String imageUrl;
    private boolean fake;
    private int level;
    private boolean special;
    private boolean comboVisual;
    private Power power;
}
