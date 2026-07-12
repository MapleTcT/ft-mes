package com.mapletct.ftmes.bpiadapter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class BpiActor {
    private final String subject;
    private final String tenantId;
    private final Set<String> roles;
    private final Set<String> plantIds;
    private final Set<String> lineIds;

    public BpiActor(String subject, String tenantId, Set<String> roles, Set<String> plantIds, Set<String> lineIds) {
        this.subject = subject;
        this.tenantId = tenantId;
        this.roles = immutable(roles);
        this.plantIds = immutable(plantIds);
        this.lineIds = immutable(lineIds);
    }

    private static Set<String> immutable(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(values));
    }

    public String getSubject() { return subject; }
    public String getTenantId() { return tenantId; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getPlantIds() { return plantIds; }
    public Set<String> getLineIds() { return lineIds; }
}
