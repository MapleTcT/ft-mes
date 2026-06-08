/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.rpc;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.supcon.supfusion.flow.api.AppServiceApi;
import com.supcon.supfusion.flow.api.dto.DiagramPublishDTO;
import com.supcon.supfusion.flow.common.dto.ComposeDiagramDTO;
import com.supcon.supfusion.flow.common.dto.ImportExportDTO;
import com.supcon.supfusion.flow.common.dto.TransportDiagramDTO;
import com.supcon.supfusion.flow.common.enumeration.AppExportStatus;
import com.supcon.supfusion.flow.common.enumeration.AppImportStatus;
import com.supcon.supfusion.flow.common.enumeration.AppUpgradeStatus;
import com.supcon.supfusion.flow.common.enumeration.DiagramStatusEnum;
import com.supcon.supfusion.flow.common.enumeration.FlowErrorEnum;
import com.supcon.supfusion.flow.common.exception.IllegalParameterException;
import com.supcon.supfusion.flow.common.exception.InvalidBpmnModelException;
import com.supcon.supfusion.flow.common.exception.StatusAbnormalException;
import com.supcon.supfusion.flow.common.po.DiagramContentPO;
import com.supcon.supfusion.flow.common.po.DiagramPO;
import com.supcon.supfusion.flow.common.util.CodeGenerator;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.ProxyUtils;
import com.supcon.supfusion.flow.common.util.ZipUtils;
import com.supcon.supfusion.flow.dao.DiagramContentMapper;
import com.supcon.supfusion.flow.dao.DiagramMapper;
import com.supcon.supfusion.flow.taskcenter.component.RedisUtils;
import com.supcon.supfusion.flow.taskcenter.job.AppExportJob;
import com.supcon.supfusion.flow.taskcenter.service.DiagramService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年8月25日 上午8:57:13
 */
@ServiceApiService
@Slf4j
public class AppService implements AppServiceApi {

    @Autowired
    private DiagramMapper diagramMapper;
    @Autowired
    private DiagramContentMapper diagramContentMapper;
    @Autowired
    private AppExportJob appExportJob;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private DiagramService diagramService;
    
    private static final String PARAM_APPNAMES = "appNames";
    
    private static final String PARAM_STATUS = "status";
    
    /**
     * 删除app同时删除流程组态数据
     * @param appId
     */
    public void delete(String appId) {
        LambdaQueryWrapper<DiagramPO> deleteWrapper = Wrappers.<DiagramPO>lambdaQuery()
                .eq(DiagramPO::getAppId, appId)
                .eq(DiagramPO::getValid, Constants.VALID);
        String tenantId = RpcContext.getContext().getTenantId();
        if (StringUtils.isNotEmpty(tenantId)) {
            deleteWrapper.eq(DiagramPO::getTenantId, tenantId);
        }
        diagramMapper.delete(deleteWrapper);
    }
    
    /**
     * 开始导出流程, 导出逻辑异步处理
     */
    
    public String flowDataExport(Map<String, List<String>> appMap) {
        List<String> apps = appMap.get(PARAM_APPNAMES);
        if (apps == null || apps.isEmpty()) {
            throw new IllegalParameterException(FlowErrorEnum.PARAMETER_APPID_ERROR);
        }
        // 后续要根据这个taskId来获取导出状态和下载的文件
        String taskId = CodeGenerator.generateUUID().toString();
        // 设置状态为开始导出
        redisUtils.setStringValue(taskId, AppExportStatus.EXPORTING.getStatus() + "");
        // 异步导出
        appExportJob.submit(new ImportExportDTO(taskId, apps.get(0)));
        JsonObject result = new JsonObject();
        result.addProperty(Constants.DATA, taskId);
        return result.toString();
    }
    
