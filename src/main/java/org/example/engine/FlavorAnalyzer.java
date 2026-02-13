package org.example.engine;


import org.example.domain.enums.YeastType;
import org.example.domain.*;

import java.util.*;

public class FlavorAnalyzer {

    private static final double INTENSE_HOP_CONCENTRATION = 8.0; // 8g/L 이상이면 "Hop Bomb"
    private static final double EXTREME_HOP_CONCENTRATION = 15.0; // 15g/L 이상이면 "NEIPA 급"

    private static final double HIGH_ROAST_PERCENTAGE = 0.10; // 전체 곡물 중 10% 이상이 로스팅 몰트
    private static final double HIGH_CRYSTAL_PERCENTAGE = 0.20; // 20% 이상이 카라멜 몰트 (달고 끈적임 위험)

    /**
     * 종합 풍미 및 결함 분석
     * @param recipe 레시피 (재료 정보)
     * @param og 초기 비중 (Gravity Units 계산용)
     * @param ibu 쓴맛 수치 (BU:GU 비율 계산용)
     * @param fermentTemp 발효 온도 (이취 분석용)
     */
    public FlavorProfile analyze(Recipe recipe, double og, double ibu, double fermentTemp) {
        Yeast yeast = recipe.getYeastItem().yeast();
        List<String> tags = new ArrayList<>();

        analyzeBalance(og, ibu, tags);

        analyzeIngredientIntensity(recipe, tags);

        analyzeOffFlavors(recipe, yeast, fermentTemp, tags);

        analyzeYeastCharacter(yeast, fermentTemp, tags);

        //태그용 스코어
        double esterScore = calculateEsterScore(yeast, fermentTemp);
        double diacetylRisk = calculateDiacetylRisk(yeast, fermentTemp);

        return new FlavorProfile(esterScore, diacetylRisk, tags.stream().distinct().toList());
    }

    /**
     * 비터와 비중 밸런스 비교
     * 적용 공식 : IBU / ((OG - 1) * 1000)
     */
    private void analyzeBalance(double og, double ibu, List<String> tags) {
        double gravityUnits = (og - 1.0) * 1000;
        if (gravityUnits <= 0) return;

        double ratio = ibu / gravityUnits;

        // 스타일별 일반적 범위: 0.3(Wheat) ~ 1.2(IPA)
        if (ratio > 1.2) {
            tags.add("Aggressively Bitter");
        } else if (ratio > 0.8) {
            tags.add("Hoppy");
        } else if (ratio >= 0.4 && ratio <= 0.6) {
            // 밸런스 좋음
        } else if (ratio < 0.3) {
            tags.add("Malty Sweetness");
        }
    }


    private void analyzeIngredientIntensity(Recipe recipe, List<String> tags) {
        double batchSize = recipe.getBatchSizeLiters();
        double totalGrainWeight = recipe.getGrainItems().stream().mapToDouble(GrainItem::weightKg).sum();

        // A. 몰트 구성 비율 분석
        double roastWeight = 0;
        double crystalWeight = 0;

        for (GrainItem item : recipe.getGrainItems()) {
            // 이름으로 유추 , 추후에 Grain 객체에 Type enum으로 정확성 높힐 예정
            String name = item.grain().name().toLowerCase();
            if (name.contains("roasted") || name.contains("chocolate") || name.contains("black")) {
                roastWeight += item.weightKg();
            }
            if (name.contains("crystal") || name.contains("caramel")) {
                crystalWeight += item.weightKg();
            }
        }

        if (totalGrainWeight > 0) {
            if ((roastWeight / totalGrainWeight) > HIGH_ROAST_PERCENTAGE) {
                tags.addAll(List.of("Roasty","Coffee"));
            }
            if ((crystalWeight / totalGrainWeight) > HIGH_CRYSTAL_PERCENTAGE) {
                tags.add("Heavy Body");
            }
        }

        double totalHopGrams = recipe.getHopItems().stream().mapToDouble(HopItem::amountGrams).sum();
        double hopConcentration = totalHopGrams / batchSize;

        if (hopConcentration >= EXTREME_HOP_CONCENTRATION) {
            tags.add("Juice Bomb");
        } else if (hopConcentration >= INTENSE_HOP_CONCENTRATION) {
            tags.add("Very Hoppy");
        }
    }

