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

    private double calculateUtilization(int minutes, double currentOG) {
        if (minutes <= 0) return 0.0;

        // Bigness Factor
        // 맥즙이 진할수록 이용률 감소
        double bignessFactor = 1.65 * Math.pow(0.000125, currentOG - 1);

        // Boil Time Factor
        // 보일링 타임에 따라 증가 (Tinseth 상수 0.04 사용)
        double boilTimeFactor = (1 - Math.exp(-0.04 * minutes)) / 4.15;

        return bignessFactor * boilTimeFactor;
    }
}