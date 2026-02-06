package org.example;

import java.util.List;

/**몰트 레시티 구성 정보용
* 홉 이름, 홉 용량, 홉 보일링 시간(60이면 0분에 투입, 15면 45분에 투입)
 */
public record HopItem(Hop hop, double amountGrams, int boilTimeMinutes) {

}
