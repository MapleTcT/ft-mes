package com.supcon.supfusion.rbac.openapi;


import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.CompanyResultDTO;
import com.supcon.supfusion.rbac.api.IRoleApiService;
import com.supcon.supfusion.rbac.common.utils.TimeTransferUtils;
import com.supcon.supfusion.systemcode.api.SystemCodeApiService;
import com.supcon.supfusion.rbac.api.dto.CreateRoleDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.rbac.api.dto.UpdateRoleDTO;
import com.supcon.supfusion.rbac.common.exception.RoleErrorEnum;
import com.supcon.supfusion.rbac.openapi.vo.CreateRoleVO;
import com.supcon.supfusion.rbac.openapi.vo.RoleVO;
import com.supcon.supfusion.rbac.openapi.vo.RolesVO;
import com.supcon.supfusion.rbac.openapi.vo.UpdateRoleVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.RoleException;
import com.supcon.supfusion.rbac.dao.field.RoleField;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.openapi.vo.role.RoleFindOneVO;
import com.supcon.supfusion.rbac.openapi.vo.roleUser.UserDetailVO;
import com.supcon.supfusion.rbac.service.IRoleService;
import com.supcon.supfusion.rbac.service.IRoleUserService;
import io.swagger.annotations.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import java.util.stream.Collectors;

/**
 * 角色表 前端控制器 openApi
 */
@Slf4j
@Setter
@Getter
@Validated
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v2")
@Api(tags = "角色管理OpenApi", description = "角色管理OpenApi接口文档说明", hidden = true)
public class RoleOpenApiController extends BaseController {
    @Autowired
    private IRoleApiService iRoleApiService;
    @ServiceApiReference
    private PersonApiService personApiService;

    @PostMapping("/roles")
    @ResponseBody
    public RoleVO createRole(@RequestBody @Valid CreateRoleVO roleVO) {
        Result<CompanyResultDTO> companyResultDTOResult = personApiService.findCompanyByCode(roleVO.getCompanyCode());
        if (companyResultDTOResult == null || companyResultDTOResult.getData() == null || companyResultDTOResult.getData().getId() == null) {
            throw new BizHttpStatusException(RoleErrorEnum.COMPANY_DONT_EXIST, 400);
        }
        CreateRoleDTO createRoleDTO = new CreateRoleDTO();
        createRoleDTO.setCid(companyResultDTOResult.getData().getId());
        createRoleDTO.setCode(roleVO.getCode());
        createRoleDTO.setName(roleVO.getName());
        createRoleDTO.setDescription(roleVO.getDescription());
        iRoleApiService.createRole(createRoleDTO);

        RoleVO response = new RoleVO();
        response.setRoleCode(roleVO.getCode());
        return response;
    }

