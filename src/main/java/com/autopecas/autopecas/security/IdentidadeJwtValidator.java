package com.autopecas.autopecas.security;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;

public final class IdentidadeJwtValidator implements OAuth2TokenValidator<Jwt> {
    private final String audience;

    public IdentidadeJwtValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            String sub = jwt.getSubject();
            if (sub == null
                    || !UUID.fromString(sub).toString().equalsIgnoreCase(sub)
                    || jwt.getExpiresAt() == null
                    || jwt.getIssuedAt() == null
                    || jwt.getIssuedAt().isAfter(Instant.now().plusSeconds(60))
                    || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                    || !jwt.getAudience().contains(audience)
                    || !Arrays.asList(jwt.getClaimAsString("scope").split(" "))
                            .contains("api:access")) return invalid();
            List<String> roles = jwt.getClaimAsStringList("roles");
            String perfil = jwt.getClaimAsString("perfil");
            if (roles == null || roles.isEmpty()) return invalid();
            if ("CLIENTE".equals(perfil)) {
                String tipo = jwt.getClaimAsString("tipo");
                if (!roles.equals(List.of("CLIENTE")) || !List.of("PF", "PJ").contains(tipo))
                    return invalid();
            } else if ("FUNCIONARIO".equals(perfil)) {
                if (!Set.of("ADMIN", "ATENDENTE", "MECANICO").containsAll(roles)) return invalid();
            } else return invalid();
            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException e) {
            return invalid();
        }
    }

    private static OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Identidade JWT invalida", null));
    }
}
