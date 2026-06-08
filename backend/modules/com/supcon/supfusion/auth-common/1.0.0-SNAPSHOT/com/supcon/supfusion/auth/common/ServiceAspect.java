package com.supcon.supfusion.auth.common;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.ibatis.javassist.ClassPool;
import org.apache.ibatis.javassist.CtClass;
import org.apache.ibatis.javassist.CtMethod;
import org.apache.ibatis.javassist.NotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class ServiceAspect {

    @Autowired
    private ObjectMapper objectMapper;

    @Pointcut("execution(* com.supcon.supfusion.auth.service.impl..*.*(..))")
    public void logAroundPointCut() {
    }


    @Around("logAroundPointCut()")
    public Object intoControllerLog(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        String uid = Long.toString(Optional.ofNullable(RpcContext.getContext().getTraceId()).orElse(IDGenerator.newInstance().generate().longValue()));
        Throwable throwable = null;

        MethodInvocationProceedingJoinPoint mjp = (MethodInvocationProceedingJoinPoint) point;
        MethodSignature signature = (MethodSignature) mjp.getSignature();


        Method method = signature.getMethod();
        String clazz = method.getDeclaringClass().getName();


        Object[] args = point.getArgs();
        Parameter[] parameters = method.getParameters();


        val sb = new StringBuilder();
        int line=-1;
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.get(clazz);
            Class<?>[] parameterTypes = method.getParameterTypes();
            CtMethod methodX = cc.getDeclaredMethod(method.getName(), Arrays.stream(parameterTypes).map(x -> {
                try {
                    return pool.get(x.getName());
                } catch (NotFoundException e) {
                    return null;
                }
            }).toArray(CtClass[]::new));
            line = methodX.getMethodInfo().getLineNumber(0);
        }catch (Throwable t){

        }

        // 类名+方法名
        sb.append("at " + clazz + "." + method.getName())
                .append("(")
                .append(method.getDeclaringClass().getSimpleName() + ".java")
                .append(":")
                .append(line)
                .append(")");
        List<Tuple2<Parameter, Integer>> params = Stream.ofAll(Arrays.stream(parameters)).zipWithIndex().filter(x -> Try.of(
                () -> (args[x._2] instanceof Serializable)
                        && !(args[x._2] instanceof BindingResult)
                        && !(args[x._2] instanceof HttpServletRequest)
                        && !(args[x._2] instanceof HttpServletResponse
                        && (Try.of(() -> objectMapper.writeValueAsString(args[x._2])).isSuccess())
                )
        ).getOrElse(false)).collect(Collectors.toList());

        Object[] objects = params.stream().map(x -> args[x._2]).toArray();



        // 调用目标方法
        Object result = null;
        try {
            result = point.proceed();
        } catch (Throwable e) {
            throwable = e;
        }
        if (throwable == null) {
            log.info(" traceId {} \n {} \n paramter: {}  \n  response {} \n Time-Consuming : {} ms", uid, sb, JSON.toJSONString(objects), JSON.toJSONString(result), System.currentTimeMillis() - startTime);
        }
        if (throwable != null) {
            log.info(" traceId {} \n {} \n paramter: {}  \n  response {} \n Time-Consuming : {} ms", uid, sb, JSON.toJSONString(objects), throwable.getMessage(), System.currentTimeMillis() - startTime);
            throw throwable;
        }
        // 调用结果返回
        return result;

    }

}
