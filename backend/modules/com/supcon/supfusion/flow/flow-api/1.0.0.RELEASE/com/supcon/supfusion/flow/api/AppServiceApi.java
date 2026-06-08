package com.supcon.supfusion.flow.api;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.supcon.supfusion.flow.api.dto.DiagramPublishDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;

@FeignClient(name = "flow-service")
public interface AppServiceApi {
    
    /**
     * 发送导出请求
     * @param param
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/service-api/flow-service/export")
    String flowDataExport(@RequestBody Map<String, List<String>> param);
    
    /**
     * App下载, 导出已发布的流程
     * @param taskId 导出任务ID
     * @param exportType 导出任务ID
     */
    @RequestMapping(method = RequestMethod.GET, value = "/service-api/flow-service/{taskId}/download")
    ResponseEntity<?> download(@PathVariable("taskId") String taskId, @RequestParam(value = "exportType") String exportType);
    /**
     * app 安装
     * @param appId
     * @param file
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/service-api/flow-service/import",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String flowDataImport(@RequestParam("appId") String appId, @RequestPart(value = "file") MultipartFile file);
    
    @RequestMapping(method = RequestMethod.DELETE, value = "/service-api/flow-service/{appId}")
    void delete(@PathVariable("appId") String appId);
    
    /**
     * 获取导出状态
     * @param taskId 导出任务ID
     * @return 
     *      {"data": {"status": "1"}}
     */
    @RequestMapping(method = RequestMethod.GET, value = "/service-api/flow-service/export/task/{taskId}/status")
    String getExportStatus(@PathVariable("taskId") String taskId);
    
    /**
     * 获取安装状态
     * @param appId
     * @return 
     *       {"data": {"status": "1"}}
     */
     @RequestMapping(method = RequestMethod.GET, value = "/service-api/flow-service/import/task/{taskId}/status")
    String getImportStatus(@PathVariable("taskId") String taskId);
     
     /**
      * 2.7->3.0数据升级接口
      * @param publishRequest
      */
     @RequestMapping(method = RequestMethod.POST, value = "/service-api/flow-service/upgrade/publish")
     Result<?> upgradePublish(@RequestBody DiagramPublishDTO publishRequest);
}
