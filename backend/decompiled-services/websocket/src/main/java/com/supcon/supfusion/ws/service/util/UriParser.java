/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.ws.service.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriParser {
    private String uri;
    private String absoluteUri;
    private List<String> sections = new ArrayList<String>();
    private Map<String, String> params = new HashMap<String, String>();

    public UriParser(String uri) {
        this.uri = uri;
        int pos = uri.indexOf("?");
        if (pos >= 0) {
            String[] pairs;
            this.absoluteUri = uri.substring(0, pos);
            String pStr = uri.substring(pos + 1);
            for (String pair : pairs = pStr.split("&")) {
                String[] nv = pair.split("=");
                if (nv.length != 2) continue;
                this.params.put(nv[0], nv[1]);
            }
        } else {
            this.absoluteUri = uri;
        }
        this.sections.add(this.absoluteUri);
    }

    public static void main(String[] args) {
        UriParser up = new UriParser("/test/aaa?x=1&yy=abc");
        System.out.println(up.getAbsoluteUri());
        System.out.println(up.getParam("x"));
        System.out.println(up.getParam("yy"));
        if (up.matches("^/test/(\\w+)")) {
            System.out.println(up.getSection(0));
        }
    }

    public String getAbsoluteUri() {
        return this.absoluteUri;
    }

    public boolean matches(String uriPattern) {
        Pattern pattern = Pattern.compile(uriPattern);
        Matcher matcher = pattern.matcher(this.uri);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); ++i) {
                this.sections.add(matcher.group(i));
            }
            return true;
        }
        return false;
    }

    public String getSection(int index) {
        return this.sections.get(index);
    }

    public String getParam(String name) {
        return this.params.get(name);
    }

    public static String getTopic(int index, String requestUri) {
        int paramsIndex = requestUri.indexOf("?");
        return paramsIndex < 0 ? requestUri.substring(index - 1) : requestUri.substring(index - 1, paramsIndex);
    }
}

