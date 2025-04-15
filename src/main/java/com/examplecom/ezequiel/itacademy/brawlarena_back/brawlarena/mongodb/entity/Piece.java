package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity;

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
    private String type; // Ejemplo: cabeza, torso, arma
    private String color;
    private String description;
}