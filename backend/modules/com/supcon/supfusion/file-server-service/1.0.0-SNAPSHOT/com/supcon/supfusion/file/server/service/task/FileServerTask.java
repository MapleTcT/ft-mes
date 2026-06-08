package com.supcon.supfusion.file.server.service.task;

import com.alibaba.fastjson.JSON;
import com.supcon.supfusion.file.server.common.constants.Constants;
import com.supcon.supfusion.file.server.common.utils.SystemUtil;
import com.supcon.supfusion.file.server.common.utils.TenantUtil;
import com.supcon.supfusion.file.server.dao.DocumentDao;
import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import com.supcon.supfusion.file.server.service.FileConvertService;
import com.supcon.supfusion.file.server.service.FileDaoService;
import com.supcon.supfusion.file.server.service.FileService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfo;
import com.supcon.supfusion.framework.cloud.common.events.TenantInfoLocalStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class FileServerTask {

    @Autowired
    private FileService fileService;
    @Autowired
    private DocumentDao documentDao;
    @Autowired
    private FileConvertService fileConvertService;
    @Autowired
    private FileDaoService fileDaoService;

    //清空一天前创建的临时目录
    @Scheduled(cron = "10 0 0 * * ?")
    public void clearTempFolderOfOneDay() throws Exception {
        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
            Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
            for (TenantInfo tenantInfo : tenantInfoSet) {
                RpcContext rpcContext = RpcContext.getContext();
                rpcContext.setTenantId(tenantInfo.getId());
                fileService.removeFolderOfOneDay(TenantUtil.getTenantId(), Constants.TEMP_FOLDER);
            }
        } else {
            fileService.removeFolderOfOneDay(TenantUtil.getTenantId(), Constants.TEMP_FOLDER);
        }
    }

//    //定时任务扫描数据库表中 还没有文件转换的记录查询出之后进行文件预览转换
//    @Scheduled(cron = "*/30 * * * * ?")
////    @Bean
//    public void fileConvertTask() {
//        log.info("------------>开始查询待附件预览转换记录");
//        if (Constants.LINUX.equalsIgnoreCase(SystemUtil.getOS())) {
//            Set<TenantInfo> tenantInfoSet = TenantInfoLocalStorage.getAll();
//            log.info("当前租户信息,tenantInfoSet:{}", JSON.toJSONString(tenantInfoSet));
//            for (TenantInfo tenantInfo : tenantInfoSet) {
//                RpcContext rpcContext = RpcContext.getContext();
//                rpcContext.setTenantId(tenantInfo.getId());
//                commonFileConvertTask();
//            }
//        } else {
//            commonFileConvertTask();
//        }
//
//    }
//
//    public void commonFileConvertTask() {
//        List<DocumentPO> documentPOS = documentDao.selectAllByIsNotConvert();
//        if (documentPOS != null && documentPOS.size() > 0) {
//            documentPOS.forEach(documentPO -> {
//                try {
//                    Thread.sleep(100L);
//                } catch (InterruptedException e) {
//                    log.error("初始转换发生错误:{}",e.getMessage());
//                }
//                fileConvertService.fileConvert(documentPO);
//            });
//        }
//        log.info("初始化转换完成");
//    }

    //删除已是删除状态的文件  现在不允许删除
//    @Scheduled(cron = "0 0 0/1 * *  ?")
//    @Scheduled(cron = "0/5 * * * *  ?")
    public void deleteFiles() throws Exception {
        log.info("开始执行删除附件任务");
        List<DocumentPO> documentPOS = documentDao.selectAllByValidIsFalse();
        if (documentPOS != null && documentPOS.size() > 0) {
            documentPOS.forEach(documentPO -> {
                Set<TenantInfo> tenantInfos = TenantInfoLocalStorage.getAll();
                if (documentPO.getFilePath() != null && !documentPO.getFilePath().equals("")) {
                    if (tenantInfos != null && tenantInfos.size() > 1) {
                        deleteAllTenant(documentPO, tenantInfos);
                    } else if (tenantInfos != null && tenantInfos.size() == 1) {
                        deleteAllTenant(documentPO, tenantInfos);
                        //删除默认租户
                        deleteDefaultTanent(documentPO);
                    } else {
                        //删除默认租户
                        deleteDefaultTanent(documentPO);
                    }
                }
            });
        }
    }

    private void deleteDefaultTanent(DocumentPO documentPO) {
        String tenantId = TenantUtil.getTenantId();
        try {
            fileService.removeFolder(tenantId, documentPO.getFilePath());
            log.info("附件：" + documentPO.getFilePath() + "删除成功！");
        } catch (Exception e) {
            log.error("delete " + tenantId + " file is fail ,filepath:" + documentPO.getFilePath());
        }
        documentDao.deleteByDocumentPOId(documentPO.getId());
        log.info("附件：" + documentPO.getFilePath() + "数据库数据删除成功！");
    }

    private void deleteAllTenant(DocumentPO documentPO, Set<TenantInfo> tenantInfos) {
        tenantInfos.forEach(tenantInfo -> {
            try {
                fileService.removeFolder(tenantInfo.getId(), documentPO.getFilePath());
                log.info("附件：" + documentPO.getFilePath() + "删除成功！");
            } catch (Exception e) {
                log.error("delete " + tenantInfo.getId() + " file is fail ,filepath:" + documentPO.getFilePath());
            }
            documentDao.deleteByDocumentPOId(documentPO.getId());
            log.info("附件：" + documentPO.getFilePath() + "数据库数据删除成功！");
        });
    }

    //定时任务删除 临时目录文件

    //@Scheduled(cron = "0 0 0 */1 * ?")
    public void clearNoUseTempFolder() throws Exception {
        //获取所有租户
        String tenantId = RpcContext.getContext().getTenantId();
        tenantId = tenantId != null && tenantId != "" ? tenantId : Constants.DEFAULT_TENANT_ID;
        fileService.removeFolder(tenantId, Constants.TEMP_FOLDER);
    }

    private Long getFileCreateTime(String filePath) {
        File file = new File(filePath);
        try {
            Path path = Paths.get(filePath);
            BasicFileAttributeView basicview = Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            BasicFileAttributes attr = basicview.readAttributes();
            return attr.creationTime().toMillis();
        } catch (Exception e) {
            e.printStackTrace();
            return file.lastModified();
        }
    }


}