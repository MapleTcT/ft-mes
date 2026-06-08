/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.util.AntPathMatcher
 */
package com.supcon.supfusion.ws.service.util;

import com.supcon.supfusion.ws.service.annotation.BeforeHandshake;
import com.supcon.supfusion.ws.service.annotation.Controller;
import com.supcon.supfusion.ws.service.annotation.OnBinary;
import com.supcon.supfusion.ws.service.annotation.OnClose;
import com.supcon.supfusion.ws.service.annotation.OnMessage;
import com.supcon.supfusion.ws.service.annotation.OnOpen;
import com.supcon.supfusion.ws.service.annotation.WsController;
import com.supcon.supfusion.ws.service.util.PackageUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

public class ClassScanner {
    private static final Logger log = LoggerFactory.getLogger(ClassScanner.class);
    private static final Map<String, ControllerRegistry> hander = new HashMap<String, ControllerRegistry>();
    private static final Map<String, MethodMapping> map = new HashMap<String, MethodMapping>();

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
                if (!Objects.equals("hand", declaredMethod.getName())) continue;
                handleMethod = declaredMethod;
                break;
            }
            ControllerRegistry controllerRegistry = ControllerRegistry.builder().className(name).method(annotation.method()).aClass(controller).instance(object).classMethod(handleMethod).build();
            String uriPattern = this.convertPatternUri(annotation.uriPattern());
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
                    continue;
                }
                if (method.getAnnotation(OnOpen.class) != null) {
                    methodMapping.setOnOpen(method);
                    continue;
                }
                if (method.getAnnotation(OnClose.class) != null) {
                    methodMapping.setOnClose(method);
                    continue;
                }
                if (method.getAnnotation(OnMessage.class) != null) {
                    methodMapping.setOnMessage(method);
                    continue;
                }
                if (method.getAnnotation(OnBinary.class) == null) continue;
                methodMapping.setOnBinary(method);
            }
            methodMapping.setInstance(instance);
            String uriPattern = this.convertPatternUri(annotation.uriPattern());
            map.put(uriPattern, methodMapping);
        }
    }

    private String convertPatternUri(String patternUri) {
        return "^" + patternUri.replaceAll("\\{.+\\}", ".*") + "$";
    }

    public static class MethodMapping {
        private Method beforeHandshake;
        private Method onOpen;
        private Method onClose;
        private Method onError;
        private Method onMessage;
        private Method onBinary;
        private Method onEvent;
        private Object instance;

        public Method getBeforeHandshake() {
            return this.beforeHandshake;
        }

        public Method getOnOpen() {
            return this.onOpen;
        }

        public Method getOnClose() {
            return this.onClose;
        }

        public Method getOnError() {
            return this.onError;
        }

        public Method getOnMessage() {
            return this.onMessage;
        }

        public Method getOnBinary() {
            return this.onBinary;
        }

        public Method getOnEvent() {
            return this.onEvent;
        }

        public Object getInstance() {
            return this.instance;
        }

        public void setBeforeHandshake(Method beforeHandshake) {
            this.beforeHandshake = beforeHandshake;
        }

        public void setOnOpen(Method onOpen) {
            this.onOpen = onOpen;
        }

        public void setOnClose(Method onClose) {
            this.onClose = onClose;
        }

        public void setOnError(Method onError) {
            this.onError = onError;
        }

        public void setOnMessage(Method onMessage) {
            this.onMessage = onMessage;
        }

        public void setOnBinary(Method onBinary) {
            this.onBinary = onBinary;
        }

        public void setOnEvent(Method onEvent) {
            this.onEvent = onEvent;
        }

        public void setInstance(Object instance) {
            this.instance = instance;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof MethodMapping)) {
                return false;
            }
            MethodMapping other = (MethodMapping)o;
            if (!other.canEqual(this)) {
                return false;
            }
            Method this$beforeHandshake = this.getBeforeHandshake();
            Method other$beforeHandshake = other.getBeforeHandshake();
            if (this$beforeHandshake == null ? other$beforeHandshake != null : !((Object)this$beforeHandshake).equals(other$beforeHandshake)) {
                return false;
            }
            Method this$onOpen = this.getOnOpen();
            Method other$onOpen = other.getOnOpen();
            if (this$onOpen == null ? other$onOpen != null : !((Object)this$onOpen).equals(other$onOpen)) {
                return false;
            }
            Method this$onClose = this.getOnClose();
            Method other$onClose = other.getOnClose();
            if (this$onClose == null ? other$onClose != null : !((Object)this$onClose).equals(other$onClose)) {
                return false;
            }
            Method this$onError = this.getOnError();
            Method other$onError = other.getOnError();
            if (this$onError == null ? other$onError != null : !((Object)this$onError).equals(other$onError)) {
                return false;
            }
            Method this$onMessage = this.getOnMessage();
            Method other$onMessage = other.getOnMessage();
            if (this$onMessage == null ? other$onMessage != null : !((Object)this$onMessage).equals(other$onMessage)) {
                return false;
            }
            Method this$onBinary = this.getOnBinary();
            Method other$onBinary = other.getOnBinary();
            if (this$onBinary == null ? other$onBinary != null : !((Object)this$onBinary).equals(other$onBinary)) {
                return false;
            }
            Method this$onEvent = this.getOnEvent();
            Method other$onEvent = other.getOnEvent();
            if (this$onEvent == null ? other$onEvent != null : !((Object)this$onEvent).equals(other$onEvent)) {
                return false;
            }
            Object this$instance = this.getInstance();
            Object other$instance = other.getInstance();
            return !(this$instance == null ? other$instance != null : !this$instance.equals(other$instance));
        }

        protected boolean canEqual(Object other) {
            return other instanceof MethodMapping;
        }

        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            Method $beforeHandshake = this.getBeforeHandshake();
            result = result * 59 + ($beforeHandshake == null ? 43 : ((Object)$beforeHandshake).hashCode());
            Method $onOpen = this.getOnOpen();
            result = result * 59 + ($onOpen == null ? 43 : ((Object)$onOpen).hashCode());
            Method $onClose = this.getOnClose();
            result = result * 59 + ($onClose == null ? 43 : ((Object)$onClose).hashCode());
            Method $onError = this.getOnError();
            result = result * 59 + ($onError == null ? 43 : ((Object)$onError).hashCode());
            Method $onMessage = this.getOnMessage();
            result = result * 59 + ($onMessage == null ? 43 : ((Object)$onMessage).hashCode());
            Method $onBinary = this.getOnBinary();
            result = result * 59 + ($onBinary == null ? 43 : ((Object)$onBinary).hashCode());
            Method $onEvent = this.getOnEvent();
            result = result * 59 + ($onEvent == null ? 43 : ((Object)$onEvent).hashCode());
            Object $instance = this.getInstance();
            result = result * 59 + ($instance == null ? 43 : $instance.hashCode());
            return result;
        }

        public String toString() {
            return "ClassScanner.MethodMapping(beforeHandshake=" + this.getBeforeHandshake() + ", onOpen=" + this.getOnOpen() + ", onClose=" + this.getOnClose() + ", onError=" + this.getOnError() + ", onMessage=" + this.getOnMessage() + ", onBinary=" + this.getOnBinary() + ", onEvent=" + this.getOnEvent() + ", instance=" + this.getInstance() + ")";
        }
    }

    public static class ControllerRegistry {
        private String method;
        private String className;
        private Class aClass;
        private Object instance;
        private Method classMethod;

        ControllerRegistry(String method, String className, Class aClass, Object instance, Method classMethod) {
            this.method = method;
            this.className = className;
            this.aClass = aClass;
            this.instance = instance;
            this.classMethod = classMethod;
        }

        public static ControllerRegistryBuilder builder() {
            return new ControllerRegistryBuilder();
        }

        public String getMethod() {
            return this.method;
        }

        public String getClassName() {
            return this.className;
        }

        public Class getAClass() {
            return this.aClass;
        }

        public Object getInstance() {
            return this.instance;
        }

        public Method getClassMethod() {
            return this.classMethod;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public void setAClass(Class aClass) {
            this.aClass = aClass;
        }

        public void setInstance(Object instance) {
            this.instance = instance;
        }

        public void setClassMethod(Method classMethod) {
            this.classMethod = classMethod;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ControllerRegistry)) {
                return false;
            }
            ControllerRegistry other = (ControllerRegistry)o;
            if (!other.canEqual(this)) {
                return false;
            }
            String this$method = this.getMethod();
            String other$method = other.getMethod();
            if (this$method == null ? other$method != null : !this$method.equals(other$method)) {
                return false;
            }
            String this$className = this.getClassName();
            String other$className = other.getClassName();
            if (this$className == null ? other$className != null : !this$className.equals(other$className)) {
                return false;
            }
            Class this$aClass = this.getAClass();
            Class other$aClass = other.getAClass();
            if (this$aClass == null ? other$aClass != null : !this$aClass.equals(other$aClass)) {
                return false;
            }
            Object this$instance = this.getInstance();
            Object other$instance = other.getInstance();
            if (this$instance == null ? other$instance != null : !this$instance.equals(other$instance)) {
                return false;
            }
            Method this$classMethod = this.getClassMethod();
            Method other$classMethod = other.getClassMethod();
            return !(this$classMethod == null ? other$classMethod != null : !((Object)this$classMethod).equals(other$classMethod));
        }

        protected boolean canEqual(Object other) {
            return other instanceof ControllerRegistry;
        }

        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            String $method = this.getMethod();
            result = result * 59 + ($method == null ? 43 : $method.hashCode());
            String $className = this.getClassName();
            result = result * 59 + ($className == null ? 43 : $className.hashCode());
            Class $aClass = this.getAClass();
            result = result * 59 + ($aClass == null ? 43 : $aClass.hashCode());
            Object $instance = this.getInstance();
            result = result * 59 + ($instance == null ? 43 : $instance.hashCode());
            Method $classMethod = this.getClassMethod();
            result = result * 59 + ($classMethod == null ? 43 : ((Object)$classMethod).hashCode());
            return result;
        }

        public String toString() {
            return "ClassScanner.ControllerRegistry(method=" + this.getMethod() + ", className=" + this.getClassName() + ", aClass=" + this.getAClass() + ", instance=" + this.getInstance() + ", classMethod=" + this.getClassMethod() + ")";
        }

        public static class ControllerRegistryBuilder {
            private String method;
            private String className;
            private Class aClass;
            private Object instance;
            private Method classMethod;

            ControllerRegistryBuilder() {
            }

            public ControllerRegistryBuilder method(String method) {
                this.method = method;
                return this;
            }

            public ControllerRegistryBuilder className(String className) {
                this.className = className;
                return this;
            }

            public ControllerRegistryBuilder aClass(Class aClass) {
                this.aClass = aClass;
                return this;
            }

            public ControllerRegistryBuilder instance(Object instance) {
                this.instance = instance;
                return this;
            }

            public ControllerRegistryBuilder classMethod(Method classMethod) {
                this.classMethod = classMethod;
                return this;
            }

            public ControllerRegistry build() {
                return new ControllerRegistry(this.method, this.className, this.aClass, this.instance, this.classMethod);
            }

            public String toString() {
                return "ClassScanner.ControllerRegistry.ControllerRegistryBuilder(method=" + this.method + ", className=" + this.className + ", aClass=" + this.aClass + ", instance=" + this.instance + ", classMethod=" + this.classMethod + ")";
            }
        }
    }
}

