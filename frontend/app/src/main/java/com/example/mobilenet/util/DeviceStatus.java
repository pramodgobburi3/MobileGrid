package com.example.mobilenet.util;

public enum DeviceStatus {
    AVAILABLE("available"),
    UNAVAILABLE("unavailable"),
    ASSIGNED("assigned");

    public final String label;

    private DeviceStatus(String label) {
        this.label = label;
    }
}
