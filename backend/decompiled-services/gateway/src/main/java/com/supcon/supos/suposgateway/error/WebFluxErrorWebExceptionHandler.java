/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.autoconfigure.web.ErrorProperties
 *  org.springframework.boot.autoconfigure.web.ResourceProperties
 *  org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler
 *  org.springframework.boot.web.reactive.error.ErrorAttributes
 *  org.springframework.context.ApplicationContext
 *  org.springframework.http.HttpStatus
 *  org.springframework.http.MediaType
 *  org.springframework.web.reactive.function.server.ServerRequest
 *  org.springframework.web.reactive.function.server.ServerResponse
 *  org.springframework.web.reactive.function.server.ServerResponse$BodyBuilder
 *  org.springframework.web.util.HtmlUtils
 *  reactor.core.publisher.Mono
 */
package com.supcon.supos.suposgateway.error;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;

public class WebFluxErrorWebExceptionHandler
extends DefaultErrorWebExceptionHandler {
    @Value(value="${spring.application.name:}")
    private String serviceName;
    @Value(value="${integration.supos.enabled:true}")
    private Boolean supOSEnabled;
    @Value(value="${server.port:8080}")
    private String port;

    public WebFluxErrorWebExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties, ErrorProperties errorProperties, ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, errorProperties, applicationContext);
    }

    protected Mono<ServerResponse> renderErrorView(ServerRequest request) {
        boolean includeStackTrace = this.isIncludeStackTrace(request, MediaType.TEXT_HTML);
        Map error = this.getErrorAttributes(request, includeStackTrace);
        HttpStatus errorStatus = this.getHttpStatus(error);
        String message = (String)this.getErrorAttributes(request, includeStackTrace).get("message");
        if (errorStatus == HttpStatus.INTERNAL_SERVER_ERROR && message.contains("\u5f53\u524d\u6a21\u5757\u65e0\u8f6f\u4ef6\u72d7\u6388\u6743")) {
            String showInfoUrl = "/greenDill/static/license/";
            return ServerResponse.temporaryRedirect((URI)URI.create(showInfoUrl + errorStatus.value() + ".html?errorMessage=" + message)).build();
        }
        URI uri = request.exchange().getRequest().getURI();
        if (errorStatus == HttpStatus.UNAUTHORIZED && uri.getRawPath().startsWith("/msService") && !this.supOSEnabled.booleanValue()) {
            String showInfoUrl = "/error/";
            String redirectUrl = null;
            try {
                redirectUrl = URLEncoder.encode(uri.toString(), "UTF-8");
            }
            catch (UnsupportedEncodingException unsupportedEncodingException) {
                // empty catch block
            }
            return ServerResponse.temporaryRedirect((URI)URI.create(showInfoUrl + errorStatus.value() + ".html?redirectUrl=" + redirectUrl + "&env=adp")).build();
        }
        return super.renderErrorView(request);
    }

    protected Mono<ServerResponse> renderDefaultErrorView(ServerResponse.BodyBuilder responseBody, Map<String, Object> error) {
        StringBuilder builder = new StringBuilder();
        Date timestamp = (Date)error.get("timestamp");
        Object message = error.get("message");
        Object trace = error.get("trace");
        builder.append("<html><body><h1>Whitelabel Error Page</h1>").append("<h2>").append(this.serviceName).append("</h2>").append("<p>This application has no configured error view, so you are seeing this as a fallback.</p>").append("<div id='created'>").append(timestamp).append("</div>").append("<div>There was an unexpected error (type=").append(this.htmlEscape(error.get("error"))).append(", status=").append(this.htmlEscape(error.get("status"))).append(").</div>");
        if (message != null) {
            builder.append("<div>").append(this.htmlEscape(message)).append("</div>");
        }
        if (trace != null) {
            builder.append("<div style='white-space:pre-wrap;'>").append(this.htmlEscape(trace)).append("</div>");
        }
        builder.append("</body></html>");
        return responseBody.syncBody((Object)builder.toString());
    }

    private String htmlEscape(Object input) {
        return input != null ? HtmlUtils.htmlEscape((String)input.toString()) : null;
    }
}

