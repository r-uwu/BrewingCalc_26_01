package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GrainRepository {
    private final Map<String, Grain> maltDb = new HashMap<>();

    public GrainRepository() {
        // 2. 임시 데이터 로드 (나중에 실제 DB 데이터로 대체될 부분)
        initData();
    }

    private void initData() {
        maltDb.put("Pilsner", new Grain("Pilsner", 1.037));
        maltDb.put("Vienna", new Grain("Vienna", 1.035));
        maltDb.put("Munich", new Grain("Munich", 1.037));
        maltDb.put("Roasted Barley", new Grain("Roasted Barley", 1.025));
        maltDb.put("Pale Ale", new Grain("Pale Ale", 1.038));
    }

    public Grain findByName(String name) {
        // 3. 맵에서 이름을 검색하여 반환. 없으면 예외 발생
        Grain grain = maltDb.get(name);
        if (grain == null) {
            throw new IllegalArgumentException("해당 이름의 몰트를 찾을 수 없습니다: " + name);
        }
        return grain;
    }
}