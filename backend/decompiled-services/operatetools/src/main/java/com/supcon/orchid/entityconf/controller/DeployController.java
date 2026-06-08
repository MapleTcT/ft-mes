/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.api.annotation.CustomRequest
 *  com.supcon.orchid.ec.entities.Module
 *  com.supcon.orchid.ec.services.EcDataSynchronizeService
 *  com.supcon.orchid.entityconf.deployer.ModuleDeploymentManager
 *  com.supcon.orchid.entityconf.deployer.SingleDeployService
 *  com.supcon.orchid.entityconf.entities.DeploymentTask
 *  com.supcon.orchid.entityconf.entities.HeartbeatTask
 *  com.supcon.orchid.entityconf.services.BAPGenerateService
 *  com.supcon.orchid.entityconf.services.ModuleService
 *  com.supcon.orchid.entityconf.services.MsModuleService
 *  com.supcon.orchid.entityconf.services.OperateService
 *  com.supcon.orchid.entityconf.services.impl.ModuleDeployService
 *  com.supcon.orchid.entityconf.services.impl.ModuleStartTaskService
 *  com.supcon.orchid.i18n.InternationalResource
 *  com.supcon.orchid.services.BAPException
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Controller
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.ResponseBody
 */
package com.supcon.orchid.entityconf.controller;

