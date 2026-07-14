package com.mapletct.ftmes.bpiadapter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "bpi.adapter.upstream-base-url=http://bpi-service:19091",
        "bpi.adapter.keycloak-jwk-set-uri=http://keycloak:8080/auth/realms/dt/protocol/openid-connect/certs",
        "bpi.adapter.keycloak-issuer=http://127.0.0.1:8080/auth/realms/dt",
        "bpi.adapter.legacy-ticket-enabled=true",
        "bpi.adapter.legacy-gateway-base-url=http://gateway:8008",
        "bpi.adapter.internal-jwt-secret=0123456789abcdef0123456789abcdef",
        "bpi.adapter.role-rules=systemRole=BPI_ADMIN|BPI_OPERATOR",
        "bpi.adapter.subject-scope-rules=admin=1000|PLANT-01|LINE-S07-01"
})
public class BpiAdapterApplicationContextTest {

    @Autowired
    private BpiBearerAuthenticationFilter authenticationFilter;

    @Test
    public void startsWithoutSecurityBeanCycle() {
        assertNotNull(authenticationFilter);
    }
}
