package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GrainRepository {
    private final Map<String, Grain> maltDb = new HashMap<>();

    public GrainRepository() {
        //임시 데이터 로드 (나중에 실제 DB 데이터로 대체될 부분)
        initData();
    }

    private void initData() {

// --- 베이스 몰트 (Base Malts) ---
        // 필스너: 가장 밝은 색상, 높은 효소 활동
        maltDb.put("Pilsner", new Grain("Pilsner", 1.308, 3.0));
        // 페일 에일: 영국/미국 에일의 기본
        maltDb.put("Pale Ale", new Grain("Pale Ale", 1.310, 5.0));
        // 마리스 오터: 고소한 풍미가 강한 고급 에일 몰트
        maltDb.put("Maris Otter", new Grain("Maris Otter", 1.310, 6.0));
        // 비엔나: 붉은 빛과 빵 구운 향
        maltDb.put("Vienna", new Grain("Vienna", 1.300, 8.0));
        // 뮌헨: 묵직한 바디감과 단맛
        maltDb.put("Munich", new Grain("Munich", 1.300, 18.0));

        // --- 밀 및 특수 몰트 (Wheat & Specialty) ---
        // 밀 몰트: 부드러운 거품과 바디감
        maltDb.put("Wheat Malt", new Grain("Wheat Malt", 1.310, 4.0));
        // 비스킷 몰트: 비스킷/크래커 같은 고소함 (당화력 없음)
        maltDb.put("Biscuit", new Grain("Biscuit", 1.295, 50.0));

        // --- 결정화 몰트 (Crystal/Caramel) ---
        // 크리스탈 40: 꿀, 캐러멜 향
        maltDb.put("Crystal 40", new Grain("Crystal 40", 1.285, 80.0));
        // 크리스탈 120: 어두운 과일, 진한 토피 향
        maltDb.put("Crystal 120", new Grain("Crystal 120", 1.275, 240.0));

        // --- 로스팅 몰트 (Roasted Malts) ---
        // 초콜릿 몰트: 초콜릿, 커피 향 (스타우트/포터용)
        maltDb.put("Chocolate", new Grain("Chocolate", 1.240, 900.0));
        // 로스티드 발리: 탄 맛과 짙은 검은색, 스타우트의 핵심
        maltDb.put("Roasted Barley", new Grain("Roasted Barley", 1.210, 1100.0));

    }

    public Grain findByName(String name) {

        Grain grain = maltDb.get(name);
        if (grain == null) {
            throw new IllegalArgumentException("해당 이름의 몰트를 찾을 수 없습니다: " + name);
        }
        return grain;
    }
}