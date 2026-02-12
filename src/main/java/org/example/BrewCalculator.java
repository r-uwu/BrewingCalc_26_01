package org.example;

import java.util.ArrayList;
import java.util.List;

public class BrewCalculator {


    // 리서치 기반 실제 세포 수 (보수적 수치보다 높음)
    // Fermentis 등 제조사는 >6B/g라고 보증하지만, 실제 계수 시 18B~20B/g가 일반적임
    private static final double DRY_YEAST_REAL_CELLS_PER_GRAM = 18_000_000_000L;
    private static final double LIQUID_YEAST_REAL_CELLS_PER_PACK = 100_000_000_000L;
    // 적정 피칭 레이트 (Target Pitch Rate)
    // 단위: 백만 세포 / mL / Plato
    private static final double TARGET_RATE_ALE = 750_000;
    private static final double TARGET_RATE_LAGER = 1_500_000;
    private static final double TARGET_RATE_HYBRID = 1_000_000;


    public double calculateOG(Recipe recipe) {
        double totalPoints = 0;

        for (GrainItem item : recipe.getGrainItems()) {
            double pointsPerKg = (item.grain().potential() - 1) * 1000;
            // 각 몰트별 (포인트 * 무게 * 전체 효율)을 합산
            totalPoints += (pointsPerKg * item.weightKg() * recipe.getEfficiency());
        }

        return 1 + (totalPoints / (recipe.getBatchSizeLiters() * 1000));
    }

    public double calculateIBU(Recipe recipe) {
        double totalIbu = 0;
        double og = calculateOG(recipe);

        for (var item : recipe.getHopItems()) {

            /*
            // 이용률 계산 (간단한 모델: 60분 끓이면 0.3, 15분이면 0.1 등)
            double utilization = estimateUtilization(item.boilTimeMinutes());

            // IBU 공식: (무게(g) * 알파산(%) * 이용률) / (배치사이즈 * 1.34) [미터법 기준]
            double ibu = (item.amountGrams() * (item.hop().alphaAcid() / 100) * utilization * 1000)
                    / recipe.getBatchSizeLiters();
             */

            double utilization = calculateUtilization(item.boilTimeMinutes(), og);
            double ibu = (item.amountGrams() * item.hop().alphaAcid() * 10 * utilization) / recipe.getBatchSizeLiters();

            totalIbu += ibu;
        }
        return totalIbu;
    }

    public double calculateSRM(Recipe recipe) {
        double mcu = 0;

        // 단위 변환 상수
        final double KG_TO_LBS = 2.20462;
        final double LITER_TO_GALLONS = 0.264172;

        double batchSizeGallons = recipe.getBatchSizeLiters() * LITER_TO_GALLONS;

        for (GrainItem item : recipe.getGrainItems()) {
            double weightLbs = item.weightKg() * KG_TO_LBS;
            // MCU = (무게_lbs * 색도_L) / 부피_gallons
            mcu += (weightLbs * item.grain().lovibond()) / batchSizeGallons;
        }

        // Morey 공식: SRM = 1.4922 * (MCU ^ 0.6859)
        // MCU가 너무 낮으면 공식 특성상 오류가 날 수 있으므로 방어 코드를 넣습니다.
        if (mcu <= 0) return 0;
        return 1.4922 * Math.pow(mcu, 0.6859);
    }

    /**
     * [Step 1] 화학적 한계 계산 (당화 온도)
     * 65도를 기준으로 1도 올라갈 때마다 가용 당분이 약 4%씩 감소한다고 가정 (실제 양조 데이터)
     */
    private double calculateMashEfficiency(double mashTemp) {
        // 표준 당화 온도 65도일 때 효율 100% (1.0)
        // 72도 이상이면 효율 70% 수준으로 급락 (덱스트린 과다)
        double diff = mashTemp - 65.0;

        // 당화 온도가 높을수록(>65) 효모가 먹을 수 있는 당이 줄어듬
        // 예: 65도->1.0, 70도->0.8 (20%는 못 먹는 당이 됨)
        double efficiency = 1.0 - (diff * 0.04);

        return Math.max(0.5, Math.min(1.05, efficiency)); // 62~63도에서는 100% 초과 효율도 가능
    }

