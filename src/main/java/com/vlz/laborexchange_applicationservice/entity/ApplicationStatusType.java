package com.vlz.laborexchange_applicationservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicationStatusType {
    NEW(1),
    REJECTED(3),
    WITHDRAWN(4);

    private final Integer id;
}