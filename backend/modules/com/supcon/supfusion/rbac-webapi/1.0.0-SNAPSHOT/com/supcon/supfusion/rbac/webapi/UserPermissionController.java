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
import com.supcon.supfusion.rbac.common.utils.OrchidUtils;
import com.supcon.supfusion.rbac.dao.bo.MenuOperatePermissionBO;
import com.supcon.supfusion.rbac.dao.field.MenuInfoField;
import com.supcon.supfusion.rbac.dao.field.MenuOperateField;
import com.supcon.supfusion.rbac.dao.field.RoleUserField;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.manager.IUserAdapter;
import com.supcon.supfusion.rbac.service.*;
import com.supcon.supfusion.rbac.service.bo.PrivilegeBO;
import com.supcon.supfusion.rbac.dao.bo.UserPermissionBO;
import com.supcon.supfusion.rbac.service.impl.RoleUserServiceImpl;
import com.supcon.supfusion.rbac.urlscan.annotation.MenuOperateCode;
import com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO;
import com.supcon.supfusion.rbac.webapi.vo.menuOperate.MenuOperateAssignVO;
import com.supcon.supfusion.rbac.webapi.vo.userPDepartment.UserPDepartmentVO;
import com.supcon.supfusion.rbac.webapi.vo.userPPosition.UserPPositionVO;
import com.supcon.supfusion.rbac.webapi.vo.userPStaff.UserPStaffVO;
import com.supcon.supfusion.rbac.webapi.vo.userPermission.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.NotNull;
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
@Api(tags = "用户权限相关接口")
public class UserPermissionController extends BaseController {

    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IUserPermissionService userPermissionService;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private IUserAdapter userAdapter;
    @Autowired
    II18nAdapter i18nAdapterService;
    @Autowired
    private IRoleUserService roleUserService;

