package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.thread.ConcurrentHashSet;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.exception.ExcelErrorEnum;
import com.supcon.supfusion.rbac.common.exception.ExcelException;
import com.supcon.supfusion.rbac.common.exception.RoleUserErrorEnum;
import com.supcon.supfusion.rbac.common.exception.RoleUserException;
import com.supcon.supfusion.rbac.common.utils.ExcelFactory;
import com.supcon.supfusion.rbac.dao.RoleMapper;
import com.supcon.supfusion.rbac.dao.RoleUserMapper;
import com.supcon.supfusion.rbac.dao.field.RoleUserField;
import com.supcon.supfusion.rbac.manager.IUserAdapter;
import com.supcon.supfusion.rbac.service.DataPermissionService;
import com.supcon.supfusion.rbac.service.IUserUrlRefService;
import com.supcon.supfusion.rbac.service.bo.ExportFileStatusBO;
import com.supcon.supfusion.rbac.dao.po.RolePO;
import com.supcon.supfusion.rbac.dao.po.RoleUserPO;
import com.supcon.supfusion.rbac.service.IRolePermissionService;
import com.supcon.supfusion.rbac.service.IRoleUserService;
import com.supcon.supfusion.rbac.service.bo.UserDetailBO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.xmlbeans.SystemProperties;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色用户表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@Service
@Transactional
public class RoleUserServiceImpl extends ServiceImpl<RoleUserMapper, RoleUserPO> implements IRoleUserService {


    @Autowired
    private RoleUserMapper roleuserMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private IRolePermissionService rolePermissionService;
    @Autowired
    private IUserAdapter userAdapter;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Qualifier("rbacRedisTemplate")
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Autowired
    private DataPermissionService dataPermissionService;

    private final Map<String, String> alias;

    private final ExcelFactory excelFactory;

    private final String EXPORT_PREFIX = "export_template:";

    public RoleUserServiceImpl() {
        this.alias = new HashMap<>();
        alias.put("用户名", "USERNAME");
        alias.put("人员名称", "PERSONNAME");
        alias.put("人员编码", "PERSONCODE");
        excelFactory = new ExcelFactory(alias);
    }

