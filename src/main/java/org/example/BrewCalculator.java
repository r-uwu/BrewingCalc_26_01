package org.example;

public class BrewCalculator {

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

    public double calculateFG(Recipe recipe, double fermentTemp) {
        double og = calculateOG(recipe);
        Yeast yeast = recipe.getYeastItem().yeast();

        // 온도 민감도에 따른 실제 감쇄율 보정
        double stress = calculateYeastStress(yeast, fermentTemp);
        double actualAttenuation = yeast.attenuation() - (stress * yeast.sensitivityFactor());

        // 공식: $FG = 1 + ((OG - 1) * (1 - Attenuation))$
        return 1 + ((og - 1) * (1 - actualAttenuation));
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

        return new FlavorProfile(esterScore, diacetylRisk);
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