package com.supcon.supfusion.rbac.service.upgrade;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.dbp.util.DatabaseInitializer;
import com.supcon.supfusion.framework.scaffold.dbp.util.DatabaseInitializerEmitter;
import com.supcon.supfusion.rbac.dao.AppRefMapper;
import com.supcon.supfusion.rbac.dao.MenuAppDesignerRelMapper;
import com.supcon.supfusion.rbac.dao.UpgradeFlagMapper;
import com.supcon.supfusion.rbac.dao.field.AppRefField;
import com.supcon.supfusion.rbac.dao.field.MenuAppDesignerRelField;
import com.supcon.supfusion.rbac.dao.po.AppRefPO;
import com.supcon.supfusion.rbac.dao.po.MenuAppDesignerRelPO;
import com.supcon.supfusion.rbac.dao.po.UpgradeFlagPO;
import com.supcon.supfusion.rbac.service.IMenuAppDesignerRelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DataUpgrade implements DatabaseInitializer, InitializingBean {
    @Autowired
    UpgradeFlagMapper upgradeFlagMapper;
    @Autowired
    AppRefMapper appRefMapper;
    @Autowired
    IMenuAppDesignerRelService menuAppDesignerRelService;
    @Autowired
    MenuAppDesignerRelMapper menuAppDesignerRelMapper;

    @Override
    public void init(TenantInfo tenantInfo) {
        final String tenantId = tenantInfo.getId();
        log.info("this tenantId start upgrade......{}", tenantId);
        Boolean hasUpgrade = isUpgraded(tenantId);
        if (hasUpgrade) {
            log.info("current tenant has been upgrade......exit!{}", tenantId);
            return;
        }
        Thread main = new Thread(() -> {
            RpcContext.getContext().setTenantId(tenantId);
            Set<String> appIds = getAppIdList();
            if (!CollectionUtils.isEmpty(appIds)) {
                final CountDownLatch latch = new CountDownLatch(appIds.size());
                final List<MenuAppDesignerRelPO> pos = new CopyOnWriteArrayList<>();
                appIds.forEach(appId -> getAppTreeFromOODM(tenantId, appId, menuAppDesignerRelPOS -> pos.addAll(menuAppDesignerRelPOS), latch));
                try {
                    latch.await();
                    insertData(pos, tenantId);
                } catch (InterruptedException e) {
                }
            }
        });
        main.setDaemon(true);
        main.setName("upgrade-" + tenantId);
        main.start();
    }

    private void getAppTreeFromOODM(String tenantId, String appId, GetAppTreeCallback callback, CountDownLatch latch) {
        GetAppTreeFromOODMThread thread = new GetAppTreeFromOODMThread(callback, tenantId, appId, latch);
        thread.start();
    }

    private boolean isUpgraded(String tenantId) {
        RpcContext.getContext().setTenantId(tenantId);
        log.info("get upgradeFlag, current tenantId={}",tenantId);
        Integer count = upgradeFlagMapper.getUpgradeFlag();
        if (count > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int order() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        DatabaseInitializerEmitter.add(this);
    }

    private Set<String> getAppIdList() {
        // 获取全部appidlist
        QueryWrapper<AppRefPO> appRefPOQueryWrapper = new QueryWrapper<>();
        appRefPOQueryWrapper.like(AppRefField.appId, "App_");
        List<AppRefPO> appRefPOList = appRefMapper.selectList(appRefPOQueryWrapper);
        Set<String> appIdList = appRefPOList.stream().map(AppRefPO::getAppId).collect(Collectors.toSet());
        return appIdList;
    }

    private void insertData(List<MenuAppDesignerRelPO> menuAppDesignerPOList, String tenantId) {
        try {
            // do save
            RpcContext.getContext().setTenantId(tenantId);
            log.info("insertData,current tenantId={}",tenantId);
            List<MenuAppDesignerRelPO> collect = menuAppDesignerPOList.stream().map(appDesignerPO -> {
                QueryWrapper<MenuAppDesignerRelPO> objectQueryWrapper = new QueryWrapper<>();
                objectQueryWrapper.eq(MenuAppDesignerRelField.appId, appDesignerPO.getAppId());
                objectQueryWrapper.eq(MenuAppDesignerRelField.code, appDesignerPO.getCode());
                MenuAppDesignerRelPO one = menuAppDesignerRelService.getOne(objectQueryWrapper);
                if (null == one) {
                    return appDesignerPO;
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            log.info("save data to rbac_menu_app_designer for tenant:{}", tenantId);
            menuAppDesignerRelService.saveBatch(collect);

            // 记录升级状态
            UpgradeFlagPO upgradeFlagPO = new UpgradeFlagPO();
            upgradeFlagPO.setId(IDGenerator.newInstance().generate().longValue());
            upgradeFlagPO.setApplicationName("rbac-upgrade");
            upgradeFlagPO.setScriptFileName("rbac");
            upgradeFlagPO.setCurrentVersion("3.0");

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            upgradeFlagPO.setCreateTime(df.format(new Date()));
            log.info("insert rbac-upgrade flag,current tenantId={}",tenantId);
            upgradeFlagMapper.insert(upgradeFlagPO);
        } catch (Exception e) {
            log.error("failure to save app menus, tenant=" + tenantId, e);
        }
    }
}