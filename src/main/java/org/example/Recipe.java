package org.example;

import java.util.ArrayList;
import java.util.List;

public class Recipe {

    //그레인
    private final List<GrainItem> items = new ArrayList<>();
    private final double batchSizeLiters;
    private final double efficiency;

    public Recipe(double batchSizeLiters, double efficiency) {
        this.batchSizeLiters = batchSizeLiters;
        this.efficiency = efficiency;
    }

    // 사용자가 몰트를 원하는 만큼 추가하는 메서드
    public void addMalt(Grain grain, double weightKg) {
        this.items.add(new GrainItem(grain, weightKg));
    }
    
    
    
    //홉
    private final List<HopItem> hopItems = new ArrayList<>();

    // 레시피에 들어가는 홉의 양과 시간을 기록하는 record
    public record HopItem(Hop hop, double amountGrams, int boilTimeMinutes) {}

    public void addHop(Hop hop, double amountGrams, int boilTimeMinutes) {
        this.hopItems.add(new HopItem(hop, amountGrams, boilTimeMinutes));
    }

    public List<HopItem> getHopItems() {
        return hopItems;
    }



    // 게터 메서드들
    public List<GrainItem> getItems() { return items; }
    public double getBatchSizeLiters() { return batchSizeLiters; }
    public double getEfficiency() { return efficiency; }
}