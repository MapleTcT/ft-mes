package com.supcon.supfusion.base.interceptor;

import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/3/12
 */
@ControllerAdvice
public class ResponseDataHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> aClass) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object o, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
        return o;
    }
}
