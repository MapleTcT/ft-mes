package com.mapletct.ftmes.rmformula.support;

import com.mapletct.ftmes.rmformula.domain.RmFormulaBusinessException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequestContext {
    private final String defaultTenant;

    public RequestContext(@Value("${rm-formula-editor.default-tenant:default}") String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    public void requireAuthenticated(HttpServletRequest request) {
        String checked = text(request.getHeader("X-ADP-Auth-Checked"));
        if (!"1".equals(checked)) {
            throw new RmFormulaBusinessException(401, "登录状态已失效，请重新登录");
        }
    }

    public String tenant() {
        String value = text(defaultTenant);
        if (value.isEmpty()) {
            return "default";
        }
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