    @GetMapping("/roles")
    @ResponseBody
    public PageResult<RolesVO> getRoles(@RequestParam(required = false) @NotEmpty(message = "公司编码不能为空") String companyCode,
                                        @RequestParam(required = false) String key,
                                        @RequestParam(required = false) Integer current,
                                        @RequestParam(required = false) Integer pageSize) {
        if (current == null) {
            current = 1;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
        if (pageSize > 500) {
            pageSize = 500;
        }
        Result<CompanyResultDTO> companyResultDTOResult = personApiService.findCompanyByCode(companyCode);
        if (companyResultDTOResult == null || companyResultDTOResult.getData() == null || companyResultDTOResult.getData().getId() == null) {
            return new PageResult(new ArrayList(), 0, pageSize, current);
        }


        PageResult<RoleDTO> pageResult = iRoleApiService.getRolesByCid(companyResultDTOResult.getData().getId(), key, current, pageSize);
        List<RolesVO> rolesVOS = new ArrayList();
        if (pageResult.getList() != null && pageResult.getList().size() > 0) {
            pageResult.getList().forEach(dto -> {
                RolesVO rolesVO = new RolesVO();
                rolesVOS.add(rolesVO);

                rolesVO.setCode(dto.getCode());
                rolesVO.setName(dto.getName());
                rolesVO.setDescription(dto.getDescription());
            });
            return new PageResult(rolesVOS, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent());
        } else {
            return new PageResult(rolesVOS, 0, pageSize, current);
        }
    }

    @PutMapping("/roles/{roleCode}")
    @ResponseBody
    public RoleVO updateRole(@PathVariable String roleCode, @RequestBody @Valid UpdateRoleVO roleVO) {

        if (StringUtils.hasText(roleVO.getName()) && roleVO.getName().length() > 50) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_NAME_LENGTH_TOO_LONG, 400);
        }
        if (StringUtils.hasText(roleVO.getDescription()) && roleVO.getDescription().length() > 255) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_DESCRIPTION_LENGTH_TOO_LONG, 400);
        }

        UpdateRoleDTO updateRoleDTO = new UpdateRoleDTO();
        updateRoleDTO.setCode(roleCode);
        updateRoleDTO.setShowName(roleVO.getName());
        updateRoleDTO.setDescription(roleVO.getDescription());
        iRoleApiService.updateRole(updateRoleDTO);

        RoleVO response = new RoleVO();
        response.setRoleCode(roleCode);
        return response;
    }

    @DeleteMapping("/roles")
    @ResponseBody
    public void deleteRole(@RequestParam(required = false) @Valid @NotNull(message = "角色编码不能为空") List<String> roleCodes) {
        iRoleApiService.deleteRolesByCodes(roleCodes);
    }
    @Autowired
    private IRoleService roleService;
    @Autowired
    private IRoleUserService roleUserService;
    @Autowired
    private SystemCodeApiService systemCodeApiService;

    /**
     * @description: 角色查找 单个 test
     * @param: roleCode 角色编码
     * @return: com.supcon.supfusion.framework.cloud.common.result.Result
     */
    @GetMapping(value = {"/roles/{roleCode}"})
    @ApiOperation(value = "根据编码查询指定角色信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleCode", value = "角色编码", required = true, paramType = "path"),
    })
    public RoleFindOneVO getRoleByCode(@PathVariable(value = "roleCode", required = true) String roleCode) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(RoleField.code, roleCode);
        RolePO rolePO = roleService.getOne(queryWrapper);

        //编码无对应角色
        if (ObjectUtils.isEmpty(rolePO)) {
            throw new RoleException(RoleErrorEnum.ROLE_CANNOT_FIND);
        }

        RoleFindOneVO roleVO = new RoleFindOneVO();
        BeanUtils.copyProperties(rolePO, roleVO);

        return roleVO;
    }

    /**
     * @description: 查询指定角色关联的用户信息
     * @param: roleCode 角色编码
     * @param: keyword 关键字
     * @param: current 翻页的页数
     * @param: pageSize 每页返回的数量
     * @return:
     */
    @GetMapping("/roles/{roleCode}/users")
    @ApiOperation(value = "分页查询指定角色关联的用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleCode", value = "角色编码", required = true, paramType = "path"),
            @ApiImplicitParam(name = "current", value = "当前页", required = true, paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页返回个数", required = false, paramType = "query")
    })
    public PageResult<UserDetailVO> findByPage(@PathVariable(value = "roleCode") String roleCode,
                                             @ApiParam(value = "当前页码", required = false, defaultValue = "1") @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = true, defaultValue = "1") Integer current,
                                             @ApiParam(value = "每页条数", required = false, defaultValue = "20") @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        RolePO rolePO = roleService.getOne(new QueryWrapper<RolePO>().eq("valid", true).eq("code", roleCode));
        //编码无对应角色
        if (null == rolePO){
            throw new RoleException(RoleErrorEnum.ROLE_CANNOT_FIND);
        }
        Long cid = rolePO.getCid();
        PageResult<RoleUserPO> page = roleUserService.findByPage(roleCode, null, current, pageSize, cid);
        //构造VO 返回前端
        List<UserDetailVO> collect = (List<UserDetailVO>) page.getList().stream().map(roleUser -> {
            RoleUserPO roleUserPO = (RoleUserPO) roleUser;
            UserDetailVO userDetailVO = new UserDetailVO();
            BeanUtils.copyProperties(roleUserPO, userDetailVO);
            userDetailVO.setPersonCode(roleUserPO.getPersonCode());
            userDetailVO.setPersonName(roleUserPO.getPersonName());
            userDetailVO.setUserName(roleUserPO.getUserName());
            // 时间转换
            String modifyTime = roleUserPO.getModifyTime();
            if (!StringUtils.isEmpty(modifyTime)){
                modifyTime = TimeTransferUtils.responseFormatTime(modifyTime);
                userDetailVO.setModifyTime(modifyTime);
            }
            return userDetailVO;
        }).collect(Collectors.toList());
        PageResult<UserDetailVO> result = new PageResult<>();
        result.setPagination(page.getPagination());
        result.setList(collect);
        return result;
    }

    /**
     * @description: 查询角色列表 分页 test
     * @param: code 角色编码
     * @return: PageResult
     */
