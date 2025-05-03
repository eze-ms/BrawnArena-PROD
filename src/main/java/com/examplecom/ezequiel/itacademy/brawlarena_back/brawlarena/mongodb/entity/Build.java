package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Transient;
import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.enums.Power;



import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "builds")
public class Build {

    @Id
    private String id;

    private String playerId;
    private String characterId;


    private long duration;

    private List<String> piecesPlaced;
    private int errors;
    private int score;

    private boolean valid;
    private Instant createdAt;

    @Transient
    private Map<Power, Integer> powerProgress;

}
