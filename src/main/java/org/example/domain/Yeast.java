package org.example.domain;


import org.example.domain.enums.YeastForm;
import org.example.domain.enums.YeastType;

/**
 * @param name
 * @param attenuation 기본 감쇄율 (0.7 ~ 0.8)
 * @param minTemp 최적 최소 온도
 * @param maxTemp 최적 최대 온도
 * @param sensitivityFactor 온도 민감도 (높을수록 최적 온도를 벗어날 때 발효력이 급격히 저하된다
 */
public record Yeast(String name, double attenuation, YeastType type, YeastForm form, double minTemp, double maxTemp, double sensitivityFactor) {
}

