package com.supcon.supfusion.ws.service.util;

import com.supcon.supfusion.ws.service.annotation.BeforeHandshake;
import com.supcon.supfusion.ws.service.annotation.Controller;
import com.supcon.supfusion.ws.service.annotation.OnBinary;
import com.supcon.supfusion.ws.service.annotation.OnClose;
import com.supcon.supfusion.ws.service.annotation.OnMessage;
import com.supcon.supfusion.ws.service.annotation.OnOpen;
import com.supcon.supfusion.ws.service.annotation.WsController;
import com.supcon.supfusion.ws.service.constant.WsConstants;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class ClassScanner {
    private static final Map<String, ControllerRegistry> hander = new HashMap<>();
    private static final Map<String, MethodMapping> map = new HashMap<>();

    public static Map<String, ControllerRegistry> getControllerRegistry() {
        return hander;
    }

    public static Map<String, MethodMapping> getWsController() {
        return map;
    }

    public void httpScan(String packageName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<String> className = PackageUtil.getClassName(packageName);
        for (String name : className) {
           log.info("http scan name======" + name);
            Class<?> controller = Class.forName(name);
            Object object = controller.newInstance();
            Controller annotation = controller.getAnnotation(Controller.class);
            Method handleMethod = null;
            for (Method declaredMethod : controller.getDeclaredMethods()) {
                if (Objects.equals(WsConstants.HAND, declaredMethod.getName())) {
                    handleMethod = declaredMethod;
                    break;
                }
            }
            ControllerRegistry controllerRegistry = ControllerRegistry.builder()
                    .className(name)
                    .method(annotation.method())
                    .aClass(controller)
                    .instance(object)
                    .classMethod(handleMethod)
                    .build();
            String uriPattern = convertPatternUri(annotation.uriPattern());
            hander.put(uriPattern, controllerRegistry);

        }
    }

    public void wsScan(String packageName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        List<String> className = PackageUtil.getClassName(packageName);
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        for (String name : className) {
            log.info("ws scan name======" + name);
            Class<?> aClass = Class.forName(name);
            Object instance = aClass.newInstance();
            WsController annotation = aClass.getAnnotation(WsController.class);
            Method[] declaredMethods = aClass.getDeclaredMethods();
            MethodMapping methodMapping = new MethodMapping();
            for (Method method : declaredMethods) {
                if (method.getAnnotation(BeforeHandshake.class) != null) {
                    methodMapping.setBeforeHandshake(method);
                } else if (method.getAnnotation(OnOpen.class) != null) {
                    methodMapping.setOnOpen(method);
                } else if (method.getAnnotation(OnClose.class) != null) {
                    methodMapping.setOnClose(method);
                } else if (method.getAnnotation(OnMessage.class) != null) {
                    methodMapping.setOnMessage(method);
                } else if (method.getAnnotation(OnBinary.class) != null) {
                    methodMapping.setOnBinary(method);
                }
            }
            methodMapping.setInstance(instance);
            String uriPattern = convertPatternUri(annotation.uriPattern());
            map.put(uriPattern, methodMapping);
        }
    }

    private String convertPatternUri(String patternUri) {
        return WsConstants.PREFIX_REGEX + patternUri.replaceAll(WsConstants.PATH_VARIABLE_REGEX, WsConstants.EVERY_CHAR_REGEX) + WsConstants.SUFFIX_REGEX;
    }

    @Data
    @Builder
    public static class ControllerRegistry {
        private String method;
        private String className;
        private Class aClass;
        private Object instance;
        private Method classMethod;
    }

    @Data
    @NoArgsConstructor
    public static class MethodMapping {
        private Method beforeHandshake;
        private Method onOpen;
        private Method onClose;
        private Method onError;
        private Method onMessage;
        private Method onBinary;
        private Method onEvent;
        private Object instance;
    }
}
