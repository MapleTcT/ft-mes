/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.supcon.orchid.ec.entities.SuposApp
 *  com.supcon.orchid.ec.services.SuposAppService
 *  com.supcon.orchid.entityconf.daos.SuposAppDao
 *  com.supcon.orchid.entityconf.entities.DeploymentMsTask
 *  com.supcon.orchid.entityconf.entities.DeploymentMsTask$TaskStatus
 *  com.supcon.orchid.entityconf.entities.ServiceStartTask
 *  com.supcon.orchid.entityconf.services.BAPGenerateService
 *  com.supcon.orchid.entityconf.services.MsModuleService
 *  com.supcon.orchid.entityconf.services.OperateService
 *  com.supcon.orchid.entityconf.services.StartProgressService
 *  com.supcon.orchid.entityconf.services.impl.ModuleDeployService
 *  com.supcon.orchid.foundation.utils.StringUtil
 *  com.supcon.orchid.i18n.InternationalResource
 *  com.supcon.orchid.services.BAPException
 *  com.supcon.orchid.services.BaseServiceImpl
 *  com.supcon.orchid.utils.EcUtils
 *  com.supcon.orchid.utils.StringUtils
 *  com.supcon.supfusion.framework.cloud.common.context.RpcContext
 *  com.supcon.supfusion.framework.cloud.common.result.ListResult
 *  com.supcon.supfusion.framework.cloud.common.result.Result
 *  com.supcon.supfusion.installer.api.AppInstallerService
 *  com.supcon.supfusion.installer.api.dto.CreateTaskDTO
 *  com.supcon.supfusion.installer.api.dto.CreateTaskDTO$InstallParamDTO
 *  com.supcon.supfusion.installer.api.dto.QueryAppDTO
 *  com.supcon.supfusion.installer.api.dto.TaskAndLogDTO
 *  com.supcon.supfusion.installer.api.dto.TaskAndLogDTO$TaskLogDTO
 *  com.supcon.supfusion.installer.common.constants.AppType
 *  com.supcon.supfusion.installer.common.constants.TaskState
 *  com.supcon.supfusion.installer.common.constants.TaskType
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.slf4j.MDC
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.context.annotation.Lazy
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.util.DigestUtils
 */
package com.supcon.orchid.entityconf.service.imps;

import com.supcon.orchid.ec.entities.SuposApp;
import com.supcon.orchid.ec.services.SuposAppService;
import com.supcon.orchid.entityconf.daos.SuposAppDao;
import com.supcon.orchid.entityconf.entities.DeploymentMsTask;
import com.supcon.orchid.entityconf.entities.ServiceStartTask;
import com.supcon.orchid.entityconf.services.BAPGenerateService;
import com.supcon.orchid.entityconf.services.MsModuleService;
import com.supcon.orchid.entityconf.services.OperateService;
import com.supcon.orchid.entityconf.services.StartProgressService;
import com.supcon.orchid.entityconf.services.impl.ModuleDeployService;
import com.supcon.orchid.foundation.utils.StringUtil;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.services.BaseServiceImpl;
import com.supcon.orchid.utils.EcUtils;
import com.supcon.orchid.utils.StringUtils;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.installer.api.AppInstallerService;
import com.supcon.supfusion.installer.api.dto.CreateTaskDTO;
import com.supcon.supfusion.installer.api.dto.QueryAppDTO;
import com.supcon.supfusion.installer.api.dto.TaskAndLogDTO;
import com.supcon.supfusion.installer.common.constants.AppType;
import com.supcon.supfusion.installer.common.constants.TaskState;
import com.supcon.supfusion.installer.common.constants.TaskType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

