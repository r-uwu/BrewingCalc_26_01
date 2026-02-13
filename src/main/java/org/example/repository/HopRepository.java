package org.example.repository;

import org.example.domain.Hop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HopRepository {
    private final Map<String, Hop> hopDb = new HashMap<>();

    public HopRepository() {
        initData();
    }

    private void initData() {

// --- [독일/유럽] 노블 홉 (전통적인 라거 및 벨기에 맥주용) ---
        addHop("Saaz", 3.5, List.of("Noble", "Herbal", "Spicy", "Earthly"));
        addHop("Hallertau Mittelfruh", 4.0, List.of("Noble", "Floral", "Mild Spice", "Sweet"));
        addHop("Tettnanger", 4.5, List.of("Noble", "Herbal", "Spicy", "Tea-like"));
        addHop("Spalter Select", 5.0, List.of("Noble", "Earthy", "Spicy", "Woody"));
        addHop("Northern Brewer", 9.0, List.of("Woody", "Pine", "Minty", "Strong"));

        // --- [미국] 클래식 C-Hops (페일 에일 및 IPA의 전설) ---
        addHop("Cascade", 7.0, List.of("Grapefruit", "Floral", "Citrus", "Pine"));
        addHop("Centennial", 10.0, List.of("Strong Citrus", "Floral", "Lemon", "Clean"));
        addHop("Columbus", 15.0, List.of("Pungent", "Black Pepper", "Earthy", "Dank"));
        addHop("Chinook", 13.0, List.of("Pine", "Grapefruit", "Smoky", "Spicy"));
        addHop("Simcoe", 13.0, List.of("Passionfruit", "Pine", "Berry", "Earthly"));
        addHop("Amarillo", 9.5, List.of("Orange", "Citrus", "Floral", "Melon"));

        // --- [미국] 모던 홉 (강렬한 열대과일 풍미) ---
        addHop("Citra", 12.0, List.of("Citrus", "Mango", "Tropical Fruits", "Lime"));
        addHop("Mosaic", 12.5, List.of("Blueberry", "Pine", "Tropical Fruit", "Herbal"));
        addHop("Sabro", 13.5, List.of("Coconut", "Tropical Fruit", "Creamy", "Cedar"));
        addHop("El Dorado", 15.0, List.of("Candy", "Pear", "Watermelon", "Stone Fruit"));
        addHop("Azacca", 14.0, List.of("Mango", "Papaya", "Orange", "Spicy"));
        addHop("Idaho 7", 13.0, List.of("Apricot", "Orange", "Red Fruit", "Black Tea"));

        // --- [영국] 전통 에일 홉 ---
        addHop("East Kent Goldings", 5.0, List.of("Honey", "Floral", "Sweet Spice", "Earthly"));
        addHop("Fuggles", 4.5, List.of("Mild", "Grassy", "Earthy", "Woody"));
        addHop("Challenger", 7.5, List.of("Cedar", "Green Tea", "Mild Citrus", "Spicy"));
        addHop("Target", 11.0, List.of("Strong", "Sage", "Spicy", "Resinous"));

        // --- [남반구] 호주/뉴질랜드 (독특한 캐릭터) ---
        addHop("Galaxy", 14.5, List.of("Passionfruit", "Peach", "Citrus", "Intense"));
        addHop("Nelson Sauvin", 12.5, List.of("White Wine", "Gooseberry", "Grape", "Crushed Gooseberry"));
        addHop("Motueka", 7.0, List.of("Lemon-Lime", "Mojito", "Tropical Fruit", "Zesty"));
        addHop("Vic Secret", 15.5, List.of("Pine", "Passionfruit", "Pineapple", "Clean"));
        addHop("Rakau", 11.0, List.of("Stone Fruit", "Apricot", "Plum", "Resinous"));

        // --- [비터링 전용] 쓴맛 강화용 ---
        addHop("Magnum", 14.0, List.of("Clean Bitterness", "Neutral", "Mild Flower"));
        addHop("Warrior", 15.5, List.of("Clean", "Resinous", "Mild Citrus"));
        addHop("Summit", 17.0, List.of("Intense Bitterness", "Onion/Garlic", "Citrus"));

        // --- [일본] 개성 강한 특수 홉 ---
        addHop("Sorachi Ace", 13.0, List.of("Lemon", "Dill", "Coriander", "Cymbopogon"));
    }

    private void addHop(String name, double alpha, List<String> tags) {
        hopDb.put(name, new Hop(name, alpha, tags));
    }



    public Hop findByName(String name) {
        Hop hop = hopDb.get(name);
        if (hop == null) {
            throw new IllegalArgumentException("해당 이름의 홉을 찾을 수 없습니다: " + name);
        }
        return hop;
    }
}
