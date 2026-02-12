package org.example;

import java.util.List;

public record FlavorProfile(
        double esterScore,     // 0.0 ~ 100.0 (높을수록 과일향이 강함)
        double diacetylRisk,   // 0.0 ~ 100.0 (높을수록 디아세틸관련 이취...)
        // 풍미에 관한 태그 추가예정
        List<String>flavorTags
) {}