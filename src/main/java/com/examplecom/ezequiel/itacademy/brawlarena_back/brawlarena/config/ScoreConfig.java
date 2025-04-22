package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "score")
public class ScoreConfig {
    private int level1Points;
    private int level2Points;
    private int level3Points;
    private int level4Points;
    private int specialBonus;
    private int comboVisualBonus;
    private int errorPenalty;
    private int baseCompletionBonus;
    private int speedBonus;
    private int flawlessBonus;
    private int firstTimeBonus;
}