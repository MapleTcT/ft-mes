package com.supcon.supfusion.rbac.api;

import com.supcon.supfusion.rbac.api.Constants.Constants;
import com.supcon.supfusion.rbac.api.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.supcon.supfusion.rbac.api.Constants.Constants.API_PREFIX;

/**
 *
 * 角色服务相关接口
 *
 * RPC内部接口
 * <ul>
 *     <li>FeignClient的值必须和spring.application.name的值一致</li>
 *     <li>内部接口统一梠式为：/service-api/{spring.application.name}/{version}/**</li>
 * </ul>
 *
 * @author
 * @date 20-5-11 下午2:14
 */
@Validated
@FeignClient(name = "rbac",contextId = "permission")
public interface IPermissionApiService {

    /**
     * @description: 获取数据权限SQL
     * @param: userId
     * @param: menuOperateCode
     * @param: currentCompanyId
     * @param: entityTable 实体表
     * @return: java.lang.String
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.PERMISSION + "/generateBaseModelSql")
    @ResponseBody
    String generateBaseModelSql(@RequestParam("userId") Long userId,@RequestParam("menuOperateCode") String menuOperateCode,@RequestParam("currentCompanyId") Long currentCompanyId,@RequestParam("entityTable") String entityTable);

    /**
     * @description: 根据flowkey和activityCode删除流程权限
     * @param: flowKey 流程key
     * @param: activityCodes
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @DeleteMapping(API_PREFIX + Constants.PERMISSION + "/deleteFlowPermission")
    @ResponseBody
    void deleteFlowPermission(@RequestParam("flowKey") String flowKey,@RequestParam("activityCodes") List<String> activityCodes);

    /**
     * @description: 根据flowkey和activityCode删除流程人员关联
     * @param: flowKey
     * @param: activityCodes
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @DeleteMapping(API_PREFIX + Constants.PERMISSION + "/deleteFlowPermissionStaff")
    @ResponseBody
    void deleteFlowPermissionStaff(@RequestParam("flowKey") String flowKey,@RequestParam("activityCodes") List<String> activityCodes);

    /**
     * @description: 根据flowkey和activityCode删除流程岗位关联
     * @param: flowKey
     * @param: activityCodes
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @DeleteMapping(API_PREFIX + Constants.PERMISSION + "/deleteFlowPermissionPosition")
    @ResponseBody
    void deleteFlowPermissionPosition(@RequestParam("flowKey") String flowKey,@RequestParam("activityCodes") List<String> activityCodes);

    /**
     * @description: 新增流程权限
     * @param: flowPermissionDTO
     * @return: com.supcon.supfusion.rbac.api.dto.FlowPermissionDTO
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PostMapping(API_PREFIX + Constants.PERMISSION + "/saveFlowPermission")
    @ResponseBody
    FlowPermissionDTO saveFlowPermission(@RequestBody FlowPermissionDTO flowPermissionDTO);

    /**
     * @description: 新增流程岗位关联
     * @param: flowPermissionPositionDTO
     * @return: com.supcon.supfusion.rbac.api.dto.FlowPermissionPositionDTO
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PostMapping(API_PREFIX + Constants.PERMISSION + "/saveFlowPermissionPosition")
    @ResponseBody
    FlowPermissionPositionDTO saveFlowPermissionPosition(@RequestBody FlowPermissionPositionDTO flowPermissionPositionDTO);

    /**
     * @description: 新增流程人员关联
     * @param: flowPermissionStaffDTO
     * @return: com.supcon.supfusion.rbac.api.dto.FlowPermissionStaffDTO
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PostMapping(API_PREFIX + Constants.PERMISSION + "/saveFlowPermissionStaff")
    @ResponseBody
    FlowPermissionStaffDTO saveFlowPermissionStaff(@RequestBody FlowPermissionStaffDTO flowPermissionStaffDTO);


    @DeleteMapping(API_PREFIX + Constants.PERMISSION + "/deleteFlowPermissionById")
    @ResponseBody
    void deleteFlowPermissionById(@RequestBody List<Long> ids);

    @DeleteMapping(API_PREFIX + Constants.PERMISSION + "/deleteFlowPermissionPositionById")
    @ResponseBody
    void deleteFlowPermissionPositionById(@RequestBody List<Long> ids);

    @DeleteMapping(API_PREFIX + Constants.PERMISSION + "/deleteFlowPermissionStaffById")
    @ResponseBody
    void deleteFlowPermissionStaffById(@RequestBody List<Long> ids);

    @PutMapping(API_PREFIX + Constants.PERMISSION + "/refreshRedis")
    @ResponseBody
    void refreshRedis(@RequestBody List<String> apps);

    @GetMapping(API_PREFIX + Constants.PERMISSION + "/refreshRedisByUser")
    @ResponseBody
    Map<String, List<String>> refreshRedisByUser(@RequestParam("userId") Long userId, @RequestParam("cid") Long cid, @RequestParam("tenantId") String tenantId, @RequestParam("method") String method);

}
