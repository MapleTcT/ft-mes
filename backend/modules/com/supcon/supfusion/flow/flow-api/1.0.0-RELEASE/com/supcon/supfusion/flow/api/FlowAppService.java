/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;

import feign.Response;

/**
 * @author: zhuangmh
 * @date: 2020年8月27日 下午4:40:55
 */
@FeignClient(name = "flow-service")
@ServiceApi(path = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "flow-service")
public interface FlowAppService {

    /**
     * 删除app同时删除流程组态数据
     * @param appId
     */
    @DeleteMapping
    void delete(String appId);
    
    /**
     * App下载, 导出已发布的流程
     * @param appId
     */
    Response download(String appId);
    
    /**
     * 开始导出流程, 导出逻辑异步处理
     * @param appId
     */
    void startExport(String appId);
    
    /**
     * 获取导出状态
     * @param appId
     * @return {@link com.supcon.supfusion.flow.common.enumeration.AppExportStatus}
     */
    String getExportStatus(String appId);
    
    /**
     * app安装
     * @param appId
     * @param diagramJson 流程组态JSON
     */
    void install(String appId, String diagramJson);
    
    /**
    * 获取安装状态
    * @param appId
    * @return {@link com.supcon.supfusion.flow.common.enumeration.AppImportStatus}
    */
   String getImportStatus(String appId);
   
   /**
    * App 升级
    * @param appId
    * @param composeDiagram 组态JSON数据
    */
   public void upgrade(String appId, String composeDiagramJson);
   
   
}
