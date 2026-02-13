package org.example.domain;

import java.util.ArrayList;
import java.util.List;

public class Recipe {

    private final List<GrainItem> grainItems = new ArrayList<>();
    private final List<HopItem> hopItems = new ArrayList<>();

    private final double batchSizeLiters;
    private final double efficiency;

    private YeastItem yeastItem;

    public Recipe(double batchSizeLiters, double efficiency) {
        this.batchSizeLiters = batchSizeLiters;
        this.efficiency = efficiency;
    }

    public void addMalt(Grain grain, double weightKg) {
        this.grainItems.add(new GrainItem(grain, weightKg));
    }

    public void addHop(Hop hop, double amountGrams, int boilTimeMinutes) {
        this.hopItems.add(new HopItem(hop, amountGrams, boilTimeMinutes));
    }


    public List<GrainItem> getGrainItems() { return grainItems; }
    public List<HopItem> getHopItems() { return hopItems; }
    public YeastItem getYeastItem() { return yeastItem; }
    public double getBatchSizeLiters() { return batchSizeLiters; }
    public double getEfficiency() { return efficiency; }

//    public void addYeast(Yeast yeast, double amount){
//        this.yeastItem = new YeastItem(yeast, amount, true, 3, 3,false);
//    }

    public void setYeastItem(YeastItem yeastItem) {
        this.yeastItem = yeastItem;
    }
}