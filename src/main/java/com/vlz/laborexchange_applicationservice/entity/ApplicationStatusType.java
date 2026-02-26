package com.vlz.laborexchange_applicationservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicationStatusType {
    NEW(1),
    REJECTED(2),
    WITHDRAWN(3),
    ACCEPTED(4);

    private final Integer id;
}