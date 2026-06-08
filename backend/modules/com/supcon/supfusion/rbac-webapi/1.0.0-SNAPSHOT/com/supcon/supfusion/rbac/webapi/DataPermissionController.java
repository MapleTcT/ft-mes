package com.supcon.supfusion.rbac.webapi;


import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.dao.po.RbacDataResourceGroupPO;
import com.supcon.supfusion.rbac.dao.po.RbacRoleDataPermissionPO;
import com.supcon.supfusion.rbac.dao.po.RbacUserDataPermissionPO;
import com.supcon.supfusion.rbac.service.DataPermissionService;
import com.supcon.supfusion.rbac.service.bo.RoleDataResourceResponseBO;
import com.supcon.supfusion.rbac.service.bo.UserDataResourceResponseBO;
import com.supcon.supfusion.rbac.webapi.vo.datapermission.BapVO;
import com.supcon.supfusion.rbac.webapi.vo.datapermission.DataResouceVO;
import com.supcon.supfusion.rbac.webapi.vo.datapermission.DataResourceGroupVO;
import com.supcon.supfusion.rbac.webapi.vo.datapermission.RoleDataResourceRequestVO;
import com.supcon.supfusion.rbac.webapi.vo.datapermission.RoleDataResourceResponseVO;
import com.supcon.supfusion.rbac.webapi.vo.datapermission.UserDataResourceRequestVO;
import com.supcon.supfusion.rbac.webapi.vo.datapermission.UserDataResourceResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(description = "数据权限", tags = {"数据权限"})
public class DataPermissionController extends BaseController {

    @Autowired
    private DataPermissionService dataPermissionService;
    @Autowired
    private ConfigService configService;
	@Autowired
    @Qualifier("rbacStringRedisTemplate")
    private StringRedisTemplate redisTemplate;
    @GetMapping("/data/resource/groups")
    @ApiOperation(value = "获取资源集")
    public ListResult<DataResourceGroupVO> queryDataResourceGroups() {
        Long cid = UserContext.getUserContext().getCompanyId();
//        cid = 1L;
        log.info("inter-api========/data/resource/groups=getCid***********************************{}",cid);
        List<RbacDataResourceGroupPO> rbacDataResourceGroupPOS = dataPermissionService.getDataResourceGroups(cid);
        log.info("inter-api========/data/resource/groups:getDataResourceGroups***********************************{}",rbacDataResourceGroupPOS);

        List<DataResourceGroupVO> dataResourceGroupVOS = new ArrayList<>();
        if (rbacDataResourceGroupPOS != null) {
            for (RbacDataResourceGroupPO rbacDataResourceGroupPO : rbacDataResourceGroupPOS) {
                DataResourceGroupVO dataResourceGroupVO = new DataResourceGroupVO();
                dataResourceGroupVO.setGroupCode(rbacDataResourceGroupPO.getGroupCode());
                dataResourceGroupVO.setGroupName(rbacDataResourceGroupPO.getGroupName());
                dataResourceGroupVO.setResourceUrl(rbacDataResourceGroupPO.getResourceUrl());
                dataResourceGroupVOS.add(dataResourceGroupVO);
            }
        }
//        log.info("inter-api========/data/resource/groups=response:dataResourceGroupVOS***********************************{}",dataResourceGroupVOS);
        return new ListResult<>(dataResourceGroupVOS);
    }

    @PostMapping("/user/{userId}/data/resource/{groupCode}")
    @ApiOperation(value = "保存用户数据权限")
    public Result saveDataResourceForUser(@ApiParam(value = "userId", required = false) @PathVariable Long userId,
                                          @ApiParam(value = "资源权限编码", required = false) @PathVariable String groupCode,
                                          @RequestBody @Valid @NotNull UserDataResourceRequestVO dataResourceRequestVO) {
        log.info("POST:/user/{userId}/data/resource/{groupCode}=params:userId={},groupCode={},dataResourceRequestVO={}***********************************",userId,groupCode,dataResourceRequestVO);
        Long cid = UserContext.getUserContext().getCompanyId();
//        cid = 1L;
        List<RbacUserDataPermissionPO> rbacUserDataPermissionPOS = new ArrayList<>();
        for (DataResouceVO dataResouceVO : dataResourceRequestVO.getDataResouceVOS()) {
            RbacUserDataPermissionPO rbacUserDataPermissionPO = new RbacUserDataPermissionPO();
            rbacUserDataPermissionPO.setId(IDGenerator.newInstance().generate().longValue());
            rbacUserDataPermissionPO.setPurviewType(1);
            rbacUserDataPermissionPO.setCid(cid);
            rbacUserDataPermissionPO.setGroupCode(groupCode);
            rbacUserDataPermissionPO.setUserId(userId);
            rbacUserDataPermissionPO.setResourceCode(dataResouceVO.getResourceCode());
            rbacUserDataPermissionPO.setResourceName(dataResouceVO.getResourceName());
            rbacUserDataPermissionPO.setResourceType(dataResouceVO.getResourceType());
            rbacUserDataPermissionPOS.add(rbacUserDataPermissionPO);
        }
        log.info("/user/{userId}/data/resource/{groupCode}=saveDataResourceForUser:userId={},groupCode={},isControlled={},rbacUserDataPermissionPOS={}***********************************", userId, groupCode, dataResourceRequestVO.isControlled(), rbacUserDataPermissionPOS);
        dataPermissionService.saveDataResourceForUser(userId, cid, groupCode, dataResourceRequestVO.isControlled(), rbacUserDataPermissionPOS);
        return new Result();
    }