    /**
     * [New] 이취(Off-Flavor) 위험 분석
     * 공정 실수나 재료 조합 오류로 인한 오프플레이버에 대한 예측
     */
    private void analyzeOffFlavors(Recipe recipe, Yeast yeast, double temp, List<String> tags) {
        // 1. 퓨젤 알코올 (Fusel Alcohol) - 꽃향기 같으면서도 아세톤/벤젠 냄새
        // 발효 온도가 효모 권장 온도보다 훨씬 높을 때 발생 / 혹은 질소 과다
        // 초반 발효 유의
        if (temp > yeast.maxTemp() + 4.0) {
            tags.add("⚠\uFE0F Fusel Alcohol");
        }

        // 2. DMS (Dimethyl Sulfide) - 삶은 옥수수/야채 냄새
        // 필스너 몰트를 사용했는데 끓임 시간이 짧을 경우 발생 (SMM -> DMS 휘발 부족)
        // 아니면 양조 과정 중 생성되거나 발효시 박테리아 감염으로도 디세틸 설파이트 발생. 맥락은 위에랑 같음
        // 보리에서 생성되는 이취로 밝은 보리나 덜 가공된 맥아에서 많이 발생
        boolean hasPilsner = recipe.getGrainItems().stream()
                .anyMatch(g -> g.grain().name().toLowerCase().contains("pilsner"));

        // 레시피의 최대 홉 끓임 시간을 전체 보일링 타임으로 추정
        int maxBoilTime = recipe.getHopItems().stream()
                .mapToInt(HopItem::boilTimeMinutes).max().orElse(0);

        if (hasPilsner && maxBoilTime < 90) { // 필스너는 90분 권장
            tags.add("⚠\uFE0F DMS");
            //tags.addAll(List.of("Off-Flavor: Cooked Corn (DMS Risk)","리스트 태그 테스트1"));
        }

        // 3. 아세트알데히드 (Acetaldehyde) - 풋사과, 아세톤 등
        // 발효 온도가 너무 낮아 효모 활동이 조기 종료되거나, 피칭량이 부족할 때 (여기선 온도만 체크)
        if (temp < yeast.minTemp() - 2.0) {
            tags.add("⚠\uFE0F Green Apple (Acetaldehyde)");
        }

        /**
         * 그 외 오프플레이버 (주로 유통 과정이지만 추후를 위해 다룸)
         *
         * 종이(Papery) - 맥주가 산화되면서 발생하는 젖은 박스, 마분지 향과 맛,
         * 맥주를 높은 온도에서 보관하면 맥주에 들어있는
         * 맥아의 지방산 성분이 고온에서 산호, 효소와 반응해 발생하는 이취
         *
         * 신맛 (사워 비어 신맛 말고 시면 안되는 맥주가 신거)
         * 맥주 오염, 특히 드래프트 맥주 장비를 제대로 청소하지 않았을 경우
         *
         * 디아세틸 - 유도일 수 있지만 이취로 분류됨
         * 버터 팝콘 같은 향. 느끼한 맛. 매싱 너무 높은 온도에서 했거나 발효가 제대로 못끝났을때 발생한다
         *
         * 페놀 - 후추향. 이스트취 있는 맥주 아니면 오프 플레이버. 오염 문제
         *
         * 뷰티릭 애시드
         *
         * 일광취 - 황 냄새. 맥주 관리 이슈
         *
         */
    }

    private void analyzeYeastCharacter(Yeast yeast, double temp, List<String> tags) {
        double ester = calculateEsterScore(yeast, temp);

        if (yeast.type() == YeastType.LAGER) {
            if (temp > 18.0) tags.add("Steam Beer Character"); // 캘리포니아 커먼 등
        } else if (yeast.type() == YeastType.WHEAT) {
            if (ester > 50) tags.addAll(List.of("Banana", "Clove"));
        }
    }

    private double calculateEsterScore(Yeast yeast, double temp) {
        //에스테르 생성 거의 없음
        if (temp < yeast.minTemp()) return 5.0;

        double range = yeast.maxTemp() - yeast.minTemp();
        double position = (temp - yeast.minTemp()) / range;

        // 공식: (온도 위치 * 100) * 민감도 가중치 , 온도가 maxTemp를 넘어가면 수치가 100을 초과하게 설계 (과도한 에스테르)
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
