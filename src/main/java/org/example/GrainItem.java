package org.example;

/** 몰트 레시티 구성 정보용
 * @param grain 곡물 정보
 * @param weightKg 투입량 (kg)
 */
public record GrainItem(Grain grain, double weightKg) {
}