package com.supcon.supfusion.configuration.services.openapi.filters;


import com.supcon.supfusion.framework.boot.cloud.security.properties.JwtTokenProperties;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.security.factory.UserContextFactory;
import com.supcon.supfusion.framework.cloud.security.pojo.JwtUser;
import com.supcon.supfusion.framework.cloud.security.util.JwtTokenUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.FilterConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 该过滤器的作用是：跳过oauth2.0的客户端认证，直接从Redis获取用户信息
 * 前提：根据认证服务的SessionId判断用户是否已经登录
 * Order 该过滤器要确保在SessionRepositoryFilter之后，在SpringSecurity过滤器之前
 */
@Component
@Slf4j
@WebFilter(filterName = "sessionUserFilter", urlPatterns = {"/**"})//过滤器名称，以及过滤url
public class SessionUserFilter implements Filter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    JwtTokenProperties jwtTokenProperties;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        UserContext userContext = UserContext.getUserContext();
        if (userContext == null){
            return;
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    @Override
    public void destroy() {

    }

    private void getJwtUser(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtTokenProperties.getHeader());
        if (authHeader != null && authHeader.startsWith(jwtTokenProperties.getTokenHead())) {
            // The part after "Bearer "
            final String authToken = authHeader.substring(jwtTokenProperties.getTokenHead().length());
            JwtUser jwtUser = jwtTokenUtil.getJwtUserFromToken(authToken);
            if (null != jwtUser && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtTokenUtil.validateToken(authToken, jwtUser)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(jwtUser, null, jwtUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    log.info("authenticated user " + jwtUser.getUsername() + ", setting security context");
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    UserContextFactory.create(jwtUser);
                }
            }

        }
    }
}
