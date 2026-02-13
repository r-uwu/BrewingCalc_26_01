package org.example.simulation;

import java.util.TreeMap;

public class TemperatureSchedule {

    private final TreeMap<Integer, Double> schedule = new TreeMap<>();

    public TemperatureSchedule(double initialTemp) {
        schedule.put(0, initialTemp);
    }

    public void addStep(int hour, double temp) {
        schedule.put(hour, temp);
    }

    public double getTempAt(int hour) {
        return schedule.floorEntry(hour).getValue();
    }
}
