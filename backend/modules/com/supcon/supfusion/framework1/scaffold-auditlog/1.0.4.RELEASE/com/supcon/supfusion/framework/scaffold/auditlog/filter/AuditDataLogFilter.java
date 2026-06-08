package com.supcon.supfusion.framework.scaffold.auditlog.filter;

import com.supcon.supfusion.framework.scaffold.auditlog.cache.AuditDataLogModelCache;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author caokele
 * 审计日志过滤器
 */
public class AuditDataLogFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(httpServletRequest, httpServletResponse);
        // 清除审计数据日志涉及模型缓存
        AuditDataLogModelCache.clear();
    }
}
