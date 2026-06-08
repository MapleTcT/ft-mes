//package com.supcon.supfusion.rbac.service.config;
//
//import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
//import com.supcon.supfusion.rbac.api.IRbacApiService;
//import com.supcon.supfusion.rbac.api.dto.MenuOperateCodeUrlRefDTO;
//import com.supcon.supfusion.rbac.common.annotation.MenuOperateCode;
//import lombok.extern.slf4j.Slf4j;
//import org.reflections.Reflections;
//import org.reflections.scanners.MethodAnnotationsScanner;
//import org.reflections.util.ClasspathHelper;
//import org.reflections.util.ConfigurationBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.web.bind.annotation.*;
//
//import java.lang.reflect.Method;
//import java.util.*;
//
//@Configuration
//@Slf4j
//@Order(value = 2)
//public class MenuOperateScan implements CommandLineRunner {
//    @Value("${spring.application.name}")
//    String appName;
//    @Autowired
//    IRbacApiService rbacApiService;
//
//    @Override
//    public void run(String... args) throws Exception {
//        Reflections reflections =
//                new Reflections(new ConfigurationBuilder().
//                        setUrls(ClasspathHelper.
//                                forPackage("com.supcon.supfusion")).
//                        setScanners(new MethodAnnotationsScanner()));
//        Set<Method> methods = reflections.getMethodsAnnotatedWith(MenuOperateCode.class);
//        List<MenuOperateCodeUrlRefDTO> list = new ArrayList<>();
//        Map<String, Integer> map = new HashMap<>();
//        map.put("GET",0);
//        map.put("POST",1);
//        map.put("PUT",2);
//        map.put("DELETE",3);
//        methods.forEach(method -> generate(method, list,map));
//        rbacApiService.saveBachUrl(list, appName);
//    }
//
//    private void generate(Method method, List<MenuOperateCodeUrlRefDTO> list,Map<String, Integer> map) {
//        String rootUrl = method.getDeclaringClass().getAnnotation(InternalApi.class).path();
//        //操作编码
//        String menuOperateCode = method.getAnnotation(MenuOperateCode.class).value();
//        if (method.getAnnotation(PutMapping.class) != null && method.getAnnotation(PutMapping.class).value().length > 0) {
//            for (String url : method.getAnnotation(PutMapping.class).value()) {
//                generateDTO(list, menuOperateCode, 2, rootUrl, filterPathParams(url));
//            }
//        } else if (method.getAnnotation(GetMapping.class) != null && method.getAnnotation(GetMapping.class).value().length > 0) {
//            for (String url : method.getAnnotation(GetMapping.class).value()) {
//                generateDTO(list, menuOperateCode, 0, rootUrl, filterPathParams(url));
//            }
//        } else if (method.getAnnotation(PostMapping.class) != null && method.getAnnotation(PostMapping.class).value().length > 0) {
//            for (String url : method.getAnnotation(PostMapping.class).value()) {
//                generateDTO(list, menuOperateCode, 1, rootUrl, filterPathParams(url));
//            }
//        } else if (method.getAnnotation(DeleteMapping.class) != null && method.getAnnotation(DeleteMapping.class).value().length > 0) {
//            for (String url : method.getAnnotation(DeleteMapping.class).value()) {
//                generateDTO(list, menuOperateCode, 3, rootUrl, filterPathParams(url));
//            }
//        } else if (method.getAnnotation(RequestMapping.class) != null && method.getAnnotation(RequestMapping.class).method().length > 0 && method.getAnnotation(RequestMapping.class).value().length > 0) {
//            int index = 0;
//            RequestMethod[] methodTypes = method.getAnnotation(RequestMapping.class).method();
//            for (String url : method.getAnnotation(RequestMapping.class).value()) {
//                generateDTO(list, menuOperateCode, map.get(methodTypes[index].name()), rootUrl, filterPathParams(url));
//                index++;
//            }
//        }
//    }
//
//    /**
//     * @description:
//     * @param: list
//     * @param: menuOperateCode
//     * @param: methodType
//     * @param: rootUrl
//     * @param: url
//     * @return: void
//     * @author: 袁阳
//     * @date: 2020/6/22
//     */
//    private void generateDTO(List<MenuOperateCodeUrlRefDTO> list, String menuOperateCode, int methodType, String rootUrl, String url) {
//        MenuOperateCodeUrlRefDTO menuOperateCodeUrlRefDTO = new MenuOperateCodeUrlRefDTO();
//        menuOperateCodeUrlRefDTO.setMenuoperateCode(menuOperateCode);
//        menuOperateCodeUrlRefDTO.setAppId(appName);
//        menuOperateCodeUrlRefDTO.setUrl(rootUrl + url);
//        menuOperateCodeUrlRefDTO.setMethodType(methodType);
//        menuOperateCodeUrlRefDTO.setRegMatch(url.contains("/[^/^]"));
//        list.add(menuOperateCodeUrlRefDTO);
//    }
//
//    /**
//     * @description: 过滤路径参数
//     * @param: url
//     * @return: java.lang.String
//     * @author: 袁阳
//     * @date: 2020/6/22
//     */
//    private String filterPathParams(String url) {
//        return url.replaceAll("/\\{[^\\}]+\\}", "/[^\\/\\^]+");
//    }
//}
