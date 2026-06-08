
package com.supcon.supfusion.systemconfig.api.tenantconfig.filter;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.systemconfig.api.SystemApiService;
import com.supcon.supfusion.systemconfig.api.dto.ConfigAndVersionDTO;
import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import com.supcon.supfusion.systemconfig.api.tenantconfig.task.RefreshConfigInfoTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 注入spring容器
//@Component
// 定义filterName 和过滤的url
@WebFilter(filterName = "annoFilter", urlPatterns = "/*")
@Slf4j
public class AnnoFilter implements Filter {
    private static final String CONFIG_SPLIT = "/";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SystemApiService systemApiService;

    private static ConcurrentHashMap<Object, Object> beanMap = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) {
        log.debug("过滤器初始化开始 ==========");
        try {
            ConfigAndVersionDTO configAndVersionDTO = systemApiService.getConfigByVersionForFramework(RefreshConfigInfoTask.versionMap);
            if (configAndVersionDTO.getIsUpdate()) {
                RefreshConfigInfoTask.configMap = configAndVersionDTO.getConfigMap();
                RefreshConfigInfoTask.versionMap = configAndVersionDTO.getVersionMap();
            }
        } catch (Exception e) {
            log.error("过滤器初始化调用系统配置发生错误 =========", e);
        }

        this.doAnnoFilter();
    }

    private void doAnnoFilter() {
        try {
            log.debug("过滤器初始化正常执行 ===========");

            Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ClassSystemConfigAnno.class);
            for (Object bean : beans.values()) {
                bean = TargetUtils.getTarget(bean);
                Field[] fields = bean.getClass().getDeclaredFields();
                for (Field field : fields) {
                    try {
                        Value fieldSystemConfigAnno = field.getAnnotation(Value.class);
                        if (null != fieldSystemConfigAnno) {
                            //识别注解值为开头为 % 的标识
                            String originValue = fieldSystemConfigAnno.value();
                            if (!originValue.startsWith("$") || !originValue.contains(":") || !originValue.contains(CONFIG_SPLIT)) {
                                continue;
                            }

                            String[] split1 = originValue.split(":");
                            String[] split2 = split1[0].split("\\{");
                            String initValue = split2[1];

                            //对上次请求的目标对象的属性清空
                            String substring1 = split1[1].substring(0, split1[1].length() - 1);
                            this.setTargetFieldValue(bean, field, substring1);

                            //不符合格式的注解值不对该注解的属性进行赋值
                            if (StringUtils.isEmpty(initValue)) {
                                continue;
                            }

                            String tenantId = RpcContext.getContext().getTenantId();
                            if (ObjectUtils.isEmpty(tenantId)) {
                                tenantId= System.getenv("SUPOS_SUPOS_APP_TENANT_ID");
                                if (ObjectUtils.isEmpty(tenantId)) {
                                    tenantId = "dt";
                                }
                            }
                            ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = RefreshConfigInfoTask.configMap.get(tenantId);

                            if (!ObjectUtils.isEmpty(moduleMap)) {
                                String[] split = initValue.split(CONFIG_SPLIT);
                                String moduleKey = "";
                                if (3 == split.length) {
                                    moduleKey = split[0] + CONFIG_SPLIT + split[1];
                                } else if (2 == split.length) {
                                    moduleKey = split[0] + CONFIG_SPLIT + split[0];
                                }
                                HashMap<String, Object> hashMap = moduleMap.get(moduleKey);
                                if (ObjectUtils.isEmpty(hashMap)) {
                                    continue;
                                }

                                String substring = initValue.substring(initValue.lastIndexOf(CONFIG_SPLIT) + 1);
                                Object finalValue = hashMap.get(substring);
                                List list = JSON.parseObject(JSON.toJSONString(finalValue), List.class);

                                if (!ObjectUtils.isEmpty(list)) {
                                    //获取代理对象的目标对象，对目标对象赋值
                                    this.setTargetFieldValue(bean, field, list.get(0));
                                }
                            }

                        }
                    } catch (Exception e) {
                        log.error("注解过滤器初始化解析字段发生错误 ", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("注解过滤器初始化发生错误 ", e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        this.doAnnoFilter();
        filterChain.doFilter(servletRequest, servletResponse);
    }


    /**
     * 获取代理对象的目标对象，对目标对象赋值
     *
     * @param bean
     * @param field
     * @param value
     * @throws Exception
     */
    private void setTargetFieldValue(Object bean, Field field, Object value) {
        try {
            if (ObjectUtils.isEmpty(value)) {
                value = null;
            } else {
                //类型判断
                if (field.getType().getName().contains("Boolean")) {
                    value = Boolean.valueOf(value.toString());
                }
                if (field.getType().getName().contains("Integer")) {
                    value = Integer.valueOf(value.toString());
                }
                if (field.getType().getName().contains("String")) {
                    value = String.valueOf(value.toString());
                }
            }
            field.setAccessible(true);
            field.set(bean, value);
        } catch (Exception e) {
            log.error("过滤器给Bean进行赋值时发生错误:{},bean:{},field:{}", e.getMessage(), bean, field);
        }
    }


    @Override
    public void destroy() {
    }
}