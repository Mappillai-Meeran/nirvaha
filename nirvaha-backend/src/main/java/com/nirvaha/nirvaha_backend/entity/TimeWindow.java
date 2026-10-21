package com.nirvaha.nirvaha_backend.entity;

public enum TimeWindow {
    H24("24 Hours"),
    D7("7 Days"),
    D30("30 Days"),
    D90("90 Days");

    private final String label;

    TimeWindow(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
