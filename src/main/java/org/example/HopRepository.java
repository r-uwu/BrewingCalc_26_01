package org.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HopRepository {
    private final Map<String, Hop> hopDb = new HashMap<>();

    public HopRepository() {
        initData();
    }

    private void initData() {

        hopDb.put("Saaz", new Hop("Saaz", 3.5, List.of("Noble, herbal character")));         // 라거 홉
        hopDb.put("Cascade", new Hop("Cascade", 7.0, List.of("Floral", "elements of citrus","notes of grapefruit")));   // 아로마 홉(미국) 자몽
        hopDb.put("Citra", new Hop("Citra", 12.0, List.of("Citrus, grapefruit, lime, tropical fruits, harsh bitterness")));     // 아로마 홉(미국) 시트러스
        hopDb.put("Magnum", new Hop("Magnum", 14.0, List.of("Clean bittering, light citrus flavor")));   // 비터 홉
        hopDb.put("Galaxy", new Hop("Galaxy", 14.5, List.of("Citrus","peach","passionfruit")));   // 호주산

    }

    public Hop findByName(String name) {
        Hop hop = hopDb.get(name);
        if (hop == null) {
            throw new IllegalArgumentException("해당 이름의 홉을 찾을 수 없습니다: " + name);
        }
        return hop;
    }
}
