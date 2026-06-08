package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.RoleIdDTO;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.RoleErrorEnum;
import com.supcon.supfusion.rbac.common.exception.RoleException;
import com.supcon.supfusion.rbac.dao.RoleMapper;
import com.supcon.supfusion.rbac.dao.RoleUserMapper;
import com.supcon.supfusion.rbac.dao.TagMapper;
import com.supcon.supfusion.rbac.dao.enums.FlowPermissionType;
import com.supcon.supfusion.rbac.dao.field.FlowPermissionField;
import com.supcon.supfusion.rbac.dao.field.RoleField;
import com.supcon.supfusion.rbac.dao.field.TagField;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.dao.po.TagPO;
import com.supcon.supfusion.rbac.service.IRoleMneCodeService;
import com.supcon.supfusion.rbac.service.IRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@Service
@Transactional
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RolePO> implements IRoleService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleUserMapper roleuserMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Autowired
    private IRoleMneCodeService roleMneCodeService;
    @ServiceApiReference
    private PersonApiService personApiService;

    /**
     * @description: 查询角色树
     * @param: keyword
     * @param: tag
     * @param: cid
     * @return: java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public List<Map<String, Object>> getRoleTreeSingleCompany(String keyword, String tag, Long cid) {
        //查询无标签角色 条件构造
        QueryWrapper<RolePO> queryWrapperNoTag = new QueryWrapper<>();
        //查询tag 条件构造
        QueryWrapper<TagPO> queryWrapperTag = new QueryWrapper<>();
        queryWrapperTag.eq("tag." + TagField.valid, 1);
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapperTag.and(tagPOQueryWrapper -> tagPOQueryWrapper.apply("role." + RoleField.code + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR).or().apply("role." + RoleField.name + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR));
                queryWrapperNoTag.and(rolePOQueryWrapper -> rolePOQueryWrapper.apply(RoleField.code + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR).or().apply(RoleField.name + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR));
            } else {
                queryWrapperTag.and(tagPOQueryWrapper -> tagPOQueryWrapper.like("role." + RoleField.code, dbStringUtil.getString(keyword)).or().like("role." + RoleField.name + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR));
                queryWrapperNoTag.and(rolePOQueryWrapper -> rolePOQueryWrapper.like(RoleField.code, dbStringUtil.getString(keyword)).or().like(RoleField.name, dbStringUtil.getString(keyword)));
            }
        }
        if (!ObjectUtils.isEmpty(cid)) {
            queryWrapperNoTag.eq(RoleField.cid, cid);
            queryWrapperTag.eq("role." + RoleField.cid, cid);
        } else {
            queryWrapperNoTag.eq(RoleField.cid, UserContext.getUserContext().getCompanyId());
            queryWrapperTag.eq("role." + RoleField.cid, UserContext.getUserContext().getCompanyId());
        }
        if (!ObjectUtils.isEmpty(tag)) {
            queryWrapperTag.like("tag." + TagField.name, tag);
        }
        queryWrapperTag.groupBy("tag." + TagField.name);
        //获取 objectid 根据tagName 分组
        //区分数据库类型
        List<Map<String, Object>> objectIdGroupByTagNameTemp = new ArrayList<>();
        if (!ObjectUtils.isEmpty(dataId.getDataId()) && dataId.getDataId().equals(DbType.SQLSERVER)) {
            queryWrapperTag.groupBy("tag." + TagField.name);
            objectIdGroupByTagNameTemp = tagMapper.findObjectIdGroupByTagNameSqlServer(queryWrapperTag);
        } else if (!ObjectUtils.isEmpty(dataId.getDataId()) && dataId.getDataId().equals(DbType.ORACLE)) {
            objectIdGroupByTagNameTemp = tagMapper.findObjectIdGroupByTagNameOracle(queryWrapperTag);
        } else {
            objectIdGroupByTagNameTemp = tagMapper.findObjectIdGroupByTagName(queryWrapperTag);
        }
        List<Map<String, Object>> objectIdGroupByTagName = new ArrayList<>();
        //把key转为小写
        objectIdGroupByTagNameTemp.forEach(map -> {
            Map<String, Object> mapTemp = new HashMap<>();
            map.keySet().forEach(key -> {
                if (key.equals("NAME")) {
                    mapTemp.put(key.toLowerCase(), map.get(key));
                } else {
                    mapTemp.put(key, map.get(key));
                }
            });
            objectIdGroupByTagName.add(mapTemp);
        });
        //用于收集有标签的角色id
        Set<Long> hasTagRoleIds = new HashSet<>();
        //循环查每个标签下的所有角色
        objectIdGroupByTagName.forEach(map -> {
            //逗号分隔的objectIds
            Object objectIds = String.valueOf(map.get("objectIds"));
            //前端不需要objectIds 所以清除
            map.remove("objectIds");
            if (!ObjectUtils.isEmpty(objectIds) || !"null".equals(objectIds)) {
                //重新构建查询条件 用于查询role
                QueryWrapper<RolePO> rolePOQueryWrapper = new QueryWrapper<>();
                List<Long> idsList = Arrays.stream(String.valueOf(objectIds).split(",")).map(Long::parseLong).collect(Collectors.toList());
                hasTagRoleIds.addAll(idsList);
                rolePOQueryWrapper.in("id", idsList);
                List<RolePO> rolePOS = roleMapper.selectList(rolePOQueryWrapper);
                map.put("children", rolePOS);
            }
        });
        //去除没有角色的标签
        List<Map<String, Object>> TagMaps = objectIdGroupByTagName.stream().filter(map -> !ObjectUtils.isEmpty(map.get("children"))).collect(Collectors.toList());

        //not in 有标签角色ID
        if (!ObjectUtils.isEmpty(hasTagRoleIds)) {
            queryWrapperNoTag.notIn("id", hasTagRoleIds);
        }
        //查询无标签role
        List<RolePO> rolePOSNoTag = roleMapper.selectList(queryWrapperNoTag);
        Map<String, Object> roleTreeNoTag = new HashMap<>();
        roleTreeNoTag.put("name", "无标签");
        roleTreeNoTag.put("children", rolePOSNoTag);
        //将无标签的放入返回数据中
        if (!ObjectUtils.isEmpty(rolePOSNoTag)) {
            TagMaps.add(roleTreeNoTag);
        }
        return TagMaps;
    }

    /**
     * @description: 查询角色树不分页
     * @param: keyword 通过关键字（编码、名称）模糊查询
     * @param: tag 标签
     * @return: java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @Override
    public List<Map<String, Object>> getRoleTree(String keyword, String tag) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(keyword)) {
            queryWrapper.and(rolePOQueryWrapper -> rolePOQueryWrapper.like("code", keyword).or().like("name", keyword));
        }
        QueryWrapper<TagPO> queryWrapperTag = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(tag)) {
            queryWrapperTag.like("name", tag);
        }
        queryWrapperTag.orderByAsc("id");
        queryWrapperTag.groupBy("name");
        //获取 objectid 根据tagName 分组
        List<Map<String, Object>> objectIdGroupByTagName = tagMapper.findObjectIdGroupByTagName(queryWrapperTag);
        //用于收集有标签的角色id
        Set<Long> hasTagRoleIds = new HashSet<>();
        objectIdGroupByTagName.forEach(map -> {
            Object objectIds = map.get("objectIds");
            if (!ObjectUtils.isEmpty(objectIds)) {
                List<Long> idsList = Arrays.stream(String.valueOf(objectIds).split(",")).map(Long::parseLong).collect(Collectors.toList());
                hasTagRoleIds.addAll(idsList);
                queryWrapper.in("id", idsList);
                List<RolePO> rolePOS = roleMapper.selectList(queryWrapper);
                map.put("children", rolePOS);
            }
        });
        //查询无标签角色
        QueryWrapper<RolePO> queryWrapperNoTag = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(keyword)) {
            queryWrapperNoTag.and(rolePOQueryWrapper -> rolePOQueryWrapper.like("code", keyword).or().like("name", keyword));
        }
        if (!ObjectUtils.isEmpty(hasTagRoleIds)) {
            queryWrapperNoTag.notIn("id", hasTagRoleIds);
        }
        List<RolePO> rolePOSNoTag = roleMapper.selectList(queryWrapperNoTag);
        Map<String, Object> roleTreeNoTag = new HashMap<>();
        roleTreeNoTag.put("name", "无标签");
        roleTreeNoTag.put("children", rolePOSNoTag);
        //将无标签的放入返回数据中
        objectIdGroupByTagName.add(roleTreeNoTag);
        return objectIdGroupByTagName;
    }

    /**
     * @description: 新增
     * @param: role 前台传过来的角色 parentId、code、name、roleType、description、tags
     * @return: boolean
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @Override
    public void saveRole(RolePO role, List<String> tags) {
        UserContext userContext = UserContext.getUserContext();
        if (!checkCodeUnique(role.getCode(), role.getId())) {
            throw new BizHttpStatusException(RoleErrorEnum.UNIQUECODE, 400);
        }
        if (!checkNameUniqe(role)) {
            throw new RoleException(RoleErrorEnum.UNIQUENAME);
        }
//        log.debug(userContext.getCompanyId().toString());
        if (role.getCid() == null || role.getCid() == -1l) {
            if (null != userContext && null != userContext.getCompanyId()) {
                role.setCid(userContext.getCompanyId());
            } else {
                log.error("role.getcid is null");
            }
        }
        if (ObjectUtils.isEmpty(role.getUuid())) {
            role.setUuid(UUID.randomUUID().toString());
        }
        roleMapper.insert(role);
        //保存标签
//        if (!ObjectUtils.isEmpty(tags)) {
//            tags.forEach(tag -> {
//                if (tag.length() > 50){
//                    throw new RoleException(RoleErrorEnum.TAG_LENGTH_LIMIT_50);
//                }
//                //如果标签不存在 则创建标签
//                if (!checkTagExist(tag, role.getId())) {
//                    TagPO tagPO = new TagPO();
//                    tagPO.setCid(userContext.getCompanyId());
//                    tagPO.setName(tag);
//                    tagPO.setObjectid(role.getId());
//                    tagMapper.insert(tagPO);
//                }
//            });
//        }
        //创建角色助记码
        roleMneCodeService.createRoleMneCode(Collections.singletonList(role));
    }

    /**
     * @description: 保存
     * @param: role
     * @param: deleteIds
     * @param: tags
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/11
     */
    @Override
    public void update(RolePO role, List<Long> deleteIds, List<String> tags) {
        UserContext userContext = UserContext.getUserContext();
        RolePO rolePO = roleMapper.selectOne(new QueryWrapper<RolePO>().eq("code", role.getCode()));
        if (ObjectUtils.isEmpty(rolePO)) {
            throw new RoleException(RoleErrorEnum.ROLE_CANNOT_FIND);
        }
        role.setId(rolePO.getId());
//        if (!checkNameUniqe(role)) {
//            throw new RoleException(RoleErrorEnum.UNIQUENAME);
//        }
        if (role.getCid() == null || role.getCid() == -1l) {
            role.setCid(userContext.getCompanyId());
        }
        if (ObjectUtils.isEmpty(role.getUuid())) {
            role.setUuid(UUID.randomUUID().toString());
        }
        roleMapper.updateById(role);
        //删除标签
        if (!ObjectUtils.isEmpty(deleteIds)) {
            QueryWrapper<TagPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", deleteIds);
            tagMapper.deleteTag(queryWrapper);
        }
        //保存标签
//        if (!ObjectUtils.isEmpty(tags)) {
//            tags.forEach(tag -> {
//                if (tag.length() > 50){
//                    throw new RoleException(RoleErrorEnum.TAG_LENGTH_LIMIT_50);
//                }
//                //如果标签不存在 则创建标签
//                if (!checkTagExist(tag, role.getId())) {
//                    TagPO tagPO = new TagPO();
//                    tagPO.setCid(userContext.getCompanyId());
//                    tagPO.setName(tag);
//                    tagPO.setObjectid(role.getId());
//                    tagMapper.insert(tagPO);
//                }
//            });
//        }
        //创建角色助记码
        roleMneCodeService.createRoleMneCode(Collections.singletonList(role));
    }

    /**
     * @description: 判断角色编码是否唯一
     * @param: roleCode 角色code
     * @param: id 角色id
     * @return: boolean
     * @author: 袁阳
     * @date: 2020/6/8
     */
    public boolean checkCodeUnique(String roleCode, Long id) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        if (id == null) {
            queryWrapper.eq("code", roleCode);
        } else {
            queryWrapper.eq("code", roleCode);
            queryWrapper.ne("id", id);
        }
        Integer count = roleMapper.selectCount(queryWrapper);
        return count == 0;
    }

    /**
     * @description: 名称唯一？代码照搬，不明白是干了啥
     * @param: role 角色
     * @return: boolean
     * @author: 袁阳
     * @date: 2020/6/8
     */
    public boolean checkNameUniqe(RolePO role) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", role.getCode());
        if (null == role.getParentId() || role.getParentId() == -1) {
            queryWrapper.and(i -> i.eq("parent_id", "-1").or().isNull("parent_id"));
        } else {
            queryWrapper.eq("parent_id", role.getParentId());
        }
        if (null != role.getId()) {
            queryWrapper.eq("id", role.getId());
        }
        Integer count = roleMapper.selectCount(queryWrapper);
        return count == 0;
    }


    /**
     * @description: 删除角色
     * @param: codes 角色codes
     * @author: 袁阳
     * @date: 2020/6/8
     */
    @Override
    public void deleteRoles(String codes) {
        if (!ObjectUtils.isEmpty(codes)) {
            deleteRolesByArray(Arrays.asList(codes.split(",")));
        }
    }


    public void deleteRolesByArray(List<String> codes) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(codes)) {
            queryWrapper.in("code", codes);
            //只查询id
            queryWrapper.select("id");
            //查询codes对应的Ids
            List<Object> ids_obj = roleMapper.selectObjs(queryWrapper);
            if (ids_obj == null || ids_obj.size() == 0) {
                return;
            }
            List<Long> ids = ids_obj.stream().map(o -> Long.valueOf(String.valueOf(o))).collect(Collectors.toList());
            checkDeletePosition(ids);
            checkDeletePermit(ids_obj);
            deleteChildrenRoles(ids_obj);
            roleMapper.deleteBatchIds(ids);
            roleMneCodeService.deleteMneCodeByIds(ids);
        }
    }

    /**
     * @description: 删除子角色
     * @param: ids 角色id数组
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/8
     */
    public void deleteChildrenRoles(List<Object> ids) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("parent_id", ids);
        List<RolePO> rolePOS = roleMapper.selectList(queryWrapper);
        if (!ObjectUtils.isEmpty(rolePOS)) {
            deleteRoles(rolePOS.stream().map(RolePO::getCode).collect(Collectors.joining(",")));
        }
    }

    /**
     * @description: 判断约束
     * @param: codes 角色codes
     * @param: ids 角色id数组
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/8
     */
    private void checkDeletePermit(List<Object> ids) {
        QueryWrapper<RoleUserPO> queryWrapperUser = new QueryWrapper<>();
        queryWrapperUser.in("ROLE_ID", ids);
        Integer countUser = roleuserMapper.selectCount(queryWrapperUser);
        if (countUser > 0) {
            throw new BizHttpStatusException(RoleErrorEnum.ASSO_WITH_ROLEUSER, 400);
        }
        QueryWrapper<RolePO> queryWrapperRole = new QueryWrapper<>();
        queryWrapperRole.in("parent_id", ids);
        Integer countRole = roleMapper.selectCount(queryWrapperRole);
        if (countRole > 0) {
            throw new BizHttpStatusException(RoleErrorEnum.HAS_CHILD_ROLE, 400);
        }
    }

    private void checkDeletePosition(List<Long> ids) {
        RoleIdDTO roleIdDTO = new RoleIdDTO();
        roleIdDTO.setRoleIds(ids);
        Result<Boolean> checkRolesExistPositionResult = personApiService.checkRolesExistPosition(roleIdDTO);
        if (checkRolesExistPositionResult == null || checkRolesExistPositionResult.getData() == null || checkRolesExistPositionResult.getData().booleanValue()) {
            throw new BizHttpStatusException(RoleErrorEnum.ASSO_WITH_ROLEPOSITION, 400);
        }
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

    @Override
    public List<RolePO> getRoleByUserPermission(Long menuOperateId, Long userId) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ru.USER_ID", userId);
        queryWrapper.eq("rr.MENUOPERATE_ID", menuOperateId);
        queryWrapper.eq("ru.valid", 1);
        queryWrapper.groupBy("r.id,r.name");
        return roleMapper.getRoleByUserPermission(queryWrapper);
    }

    @Override
    public List<RolePO> getRoleByUserPermissionFlow(String menuOperateCode, List<Long> roleIds) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("fp." + FlowPermissionField.typeId, roleIds);
        queryWrapper.eq("fp." + FlowPermissionField.activityCode, menuOperateCode);
        queryWrapper.eq("fp." + FlowPermissionField.flowPermissionType, FlowPermissionType.ROLE);
        if (ObjectUtils.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        return roleMapper.getRoleByUserPermissionFlow(queryWrapper);
    }

    @Override
    public List<RolePO> getRoleTreeNoTag(String keyword, Long cid) {
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapper.apply("name like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR);
            } else {
                queryWrapper.like("name", dbStringUtil.getString(keyword));
            }
        }
        if (!ObjectUtils.isEmpty(cid)) {
            queryWrapper.eq("cid", cid);
        } else {
            queryWrapper.eq("cid", UserContext.getUserContext().getCompanyId());
        }
        return this.list(queryWrapper);
    }

    /**
     * @description: 分页查询角色列表
     * @param: current 翻页的页数
     * @param: pageSize 每页返回的元数量
     * @return: com.supcon.supfusion.framework.cloud.common.result.PageResult
     * @author: xhf
     */
    @Override
    public PageResult<RolePO> getRolesByPage(Integer current, Integer pageSize) {
        Page<RolePO> page = new Page<>(current, pageSize);
        QueryWrapper<RolePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("valid", true);

        //分页查询
        Page<RolePO> rolesPage = roleMapper.selectPage(page, queryWrapper);
        //构建pageResult
        return new PageResult<>(rolesPage.getRecords(), rolesPage.getTotal(), rolesPage.getSize(), rolesPage.getCurrent());
    }

    @Override
    public PageResult<RolePO> querySubRolesByParentCode(String roleCode, Boolean all, Integer current, Integer pageSize) {
        QueryWrapper<RolePO> rolePOQueryWrapper = new QueryWrapper<>();
        rolePOQueryWrapper.eq("valid", true);
        rolePOQueryWrapper.eq("code", roleCode);
        RolePO rolePO = roleMapper.selectOne(rolePOQueryWrapper);

        if (rolePO == null){
            throw new BizHttpStatusException(RoleErrorEnum.ROLE_CANNOT_FIND, 400);
        }

        Long parentId = rolePO.getId();
        QueryWrapper<RolePO> subRoleWrapper = new QueryWrapper<>();
        subRoleWrapper.eq("valid", true);

        if (all) {
            subRoleWrapper.likeRight("FULL_PATH_NAME", rolePO.getFullPathName() + "/");
        } else {
            subRoleWrapper.eq("parent_id", parentId);
        }
        // 排序
        subRoleWrapper.orderByDesc("modify_time");
        // 分页
        Page<RolePO> departmentPage = new Page<>(current, pageSize);
        Page<RolePO> departmentResPage = roleMapper.selectPage(departmentPage, subRoleWrapper);
        List<RolePO> list = departmentResPage.getRecords();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return new PageResult<RolePO>(list, departmentResPage.getTotal(), pageSize, current);
    }
}
