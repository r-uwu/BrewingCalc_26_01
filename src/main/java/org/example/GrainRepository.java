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




        maltDb.put("Pilsner", new Grain("Pilsner", 1.037, 2.0));
        maltDb.put("Vienna", new Grain("Vienna", 1.035, 4.0));
        maltDb.put("Munich", new Grain("Munich", 1.037, 9.0));
        maltDb.put("Roasted Barley", new Grain("Roasted Barley", 1.025, 300));
        maltDb.put("Pale Ale", new Grain("Pale Ale", 1.038, 2.0));
    }

    public Grain findByName(String name) {

        Grain grain = maltDb.get(name);
        if (grain == null) {
            throw new IllegalArgumentException("해당 이름의 몰트를 찾을 수 없습니다: " + name);
        }
        return grain;
    }
}