/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  com.alibaba.fastjson.JSONArray
 *  com.supcon.orchid.ProjectFlagHolder
 *  com.supcon.orchid.ec.entities.MsModule
 *  com.supcon.orchid.ec.entities.MsModuleIpAdress
 *  com.supcon.orchid.entityconf.entities.DeploymentMsTask
 *  com.supcon.orchid.entityconf.entities.DeploymentMsTask$TaskStatus
 *  com.supcon.orchid.entityconf.entities.ServiceStartTask
 *  com.supcon.orchid.entityconf.services.MsModuleService
 *  com.supcon.orchid.entityconf.services.OperateService
 *  com.supcon.orchid.entityconf.services.StartProgressService
 *  com.supcon.orchid.entityconf.services.StartProgressService$Stage
 *  com.supcon.orchid.entityconf.services.impl.ModuleDeployService
 *  com.supcon.orchid.i18n.InternationalResource
 *  com.supcon.orchid.id.SnowFlakeIdWorker
 *  com.supcon.orchid.services.Page
 *  com.supcon.orchid.utils.EcUtils
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.slf4j.MDC
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.supcon.orchid.entityconf.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supcon.orchid.ProjectFlagHolder;
import com.supcon.orchid.ec.entities.MsModule;
import com.supcon.orchid.ec.entities.MsModuleIpAdress;
import com.supcon.orchid.entityconf.controller.AbstractBaseControllerSupport;
import com.supcon.orchid.entityconf.entities.DeploymentMsTask;
import com.supcon.orchid.entityconf.entities.ServiceStartTask;
import com.supcon.orchid.entityconf.services.LogListenService;
import com.supcon.orchid.entityconf.services.MsModuleService;
import com.supcon.orchid.entityconf.services.OperateService;
import com.supcon.orchid.entityconf.services.StartProgressService;
import com.supcon.orchid.entityconf.services.impl.ModuleDeployService;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.id.SnowFlakeIdWorker;
import com.supcon.orchid.services.Page;
import com.supcon.orchid.utils.EcUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/servicemanager/msModule"})
public class OperateToolController
extends AbstractBaseControllerSupport {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Logger publishLogger = LoggerFactory.getLogger((String)"bap.ec.generator.publishLog");
    @Autowired
    private MsModuleService msModuleService;
    @Autowired
    private LogListenService logListenService;
    @Autowired
    private OperateService operateService;
    private DeploymentMsTask deploymentMsTask;
    @Autowired
    private StartProgressService startProgressService;
    @Autowired
    private ModuleDeployService moduleDeployService;

    @PostMapping(value={"/save"})
    public Map<String, Object> save(@RequestBody Map<String, Object> map) {
        Map<String, Object> jsonMap = new HashMap<String, Object>();
        try {
            String moduleSelectsIds = (String)map.get("moduleSelectsIds") == null ? "" : map.get("moduleSelectsIds").toString();
            String ipAdress = JSONArray.toJSONString((Object)map.get("adressResult"));
            ArrayList<Map<String, Object>> msList = new ArrayList<Map<String, Object>>();
            msList.add(map);
            String msJson = JSONArray.toJSONString(msList);
            List list = JSON.parseArray((String)msJson, MsModule.class);
            List adressList = JSON.parseArray((String)ipAdress, MsModuleIpAdress.class);
            jsonMap = this.msModuleService.saveMsModule((MsModule)list.get(0), moduleSelectsIds, adressList, this.deploymentMsTask);
        }
        catch (Exception e) {
            e.printStackTrace();
            jsonMap.put("success", false);
            jsonMap.put("message", e.getMessage());
        }
        return jsonMap;
    }

    @GetMapping(value={"/delete"})
    public Map<String, Object> delete(HttpServletRequest request) {
        String code = request.getParameter("code");
        String ipAdress = request.getParameter("ipAdress");
        Map jsonMap = this.msModuleService.deleteMsModule(code, ipAdress);
        return jsonMap;
    }

    @PostMapping(value={"/msQueryList"})
    public Page msQueryList(@RequestBody Page page) {
        ProjectFlagHolder.getInstance().getProjFlag().set(false);
        return this.msModuleService.msQueryList(page);
    }

    @GetMapping(value={"/modRel"})
    public List<Map> getModuleRelation(String msCode, String type) {
        return this.msModuleService.getMsModuleRelation(msCode, type);
    }

    @PostMapping(value={"/modRelNew"})
    public Page getModuleRelationNew(@RequestBody Page page) {
        return this.msModuleService.getModuleRelationNew(page);
    }

    @GetMapping(value={"/edit"})
    public Map<String, Object> edit(HttpServletRequest request) {
        String code = request.getParameter("code");
        return this.msModuleService.edit(code);
    }

    @PostMapping(value={"/excute"})
    public Boolean execute2(@RequestBody List<ServiceStartTask> serviceStartTasks) {
        this.logger.info("Current deploy state:" + this.moduleDeployService.getSTATE());
        if (this.moduleDeployService.getSTATE() < 1) {
            for (ServiceStartTask serviceStartTask : serviceStartTasks) {
                serviceStartTask.setDeployType(this.transferDeployType(serviceStartTask.getDeployType()));
            }
            this.operateService.startTask(serviceStartTasks);
        }
        return Boolean.TRUE;
    }

    private short transferDeployType(short deployType) {
        if (deployType == 0) {
            return 8;
        }
        if (deployType == 1) {
            return 78;
        }
        if (deployType == 2) {
            return 118;
        }
        return 0;
    }

    @PostMapping(value={"/excute3"})
    public Map<String, Object> excute(@RequestBody List<Map<String, Object>> list) {
        if (null == this.deploymentMsTask || this.deploymentMsTask.getStatus().equals((Object)DeploymentMsTask.TaskStatus.FINISHED) || this.deploymentMsTask.getStatus().equals((Object)DeploymentMsTask.TaskStatus.FAILED)) {
            this.startProgressService.init();
            this.startProgressService.addProgress(0.2, StartProgressService.Stage.Prepare);
            this.deploymentMsTask = new DeploymentMsTask();
            this.deploymentMsTask.setId(Long.valueOf(SnowFlakeIdWorker.getInstance().nextId()));
            this.deploymentMsTask.setDeployUser(this.getCurrentUser().getName());
            this.deploymentMsTask.setStatus(DeploymentMsTask.TaskStatus.RUNNING);
            this.deploymentMsTask.setMsModuleList(list);
            this.deploymentMsTask.setLocale(this.getUserLanguage());
            String deploymentMsTaskId = this.deploymentMsTask.getId().toString();
            EcUtils.deployTask.put("bapEcTask", deploymentMsTaskId);
            MDC.put((String)"bapEcTask", (String)deploymentMsTaskId);
            String msModuleName = "";
            for (int i = 0; i < list.size(); ++i) {
                msModuleName = i == 0 ? (list.get(i).get("name") == null ? "" : list.get(i).get("name").toString()) : msModuleName + "\u3001" + (list.get(i).get("name") == null ? "" : list.get(i).get("name").toString());
            }
            this.deploymentMsTask.setModuleName(msModuleName);
            publishLogger.info(InternationalResource.get((String)"ec.msModule.serviceStartInfo", (String)this.getUserLanguage(), (Object[])new Object[]{msModuleName, list.size()}));
            this.startProgressService.addProgress(0.2, StartProgressService.Stage.Prepare);
            this.operateService.startCommandAsync(list, this.deploymentMsTask);
        }
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("success", true);
        return map;
    }

    @PostMapping(value={"/stop"})
    public Map<String, Object> stop(@RequestBody List<Map<String, Object>> list) {
        return this.operateService.stopCommand(list);
    }

    @GetMapping(value={"/codeVaild"})
    public Map<String, Object> codeVaild(HttpServletRequest request) {
        String moduleCode = request.getParameter("moduleCode");
        String msModuleCode = request.getParameter("msModuleCode");
        Map map = this.msModuleService.codeVaild(moduleCode, msModuleCode);
        return map;
    }

    @PostMapping(value={"/getProgressiveLog"})
    public Map<String, Object> getProgressiveLog() {
        return this.logListenService.getProgressiveLog();
    }

    @GetMapping(value={"/downloadFile"})
    public void downloadFile(HttpServletRequest request, HttpServletResponse response) {
        this.operateService.downloadFile(request, response);
    }

    @PostMapping(value={"/moduleRelValid"})
    public Map<String, Object> moduleRelValid(@RequestBody List<Map<String, Object>> list) {
        return this.operateService.moduleRelValid(list, this.getUserLanguage(), this.deploymentMsTask);
    }
}

