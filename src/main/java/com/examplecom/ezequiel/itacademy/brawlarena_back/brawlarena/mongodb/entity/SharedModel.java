package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "shared_models")
public class SharedModel {

    @Id
    private String id;

    private String playerId;
    private String characterId;

    private List<String> powers;
    private int score;
    private Instant sharedAt;
    private boolean highlighted;

}