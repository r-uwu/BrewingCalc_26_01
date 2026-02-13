package org.example.service;

import org.example.domain.*;
import org.example.domain.enums.YeastForm;
import org.example.domain.enums.YeastType;
import org.example.engine.BrewCalculator;
import org.example.repository.GrainRepository;
import org.example.repository.HopRepository;
import org.example.repository.YeastRepository;
import org.example.simulation.SimulationLog;
import org.example.simulation.TemperatureSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrewingSimulatorTest {

    private GrainRepository grainRepo;
    private HopRepository hopRepo;
    private YeastRepository yeastRepo;
    private BrewingSimulator simulator;
    private BrewCalculator calculator;

    @BeforeEach
    void setUp() {
        grainRepo = new GrainRepository();
        hopRepo = new HopRepository();
        yeastRepo = new YeastRepository();
        simulator = new BrewingSimulator();

        calculator = new BrewCalculator();
    }

    @Test
    @DisplayName("시나리오 1: 표준 에일 (14일)")
    void testStandardAle() {
        Recipe recipe = createRecipe(20.0, "Pilsner", 5.0, "Citra", 15, "US-05", 11.5);

        TemperatureSchedule schedule = new TemperatureSchedule(18.0); //초기 발효온도
        schedule.addStep(72, 21.0);
        schedule.addStep(168, 22.0);

        System.out.println("\n=== [Scenario 1] Standard Ale (14 Days) ===");
        runSimulationAndPrint(recipe, schedule, 14);
    }

    @Test
    @DisplayName("시나리오 3: 홉 폭탄! West Coast IPA (몰트 2종, 홉 3종)")
    void testComplexIPA() {

        Recipe recipe = new Recipe(20.0, 0.72);

        recipe.addMalt(grainRepo.findByName("Pilsner"), 5.5);
        recipe.addMalt(grainRepo.findByName("Vienna"), 1.0);

        recipe.addHop(hopRepo.findByName("Magnum"), 20, 60);
        recipe.addHop(hopRepo.findByName("Citra"), 20, 15);
        recipe.addHop(hopRepo.findByName("Mosaic"), 30, 5);

        recipe.setYeastItem(new YeastItem(yeastRepo.findByName("US-05"), 11.5, true, 0, 0, false));


        TemperatureSchedule schedule = new TemperatureSchedule(19.0);
        schedule.addStep(168, 21.0);
        schedule.addStep(336, 15.0);

        System.out.println("\n=== 호피한 West Coast IPA (21 Days) ===");
        runSimulationAndPrint(recipe, schedule, 21);
    }

    // --- Helper Methods ---

    /*
    private void runSimulationAndPrint(Recipe recipe, TemperatureSchedule schedule, int durationDays) {
        List<SimulationLog> logs = simulator.simulate(recipe, schedule, durationDays);
        printLogsSmartly(logs);
    }

     */

    private void runSimulationAndPrint(Recipe recipe, TemperatureSchedule schedule, int durationDays) {

        double og = calculator.calculateOG(recipe);
        double ibu = calculator.calculateIBU(recipe);
        double srm = calculator.calculateSRM(recipe);
        List<SimulationLog> logs = simulator.simulate(recipe, schedule, durationDays);

        // 1. [Recipe Overview]
        System.out.println("\n" + "=".repeat(35) + " RECIPE OVERVIEW " + "=".repeat(35));
        System.out.printf("Target Batch Size: %.1f L  |  Expected Efficiency: %.0f%%\n",
                recipe.getBatchSizeLiters(), recipe.getEfficiency() * 100);

        System.out.println("\n[Grains]");
        recipe.getGrainItems().forEach(g -> System.out.printf(" - %-20s : %.2f kg\n", g.grain().name(), g.weightKg()));

        System.out.println("\n[Hops]");
        recipe.getHopItems().forEach(h -> System.out.printf(" - %-20s : %.1f g (%d min)\n",
                h.hop().name(), h.amountGrams(), h.boilTimeMinutes()));

        System.out.println("\n[Yeast]");
        System.out.printf(" - %-20s : %.1f g (%s)\n",
                recipe.getYeastItem().yeast().name(), recipe.getYeastItem().amount(), recipe.getYeastItem().yeast().type());

        System.out.println("\n[Design Targets]");
        System.out.printf(" OG: %.4f  |  IBU: %.1f  |  SRM: %.1f\n", og, ibu, srm);

        // 2. [Brewhouse & Fermentation Logs]
        System.out.println("\n" + "-".repeat(90));
        System.out.println(String.format("%-14s | %-7s | %-7s | %-5s | %-25s | %-25s",
                "Timeline", "Temp", "Gravity", "ABV", "Phase/Event", "Flavor Tags"));
        System.out.println("-".repeat(90));

        for (SimulationLog log : logs) {
            // 출력 필터: 공정(-시간), 시작(0), 종료, 24시간 간격, 상태 변화
            boolean isProcess = log.hour() <= 0;
            boolean isPeriodic = log.hour() > 0 && (log.hour() % 24 == 0 || log.hour() == (durationDays * 24));
            boolean isPhaseChange = isPhaseChanged(logs, log);

            if (isProcess || isPeriodic || isPhaseChange) {
                String timeLabel = (log.hour() < 0) ? "Day - " + Math.abs(log.hour()) + "m" : "Day " + (log.hour()/24 + 1) + " (" + log.hour() + "h)";
                if (log.hour() == 0) timeLabel = "Pitching";

                System.out.println(String.format("%-14s | %5.1f°C | %7.4f | %4.1f%% | %-25s | %-25s",
                        timeLabel, log.temperature(), log.gravity(), log.abv(),
                        log.phase(), shortenTags(String.join(", ", log.flavorTags()))));

                if (log.hour() == 0) System.out.println("-".repeat(90)); // 발효 시작 구분선
            }
        }
    }

    private boolean isPhaseChanged(List<SimulationLog> logs, SimulationLog current) {
        //음수 시간 있음 참고 (매싱, 보일링 구간)
        if (current.hour() <= 0) return true;

        int currentIndex = logs.indexOf(current);
        if (currentIndex == 0) return true;

        String prevPhase = logs.get(currentIndex - 1).phase();
        return !prevPhase.equals(current.phase());
    }

    private String shortenTags(String tags) {
        if (tags.length() > 25) return tags.substring(0, 22) + "...";
        return tags;
    }

    private Recipe createRecipe(double size, String grain, double grainKg, String hop, int hopG, String yeast, double yeastG) {
        Recipe recipe = new Recipe(size, 0.72);
        recipe.addMalt(grainRepo.findByName(grain), grainKg);
        recipe.addHop(hopRepo.findByName(hop), hopG, 60);
        Yeast y = yeastRepo.findByName(yeast);
        recipe.setYeastItem(new YeastItem(y, yeastG, true, 0, 0, false));
        return recipe;
    }
}