    /**
     * @see com.supcon.supfusion.flow.api.AppServiceApi.getExportStatus(String)
     */
    public String getExportStatus(String taskId) {
        String taskStatus = redisUtils.getStringValue(taskId);
        int status = Optional.ofNullable(taskStatus).map(s -> Integer.parseInt(taskStatus)).orElse(AppExportStatus.EXPORT_SUCCESS.getStatus());
        JsonObject result = new JsonObject();
        JsonObject statusJson = new JsonObject();
        statusJson.addProperty(PARAM_STATUS, status);
        result.add(Constants.DATA, statusJson);
        return result.toString();
    }
    
    /**
     * @see com.supcon.supfusion.flow.api.AppServiceApi.download(String, String)
     */
    @Override
    public ResponseEntity<?> download(String taskId, String exportType) {
        File file = new File(String.format(Constants.APP_EXPORT_ZIP_PATH, taskId));
        byte[] bytes = new byte[] {};
        try {
            bytes = FileUtils.readFileToByteArray(file);
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(bytes);
        } catch (IOException e) {
            log.error("app下载, 流程文件流数据读取失败", e);
        }
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
    
    /**
     * @see com.supcon.supfusion.flow.api.AppServiceApi.install(String, MultipartFile)
     */
    public String flowDataImport(String appId, MultipartFile uploadFile) {
        final String taskId = CodeGenerator.generateUUID().toString();
        String lockStatus = redisUtils.getStringValue(taskId);
        if (lockStatus != null && AppImportStatus.IMPORTING.getStatus() == Integer.parseInt(lockStatus)) {
            throw new StatusAbnormalException(FlowErrorEnum.APP_IMPORTING_ERROR);
        }
        String targetForder = Constants.APP_FOLDER_PATH + taskId + "/";
        File folderFile = new File(targetForder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }
        File sourceFile = new File(targetForder + uploadFile.getName());
        JsonObject result = new JsonObject();
        try {
            uploadFile.transferTo(sourceFile);
            // 解压文件放到/tmp/flow/taskId/
            ZipUtils.unzip(sourceFile, targetForder);
            redisUtils.setStringValue(taskId, AppImportStatus.IMPORTING.getStatus() + "");
            // 获取/tmp/flow/taskId/目录下第一个文件, 目前只有一个json文件
            File[] files = new File(targetForder).listFiles();
            for (File file : files) {
                if (file.getName().endsWith(".data")) {
                    // 读取文件内容
                    String diagramJson = FileUtils.readFileToString(file, "utf-8");
                    TransportDiagramDTO diagrams = new Gson().fromJson(diagramJson, TransportDiagramDTO.class);
                    ProxyUtils.getProxyObject(AppService.class).dataInstall(appId, diagrams.getDiagrams());
                    break;
                }
            }
            redisUtils.setStringValue(taskId, AppImportStatus.IMPORT_SUCCESS.getStatus() + "");
            result.addProperty(Constants.DATA, taskId);
        } catch (IOException e) {
            redisUtils.setStringValue(taskId, AppImportStatus.IMPORT_FAIL.getStatus() + "");
            log.error("app安装, 流程导入异常", e);
        } finally {
            sourceFile.delete();
            folderFile.delete();
        }
        return result.toString(); 
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void dataInstall(String appId, List<DiagramPO> diagrams) {
        if (diagrams == null) {
            return;
        }
        // 清空数据
        delete(appId);
        // 插入数据, ID重新生成
        for (DiagramPO diagram : diagrams) {
            create(diagram);
        }
    }
    
    /**
     * @see com.supcon.supfusion.flow.api.AppServiceApi.getImportStatus(String)
     */
    public String getImportStatus(String taskId) {
        String taskStatus = redisUtils.getStringValue(taskId);
        int status = Optional.ofNullable(taskStatus).map(s -> Integer.parseInt(taskStatus)).orElse(AppImportStatus.IMPORT_SUCCESS.getStatus());
        JsonObject result = new JsonObject();
        JsonObject statusJson = new JsonObject();
        statusJson.addProperty(PARAM_STATUS, status);
        result.add(Constants.DATA, statusJson);
        return result.toString();
    }
    
    private void create(DiagramPO diagram) {
        String tenantId = RpcContext.getContext().getTenantId();
        Long cid = UserContext.getUserContext().getCompanyId();
        DiagramContentPO diagramContent = new DiagramContentPO();
        diagramContent.setId(CodeGenerator.generateUUID());
        diagramContent.setDraftJson(diagram.getDraftJson());
        diagramContent.setPublishedJson(diagram.getPublishedJson());
        DiagramPO newDiagramPO = new DiagramPO();
        newDiagramPO.setId(CodeGenerator.generateUUID());
        newDiagramPO.setContentId(diagramContent.getId());
        Integer maxVersion = diagramMapper.selectMaxVersion(diagram.getAppId(), diagram.getProcessKey(), diagram.getTenantId());
        int varsion = maxVersion == null ? 1 : maxVersion.intValue() + 1;
        newDiagramPO.setVersion(varsion);
        newDiagramPO.setAppId(diagram.getAppId());
        newDiagramPO.setCid(cid == null ? 1000 : cid); // 1000为默认公司ID
        newDiagramPO.setMultiCompany(diagram.getMultiCompany());
        newDiagramPO.setProcessKey(diagram.getProcessKey());
        newDiagramPO.setProcessName(diagram.getProcessName());
        newDiagramPO.setStartOnMobile(diagram.getStartOnMobile());
        newDiagramPO.setProcessStatus(DiagramStatusEnum.CREATION.getStatus());
        newDiagramPO.setEnabled(Constants.DISABLED);
        newDiagramPO.setTenantId(tenantId);
        newDiagramPO.setCreator(diagram.getCreator());
        newDiagramPO.setCreatorStaff(diagram.getCreatorStaff());
        diagramMapper.insert(newDiagramPO);
        diagramContentMapper.insert(diagramContent);
    }
    
    /**
     * App 升级
     * @param appId
     * @param composeDiagramJson 组态数据
     */
    public void upgrade(String appId, String composeDiagramJson) {
        final String upgradeLock = AppUpgradeStatus.UPGRADING.getStatus().concat(appId);
        String lockStatus = redisUtils.getStringValue(upgradeLock);
        if (AppUpgradeStatus.UPGRADING.getStatus().equals(lockStatus)) {
            throw new StatusAbnormalException(FlowErrorEnum.APP_UPGRADING_ERROR);
        }
        redisUtils.setStringValue(upgradeLock, AppUpgradeStatus.UPGRADING.getStatus());
        try {
            // TODO 备份数据
            // backupDiagram(appId);
            ComposeDiagramDTO composeDiagram = new Gson().fromJson(composeDiagramJson, ComposeDiagramDTO.class);
            ProxyUtils.getProxyObject(AppService.class).upgradeDiagram(composeDiagram);
            redisUtils.setStringValue(upgradeLock, AppUpgradeStatus.UPGRADE_SUCCESS.getStatus());
        } catch (Exception e) {
            redisUtils.setStringValue(upgradeLock, AppUpgradeStatus.UPGRADE_FAIL.getStatus());
            log.error("app({})升级, 流程升级异常", appId, e);
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void upgradeDiagram(ComposeDiagramDTO composeDiagram) {
        // 插入数据, ID重新生成
        for (DiagramPO diagram : composeDiagram.getDiagrams()) {
            create(diagram);
        }
    }

    /**
     * @throws DocumentException 
     * @throws UnsupportedEncodingException 
     * @throws NumberFormatException 
     * @see com.supcon.supfusion.flow.api.AppServiceApi#publish(com.supcon.supfusion.flow.api.dto.DiagramPublishDTO)
     */
    @Override
    public Result<?> upgradePublish(DiagramPublishDTO publishRequest) {
        try {
            diagramService.upgradePublish(Long.parseLong(publishRequest.getId()), publishRequest.getXml());
            return Result.custom().code(200).build();
        } catch (Exception e) {
            log.error("运行期数据升级失败", e);
            throw new InvalidBpmnModelException(FlowErrorEnum.RUNTIME_UPGRADE_FAIL);
        } 
    }

}
