package org.example.engine;

import org.example.domain.GrainItem;
import org.example.domain.Recipe;

public class DensityEngine {


    public double calculateOG(Recipe recipe) {
        double totalPoints = 0;

        for (GrainItem item : recipe.getGrainItems()) {
            double pointsPerKg = (item.grain().potential() - 1) * 1000;
            // 각 몰트별 (포인트 * 무게 * 전체 효율)을 합산
            totalPoints += (pointsPerKg * item.weightKg() * recipe.getEfficiency());
        }

        return 1 + (totalPoints / (recipe.getBatchSizeLiters() * 1000));
    }

}
