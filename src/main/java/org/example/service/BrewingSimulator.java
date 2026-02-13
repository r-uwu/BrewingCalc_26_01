package org.example.service;

import org.example.domain.FlavorProfile;
import org.example.domain.Recipe;
import org.example.domain.Yeast;
import org.example.engine.*;
import org.example.simulation.SimulationLog;
import org.example.simulation.TemperatureSchedule;

import java.util.ArrayList;
import java.util.List;

public class BrewingSimulator {

    // 엔진 로드
    private final BrewCalculator calculator = new BrewCalculator();
    private final FermentationEngine fermentationEngine = new FermentationEngine();
    private final DensityEngine densityEngine = new DensityEngine();

    /**
     * 시뮬레이션 메인 메서드
     */
    public List<SimulationLog> simulate(Recipe recipe, TemperatureSchedule tempSchedule, int durationDays) {
        List<SimulationLog> logs = new ArrayList<>();
        int totalHours = durationDays * 24;

        simulateBrewhouse(recipe, logs);

        // 발효 초기값 설정
        double currentGravity = densityEngine.calculateOG(recipe);
        final double startOG = currentGravity;

        // 목표 지점(TargetFG) 계산
        double optimalTemp = recipe.getYeastItem().yeast().maxTemp();
        double targetFG = fermentationEngine.calculateFG(recipe, startOG, optimalTemp, 65.0);


        FlavorProfile lastProfile = null;
        String phase = "Lag Phase";

        for (int hour = 0; hour <= totalHours; hour++) {
            double currentTemp = tempSchedule.getTempAt(hour);

            // 비중
            double drop = calculateHourlyDrop(hour, currentGravity, targetFG, currentTemp, recipe.getYeastItem().yeast());

            if (drop < 0) drop = 0;

            currentGravity -= drop;

            //if (currentGravity < targetFG) currentGravity = targetFG;

            // 혹시라도 비중이 시작점보다 높아질까봐 엔트로피 보정
            if (currentGravity > startOG) currentGravity = startOG;

            double currentABV = fermentationEngine.calculateABV(startOG, currentGravity);

            phase = determinePhase(hour, currentGravity, startOG, targetFG, currentTemp);

            // flavor 분석
            if (hour == 0 || hour % 24 == 0 || hour == totalHours) {
                lastProfile = calculator.predictFlavorProfile(recipe, currentTemp);
            }

            if (lastProfile != null) {
                logs.add(new SimulationLog(
                        hour, currentTemp, currentGravity, currentABV, phase,
                        lastProfile.flavorTags(), lastProfile.esterScore(), lastProfile.diacetylRisk()
                ));
            }
            
            //비중 낮아져서 컨디셔닝 페이즈로 가면 발효 종료로 간주하는 코드
            //if (phase.contains("Finished") && hour > 240) break;
        }

        return logs;
    }

    private void simulateBrewhouse(Recipe recipe, List<SimulationLog> logs) {
        double og = densityEngine.calculateOG(recipe);

        double mashGravity = 1.0 + (og - 1.0) * 0.82;
        logs.add(new SimulationLog(-120, 65.0, mashGravity, 0.0, "Mashing Start", List.of("Starch Conversion"), 0, 0));
        logs.add(new SimulationLog(-90, 75.0, mashGravity + 0.002, 0.0, "Mash Out", List.of("Enzyme Denature"), 0, 0));


        logs.add(new SimulationLog(-60, 100.0, mashGravity + 0.005, 0.0, "Boil Start", List.of("Sterilization"), 0, 0));

        recipe.getHopItems().stream()
                .sorted((h1, h2) -> Integer.compare(h2.boilTimeMinutes(), h1.boilTimeMinutes()))
                .forEach(hop -> {
                    int logTime = -hop.boilTimeMinutes();
                    logs.add(new SimulationLog(logTime, 100.0, 0, 0,
                            "Hop Addition: " + hop.hop().name(),
                            List.of(hop.amountGrams() + "g added"), 0, 0));
                });

        //월풀
        logs.add(new SimulationLog(0, 20.0, og, 0.0, "Fermenter In", List.of("Oxygenation"), 0, 0));
    }


    private double calculateHourlyDrop(int hour, double currentG, double targetFG, double temp, Yeast yeast) {
        double remainingSugar = currentG - targetFG;
        if (remainingSugar <= 0.0001) return 0.0;

        // 1. 적응기 (Lag Phase): 0~12시간 (실제로는 미세하게 시작됨)
        if (hour < 12) return 0.0002;

        // 2. Q10 온도 활성도 (기존 유지하되 감도 조정)
        double baseTemp = 20.0;
        double q10 = 2.5; // 효모의 온도 민감도를 조금 더 높임
        double tempActivity = Math.pow(q10, (temp - baseTemp) / 10.0);

        // 3. 효모 생존 한계 페널티
        if (temp < yeast.minTemp()) {
            tempActivity *= 0.15; // 권장 온도 미만 시 활동 급감
        } else if (temp > yeast.maxTemp() + 5) {
            tempActivity = 0.0; // 사멸
        }

        // 4. [핵심 수정] 반응 속도 상수 (k) 상향
        // 실제 발효 속도를 반영하기 위해 기존보다 4~5배 상향 조정
        double reactionConstant;
        if (hour < 96) {
            // 왕성한 발효기 (Log Phase)
            reactionConstant = 0.025;
        } else {
            // 안정기 (Stationary Phase)
            reactionConstant = 0.008;
        }

        // dG/dt = k * (G - G_target) * Activity
        double drop = remainingSugar * reactionConstant * tempActivity;

        return Math.min(drop, remainingSugar);
    }

    private String determinePhase(int hour, double currentG, double startOG, double targetFG, double temp) {
        if (hour < 0) return "Brewhouse";
        if (hour < 12) return "Lag Phase";

        // 목표 비중과 현재 비중의 차이가 0.002 미만이면 발효 종료로 간주
        // == > 목표 비중과의 차이보단 발효 온도가 5도 미만이면 발효 종료도 ㄱㅊ을ㄷ스
        if (Math.abs(currentG - targetFG) < 0.001) {
            if (temp < 5.0) return "Cold Crashing / Lagering";
            return "Finished / Conditioning";
        }
        return "Fermenting";
    }
}