package com.supcon.supfusion.rbac.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.rbac.api.IRoleApiService;
import com.supcon.supfusion.rbac.api.dto.AdminRoleDTO;
import com.supcon.supfusion.rbac.api.dto.CreateRoleDTO;
import com.supcon.supfusion.rbac.api.dto.Resource;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.rbac.api.dto.RoleResourceDTO;
import com.supcon.supfusion.rbac.api.dto.UpdateRoleDTO;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.RoleErrorEnum;
import com.supcon.supfusion.rbac.dao.RoleMapper;
import com.supcon.supfusion.rbac.dao.RolePermissionMapper;
import com.supcon.supfusion.rbac.dao.TagMapper;
import com.supcon.supfusion.rbac.dao.po.MenuInfoCompanyRefPO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.dao.po.TagPO;
import com.supcon.supfusion.rbac.service.IMenuInfoCompanyRefService;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.IRolePermissionService;
import com.supcon.supfusion.rbac.service.IRoleService;
import com.supcon.supfusion.rbac.service.IRoleUserService;
import com.supcon.supfusion.rbac.service.IUserUrlRefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import com.supcon.supfusion.rbac.common.utils.ThreadPoolUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@ServiceApiService
@Transactional
@Slf4j
public class RoleApiServiceImpl implements IRoleApiService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private IRolePermissionService rolePermissionService;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private IRoleUserService roleUserService;
    @Autowired
    private IMenuInfoCompanyRefService menuInfoCompanyRefService;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Autowired
    private DataId dataId;

    @Override
    public Map<Long, String> findBatchName(String ids) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", Arrays.asList(ids.split(",")));
        queryWrapper.select("id", "name");
        List<RolePO> rolePOS = roleMapper.selectList(queryWrapper);
        //构建返回数据结构
        Map<Long, String> result = new HashMap<>();
        rolePOS.forEach(rolePO -> {
            result.put(rolePO.getId(), rolePO.getName());
        });
        return result;
    }

    @Override
    public List<RoleDTO> findRoleByIds(List<Long> ids) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        List<RolePO> rolePOS = roleMapper.selectList(queryWrapper);
        return rolePOS.stream().map(rolePO -> {
            RoleDTO roleDTO = new RoleDTO();
            BeanUtils.copyProperties(rolePO, roleDTO);
            return roleDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RoleDTO> findRoleByCodes(List<String> codes) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("code", codes);
        List<RolePO> rolePOS = roleMapper.selectList(queryWrapper);
        return rolePOS.stream().map(rolePO -> {
            RoleDTO roleDTO = new RoleDTO();
            BeanUtils.copyProperties(rolePO, roleDTO);
            return roleDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public RoleDTO createAdminRole(AdminRoleDTO adminRoleDTO) {
        try {
            //创建公司管理员
            log.info("创建公司同时创建系统管理员...............adminRoleDTO,{}",adminRoleDTO);
            RolePO rolePO = new RolePO();
            RolePO systemRole = roleService.getOne(new QueryWrapper<RolePO>().eq("code", adminRoleDTO.getCompanyCode() + "_companySystemRole"));
            if (ObjectUtils.isEmpty(systemRole)) {
                rolePO.setCode(adminRoleDTO.getCompanyCode() + "_companySystemRole");
                rolePO.setCid(adminRoleDTO.getCid());
                rolePO.setUuid(UUID.randomUUID().toString());
                rolePO.setName(Constants.DEFAULT_ROLE_NAME);
                rolePO.setDescription(Constants.DEFAULT_ROLE_NAME);
            } else {
                rolePO = systemRole;
                rolePO.setCid(adminRoleDTO.getCid());
            }
            roleService.saveOrUpdate(rolePO);
            log.info("createAdminRole insert admin role=================================================================>,{}",rolePO);
            TagPO tagPO = new TagPO();
            tagPO.setName(Constants.DEFAULT_ROLE_NAME);
            tagPO.setCid(adminRoleDTO.getCid());
            if (!checkTagExist(tagPO.getName(), rolePO.getId())) {
                tagPO.setObjectid(rolePO.getId());
                tagMapper.insert(tagPO);
            }

            if (ObjectUtils.isEmpty(systemRole)) {
                grantPermission(rolePO.getId(), adminRoleDTO.getCid());
            }
            bindMenu(adminRoleDTO.getCid());
            bindRoleUser(adminRoleDTO.getUserId(), rolePO.getId(), adminRoleDTO.getUserName());
            Long rolePOId = rolePO.getId();
            //创建普通用户角色
            RolePO normalRolePO = new RolePO();
            RolePO dbNormalRole = roleService.getOne(new QueryWrapper<RolePO>().eq("code", adminRoleDTO.getCompanyCode() + "_normalRole"));
            if (ObjectUtils.isEmpty(dbNormalRole)) {
                normalRolePO.setCode(adminRoleDTO.getCompanyCode() + "_normalRole");
                normalRolePO.setCid(adminRoleDTO.getCid());
                normalRolePO.setUuid(UUID.randomUUID().toString());
                normalRolePO.setName(Constants.DEFAULT_NORMAL_ROLE_NAME);
                normalRolePO.setDescription(Constants.DEFAULT_NORMAL_ROLE_NAME);
            } else {
                normalRolePO = dbNormalRole;
                normalRolePO.setCid(adminRoleDTO.getCid());
            }
            roleService.saveOrUpdate(normalRolePO);
            TagPO normalTagPO = new TagPO();
            normalTagPO.setName(Constants.DEFAULT_NORMAL_ROLE_NAME);
            normalTagPO.setCid(adminRoleDTO.getCid());
            if (!checkTagExist(normalTagPO.getName(), normalRolePO.getId())) {
                normalTagPO.setObjectid(normalRolePO.getId());
                tagMapper.insert(normalTagPO);
            }
            if (ObjectUtils.isEmpty(dbNormalRole)) {
                grantNormalPermission(normalRolePO.getId(), adminRoleDTO.getCid());
            }
//            bindNormalMenu(adminRoleDTO.getCid());
            RoleDTO roleDTO = new RoleDTO();
            BeanUtils.copyProperties(rolePO, roleDTO);
            //刷权限速度太慢，会导致feignClient超时，异步处理刷权限过程
            ThreadPoolUtils.getThreadPool().execute(() -> {
                //延时1s，临时处理不同线程事务问题
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                rolePermissionService.freshSubOperate(rolePOId, null, null, adminRoleDTO.getCid());
                userUrlRefService.addUserUrlRefListForUserFlow(Collections.singletonList(adminRoleDTO.getUserId()), adminRoleDTO.getCid(), RpcContext.getContext().getTenantId());
            });
            return roleDTO;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public RoleDTO bindCompanyAdminUser(AdminRoleDTO adminRoleDTO) {
        //查询该公司的公司管理员角色
        //默认公司的公司管理员另外处理
        log.info("创建系统管理员.......................................adminRoleDTO=====1===============,{}",adminRoleDTO);
        RolePO rolePO = new RolePO();
        if (adminRoleDTO.getCid() != 1000L) {
            rolePO = roleService.getOne(new QueryWrapper<RolePO>().eq("CID", adminRoleDTO.getCid()).eq("CODE", adminRoleDTO.getCompanyCode() + "_companySystemRole"));
        } else {
            rolePO = roleService.getById(2L);
        }
        bindRoleUser(adminRoleDTO.getUserId(), rolePO.getId(), adminRoleDTO.getUserName());
        Long roleId = rolePO.getId();
        RoleDTO roleDTO = new RoleDTO();
        BeanUtils.copyProperties(rolePO, roleDTO);
        ThreadPoolUtils.getThreadPool().execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rolePermissionService.freshSubOperate(roleId, null, null, null);
            userUrlRefService.addUserUrlRefListForUserFlow(Collections.singletonList(adminRoleDTO.getUserId()), adminRoleDTO.getCid(), RpcContext.getContext().getTenantId());
        });
        return roleDTO;
    }

    @Override
    public PageResult<RoleDTO> getRolesByCid(Long cid, String keyword, Integer current, Integer pageSize) {
        List<RoleDTO> roleDTOS = new ArrayList<>();

        if (current == null) {
            current = 1;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
        Page<RolePO> page = new Page<>(current, pageSize);
        QueryWrapper<RolePO> queryWrapper = Wrappers.<RolePO>query().eq(RolePO.getCidFieldName(), cid);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> {
                String key = dbStringUtil.getString(keyword);
                if (DbType.ORACLE.equals(dataId.getDataId())) {
                    wrapper.apply("name like {0} escape '\\'", Constants.SQL_LIKE_CHAR + key + Constants.SQL_LIKE_CHAR);
                    wrapper.or().apply("code like {0} escape '\\'", Constants.SQL_LIKE_CHAR + key + Constants.SQL_LIKE_CHAR);
                    wrapper.or().apply("description like {0} escape '\\'", Constants.SQL_LIKE_CHAR + key + Constants.SQL_LIKE_CHAR);
                } else {
                    wrapper.like("name", dbStringUtil.getString(key));
                    wrapper.or().like("code", dbStringUtil.getString(key));
                    wrapper.or().like("description", dbStringUtil.getString(key));
                }
            });
        }
        queryWrapper.eq(RolePO.getValidFieldName(), 1);
        queryWrapper.orderByDesc(RolePO.getNameFieldName());
        IPage<RolePO> rolePOS = roleMapper.getRolePage(page, queryWrapper);
        if (rolePOS.getRecords() == null || rolePOS.getRecords().size() == 0) {
            return new PageResult(roleDTOS, rolePOS.getTotal(), pageSize, current);
        }
        rolePOS.getRecords().forEach(rolePO -> {
            RoleDTO roleDTO = new RoleDTO();
            roleDTOS.add(roleDTO);
            BeanUtils.copyProperties(rolePO, roleDTO);
            roleDTO.setCreateTime(rolePO.getCreateTime());
            roleDTO.setCreator(rolePO.getCreator());
            roleDTO.setModifyTime(rolePO.getModifyTime());
            roleDTO.setModifier(rolePO.getModifier());
        });
        return new PageResult(roleDTOS, rolePOS.getTotal(), pageSize, current);
    }

    @Override
    public void deleteRolesByCodes(List<String> codes) {
        roleService.deleteRolesByArray(codes);
    }

    @Override
    public void createRole(CreateRoleDTO role) {
        if (role.getCode().endsWith("_ADMINISTRATOR") || role.getCode().endsWith("_SECURITY_ADMIN") || role.getCode().endsWith("_SECURITY_AUDITOR")) {
            throw new BizHttpStatusException(RoleErrorEnum.CODE_KEYWORD_ERROR, 400);
        }
        if (!validCode(role.getCode())) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_CODE_FORMAT_ERROR, 400);
        }
        if (role.getCode().length() > 50) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_CODE_LENGTH_TOO_LONG, 400);
        }
        if (role.getName().length() > 50) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_NAME_LENGTH_TOO_LONG, 400);
        }
        if (StringUtils.hasText(role.getDescription()) && role.getDescription().length() > 255) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_DESCRIPTION_LENGTH_TOO_LONG, 400);
        }
        RolePO rolePO = new RolePO();
        BeanUtils.copyProperties(role, rolePO);
        roleService.saveRole(rolePO, null);
    }

    @Override
    public void updateRole(UpdateRoleDTO roleDTO) {

        RolePO rolePO = new RolePO();
        if (StringUtils.isEmpty(roleDTO.getCode())) {
            return;
        }
        if (StringUtils.hasText(roleDTO.getShowName()) && roleDTO.getShowName().length() > 50) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_NAME_LENGTH_TOO_LONG, 400);
        }
        if (StringUtils.hasText(roleDTO.getDescription()) && roleDTO.getDescription().length() > 255) {
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_DESCRIPTION_LENGTH_TOO_LONG, 400);
        }
        if (StringUtils.isEmpty(roleDTO.getShowName()) && StringUtils.isEmpty(roleDTO.getDescription())) {
            return;
        }
        if (StringUtils.hasText(roleDTO.getShowName())) {
            rolePO.setName(roleDTO.getShowName());
        }
        if (StringUtils.hasText(roleDTO.getDescription())) {
            rolePO.setDescription(roleDTO.getDescription());
        }
        roleService.update(rolePO, Wrappers.<RolePO>query().eq(RolePO.getCodeFieldName(), roleDTO.getCode()));
    }

    @Override
    public Result<RoleResourceDTO> getRoleResources(String code) {
        RolePO rolePO = roleMapper.selectOne(Wrappers.<RolePO>query().eq(RolePO.getCodeFieldName(), code));
        if (rolePO == null) {
            return new Result<>();
        }
        List<MenuInfoPO> menuInfoPOS = rolePermissionMapper.getRoleMenus(rolePO.getId());
        if (menuInfoPOS == null) {
            return new Result<>();
        }

        RoleResourceDTO roleResourceDTO = new RoleResourceDTO();
        List<Resource> resources = new ArrayList<>();
        roleResourceDTO.setResources(resources);
        roleResourceDTO.setName(rolePO.getCode());
        roleResourceDTO.setShowname(rolePO.getName());
        roleResourceDTO.setCreateTime(rolePO.getCreateTime());
        roleResourceDTO.setCreateUsername(rolePO.getCreator());
        roleResourceDTO.setModifyTime(rolePO.getModifyTime());
        roleResourceDTO.setModifyUsername(rolePO.getModifier());
        menuInfoPOS.forEach(menuInfoPO -> {
            Resource resource = new Resource();
            resources.add(resource);
            resource.setId(menuInfoPO.getId());
            resource.setParentId(menuInfoPO.getParentId());
            resource.setDescription(menuInfoPO.getMemo());
            if (menuInfoPO.getIsHide() && menuInfoPO.getIsHide()) {
                resource.setHide(1);
            } else {
                resource.setHide(0);
            }
            resource.setName(menuInfoPO.getName());
            resource.setResourceCode(menuInfoPO.getCode());
            resource.setResourceOrder(menuInfoPO.getSort() != null ? menuInfoPO.getSort().toString() : null);
            if (menuInfoPO.getMenuType() != null) {
                switch (menuInfoPO.getMenuType()) {
                    case 0:
                        resource.setResourceFunctionType("folder");
                        break;
                    case 1:
                        resource.setResourceFunctionType("page");
                        break;
                    default:
                        resource.setResourceFunctionType("app");
                        break;
                }
            }
            if (menuInfoPO.getParentId() != null) {
                MenuInfoPO parentMenu = menuInfoService.getById(menuInfoPO.getParentId());
                if (parentMenu != null) {
                    resource.setParentCode(parentMenu.getParentCode());
                }
            }
        });
        return new Result<>(roleResourceDTO);
    }

    private void bindRoleUser(Long userId, Long roleId, String userName) {
        log.info("bindRoleUser start======================================2,{}");
        RoleUserPO roleUserPO = new RoleUserPO();
        roleUserPO.setUserName(userName);
        roleUserPO.setUserId(userId);
        roleUserPO.setRoleId(roleId);
        RoleUserPO roleUserOne = roleUserService.getOne(new QueryWrapper<RoleUserPO>().eq("ROLE_ID", roleId).eq("USER_ID", userId));
        if (ObjectUtils.isEmpty(roleUserOne)) {
            roleUserService.saveOrUpdate(roleUserPO);
            log.info("roleUser save success======================================3,{}",roleUserPO);
            // 检查数据
            List<RoleUserPO> allRoleUser = roleUserService.getAllRoleUser();
            log.info("getAllRoleUser==================================================3.1==={}",allRoleUser);
        }
    }

    private void bindMenu(Long cid) {
        List<String> menuCode = Arrays.asList("user", "online", "ip", "role", "organization", "personmanage", "organizationmanage", "organizationcompany", "groupmanage", "auth", "personalInfo", "baseInfo", "editPassword", "selfstationletter");
        List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in("CODE", menuCode));
        List<MenuInfoCompanyRefPO> list = new ArrayList<>();
        menuInfoPOS.forEach(menuInfoPO -> {
            MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
            menuInfoCompanyRefPO.setMenuinfoName(menuInfoPO.getName());
            menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
            menuInfoCompanyRefPO.setCompanyId(cid);
            list.add(menuInfoCompanyRefPO);
        });
        menuInfoCompanyRefService.saveOrUpdateBatch(list);
    }

    private void bindNormalMenu(Long cid) {
        List<String> menuCode = Arrays.asList("personalInfo", "baseInfo", "editPassword", "selfstationletter");
        List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in("CODE", menuCode));
        List<MenuInfoCompanyRefPO> list = new ArrayList<>();
        menuInfoPOS.forEach(menuInfoPO -> {
            MenuInfoCompanyRefPO menuInfoCompanyRefPO = new MenuInfoCompanyRefPO();
            menuInfoCompanyRefPO.setMenuinfoName(menuInfoPO.getName());
            menuInfoCompanyRefPO.setMenuinfoId(menuInfoPO.getId());
            menuInfoCompanyRefPO.setCompanyId(cid);
            list.add(menuInfoCompanyRefPO);
        });
        menuInfoCompanyRefService.saveOrUpdateBatch(list);
    }

    private boolean validCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return false;
        }
        return code.matches("[A-Za-z0-9_]+");
    }

    private void grantPermission(Long roleId, Long cid) {
        List<String> permissionCode = Arrays.asList(
                "queryUser",
                "queryUserPermission",
                "onlineUser",
                "addRole",
                "editRole",
                "deleteRole",
                "queryRole",
                "queryRoleUser",
                "addRoleUser",
                "deleteRoleUser",
                "queryRolePermissions",
                "addPerson",
                "updatePerson",
                "deletePerson",
                "importPerson",
                "exportPerson",
                "offPosition",
                "queryPersonDetailById",
                "transferPosition",
                "querypersonManager",
                "importOrg",
                "importPos",
                "addDepartment",
                "updateDepartment",
                "deleteDep",
                "getDepTree",
                "addPosition",
                "getPosDetail",
                "companies",
                "addCompany",
                "updateCompany",
                "delCompany",
                "company",
                "createGroup",
                "updateGroup",
                "deletGroup",
                "queryGroup",
                "updateUser",
                "lockUser",
                "deleteUser",
                "createUser",
                "updateOperate",
                "queryOperate",
                "IpConfig",
                "deletePos",
                "updatePos",
                "importUser",
                "personalInfo_default",
                "editPassword_default",
                "openMsgManager");
        rolePermissionService.grantPermission(roleId, permissionCode, cid);
    }

    private void grantNormalPermission(Long roleId, Long cid) {
        List<String> permissionCode = Arrays.asList(
                "personalInfo_default",
                "baseInfo_default",
                "editPassword_default",
                "openMsgManager");
        rolePermissionService.grantPermission(roleId, permissionCode, cid);
    }

    /**
     * @description: 校验标签是否存在
     * @param: tagName 标签名
     * @return: boolean
     * @author: 袁阳
     * @date: 2020/6/10
     */
    private boolean checkTagExist(String tagName, Long roleId) {
        QueryWrapper<TagPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", tagName);
        queryWrapper.eq("objectid", roleId);
        Integer count = tagMapper.selectCount(queryWrapper);
        return count > 0;
    }

}
