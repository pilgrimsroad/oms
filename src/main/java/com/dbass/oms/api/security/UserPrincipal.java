package com.dbass.oms.api.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPrincipal {
    private final String userId;
    private final String userUrl;
    private final String userType;
}