    /**
     * @description: 根据菜单ID查询用户权限，分权限和未分权限的都查
     * @param: menuId
     * @param: userId
     * @return: com.supcon.supfusion.framework.cloud.common.result.Result<java.util.Map < java.lang.String, java.util.List < com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO>>>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("queryUserPermission")
    @GetMapping("/userPermissions")
    @ApiOperation(value = "根据菜单ID查询用户权限，分权限和未分权限的都查")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", value = "菜单ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "roleId", value = "角色ID", required = true, paramType = "query"),
    })
    public Result findPermissionsAssigned(@RequestParam("menuId") Long menuId, @RequestParam("userId") Long userId) {
        log.info("GET:/userPermissions==params:menuId={},userId={}====================================================",menuId,userId);
        Map<String, List<MenuInfoAssignVO>> result = null;
        //查询菜单的分配权限操作
        List<MenuOperatePermissionBO> assignedPermission = menuOperateService.getAssignMenuOperateUser(menuId);
        List<MenuOperateAssignVO> menuOperateAssignVOS = JSONArray.parseArray(JSON.toJSONString(assignedPermission), MenuOperateAssignVO.class);
        List<UserPermissionBO> upList = userPermissionService.getUserPermissionListFull(userId,1);
        //传给前端结构构造
        List<UserPermissionFindVO> userPermissionFindVOS = this.userPermissionPO2userPermissionVO(upList);
        result = this.menuOperateVODataComplete(menuOperateAssignVOS,userPermissionFindVOS);
//        log.info("GET:/userPermissions==response:{}====================================================",menuOperateVODataComplete(menuOperateAssignVOS,userPermissionFindVOS));
        System.out.println(123);
        return Result.data(result);
    }

    private List<UserPermissionFindVO> userPermissionPO2userPermissionVO(List<UserPermissionBO> upList) {
        return upList.stream().map(userPermissionBO -> {
            UserPermissionFindVO userPermissionFindVO = new UserPermissionFindVO();
            BeanUtils.copyProperties(userPermissionBO, userPermissionFindVO);
            if (!ObjectUtils.isEmpty(userPermissionBO.getPositions())) {
                userPermissionFindVO.setPositions(userPermissionBO.getPositions().stream().map(userPPositionPO -> {
                    UserPPositionVO userPPositionVO = new UserPPositionVO();
                    userPPositionVO.setId(userPPositionPO.getPositionId());
                    return userPPositionVO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(userPermissionBO.getDepartments())) {
                userPermissionFindVO.setDepartments(userPermissionBO.getDepartments().stream().map(userPDepartmentPO -> {
                    UserPDepartmentVO userPDepartmentVO = new UserPDepartmentVO();
                    userPDepartmentVO.setId(userPDepartmentPO.getDepartmentId());
                    return userPDepartmentVO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(userPermissionBO.getStaffs())) {
                userPermissionFindVO.setStaffs(userPermissionBO.getStaffs().stream().map(userPStaffPO -> {
                    UserPStaffVO userPStaffVO = new UserPStaffVO();
                    userPStaffVO.setId(userPStaffPO.getStaffId());
                    return userPStaffVO;
                }).collect(Collectors.toList()));
            }
            return userPermissionFindVO;
        }).collect(Collectors.toList());
    }

    private Map<String, List<MenuInfoAssignVO>> menuOperateVODataComplete(List<MenuOperateAssignVO> menuOperateVOS, List<UserPermissionFindVO> userPermissionFindVOS) {
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
            Optional<UserPermissionFindVO> first = userPermissionFindVOS.stream().filter(userPermissionVO -> userPermissionVO.getMenuOperateId().equals(menuOperateAssignVO.getId())).findFirst();
            if (first.isPresent()){
                //添加分配的权限
                menuOperateAssignVO.setUserPermission(first.get());
                menuInfoAssignVOS.add(menuInfoVO);
            }else{
                if (menuOperateAssignVO.getDefaultOperate()){
                    Optional<MenuOperateAssignVO> otherOperateInMenu = menuOperateVOS.stream().filter(operate -> operate.getMenuinfoId().equals(menuOperateAssignVO.getMenuinfoId()) && !operate.getCode().equals(menuOperateAssignVO.getCode())).findFirst();
                    if (otherOperateInMenu.isPresent()){
                        return;
                    }
                }
                //默认的用户权限
                UserPermissionFindVO userPermissionFindVO = new UserPermissionFindVO();
                userPermissionFindVO.setMenuOperateId(menuOperateAssignVO.getId());
                userPermissionFindVO.setMenuOperateCode(menuOperateAssignVO.getCode());
                userPermissionFindVO.setUserId(UserContext.getUserContext().getUserId());
                menuOperateAssignVO.setUserPermission(userPermissionFindVO);
                //添加到未分配列表中
                menuInfoUnAssignVOS.add(menuInfoVO);
            }
        });
        result.put("assign",this.groupMenuInfoAssignVo(menuInfoAssignVOS));
        result.put("unassign",this.groupMenuInfoAssignVo(menuInfoUnAssignVOS));
        return result;
    }

    //为防止前端因同菜单的数据分散开来造成表格错位
    private List<MenuInfoAssignVO> groupMenuInfoAssignVo(List<MenuInfoAssignVO> menuInfoAssignVOS) {
        if (ObjectUtils.isEmpty(menuInfoAssignVOS)) {
            return menuInfoAssignVOS;
        }
        Map<Long, List<MenuInfoAssignVO>> m = new TreeMap<>();
        List<Long> menuinfoIds = menuInfoAssignVOS.stream().map(MenuInfoAssignVO::getMenuInfoId).distinct().collect(Collectors.toList());
        menuinfoIds.forEach(menuinfoId -> m.put(menuinfoId, new ArrayList<>()));
        menuInfoAssignVOS.forEach(menuInfoAssignVO -> {
            List<MenuInfoAssignVO> list = m.get(menuInfoAssignVO.getMenuInfoId());
            list.add(menuInfoAssignVO);
        });
        List<MenuInfoAssignVO> result = new ArrayList<>();
        m.forEach((menuInfoId, sortList) -> result.addAll(sortList));
        return result;
    }

    /**
     * @description: 查询所有菜单已分配权限的用户权限
     * @param: userId
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("queryUserPermission")
    @GetMapping("/userPermissions/assigned")
    @ApiOperation(value = "查询所有菜单已分配权限的用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, paramType = "query"),
    })
    public ListResult<MenuInfoAssignVO> findPermissionsAssigned(@RequestParam("userId") Long userId) {
        log.info("GET:/userPermissions/assigned===params:userId={}================================================",userId);
        Map<String, List<MenuInfoAssignVO>> result;
        //查询菜单的已分配权限操作
        List<MenuOperatePermissionBO> assignedPermission = menuOperateService.getAssignMenuOperateUser(null);
        List<MenuOperateAssignVO> menuOperateAssignVOS = JSONArray.parseArray(JSON.toJSONString(assignedPermission), MenuOperateAssignVO.class);
        List<UserPermissionBO> upList = userPermissionService.getUserPermissionListFull(userId,1);
        //传给前端结构构造
        List<UserPermissionFindVO> userPermissionFindVOS = this.userPermissionPO2userPermissionVO(upList);
        result = this.menuOperateVODataComplete(menuOperateAssignVOS,userPermissionFindVOS);
//        log.info("GET:/userPermissions/assigned===response:={}================================================",menuOperateVODataComplete(menuOperateAssignVOS,userPermissionFindVOS).get("assign"));
        return new ListResult<>(result.get("assign"));
    }

    /**
     * @description: 修改用户权限
     * @param: userPermissionAddVO
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @MenuOperateCode("addUserPermission")
    @PostMapping("/userPermission")
    @ApiOperation(value = "修改用户权限")
    public void addUserPermission(@RequestBody UserPermissionAddVO userPermissionAddVO) {
        log.info("POST:/userPermission==requestBody:userPermissionAddVO={}***********************************",userPermissionAddVO);
        if (Objects.isNull(userPermissionAddVO)) {
            return;
        }
        List<UserPermissionReVO> list = userPermissionAddVO.getList();
        list.stream().forEach(this::accept);
        List<Long> userIds = list.stream().map(UserPermissionReVO::getUserId).collect(Collectors.toList());
        if (null != list && !list.isEmpty()) {
//            List<Long> companyIdByUserId = userAdapter.getCompanyIdByUserId(list.get(0).getUserId());
            // 保存用户和请求URL关联关系数据
//            userUrlRefService.addUserUrlRefList(Collections.singletonList(UserContext.getUserContext().getCompanyId()));
            userUrlRefService.addUserUrlRefListForUserFlow(userIds, null, RpcContext.getContext().getTenantId());
        }
    }

    private void accept(UserPermissionReVO rpAdd) {
        Long userId = rpAdd.getUserId();
        List<UserPermissionBaseVO> upAddOrUpdateList = rpAdd.getAddList();
        List<UserPermissionBaseVO> upDeleteList = rpAdd.getDeleteList();
        for (UserPermissionBaseVO upBase : upAddOrUpdateList) {
            upBase.setUserId(userId);
            upBase.setPurviewType(1);
        }
        //结构转换
        List<UserPermissionPO> upAddOrUpdatePOList = this.VO2PO(upAddOrUpdateList);
        //结构转换
        List<UserPermissionPO> upDeletePOList = this.VO2PO(upDeleteList);
        // 删除对应的用户权限数据
        userPermissionService.batchDeleteUserPermissions(upDeletePOList);
        // 新增或者修改对应的用户权限数据
        userPermissionService.addOrUpdateUserPermission(upAddOrUpdatePOList);
    }

    private List<UserPermissionPO> VO2PO(List<UserPermissionBaseVO> userPermissionBaseVOS){
        return userPermissionBaseVOS.stream().map(userPermissionBaseVO -> {
            UserPermissionPO userPermissionPO = new UserPermissionPO();
            BeanUtils.copyProperties(userPermissionBaseVO, userPermissionPO);
            if (!ObjectUtils.isEmpty(userPermissionBaseVO.getPositions())) {
                userPermissionPO.setPositions(userPermissionBaseVO.getPositions().stream().map(userPPositionVO -> {
                    UserPPositionPO userPPositionPO = new UserPPositionPO();
                    userPPositionPO.setPositionId(userPPositionVO.getId());
                    return userPPositionPO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(userPermissionBaseVO.getStaffs())) {
                userPermissionPO.setStaffs(userPermissionBaseVO.getStaffs().stream().map(userPStaffVO -> {
                    UserPStaffPO userPStaffPO = new UserPStaffPO();
                    userPStaffPO.setStaffId(userPStaffVO.getId());
                    return userPStaffPO;
                }).collect(Collectors.toList()));
            }
            if (!ObjectUtils.isEmpty(userPermissionBaseVO.getDepartments())) {
                userPermissionPO.setDepartments(userPermissionBaseVO.getDepartments().stream().map(userPDepartmentVO -> {
                    UserPDepartmentPO userPDepartmentPO = new UserPDepartmentPO();
                    userPDepartmentPO.setDepartmentId(userPDepartmentVO.getId());
                    return userPDepartmentPO;
                }).collect(Collectors.toList()));
            }
            userPermissionPO.setCid(UserContext.getUserContext().getCompanyId());
            return userPermissionPO;
        }).collect(Collectors.toList());
    }

    /**
     * @description: 刷操作URL到redis根据用户
     * @param: userName
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/user/userUrlRef")
    @ApiOperation(value = "刷操作URL到redis根据用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = false, paramType = "query"),
    })
    public void addUserUrlRef(@RequestParam(value = "userName", required = false) String userName) {
        log.info("GET:/user/userUrlRef==params:userName={}==================================",userName);
        List<Long> companyIdByUserName = new ArrayList<>();
        if (!ObjectUtils.isEmpty(userName)) {
            companyIdByUserName = userAdapter.getCompanyIdByUserName(userName);
        }
        // 保存用户和请求URL关联关系数据
        userUrlRefService.addUserUrlRefListForUserFlow(companyIdByUserName, null, RpcContext.getContext().getTenantId());
    }

    /**
     * @description: 查询菜单下该用户拥有的操作权限  判断该菜单下哪些按钮显示
     * @param: menuInfoCode
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<java.lang.String>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/userPermission/findUserOperate")
    @ApiOperation(value = "判断该菜单下哪些按钮显示")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuInfoCode", value = "菜单ID", required = true, paramType = "query"),
    })
    public ListResult<String> findUserOperate(@RequestParam(value = "menuInfoCode") String menuInfoCode) {
        log.info("GET:/userPermission/findUserOperate==params:menuInfoCode={}***********************************",menuInfoCode);
        List<String> userOperate = userPermissionService.findUserOperate(menuInfoCode);
//        log.info("GET:/userPermission/findUserOperate==response:userOperate={}***********************************",userOperate);
        return new ListResult<>(userOperate);
    }

    @GetMapping("/userPermissions/fromRole")
    @ApiOperation(value = "根据菜单ID查询用户权限，来源于角色")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuId", value = "菜单ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, paramType = "query"),
    })
    public Result findPermissionsAssignedRole(@RequestParam("menuId") @NotNull Long menuId, @RequestParam("userId") @NotNull Long userId) {
        log.info("GET:/userPermissions/fromRole===params:menuId={},userId={}***********************************",menuId,userId);
        //查询菜单的已分配权限操作
        // sql 超长处理
        List<MenuOperatePermissionBO> assignedPermission = menuOperateService.getAssignMenuOperateUserFromRole(null, userId);
        List<MenuOperateAssignVO> menuOperateAssignVOS = JSONArray.parseArray(JSON.toJSONString(assignedPermission), MenuOperateAssignVO.class);
        List<UserPermissionBO> upList = userPermissionService.getUserPermissionListFull(userId,0);
        //传给前端结构构造
        List<UserPermissionFindVO> userPermissionFindVOS = this.userPermissionPO2userPermissionVO(upList);
        Map<String, List<MenuInfoAssignVO>> result = this.menuOperateVODataComplete(menuOperateAssignVOS, userPermissionFindVOS);
//        log.info("GET:/userPermissions/fromRole===response:={}***********************************",menuOperateVODataComplete(menuOperateAssignVOS, userPermissionFindVOS).get("assign"));
        return Result.data(result.get("assign"));
    }


    /**
     * @description: 查询所有菜单已分配权限的角色权限
     * @param: userId
     * @return: com.supcon.supfusion.framework.cloud.common.result.ListResult<com.supcon.supfusion.rbac.webapi.vo.MenuInfo.MenuInfoAssignVO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @GetMapping("/userPermissions/assigned/fromRole")
    @ApiOperation(value = "查询所有菜单已分配权限的用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, paramType = "query"),
    })
    public ListResult<MenuInfoAssignVO> findPermissionsUnAssignFromRole(@RequestParam("userId") Long userId) {
        log.info("GET:/userPermissions/assigned/fromRole==params:userId={}***********************************",userId);
        //查询菜单的已分配权限操作
        List<MenuOperatePermissionBO> assignedPermission = menuOperateService.getAssignMenuOperateUserFromRole(null, userId);
        List<MenuOperateAssignVO> menuOperateAssignVOS = JSONArray.parseArray(JSON.toJSONString(assignedPermission), MenuOperateAssignVO.class);
        List<UserPermissionBO> upList = userPermissionService.getUserPermissionListFull(userId,0);
        //传给前端结构构造
        List<UserPermissionFindVO> userPermissionFindVOS = this.userPermissionPO2userPermissionVO(upList);
        Map<String, List<MenuInfoAssignVO>> result = this.menuOperateVODataComplete(menuOperateAssignVOS, userPermissionFindVOS);
//        log.info("GET:/userPermissions/assigned/fromRole==response:={}***********************************",menuOperateVODataComplete(menuOperateAssignVOS, userPermissionFindVOS).get("assign"));
        return new ListResult<>(result.get("assign"));
    }

    @GetMapping("/userPermission/checkUserPermission")
    @ApiOperation(value = "检查当前用户是否拥有指定菜单操作权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "menuOperateCode", value = "菜单操作编码", required = true, paramType = "query"),
            @ApiImplicitParam(name = "cid", value = "所属公司id", required = false, paramType = "query"),
    })
    public Result<Boolean> checkUserPermission(@RequestParam(value = "menuOperateCode") String menuOperateCode, @RequestParam(value = "cid", required = false) String cid) {
        log.info("GET:/userPermission/checkUserPermission===menuOperateCode={},cid={}***********************************",menuOperateCode,cid);
        Result success = Result.success();
        success.setData(userPermissionService.checkUserPermission(menuOperateCode, cid));
        success.setCode(200);
        success.setMessage("操作成功");
        return success;
    }

    /**
     * 获取权限编码pc
     *
     * @param codes 菜单操作编码,以逗号隔开
     * @return
     */
    @GetMapping(value = "/powerCode/getPowerCode", produces = "application/json")
    public Result<Map<String, String>> generateNormalOperatePowerCode(String codes) {
        log.info("GET:/powerCode/getPowerCode===params:codes={}***********************************",codes);
        Result success = Result.success();
        if (StringUtils.isEmpty(codes)) {
            success.setData(Collections.emptyMap());
            return success;
        }
        Map<String, String> responseMap = new HashMap<>();
        String[] codesStrs = codes.split(",");
        for (String menuOperateCode : codesStrs) {
            List<MenuOperatePO> menuOperates = menuOperateService.getByCode(menuOperateCode, UserContext.getUserContext().getCompanyId());
            if (!CollectionUtils.isEmpty(menuOperates)) {
                MenuOperatePO mo = menuOperates.get(0);
                String pc = new String(OrchidUtils
                        .encode((mo.getCode() + "|" + (mo.getFlowKey() == null ? "" : mo.getFlowKey())).getBytes()));
                responseMap.put(menuOperateCode, pc);
            }
        }
        success.setData(responseMap);
        success.setCode(200);
        success.setMessage("操作成功");
        return success;
    }

