package org.example.simulation;

import java.util.List;

public record SimulationLog (
    int hour,
    double temperature,
    double gravity,
    double abv,
    String phase,
    List<String> flavorTags,

    double esterScore,
    double diacetylRisk
){}
