package org.example.engine;

import org.example.domain.FlavorProfile;
import org.example.domain.HopItem;
import org.example.domain.Recipe;
import org.example.domain.Yeast;
import org.example.domain.enums.YeastType;

import java.util.*;

public class FlavorAnalyzer {

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


    public FlavorProfile predictFlavorProfile(Recipe recipe, double fermentTemp) {
        Yeast yeast = recipe.getYeastItem().yeast();

        //기전 - 활동 권장 온도 내에서의 위치를 백분율로 계산하여 에스테르 수치 정하게
        double esterScore = calculateEsterScore(yeast, fermentTemp);

        // 디아세틸 생성 / 특히 라거 효모이거나 온도가 너무 낮을 때 위험도가 상승합니다.
        double diacetylRisk = calculateDiacetylRisk(yeast, fermentTemp);


        List<String> tags = new ArrayList<>();

        // 1. 에스테르 관련 태그 (과일 향)
        if (esterScore > 90) {
            tags.addAll(List.of("Strong Esters", "Banana", "Tropical"));
        } else if (esterScore > 60) {
            tags.addAll(List.of("Fruity", "Red Apple", "Pear"));
        } else if (esterScore < 20) {
            tags.add("Clean");
            tags.add("Neutral");
        }

        // 2. 디아세틸 관련 태그 (버터/질감)
        if (diacetylRisk > 70) {
            tags.addAll(List.of("Diacetyl Alert", "Buttery", "Slick Mouthfeel"));
        } else if (diacetylRisk > 40) {
            tags.add("Creamy");
        }

        // 3. 온도 및 스트레스 관련 태그
        if (fermentTemp > yeast.maxTemp() + 3) {
            tags.addAll(List.of("Fusel Alcohols", "Hot/Alcoholic", "Solvent-like"));
        }
        if (fermentTemp < yeast.minTemp()) {
            tags.add("Sulphury");
            tags.add("Stuck Fermentation Risk");
        }

        // 4. 효모 타입별 특성 태그
        if (yeast.type() == YeastType.LAGER && esterScore < 15) {
            tags.addAll(List.of("Crisp", "Authentic Lager", "Refined"));
        }
        if (yeast.type() == YeastType.WHEAT && esterScore > 50) {
            tags.addAll(List.of("Clove-like", "Bubblegum", "Classic Weizen"));
        }

        List<String> finalTags = tags.stream().distinct().toList();

        return new FlavorProfile(esterScore, diacetylRisk, finalTags);
    }

    private double calculateEsterScore(Yeast yeast, double temp) {
        //에스테르 생성 거의 없음
        if (temp < yeast.minTemp()) return 5.0;

        double range = yeast.maxTemp() - yeast.minTemp();
        double position = (temp - yeast.minTemp()) / range;

        // 공식: (온도 위치 * 100) * 민감도 가중치
        // 온도가 maxTemp를 넘어가면 수치가 100을 초과하게 설계 (과도한 에스테르)
        return Math.max(0, position * 100 * (1 + yeast.sensitivityFactor()));
    }

    private double calculateDiacetylRisk(Yeast yeast, double temp) {
        double risk = 0;

        if (yeast.type() == YeastType.LAGER && temp < yeast.minTemp() + 2) {
            risk += 40;
        }

        // 효모 스트레스
        if (temp < yeast.minTemp() || temp > yeast.maxTemp()) {
            double deviation = Math.abs(temp - ((yeast.minTemp() + yeast.maxTemp()) / 2));
            risk += deviation * yeast.sensitivityFactor() * 10;
        }

        return Math.min(100, risk);
    }



}
