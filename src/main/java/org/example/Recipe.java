package org.example;

import java.util.ArrayList;
import java.util.List;

public class Recipe {

    private final List<GrainItem> grainItems = new ArrayList<>();
    private final List<HopItem> hopItems = new ArrayList<>();

    private final double batchSizeLiters;
    private final double efficiency;

    public Recipe(double batchSizeLiters, double efficiency) {
        this.batchSizeLiters = batchSizeLiters;
        this.efficiency = efficiency;
    }

    public void addMalt(Grain grain, double weightKg) {
        this.grainItems.add(new GrainItem(grain, weightKg));
    }

    // 레시피에 들어가는 홉의 양과 시간을 기록하는 record
    public record HopItem(Hop hop, double amountGrams, int boilTimeMinutes) {}

    public void addHop(Hop hop, double amountGrams, int boilTimeMinutes) {
        this.hopItems.add(new HopItem(hop, amountGrams, boilTimeMinutes));
    }


    // 게터 메서드들
    public List<GrainItem> getGrainItems() { return grainItems; }
    public List<HopItem> getHopItems() { return hopItems; }
    public double getBatchSizeLiters() { return batchSizeLiters; }
    public double getEfficiency() { return efficiency; }
}