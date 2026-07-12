package com.mapletct.ftmes.bpi.application;

import com.mapletct.ftmes.bpi.application.error.BpiForbiddenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class ActorContextFactory {

    public ActorContext from(Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new BpiForbiddenException("Trusted token is missing tenant_id.");
        }
        return new ActorContext(
                tenantId,
                jwt.getSubject(),
                claimSet(jwt, "roles"),
                claimSet(jwt, "plant_ids"),
                claimSet(jwt, "line_ids"));
    }

    private Set<String> claimSet(Jwt jwt, String claim) {
        List<String> values = jwt.getClaimAsStringList(claim);
        return values == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(values));
    }
}