    @GetMapping("/user/{userId}/data/resource/{groupCode}")
    @ApiOperation(value = "查询用户数据权限")
    public Result<UserDataResourceResponseVO> queryDataResourceByUser(@ApiParam(value = "userId", required = false) @PathVariable Long userId,
                                                                      @ApiParam(value = "资源权限编码", required = false) @PathVariable String groupCode) {
        log.info("GET:/user/{userId}/data/resource/{groupCode}====userId={},groupCode={}***********************************",userId,groupCode);
        Long cid = UserContext.getUserContext().getCompanyId();
//        cid = 1L;
        UserDataResourceResponseBO userDataResourceResponseBO = dataPermissionService.queryDataResourceByUser(userId, cid, groupCode);

        UserDataResourceResponseVO userDataResourceResponseVO = new UserDataResourceResponseVO();
        userDataResourceResponseVO.setControlled(userDataResourceResponseBO.isControlled());
        List<DataResouceVO> dataResouceVOS = new ArrayList<>();
        userDataResourceResponseVO.setDataResouceVOS(dataResouceVOS);
        if (userDataResourceResponseBO.getDataResouceVOS() != null) {
            for (RbacUserDataPermissionPO rbacUserDataPermissionPO : userDataResourceResponseBO.getDataResouceVOS()) {
                DataResouceVO dataResouceVO = new DataResouceVO();
                dataResouceVOS.add(dataResouceVO);

                dataResouceVO.setResourceCode(rbacUserDataPermissionPO.getResourceCode());
                dataResouceVO.setResourceName(rbacUserDataPermissionPO.getResourceName());
                dataResouceVO.setResourceType(rbacUserDataPermissionPO.getResourceType());
            }
        }
        log.info("GET:/user/{userId}/data/resource/{groupCode}=response===userDataResourceResponseVO={}***********************************",userDataResourceResponseVO);
        return new Result(userDataResourceResponseVO);
    }

    @PostMapping("/role/{roleId}/data/resource/{groupCode}")
    @ApiOperation(value = "保存角色数据权限")
    public Result saveDataResourceForUser(@ApiParam(value = "roleId", required = false) @PathVariable Long roleId,
                                          @ApiParam(value = "资源权限编码", required = false) @PathVariable String groupCode,
                                          @RequestBody @Valid @NotNull RoleDataResourceRequestVO dataResourceRequestVO) {
        log.info("POST:/role/{roleId}/data/resource/{groupCode}=params:roleId={},groupCode={},dataResourceRequestVO={}***********************************",roleId,groupCode,dataResourceRequestVO);
        Long cid = UserContext.getUserContext().getCompanyId();
//        cid = 1L;
        List<RbacRoleDataPermissionPO> rbacRoleDataPermissionPOS = new ArrayList<>();
        for (DataResouceVO dataResouceVO : dataResourceRequestVO.getDataResouceVOS()) {
            RbacRoleDataPermissionPO rbacRoleDataPermissionPO = new RbacRoleDataPermissionPO();
            rbacRoleDataPermissionPO.setId(IDGenerator.newInstance().generate().longValue());
            rbacRoleDataPermissionPO.setCid(cid);
            rbacRoleDataPermissionPO.setGroupCode(groupCode);
            rbacRoleDataPermissionPO.setRoleId(roleId);
            rbacRoleDataPermissionPO.setResourceCode(dataResouceVO.getResourceCode());
            rbacRoleDataPermissionPO.setResourceName(dataResouceVO.getResourceName());
            rbacRoleDataPermissionPO.setResourceType(dataResouceVO.getResourceType());
            rbacRoleDataPermissionPOS.add(rbacRoleDataPermissionPO);
        }
        log.info("/user/{userId}/data/resource/{groupCode}=saveDataResourceForRole:roleId={},cid={},groupCode={},isControlled={},rbacUserDataPermissionPOS={}=================================", roleId, cid, groupCode, dataResourceRequestVO.isControlled(), rbacRoleDataPermissionPOS);
        dataPermissionService.saveDataResourceForRole(roleId, cid, groupCode, dataResourceRequestVO.isControlled(), rbacRoleDataPermissionPOS);
        return new Result();
    }