    @GetMapping("/userPermission/findAllUserPermission")
    @ApiOperation(value = "检查当前用户是否拥有指定菜单操作权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "operateCode", value = "菜单操作编码", required = false, paramType = "query"),
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "menuCode", value = "菜单编码", required = false, paramType = "query"),
    })
    public ListResult<PrivilegeVO> findAllUserPermission(@RequestParam(value = "operateCode",required = false) String operateCode, @RequestParam(value = "userId",required = true) Long userId, @RequestParam(value = "menuCode",required = false) String menuCode) {
        log.info("GET:/userPermission/findAllUserPermission==params:operateCode={},userID={},menuCode={}***********************************",operateCode,userId,menuCode);
        List<String> operateCodes = new ArrayList<>();
        //构造操作CODE->菜单CODE map
        Map<String,String> operate_menu = new HashMap<>();
        if (!ObjectUtils.isEmpty(menuCode)){
            MenuInfoPO menuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.code, menuCode));
            if (!ObjectUtils.isEmpty(menuInfoPO)){
                List<MenuOperatePO> operatePOS = menuOperateService.list(new QueryWrapper<MenuOperatePO>().eq(MenuOperateField.menuinfoId, menuInfoPO.getId()));
                if (!ObjectUtils.isEmpty(operatePOS)){
                    operateCodes.addAll(operatePOS.stream().map(MenuOperatePO::getCode).collect(Collectors.toList()));
                }
                operatePOS.forEach(menuOperatePO -> {
                    operate_menu.put(menuOperatePO.getCode(),menuCode);
                });
            }
        }
        if (!ObjectUtils.isEmpty(operateCode)){
            operateCodes.add(operateCode);
            List<MenuOperatePO> operatePO = menuOperateService.list(new QueryWrapper<MenuOperatePO>().eq(MenuOperateField.code, operateCode).select(MenuOperateField.menuinfoId));
            if (!ObjectUtils.isEmpty(operatePO)){
                MenuInfoPO menuInfoPO = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.id, operatePO.get(0).getMenuinfoId()).select(MenuInfoField.code));
                operate_menu.put(operateCode,menuInfoPO.getCode());
            }
        }
        List<PrivilegeBO> permissions = userPermissionService.findAllUserPermission(operateCodes, userId, operate_menu);
        List<PrivilegeVO> privilegeVOS = permissions.stream().map(privilegeBO -> {
            PrivilegeVO privilegeVO = new PrivilegeVO();
            BeanUtils.copyProperties(privilegeBO, privilegeVO);
            return privilegeVO;
        }).collect(Collectors.toList());
