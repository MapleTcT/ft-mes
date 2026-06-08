/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.ec.entities.Module
 *  com.supcon.orchid.ec.entities.SuposApp
 *  com.supcon.orchid.ec.services.SuposAppService
 *  com.supcon.orchid.entityconf.services.ModuleService
 *  com.supcon.orchid.entityconf.services.OperateService
 *  com.supcon.orchid.entityconf.services.impl.ModuleDeployService
 *  com.supcon.orchid.foundation.entities.MenuInfo
 *  com.supcon.orchid.foundation.services.MenuInfoService
 *  com.supcon.orchid.i18n.InternationalResource
 *  com.supcon.orchid.services.BAPException
 *  com.supcon.orchid.services.Page
 *  com.supcon.orchid.utils.StringUtils
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.orchid.entityconf.controller;

import com.supcon.orchid.ec.entities.Module;
import com.supcon.orchid.ec.entities.SuposApp;
import com.supcon.orchid.ec.services.SuposAppService;
import com.supcon.orchid.entityconf.controller.AbstractBaseControllerSupport;
import com.supcon.orchid.entityconf.services.LogListenService;
import com.supcon.orchid.entityconf.services.ModuleService;
import com.supcon.orchid.entityconf.services.OperateService;
import com.supcon.orchid.entityconf.services.impl.ModuleDeployService;
import com.supcon.orchid.foundation.entities.MenuInfo;
import com.supcon.orchid.foundation.services.MenuInfoService;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.services.Page;
import com.supcon.orchid.utils.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/servicemanager/msModule/supos/app"})
public class SuposAppController
extends AbstractBaseControllerSupport {
    private static final Logger log = LoggerFactory.getLogger(SuposAppController.class);
    @Autowired
    private SuposAppService suposAppService;
    @Autowired
    private OperateService operateService;
    @Autowired
    private ModuleDeployService moduleDeployService;
    @Value(value="${supos.isSaasPackage:false}")
    private Boolean isSaasPackage;
    @Autowired
    private LogListenService logListenService;
    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private ModuleService moduleService;

    @RequestMapping(value={"/isSaasPackage"})
    public Boolean saasPackage() {
        return this.isSaasPackage;
    }

    @RequestMapping(value={"/install/{appCodes}"})
    public String install(@PathVariable(value="appCodes") String appCodes) {
        if (this.moduleDeployService.getSTATE() > 0) {
            return this.moduleDeployService.getDeploymentLog().getDeploymentId();
        }
        String[] appCodeArray = appCodes.split(",");
        ArrayList<SuposApp> appList = new ArrayList<SuposApp>(appCodeArray.length);
        for (String appCode : appCodeArray) {
            SuposApp suposApp = this.suposAppService.get(appCode);
            this.suposAppService.increaseAppVersion(suposApp);
            Optional.ofNullable(suposApp).orElseThrow(() -> new BAPException(InternationalResource.get((String)"ec.suposapp.notfind", (String)this.getUserLanguage(), (Object[])new Object[]{appCode})));
            appList.add(suposApp);
        }
        return this.suposAppService.installApp(appList);
    }

    @RequestMapping(value={"/build/{appCodes}"})
    public String createBuildTask(@PathVariable(value="appCodes") String appCodes) {
        if (this.moduleDeployService.getSTATE() > 0) {
            return this.moduleDeployService.getDeploymentLog().getDeploymentId();
        }
        String[] appCodeArray = appCodes.split(",");
        ArrayList<SuposApp> appList = new ArrayList<SuposApp>(appCodeArray.length);
        for (String appCode : appCodeArray) {
            SuposApp suposApp = this.suposAppService.get(appCode);
            Optional.ofNullable(suposApp).orElseThrow(() -> new BAPException(InternationalResource.get((String)"ec.suposapp.notfind", (String)this.getUserLanguage(), (Object[])new Object[]{appCode})));
            appList.add(suposApp);
        }
        return this.suposAppService.buildApp(appList);
    }

    @RequestMapping(value={"/install-task/{appCode}"})
    public String createInstallTask(@PathVariable(value="appCode") String appCode) {
        return this.suposAppService.createInstallTask(appCode);
    }

    @RequestMapping(value={"/install-kill/{task}"})
    public String installKill(@PathVariable(value="task") String task) {
        this.suposAppService.installKill(task);
        return "success";
    }

    @RequestMapping(value={"/logs/{taskId}"})
    public Map logs(@PathVariable(value="taskId") long taskId) {
        return this.suposAppService.installLogs(taskId);
    }

    @RequestMapping(value={"/logs/download"})
    public void downloadLogs(HttpServletRequest request, HttpServletResponse response) {
        this.operateService.downloadFile(request, response);
    }

    @RequestMapping(value={"/package/download"})
    public void downloadPackage(HttpServletRequest request, HttpServletResponse response) {
        this.operateService.downloadPackage(request, response);
    }

    @PostMapping(value={"/list"})
    public Page list(@RequestBody Page page) {
        List result = this.suposAppService.list();
        result.stream().forEach(supos -> {
            this.dealSuposAppModuleMap((SuposApp)supos);
            this.dealSuposAppMenusMap((SuposApp)supos);
        });
        page.setTotalCount((long)result.size());
        page.setResult(result);
        return page;
    }

    @RequestMapping(value={"/add"})
    public String add(@RequestBody SuposApp app) {
        this.suposAppService.add(app);
        return "SUCCESS";
    }

    @RequestMapping(value={"/merge"})
    public String merge(@RequestBody SuposApp app) {
        this.suposAppService.merge(app);
        return "SUCCESS";
    }

    @RequestMapping(value={"/{appCode}"})
    public SuposApp get(@PathVariable String appCode) {
        SuposApp suposApp = this.suposAppService.get(appCode);
        if (suposApp != null) {
            this.dealSuposAppModuleMap(suposApp);
            this.dealSuposAppMenusMap(suposApp);
        }
        return suposApp;
    }

    @RequestMapping(value={"/del/{appCodes}"})
    public String delete(@PathVariable String appCodes) {
        List<String> appCodeList = Arrays.asList(appCodes.split(","));
        this.suposAppService.deleteBatch(appCodeList);
        return "SUCCESS";
    }

    private void dealSuposAppModuleMap(SuposApp suposApp) {
        String modules = suposApp.getModules();
        if (StringUtils.isEmpty((String)modules)) {
            return;
        }
        List<String> moduleCodes = Arrays.asList(modules.split(","));
        HashMap map = new HashMap(moduleCodes.size());
        moduleCodes.stream().forEach(moduleCode -> {
            Module module = this.moduleService.getModule(moduleCode);
            String moduleName = InternationalResource.get((String)module.getName(), (String)this.getUserLanguage());
            map.put(module.getCode(), moduleName);
        });
        suposApp.setModulesMap(map);
    }

    private void dealSuposAppMenusMap(SuposApp suposApp) {
        String menus = suposApp.getMenus();
        if (StringUtils.isEmpty((String)menus)) {
            return;
        }
        List<String> menuCodes = Arrays.asList(menus.split(","));
        HashMap map = new HashMap(menuCodes.size());
        menuCodes.stream().forEach(menuCode -> {
            MenuInfo menuInfo = this.menuInfoService.getMenuInfo(menuCode);
            if (menuInfo != null) {
                String menuName = InternationalResource.get((String)menuInfo.getName(), (String)this.getUserLanguage());
                map.put(menuInfo.getCode(), menuName);
            }
        });
        suposApp.setMenusMap(map);
    }
}

