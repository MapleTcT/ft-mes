package com.mapletct.ftmes.rmformula;

import com.mapletct.ftmes.rmformula.domain.RmFormulaBusinessException;
import com.mapletct.ftmes.rmformula.support.RequestContext;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RequestContextTest {
    private final RequestContext context = new RequestContext("default");

    @Test
    public void acceptsOnlyTheInternalNginxAuthProof() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-ADP-Auth-Checked", "1");

        context.requireAuthenticated(request);
    }

    @Test
    public void rejectsAClientCredentialWithoutTheInternalProof() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer client-supplied-token");

        try {
            context.requireAuthenticated(request);
            fail("Expected authentication failure");
        } catch (RmFormulaBusinessException exception) {
            assertEquals(401, exception.getCode());
        }
    }
}
