package org.example;

import java.util.HashMap;
import java.util.Map;

public class HopRepository {
    private final Map<String, Hop> hopDb = new HashMap<>();

    public HopRepository() {
        initData();
    }

    private void initData() {

        hopDb.put("Saaz", new Hop("Saaz", 3.5));         // 주로 라거용, 낮은 알파산
        hopDb.put("Cascade", new Hop("Cascade", 7.0));   // 미국 에일용, 자몽 향
        hopDb.put("Citra", new Hop("Citra", 12.0));     // 강력한 열대과일 향
        hopDb.put("Magnum", new Hop("Magnum", 14.0));   // 비터링(쓴맛) 전용
        hopDb.put("Galaxy", new Hop("Galaxy", 14.5));   // 호주산, 아주 높은 알파산

    }

    public Hop findByName(String name) {
        Hop hop = hopDb.get(name);
        if (hop == null) {
            throw new IllegalArgumentException("해당 이름의 홉을 찾을 수 없습니다: " + name);
        }
        return hop;
    }
}
