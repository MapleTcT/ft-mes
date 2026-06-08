/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.beans.factory.InitializingBean
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.http.server.PathContainer
 *  org.springframework.web.server.ServerWebExchange
 *  org.springframework.web.util.pattern.PathPattern
 *  org.springframework.web.util.pattern.PathPatternParser
 */
package com.supcon.supos.suposgateway.filter.support;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.server.PathContainer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

@ConfigurationProperties(value="path")
public class PathMatcher
implements InitializingBean {
    private PathPatternParser pathPatternParser = new PathPatternParser();
    private List<PathPattern> blackListPathPatterns = new ArrayList<PathPattern>();
    private List<PathPattern> excludeGlobalPathPatterns = new ArrayList<PathPattern>();
    private List<PathPattern> excludeSignPathPatterns = new ArrayList<PathPattern>();
    private List<String> blackList = new ArrayList<String>();
    private List<String> excludeGlobal = new ArrayList<String>();
    private List<String> excludeSign = new ArrayList<String>();

    public void setBlackList(List<String> blackList) {
        this.blackList = blackList;
    }

    public void setExcludeGlobal(List<String> excludeGlobal) {
        this.excludeGlobal = excludeGlobal;
    }

    public void setExcludeSign(List<String> excludeSign) {
        this.excludeSign = excludeSign;
    }

    public void afterPropertiesSet() throws Exception {
        this.pathPatternParser.setMatchOptionalTrailingSeparator(true);
        this.blackList.forEach(pattern -> {
            PathPattern pathPattern = this.pathPatternParser.parse(pattern);
            this.blackListPathPatterns.add(pathPattern);
        });
        this.excludeGlobal.forEach(pattern -> {
            PathPattern pathPattern = this.pathPatternParser.parse(pattern);
            this.excludeGlobalPathPatterns.add(pathPattern);
        });
        this.excludeSign.forEach(pattern -> {
            PathPattern pathPattern = this.pathPatternParser.parse(pattern);
            this.excludeSignPathPatterns.add(pathPattern);
        });
    }

    public boolean matchBlackList(ServerWebExchange exchange) {
        if (this.blackListPathPatterns.isEmpty()) {
            return false;
        }
        PathContainer pathContainer = PathContainer.parsePath((String)exchange.getRequest().getURI().getRawPath());
        return this.blackListPathPatterns.stream().anyMatch(pattern -> pattern.matches(pathContainer));
    }

    public boolean matchExcludeGlobal(ServerWebExchange exchange) {
        if (this.excludeGlobalPathPatterns.isEmpty()) {
            return false;
        }
        PathContainer pathContainer = PathContainer.parsePath((String)exchange.getRequest().getURI().getRawPath());
        return this.excludeGlobalPathPatterns.stream().anyMatch(pattern -> pattern.matches(pathContainer));
    }

    public boolean matchExcludeSign(ServerWebExchange exchange) {
        if (this.excludeSignPathPatterns.isEmpty()) {
            return false;
        }
        PathContainer pathContainer = PathContainer.parsePath((String)exchange.getRequest().getURI().getRawPath());
        return this.excludeSignPathPatterns.stream().anyMatch(pattern -> pattern.matches(pathContainer));
    }
}

