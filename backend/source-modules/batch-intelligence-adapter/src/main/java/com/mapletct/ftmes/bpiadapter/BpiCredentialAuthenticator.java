package com.mapletct.ftmes.bpiadapter;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class BpiCredentialAuthenticator {

    private final JwtDecoder jwtDecoder;
    private final LegacyTicketVerifier legacyTicketVerifier;

    public BpiCredentialAuthenticator(JwtDecoder jwtDecoder, LegacyTicketVerifier legacyTicketVerifier) {
        this.jwtDecoder = jwtDecoder;
        this.legacyTicketVerifier = legacyTicketVerifier;
    }

    public Jwt authenticate(String credential) {
        if (credential == null || credential.trim().isEmpty()) {
            throw new BadCredentialsException("BPI credential is missing");
        }
        if (isJwt(credential)) return jwtDecoder.decode(credential);
        if (legacyTicketVerifier.supports(credential)) return legacyTicketVerifier.verify(credential);
        throw new BadCredentialsException("BPI credential format is not supported");
    }

    private boolean isJwt(String credential) {
        int first = credential.indexOf('.');
        return first > 0 && credential.indexOf('.', first + 1) > first + 1;
    }
}
