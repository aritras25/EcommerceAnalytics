package org.example.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum EventType {
    PAGE_VIEW,
    CLICK,
    ADD_TO_CART,
    CHECKOUT,
    SEARCH,
    PURCHASE;

    @JsonCreator
    public static EventType fromString(String value) {
        if (value == null) return null;
        return EventType.valueOf(value.trim().toUpperCase());
    }
}