import com.supcon.orchid.api.annotation.CustomRequest;
import com.supcon.orchid.ec.entities.Module;
import com.supcon.orchid.ec.services.EcDataSynchronizeService;
import com.supcon.orchid.entityconf.controller.AbstractBaseControllerSupport;
import com.supcon.orchid.entityconf.deployer.ModuleDeploymentManager;
import com.supcon.orchid.entityconf.deployer.SingleDeployService;
import com.supcon.orchid.entityconf.entities.DeploymentTask;
import com.supcon.orchid.entityconf.entities.HeartbeatTask;
import com.supcon.orchid.entityconf.services.BAPGenerateService;
import com.supcon.orchid.entityconf.services.ModuleService;
import com.supcon.orchid.entityconf.services.MsModuleService;
import com.supcon.orchid.entityconf.services.OperateService;
import com.supcon.orchid.entityconf.services.impl.ModuleDeployService;
import com.supcon.orchid.entityconf.services.impl.ModuleStartTaskService;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.services.BAPException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value={"/servicemanager/msModule"})
public class DeployController
extends AbstractBaseControllerSupport {
    private static final Logger logger = LoggerFactory.getLogger(DeployController.class);
    @Autowired
    private ModuleDeploymentManager manager;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    SingleDeployService singleDeployService;
    @Autowired
    private BAPGenerateService bapGenerateService;
    @Autowired
    private EcDataSynchronizeService ecDataSynchronizeService;
    @Autowired
    private MsModuleService msModuleService;
    @Autowired
    private OperateService operateServiceImpl;
    @Autowired
    private ModuleDeployService moduleDeployService;
    @Value(value="${logPath:''}")
    private String codeLogPath;
    @Autowired
    private ModuleStartTaskService moduleStartTaskService;
    private String jsonResult;

    @RequestMapping(value={"/generate/taskState"})
    @ResponseBody
    public Boolean taskState() {
        return this.moduleDeployService.getSTATE() > 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @RequestMapping(value={"/generate/addTask"})
    @ResponseBody
    public Object addTask(String taskList) {
        CopyOnWriteArrayList currentTasks = this.manager.getCurrentTasks();
        if (null == currentTasks || currentTasks.size() == 0) {
            DeployController deployController = this;
            synchronized (deployController) {
                currentTasks = this.manager.getCurrentTasks();
                if (null == currentTasks || currentTasks.size() == 0) {
                    List deploymentTasks = this.manager.batchAddtask(taskList);
                    ArrayList<DeploymentTask> cloneDeploymentTasks = new ArrayList<DeploymentTask>();
                    ArrayList<String> moduleCodes = new ArrayList<String>();
                    for (int i = 0; i < deploymentTasks.size(); ++i) {
                        DeploymentTask deploymentTask = (DeploymentTask)deploymentTasks.get(i);
                        if (i == 0) {
                            deploymentTask.setDeployUser(null != this.getCurrentUser() ? this.getCurrentUser().getName() : "system");
                        }
                        deploymentTask.setLocale(this.getUserLanguage());
                        DeploymentTask task = this.manager.cloneDeploymentTask(deploymentTask);
                        if (null == task) continue;
                        cloneDeploymentTasks.add(task);
                        moduleCodes.add(task.getModule().getCode());
                    }
                    this.singleDeployService.doGenerateAll(cloneDeploymentTasks);
                }
            }
        }
        return currentTasks;
    }

    @CustomRequest
    @RequestMapping(value={"/generate"})
    @ResponseBody
    public Object geneate(@RequestParam(value="moduleCode", required=true) String moduleCode) {
        try {
            ArrayList<DeploymentTask> cloneDeploymentTasks = new ArrayList<DeploymentTask>();
            DeploymentTask deploymentTask = new DeploymentTask(moduleCode, 16);
            DeploymentTask task = this.manager.cloneDeploymentTask(deploymentTask);
            cloneDeploymentTasks.add(task);
            this.singleDeployService.doGenerateAll(cloneDeploymentTasks);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
            return InternationalResource.get((String)"ec.msModule.module.generate.error", (String)this.getUserLanguage()) + " " + this.codeLogPath + "/servicemanager.log" + InternationalResource.get((String)"foundation.base.audit", (String)this.getUserLanguage());
        }
        return "success";
    }

    @CustomRequest
    @RequestMapping(value={"/projgenerate"})
    @ResponseBody
    public Object projGeneate(@RequestParam(value="moduleCode", required=true) String moduleCode) {
        try {
            Module module = this.moduleService.getModule(moduleCode);
            this.bapGenerateService.generateProjViews(module, true);
            this.ecDataSynchronizeService.synchronizeEcDataFromProjToRumtime(module == null ? null : module.getCode());
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
            return InternationalResource.get((String)"ec.msModule.module.generate.error", (String)this.getUserLanguage()) + " " + this.codeLogPath + "/servicemanager.log" + InternationalResource.get((String)"foundation.base.audit", (String)this.getUserLanguage());
        }
        return "success";
    }

    @RequestMapping(value={"/generate/generateBootStartProject"})
    @ResponseBody
    public String generateBootStartProject(String codes) {
        String[] codeArr = codes.split(",");
        this.singleDeployService.doGenerateBootStartProject(codeArr);
        return "success";
    }

    @CustomRequest
    @RequestMapping(value={"/buidPackage"})
    @ResponseBody
    public String buidPackage(@RequestParam(value="moduleCode", required=true) String moduleCode) {
        Module module = this.msModuleService.getModule(moduleCode);
        try {
            this.bapGenerateService.buildPackage(module, null);
            if (this.operateServiceImpl.moduleJarVaild(moduleCode).booleanValue()) {
                throw new BAPException(InternationalResource.get((String)"ec.msModule.module.errorPackage", (String)this.getUserLanguage()) + this.codeLogPath + "/servicemanager.log" + InternationalResource.get((String)"foundation.base.audit", (String)this.getUserLanguage()));
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
            return InternationalResource.get((String)"ec.msModule.module.errorPackage", (String)this.getUserLanguage()) + " " + this.codeLogPath + "/servicemanager.log" + InternationalResource.get((String)"foundation.base.audit", (String)this.getUserLanguage());
        }
        return "success";
    }

    @CustomRequest
    @RequestMapping(value={"/ecDataSynchronize"})
    @ResponseBody
    public String ecDataSynchronize(@RequestParam(value="moduleCode", required=true) String moduleCode) {
        try {
            this.ecDataSynchronizeService.synchronizeEcDataFromDevToRumtime(moduleCode);
            this.ecDataSynchronizeService.clearCache();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
        }
        return "success";
    }

    @RequestMapping(value={"/deploy/batchTask"})
    public String getBatchTask() {
        CopyOnWriteArrayList currentTasks = this.manager.getCurrentTasks();
        this.jsonResult = this.getDeployResult(currentTasks);
        return "success";
    }

    private String getDeployResult(CopyOnWriteArrayList<DeploymentTask> currentTasks) {
        if (null != currentTasks && currentTasks.size() > 0) {
            DeploymentTask batchTask = currentTasks.get(0);
            this.jsonResult = "[" + batchTask.toJsonString() + "]";
        } else {
            this.jsonResult = "[]";
        }
        return this.jsonResult;
    }

    @PostMapping(value={"/deploy/heartbeat"})
    @ResponseBody
    public void listenDeploy(@RequestBody HeartbeatTask heartbeatTask) {
        this.moduleStartTaskService.listenDeploy(heartbeatTask);
    }
}

