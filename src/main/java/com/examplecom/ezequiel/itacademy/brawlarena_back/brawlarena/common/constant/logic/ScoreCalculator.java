package com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.common.constant.logic;

import com.examplecom.ezequiel.itacademy.brawlarena_back.brawlarena.mongodb.entity.Piece;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ScoreCalculator {

    public static int calculateScore(List<Piece> piezasColocadas, List<Piece> piezasCorrectas, int errores, long duration, boolean primeraVezCompletado) {
        if (piezasColocadas == null || piezasCorrectas == null) {
            throw new IllegalArgumentException("Las listas de piezas no pueden ser null");
        }

        int score = 0;

        Set<String> idsCorrectas = new HashSet<>();
        for (Piece p : piezasCorrectas) {
            idsCorrectas.add(p.getId());
        }

        for (Piece colocada : piezasColocadas) {
            if (idsCorrectas.contains(colocada.getId())) {
                score += switch (colocada.getLevel()) {
                    case 1 -> 50;
                    case 2 -> 100;
                    case 3 -> 150;
                    case 4 -> 200;
                    default -> 0;
                };

                if (colocada.isSpecial()) {
                    score += 200;
                }
                if (colocada.isComboVisual()) {
                    score += 100;
                }
            } else {
                score -= 30;
            }
        }

        score -= errores * 30;

        if (duration < 60) {
            score += 150;
        }

        if (errores == 0) {
            score += 100;
        }

        score += 300;

        if (primeraVezCompletado) {
            score += 200;
        }

        return Math.max(score, 0);
    }

}