//        log.info("GET:/userPermission/findAllUserPermission==response:={}***********************************",privilegeVOS);
        return new ListResult<>(privilegeVOS);
    }

    @GetMapping("/userPermission/findUserEntityPermission")
    @ApiOperation(value = "获取用户某个实体模块拥有的权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "userId", value = "用户ID", required = true, paramType = "query"),
            @ApiImplicitParam(name = "entityCode", value = "实体编码", required = true, paramType = "query"),
    })
    public ListResult<PrivilegeVO> checkUserPermissionFusion(@RequestParam(value = "entityCode",required = false) String entityCode, @RequestParam(value = "userId",required = true) Long userId) {
        log.info("GET:/userPermission/findUserEntityPermission====userId={},=====entityCode={}***********************************",userId,entityCode);
        List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().eq(MenuInfoField.entityCode, entityCode));
        if (ObjectUtils.isEmpty(menuInfoPOS)){
            return new ListResult<>();
        }
        Map<Long,String> map = new HashMap<>();
        //构造菜单map
        menuInfoPOS.forEach(menuInfoPO -> {
            map.put(menuInfoPO.getId(),menuInfoPO.getCode());
        });
        List<Long> menuIds = menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        List<MenuOperatePO> operatePOS = menuOperateService.list(new QueryWrapper<MenuOperatePO>().in(MenuOperateField.menuinfoId, menuIds));
        if (ObjectUtils.isEmpty(operatePOS)){
            return new ListResult<>();
        }

        //构造操作CODE->菜单CODE map
        Map<String,String> operate_menu = new HashMap<>();
        operatePOS.forEach(menuOperatePO -> {
            operate_menu.put(menuOperatePO.getCode(),map.get(menuOperatePO.getMenuinfoId()));
        });
        List<PrivilegeBO> permissionBos = userPermissionService.findAllUserPermission(new ArrayList<>(operate_menu.keySet()), userId, operate_menu);
        List<PrivilegeVO> privilegeVOS = permissionBos.stream().map(permission -> {
            PrivilegeVO privilegeVO = new PrivilegeVO();
            BeanUtils.copyProperties(permission, privilegeVO);
            return privilegeVO;
        }).collect(Collectors.toList());
//        log.info("GET:/userPermission/findUserEntityPermission====response={}***********************************",privilegeVOS);
        return new ListResult<>(privilegeVOS);
    }

    private List<MenuOperateAssignVO> filterRepeat(List<MenuOperateAssignVO> menuOperateAssignVOS){
        return menuOperateAssignVOS.stream().filter(RoleUserServiceImpl.distinctByKey(MenuOperateAssignVO::getCode)).collect(Collectors.toList());
    }
}

