package org.example.domain;

import java.util.List;

/** 홉 원재료
 * @param name 홉 이름
 * @param alphaAcid 알파산 함량 (%)
 * flavorTags 홉 플레이버 태그
 */
public record Hop(String name, double alphaAcid, List<String> flavorTags) {
}