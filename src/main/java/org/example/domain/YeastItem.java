package org.example.domain;

public record YeastItem(
        Yeast yeast,
        double amount,          // 투입량 (Dry면 kg, Liquid면 L)
        boolean amountIsWeight, // true면 kg, false면 L
        int timesCultured, // 재사용 횟수
        int ageInMonths,
        boolean addToSecondary  // 2차 발효 시 투입 여부
) {
}