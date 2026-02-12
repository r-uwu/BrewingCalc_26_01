package org.example;

import java.util.*;

public class FlavorTagAnalyzer {

    private static final double INTENSE_THRESHOLD = 5.0; // 5g/L 이상이면 "Intense"
    private static final double SUBTLE_THRESHOLD = 1.0;  // 1g/L 미만이면 "Subtle"

    public List<String> analyze(Recipe recipe, double esterScore, double diacetylRisk) {
        List<String> tags = new ArrayList<>();

        // 1. 홉 분석 (양에 따른 가중치)
        analyzeHops(recipe, tags);

        // 2. 효모 분석 (기존 로직 이관)
        analyzeYeast(esterScore, diacetylRisk, tags);

        return tags.stream().distinct().limit(30).toList();
    }

    private void analyzeHops(Recipe recipe, List<String> tags) {
        double batchSize = recipe.getBatchSizeLiters();

        for (HopItem item : recipe.getHopItems()) {
            double concentration = item.amountGrams() / batchSize; // g/L 계산
            List<String> hopBaseTags = item.hop().flavorTags();

            for (String baseTag : hopBaseTags) {
                if (concentration >= INTENSE_THRESHOLD) {
                    tags.add("Intense " + baseTag);
                    tags.add("Hop Bomb");
                } else if (concentration <= SUBTLE_THRESHOLD) {
                    tags.add("Hint of " + baseTag);
                } else {
                    tags.add(baseTag);
                }
            }
        }

        // 전체 홉 사용량이 매우 많을 경우
        double totalHopConcentration = recipe.getHopItems().stream()
                .mapToDouble(h -> h.amountGrams() / batchSize).sum();
        if (totalHopConcentration > 2.0) tags.add("Extremely Hoppy");
    }

    private void analyzeYeast(double ester, double diacetyl, List<String> tags) {
        if (ester > 60) tags.add("Estery");
        if (diacetyl > 20) tags.add("Buttery");
        // ... 기존 효모 태그 로직들
    }

}
