package edu.ozu.mapp.utils;

public enum ActionType {
    ACCEPT("ACCEPT"), OFFER("OFFER");

    String value;

    ActionType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
