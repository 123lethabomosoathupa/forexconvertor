package com.forex.transaction.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * Principal for the stateless JWT API chain (/api/v1/**).
 * Carries userId (Long) and email extracted directly from JWT claims.
 */
@Getter
@AllArgsConstructor
public class ForexUserPrincipal implements Principal {

    private final Long   userId;
    private final String email;

    @Override
    public String getName() {
        return email;
    }
}