@Service
public class SuposAppServiceImpl
extends BaseServiceImpl<SuposApp>
implements SuposAppService {
    private static final Logger logger = LoggerFactory.getLogger(SuposAppServiceImpl.class);
    private static final Logger publishLogger = LoggerFactory.getLogger((String)"bap.ec.generator.publishLog");
    @Autowired
    private AppInstallerService appInstallerService;
    @Autowired
    private BAPGenerateService bapGenerateService;
    private OperateService operateService;
    @Autowired
    private MsModuleService msModuleService;
    @Autowired
    private SuposAppDao suposAppDao;
    @Autowired
    private ModuleDeployService moduleDeployService;
    @Autowired
    private StartProgressService startProgressService;
    @Value(value="${entityconf.generatePath}")
    private String tmpPath;
    private final DeploymentMsTask deploymentMsTask = new DeploymentMsTask();
    @Value(value="${supos.app.path}")
    private String appPath;
    @Value(value="${logPath:''}")
    private String logPath;
    @Value(value="${entityPath:''}")
    private String entityPath;
    @Value(value="${bap.workspace:''}")
    private String workspace;
    @Value(value="${logPath:''}")
    private String codeLogPath;

    @Autowired
    @Lazy
    public void setOperateService(OperateService operateService) {
        this.operateService = operateService;
    }

    public Long taskId() {
        if (this.deploymentMsTask.getStatus() == DeploymentMsTask.TaskStatus.RUNNING) {
            return this.deploymentMsTask.getId();
        }
        return null;
    }

    public void installSupOsApp(SuposApp app) {
        RpcContext.getContext().setLanguage(ModuleDeployService.CURRENT_LANGUAGE);
        String supOsTask = this.createInstallTask(app.getCode());
        if (supOsTask == null) {
            this.moduleDeployService.showFailureDeployLog((Exception)new BAPException("\u521b\u5efa\u5b89\u88c5\u4efb\u52a1\u5931\u8d25"));
        }
        boolean isFinish = false;
        long timeNum = 0L;
        while (!isFinish) {
            Result taskLog = null;
            try {
                taskLog = this.appInstallerService.getTaskLog(supOsTask, null);
            }
            catch (Exception e) {
                this.log.error(e.getMessage(), (Throwable)e);
                break;
            }
            TaskAndLogDTO data = (TaskAndLogDTO)taskLog.getData();
            if (data != null) {
                this.log.info("install task progress: " + data.getProgress());
                if (100 == data.getProgress() || TaskState.ENDING == data.getTaskState()) {
                    this.log.info("install task end log: " + data.toString());
                    if (data.getResult().booleanValue()) {
                        isFinish = true;
                        break;
                    }
                    this.moduleDeployService.showDeployLog(InternationalResource.get((String)"ec.suposapp.install.failed.suposlogs"), Boolean.valueOf(false), new Object[0]);
                    List logs = data.getLogs();
                    if (logs.isEmpty()) break;
                    for (int i = 0; i < logs.size(); ++i) {
                        TaskAndLogDTO.TaskLogDTO taskLogDTO = (TaskAndLogDTO.TaskLogDTO)logs.get(i);
                        String content = taskLogDTO.getContent();
                        if (i == logs.size() - 1) {
                            this.moduleDeployService.showFailureDeployLog((Exception)new RuntimeException(content));
                            continue;
                        }
                        publishLogger.error(content);
                    }
                    break;
                }
            }
            if (++timeNum > 100L) break;
            try {
                Thread.sleep(3000L);
            }
            catch (InterruptedException e) {
                break;
            }
        }
        if (isFinish) {
            this.taskSuccess(supOsTask);
        } else {
            this.taskFailed(supOsTask);
        }
        this.registryMenus(app);
    }

    public String installApp(List<SuposApp> appList) {
        List emptyList = Collections.emptyList();
        List serviceStartTasks = appList.stream().map(app -> {
            ServiceStartTask serviceStartTask = new ServiceStartTask();
            serviceStartTask.setCode(app.getCode());
            boolean isMaster = app.getAppType() == 0;
            serviceStartTask.setDeployType(isMaster ? (short)118 : (short)48);
            serviceStartTask.setModuleSelectsIds(app.getModules());
            serviceStartTask.setName(app.getName());
            serviceStartTask.setAdressResult(emptyList);
            serviceStartTask.setSuposApp(app);
            return serviceStartTask;
        }).collect(Collectors.toList());
        this.operateService.startTask(serviceStartTasks);
        return this.moduleDeployService.getDeploymentLog().getDeploymentId();
    }

    public String createInstallTask(String appCode) {
        CreateTaskDTO taskDTO = this.createTaskDTO(appCode);
        this.log.info("Understand the installation task of supos app code {}, appId{}", (Object)appCode, (Object)taskDTO.getAppId());
        this.appInstallerService.clearInstallingTaskByApp(taskDTO.getAppId());
        this.log.info("app install params: " + taskDTO.toString());
        Result result = this.appInstallerService.createTask(taskDTO);
        this.log.info("app install result: " + result.toString());
        return (String)result.getData();
    }

    private void registryMenus(SuposApp app) {
        String[] split;
        if (app == null || StringUtils.isEmpty((String)app.getMenus())) {
            return;
        }
        String menus = app.getMenus();
        if (StringUtils.isEmpty((String)menus)) {
            return;
        }
        for (String s : split = menus.split(",")) {
            this.suposAppDao.updateMenuStatus(s);
        }
        String appId = this.getAppId(app.getCode());
        this.suposAppDao.addMenuAppRef(menus, appId);
    }

    public void registryProjMenus(SuposApp app, String menus) {
        String[] split;
        if (app == null || StringUtils.isEmpty((String)app.getMenus())) {
            return;
        }
        if (StringUtils.isEmpty((String)menus)) {
            return;
        }
        for (String s : split = menus.split(",")) {
            this.suposAppDao.updateMenuStatus(s);
        }
        String appId = this.getAppId(app.getCode());
        this.suposAppDao.addProjMenuAppRef(menus, appId);
    }

    public String buildApp(List<SuposApp> suposApps) {
        List emptyList = Collections.emptyList();
        List serviceStartTasks = suposApps.stream().map(app -> {
            ServiceStartTask serviceStartTask = new ServiceStartTask();
            serviceStartTask.setCode(app.getCode());
            boolean isMaster = app.getAppType() == 0;
            serviceStartTask.setDeployType(isMaster ? (short)150 : (short)144);
            serviceStartTask.setModuleSelectsIds(app.getModules());
            serviceStartTask.setName(app.getName());
            serviceStartTask.setAdressResult(emptyList);
            serviceStartTask.setSuposApp(app);
            return serviceStartTask;
        }).collect(Collectors.toList());
        this.operateService.startTask(serviceStartTasks);
        return this.moduleDeployService.getDeploymentLog().getDeploymentId();
    }

    private CreateTaskDTO createTaskDTO(String appCode) {
        CreateTaskDTO createTaskDTO = new CreateTaskDTO();
        createTaskDTO.setAppId(this.getAppId(appCode));
        createTaskDTO.setAppType(AppType.ENTERPRISE);
        createTaskDTO.setTaskType(TaskType.INSTALL);
        createTaskDTO.setBatch(Boolean.valueOf(true));
        CreateTaskDTO.InstallParamDTO installTaskParam = new CreateTaskDTO.InstallParamDTO();
        String packageUrl = "file://" + this.appPath + File.separator + this.moduleDeployService.getDeploymentLog().getDeploymentId() + File.separator + appCode + ".zip";
        this.log.info(packageUrl);
        installTaskParam.setPackageUrl(packageUrl);
        installTaskParam.setSkipConfig(Boolean.valueOf(true));
        createTaskDTO.setInstallTaskParam(installTaskParam);
        return createTaskDTO;
    }

    private String getAppId(String appCode) {
        return DigestUtils.md5DigestAsHex((byte[])("supcon-" + appCode.toLowerCase()).getBytes());
    }

    private Boolean buildAppJar(List<SuposApp> apps) {
        ArrayList list = new ArrayList();
        apps.stream().forEach(app -> {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("code", app.getCode());
            map.put("deployType", 2);
            map.put("moduleSelectsIds", app.getModules());
            map.put("name", app.getName());
            list.add(map);
        });
        Map jsonMap = this.operateService.startCommand(list, this.deploymentMsTask);
        return Boolean.parseBoolean(jsonMap.get("success").toString());
    }

    private void taskSuccess(String task) {
        MDC.put((String)"bapEcTask", (String)String.valueOf(this.deploymentMsTask.getId()));
        publishLogger.info(InternationalResource.get((String)"ec.suposapp.install.success"));
        this.deploymentMsTask.setStatus(DeploymentMsTask.TaskStatus.FINISHED);
        publishLogger.info("TASK_SUCCESS");
    }

    public void taskFailed(String task) {
        MDC.put((String)"bapEcTask", (String)String.valueOf(this.deploymentMsTask.getId()));
        this.deploymentMsTask.setStatus(DeploymentMsTask.TaskStatus.FAILED);
        publishLogger.info("TASK_FAILED");
    }

    public String increaseAppVersion(SuposApp suposApp) {
        Integer integer = 100;
        String appVersion = suposApp.getAppVersion();
        try {
            String replace = appVersion.trim().replace(".", "");
            integer = Integer.valueOf(replace) + 1;
        }
        catch (Exception e) {
            new BAPException(InternationalResource.get((String)"ec.suposapp.version.error", (String)this.getCurrentLanguage()));
        }
        int z = integer % 10;
        int y = integer / 10 % 10;
        int x = integer / 100;
        String newVersion = x + "." + y + "." + z;
        this.suposAppDao.updataAppVersion(newVersion, appVersion, suposApp.getCode());
        suposApp.setAppVersion(newVersion);
        return newVersion;
    }

    public void installKill(String task) {
        this.taskFailed(task);
    }

    public Map<String, Object> installLogs(long taskId) {
        HashMap<String, Object> resultMap = new HashMap<String, Object>();
        String path = this.logPath + File.separator + "publish" + File.separator + taskId + ".log";
        File file = new File(path);
        if (!file.exists()) {
            resultMap.put("point", 0);
            return resultMap;
        }
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path));){
            String s;
            while ((s = reader.readLine()) != null) {
                if (s.contains("FAILURE_") || s.contains("TASK_FAILED")) {
                    resultMap.put("success", false);
                    resultMap.put("point", 100);
                    result.append(System.lineSeparator() + "<p style=\"color:red\">" + s + "</p><br>");
                    break;
                }
                if (s.contains("TASK_SUCCESS")) {
                    resultMap.put("success", true);
                    resultMap.put("point", 100);
                    break;
                }
                resultMap.put("point", 0);
                result.append(System.lineSeparator() + s + "<br>");
            }
            if (this.startProgressService.getFinishFlag().booleanValue()) {
                resultMap.put("success", true);
                resultMap.put("point", 100);
            }
            resultMap.put("logs", result.toString());
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
        }
        return resultMap;
    }

    private void buildJavaOpts(SuposApp app) {
        if (app.getAppType() == 0) {
            StringBuffer jvmParams = new StringBuffer();
            jvmParams.append(" -Xmx").append(app.getMemory()).append("m").append(" -Xms").append(app.getMemory()).append("m").append(" -Xmn").append(Float.valueOf(app.getMemory() / 2L).longValue()).append("m").append(" -Xss512k -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/gc.log").append(" -Dfile.encoding=UTF-8");
            app.setJavaOpts(jvmParams.toString());
        }
    }

    public void prepareForBuild(SuposApp app) {
        if (app.getAppType() == 0) {
            StringBuffer jvmParams = new StringBuffer();
            StringBuffer initmodule = new StringBuffer();
            String initParam = this.msModuleService.addInitParam(app.getModules(), initmodule);
            String taskid = (String)EcUtils.deployTask.get("bapEcTask");
            jvmParams.append(" -Xmx").append(Double.valueOf(app.getMemory().longValue()).longValue()).append("m").append(" -Xms").append(Double.valueOf(app.getMemory().longValue()).longValue()).append("m").append(" -Xmn").append(Double.valueOf((double)app.getMemory().longValue() * 0.5).longValue()).append("m").append(" -Xss512k -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/tmp/gc.log").append(" -DDeployType=").append("1").append(" -DMS_Module=").append(this.entityPath).append(" -DInit.modules=").append(initParam).append(" -DBAP_WORKSPACE=").append(this.workspace).append(" -Dtask.id=").append(taskid).append(" -Dec.log.path=").append(this.codeLogPath + File.separatorChar + "publish" + File.separatorChar + taskid + ".log").append(" -Dfile.encoding=UTF-8").append(" -DInit.sql.modules=").append(initmodule);
            app.setJavaOpts(jvmParams.toString());
        }
    }

    @Transactional(readOnly=true)
    public List list() {
        List suposApps = this.suposAppDao.list();
        try {
            ListResult apps = this.appInstallerService.getApps();
            Collection appList = apps.getList();
            HashSet<String> appSet = new HashSet<String>(appList.size());
            for (QueryAppDTO queryAppDTO : appList) {
                appSet.add(queryAppDTO.getAppId());
            }
            for (SuposApp suposApp : suposApps) {
                String appID = this.getAppId(suposApp.getCode());
                int appStatus = appSet.contains(appID) ? 1 : 0;
                suposApp.setAppStatus(Integer.valueOf(appStatus));
            }
        }
        catch (Exception e) {
            this.log.error(e.getMessage(), (Throwable)e);
        }
        return suposApps;
    }

    public void add(SuposApp suposApp) {
        this.checkApp(suposApp, "add");
        this.suposAppDao.add(suposApp);
    }

    public void merge(SuposApp suposApp) {
        this.checkApp(suposApp, "merge");
        this.suposAppDao.merge(suposApp);
    }

    public SuposApp get(String code) {
        SuposApp suposApp = this.suposAppDao.get(code);
        return suposApp;
    }

    public synchronized void delete(String code) {
        this.suposAppDao.delete(code);
    }

    public void deleteBatch(List<String> codes) {
        codes.stream().forEach(code -> this.checkAppDelete((String)code));
        codes.stream().forEach(code -> this.delete((String)code));
    }

    private void checkApp(SuposApp suposApp, String dealType) {
        if (suposApp == null || StringUtil.isEmpty((Object)suposApp.getCode())) {
            throw new BAPException(InternationalResource.get((String)"ec.suposapp.code.notnull", (String)this.getCurrentLanguage()));
        }
        SuposApp app = this.get(suposApp.getCode());
        if (!StringUtil.isEmpty((Object)dealType) && "add".equals(dealType) && Objects.nonNull(app)) {
            throw new BAPException(InternationalResource.get((String)"ec.suposapp.code.exsits", (String)this.getCurrentLanguage()));
        }
        if (suposApp.getAppType() == 0) {
            String[] modules;
            if (StringUtil.isEmpty((Object)suposApp.getMemory())) {
                throw new BAPException(InternationalResource.get((String)"ec.suposapp.memory.notnull", (String)this.getCurrentLanguage()));
            }
            if (suposApp.getMemory() < 800L) {
                throw new BAPException(InternationalResource.get((String)"ec.suposapp.memory.mini.limits", (String)this.getCurrentLanguage()));
            }
            if (StringUtil.isEmpty((Object)suposApp.getModules())) {
                throw new BAPException(InternationalResource.get((String)"ec.suposapp.module.notnull", (String)this.getCurrentLanguage()));
            }
            for (String module : modules = suposApp.getModules().split(",")) {
                List<SuposApp> appList;
                if ("merge".equals(dealType) && Objects.nonNull(app) && app.getModules().contains(module) || (appList = this.findAppsByModuleCode(module)) == null || appList.size() <= 0) continue;
                throw new BAPException(InternationalResource.get((String)"ec.suposapp.module.exsits", (String)this.getCurrentLanguage(), (Object[])new Object[]{module}));
            }
        } else if (StringUtil.isEmpty((Object)suposApp.getMainAppCode())) {
            throw new BAPException(InternationalResource.get((String)"ec.suposapp.invented.must.follow.main", (String)this.getCurrentLanguage()));
        }
        if (StringUtil.isBlank((CharSequence)suposApp.getAppVersion())) {
            suposApp.setAppVersion("1.0.0");
        }
    }

    private void checkAppDelete(String code) {
        List suposApps;
        SuposApp suposApp = this.get(code);
        if (suposApp == null) {
            throw new BAPException(InternationalResource.get((String)"ec.suposapp.notfind", (String)this.getCurrentLanguage(), (Object[])new Object[]{code}));
        }
        if (suposApp.getAppType() == 0 && !(suposApps = this.suposAppDao.listInventedAppsByMainApp(suposApp.getCode())).isEmpty()) {
            throw new BAPException(InternationalResource.get((String)"ec.suposapp.delete.fictitious.before.main", (String)this.getCurrentLanguage()));
        }
    }

    public List<SuposApp> findAppsByModuleCode(String moduleCode) {
        List suposApps = null;
        SuposApp mainApp = this.findMainAppByModuleCode(moduleCode);
        if (mainApp != null) {
            mainApp.setModules(moduleCode);
            suposApps = this.suposAppDao.listInventedAppsByMainApp(mainApp.getCode());
            suposApps.add(mainApp);
        }
        return suposApps;
    }

    public SuposApp findMainAppByModuleCode(String moduleCode) {
        return this.suposAppDao.getMainAppByModule(moduleCode);
    }
}

