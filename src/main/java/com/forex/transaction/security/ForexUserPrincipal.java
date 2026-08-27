package com.forex.transaction.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * Stored as the Authentication principal in the SecurityContext.
 *
 * Controllers retrieve it via:
 *   @AuthenticationPrincipal ForexUserPrincipal principal
 *
 * Contains both userId (Long) and userEmail (String) — extracted directly
 * from the JWT so no database lookup is needed in this service.
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
