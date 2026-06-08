package com.supcon.supfusion.rbac.webapi;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.dao.bo.MenuOperatePermissionBO;
import com.supcon.supfusion.rbac.dao.bo.RolePermissionBO;
import com.supcon.supfusion.rbac.dao.field.RoleUserField;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.manager.IUserAdapter;
import com.supcon.supfusion.rbac.service.*;
import com.supcon.supfusion.rbac.urlscan.annotation.MenuOperateCode;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO;
import com.supcon.supfusion.rbac.webapi.vo.menuOperate.MenuOperateAssignVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePDepartment.RolePDepartmentVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePPosition.RolePPositionVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePStaff.RolePStaffVO;
import com.supcon.supfusion.rbac.webapi.vo.rolePermission.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.context.i18n.LocaleContextHolder.getLocale;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-15
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "角色权限相关接口")
public class RolePermissionController extends BaseController {

    @Autowired
    private IRolePermissionService rolePermissionService;
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Autowired
    private IRoleUserService roleUserService;
    @Autowired
    private IUserAdapter userAdapter;
    @Autowired
    II18nAdapter i18nAdapterService;

    /**
     * @description: 根据菜单ID查询角色权限，分权限和未分权限的都查
     * @param: menuId
     * @param: roleId
     * @return: com.supcon.supfusion.framework.cloud.common.result.Result<java.util.Map < java.lang.String, java.util.List < com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO>>>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("queryRolePermissions")
    @GetMapping("/rolePermissions")
    @ApiOperation(value = "根据菜单ID查询角色权限，分权限和未分权限的都查")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", value = "菜单ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "roleId", value = "角色ID", required = true, paramType = "query"),
    })
    public Result findPermissionsAssigned(@RequestParam("menuId") Long menuId, @RequestParam("roleId") Long roleId) {
        log.info("GET:/rolePermissions===params:menuId={},roleId={}***********************************", menuId, roleId);
        List<MenuOperatePermissionBO> assignedPermission = menuOperateService.getAssignMenuOperateUser(menuId);
        List<MenuOperateAssignVO> menuOperateAssignVOS = JSONArray.parseArray(JSON.toJSONString(assignedPermission), MenuOperateAssignVO.class);
        List<RolePermissionBO> rpList = rolePermissionService.getRolePermissionList(roleId);
        List<RolePermissionFindVO> rolePermissionFindVOS = this.rolePermissionPO2rolePermissionVO(rpList);
//        log.info("GET:/rolePermissions===response:{}***********************************", menuOperateVODataComplete(menuOperateAssignVOS, rolePermissionFindVOS, roleId));
        return Result.data(this.menuOperateVODataComplete(menuOperateAssignVOS, rolePermissionFindVOS, roleId));
    }

    private List<RolePermissionFindVO> rolePermissionPO2rolePermissionVO(List<RolePermissionBO> rpList) {
        return rpList.stream().map(rolePermissionBO -> {
            RolePermissionFindVO rolePermissionFindVO = new RolePermissionFindVO();
            BeanUtils.copyProperties(rolePermissionBO, rolePermissionFindVO);
            if (!ObjectUtils.isEmpty(rolePermissionBO.getPositions())) {
                rolePermissionFindVO.setPositions(rolePermissionBO.getPositions().stream().map(rolePPositionPO -> {
                    RolePPositionVO rolePPositionVO = new RolePPositionVO();
                    rolePPositionVO.setId(rolePPositionPO.getPositionId());
                    return rolePPositionVO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(rolePermissionBO.getDepartments())) {
                rolePermissionFindVO.setDepartments(rolePermissionBO.getDepartments().stream().map(userPDepartmentPO -> {
                    RolePDepartmentVO rolePDepartmentVO = new RolePDepartmentVO();
                    rolePDepartmentVO.setId(userPDepartmentPO.getDepartmentId());
                    return rolePDepartmentVO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(rolePermissionBO.getStaffs())) {
                rolePermissionFindVO.setStaffs(rolePermissionBO.getStaffs().stream().map(userPStaffPO -> {
                    RolePStaffVO rolePStaffVO = new RolePStaffVO();
                    rolePStaffVO.setId(userPStaffPO.getStaffId());
                    return rolePStaffVO;
                }).collect(Collectors.toList()));
            }
            return rolePermissionFindVO;
        }).collect(Collectors.toList());
    }

    private Map<String, List<MenuInfoAssignVO>> menuOperateVODataComplete(List<MenuOperateAssignVO> menuOperateVOS, List<RolePermissionFindVO> rolePermissionFindVOS,Long roleId) {
        Map<String, List<MenuInfoAssignVO>> result = new HashMap<>();
        List<MenuInfoAssignVO> menuInfoAssignVOS = new ArrayList<>();
        List<MenuInfoAssignVO> menuInfoUnAssignVOS = new ArrayList<>();
        menuOperateVOS.forEach(menuOperateAssignVO -> {
            menuOperateAssignVO.setNameDisplay(i18nAdapterService.getRemoteMessage(menuOperateAssignVO.getName(), null, LocaleContextHolder.getLocale()));
            MenuInfoAssignVO menuInfoVO = new MenuInfoAssignVO();
            menuInfoVO.setName(menuOperateAssignVO.getMenuinfoName());
            menuInfoVO.setMenuInfoId(menuOperateAssignVO.getMenuinfoId());
            menuInfoVO.setOp(menuOperateAssignVO);
            menuInfoVO.setNameDisplay(i18nAdapterService.getRemoteMessage(menuOperateAssignVO.getMenuinfoName(), null, LocaleContextHolder.getLocale()));
            Optional<RolePermissionFindVO> first = rolePermissionFindVOS.stream().filter(userPermissionVO -> userPermissionVO.getMenuOperateId().equals(menuOperateAssignVO.getId())).findFirst();
            if (first.isPresent()){
                //添加分配的权限
                menuOperateAssignVO.setRolePermission(first.get());
                menuInfoAssignVOS.add(menuInfoVO);
            }else{
                if (menuOperateAssignVO.getDefaultOperate()){
                    Optional<MenuOperateAssignVO> otherOperateInMenu = menuOperateVOS.stream().filter(operate -> operate.getMenuinfoId().equals(menuOperateAssignVO.getMenuinfoId()) && !operate.getCode().equals(menuOperateAssignVO.getCode())).findFirst();
                    if (otherOperateInMenu.isPresent()){
                        return;
                    }
                }
                //默认的用户权限
                RolePermissionFindVO rolePermissionFindVO = new RolePermissionFindVO();
                rolePermissionFindVO.setMenuOperateId(menuOperateAssignVO.getId());
                rolePermissionFindVO.setRoleId(roleId);
                menuOperateAssignVO.setRolePermission(rolePermissionFindVO);
                //添加到未分配列表中
                menuInfoUnAssignVOS.add(menuInfoVO);
            }
        });
        result.put("assign",this.groupMenuInfoAssignVo(menuInfoAssignVOS));
        result.put("unassign",this.groupMenuInfoAssignVo(menuInfoUnAssignVOS));
        return result;
    }

    /**
     * @description: 查询所有菜单已分配权限的角色权限
     * @param: roleId
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("queryRolePermissions")
    @GetMapping("/rolePermissions/assigned")
    @ApiOperation(value = "查询所有菜单已分配权限的角色权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", required = true, paramType = "query"),
    })
    public ListResult<MenuInfoAssignVO> findPermissionsUnAssigned(@RequestParam("roleId") Long roleId) {
        log.info("GET:/rolePermissions/assigned===params:roleId={}***********************************",roleId);
        List<MenuOperatePermissionBO> assignedPermission = menuOperateService.getAssignMenuOperateUser(null);
        List<MenuOperateAssignVO> menuOperateAssignVOS = JSONArray.parseArray(JSON.toJSONString(assignedPermission), MenuOperateAssignVO.class);
        List<RolePermissionBO> rpList = rolePermissionService.getRolePermissionList(roleId);
        List<RolePermissionFindVO> rolePermissionFindVOS = this.rolePermissionPO2rolePermissionVO(rpList);
        Map<String, List<MenuInfoAssignVO>> result = this.menuOperateVODataComplete(menuOperateAssignVOS, rolePermissionFindVOS, roleId);
//        log.info("GET:/rolePermissions/assigned===response:{}***********************************",result.get("assign"));
        return new ListResult<>(result.get("assign"));
    }

    private List<MenuInfoAssignVO> groupMenuInfoAssignVo(List<MenuInfoAssignVO> menuInfoAssignVOS){
        if (ObjectUtils.isEmpty(menuInfoAssignVOS)){
            return menuInfoAssignVOS;
        }
        Map<Long,List<MenuInfoAssignVO>> m = new TreeMap<>();
        List<Long> menuinfoIds = menuInfoAssignVOS.stream().map(MenuInfoAssignVO::getMenuInfoId).distinct().collect(Collectors.toList());
        menuinfoIds.forEach(menuinfoId -> {
            m.put(menuinfoId,new ArrayList<MenuInfoAssignVO>());
        });
        menuInfoAssignVOS.forEach(menuInfoAssignVO -> {
            List<MenuInfoAssignVO> list = m.get(menuInfoAssignVO.getMenuInfoId());
            list.add(menuInfoAssignVO);
        });
        List<MenuInfoAssignVO> result = new ArrayList<>();
        m.forEach((menuInfoId,sortList) -> {
            result.addAll(sortList);
        });
        return result;
    }

    /**
     * @description: 修改角色权限
     * @param: rolePermissionAddVO
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("addRolePermissions")
    @PostMapping("/rolePermission")
    @ApiOperation(value = "修改角色权限")
    @Transactional
    public void addRolePermission(@RequestBody RolePermissionAddVO rolePermissionAddVO) {
        log.info("POST:/rolePermission===requestBody:rolePermissionAddVO={}***********************************",rolePermissionAddVO);
        if (Objects.isNull(rolePermissionAddVO)) {
            return;
        }
        List<RolePermissionReVO> list = rolePermissionAddVO.getList();
        list.stream().forEach(this::accept);
        //刷redis权限缓存
        if (null != list && !list.isEmpty()) {
            addRoleUrlRef(list.get(0).getRoleId());
        }
    }

    private void accept(RolePermissionReVO rpAdd) {
        Long roleId = rpAdd.getRoleId();
        List<RolePermissionBaseVO> rpAddOrUpdateList = rpAdd.getAddList();
        List<RolePermissionBaseVO> rpDeleteList = rpAdd.getDeleteList();
        rpAddOrUpdateList.stream().forEach(rpBase -> rpBase.setRoleId(roleId));
        //结构转换
        List<RolePermissionPO> rpAddOrUpdatePOList = rpAddOrUpdateList.stream().map(rolePermissionBaseVO -> {
            RolePermissionPO rolePermissionPO = new RolePermissionPO();
            BeanUtils.copyProperties(rolePermissionBaseVO, rolePermissionPO);
            if (!ObjectUtils.isEmpty(rolePermissionBaseVO.getPositions())) {
                rolePermissionPO.setPositions(rolePermissionBaseVO.getPositions().stream().map(rolePPositionVO -> {
                    RolePPositionPO rolePPositionPO = new RolePPositionPO();
                    rolePPositionPO.setPositionId(rolePPositionVO.getId());
                    return rolePPositionPO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(rolePermissionBaseVO.getStaffs())) {
                rolePermissionPO.setStaffs(rolePermissionBaseVO.getStaffs().stream().map(rolePStaffVO -> {
                    RolePStaffPO rolePStaffPO = new RolePStaffPO();
                    rolePStaffPO.setStaffId(rolePStaffVO.getId());
                    return rolePStaffPO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(rolePermissionBaseVO.getDepartments())) {
                rolePermissionPO.setDepartments(rolePermissionBaseVO.getDepartments().stream().map(rolePPositionVO -> {
                    RolePDepartmentPO rolePDepartmentPO = new RolePDepartmentPO();
                    rolePDepartmentPO.setDepartmentId(rolePPositionVO.getId());
                    return rolePDepartmentPO;
                }).collect(Collectors.toList()));
            }
            rolePermissionPO.setCid(UserContext.getUserContext().getCompanyId());
            rolePermissionPO.setRoleId(roleId);
            return rolePermissionPO;
        }).collect(Collectors.toList());
        List<RolePermissionPO> rpDeletePOList = rpDeleteList.stream().map(rolePermissionBaseVO -> {
            RolePermissionPO rolePermissionPO = new RolePermissionPO();
            BeanUtils.copyProperties(rolePermissionBaseVO, rolePermissionPO);
            if (!ObjectUtils.isEmpty(rolePermissionBaseVO.getPositions())) {
                rolePermissionPO.setPositions(rolePermissionBaseVO.getPositions().stream().map(rolePPositionVO -> {
                    RolePPositionPO rolePPositionPO = new RolePPositionPO();
                    rolePPositionPO.setPositionId(rolePPositionVO.getId());
                    return rolePPositionPO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(rolePermissionBaseVO.getStaffs())) {
                rolePermissionPO.setStaffs(rolePermissionBaseVO.getStaffs().stream().map(rolePStaffVO -> {
                    RolePStaffPO rolePStaffPO = new RolePStaffPO();
                    rolePStaffPO.setStaffId(rolePStaffVO.getId());
                    return rolePStaffPO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(rolePermissionBaseVO.getDepartments())) {
                rolePermissionPO.setDepartments(rolePermissionBaseVO.getDepartments().stream().map(rolePPositionVO -> {
                    RolePDepartmentPO rolePDepartmentPO = new RolePDepartmentPO();
                    rolePDepartmentPO.setDepartmentId(rolePPositionVO.getId());
                    return rolePDepartmentPO;
                }).collect(Collectors.toList()));
            }
            rolePermissionPO.setCid(UserContext.getUserContext().getCompanyId());
            rolePermissionPO.setRoleId(roleId);
            return rolePermissionPO;
        }).collect(Collectors.toList());
        // 删除对应的角色权限数据
        rolePermissionService.batchDeleteRolePermissions(rpDeletePOList);
        // 新增或者修改对应的权限数据
        rolePermissionService.addOrUpdateRolePermission(rpAddOrUpdatePOList);
        if (ObjectUtils.isEmpty(rpAddOrUpdatePOList)) {
            rolePermissionService.freshSubOperate(roleId, null, null, null);
        }
    }

    /**
     * @description: 刷操作URL到redis  根据角色
     * @param: roleId
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/role/userUrlRef")
    @ApiOperation(value = "刷操作URL到redis根据角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", value = "角色ID", required = false, paramType = "query"),
    })
    @Transactional
    public void addRoleUrlRef(@RequestParam(value = "roleId", required = false) Long roleId) {
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(roleId)) {
            queryWrapper.eq(RoleUserField.roleId, roleId);
        }
        queryWrapper.select(RoleUserField.userId, RoleUserField.userName);
        List<RoleUserPO> roleUserPOS = roleUserService.list(queryWrapper);
        List<String> userNames = roleUserPOS.stream().map(RoleUserPO::getUserName).collect(Collectors.toList());
        Set<Long> cid = new HashSet<>();
        userNames.forEach(userName -> {
            List<Long> companyIdByUserName = userAdapter.getCompanyIdByUserName(userName);
            cid.addAll(companyIdByUserName);
        });

        // 保存用户和请求URL关联关系数据
        userUrlRefService.addUserUrlRefListForUserFlow(roleUserPOS.stream().map(RoleUserPO::getUserId).collect(Collectors.toList()),null, RpcContext.getContext().getTenantId());
    }
}

