package com.vlz.laborexchange_applicationservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicationStatusType {
    NEW(1),
    REVIEWING(2),
    INTERVIEW(3),
    OFFER(4),
    REJECTED(5),
    WITHDRAWN(6);

    private final Integer id;

    public static ApplicationStatusType fromId(Integer id) {
        for (ApplicationStatusType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown status id: " + id);
    }
}