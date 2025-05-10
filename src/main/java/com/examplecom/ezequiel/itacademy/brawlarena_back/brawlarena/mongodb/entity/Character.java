package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity;


import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.deserializer.PowerDeserializer;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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

    @JsonDeserialize(contentUsing = PowerDeserializer.class)
    private List<Power> powers;
    private String imageUrl;
    private Integer cost;
    private String gameImageUrl;

}

