package org.example.engine;

import org.example.domain.Yeast;

/**
 * 발효 물리학 (Fermentation Physics)
 * 경험적 보정이 아닌, 화학 반응 속도론과 열역학 법칙에 기반한 계산을 수행합니다.
 */
public class FermentationPhysics {

    // 반응 속도 상수 k (효모의 기본적인 당 분해 능력)
    // 실험적으로 에일/라거에 따라 약 0.003 ~ 0.005 범위를 가짐
    private static final double BASE_KINETIC_CONSTANT = 0.0045;

    // 생물학적 온도 계수 Q10 (온도가 10도 변할 때 대사량 변화율)
    // 효모는 통상 2.0 (10도 오르면 속도 2배)
    private static final double Q10_COEFFICIENT = 2.0;

    // 기준 온도 (이 온도에서 속도 계수가 1.0임)
    private static final double REFERENCE_TEMP = 20.0;

    /**
     * 시간당 비중 감소량(Delta Gravity) 계산
     * 근거: 1차 반응 속도론(First-Order Kinetics) + 아레니우스 온도 법칙
     *
     * @param currentGravity 현재 비중
     * @param targetFG 목표 최종 비중 (발효 한계점)
     * @param temp 현재 온도
     * @param yeast 효모 정보 (적정 온도 범위 확인용)
     * @return 시간당 감소할 비중 수치 (항상 양수)
     */
    public double calculateGravityDrop(double currentGravity, double targetFG, double temp, Yeast yeast) {
        // 1. [물리적 한계] 남은 발효 가능 당분 (Fermentable Extract)
        // 당분이 없으면 반응 속도는 0이다. (음수가 될 수 없음)
        double remainingSugar = currentGravity - targetFG;
        if (remainingSugar <= 0) return 0.0;

        // 2. [열역학] 온도에 따른 활성도 계수 (Arrhenius/Q10 Logic)
        double tempFactor = calculateTemperatureFactor(temp, yeast);

        // 3. [반응 속도론] 최종 감소량 계산
        // dG/dt = k * [Sugar] * [TempFactor]
        double drop = BASE_KINETIC_CONSTANT * remainingSugar * tempFactor;

        // 4. [안전 장치] 한 번에 남은 당보다 많이 먹을 순 없음
        return Math.min(drop, remainingSugar);
    }

    /**
     * 온도 계수 계산 (Q10 법칙 적용)
     * 온도가 낮아져도 0에 수렴할 뿐, 절대 음수가 되지 않음을 보장합니다.
     */
    private double calculateTemperatureFactor(double temp, Yeast yeast) {
        // A. 효모 생존 한계 (Thermal Death / Dormancy)
        // 너무 뜨거우면 효모 사멸 (활성도 0)
        if (temp > yeast.maxTemp() + 15.0) return 0.0;

        // 너무 차가우면 동면 (활성도 0에 근접)
        // 물이 어는점(0도) 근처에서는 대사 정지
        if (temp <= 0.0) return 0.0;

        // B. Q10 공식 적용: Factor = Q10 ^ ((Current - Ref) / 10)
        // 예: 20도 -> 2.0^0 = 1.0 (기준 속도)
        // 예: 10도 -> 2.0^-1 = 0.5 (속도 절반)
        // 예:  0도 -> 2.0^-2 = 0.25 (속도 1/4) -> 절대 음수 안 나옴!
        double factor = Math.pow(Q10_COEFFICIENT, (temp - REFERENCE_TEMP) / 10.0);

        // C. 효모별 적정 온도 페널티 (Strain Specific)
        // 에일 효모를 10도에 두면, Q10 법칙보다 더 급격하게 활동이 둔화됨
        if (temp < yeast.minTemp()) {
            // 적정 온도보다 낮을수록 효율 급감 (Sigmoid Penalty)
            double gap = yeast.minTemp() - temp;
            factor *= Math.max(0, 1.0 - (gap * 0.15)); // 1도 낮을 때마다 15%씩 추가 감소
        }

        return Math.max(0.0, factor);
    }
}