    /**
     * [Step 2] 생물학적 수행률 계산 (발효 온도) - 여기가 핵심!
     * 온도가 Min보다 낮으면 수행률이 '지수 함수적'으로 떡락합니다.
     */
    private double calculateYeastPerformance(Yeast yeast, double fermentTemp) {
        double min = yeast.minTemp();
        double max = yeast.maxTemp();

        // Case A: 최적 범위 안 (Best Condition)
        if (fermentTemp >= min && fermentTemp <= max) {
            return 1.0;
        }

        // Case B: 너무 추움 (Cold Crash / Stuck Fermentation)
        // 1도만 낮아져도 수행률이 팍팍 깎여야 함 (사용자 요구사항)
        if (fermentTemp < min) {
            double gap = min - fermentTemp;
            // Sigmoid 형태의 급격한 감소 적용
            // 예: 1도 낮음 -> 90%, 3도 낮음 -> 50%, 5도 낮음 -> 10%
            // 공식: 1 / (1 + e^(gap - tolerance)) 변형
            double penalty = Math.pow(gap, 2.5) * yeast.sensitivityFactor() * 0.5;
            return Math.max(0.1, 1.0 - penalty);
        }

        // Case C: 너무 더움 (Heat Stress)
        // 더우면 발효는 잘 되지만(수행률 1.0 유지), 너무 뜨거우면(35도+) 효모가 죽음
        if (fermentTemp > max) {
            double gap = fermentTemp - max;
            if (gap > 10) return 0.2; // 10도 이상 높으면 사멸
            return 1.0; // 약간 더운 건 발효 자체는 엄청 잘 됨 (맛이 가서 문제지)
        }

        return 1.0;
    }

    /**
     * 1. 효모 생존율(Viability) 계산
     * 액상 효모는 매달 급격히 사멸하며, 건조 효모는 비교적 오래 버팁니다.
     *
     * 효모의 종합 건강 상태(Health Factor) 계산 (0.0 ~ 1.0)
     * 생존율(Viability)과 활력도(Vitality)를 모두 고려함
     */
    private double calculateViability(YeastItem item) {

        double viability = 1.0;
        int age = item.ageInMonths();

        if (item.yeast().form() == YeastForm.DRY) {
            viability = Math.max(0, 1.0 - (0.016 * age)); // 건조: 월 1.6% 감소
        } else {
            viability = Math.max(0, 1.0 - (0.21 * age));  // 액상: 월 21% 감소
        }

        // 1세대 지날 때마다 건강 상태가 5%씩 나빠진다고 가정 (보수적 접근)
        double generationPenalty = item.timesCultured() * 0.05;

        // 재사용 횟수가 너무 많으면(대충 10회 이상) 패널티 증가하는 방식
        double vitalityFactor = Math.max(0, 1.0 - generationPenalty);

        // 최종 건강 점수 = 생존율 * 활력도
        return viability * vitalityFactor;
    }

    /**
     * 2. 실제 살아있는 세포 수(Viable Cells) 계산
     */
    private double calculateViableCells(YeastItem item) {
        if (item == null || item.amount() <= 0) return 0;

        double baseCount = (item.yeast().form() == YeastForm.DRY)
                ? item.amount() * DRY_YEAST_REAL_CELLS_PER_GRAM
                : item.amount() * LIQUID_YEAST_REAL_CELLS_PER_PACK; // 액상은 팩 단위 가정

        // 총 세포 수 * 생존율
        return baseCount * calculateViability(item);
    }

    /**
     * 3. 당화 온도에 따른 가용 발효당 비율 보정 (Mash Factor)
     * 표준 당화 온도(65°C)를 기준으로 ± 보정
     */
    private double calculateMashFactor(double mashTemp) {
        // 65도 미만: 분해 잘됨 (효모가 먹기 좋음) -> 감쇄율 증가
        // 70도 이상: 분해 덜됨 (덱스트린 많음) -> 감쇄율 감소
        // 실험적 계수: 1도당 약 2.5%의 감쇄율 변동 (정밀 모델)
        double diff = 65.0 - mashTemp;
        return diff * 0.025;
    }

    /**
     * 목표 효모 세포 수 계산 (Target Cells)
     * 공식: 배치용량(mL) * 초기비중(Plato) * 목표레이트
     */
    private double calculateTargetCells(Recipe recipe) {
        // 1. 초기 비중(OG)을 플라토(Plato) 단위로 변환
        // 근사식: (OG - 1) * 250 => 예: 1.050 -> 12.5 Plato
        double og = calculateOG(recipe);
        double plato = (og - 1) * 250;

        // 2. 배치 용량을 mL로 변환
        double batchMl = recipe.getBatchSizeLiters() * 1000;

        // 3. 효모 타입에 따른 목표 레이트 설정
        // Yeast 객체에 접근하기 위해 null 체크
        if (recipe.getYeastItem() == null) return 0;

        YeastType type = recipe.getYeastItem().yeast().type();
        double targetRate;

        if (type == YeastType.LAGER) {
            targetRate = TARGET_RATE_LAGER;
        } else if (type == YeastType.ALE || type == YeastType.WHEAT) {
            targetRate = TARGET_RATE_ALE;
        } else {
            targetRate = TARGET_RATE_HYBRID; // 그 외
        }

        // 4. 최종 목표 세포 수 반환
        return batchMl * plato * targetRate;
    }


