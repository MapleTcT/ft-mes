package com.supcon.supfusion.configuration.services.openapi.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class ResponseUtils {

    public static final String ENCODING_PREFIX = "encoding";
    public static final String NOCACHE_PREFIX = "no-cache";
    public static final String ENCODING_DEFAULT = "UTF-8";
    public static final boolean NOCACHE_DEFAULT = true;

    public static final String JSON_TYPE = "application/json";
    public static final String XML_TYPE = "text/xml";
    public static final String HTML_TYPE = "text/html";
    public static final String XLS_TYPE = "application/vnd.ms-excel";

    public static void render(HttpServletResponse response, final String contentType, final String content, final String... headers) {
        String encoding = ENCODING_DEFAULT;
        boolean noCache = NOCACHE_DEFAULT;
        for (String header : headers) {
            String headerName = StringUtils.substringBefore(header, ":");
            String headerValue = StringUtils.substringAfter(header, ":");
            if (StringUtils.equalsIgnoreCase(headerName, ENCODING_PREFIX))
                encoding = headerValue;
            else if (StringUtils.equalsIgnoreCase(headerName, NOCACHE_PREFIX))
                noCache = Boolean.parseBoolean(headerValue);
            else
                throw new IllegalArgumentException("header's prefix is not a correct type.");
        }
        String fullContentType = contentType + "; charset=" + encoding;
        response.setContentType(fullContentType);
        if (noCache) {
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
        }
        try {
            response.getWriter().write(content);
            response.getWriter().flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

}
