package com.mapletct.ftmes.bpiadapter;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class BpiRoutePolicy {

    private static final String ID = "[A-Za-z0-9._:-]{1,128}";
    private static final Pattern GET_ROUTE = Pattern.compile(
            "^/(?:overview|lines/" + ID + "/current-state|candidates(?:/" + ID + ")?|"
                    + "batches(?:/" + ID + "(?:/(?:evidence|timeline))?)?|"
                    + "topologies(?:/" + ID + ")?|rules(?:/" + ID + ")?|rule-simulations/" + ID + ")$");
    private static final Pattern POST_ROUTE = Pattern.compile(
            "^/(?:candidates/" + ID + "/(?:confirm|reject)|batches/" + ID + "/(?:suspend|resume)|"
                    + "rules/" + ID + "/(?:simulate|publish|publication/retry))$");

    public boolean allows(HttpMethod method, String path) {
        if (method == HttpMethod.GET) return GET_ROUTE.matcher(path).matches();
        if (method == HttpMethod.POST) return POST_ROUTE.matcher(path).matches();
        return false;
    }
}