    public double calculateFG(Recipe recipe, double fermentTemp, double mashTemp) {
        double og = calculateOG(recipe);
        Yeast yeast = recipe.getYeastItem().yeast();
        YeastItem item = recipe.getYeastItem();

        if(item == null || item.amount() <= 0) return og;

        // 온도 민감도에 따른 실제 감쇄율 보정
//        double stress = calculateYeastStress(yeast, fermentTemp);
//        double actualAttenuation = yeast.attenuation() - (stress * yeast.sensitivityFactor());

        double viableCells = calculateViableCells(item);
        double targetCells = calculateTargetCells(recipe); // 기존 메서드 활용
        double pitchRatio = (targetCells > 0) ? (viableCells / targetCells) : 0;

        // 정밀 성장 곡선 계수 (k=4.5가 실제 데이터에 더 근접)
        double pitchingEfficiency = 1.0 - Math.exp(-4.5 * pitchRatio);

        double mashLimit = calculateMashEfficiency(mashTemp);
        double yeastPerformance = calculateYeastPerformance(yeast, fermentTemp);

        // B. 당화 온도에 따른 효모 스펙 보정
        // 효모가 아무리 건강해도, 70도에서 당화하면 먹을 게 없어서 FG가 안 떨어짐
        double mashCorrection = calculateMashFactor(mashTemp);
        double potentialAttenuation = yeast.attenuation() + mashCorrection;

        // C. 온도 스트레스 (기존 유지)
        double tempStress = calculateYeastStress(yeast, fermentTemp) * yeast.sensitivityFactor();

        // D. 알코올 내성(Tolerance) 체크
        // 현재 예상되는 ABV가 효모의 한계치(예: 11%)를 넘으면 강제로 발효 중단
        // (Yeast 레코드에 tolerance 필드가 있다고 가정, 없으면 기본값 10%~12% 적용)
        double estimatedABV = (og - 1) * 131.25 * potentialAttenuation; // 대략적 예상
        double tolerancePenalty = 0;
        double yeastTolerance = 11.0; // US-05 기준 약 11%

        if (estimatedABV > yeastTolerance) {
            // 한계치에 다다르면 효율 급감
            tolerancePenalty = (estimatedABV - yeastTolerance) * 0.5;
        }

        // === 최종 감쇄율 합산 ===
//        double finalAttenuation = (potentialAttenuation * pitchingEfficiency)
//                - tempStress
//                - tolerancePenalty;
        // === 종합 감쇄율 계산 ===
        // 기본 감쇄율 * 당화한계 * (피칭효율 * 온도수행률)
        double attenuation = yeast.attenuation() * mashLimit * (pitchingEfficiency * yeastPerformance);

        // 물리적 한계 설정 (0% ~ 100%)
        attenuation = Math.max(0.0, Math.min(1.0, attenuation));

        // 공식: $FG = 1 + ((OG - 1) * (1 - Attenuation))$
        return 1 + ((og - 1) * (1 - attenuation));
    }


    public double calculateABV(double og, double fg) {
        // 공식: $ABV = (OG - FG) * 131.25$
        return (og - fg) * 131.25;
    }


    private double calculateUtilization(int minutes, double currentOG) {
        if (minutes <= 0) return 0.0;

        // og높으면 이용률 감소
        double bignessFactor = 1.65 * Math.pow(0.000125, currentOG - 1);

        // 보일링 타임에 따라 증가 (Tinseth 상수 0.04 사용)
        double boilTimeFactor = (1 - Math.exp(-0.04 * minutes)) / 4.15;

        return bignessFactor * boilTimeFactor;
    }

    private double calculateYeastStress(Yeast yeast, double temp) {
        if (temp >= yeast.minTemp() && temp <= yeast.maxTemp()) return 0.0;
        return (temp < yeast.minTemp()) ? (yeast.minTemp() - temp) : (temp - yeast.maxTemp());
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