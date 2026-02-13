package org.example.engine;

import org.example.domain.FlavorProfile;
import org.example.domain.Recipe;


public class BrewCalculator {


    private final DensityEngine densityEngine = new DensityEngine();
    private final SensoryEngine sensoryEngine = new SensoryEngine();
    private final FermentationEngine fermentationEngine = new FermentationEngine();
    private final FlavorAnalyzer flavorAnalyzer = new FlavorAnalyzer();

    public double calculateOG(Recipe recipe) {
        return densityEngine.calculateOG(recipe);
    }

    public double calculateFG(Recipe recipe, double fermentTemp, double mashTemp) {
        // FG 계산에는 OG가 필요하므로 비중 엔진에서 먼저 계산 후 전달
        double og = calculateOG(recipe);
        return fermentationEngine.calculateFG(recipe, og, fermentTemp, mashTemp);
    }

    public double calculateABV(double og, double fg) {
        return fermentationEngine.calculateABV(og, fg);
    }

    public double calculateIBU(Recipe recipe) {
        double og = calculateOG(recipe);
        return sensoryEngine.calculateIBU(recipe, og);
    }

    public double calculateSRM(Recipe recipe) {
        return sensoryEngine.calculateSRM(recipe);
    }

    public FlavorProfile predictFlavorProfile(Recipe recipe, double fermentTemp) {

        double og = densityEngine.calculateOG(recipe);
        double ibu = sensoryEngine.calculateIBU(recipe, og);

        return flavorAnalyzer.analyze(recipe, og, ibu, fermentTemp);
    }

}