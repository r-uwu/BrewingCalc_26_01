package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrewCalculatorTest {

    @Test
    @DisplayName("몰트와 홉 저장소를 모두 활용한 레시피 테스트")
    void testFullRecipe() {

        GrainRepository grainRepo = new GrainRepository();
        HopRepository hopRepo = new HopRepository();

        //배치 용량, 수율
        Recipe recipe = new Recipe(20.0, 0.72);

        //일단 이름으로 색인, 몰트 용량
        recipe.addMalt(grainRepo.findByName("Pilsner"), 5.0);
        recipe.addMalt(grainRepo.findByName("Vienna"), 0.5);

        //boilTime은 60이면 0분에 넣은거, 15면 45분에 넣은거라고 보면될듯
        recipe.addHop(hopRepo.findByName("Magnum"), 10.0, 60);
        recipe.addHop(hopRepo.findByName("Cascade"), 30.0, 15);


        //======================


        BrewCalculator calculator = new BrewCalculator();
        double actualOg = calculator.calculateOG(recipe);
        double calculateIbu = calculator.calculateIBU(recipe);
        double srm = calculator.calculateSRM(recipe);

        System.out.println("====== 종합 레시피 ======");
        recipe.getGrainItems().forEach(m -> System.out.println("몰트: " + m.grain().name() + " " + m.weightKg() + "kg efficiency "+recipe.getEfficiency()));
        recipe.getHopItems().forEach(h -> System.out.println("홉: " + h.hop().name() + " " + h.amountGrams() + "g (" + h.boilTimeMinutes() + "min)"));

        System.out.println("\nog : " + actualOg);
        System.out.printf("IBUs : %.2f IBUs\n", calculateIbu);
        System.out.printf("SRM : %.1f SRM\n", srm);
        System.out.println("==========================");


        //테스트는 일단 하지 않는걸로
        //assertEquals(1.00666, actualOg, 0.0001);
    }
}