package cn.supcon.supfusion.systemconfig.config.filters;

import cn.supcon.supfusion.systemconfig.config.RefreshLocalConfigFromRemote;
import cn.supcon.supfusion.systemconfig.config.utils.TargetUtils;
import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.systemconfig.api.SystemApiService;
import com.supcon.supfusion.systemconfig.api.dto.ConfigAndVersionDTO;
import com.supcon.supfusion.systemconfig.api.tenantconfig.annotation.ClassSystemConfigAnno;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static AtomicBoolean scanned = new AtomicBoolean(false);

    @Override
    public void init(FilterConfig filterConfig) {
        if (log.isDebugEnabled()) {
            log.debug("==> beginning to init filter: {}", AnnoFilter.class.getName());
        }
        try {
            ConfigAndVersionDTO configAndVersionDTO = systemApiService.getConfigByVersionForFramework(RefreshLocalConfigFromRemote.versionMap);
            if (configAndVersionDTO.getIsUpdate()) {
                RefreshLocalConfigFromRemote.configMap = configAndVersionDTO.getConfigMap();
                RefreshLocalConfigFromRemote.versionMap = configAndVersionDTO.getVersionMap();
            }
        } catch (Exception e) {
            log.error("fail to init filter: " + AnnoFilter.class.getName(), e);
        }

        this.scanConfigAnnotatedBeans();
        this.doRefresh();
    }

    private void scanConfigAnnotatedBeans() {
        try {
            Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ClassSystemConfigAnno.class);
            if (!CollectionUtils.isEmpty(beans)) {
                beans.forEach((k,v) -> {
                    try {
                        Object entity = TargetUtils.getTarget(v);
                        if (entity != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("find annotated bean class {} with '@ClassSystemConfigAnno'", entity.getClass().getName());
                            }
                            Field[] fields = entity.getClass().getDeclaredFields();
                            if (fields != null && fields.length > 0) {
                                Arrays.asList(fields).forEach(f -> {
                                    if (f.isAnnotationPresent(Value.class)) {
                                        if (log.isDebugEnabled()) {
                                            log.debug("find annotated(@Value) field {} on bean class {} who annotated with '@ClassSystemConfigAnno'", f.getName(), entity.getClass().getName());
                                        }
                                        ConfigAnnotatedBeanCache.put(entity, f);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        log.warn("fail to get target bean class for bean '" + v.getClass().getName() + "'", e);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("fail to scan bean classes witch annotated '@ClassSystemConfigAnno', system will exit", e);
        }
    }

    private void doRefresh() {
        if (!ConfigAnnotatedBeanCache.isEmpty()) {
            ConfigAnnotatedBeanCache.getCache().forEach((bean,fields) -> fields.forEach(field -> refreshAnnotatedFieldValue(bean, field)));
        }
    }

    private void refreshAnnotatedFieldValue(Object bean, Field field) {
        try {
            Value fieldSystemConfigAnno = field.getAnnotation(Value.class);
            if (null != fieldSystemConfigAnno) {
                //识别注解值为开头为 % 的标识
                String originValue = fieldSystemConfigAnno.value();
                if (!originValue.startsWith("$") || !originValue.contains(":") || !originValue.contains(CONFIG_SPLIT)) {
                    return;
                }

                String[] split1 = originValue.split(":");
                String[] split2 = split1[0].split("\\{");
                String initValue = split2[1];

                //对上次请求的目标对象的属性清空
                String substring1 = split1[1].substring(0, split1[1].length() - 1);
                this.setTargetFieldValue(bean, field, substring1);

                //不符合格式的注解值不对该注解的属性进行赋值
                if (StringUtils.isEmpty(initValue)) {
                    return;
                }

                String tenantId = RpcContext.getContext().getTenantId();
                if (ObjectUtils.isEmpty(tenantId)) {
                    tenantId= System.getenv("SUPOS_SUPOS_APP_TENANT_ID");
                    if (ObjectUtils.isEmpty(tenantId)) {
                        tenantId = "dt";
                    }
                }
                ConcurrentHashMap<String, HashMap<String, Object>> moduleMap = RefreshLocalConfigFromRemote.configMap.get(tenantId);

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
                        return;
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

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (scanned.compareAndSet(false, true)) {
            this.scanConfigAnnotatedBeans();
        }
        this.doRefresh();
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

    /**
     * 注解BEAN缓存，一次扫描，全程使用
     */
    private static final class ConfigAnnotatedBeanCache {

        private static final Map<Object, Set<Field>> CACHE = new ConcurrentHashMap<>();

        static synchronized void put(Object bean, Field field) {
            CACHE.computeIfAbsent(bean, k -> new HashSet<>()).add(field);
        }

        static boolean isEmpty() {
            return CollectionUtils.isEmpty(CACHE);
        }

        static Map<Object, Set<Field>> getCache() {
            return CACHE;
        }

        static void clear() {
            CACHE.clear();
        }
    }
}