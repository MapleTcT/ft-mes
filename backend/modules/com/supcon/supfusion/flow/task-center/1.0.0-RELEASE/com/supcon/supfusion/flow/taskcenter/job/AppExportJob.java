/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.job;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.supcon.supfusion.flow.common.dto.ImportExportDTO;
import com.supcon.supfusion.flow.common.dto.TransportDiagramDTO;
import com.supcon.supfusion.flow.common.enumeration.AppExportStatus;
import com.supcon.supfusion.flow.common.po.DiagramPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.ZipUtils;
import com.supcon.supfusion.flow.dao.DiagramMapper;
import com.supcon.supfusion.flow.taskcenter.component.RedisUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zhuangmh
 * @date: 2020年08月25日 上午10:14:04
 */
@Component
@Slf4j
public class AppExportJob extends JobExecutor<ImportExportDTO> {
    
    @Autowired
    private DiagramMapper diagramMapper;
    @Autowired
    private RedisUtils redisUtils;
    /**
     * <ul>
     *  异步app导出流程组态数据
     * </ul>
     */
    @Override
    public void submit(ImportExportDTO importExportDTO) {
        JOB_THREAD_POOL.execute(() -> {
            String path = String.format(Constants.APP_EXPORT_FILE_PATH, importExportDTO.getTaskId());
            File file = new File(path);
            try {
                List<DiagramPO> diagrams = diagramMapper.selectListByApp(importExportDTO.getAppId());
                // 流程组态数据写入文件
                FileUtils.write(file, new Gson().toJson(new TransportDiagramDTO(diagrams)), Constants.ENCODE_UTF8);
                String destPath = String.format(Constants.APP_EXPORT_ZIP_PATH, importExportDTO.getTaskId());
                // 组态文件压缩到zip文件
                boolean result = ZipUtils.zip(destPath, Collections.singletonList(file));
                String status = result ? AppExportStatus.EXPORT_SUCCESS.getStatus() + "" : AppExportStatus.EXPORT_FAIL.getStatus() + "";
                redisUtils.setStringValue(importExportDTO.getTaskId(), status);
            } catch (Exception e) {
                redisUtils.setStringValue(importExportDTO.getTaskId(), AppExportStatus.EXPORT_FAIL.getStatus() + "");
                log.error("app({})流程数据导出到文件异常", importExportDTO.getAppId(), e);
            } finally {
                // 删除组态数据文件(.data文件)
                file.delete();
            }
        });
    }

}
