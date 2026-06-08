package com.supcon.supfusion.rbac.webapi;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.common.exception.RoleUserErrorEnum;
import com.supcon.supfusion.rbac.common.exception.RoleUserException;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.service.IRoleUserService;
import com.supcon.supfusion.rbac.service.bo.ExportFileStatusBO;
import com.supcon.supfusion.rbac.service.bo.UserDetailBO;
import com.supcon.supfusion.rbac.urlscan.annotation.MenuOperateCode;
import com.supcon.supfusion.rbac.webapi.vo.role.RoleUserRoleVO;
import com.supcon.supfusion.rbac.webapi.vo.roleUser.*;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色用户表 前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "角色用户关联相关接口")
public class RoleUserInterApiController extends BaseController {

    @Autowired
    private IRoleUserService roleUserService;
    @Qualifier("rbacRedisTemplate")
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * @description: 角色关联用户
     * @param: params roleId 角色Id,userIds 多个用户ids
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @PostMapping("/roleUser")
    @ApiOperation(value = "新增角色用户关联")
    public void save(@RequestBody RoleUserAddVO roleUserAddVO){
        log.info("POST:/roleUser====roleUserAddVO:{}***********************************",roleUserAddVO);
        //userDetailVOs转成userDetailBOs转成
        List<UserDetailBO> userDetailBOS = roleUserAddVO.getUsers().stream().map(userDetailVO -> {
            UserDetailBO userDetailBO = new UserDetailBO();
            BeanUtils.copyProperties(userDetailVO, userDetailBO);
            return userDetailBO;
        }).collect(Collectors.toList());
        roleUserService.saveRoleUsers(roleUserAddVO.getRoleId(),userDetailBOS);
    }

    /**
     * @description: 删除角色用户关联
     * @param: roleUserIds 角色用户id
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @DeleteMapping("/roleUser/{roleUserIds}")
    @ApiOperation(value = "批量删除角色用户关联")
    @ApiImplicitParams({
            @ApiImplicitParam(name="roleUserIds",value="角色用户关联ID，逗号分隔",required=true,paramType="path"),
    })
    public void delete(@PathVariable String roleUserIds){
        log.info("DELETE:/roleUser/{roleUserIds}===roleUserIds={}***********************************",roleUserIds);
        roleUserService.deleteRoleUsers(roleUserIds);
    }

    /**
     * @description: 分页查询角色用户
     * @param: roleCode 角色编码
     * @param: current 翻页的页数
     * @param: pageSize 每页返回的元数量
     * @return: com.supcon.supfusion.framework.cloud.common.result.PageResult
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @GetMapping("/roleUsers")
    @ApiOperation(value = "分页查询角色用户关联")
    @ApiImplicitParams({
            @ApiImplicitParam(name="roleCode",value="角色编码",required=false,paramType="query"),
            @ApiImplicitParam(name="keyword",value="关键字,模糊查询用户ID，用户名，人员名，人员编码",required=false,paramType="query"),
            @ApiImplicitParam(name="current",value="当前页",required=true,paramType="query"),
            @ApiImplicitParam(name="pageSize",value="每页返回个数",required=true,paramType="query"),
            @ApiImplicitParam(name="cid",value="公司ID",required=false,paramType="query"),
    })
    public PageResult<RoleUserVO> findByPage(@RequestParam(required = false) String roleCode,@RequestParam(required = false,value = "keyword") String keyword,@RequestParam Integer current,@RequestParam Integer pageSize,@RequestParam(value = "cid",required = false) Long cid){
        log.info("GET:/roleUsers==params:roleCode={},keyword={},cid={}***********************************",roleCode,keyword,cid);
        PageResult<RoleUserPO> page = roleUserService.findByPage(roleCode,keyword, current, pageSize,cid);
        //构造VO 返回前端
        List<RoleUserVO> collect = (List<RoleUserVO>) page.getList().stream().map(roleUser -> {
            RoleUserPO roleUserPO = (RoleUserPO) roleUser;
            RoleUserVO roleUserVO = new RoleUserVO();
            BeanUtils.copyProperties(roleUserPO, roleUserVO);
            RoleUserRoleVO roleUserRoleVO = new RoleUserRoleVO();
            BeanUtils.copyProperties(roleUserPO.getRole(),roleUserRoleVO);
            roleUserVO.setRole(roleUserRoleVO);
            UserDetailVO userDetailVO = new UserDetailVO();
            userDetailVO.setPersonCode(roleUserPO.getPersonCode());
            userDetailVO.setPersonName(roleUserPO.getPersonName());
            userDetailVO.setId(roleUserPO.getUserId());
            userDetailVO.setUserName(roleUserPO.getUserName());
            roleUserVO.setUser(userDetailVO);
            roleUserVO.setFromPosition(roleUser.getFromPosition());
            return roleUserVO;
        }).collect(Collectors.toList());
//        log.info("GET:/roleUsers==response:collect={}***********************************",collect);
        PageResult<RoleUserVO> result = new PageResult<>();
        result.setPagination(page.getPagination());
        result.setList(collect);
        return result;
    }

    /**
     * @description: 导出数据
     * @param: roleUserIds
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/30
     */
    @MenuOperateCode("exportRoleUser")
    @PostMapping("/roleUser/createTemp")
    @ApiOperation(value = "导出excel")
    @ApiParam(value = "导出角色用户关联",name = "exportRoleUserVO")
    public void export(@RequestBody ExportRoleUserVO exportRoleUserVO){
        log.info("POST:/roleUser/createTemp==requestBody:exportRoleUserVO={}***********************************",exportRoleUserVO);
        List<Long> roleUserIds = exportRoleUserVO.getRoleUserIds();
        String id = exportRoleUserVO.getId();
        Long roleId = exportRoleUserVO.getRoleId();
        int current = exportRoleUserVO.getCurrent();
        int pageSize = exportRoleUserVO.getPageSize();
        String keyword = exportRoleUserVO.getKeyword();
        if (current > 0 && pageSize > 0){
            roleUserService.createTemp(current,pageSize,id,roleId,keyword);
        }else{
            roleUserService.createTemp(roleUserIds,id,roleId,keyword);
        }
    }

    /**
     * @description: 下载文件
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/30
     */
    @MenuOperateCode("exportRoleUser")
    @GetMapping("/roleUser/downloadFile")
    @ApiOperation(value = "下载文件")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id",value="ID",required=true,paramType="query"),
    })
    public void export(@RequestParam("id") String id){
        roleUserService.export(id);
    }

    /**
     * @description: 查询临时文件完成状态
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/30
     */
    @MenuOperateCode("exportRoleUser")
    @GetMapping("/roleUser/getFileStatus")
    @ApiOperation(value = "查询临时文件完成状态")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id",value="ID",required=true,paramType="query"),
    })
    public Result<ExportFileStatusVO> getFileStatus(@RequestParam("id") String id){
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        Object o = ops.get("keyObj", id);
        if (!ObjectUtils.isEmpty(o)){
            ExportFileStatusBO efBO = (ExportFileStatusBO) o;
            ExportFileStatusVO efVO = new ExportFileStatusVO();
            BeanUtils.copyProperties(efBO,efVO);
            return Result.data(efVO);
        }
        return Result.data(null);
    }
}

