package com.supcon.orchid.ec.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.supcon.orchid.ec.entities.Entity;
import com.supcon.orchid.ec.entities.ExtraView;
import com.supcon.orchid.ec.entities.View;
import com.supcon.orchid.ec.enums.ShowType;
import com.supcon.orchid.ec.enums.ViewType;
import com.supcon.orchid.services.BAPException;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnClass(name = "com.supcon.greendill.BaseServiceApp")
public class BaseServiceCacheService implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(BaseServiceCacheService.class);
    private static final long MAXIMUM_SIZE = 5000L;
    private static final long EXPIRE_AFTER_ACCESS_MINUTES = 60L;

    private static final Cache<String, View> layoutJsonViewByCodeMap = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_SIZE)
            .expireAfterAccess(EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES)
            .build();

    @Autowired
    private SessionFactory sessionFactory;

    public View getLayoutJsonViewByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return layoutJsonViewByCodeMap.get(generateTenantKey(code), this::loadLayoutJsonViewByCode);
    }

    private View loadLayoutJsonViewByCode(String key) {
        String code = splitTenantKey(key);
        StatelessSession session = null;
        long startTime = System.currentTimeMillis();
        boolean isSuccess = true;
        try {
            session = sessionFactory.openStatelessSession();
            List<Map<String, Object>> viewMapList = session.createNativeQuery(layoutJsonViewSql())
                    .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                    .setParameter("code", code)
                    .getResultList();
            if (viewMapList.isEmpty()) {
                return null;
            }
            View view = toView(new CaseInsensitiveMap<>(viewMapList.get(0)));
            log.info(String.format("Caffeine >> load cache for method: loadLayoutJsonViewByCode, param: %s, spendTime: %dms, isSuccess: %b",
                    code, System.currentTimeMillis() - startTime, true));
            return view;
        } catch (Exception e) {
            isSuccess = false;
            log.error(e.getMessage(), e);
            throw new BAPException(e);
        } finally {
            if (session != null) {
                session.close();
            }
            if (!isSuccess) {
                log.info(String.format("Caffeine >> load cache for method: loadLayoutJsonViewByCode, param: %s, spendTime: %dms, isSuccess: %b",
                        code, System.currentTimeMillis() - startTime, false));
            }
        }
    }

    private static String layoutJsonViewSql() {
        return "select ev.code as code, ev.attachment_flag as attachment_flag, ev.title as title, "
                + "ev.is_shadow as is_shadow, ev.shadow_view_code as shadow_view_code, "
                + "ev.entity_code as entity_code, ev.type as type, ev.mobile as mobile, ev.url as url, "
                + "ev.has_attachment as has_attachment, ev.move_flag as move_flag, "
                + "ev.only_for_query as only_for_query, ev.show_type as show_type, "
                + "ev.deal_info_show as deal_info_show, ev.enable_simple_deal_info as enable_simple_deal_info, "
                + "ev.deal_info_group as deal_info_group, ev.retrial_flag as retrial_flag, "
                + "ev.script_code as script_code, ev.module_code as module_code, "
                + "case when rev.view_json is null then null else convert_from(lo_get(rev.view_json), 'UTF8') end as view_json, "
                + "case when rev.config is null then null else convert_from(lo_get(rev.config), 'UTF8') end as config "
                + "from runtime_view ev left join runtime_extra_view rev on ev.code = rev.code "
                + "where ev.code = :code and ev.valid = 1";
    }

    private static View toView(Map<String, Object> row) {
        View view = new View();
        view.setCode((String) row.get("code"));
        view.setTitle((String) row.get("title"));
        view.setIsShadow(toBoolean(row.get("is_shadow")));

        String shadowViewCode = (String) row.get("shadow_view_code");
        if (shadowViewCode != null) {
            View shadowView = new View();
            shadowView.setCode(shadowViewCode);
            view.setShadowView(shadowView);
        }

        String entityCode = (String) row.get("entity_code");
        if (entityCode != null) {
            Entity entity = new Entity();
            entity.setCode(entityCode);
            view.setEntity(entity);
        }

        view.setType(toViewType((String) row.get("type")));
        view.setMobile(toBoolean(row.get("mobile")));
        view.setUrl((String) row.get("url"));
        view.setHasAttachment(toBoolean(row.get("has_attachment")));
        view.setMoveFlag(toBoolean(row.get("move_flag")));
        view.setOnlyForQuery(toBoolean(row.get("only_for_query")));
        view.setShowType(toShowType((String) row.get("show_type")));
        view.setDealInfoShow(toBoolean(row.get("deal_info_show")));
        view.setDealInfoGroup((String) row.get("deal_info_group"));
        view.setEnableSimpleDealInfo(toBoolean(row.get("enable_simple_deal_info")));
        view.setRetrialFlag(toBoolean(row.get("retrial_flag")));
        view.setScriptCode((String) row.get("script_code"));
        view.setModuleCode((String) row.get("module_code"));
        view.setAttachmentFlag(toBoolean(row.get("attachment_flag")));

        String viewJson = (String) row.get("view_json");
        if (viewJson != null) {
            ExtraView extraView = new ExtraView();
            extraView.setCode(view.getCode());
            extraView.setViewJson(viewJson);
            extraView.setConfig((String) row.get("config"));
            view.setExtraView(extraView);
        }
        return view;
    }

    private static boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        return Boolean.parseBoolean(value.toString()) || "1".equals(value.toString());
    }

    private static ViewType toViewType(String viewType) {
        return viewType == null ? null : ViewType.valueOf(viewType);
    }

    private static ShowType toShowType(String showType) {
        return showType == null ? null : ShowType.valueOf(showType);
    }

    private static String generateTenantKey(String source) {
        return RpcContext.getContext().getTenantId() + "_" + source;
    }

    private static String splitTenantKey(String key) {
        String prefix = RpcContext.getContext().getTenantId() + "_";
        return key.startsWith(prefix) ? StringUtils.substringAfter(key, prefix) : key;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("BaseService layoutJson PostgreSQL OID compatibility patch loaded");
    }
}
