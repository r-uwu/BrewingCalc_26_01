package org.example.service;

import org.example.domain.FlavorProfile;
import org.example.domain.Recipe;
import org.example.domain.YeastItem;
import org.example.engine.BrewCalculator;
import org.example.engine.FlavorAnalyzer;
import org.example.repository.GrainRepository;
import org.example.repository.HopRepository;
import org.example.repository.YeastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrewCalculatorTest {


    private GrainRepository grainRepo;
    private HopRepository hopRepo;
    private YeastRepository yeastRepo;
    private BrewCalculator calculator;
    private FlavorAnalyzer tagAnalyzer;

    /*
    @Test
    @DisplayName("몰트와 홉 저장소를 모두 활용한 레시피 테스트")
    void testFullRecipe() {


        //배치 용량, 수율
        Recipe recipe = new Recipe(20.0, 0.72);

        //일단 이름으로 색인, 몰트 용량
        recipe.addMalt(grainRepo.findByName("Pilsner"), 5.0);
        recipe.addMalt(grainRepo.findByName("Vienna"), 1.5);

        //boilTime은 60이면 0분에 넣은거, 15면 45분에 넣은거라고 보면될듯
        recipe.addHop(hopRepo.findByName("Magnum"), 10.0, 60);
        recipe.addHop(hopRepo.findByName("Cascade"), 30.0, 15);

        //recipe.addYeast(yeastRepo.findByName("US-05"),11);
        recipe.setYeastItem(new YeastItem(yeastRepo.findByName("US-05"), 11.5, true, 0, 5, false));



        double fermentTemp = 20.5;
        double mashTemp = 65.5;

        //======================

        BrewCalculator calculator = new BrewCalculator();

        double og = calculator.calculateOG(recipe);
        double fg = calculator.calculateFG(recipe, fermentTemp, mashTemp);

        double calculateIbu = calculator.calculateIBU(recipe);
        double srm = calculator.calculateSRM(recipe);

        double abv = calculator.calculateABV(og, fg);

        FlavorProfile flavor = calculator.predictFlavorProfile(recipe, fermentTemp);
        List<String> tags = tagAnalyzer.analyze(recipe, flavor.esterScore(), flavor.diacetylRisk());

        System.out.println("====== 종합 레시피 ======");
        recipe.getGrainItems().forEach(m -> System.out.println("몰트: " + m.grain().name() + " " + m.weightKg() + "kg efficiency "+recipe.getEfficiency()));
        recipe.getHopItems().forEach(h -> System.out.println("홉: " + h.hop().name() + " " + h.amountGrams() + "g (" + h.boilTimeMinutes() + "min)"));

        System.out.println("\nog : " + og);
        System.out.println("fg : " + fg);

        System.out.printf("IBUs : %.2f IBUs\n", calculateIbu);
        System.out.printf("SRM : %.1f SRM\n", srm);
        System.out.printf("ABV : %.2f %%\n", abv);

        System.out.println("\n--- Flavor Report ---");
        System.out.printf("Ester Score: %.1f\n", flavor.esterScore());
        System.out.printf("Diacetyl Risk: %.1f\n", flavor.diacetylRisk());

        System.out.println("Flavor of Fermentation : " + flavor.flavorTags());
        System.out.println("Flavor of Hop : " + tags);

        System.out.println("\n--- Precision Result ---");
        System.out.println("Yeast Age : 5 months");
        System.out.println("Mash Temp : " + mashTemp + "°C");

        System.out.println("==========================");

        //테스트는 일단 하지 않는걸로
        //assertEquals(1.00666, actualOg, 0.0001);
    }
    */

    @BeforeEach
    void setUp() {
        grainRepo = new GrainRepository();
        hopRepo = new HopRepository();
        yeastRepo = new YeastRepository();
        calculator = new BrewCalculator();
        tagAnalyzer = new FlavorAnalyzer();
    }

    @Test
    @DisplayName("여러 케이스에서 시뮬레이션")
    void testAllScenarios() {

        runBrewScenario(
                "케이스 1: Standard American Pale Ale",
                20.0,
                List.of(new GrainInput("Pilsner", 4.5), new GrainInput("Vienna", 0.5)),
                List.of(new HopInput("Magnum", 15, 60), new HopInput("Cascade", 20, 15)),
                new YeastInput("US-05", 11.5, 0, 0), // 신선한 효모, 0세대
                65.0, 20.0
        );

        runBrewScenario(
                "케이스 2: Stuck Fermentation (Cold & Old)",
                20.0,
                List.of(new GrainInput("Pilsner", 5.0), new GrainInput("Vienna", 1.0)),
                List.of(new HopInput("Magnum", 10, 60)),
                new YeastInput("US-05", 11.5, 10, 0), // 10개월 된 효모 (사멸 위험)
                65.0, 14.0 // 너무 낮은 발효 온도
        );

        //고온당화 라거
        runBrewScenario(
                "케이스 3: Sweet & Full Body Lager",
                20.0,
                List.of(new GrainInput("Pilsner", 5.5)),
                List.of(new HopInput("Magnum", 20, 60)),
                new YeastInput("W-34/70", 23.0, 1, 0), // 라거는 2봉지(23g) 투입이 정석
                71.0, 12.0 // 높은 당화 온도
        );

        runBrewScenario(
                "케이스 4: 10L 고도수 에일",
                10.0, // 배치 용량 (L)
                List.of(new GrainInput("Pilsner", 3.5), new GrainInput("Vienna", 0.5)),
                List.of(new HopInput("Magnum", 10, 60), new HopInput("Cascade", 20, 15)),
                new YeastInput("US-05", 11.5, 1, 0), // 10L에 11.5g이면 충분한 양
                64.0, 19.0
        );

        runBrewScenario(
                "케이스 5: 20L PILSNER",
                20.0,
                List.of(new GrainInput("Pilsner", 4.5), new GrainInput("Vienna", 1.0)),
                List.of(new HopInput("Magnum", 15, 60)),
                new YeastInput("US-05", 11.5, 12, 0), // 12개월 된 효모 (사멸 위험)
                66.0, 14.0 // 에일 효모에겐 너무 낮은 온도
        );

        runBrewScenario(
                "케이스 6: 50L 라거",
                50.0,
                List.of(new GrainInput("Pilsner", 12.0)),
                List.of(new HopInput("Magnum", 50, 60)),
                new YeastInput("W-34/70", 46.0, 2, 0), // 50L이므로 라거 효모 4봉지 투입
                70.0, 12.0
        );

    }


    private void runBrewScenario(String title, double batchSize, List<GrainInput> grains, List<HopInput> hops,
                                 YeastInput yeastIn, double mashTemp, double fermentTemp) {

        Recipe recipe = new Recipe(batchSize, 0.72);
        grains.forEach(g -> recipe.addMalt(grainRepo.findByName(g.name), g.weight));
        hops.forEach(h -> recipe.addHop(hopRepo.findByName(h.name), h.amount, h.time));

        // YeastItem 생성 (yeast, amount, isWeight, timesCultured, ageInMonths, addToSecondary)
        recipe.setYeastItem(new YeastItem(
                yeastRepo.findByName(yeastIn.name), yeastIn.amount, true, yeastIn.gen, yeastIn.age, false
        ));

        double og = calculator.calculateOG(recipe);
        double fg = calculator.calculateFG(recipe, fermentTemp, mashTemp);
        double abv = calculator.calculateABV(og, fg);
        FlavorProfile flavor = calculator.predictFlavorProfile(recipe, fermentTemp);
        //List<String> tags = tagAnalyzer.analyze(recipe, flavor.esterScore(), flavor.diacetylRisk());
        List<String> tags = flavor.flavorTags();

        System.out.println("\n" + "=".repeat(15) + " " + title + " " + "=".repeat(15));
        System.out.printf("[설정조건]    BatchSize: %.1fL | Mash: %.1f°C | Ferment: %.1f°C | Yeast Age: %d months\n",
                batchSize, mashTemp, fermentTemp, yeastIn.age);
        System.out.printf("[Results]    OG: %.4f | FG: %.4f | ABV: %.2f %%\n", og, fg, abv);
        System.out.printf("[Sensory]    IBU: %.1f | SRM: %.1f | Ester: %.1f\n",
                calculator.calculateIBU(recipe), calculator.calculateSRM(recipe), flavor.esterScore());
        System.out.println("[Tags]       " + tags);
        System.out.println("=".repeat(50 + title.length()));
    }

    // 데이터 전달용 간단한 내부 클래스들
    record GrainInput(String name, double weight) {}
    record HopInput(String name, double amount, int time) {}
    record YeastInput(String name, double amount, int age, int gen) {}

}