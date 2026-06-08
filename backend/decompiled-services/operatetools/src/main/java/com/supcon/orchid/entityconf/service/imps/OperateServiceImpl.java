/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  com.alibaba.fastjson.JSONArray
 *  com.alibaba.fastjson.JSONObject
 *  com.sun.jna.Pointer
 *  com.sun.jna.platform.win32.Kernel32
 *  com.sun.jna.platform.win32.WinNT$HANDLE
 *  com.supcon.orchid.ec.entities.Module
 *  com.supcon.orchid.ec.entities.ModuleRelation
 *  com.supcon.orchid.ec.entities.MsModule
 *  com.supcon.orchid.ec.entities.MsModuleIpAdress
 *  com.supcon.orchid.ec.entities.MsModuleRelation
 *  com.supcon.orchid.ec.services.EcDataSynchronizeService
 *  com.supcon.orchid.entityconf.daos.ModuleDao
 *  com.supcon.orchid.entityconf.daos.MsModuleIpAdressDAO
 *  com.supcon.orchid.entityconf.daos.MsModuleRelationDAO
 *  com.supcon.orchid.entityconf.deployer.ModuleDeploymentManager
 *  com.supcon.orchid.entityconf.deployer.SingleDeployService
 *  com.supcon.orchid.entityconf.entities.DeploymentMsTask
 *  com.supcon.orchid.entityconf.entities.DeploymentMsTask$TaskStatus
 *  com.supcon.orchid.entityconf.entities.DeploymentTask
 *  com.supcon.orchid.entityconf.entities.GeneratePackageTask
 *  com.supcon.orchid.entityconf.entities.NewDeploymentTask
 *  com.supcon.orchid.entityconf.entities.ServiceStartTask
 *  com.supcon.orchid.entityconf.entities.StartService
 *  com.supcon.orchid.entityconf.services.BAPGenerateService
 *  com.supcon.orchid.entityconf.services.ModuleService
 *  com.supcon.orchid.entityconf.services.MsModuleService
 *  com.supcon.orchid.entityconf.services.OperateService
 *  com.supcon.orchid.entityconf.services.StartProgressService
 *  com.supcon.orchid.entityconf.services.StartProgressService$Stage
 *  com.supcon.orchid.entityconf.services.impl.ModuleDeployService
 *  com.supcon.orchid.foundation.services.AclWhiteListservice
 *  com.supcon.orchid.foundation.services.MenuInfoService
 *  com.supcon.orchid.i18n.InternationalResource
 *  com.supcon.orchid.services.BAPException
 *  com.supcon.orchid.services.ConfigurationService
 *  com.supcon.orchid.utils.EcUtils
 *  com.supcon.supfusion.framework.scaffold.redis.external.SimpleRedisTemplate
 *  com.supcon.supfusion.systemconfig.api.SystemApiService
 *  com.supcon.supfusion.systemconfig.api.dto.XmlContentDTO
 *  javax.persistence.OptimisticLockException
 *  javax.servlet.ServletOutputStream
 *  javax.servlet.http.HttpServletRequest
 *  javax.servlet.http.HttpServletResponse
 *  org.apache.commons.collections.CollectionUtils
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.lang.StringUtils
 *  org.hibernate.StaleObjectStateException
 *  org.jasypt.commons.CommonUtils
 *  org.jboss.logging.MDC
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.slf4j.MDC
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.scheduling.annotation.EnableAsync
 *  org.springframework.security.concurrent.DelegatingSecurityContextRunnable
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.supcon.orchid.entityconf.service.imps;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.supcon.orchid.ec.entities.Module;
import com.supcon.orchid.ec.entities.ModuleRelation;
import com.supcon.orchid.ec.entities.MsModule;
import com.supcon.orchid.ec.entities.MsModuleIpAdress;
import com.supcon.orchid.ec.entities.MsModuleRelation;
import com.supcon.orchid.ec.services.EcDataSynchronizeService;
import com.supcon.orchid.entityconf.daos.ModuleDao;
import com.supcon.orchid.entityconf.daos.MsModuleIpAdressDAO;
import com.supcon.orchid.entityconf.daos.MsModuleRelationDAO;
import com.supcon.orchid.entityconf.deployer.ModuleDeploymentManager;
import com.supcon.orchid.entityconf.deployer.SingleDeployService;
import com.supcon.orchid.entityconf.entities.DeploymentMsTask;
import com.supcon.orchid.entityconf.entities.DeploymentTask;
import com.supcon.orchid.entityconf.entities.GeneratePackageTask;
import com.supcon.orchid.entityconf.entities.NewDeploymentTask;
import com.supcon.orchid.entityconf.entities.ServiceStartTask;
import com.supcon.orchid.entityconf.entities.StartService;
import com.supcon.orchid.entityconf.services.BAPGenerateService;
import com.supcon.orchid.entityconf.services.FeignService;
import com.supcon.orchid.entityconf.services.ModuleService;
import com.supcon.orchid.entityconf.services.MsModuleService;
import com.supcon.orchid.entityconf.services.OperateService;
import com.supcon.orchid.entityconf.services.StartProgressService;
import com.supcon.orchid.entityconf.services.TransferService;
import com.supcon.orchid.entityconf.services.impl.ModuleDeployService;
import com.supcon.orchid.foundation.services.AclWhiteListservice;
import com.supcon.orchid.foundation.services.MenuInfoService;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.services.ConfigurationService;
import com.supcon.orchid.utils.EcUtils;
import com.supcon.supfusion.framework.scaffold.redis.external.SimpleRedisTemplate;
import com.supcon.supfusion.systemconfig.api.SystemApiService;
import com.supcon.supfusion.systemconfig.api.dto.XmlContentDTO;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.persistence.OptimisticLockException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleObjectStateException;
import org.jasypt.commons.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@EnableAsync
@Service
@Transactional
public class OperateServiceImpl
implements OperateService {
    @Autowired
    private FeignService feignService;
    private TransferService transferService;
    @Autowired
    private MsModuleIpAdressDAO msModuleIpAdressDAO;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private MsModuleService msModuleService;
    @Autowired
    private ModuleDao moduleDao;
    @Value(value="${entityconf.generatePath:''}")
    private String generatePath;
    @Value(value="${supos.app.path}")
    private String appPath;
    private String userLanguage;
    private static final Logger logger = LoggerFactory.getLogger(OperateServiceImpl.class);
    private static final Logger publishLogger = LoggerFactory.getLogger((String)"bap.ec.generator.publishLog");
    private static String OCD_PATH = "service" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "META-INF" + File.separator + "ocd" + File.separator + "ocd.xml";
    @Autowired
    private SingleDeployService singleDeployService;
    @Autowired
    private MsModuleRelationDAO msModuleRelationDAO;
    @Autowired
    private BAPGenerateService bapGenerateService;
    @Autowired
    private ModuleDeploymentManager manager;
    @Value(value="${spring.profiles.active}")
    private String profilesActive;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AclWhiteListservice aclWhiteListservice;
    @Autowired
    private SystemApiService systemApiService;
    public static Properties ORCHID_PRODUCT = new Properties();
    @Value(value="${logPath:''}")
    private String codeLogPath;
    @Value(value="${entityPath:''}")
    private String entityPath;
    @Value(value="${entityconf.jdk:''}")
    private String msJdk;
    @Value(value="${maven.repository.path:''}")
    private String mavenRepositoryPath;
    @Value(value="${entityconf.moduleServerPath:''}")
    private String moduleServerPath;
    @Value(value="${integration.supos.enabled:false}")
    private Boolean suposEnable;
    @Autowired
    private SimpleRedisTemplate redisTemplate;
    @Autowired
    private EcDataSynchronizeService ecDataSynchronizeService;
    @Autowired
    private ModuleDeployService moduleDeployService;
    @Autowired
    private StartProgressService startProgressService;
    private volatile Exception startException;
    private GenerateTaskRunner generateTaskRunner;
    private List<List<String>> serviceModuleList = new ArrayList<List<String>>();
    private static int CURRENTCPUCORES;
    private Map<String, Set<String>> moduleRelations;
    private Map<String, Set<String>> moduleReferences = new HashMap<String, Set<String>>();
    private Map<String, Set<Integer>> moduleServiceIndexMap = new HashMap<String, Set<Integer>>();
    private Set<String> generatedSet = new CopyOnWriteArraySet<String>();
    private Set<String> packagedSet = new CopyOnWriteArraySet<String>();
    private List<String> allModuleCodes = new ArrayList<String>();
    private List<Integer> serviceModuleSizeList = new ArrayList<Integer>();
    private static final Map<String, Map<String, Object>> currentInstanceMap;
    @Autowired
    private MenuInfoService menuInfoService;

    @Async
    public void startCommandAsync(List<Map<String, Object>> list, DeploymentMsTask deploymentMsTask) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> map = this.startCommand(list, deploymentMsTask);
        if (((Boolean)map.get("success")).booleanValue()) {
            this.startProgressService.setFinishFlag(Boolean.valueOf(true));
            this.userLanguage = deploymentMsTask.getLocale();
            publishLogger.info(InternationalResource.get((String)"ec.msModule.serviceAllStart", (String)this.userLanguage), (Object)((System.currentTimeMillis() - startTime) / 1000L));
        } else {
            publishLogger.error("FAILURE_:" + map.get("message"));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void downloadPackage(HttpServletRequest request, HttpServletResponse response) {
        String taskid = request.getParameter("taskId");
        String fileName = taskid + ".zip";
        String downloadFilePath = this.appPath + File.separator + fileName;
        response.setContentType("application/force-download");
        response.addHeader("Content-Disposition", "attachment;fileName=supplant.zip");
        File file = new File(downloadFilePath);
        if (file.exists()) {
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                ServletOutputStream outputStream = response.getOutputStream();
                int i = bis.read(buffer);
                while (i != -1) {
                    outputStream.write(buffer, 0, i);
                    i = bis.read(buffer);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (bis != null) {
                    try {
                        bis.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private List<NewDeploymentTask> transferTask(List<Module> modules, List<ServiceStartTask> list) {
        NewDeploymentTask deploymentTask;
        ArrayList<NewDeploymentTask> deploymentTasks = new ArrayList<NewDeploymentTask>(list.size());
        ArrayList<ServiceStartTask> commonServices = new ArrayList<ServiceStartTask>(list.size());
        ArrayList<ServiceStartTask> slaveServices = new ArrayList<ServiceStartTask>(list.size());
        for (ServiceStartTask serviceStartTask : list) {
            if (serviceStartTask.getDeployType() == 48 || serviceStartTask.getDeployType() == 144) {
                slaveServices.add(serviceStartTask);
                continue;
            }
            commonServices.add(serviceStartTask);
        }
        for (Module module : modules) {
            deploymentTask = new NewDeploymentTask();
            deploymentTask.setModule(module);
            ServiceStartTask serviceStartTask = this.getServiceStartTask(module, commonServices);
            if (serviceStartTask != null) {
                deploymentTask.setDeployType((int)serviceStartTask.getDeployType());
                serviceStartTask.getDeploymentTasks().add(deploymentTask);
            } else {
                deploymentTask.setDeployType(6);
            }
            deploymentTasks.add(deploymentTask);
        }
        for (ServiceStartTask serviceStartTask : slaveServices) {
            deploymentTask = new NewDeploymentTask();
            deploymentTask.setDeployType((int)serviceStartTask.getDeployType());
            serviceStartTask.getDeploymentTasks().add(deploymentTask);
            deploymentTasks.add(deploymentTask);
        }
        return deploymentTasks;
    }

    private ServiceStartTask getServiceStartTask(Module module, List<ServiceStartTask> commonServices) {
        for (ServiceStartTask serviceStartTask : commonServices) {
            List sortModules = serviceStartTask.getModules();
            Optional<Module> result = sortModules.stream().filter(module1 -> module1.getCode().equals(module.getCode())).findAny();
            if (!result.isPresent()) continue;
            return serviceStartTask;
        }
        return null;
    }

    public void startTask(List<ServiceStartTask> serviceStartTasks) {
        try {
            this.msModuleService.isVaild2(serviceStartTasks);
            List allModule = this.moduleService.findAllModules();
            ArrayList sortModules = new ArrayList(allModule.size());
            for (ServiceStartTask serviceStartTask : serviceStartTasks) {
                String[] refModuleCodeArray;
                String refModuleCodes;
                String moduleSelectsIds = serviceStartTask.getModuleSelectsIds();
                if (StringUtils.isNotBlank((String)moduleSelectsIds)) {
                    String[] moduleSelectsIdsStr = moduleSelectsIds.split(",");
                    ArrayList<Module> serviceStartTaskModule = new ArrayList<Module>(moduleSelectsIdsStr.length);
                    for (String moduleCode : moduleSelectsIdsStr) {
                        Module module = this.getModuleByCode(allModule, moduleCode);
                        if (module == null) continue;
                        serviceStartTaskModule.add(module);
                    }
                    serviceStartTask.setModules(serviceStartTaskModule);
                    sortModules.addAll(serviceStartTaskModule);
                }
                if ((serviceStartTask.getDeployType() & 4) <= 0 || !StringUtils.isNotBlank((String)(refModuleCodes = this.getReferSelectsIds(this.judgeGuide(serviceStartTask.getModuleSelectsIds()))))) continue;
                for (String refModuleCode : refModuleCodeArray = refModuleCodes.split(",")) {
                    Module refModule = this.getModuleByCode(allModule, refModuleCode);
                    if (refModule == null || sortModules.contains(refModule)) continue;
                    sortModules.add(refModule);
                }
            }
            List modules = this.moduleService.deployModuleOrder(sortModules);
            List<NewDeploymentTask> deploymentTasks = this.transferTask(modules, serviceStartTasks);
            this.moduleDeployService.addTask(deploymentTasks, serviceStartTasks);
        }
        catch (Exception e) {
            this.moduleDeployService.dealFailure(e);
        }
    }

    private Module getModuleByCode(List<Module> allModule, String moduleCode) {
        Optional<Module> moduleOptional = allModule.stream().filter(module -> moduleCode.equals(module.getCode())).findAny();
        return moduleOptional.orElse(null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Map<String, Object> startCommand(List<Map<String, Object>> list, DeploymentMsTask deploymentMsTask) {
        this.startException = null;
        EcUtils.deployTask.put("bapEcTask", String.valueOf(deploymentMsTask.getId()));
        MDC.put((String)"bapEcTask", (String)String.valueOf(deploymentMsTask.getId()));
        org.jboss.logging.MDC.put((String)"bapEcTask", (Object)deploymentMsTask.getId());
        org.jboss.logging.MDC.put((String)"BAP.EC.Module", (Object)deploymentMsTask.getModuleCode());
        this.userLanguage = deploymentMsTask.getLocale();
        HashMap<String, Object> jsonMap = new HashMap<String, Object>(16);
        jsonMap.put("success", true);
        jsonMap.put("message", InternationalResource.get((String)"ec.msModule.startSuccess", (String)this.userLanguage));
        if (null == list || list.isEmpty()) {
            return jsonMap;
        }
        if (this.suposEnable.booleanValue()) {
            CURRENTCPUCORES = 1;
        } else {
            CURRENTCPUCORES = Runtime.getRuntime().availableProcessors();
            if (CURRENTCPUCORES > 8) {
                CURRENTCPUCORES = 6;
            } else if (CURRENTCPUCORES >= 4) {
                CURRENTCPUCORES -= 2;
            }
        }
        list.forEach(serviceMap -> {
            String moduleSelectsIds = (String)serviceMap.get("moduleSelectsIds");
            String[] moduleSelectsIdsStr = moduleSelectsIds.split(",");
            this.allModuleCodes.addAll(Arrays.asList(moduleSelectsIdsStr));
            this.serviceModuleSizeList.add(moduleSelectsIdsStr.length);
        });
        List publishModules = this.moduleService.findModule(this.allModuleCodes);
        this.moduleRelations = this.getModuleRelationMap(publishModules);
        StartServiceTaskRunner startServiceTaskRunner = new StartServiceTaskRunner();
        try {
            this.msModuleService.isVaild(list);
            this.generateTaskRunner = new GenerateTaskRunner(list);
            String serviceDeployType = null;
            for (int i = 0; i < list.size(); ++i) {
                String deployType;
                String moduleSelectsIds;
                String serviceCode;
                Map<String, Object> serviceMap2 = list.get(i);
                String string = serviceCode = serviceMap2.get("code") == null ? "" : serviceMap2.get("code").toString();
                if (CommonUtils.isEmpty((String)serviceCode)) continue;
                String string2 = moduleSelectsIds = serviceMap2.get("moduleSelectsIds") == null ? "" : serviceMap2.get("moduleSelectsIds").toString();
                if (CommonUtils.isEmpty((String)moduleSelectsIds)) {
                    throw new BAPException(InternationalResource.get((String)"ec.msModule.moduleRel", (String)this.userLanguage));
                }
                serviceDeployType = deployType = serviceMap2.get("deployType") == null ? "" : serviceMap2.get("deployType").toString();
                String serviceName = serviceMap2.get("name") == null ? "" : serviceMap2.get("name").toString();
                List instanceAddressList = null;
                if (!"2".equals(deployType)) {
                    String addressResult = JSONArray.toJSONString((Object)serviceMap2.get("adressResult"));
                    instanceAddressList = JSON.parseArray((String)addressResult, MsModuleIpAdress.class);
                    if (null == instanceAddressList || instanceAddressList.isEmpty()) {
                        throw new BAPException(InternationalResource.get((String)"ec.msModule.ipNotExist", (String)this.userLanguage));
                    }
                    for (MsModuleIpAdress instanceAddress : instanceAddressList) {
                        this.stopPubMethod(instanceAddress, serviceCode);
                    }
                }
                this.aclWhiteListservice.readAclWhiteListToRedis(moduleSelectsIds);
                this.startProgressService.addProgress(0.2, StartProgressService.Stage.Prepare);
                if ("1".equals(deployType) || "2".equals(deployType)) {
                    this.addTask(serviceCode, moduleSelectsIds, instanceAddressList, serviceName, i);
                    continue;
                }
                ArrayList<Module> moduleTempList = new ArrayList<Module>();
                for (String moduleCode : moduleSelectsIds.split(",")) {
                    moduleTempList.add(this.moduleService.getModule(moduleCode));
                }
                StartService startService = new StartService(serviceCode, serviceName, moduleSelectsIds, moduleTempList, instanceAddressList, Integer.valueOf(i), true);
                startServiceTaskRunner.submit(startService);
            }
            if ("1".equals(serviceDeployType) || "2".equals(serviceDeployType)) {
                Set toGenerateModuleCodeSet = this.serviceModuleList.stream().flatMap(list12 -> list12.stream()).collect(Collectors.toSet());
                this.startProgressService.setProperties(toGenerateModuleCodeSet.size(), list.size());
                this.generateTaskRunner.init();
                this.generateTaskRunner.getPackageTaskRunner().getStartServiceTaskRunner().getStartServiceThread().join();
                if (null != this.startException) {
                    throw this.startException;
                }
            } else {
                this.startProgressService.setProperties(0, list.size());
                startServiceTaskRunner.init();
                startServiceTaskRunner.getStartServiceThread().join();
                if (null != this.startException) {
                    throw this.startException;
                }
            }
            deploymentMsTask.setStatus(DeploymentMsTask.TaskStatus.FINISHED);
        }
        catch (Exception e) {
            deploymentMsTask.setStatus(DeploymentMsTask.TaskStatus.FAILED);
            logger.error(e.getMessage(), (Throwable)e);
            if (e instanceof HibernateOptimisticLockingFailureException || e instanceof OptimisticLockException || e instanceof StaleObjectStateException) {
                publishLogger.error("FAILURE_:\u672c\u6570\u636e\u5df2\u7ecf\u88ab\u5176\u4ed6\u4eba\u4fee\u6539\u6216\u5220\u9664\uff0c\u8bf7\u5237\u65b0\u9875\u9762\u540e\u91cd\u8bd5\u3002", (Object)e.getMessage());
            } else {
                publishLogger.error("FAILURE_:" + e.getMessage());
            }
            jsonMap.put("success", false);
            jsonMap.put("message", InternationalResource.get((String)"ec.msModule.service.errorStart", (String)this.userLanguage) + ":" + e.getMessage());
        }
        finally {
            this.generateTaskRunner = null;
            this.startException = null;
            this.serviceModuleList.clear();
            this.moduleRelations = null;
            this.moduleReferences.clear();
            this.moduleServiceIndexMap.clear();
            this.generatedSet.clear();
            this.packagedSet.clear();
            this.allModuleCodes.clear();
            this.startProgressService.reset();
            this.serviceModuleSizeList.clear();
            deploymentMsTask.setStatus(DeploymentMsTask.TaskStatus.FINISHED);
            this.ecDataSynchronizeService.clearCache();
            MDC.remove((String)"BAP.EC.Module");
            MDC.remove((String)"bapEcTask");
        }
        return jsonMap;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean exeProcess(Process process, String ipAddress) {
        InputStream is1 = process.getInputStream();
        InputStream is2 = process.getErrorStream();
        StreamGobbler outputGobbler = null;
        StreamGobbler errorGobbler = null;
        try {
            outputGobbler = new StreamGobbler(is1, "Output");
            errorGobbler = new StreamGobbler(is2, "Error");
            errorGobbler.start();
            outputGobbler.start();
            if (this.heartBeat(ipAddress).booleanValue()) {
                boolean bl = true;
                return bl;
            }
            try {
                if (null != process) {
                    process.destroy();
                }
            }
            catch (Exception e) {
                logger.warn(e.getMessage(), (Throwable)e);
            }
        }
        catch (Exception e) {
            logger.error("\u7b49\u5f85\u5b9e\u4f8b\u8fd0\u884c\u8fc7\u7a0b\u4e2d\u9519\u8bef\uff1a", (Throwable)e);
            try {
                process.getOutputStream().close();
            }
            catch (Exception e2) {
                logger.warn(e2.getMessage(), (Throwable)e2);
            }
        }
        finally {
            currentInstanceMap.remove(ipAddress);
        }
        return false;
    }

    private Boolean heartBeat(String ipAddress) throws InterruptedException {
        int timeOut = 300;
        long lastHeartbeatId = 0L;
        Map<String, Object> instanceMap = currentInstanceMap.get(ipAddress);
        for (int i = 0; i < timeOut; ++i) {
            if (instanceMap.containsKey("status")) {
                Long heartbeatId;
                String status = String.valueOf(instanceMap.get("status"));
                if ("FINISHED".equals(status)) {
                    logger.info("\u5b9e\u4f8b\u8fd0\u884c\u5b8c\u6210");
                    return true;
                }
                if ("FAILED".equals(status)) {
                    logger.info("\u5b9e\u4f8b\u8fd0\u884c\u5931\u8d25");
                    return false;
                }
                if ("STARTING".equals(status) && (heartbeatId = (Long)instanceMap.get("heartbeatId")) != null && heartbeatId > lastHeartbeatId) {
                    logger.info("\u6536\u5230\u5b9e\u4f8b\u5fc3\u8df3...");
                    lastHeartbeatId = heartbeatId;
                    i = 0;
                }
            }
            Thread.sleep(2000L);
        }
        return true;
    }

    public Boolean randomRed(RandomAccessFile raf, int i, String msModuleCode, long pointer) {
        Boolean ifStart = false;
        try {
            if (i == 0) {
                logger.info("-----------\u8282\u70b9\u4e3a :" + pointer);
            } else {
                raf.seek(pointer);
                logger.info("-----------pointer\u8282\u70b9\u4e3a :" + pointer + "--------------i\u53c2\u6570" + i);
                String line = null;
                while ((line = raf.readLine()) != null) {
                    if (!line.contains("Started " + msModuleCode)) continue;
                    ifStart = true;
                    break;
                }
                pointer = raf.getFilePointer();
                raf.close();
            }
        }
        catch (Exception e) {
            System.out.println("\u5f02\u5e38\uff1a" + e.getMessage());
            pointer = 0L;
            logger.error("------------randomRed\u65b9\u6cd5\u5f02\u5e38\uff1a" + e.getMessage());
        }
        return ifStart;
    }

    public Map<String, Object> stopCommand(List<Map<String, Object>> list) {
        HashMap<String, Object> jsonMap;
        block6: {
            jsonMap = new HashMap<String, Object>(16);
            jsonMap.put("success", true);
            jsonMap.put("message", InternationalResource.get((String)"ec.msModule.service.stopSuccess", (String)this.userLanguage));
            try {
                if (list == null || list.size() <= 0) break block6;
                for (Map<String, Object> map : list) {
                    String serviceCodes;
                    String string = serviceCodes = map.get("code") == null ? "" : map.get("code").toString();
                    if (CommonUtils.isNotEmpty((String)serviceCodes)) {
                        String ipAdress = JSONArray.toJSONString((Object)map.get("adressResult"));
                        List adressList = JSON.parseArray((String)ipAdress, MsModuleIpAdress.class);
                        if (null != adressList && adressList.size() > 0) {
                            for (MsModuleIpAdress msModuleIpAdress : adressList) {
                                this.stopPubMethod(msModuleIpAdress, serviceCodes);
                                this.msModuleIpAdressDAO.createNativeQuery("DELETE FROM ec_msmodule_ipadress WHERE IPADRESS = ?", new Object[]{msModuleIpAdress.getIpadress()}).executeUpdate();
                                msModuleIpAdress.setCode(UUID.randomUUID().toString());
                                msModuleIpAdress.setValid(true);
                                msModuleIpAdress.setMsModule(this.msModuleService.getMsModule(serviceCodes));
                                msModuleIpAdress.setStatus(Integer.valueOf(0));
                                msModuleIpAdress.setDeleteTime(new Date());
                                msModuleIpAdress.setIpadress(msModuleIpAdress.getIpadress());
                                this.msModuleIpAdressDAO.save((Object)msModuleIpAdress);
                            }
                            continue;
                        }
                        throw new BAPException(InternationalResource.get((String)"ec.msModule.ipNotExist", (String)this.userLanguage));
                    }
                    throw new BAPException(InternationalResource.get((String)"ec.msModule.serviceNotExist", (String)this.userLanguage));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage(), (Throwable)e);
                jsonMap.put("success", false);
                jsonMap.put("message", InternationalResource.get((String)"ec.msModule.service.stopError", (String)this.userLanguage) + ":" + e.getMessage());
            }
        }
        return jsonMap;
    }

    private void stopPubMethod(MsModuleIpAdress msModuleIpAdress, String serviceCode) {
        this.stopPubMethod(msModuleIpAdress.getIpadress(), serviceCode, msModuleIpAdress.getStatus().shortValue());
    }

    public void stopPubMethod(String ipAddress, String serviceCode, short status) {
        try {
            String pidMap;
            JSONObject pidNr;
            logger.info("\u5f00\u59cb\u505c\u6b62-----------" + ipAddress + "\u4e0a\u7684" + serviceCode + "\u670d\u52a1");
            String servicePidKey = serviceCode + "_" + ipAddress + "_PID";
            Object servicePid = this.redisTemplate.opsForValue().get((Object)servicePidKey);
            String pid = servicePid == null ? "" : servicePid.toString();
            String url = "http://" + ipAddress;
            this.transferService = this.feignService.newInstanceByUrl(TransferService.class, url);
            if (CommonUtils.isEmpty((String)pid) && status == 1 && null != (pidNr = JSONObject.parseObject((String)(pidMap = this.transferService.getPid())))) {
                JSONObject pidNrData;
                if (null == pidNr.getBoolean("success")) {
                    JSONObject property = JSONObject.parseObject((String)(pidNr.getString("property") == null ? "" : pidNr.getString("property")));
                    pid = property.getString("value") == null ? "" : property.getString("value").toString();
                } else if (pidNr.getBoolean("success").booleanValue() && null != (pidNrData = JSONObject.parseObject((String)(pidNr.getString("data") == null ? "" : pidNr.getString("data"))))) {
                    JSONObject property = JSONObject.parseObject((String)(pidNrData.getString("property") == null ? "" : pidNrData.getString("property")));
                    String string = pid = property.getString("value") == null ? "" : property.getString("value").toString();
                }
            }
            if (status == 1) {
                this.transferService.stop();
            }
            Thread.sleep(2000L);
            if (CommonUtils.isNotEmpty((String)pid)) {
                logger.info(ipAddress + "\u7684pid\u4e3a" + pid);
                String[] commands = new String[]{"taskkill /f /pid " + pid};
                this.bapGenerateService.executeCommand(commands, null);
                logger.info("\u8fdb\u884c\u4e86\u5220\u9664\u8fdb\u7a0b\u7684\u64cd\u4f5c: taskkill /f /pid " + pid + "\u7684\u64cd\u4f5c");
                this.redisTemplate.delete((Object)servicePidKey);
            }
            logger.info("\u670d\u52a1\u505c\u6b62\u7ed3\u675f");
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
            throw new RuntimeException(e);
        }
    }

    public void jarVaild(List<Map<String, Object>> list) {
        String truePath = this.generatePath.replaceAll("//", "\\\\");
        if (list != null && list.size() > 0) {
            for (Map<String, Object> map : list) {
                String serviceCodes;
                String string = serviceCodes = map.get("code") == null ? "" : map.get("code").toString();
                String path = truePath + "/" + serviceCodes + "/target/" + serviceCodes + "-1.0.0.jar";
                File testFile = new File(path);
                if (testFile.exists()) continue;
                throw new BAPException(truePath + "/" + serviceCodes + "/target  \u8def\u5f84\u4e0b\u6ca1\u6709" + serviceCodes + "\u7684jar\u5305");
            }
        }
    }

    public void msJarVaild(String serviceCode, String jarPath) {
        File testFile = new File(jarPath);
        if (!testFile.exists()) {
            throw new BAPException(InternationalResource.get((String)"ec.module.generate.compilePackageFailed", (String)this.userLanguage) + jarPath + InternationalResource.get((String)"ec.msModule.packagepath", (String)this.userLanguage) + serviceCode + InternationalResource.get((String)"ec.msModule.packageJar", (String)this.userLanguage));
        }
    }

    public Boolean moduleJarVaild(String moduleCode) {
        Module module = this.msModuleService.getModule(moduleCode);
        Boolean isSuccess = true;
        String path = null;
        if (module.getIsProto() == null || !module.getIsProto().booleanValue()) {
            path = this.mavenRepositoryPath + File.separator + "com/supcon/greendill" + File.separator + module.getArtifact() + File.separator + "com.supcon.greendill." + module.getArtifact() + ".service" + File.separator + module.getProjectVersion();
        } else {
            String bootstrapFileName = module.getArtifact() + "-bootstrap";
            path = this.mavenRepositoryPath + File.separator + "com/supcon/supfusion" + File.separator + module.getArtifact() + File.separator + bootstrapFileName + File.separator + module.getProjectVersion();
        }
        logger.info("\u5224\u65ad\u6a21\u5757\u662f\u5426\u6253\u5305\uff0c\u5bfb\u627e\u672c\u5730\u4ed3\u5e93\u5730\u5740\uff1a" + path);
        File testFile = new File(path);
        if (testFile.exists()) {
            File[] listFiles = testFile.listFiles();
            if (listFiles.length == 0) {
                logger.info("\u5b50\u6587\u4ef6\u4e3a\u7a7a");
                return true;
            }
            for (File f : listFiles) {
                String fileName = "-" + module.getProjectVersion() + ".jar";
                if (!f.getName().contains(fileName)) continue;
                isSuccess = false;
                break;
            }
        }
        return isSuccess;
    }

    public List<MsModuleRelation> getMsByCode(String code) {
        List list = this.msModuleRelationDAO.findByHql("From MsModuleRelation where msModule.code=?0 ", new Object[]{code});
        return list;
    }

    public void addTask(String code, String moduleSelectsIds, List<MsModuleIpAdress> adressList, String msName) {
        try {
            boolean proto = true;
            long currentTimes = System.currentTimeMillis();
            if (CommonUtils.isNotEmpty((String)moduleSelectsIds) && this.proGenerateAndPackage(moduleSelectsIds, code, proto, msName)) {
                String refCode = this.getReferSelectsIds(this.judgeGuide(moduleSelectsIds));
                String[] modules = refCode.split(",");
                List<String> modulesList = Arrays.asList(modules);
                for (String moduleCode : modules) {
                    if (!CommonUtils.isNotEmpty((String)moduleCode)) continue;
                    ArrayList<DeploymentTask> cloneDeploymentTasks = new ArrayList<DeploymentTask>();
                    DeploymentTask deploymentTask = new DeploymentTask(moduleCode, 16);
                    DeploymentTask task = this.manager.cloneDeploymentTask(deploymentTask);
                    cloneDeploymentTasks.add(task);
                    Module module = this.msModuleService.getModule(moduleCode);
                    String codeName = InternationalResource.get((String)module.getName());
                    if (null != module.getIsProto() && module.getIsProto().booleanValue()) {
                        throw new BAPException(codeName + InternationalResource.get((String)"ec.msModule.module.isProto", (String)this.userLanguage));
                    }
                    publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.generate", (String)this.userLanguage));
                    currentTimes = System.currentTimeMillis();
                    this.singleDeployService.doGenerateAll(cloneDeploymentTasks);
                    publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.generateTime", (String)this.userLanguage), (Object)((System.currentTimeMillis() - currentTimes) / 1000L));
                    publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.startPackage", (String)this.userLanguage));
                    currentTimes = System.currentTimeMillis();
                    this.bapGenerateService.buildPackage(module, null);
                    String ocdpath = this.generatePath + File.separator + module.getCode() + File.separator + OCD_PATH;
                    File ocdfile = new File(ocdpath);
                    if (ocdfile.exists()) {
                        this.registerOcdFile(ocdfile.getPath());
                    }
                    if (this.moduleJarVaild(moduleCode).booleanValue()) {
                        String logCodePath = this.codeLogPath + "/servicemanager.log";
                        throw new BAPException(InternationalResource.get((String)"ec.msModule.module.errorPackage", (String)this.userLanguage) + logCodePath + InternationalResource.get((String)"foundation.base.audit", (String)this.userLanguage));
                    }
                    publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.packageSuccess", (String)this.userLanguage), (Object)((System.currentTimeMillis() - currentTimes) / 1000L));
                }
                List<Module> list = this.getModuleByCode(modulesList);
                publishLogger.info(msName + InternationalResource.get((String)"ec.msModule.service.generate", (String)this.userLanguage));
                currentTimes = System.currentTimeMillis();
                this.bapGenerateService.generateBootStartProject(code, list);
                publishLogger.info(msName + " " + InternationalResource.get((String)"ec.msModule.service.package", (String)this.userLanguage));
                currentTimes = System.currentTimeMillis();
                this.bapGenerateService.buildMisPackage(code, null);
                this.copyJARToInstanceDir(code, adressList);
                publishLogger.info(msName + " " + InternationalResource.get((String)"ec.msModule.service.packageSuccess", (String)this.userLanguage), (Object)((System.currentTimeMillis() - currentTimes) / 1000L));
                currentTimes = System.currentTimeMillis();
                this.configurationService.zkUpload(code, this.generatePath, ORCHID_PRODUCT, this.profilesActive);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
            e.printStackTrace();
            throw new BAPException(e.getMessage());
        }
    }

    public void addTask(String code, String moduleSelectsIds, List<MsModuleIpAdress> adressList, String msName, Integer serviceIndex) {
        try {
            boolean proto = true;
            if (CommonUtils.isNotEmpty((String)moduleSelectsIds) && this.proGenerateAndPackage(moduleSelectsIds, code, proto, msName)) {
                String refCode = this.getReferSelectsIds(this.judgeGuide(moduleSelectsIds));
                String[] modules = refCode.split(",");
                ArrayList<String> modulesList = new ArrayList<String>();
                this.startProgressService.addProgress(0.4, StartProgressService.Stage.Prepare);
                for (String moduleCode : modules) {
                    if (!CommonUtils.isNotEmpty((String)moduleCode)) continue;
                    Module module = this.msModuleService.getModule(moduleCode);
                    GeneratePackageTask task = new GeneratePackageTask(module, serviceIndex, code);
                    this.generateTaskRunner.submit(task);
                    modulesList.add(moduleCode);
                    if (this.moduleServiceIndexMap.containsKey(moduleCode)) {
                        this.moduleServiceIndexMap.get(moduleCode).add(serviceIndex);
                        continue;
                    }
                    this.moduleServiceIndexMap.put(moduleCode, new HashSet<Integer>(Arrays.asList(serviceIndex)));
                }
                this.serviceModuleList.add(modulesList);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
            throw new BAPException(e.getMessage());
        }
    }

    private void copyJARToInstanceDir(String serviceCode, List<MsModuleIpAdress> instanceAddressList) {
        if (serviceCode == null || instanceAddressList == null || instanceAddressList.isEmpty()) {
            return;
        }
        String jarName = new StringBuffer().append(serviceCode).append("-1.0.0.jar").toString();
        String sourcePath = new StringBuffer().append(this.generatePath).append(File.separator).append(serviceCode).append(File.separator).append("target").append(File.separator).append(jarName).toString();
        String destBasePath = new StringBuffer().append(this.moduleServerPath).append(File.separator).append(serviceCode).toString();
        for (MsModuleIpAdress instanceAddress : instanceAddressList) {
            String ipAddress = instanceAddress.getIpadress().replace(":", "-");
            String destPath = new StringBuffer().append(destBasePath).append(File.separator).append(ipAddress).append(File.separator).append(jarName).toString();
            logger.info("\u590d\u5236jar\u5305\uff1a" + sourcePath + "=>" + destPath);
            File source = new File(sourcePath);
            File dest = new File(destPath);
            try {
                FileUtils.copyFile((File)source, (File)dest);
            }
            catch (IOException e) {
                logger.trace("FileUploadException : ", (Throwable)e);
            }
        }
    }

    public void addTaskBefore(String[] codes) {
        if (codes == null || codes.length == 0) {
            return;
        }
        for (String code : codes) {
            String msCodes = "";
            MsModule msmodule = this.msModuleService.getMsModule(code);
            String relationHql = "from MsModuleRelation where msModule.code = ?0 and valid = 1";
            List msModuleRelations = this.msModuleRelationDAO.findByHql(relationHql, new Object[]{code});
            for (MsModuleRelation msCode : msModuleRelations) {
                msCodes = msCodes + "," + msCode.getCode();
            }
            String addressHql = "from MsModuleIpAdress where msModule.code = ?0 and valid = 1";
            List msModuleIpAdresses = this.msModuleIpAdressDAO.findByHql(addressHql, new Object[]{code});
            this.addTask(code, msCodes, msModuleIpAdresses, msmodule.getName());
        }
    }

    private List<Module> getModuleByCode(List<String> codes) {
        List moduleList = this.moduleDao.createQuery("from Module where code in (:codes) and valid=true", new Object[0]).setParameterList("codes", codes).list();
        return moduleList;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void downloadFile(HttpServletRequest request, HttpServletResponse response) {
        String taskid = request.getParameter("taskId");
        String fileName = taskid + "-full.log";
        String downloadFilePath = this.codeLogPath + "/publish/" + taskid + "-full.log";
        response.setContentType("application/force-download");
        response.addHeader("Content-Disposition", "attachment;fileName=" + fileName);
        File file = new File(downloadFilePath);
        if (file.exists()) {
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                fis = new FileInputStream(file);
                bis = new BufferedInputStream(fis);
                ServletOutputStream outputStream = response.getOutputStream();
                int i = bis.read(buffer);
                while (i != -1) {
                    outputStream.write(buffer, 0, i);
                    i = bis.read(buffer);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (bis != null) {
                    try {
                        bis.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String judgeGuide(String moduleSelectsIds) {
        String[] code = moduleSelectsIds.split(",");
        String str = "";
        for (String mcode : code) {
            if (!CommonUtils.isNotEmpty((String)mcode)) continue;
            str = this.judgeGuideTarget(mcode, moduleSelectsIds, str);
        }
        return str;
    }

    public String judgeGuideTarget(String mcode, String moduleSelectsIds, String str) {
        List list = this.msModuleIpAdressDAO.createNativeQuery("select target_module_code as relcode from ec_module_relation   where  MODULE_CODE= ?", new Object[]{mcode}).list();
        if (null == list || list.size() <= 0) {
            str = str + "," + mcode;
            str = this.judgeGuideModule(mcode, moduleSelectsIds, str);
        }
        return str;
    }

    public String judgeGuideModule(String mcode, String moduleSelectsIds, String str) {
        List list = this.msModuleIpAdressDAO.createNativeQuery("select MODULE_CODE as relcode from ec_module_relation   where  target_module_code= ?", new Object[]{mcode}).list();
        if (null != list && list.size() > 0) {
            for (String relcode : list) {
                if (!moduleSelectsIds.contains(relcode)) continue;
                str = !str.contains(relcode) ? str + "," + relcode : str.replace(relcode, "") + "," + relcode;
                str = this.judgeGuideModule(relcode, moduleSelectsIds, str);
            }
        } else if (moduleSelectsIds.contains(mcode) && !str.contains(mcode)) {
            str = str + "," + mcode;
        }
        return str;
    }

    private String getReferSelectsIds(String moduleSelectsIds) {
        String[] moduleSelectsId;
        StringBuffer refer = new StringBuffer();
        refer.append(moduleSelectsIds);
        for (String moduleCode : moduleSelectsId = moduleSelectsIds.split(",")) {
            List list;
            if (!CommonUtils.isNotEmpty((String)moduleCode) || null == (list = this.msModuleIpAdressDAO.createNativeQuery("select TARGET_MODULE_CODE as relcode from ec_module_reference where module_code=?", new Object[]{moduleCode}).list()) || list.size() <= 0) continue;
            HashSet<String> referenceList = new HashSet<String>();
            for (String tCode : list) {
                if (!this.allModuleCodes.contains(tCode) && !this.moduleJarVaild(tCode).booleanValue()) continue;
                this.montageStr(refer, tCode);
                this.getAssociatedAndRefer(tCode, refer);
                this.getModuleReference(tCode, refer);
                referenceList.add(tCode);
            }
            this.moduleReferences.put(moduleCode, referenceList);
        }
        return refer.toString();
    }

    private void getAssociatedAndRefer(String mcode, StringBuffer str) {
        List list = this.msModuleIpAdressDAO.createNativeQuery("select target_module_code as relcode from ec_module_relation   where  MODULE_CODE= ?", new Object[]{mcode}).list();
        if (null != list && list.size() > 0) {
            for (String targetcode : list) {
                this.montageStr(str, targetcode);
                this.getAssociatedAndRefer(targetcode, str);
                this.getModuleReference(targetcode, str);
            }
        }
    }

    private void getModuleReference(String targetcode, StringBuffer str) {
        List list = this.msModuleIpAdressDAO.createNativeQuery("select TARGET_MODULE_CODE as relcode from ec_module_reference where module_code=?", new Object[]{targetcode}).list();
        if (null != list && list.size() > 0) {
            for (String code : list) {
                if (!this.allModuleCodes.contains(code) && !this.moduleJarVaild(code).booleanValue()) continue;
                this.montageStr(str, code);
                this.getAssociatedAndRefer(code, str);
                this.getModuleReference(code, str);
            }
        }
    }

    private void montageStr(StringBuffer str, String code) {
        String strCode = "";
        String[] strList = str.toString().split(",");
        if (Arrays.asList(strList).contains(code)) {
            ArrayList<String> arrayList = new ArrayList<String>(Arrays.asList(strList));
            arrayList.remove(code);
            String arrayListStr = ((Object)arrayList).toString().replace("[", "");
            strCode = code + "," + arrayListStr.replace("]", "").replace(" ", "");
        } else {
            strCode = code + "," + str.toString();
        }
        str.delete(0, str.length());
        str.append(strCode);
    }

    private boolean proGenerateAndPackage(String moduleSelectsIds, String code, boolean proto, String msName) {
        String protoPath = this.generatePath + File.separator + code;
        String apiXxPath = protoPath + File.separator + "api" + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + "com" + File.separator + "supcon" + File.separator + "greendill";
        String[] modules = moduleSelectsIds.split(",");
        List<String> modulesList = Arrays.asList(modules);
        List<Module> list = this.getModuleByCode(modulesList);
        for (Module module : list) {
            if (null == module.getIsProto() || !module.getIsProto().booleanValue()) continue;
            proto = false;
            try {
                File protoFile = new File(protoPath);
                if (!protoFile.exists()) break;
                FileUtils.deleteDirectory((File)protoFile);
                break;
            }
            catch (IOException e) {
                logger.error(e.getMessage());
                throw new BAPException(e.getMessage());
            }
        }
        try {
            if (!proto) {
                this.bapGenerateService.generateProtoProject(code, list);
                for (Module module : list) {
                    File moduleFile;
                    String modulePath = this.generatePath + File.separator + module.getCode();
                    String apiPath = modulePath + File.separator + module.getArtifact();
                    File apiFile = new File(apiPath);
                    if (apiFile.exists()) {
                        FileUtils.copyDirectoryToDirectory((File)apiFile, (File)new File(apiXxPath));
                    }
                    if (!(moduleFile = new File(modulePath)).exists()) {
                        throw new BAPException(InternationalResource.get((String)"ec.msModule.module.isProtoFirst", (String)this.userLanguage));
                    }
                    this.bapGenerateService.generateProtoPom(module, modulePath, code, list);
                    FileUtils.copyDirectoryToDirectory((File)moduleFile, (File)new File(protoPath));
                }
                publishLogger.info(msName + InternationalResource.get((String)"ec.msModule.service.package", (String)this.userLanguage));
                long protoTimes = System.currentTimeMillis();
                this.bapGenerateService.buildProtoPackage(code, list, null);
                publishLogger.info(msName + InternationalResource.get((String)"ec.msModule.service.packageSuccess", (String)this.userLanguage), (Object)((System.currentTimeMillis() - protoTimes) / 1000L));
            }
        }
        catch (IOException e) {
            logger.error(e.getMessage());
            throw new BAPException(e.getMessage());
        }
        return proto;
    }

    private void getRuntimePid(Process process, String serviceCode, MsModuleIpAdress moduleIpAdress) {
        try {
            Field f = process.getClass().getDeclaredField("handle");
            f.setAccessible(true);
            long handl = f.getLong(process);
            Kernel32 kernel = Kernel32.INSTANCE;
            WinNT.HANDLE handle = new WinNT.HANDLE();
            handle.setPointer(Pointer.createConstant((long)handl));
            int ret = kernel.GetProcessId(handle);
            Long PID = ret;
            String pidKey = serviceCode + "_" + moduleIpAdress.getIpadress() + "_PID";
            this.redisTemplate.opsForValue().set((Object)pidKey, (Object)PID);
        }
        catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Object> moduleRelValid(List<Map<String, Object>> list, String userLanguage, DeploymentMsTask deploymentMsTask) {
        HashMap<String, Object> jsonMap = new HashMap<String, Object>(16);
        jsonMap.put("success", true);
        jsonMap.put("confirm", false);
        jsonMap.put("message", "");
        try {
            if (null == deploymentMsTask || !DeploymentMsTask.TaskStatus.RUNNING.equals((Object)deploymentMsTask.getStatus())) {
                StringBuffer allStr = new StringBuffer();
                for (Map<String, Object> map : list) {
                    String[] modules;
                    StringBuffer str = new StringBuffer();
                    StringBuffer strModule = new StringBuffer();
                    String serviceCodes = map.get("code") == null ? "" : map.get("code").toString();
                    if (!CommonUtils.isNotEmpty((String)serviceCodes)) continue;
                    String moduleSelectsIds = map.get("moduleSelectsIds") == null ? "" : map.get("moduleSelectsIds").toString();
                    String refCode = this.getReferSelectsIds(this.judgeGuide(moduleSelectsIds));
                    MsModule msModule = this.msModuleService.getMsModule(serviceCodes);
                    str.append(msModule.getName() + InternationalResource.get((String)"ec.msService.refModule", (String)userLanguage) + "\uff1a");
                    for (String moduleCode : modules = refCode.split(",")) {
                        if (!CommonUtils.isNotEmpty((String)moduleCode) || moduleSelectsIds.contains(moduleCode)) continue;
                        Module module = this.msModuleService.getModule(moduleCode);
                        strModule.append(InternationalResource.get((String)module.getName(), (String)userLanguage) + ",");
                    }
                    if (null == strModule || "".equals(strModule.toString())) continue;
                    strModule.deleteCharAt(strModule.length() - 1);
                    str.append(strModule);
                    allStr.append(str + "\uff1b");
                }
                if (null != allStr && !"".equals(allStr.toString())) {
                    allStr.append(InternationalResource.get((String)"ec.msService.refGenerate", (String)userLanguage));
                    jsonMap.put("confirm", true);
                    jsonMap.put("message", allStr);
                }
            }
        }
        catch (Exception e) {
            jsonMap.put("success", false);
            jsonMap.put("message", e.getMessage());
        }
        return jsonMap;
    }

    private void registerOcdFile(String path) {
        try {
            byte[] b = Files.readAllBytes(Paths.get(path, new String[0]));
            String ocd = Base64.getEncoder().encodeToString(b);
            ArrayList<String> list = new ArrayList<String>();
            list.add(ocd);
            XmlContentDTO xmlContentDTO = new XmlContentDTO();
            xmlContentDTO.setXmlList(list);
            this.systemApiService.saveOcdContent(xmlContentDTO);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), (Throwable)e);
            throw new BAPException((Throwable)e);
        }
    }

    private Map<String, Set<String>> getModuleRelationMap(List<Module> publishModule) {
        HashMap<String, Set<String>> modules = new HashMap<String, Set<String>>();
        List moduleRelations = this.moduleService.getAllRelation();
        block0: for (int i = 0; i < moduleRelations.size(); ++i) {
            ModuleRelation moduleRelation = (ModuleRelation)moduleRelations.get(i);
            for (int j = 0; j < publishModule.size(); ++j) {
                if (!moduleRelation.getModule().getCode().equals(publishModule.get(j).getCode())) continue;
                if (!publishModule.contains(moduleRelation.getTarget())) continue block0;
                if (null != modules.get(publishModule.get(j).getCode())) {
                    ((Set)modules.get(publishModule.get(j).getCode())).add(moduleRelation.getTarget().getCode());
                    continue block0;
                }
                HashSet<String> moduleList = new HashSet<String>();
                moduleList.add(moduleRelation.getTarget().getCode());
                modules.put(publishModule.get(j).getCode(), moduleList);
                continue block0;
            }
        }
        return modules;
    }

    static {
        currentInstanceMap = new ConcurrentHashMap<String, Map<String, Object>>();
    }

    private class StartServiceTaskRunner {
        private final BlockingQueue<StartService> startServiceChannel;
        private volatile boolean inUse = true;
        private volatile boolean hasPulled = false;
        private volatile Thread startServiceThread;
        private GenerateTaskRunner generateTaskRunner;
        private PackageTaskRunner packageTaskRunner;
        private ExecutorService batchStartThreadPool = Executors.newFixedThreadPool(OperateServiceImpl.access$1100());

        public StartServiceTaskRunner() {
            this.startServiceChannel = new LinkedBlockingQueue<StartService>();
            this.startServiceThread = new StartServiceThread("startService");
        }

        public StartServiceTaskRunner(GenerateTaskRunner generateTaskRunner, PackageTaskRunner packageTaskRunner) {
            this.generateTaskRunner = generateTaskRunner;
            this.packageTaskRunner = packageTaskRunner;
            this.startServiceChannel = new LinkedBlockingQueue<StartService>();
            this.startServiceThread = new StartServiceThread("startService");
        }

        public void init() {
            Thread t = this.startServiceThread;
            if (null != t) {
                t.start();
            }
        }

        public void submit(StartService task) throws InterruptedException {
            this.startServiceChannel.put(task);
        }

        public void shutdown() {
            this.inUse = false;
            Thread t = this.startServiceThread;
            if (null != t) {
                t.interrupt();
            }
        }

        public Thread getStartServiceThread() {
            return this.startServiceThread;
        }

        class StartServiceThread
        extends Thread {
            public StartServiceThread(String name) {
                super(name);
            }

            @Override
            public void run() {
                while (true) {
                    if (!StartServiceTaskRunner.this.inUse || (null == StartServiceTaskRunner.this.packageTaskRunner || !StartServiceTaskRunner.this.packageTaskRunner.getInUse()) && StartServiceTaskRunner.this.startServiceChannel.isEmpty() && StartServiceTaskRunner.this.hasPulled) break;
                    try {
                        StartServiceTaskRunner.this.hasPulled = false;
                        StartService startService = (StartService)StartServiceTaskRunner.this.startServiceChannel.poll(500L, TimeUnit.MILLISECONDS);
                        if (null != startService) {
                            StartServiceTaskRunner.this.batchStartThreadPool.execute(() -> {
                                Process process = null;
                                try {
                                    String deployType = "0";
                                    StartServiceTaskRunner.this.hasPulled = true;
                                    org.jboss.logging.MDC.put((String)"bapEcTask", EcUtils.deployTask.get("bapEcTask"));
                                    String serviceCode = startService.getServiceCode();
                                    String serviceName = startService.getServiceName();
                                    Integer serviceIndex = startService.getServiceIndex();
                                    List instanceAddressList = startService.getAdressList();
                                    String moduleSelectsIds = startService.getModuleSelectsIds();
                                    long currentTimes = System.currentTimeMillis();
                                    if (null != StartServiceTaskRunner.this.generateTaskRunner) {
                                        deployType = "1";
                                        List list = startService.getModuleList();
                                        publishLogger.info(serviceName + InternationalResource.get((String)"ec.msModule.service.generate", (String)OperateServiceImpl.this.userLanguage));
                                        OperateServiceImpl.this.bapGenerateService.generateBootStartProject(serviceCode, list);
                                        publishLogger.info(serviceName + " " + InternationalResource.get((String)"ec.msModule.service.package", (String)OperateServiceImpl.this.userLanguage));
                                        currentTimes = System.currentTimeMillis();
                                        OperateServiceImpl.this.bapGenerateService.buildMisPackage(serviceCode, null);
                                        OperateServiceImpl.this.copyJARToInstanceDir(serviceCode, instanceAddressList);
                                        publishLogger.info(serviceName + " " + InternationalResource.get((String)"ec.msModule.service.packageSuccess", (String)OperateServiceImpl.this.userLanguage), (Object)((System.currentTimeMillis() - currentTimes) / 1000L));
                                        currentTimes = System.currentTimeMillis();
                                        OperateServiceImpl.this.configurationService.zkUpload(serviceCode, OperateServiceImpl.this.generatePath, ORCHID_PRODUCT, OperateServiceImpl.this.profilesActive);
                                    }
                                    MsModule serviceModule = null;
                                    String ramNumStr = "";
                                    if (startService.isNeedStartUp()) {
                                        serviceModule = OperateServiceImpl.this.msModuleService.getMsModule(serviceCode);
                                        ramNumStr = serviceModule.getRamNUM();
                                    }
                                    StringBuffer jdkPath = new StringBuffer();
                                    jdkPath.append(OperateServiceImpl.this.msJdk).append("\\java.exe -jar");
                                    StringBuffer jvmParams = new StringBuffer();
                                    String taskid = (String)EcUtils.deployTask.get("bapEcTask");
                                    if (startService.isNeedStartUp()) {
                                        Integer ramNum;
                                        if (ramNumStr.contains("G")) {
                                            ramNum = new BigDecimal(ramNumStr.replaceAll("G", "")).multiply(BigDecimal.valueOf(1024L)).intValue();
                                            jvmParams.append(" -Xmx").append(ramNum).append("M");
                                        } else if (ramNumStr.contains("M")) {
                                            ramNum = new BigDecimal(ramNumStr.replaceAll("M", "")).intValue();
                                            jvmParams.append(" -Xmx").append(ramNum).append("M");
                                        }
                                        StringBuffer initmodule = new StringBuffer();
                                        String initParam = OperateServiceImpl.this.msModuleService.addInitParam(moduleSelectsIds, initmodule);
                                        jvmParams.append(" -DInit.sql.modules=").append(initmodule).append(" -DDeployType=").append(deployType).append(" -XX:MetaspaceSize=256m").append(" -XX:MaxPermSize=256m").append(" -DMS_Module=").append(OperateServiceImpl.this.entityPath).append(" -DInit.modules=").append(initParam).append(" -DBAP_WORKSPACE=").append(System.getProperty("BAP_WORKSPACE")).append(" -Dtask.id=").append(taskid).append(" -Dfile.encoding=UTF-8");
                                    }
                                    if (instanceAddressList != null && !instanceAddressList.isEmpty()) {
                                        List moduleList;
                                        for (MsModuleIpAdress instanceAddress : instanceAddressList) {
                                            String ipAddress = instanceAddress.getIpadress();
                                            String[] ipInfoArr = ipAddress.split(":");
                                            String serverAddress = ipInfoArr[0];
                                            String serverPort = ipInfoArr[1];
                                            StringBuffer startCmd = new StringBuffer();
                                            startCmd.append(jdkPath).append(jvmParams);
                                            startCmd.append(" -Dsupfusion.cloud.registry.ip=").append(serverAddress);
                                            startCmd.append(" -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=").append("1").append(serverPort);
                                            StringBuffer endParams = new StringBuffer();
                                            endParams.append(" --server.port=").append(serverPort).append(" --server.address=").append(serverAddress).append(" --ec.log.path=").append(OperateServiceImpl.this.codeLogPath + File.separatorChar + "publish" + File.separatorChar + taskid + ".log").append(" --serviceCode=").append(serviceCode).append(" --spring.profiles.active=").append(OperateServiceImpl.this.profilesActive);
                                            StringBuffer jarPath = new StringBuffer();
                                            jarPath.append(OperateServiceImpl.this.moduleServerPath.replaceAll("//", "\\\\")).append("/").append(serviceCode).append("/").append(serverAddress).append("-").append(serverPort).append("/").append(serviceCode).append("-1.0.0.jar");
                                            OperateServiceImpl.this.msJarVaild(serviceCode, jarPath.toString());
                                            String cmds = startCmd.append(" ").append(jarPath).append(endParams).toString();
                                            logger.info("\u542f\u52a8\u547d\u4ee4:" + cmds);
                                            publishLogger.info(InternationalResource.get((String)"ec.msModule.service.start", (String)OperateServiceImpl.this.userLanguage, (Object[])new Object[]{serviceName, ipAddress}));
                                            if (null != StartServiceTaskRunner.this.generateTaskRunner) {
                                                OperateServiceImpl.this.startProgressService.startJarAddProgress(serviceIndex, (Integer)OperateServiceImpl.this.serviceModuleSizeList.get(serviceIndex), Boolean.valueOf(false));
                                            } else {
                                                OperateServiceImpl.this.startProgressService.startJarAddProgress(serviceIndex, (Integer)OperateServiceImpl.this.serviceModuleSizeList.get(serviceIndex), Boolean.valueOf(true));
                                            }
                                            currentTimes = System.currentTimeMillis();
                                            String logPath = OperateServiceImpl.this.codeLogPath + File.separator + ".." + File.separator + serviceCode + File.separator + serviceCode + ".log";
                                            File logFile = new File(logPath);
                                            logPath = logFile.getCanonicalPath();
                                            currentInstanceMap.remove(ipAddress);
                                            currentInstanceMap.put(ipAddress, new ConcurrentHashMap(4));
                                            if (startService.isNeedStartUp()) {
                                                process = Runtime.getRuntime().exec(cmds);
                                            }
                                            OperateServiceImpl.this.getRuntimePid(process, serviceCode, instanceAddress);
                                            if (!OperateServiceImpl.this.exeProcess(process, ipAddress)) {
                                                String startError = InternationalResource.get((String)"ec.msModule.service.startError", (String)OperateServiceImpl.this.userLanguage);
                                                throw new BAPException(startError + " " + logPath + " " + InternationalResource.get((String)"foundation.base.audit", (String)OperateServiceImpl.this.userLanguage));
                                            }
                                            serviceModule.setStatus(Integer.valueOf(1));
                                            OperateServiceImpl.this.msModuleService.save((Object)serviceModule);
                                            OperateServiceImpl.this.msModuleService.deleteMsIpByIp(instanceAddress.getIpadress());
                                            instanceAddress.setCode(UUID.randomUUID().toString());
                                            instanceAddress.setValid(true);
                                            instanceAddress.setMsModule(serviceModule);
                                            instanceAddress.setStatus(Integer.valueOf(1));
                                            instanceAddress.setPublishTime(new Date());
                                            instanceAddress.setIpadress(instanceAddress.getIpadress());
                                            OperateServiceImpl.this.msModuleService.createMsIp(instanceAddress);
                                            String startSuccess = InternationalResource.get((String)"ec.msModule.service.startSuccess", (String)OperateServiceImpl.this.userLanguage);
                                            publishLogger.info("FINISH_" + serviceCode + " " + serviceName + " " + startSuccess, (Object)((System.currentTimeMillis() - currentTimes) / 1000L));
                                            OperateServiceImpl.this.startProgressService.getServiceFinishFlagMap().put(serviceIndex, true);
                                            ((Thread)OperateServiceImpl.this.startProgressService.getThreadMap().get(serviceIndex)).join();
                                        }
                                        if (null != StartServiceTaskRunner.this.generateTaskRunner && null != StartServiceTaskRunner.this.generateTaskRunner && CollectionUtils.isNotEmpty((Collection)(moduleList = startService.getModuleList()))) {
                                            moduleList.forEach(module -> OperateServiceImpl.this.bapGenerateService.syncModuleCompanyRef(module));
                                        }
                                    }
                                    if (CommonUtils.isNotEmpty((String)moduleSelectsIds)) {
                                        String[] modules;
                                        for (String moduleCode : modules = moduleSelectsIds.split(",")) {
                                            if (startService.isNeedStartUp()) {
                                                Module module2 = OperateServiceImpl.this.msModuleService.getModule(moduleCode);
                                                OperateServiceImpl.this.msModuleService.updateDeploymentLog(module2);
                                            }
                                            logger.info("-------\u6dfb\u52a0SYS_DEPLOYMENT_LOG\u4fe1\u606f\u6210\u529f");
                                        }
                                    }
                                }
                                catch (Exception e) {
                                    if (null != process) {
                                        process.destroy();
                                        publishLogger.info(InternationalResource.get((String)"ec.msModule.service.close", (String)OperateServiceImpl.this.userLanguage));
                                    }
                                    OperateServiceImpl.this.startException = e;
                                    if (null != StartServiceTaskRunner.this.generateTaskRunner) {
                                        StartServiceTaskRunner.this.generateTaskRunner.shutdown();
                                    }
                                    StartServiceTaskRunner.this.shutdown();
                                    logger.error(e.getMessage(), (Throwable)e);
                                    throw new BAPException((Throwable)e);
                                }
                                finally {
                                    org.jboss.logging.MDC.remove((String)"bapEcTask");
                                }
                            });
                            continue;
                        }
                        StartServiceTaskRunner.this.hasPulled = true;
                    }
                    catch (InterruptedException interruptedException) {}
                }
                StartServiceTaskRunner.this.batchStartThreadPool.shutdown();
                try {
                    StartServiceTaskRunner.this.batchStartThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                StartServiceTaskRunner.this.inUse = false;
            }
        }
    }

    private class PackageTaskRunner {
        private final BlockingQueue<GeneratePackageTask> packageChannel;
        private volatile boolean inUse = true;
        private volatile boolean hasPulled = false;
        private volatile Thread packageThread;
        private final GenerateTaskRunner generateTaskRunner;
        private final StartServiceTaskRunner startServiceTaskRunner;
        private final List<Map<String, Object>> list;

        public PackageTaskRunner(List<Map<String, Object>> list, GenerateTaskRunner generateTaskRunner, BlockingQueue<GeneratePackageTask> channel) {
            this.list = list;
            this.generateTaskRunner = generateTaskRunner;
            this.startServiceTaskRunner = new StartServiceTaskRunner(generateTaskRunner, this);
            this.packageChannel = channel;
            this.packageThread = new PackageThread("package");
        }

        public void init() {
            Thread t = this.packageThread;
            if (null != t) {
                t.start();
                this.startServiceTaskRunner.init();
            }
        }

        public void submit(GeneratePackageTask task) throws InterruptedException {
            this.packageChannel.put(task);
        }

        public void shutdown() {
            this.inUse = false;
            Thread t = this.packageThread;
            if (null != t) {
                t.interrupt();
            }
        }

        public boolean getInUse() {
            return this.inUse;
        }

        public Thread getPackageThread() {
            return this.packageThread;
        }

        public StartServiceTaskRunner getStartServiceTaskRunner() {
            return this.startServiceTaskRunner;
        }

        class PackageThread
        extends Thread {
            ExecutorService batchPackageThreadPool;

            public PackageThread(String name) {
                super(name);
                this.batchPackageThreadPool = Executors.newFixedThreadPool(CURRENTCPUCORES);
            }

            @Override
            public void run() {
                while (true) {
                    if (!PackageTaskRunner.this.inUse || !PackageTaskRunner.this.generateTaskRunner.getInUse() && PackageTaskRunner.this.packageChannel.isEmpty() && PackageTaskRunner.this.hasPulled) break;
                    this.batchPackage();
                }
                this.batchPackageThreadPool.shutdown();
                try {
                    this.batchPackageThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                PackageTaskRunner.this.inUse = false;
            }

            private void batchPackage() {
                try {
                    PackageTaskRunner.this.hasPulled = false;
                    GeneratePackageTask generatePackageTask = (GeneratePackageTask)PackageTaskRunner.this.packageChannel.poll(500L, TimeUnit.MILLISECONDS);
                    if (null != generatePackageTask) {
                        this.batchPackageThreadPool.execute(() -> {
                            try {
                                PackageTaskRunner.this.hasPulled = true;
                                Module module = generatePackageTask.getModule();
                                org.jboss.logging.MDC.put((String)"bapEcTask", EcUtils.deployTask.get("bapEcTask"));
                                org.jboss.logging.MDC.put((String)"BAP.EC.Module", (Object)module.getCode());
                                Set relationModules = (Set)OperateServiceImpl.this.moduleRelations.get(module.getCode());
                                Set referenceModules = (Set)OperateServiceImpl.this.moduleReferences.get(module.getCode());
                                while ((null != relationModules && !OperateServiceImpl.this.packagedSet.containsAll(relationModules) || null != referenceModules && !OperateServiceImpl.this.packagedSet.containsAll(referenceModules)) && PackageTaskRunner.this.inUse) {
                                    Thread.sleep(500L);
                                }
                                logger.info("\u5f00\u59cb\u6253\u5305" + module.getCode());
                                String codeName = InternationalResource.get((String)module.getName());
                                publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.startPackage", (String)OperateServiceImpl.this.userLanguage));
                                long currentTimes = System.currentTimeMillis();
                                try {
                                    OperateServiceImpl.this.bapGenerateService.buildPackage(module, null);
                                }
                                catch (Exception e) {
                                    logger.error(e.getMessage(), (Throwable)e);
                                    throw new BAPException(e.getMessage(), (Throwable)e);
                                }
                                String ocdpath = OperateServiceImpl.this.generatePath + File.separator + module.getCode() + File.separator + OCD_PATH;
                                File ocdfile = new File(ocdpath);
                                if (ocdfile.exists()) {
                                    OperateServiceImpl.this.registerOcdFile(ocdfile.getPath());
                                }
                                if (OperateServiceImpl.this.moduleJarVaild(module.getCode()).booleanValue()) {
                                    String logCodePath = OperateServiceImpl.this.codeLogPath + "/servicemanager.log";
                                    throw new BAPException(InternationalResource.get((String)"ec.msModule.module.errorPackage", (String)OperateServiceImpl.this.userLanguage) + logCodePath + InternationalResource.get((String)"foundation.base.audit", (String)OperateServiceImpl.this.userLanguage));
                                }
                                OperateServiceImpl.this.packagedSet.add(module.getCode());
                                publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.packageSuccess", (String)OperateServiceImpl.this.userLanguage), (Object)((System.currentTimeMillis() - currentTimes) / 1000L));
                                logger.info("\u7ed3\u675f\u6253\u5305" + module.getCode());
                                Iterator iterator = ((Set)OperateServiceImpl.this.moduleServiceIndexMap.get(module.getCode())).iterator();
                                while (iterator.hasNext()) {
                                    int serviceIndex = (Integer)iterator.next();
                                    OperateServiceImpl.this.startProgressService.addProgress(1.0, StartProgressService.Stage.Package);
                                    if (!OperateServiceImpl.this.packagedSet.containsAll((Collection)OperateServiceImpl.this.serviceModuleList.get(serviceIndex))) continue;
                                    Map serviceMap = (Map)PackageTaskRunner.this.list.get(serviceIndex);
                                    String serviceCode = serviceMap.get("code") == null ? "" : serviceMap.get("code").toString();
                                    String moduleSelectsIds = serviceMap.get("moduleSelectsIds") == null ? "" : serviceMap.get("moduleSelectsIds").toString();
                                    ArrayList<Module> moduleTempList = new ArrayList<Module>();
                                    for (String moduleCode : moduleSelectsIds.split(",")) {
                                        moduleTempList.add(OperateServiceImpl.this.moduleService.getModule(moduleCode));
                                    }
                                    String serviceName = serviceMap.get("name") == null ? "" : serviceMap.get("name").toString();
                                    String addressResult = JSONArray.toJSONString(serviceMap.get("adressResult"));
                                    List instanceAddressList = JSON.parseArray((String)addressResult, MsModuleIpAdress.class);
                                    boolean isNeedStartUp = true;
                                    if ("2".equals(serviceMap.get("deployType").toString())) {
                                        isNeedStartUp = false;
                                    }
                                    StartService startService = new StartService(serviceCode, serviceName, moduleSelectsIds, moduleTempList, instanceAddressList, Integer.valueOf(serviceIndex), isNeedStartUp);
                                    PackageTaskRunner.this.startServiceTaskRunner.submit(startService);
                                }
                                org.jboss.logging.MDC.remove((String)"bapEcTask");
                                org.jboss.logging.MDC.remove((String)"BAP.EC.Module");
                            }
                            catch (Exception e) {
                                logger.error(e.getMessage(), (Throwable)e);
                                OperateServiceImpl.this.startException = e;
                                this.batchPackageThreadPool.shutdown();
                                PackageTaskRunner.this.generateTaskRunner.shutdown();
                                PackageTaskRunner.this.shutdown();
                                throw new BAPException(e.getMessage(), (Throwable)e);
                            }
                        });
                    } else {
                        PackageTaskRunner.this.hasPulled = true;
                    }
                }
                catch (InterruptedException e) {
                    PackageTaskRunner.this.packageThread = null;
                }
            }
        }
    }

    private class GenerateTaskRunner {
        private final BlockingQueue<GeneratePackageTask> generateChannel;
        private volatile boolean inUse = true;
        private volatile Thread generateThread;
        private final PackageTaskRunner packageTaskRunner;
        private ExecutorService threadPoolExecutor1 = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        public GenerateTaskRunner(List<Map<String, Object>> list, final BlockingQueue<GeneratePackageTask> generateChannel, BlockingQueue<GeneratePackageTask> packageChannel) {
            Runnable originalRunnable = new Runnable(){

                @Override
                public void run() {
                    while (GenerateTaskRunner.this.inUse && !generateChannel.isEmpty()) {
                        try {
                            GeneratePackageTask generatePackageTask = (GeneratePackageTask)generateChannel.take();
                            Module module = generatePackageTask.getModule();
                            org.jboss.logging.MDC.put((String)"bapEcTask", EcUtils.deployTask.get("bapEcTask"));
                            org.jboss.logging.MDC.put((String)"BAP.EC.Module", (Object)module.getCode());
                            if (!OperateServiceImpl.this.generatedSet.contains(module.getCode())) {
                                logger.info("\u5f00\u59cb\u751f\u6210" + module.getCode());
                                ArrayList<DeploymentTask> cloneDeploymentTasks = new ArrayList<DeploymentTask>();
                                DeploymentTask deploymentTask = new DeploymentTask(module.getCode(), 16);
                                DeploymentTask task = OperateServiceImpl.this.manager.cloneDeploymentTask(deploymentTask);
                                cloneDeploymentTasks.add(task);
                                String codeName = InternationalResource.get((String)module.getName());
                                if (null != module.getIsProto() && module.getIsProto().booleanValue()) {
                                    throw new BAPException(codeName + InternationalResource.get((String)"ec.msModule.module.isProto", (String)OperateServiceImpl.this.userLanguage));
                                }
                                publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.generate", (String)OperateServiceImpl.this.userLanguage));
                                long currentTimes = System.currentTimeMillis();
                                OperateServiceImpl.this.singleDeployService.doGenerateAll(cloneDeploymentTasks);
                                GenerateTaskRunner.this.threadPoolExecutor1.execute(() -> OperateServiceImpl.this.ecDataSynchronizeService.synchronizeEcDataFromDevToRumtime(module.getCode()));
                                OperateServiceImpl.this.generatedSet.add(module.getCode());
                                publishLogger.info(codeName + " " + InternationalResource.get((String)"ec.msModule.module.generateTime", (String)OperateServiceImpl.this.userLanguage), (Object)((System.currentTimeMillis() - currentTimes) / 1000L));
                                GenerateTaskRunner.this.packageTaskRunner.submit(generatePackageTask);
                            }
                            org.jboss.logging.MDC.remove((String)"bapEcTask");
                            org.jboss.logging.MDC.remove((String)"BAP.EC.Module");
                        }
                        catch (Exception e) {
                            logger.error(e.getMessage(), (Throwable)e);
                            GenerateTaskRunner.this.shutdown();
                            OperateServiceImpl.this.startException = e;
                            throw new BAPException(e.getMessage(), (Throwable)e);
                        }
                    }
                    GenerateTaskRunner.this.inUse = false;
                }
            };
            DelegatingSecurityContextRunnable wrappedRunnable = new DelegatingSecurityContextRunnable(originalRunnable);
            this.generateChannel = generateChannel;
            this.generateThread = new GenerateThread("generate", (Runnable)wrappedRunnable);
            this.packageTaskRunner = new PackageTaskRunner(list, this, packageChannel);
        }

        public GenerateTaskRunner(List<Map<String, Object>> list) {
            this(list, new LinkedBlockingQueue<GeneratePackageTask>(), new LinkedBlockingQueue<GeneratePackageTask>());
        }

        public void init() {
            Thread t = this.generateThread;
            if (null != t) {
                t.start();
                this.packageTaskRunner.init();
            }
        }

        public void submit(GeneratePackageTask task) throws InterruptedException {
            this.generateChannel.put(task);
        }

        public void shutdown() {
            this.inUse = false;
            Thread t = this.generateThread;
            if (null != t) {
                t.interrupt();
            }
        }

        public PackageTaskRunner getPackageTaskRunner() {
            return this.packageTaskRunner;
        }

        public boolean getInUse() {
            return this.inUse;
        }

        class GenerateThread
        extends Thread {
            public GenerateThread(String name, Runnable runnable) {
                super(runnable, name);
            }
        }
    }

    class StreamGobbler
    extends Thread {
        InputStream is;
        String type;

        public StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            InputStreamReader isr = null;
            try {
                isr = new InputStreamReader(this.is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                }
            }
            catch (Exception ioe) {
                logger.error(ioe.getMessage(), (Throwable)ioe);
                if (null != isr) {
                    try {
                        isr.close();
                    }
                    catch (IOException e) {
                        logger.error(e.getMessage(), (Throwable)e);
                    }
                }
            }
        }
    }
}

