package com.ktb.answer.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum TurnType {
    MAIN("main"),
    NEW_TOPIC("new_topic"),
    FOLLOW_UP("follow_up"),
    SESSION_END("session_end");

    private final String wireValue;

    TurnType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    public static TurnType fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.wireValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported turn type: " + value));
    }
}
