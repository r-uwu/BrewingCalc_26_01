package org.example.engine;

import org.example.domain.GrainItem;
import org.example.domain.Recipe;

public class SensoryEngine {

    public double calculateIBU(Recipe recipe, double og) {
        double totalIbu = 0;
        //double og = calculateOG(recipe);

        for (var item : recipe.getHopItems()) {

            double utilization = calculateUtilization(item.boilTimeMinutes(), og);
            double ibu = (item.amountGrams() * item.hop().alphaAcid() * 10 * utilization) / recipe.getBatchSizeLiters();

            totalIbu += ibu;
        }
        return totalIbu;
    }

    private double calculateUtilization(int minutes, double currentOG) {

        if (minutes <= 0) return 0.0;
        // og 높으면 이용률 감소
        double bignessFactor = 1.65 * Math.pow(0.000125, currentOG - 1);
        // 보일링 타임에 따라 증가
        double boilTimeFactor = (1 - Math.exp(-0.04 * minutes)) / 4.15;
        return bignessFactor * boilTimeFactor;
    }

    public double calculateSRM(Recipe recipe) {
        double mcu = 0;
        final double KG_TO_LBS = 2.20462;
        final double LITER_TO_GALLONS = 0.264172;

        double batchSizeGallons = recipe.getBatchSizeLiters() * LITER_TO_GALLONS;

        for (GrainItem item : recipe.getGrainItems()) {
            double weightLbs = item.weightKg() * KG_TO_LBS;
            mcu += (weightLbs * item.grain().lovibond()) / batchSizeGallons;
        }

        if (mcu <= 0) return 0;
        return 1.4922 * Math.pow(mcu, 0.6859);

    }


}