    /**
     * @description: 角色添加用户
     * @param: params
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @Override
    @Transactional
    public void saveRoleUsers(Long roleId, List<UserDetailBO> userDetailBOS) {
        if (ObjectUtils.isEmpty(roleId)) {
            throw new RoleUserException(RoleUserErrorEnum.NO_ROLE_FIND);
        }
        RolePO rolePO = roleMapper.selectById(roleId);
        if (ObjectUtils.isEmpty(rolePO)) {
            throw new RoleUserException(RoleUserErrorEnum.NO_ROLE_FIND);
        }
        //新增RoleUser
        List<RoleUserPO> roleUserPOS = new ArrayList<>();
        //用于收集未关联过角色的用户ID，然后这些ID是需要刷权限的
        List<Long> newRoleUser = new ArrayList<>();
        userDetailBOS.forEach(userDetailBO -> {
            //没有关联过该角色才可新增,否则修改
            if (!checkRoleUserExist(roleId, userDetailBO.getId())) {
                RoleUserPO roleUserPOTemp = new RoleUserPO();
                roleUserPOTemp.setRoleId(roleId);
                roleUserPOTemp.setUserId(userDetailBO.getId());
                roleUserPOTemp.setUserName(userDetailBO.getUserName());
                roleUserPOTemp.setPersonCode(userDetailBO.getPersonCode());
                roleUserPOTemp.setPersonName(userDetailBO.getPersonName());
                roleUserPOS.add(roleUserPOTemp);
                newRoleUser.add(userDetailBO.getId());
            } else {
                RoleUserPO roleUserPO = roleuserMapper.selectOne(new QueryWrapper<RoleUserPO>().eq("ROLE_ID", roleId).eq("USER_ID", userDetailBO.getId()));
                roleUserPO.setUserName(userDetailBO.getUserName());
                roleUserPO.setPersonCode(userDetailBO.getPersonCode());
                roleUserPO.setPersonName(userDetailBO.getPersonName());
                //如果是来自岗位，则更新为来自岗位、角色
                if (2 == roleUserPO.getFromPosition()) {
                    roleUserPO.setFromPosition(3);
                }
                this.saveOrUpdate(roleUserPO);
            }
        });
        //批量新增
        saveBatch(roleUserPOS);

        //刷新用户权限
        if (newRoleUser.size() > 0) {
            newRoleUser.forEach(userId -> {
                rolePermissionService.freshSubOperate(roleId, null, userId,null);
                /**
                 * 数据权限
                 */
                dataPermissionService.bindUserPermissionByRole(roleId, userId);
                rolePermissionService.freshSubOperate(roleId, null, userId, null);
            });
            userUrlRefService.addUserUrlRefListForUserFlow(newRoleUser, null, RpcContext.getContext().getTenantId());
        }
        //用户服务关联角色
        if (!ObjectUtils.isEmpty(userDetailBOS)) {
            List<Long> userIds = userDetailBOS.stream().map(UserDetailBO::getId).collect(Collectors.toList());
            userAdapter.bindRole(roleId, userIds, true);
        }
    }

    /**
     * @description: 角色用户关联删除
     * @param: roleUserId 角色关联ID数组
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/9
     */
    @Override
    @Transactional
    public void deleteRoleUsers(String roleUserIds) {
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("ID", Arrays.asList(roleUserIds.split(",")));
        queryWrapper.select("ID", "ROLE_ID", "USER_ID", "FROM_POSITION");
        List<RoleUserPO> roleUserPOS = roleuserMapper.selectList(queryWrapper);
        List<RoleUserPO> roleUserPOList = roleUserPOS.stream().filter(roleUserPO -> 3 == roleUserPO.getFromPosition()).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(roleUserPOList)) {
            roleuserMapper.update(null, new UpdateWrapper<RoleUserPO>().in(RoleUserField.id, roleUserPOList.stream()
                    .map(RoleUserPO::getId)
                    .collect(Collectors.toList())).set(RoleUserField.fromPosition, 2).set(RoleUserField.positionFlag, 1));
        }
        List<RoleUserPO> roleUserPOList1 = roleUserPOS.stream().filter(roleUserPO -> 1 == roleUserPO.getFromPosition()).collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(roleUserPOList1)) {
            roleuserMapper.deleteBatchIds(roleUserPOList1.stream().map(RoleUserPO::getId).collect(Collectors.toList()));
            /**
             * 数据权限
             */
            roleUserPOList1.forEach(roleUserPO -> dataPermissionService.unbindUserPermissionByRole(roleUserPO.getRoleId(), roleUserPO.getUserId()));
        }
        if (!ObjectUtils.isEmpty(roleUserPOS)) {
            roleUserPOS.forEach(roleUserPO -> {
                rolePermissionService.freshSubOperate(roleUserPO.getRoleId(), null, roleUserPO.getUserId(), null);
            });
            userUrlRefService.addUserUrlRefListForUserFlow(roleUserPOS.stream().map(RoleUserPO::getUserId).collect(Collectors.toList()), null, RpcContext.getContext().getTenantId());
        }
        //用户服务解除关联角色
        if (!ObjectUtils.isEmpty(roleUserPOS)) {
            Map<Long, List<RoleUserPO>> roleUsersGroup = roleUserPOS.stream().collect(Collectors.groupingBy(RoleUserPO::getRoleId));
            roleUsersGroup.forEach((roleId, roleUsers) -> {
                List<Long> userIds = roleUsers.stream().map(RoleUserPO::getUserId).collect(Collectors.toList());
                userAdapter.bindRole(roleId, userIds, false);
            });
        }
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
    @Override
    public PageResult<RoleUserPO> findByPage(String roleCode, String keyword, Integer current, Integer pageSize, Long cid) {
        Page<RoleUserPO> page = new Page<>(current, pageSize);
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("roleUser.VALID", 1);
        if (!ObjectUtils.isEmpty(roleCode)) {
            queryWrapper.eq("role.code", roleCode);
        }
        if (!ObjectUtils.isEmpty(cid)) {
            queryWrapper.eq("role.cid", cid);
        } else {
            queryWrapper.eq("role.cid", UserContext.getUserContext().getCompanyId());
        }
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapper.and(roleUserPOQueryWrapper -> roleUserPOQueryWrapper.apply(RoleUserField.userName + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR).or().apply(RoleUserField.personName + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR).or().apply(RoleUserField.personCode + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR));
            } else {
                queryWrapper.and(roleUserPOQueryWrapper -> roleUserPOQueryWrapper.like(RoleUserField.userName, dbStringUtil.getString(keyword)).or().like(RoleUserField.personName, dbStringUtil.getString(keyword)).or().like(RoleUserField.personCode, dbStringUtil.getString(keyword)));
            }
        }
        queryWrapper.orderByDesc("roleUser.modify_time");
        //分页查询
        IPage<RoleUserPO> roleUsersPage = roleuserMapper.getRoleUsersPage(page, queryWrapper);
        //构建pageResult
        return new PageResult<>(roleUsersPage.getRecords(), roleUsersPage.getTotal(), roleUsersPage.getSize(), roleUsersPage.getCurrent());
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Set<Object> set = new ConcurrentHashSet<>();
        return t -> set.add(keyExtractor.apply(t));
    }

    /**
     * @description: 导出接口
     * @param: roleUserIds
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/24
     */
    @Override
    public void createTemp(List<Long> roleUserIds, String id, Long roleId, String keyword) {
        //查询需要导出的数据
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ru.valid", 1);
        if (!ObjectUtils.isEmpty(roleUserIds)) {
            queryWrapper.in("ru.ID", roleUserIds);
        }
        if (!ObjectUtils.isEmpty(roleId)) {
            queryWrapper.eq("ru.ROLE_ID", roleId);
        }
        if (!ObjectUtils.isEmpty(keyword)) {
            queryWrapper.and(roleUserPOQueryWrapper -> roleUserPOQueryWrapper.like("ru.USER_ID", keyword)
                    .or().like("ru.USER_NAME", keyword).or().like("ru.PERSON_NAME", keyword).or().like("ru.PERSON_CODE", keyword));
        }
        List<Map<String, Object>> exportData = roleuserMapper.exportData(queryWrapper);
        generateExcel(id, exportData);
    }

    /**
     * @description: 判断该用户是否已在该角色下
     * @param: roleId
     * @param: userId
     * @return: boolean
     * @author: 袁阳
     * @date: 2020/6/9
     */
    public boolean checkRoleUserExist(Long roleId, Long userId) {
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("ROLE_ID", roleId);
        queryWrapper.eq("USER_ID", userId);
        Integer count = roleuserMapper.selectCount(queryWrapper);
        return count > 0;
    }

    /**
     * @description: 生成excel文件
     * @param: current
     * @param: pageSize
     * @param: id
     * @param: roleId
     * @param: keyword
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void createTemp(int current, int pageSize, String id, Long roleId, String keyword) {
        //查询需要导出的数据
        QueryWrapper<RoleUserPO> queryWrapper = new QueryWrapper<>();
        Page<RoleUserPO> page = new Page<>(current, pageSize);
        queryWrapper.eq("ru.valid", 1);
        if (!ObjectUtils.isEmpty(roleId)) {
            queryWrapper.eq("ru.ROLE_ID", roleId);
        }
        if (!ObjectUtils.isEmpty(keyword)) {
            if (DbType.ORACLE.equals(dataId.getDataId())) {
                queryWrapper.and(roleUserPOQueryWrapper -> roleUserPOQueryWrapper.apply("ru." + RoleUserField.userId + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR)
                        .or().apply("ru." + RoleUserField.userName + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR).or().apply("ru." + RoleUserField.personName + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR).or().apply("ru." + RoleUserField.personCode + " like {0} escape '\\'", Constants.SQL_LIKE_CHAR + dbStringUtil.getString(keyword) + Constants.SQL_LIKE_CHAR));
            } else {
                queryWrapper.and(roleUserPOQueryWrapper -> roleUserPOQueryWrapper.like("ru." + RoleUserField.userId, dbStringUtil.getString(keyword))
                        .or().like("ru." + RoleUserField.userName, dbStringUtil.getString(keyword)).or().like("ru." + RoleUserField.personName, dbStringUtil.getString(keyword)).or().like("ru." + RoleUserField.personCode, dbStringUtil.getString(keyword)));
            }
        }
        List<Map<String, Object>> exportData = roleuserMapper.exportDataPage(page, queryWrapper);
        generateExcel(id, exportData);
    }

    /**
     * @description: 生成excel文件
     * @param: id
     * @param: exportData
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    private void generateExcel(String id, List<Map<String, Object>> exportData) {
        //TODO 文件路径待定
        String path = "." + SystemProperties.getProperty("file.separator");
        ExportFileStatusBO exportFileStatusBO = new ExportFileStatusBO();
        exportFileStatusBO.setFilePath(path + id + ".xlsx");
        exportFileStatusBO.setId(id);
        HashOperations<String, Object, Object> keyObj = redisTemplate.opsForHash();
        //状态码2 文件正在创建中
        exportFileStatusBO.setStatus(2);
        keyObj.put("keyObj", id, exportFileStatusBO);
        try {
            List<String> header = Arrays.asList("用户名", "人员名称", "人员编码");
            Workbook excel = excelFactory.createExcel(header, exportData);
            excelFactory.createTempFile(excel, path + id + ".xlsx");
            //状态码1 文件创建成功
            exportFileStatusBO.setStatus(1);
            //在redis设置文件超时时间
            ValueOperations<String, Object> op = redisTemplate.opsForValue();
            op.set(EXPORT_PREFIX + id, "", 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            //状态码0 文件创建失败
            exportFileStatusBO.setStatus(0);
            log.error(e.getMessage());
        }
        keyObj.put("keyObj", id, exportFileStatusBO);
    }

    /**
     * @description: 导出excel文件
     * @param: id
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void export(String id) {
        HashOperations<String, Object, Object> hash = redisTemplate.opsForHash();
        Object o = hash.get("keyObj", id);
        if (!ObjectUtils.isEmpty(o)) {
            ExportFileStatusBO exportFilePO = (ExportFileStatusBO) o;
            File file = new File(exportFilePO.getFilePath());
            try {
                FileInputStream fi = new FileInputStream(file);
                excelFactory.output(fi, Constants.EXPORT_EXCEL_NAME);
            } catch (FileNotFoundException e) {
                log.error(e.getMessage());
            }
        } else {
            throw new ExcelException(ExcelErrorEnum.FILE_NOT_EXIST);
        }
    }

    @Override
    public List<RoleUserPO> getAllRoleUser() {
        List<RoleUserPO> allRoleUser = roleuserMapper.getAllRoleUser();
        return allRoleUser;
    }
}
