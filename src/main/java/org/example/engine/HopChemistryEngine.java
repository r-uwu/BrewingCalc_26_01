package org.example.engine;

/**
 * 드라이 호핑과 발효 후반부의 미세 화학 반응을 처리하는 전담 엔진
 */
public class HopChemistryEngine {

    /*
    드홉 과정에서의 ibu 상승 고려
    홉 크립으로 덱스트린이 발효당이 되게(비중 낮아지고 abv 상승)
    디아세틸 증가하는데, 온도가 낮을 시에는 맥주에 잔여하게

    화학적으로 얼마나 가깝게 만들 수 있을까
     */

    // 디아세틸 청소 기본 속도 (20도 기준, 시간당 감소 비율)
    private static final double DIACETYL_REDUCTION_RATE = 0.015;
    private static final double DIACETYL_Q10 = 2.5; // 온도 민감도

    /**
     * 드라이 호핑 투입 순간의 IBU 변화량 계산 (용해도 모델)
     */
    public double calculateHumulinoneIBU(double gramsPerLiter, double currentABV) {
        // 알코올이 용매 역할을 하므로, ABV가 높을수록 추출 한계치가 높아짐
        double solubilityLimit = 5.0 + (currentABV * 0.5);

        double extractionFactor = 1.0 - Math.exp(-0.2 * gramsPerLiter);

        return solubilityLimit * extractionFactor;
    }

    /**
     * 홉 효소 작용(Hop Creep)으로 인한 목표 비중(FG) 추가 하락폭 계산
     */
    public double calculateHopCreepDrop(double gramsPerLiter, double currentFG) {
        // 현재 FG가 높을수록 분해할 덱스트린이 많다고 가정 (1.000을 기준으로 차이 계산)
        double availableDextrins = currentFG - 1.000;
        if (availableDextrins <= 0) return 0.0;

        // 홉 농도에 비례하되, 남아있는 당의 10% 이상은 분해하지 못함
        double maxDrop = availableDextrins * 0.10;
        double enzymeActivity = Math.min(gramsPerLiter * 0.00005, maxDrop);

        return enzymeActivity;
    }

    /**
     * 1차 반응 속도론 기반 디아세틸 계산
     * @return 시간당 감소해야 할 디아세틸 수치
     */
    public double calculateDiacetylReduction(double currentDiacetyl, double temp) {
        if (currentDiacetyl <= 0) return 0.0;

        if (temp < 10.0) return 0.0;

        double tempFactor = Math.pow(DIACETYL_Q10, (temp - 20.0) / 10.0);

        // 1차 반응: 농도가 높을수록 빨리 줄어들고, 낮아지면 천천히 줄어듦
        double reduction = currentDiacetyl * DIACETYL_REDUCTION_RATE * tempFactor;

        return Math.min(reduction, currentDiacetyl);

    }
}
