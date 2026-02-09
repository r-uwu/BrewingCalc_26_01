package org.example;

import java.util.HashMap;
import java.util.Map;

public class YeastRepository {
    private final Map<String, Yeast> yeastDb = new HashMap<>();

    public YeastRepository() {
        //name, attenuation, type, form, minTemp, maxTemp, sensitivityFactor

        yeastDb.put("US-05", new Yeast("SafAle US-05", 0.81, YeastType.ALE, YeastForm.DRY, 18.0, 28.0, 0.05
        ));
        yeastDb.put("S-04", new Yeast("SafAle S-04", 0.75, YeastType.ALE, YeastForm.DRY, 15.0, 20.0, 0.15
        ));
        yeastDb.put("W-34/70", new Yeast("Saflager W-34/70", 0.83, YeastType.LAGER, YeastForm.DRY, 9.0, 15.0, 0.3
        ));
    }

    public Yeast findByName(String name) {
        Yeast yeast = yeastDb.get(name);
        if (yeast == null) throw new IllegalArgumentException("효모를 찾을 수 없습니다: " + name);
        return yeast;
    }
}