//    @GetMapping(value = {"/roles"})
//    @ApiOperation(value = "根据编码查询角色")
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "current", value = "当前页数", required = false, paramType = "query"),
//            @ApiImplicitParam(name = "pageSize", value = "每页数量", required = false, paramType = "query")
//    })
//    public PageResult<RoleFindOneVO> getRolesByPage(@ApiParam(value = "当前页码", required = false, defaultValue = "1") @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
//                                                    @ApiParam(value = "每页条数", required = false, defaultValue = "20") @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
//        PageResult<RolePO> rolesByPage = roleService.getRolesByPage(current, pageSize);
//        if (rolesByPage == null || rolesByPage.getPagination() == null || rolesByPage.getPagination().getTotal() == 0){
//            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
//        }
//
//        List<RoleFindOneVO> roleVOList = new ArrayList<>();
//        rolesByPage.getList().stream().forEach(rolePO -> {
//            RoleFindOneVO roleVO = new RoleFindOneVO();
//            BeanUtils.copyProperties(rolePO, roleVO);
//            // 角色类型
//            String roleType = rolePO.getRoleType();
//            if (!StringUtils.isEmpty(roleType)) {
//                Result<SystemCodeResultDTO> systemCodeResultDTORes = systemCodeApiService.queryValueByCode(roleType.split("/")[0], roleType.split("/")[1]);
//                roleVO.setRoleType(systemCodeResultDTORes.getData().getDisplayName());
//            }
//            roleVOList.add(roleVO);
//        });
//
//        Pagination pagination = rolesByPage.getPagination();
//        PageResult<RoleFindOneVO> res = new PageResult<>(roleVOList, pagination.getTotal(), pageSize, current);
//        res.setCode(BizErrorEnum.SYSTEM_OK.getCode());
//        res.setMessage(BizErrorEnum.SYSTEM_OK.getMessage());
//        return res;
//    }

    /**
     * @description: 查询角色的子节点列表
     * @param: code 角色编码
     * @return:
     */
    @GetMapping(value = {"/roles/{roleCode}/children"})
    @ApiOperation(value = "根据编码查询角色的子节点列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleCode", value = "角色编码", required = true, paramType = "path"),
            @ApiImplicitParam(name = "multistage", value = "是否查询多级", required = false, paramType = "query"),
            @ApiImplicitParam(name = "current", value = "当前页", required = true, paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "每页返回个数", required = false, paramType = "query")
    })
    public PageResult<RoleFindOneVO> getChildRoles(@PathVariable(value = "roleCode", required = true) String roleCode,
                                                   @ApiParam(value = "是否查询多级", required = false, defaultValue = "true") @RequestParam(value = "multistage", required = false, defaultValue = "1") boolean multistage,
                                                   @ApiParam(value = "当前页码", required = false, defaultValue = "1") @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = true, defaultValue = "1") Integer current,
                                                   @ApiParam(value = "每页条数", required = false, defaultValue = "20") @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<RolePO> rolePOList = roleService.querySubRolesByParentCode(roleCode, multistage, current, pageSize);
        if (rolePOList == null || CollectionUtils.isEmpty(rolePOList.getList())){
            return new PageResult<>(new ArrayList<>(1), 0, pageSize, current);
        }

        List<RoleFindOneVO> roleFindOneVOList = new ArrayList<>();
        rolePOList.getList().stream().forEach(role -> {
            RoleFindOneVO roleVO = new RoleFindOneVO();
            BeanUtils.copyProperties(role, roleVO);
            roleFindOneVOList.add(roleVO);
        });

        return new PageResult<RoleFindOneVO>(roleFindOneVOList, rolePOList.getPagination().getTotal(), pageSize, current);
    }
}