    @GetMapping("/role/{roleId}/data/resource/{groupCode}")
    @ApiOperation(value = "获取角色数据权限")
    public Result<RoleDataResourceResponseVO> queryDataResourceByRole(@ApiParam(value = "roleId", required = false) @PathVariable Long roleId,
                                                                      @ApiParam(value = "资源权限编码", required = false) @PathVariable String groupCode) {
        log.info("GET:/role/{roleId}/data/resource/{groupCode}====roleID={},groupCode={}***********************************",roleId,groupCode);
        Long cid = UserContext.getUserContext().getCompanyId();
//        cid = 1L;
        RoleDataResourceResponseBO roleDataResourceResponseBO = dataPermissionService.queryDataResourceByRole(roleId, cid, groupCode);

        RoleDataResourceResponseVO roleDataResourceResponseVO = new RoleDataResourceResponseVO();
        roleDataResourceResponseVO.setControlled(roleDataResourceResponseBO.isControlled());
        List<DataResouceVO> dataResouceVOS = new ArrayList<>();
        roleDataResourceResponseVO.setDataResouceVOS(dataResouceVOS);
        if (roleDataResourceResponseBO.getDataResouceVOS() != null) {
            for (RbacRoleDataPermissionPO rbacRoleDataPermissionPO : roleDataResourceResponseBO.getDataResouceVOS()) {
                DataResouceVO dataResouceVO = new DataResouceVO();
                dataResouceVOS.add(dataResouceVO);

                dataResouceVO.setResourceCode(rbacRoleDataPermissionPO.getResourceCode());
                dataResouceVO.setResourceName(rbacRoleDataPermissionPO.getResourceName());
                dataResouceVO.setResourceType(rbacRoleDataPermissionPO.getResourceType());
            }
        }
        log.info("GET:/user/{userId}/data/resource/{groupCode}=response====roleDataResourceResponseVO={}***********************************",roleDataResourceResponseVO);
        return new Result(roleDataResourceResponseVO);
    }

    @GetMapping("/bap")
    @ApiOperation(value = "获取BAP配置信息")
    public BapVO getBap() throws NacosException, IOException {
    	String tenantId =RpcContext.getContext().getTenantId();
    	//从redis中获取数据
        String resourceSupplant =redisTemplate.opsForValue().get(tenantId+":integration.supplant.enabled");

        log.info("integration.supplant.enabled  value is "+resourceSupplant);
        BapVO bapVO = new BapVO();
        if (StringUtils.isEmpty(resourceSupplant)) {
            bapVO.setInstall(false);
        } else {
            bapVO.setInstall(new Boolean(resourceSupplant));
        }
        return bapVO;
//        String config = configService.getConfig("supfusion-common.properties", "prod", 5000L);
//        if (StringUtils.isEmpty(config)) {
//            BapVO bapVO = new BapVO();
//            bapVO.setInstall(false);
//            return bapVO;
//        }
//        Properties properties = parseString(config);
//        String enabled = properties.getProperty("integration.supplant.enabled");
//        BapVO bapVO = new BapVO();
//        if (StringUtils.isEmpty(enabled)) {
//            bapVO.setInstall(false);
//        } else {
//            bapVO.setInstall(new Boolean(enabled));
//        }
//        return bapVO;
    }

    public static Properties parseString(String content) throws IOException {
        Properties props = new Properties();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        props.load(inputStream);
        return props;
    }

    @GetMapping("/resources")
    public PageResult mock() {
        Map map = new HashMap();
        map.put("id", 1);
        map.put("parentId", -1);
        map.put("code", "test001");
        map.put("name", "测试组001");
        map.put("resType", "flowChartZone");
        map.put("hasInfo", false);

        Map map2 = new HashMap();
        map2.put("id", 2);
        map2.put("parentId", -1);
        map2.put("code", "test002");
        map2.put("name", "测试组002");
        map2.put("resType", "flowChartZone");
        map2.put("hasInfo", false);
        List list = new ArrayList();
        list.add(map);
        list.add(map2);
        return new PageResult(list, 2, 20, 1);
    }
}

