package com.supcon.supfusion.base.interceptor;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description:工程期sessionfactory切换拦截
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/3/12
 */
@Component
public class ProjectSessionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Boolean isProj = Boolean.valueOf(request.getParameter("isProj"));
        if(null!=isProj && isProj){
            ProjectFlagHolder.getInstance().getProjFlag().set(true);
        }
        return true;
    }
}
