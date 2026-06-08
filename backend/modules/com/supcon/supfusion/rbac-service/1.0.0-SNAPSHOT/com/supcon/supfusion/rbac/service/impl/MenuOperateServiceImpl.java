package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.scaffold.dbp.constants.DbType;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.rbac.common.exception.MenuOperateErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuOperateException;
import com.supcon.supfusion.rbac.dao.MenuOperateCodeUrlRefMapper;
import com.supcon.supfusion.rbac.dao.MenuoperateMapper;
import com.supcon.supfusion.rbac.dao.RolePermissionMapper;
import com.supcon.supfusion.rbac.dao.UserPermissionMapper;
import com.supcon.supfusion.rbac.dao.bo.MenuOperatePermissionBO;
import com.supcon.supfusion.rbac.dao.field.MenuInfoField;
import com.supcon.supfusion.rbac.dao.field.MenuOperateCodeUrlRefField;
import com.supcon.supfusion.rbac.dao.field.MenuOperateField;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import com.supcon.supfusion.rbac.dao.po.MenuOperatePO;
import com.supcon.supfusion.rbac.dao.po.RolePPositionPO;
import com.supcon.supfusion.rbac.dao.po.RolePStaffPO;
import com.supcon.supfusion.rbac.dao.po.RolePermissionPO;
import com.supcon.supfusion.rbac.dao.po.UserPPositionPO;
import com.supcon.supfusion.rbac.dao.po.UserPStaffPO;
import com.supcon.supfusion.rbac.dao.po.UserPermissionPO;
import com.supcon.supfusion.rbac.dao.query.MenuOperateQuery;
import com.supcon.supfusion.rbac.manager.II18nAdapter;
import com.supcon.supfusion.rbac.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * <p>
 * 操作表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
@Slf4j
@Service
@Transactional
public class MenuOperateServiceImpl extends ServiceImpl<MenuoperateMapper, MenuOperatePO> implements IMenuOperateService {

    @Autowired
    MenuoperateMapper menuoperateMapper;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    II18nAdapter i18nAdapterService;
    @Autowired
    private IUserPermissionService userPermissionService;
    @Autowired
    private IRolePermissionService rolePermissionService;
    @Autowired
    private MenuOperateCodeUrlRefMapper menuOperateCodeUrlRefMapper;
    @Autowired
    private UserPermissionMapper userPermissionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private IRolePPositionService rolePPositionService;
    @Autowired
    private IRolePStaffService rolePStaffService;
    @Autowired
    private IUserPPositionService userPPositionService;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;
    @Autowired
    private IUserPStaffService userPStaffService;
    @Autowired
    private IFlowPermissionService flowPermissionService;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;
    @Autowired
    private UserApiService userApiService;

    /**
     * @description:根据条件分页查询操作
     * @param: menuinfoId 菜单id
     * @param: keyword 通过关键字（编码、名称）模糊查询
     * @param: current  翻页页数
     * @param: pageSize 每页返回的元数量
     * @return: IPage<Map < String, Object>>
     * @author: fjh
     * @date: 2020/6/9
     */
    @Override
    public PageResult<MenuOperatePO> getMenuOperates(Long menuinfoId, String keyword, Integer current, Integer pageSize) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(menuinfoId)) {
            queryWrapper.eq("mo." + MenuOperateField.menuinfoId, menuinfoId);
        }
        if (!ObjectUtils.isEmpty(keyword)) {
            Map<String, String> map = i18nAdapterService.MessageResourceSearchOne(keyword);
            queryWrapper.and(queryWrapper1 -> {
                //%等 特殊字符转换
                if (DbType.ORACLE.equals(dataId.getDataId()) && !ObjectUtils.isEmpty(map)) {
                    Set<String> strings = map.keySet();
                    List<String> name = new ArrayList<>(strings);
                    int batch = name.size() / 1000;
                    if (batch == 0) {
                        queryWrapper1.in("mo.name", name);
                    } else {
                        for (int i = 0; i < batch; i++) {
                            queryWrapper1.or().in("mo.name", name.subList(i * 1000, i * 1000 + 1000));
                        }
                        if (name.size() % 1000 != 0) {
                            queryWrapper1.or().in("mo.name", name.subList(batch * 1000, name.size()));
                        }
                    }
                }else if(!ObjectUtils.isEmpty(map)){
                    queryWrapper1.in("mo.name", map.keySet());
                }
                queryWrapper1.or().like("mo." + MenuOperateField.code, keyword);
            });
        }
        queryWrapper.eq("mo.valid", 1);
        queryWrapper.eq("mi.valid",1);
        PageResult<MenuOperatePO> result = new PageResult<>();
        //如果有分页条件，则使用分页查询
        if (!ObjectUtils.isEmpty(pageSize)) {
            Page<MenuOperatePO> page = new Page<>(ObjectUtils.isEmpty(current) ? 1 : current, pageSize);
            menuoperateMapper.getMenuOperatePage(page, queryWrapper);
            if (!ObjectUtils.isEmpty(page)) {
                result.setPagination(new Pagination((int) page.getTotal(), (int) page.getSize(), (int) page.getCurrent()));
                result.setList(page.getRecords());
            }
        }
        return result;
    }

    /**
     * @description: 判断操作编码是否唯一
     * @param: MenuOperateCode 操作编码
     * @param: id
     * @return: boolean
     * @author: fjh
     * @date: 2020/6/9
     */
    public boolean checkCodeUnique(String MenuOperateCode, Long id) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        if (id == null) {
            queryWrapper.eq("code", MenuOperateCode);
        } else {
            queryWrapper.eq("code", MenuOperateCode);
            queryWrapper.ne("id", id);
        }
        Integer count = menuoperateMapper.selectCount(queryWrapper);
        return count == 0;
    }

    /**
     * @param menuOperate 操作对象
     * @return boolean
     * @description: 保存操作
     * @date: 2020/6/9
     */
    @Override
    public boolean save(MenuOperatePO menuOperate) {
        boolean isAdd = (null == menuOperate.getId());
        if (isAdd && !checkCodeUnique(menuOperate.getCode(), menuOperate.getId())) {
            throw new MenuOperateException(MenuOperateErrorEnum.UNIQUECODE);
        }
        if (null == menuOperate.getApp() && null != menuOperate.getMenuinfoId()) {
            MenuInfoPO miPO = menuInfoService.getById(menuOperate.getMenuinfoId());
            menuOperate.setApp(miPO.getApp());
            if (!ObjectUtils.isEmpty(menuOperate.getUrls())){
                menuOperate.getUrls().forEach(menuOperateCodeUrlRefPO -> menuOperateCodeUrlRefPO.setApp(miPO.getApp()));
                //清空操作URL
                menuOperateCodeUrlRefService.remove(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq(MenuOperateCodeUrlRefField.menuoperateCode,menuOperate.getCode()));
                //保存操作URL
                menuOperateCodeUrlRefService.saveOrUpdateBatch(menuOperate.getUrls());
                //redis新增URL
                menuOperateCodeUrlRefService.addRedisUrl(menuOperate.getUrls());
            }
        }
        if (isAdd) {
            menuoperateMapper.insert(menuOperate);
        } else {
            menuoperateMapper.update(menuOperate,new QueryWrapper<MenuOperatePO>().eq(MenuOperateField.code,menuOperate.getCode()));
        }
        return true;
    }

    /**
     * @description: 删除操作
     * @param: codes 操作编码','分割
     * @return: void
     * @author: fjh
     * @date: 2020/6/9
     */
    @Override
    public void deleteMenuOperates(String codes) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(codes)) {
            List<String> codesList = Arrays.asList(codes.split(","));
            codesList.forEach(code -> {
                if (!checkDeletePermit(code)) {
                    throw new MenuOperateException(MenuOperateErrorEnum.OPERATE_HAS_GRANT_PERMISSION);
                }
            });
            queryWrapper.in("code", codesList);
            List<MenuOperatePO> menuOperatePOS = this.list(queryWrapper);
            if (ObjectUtils.isEmpty(menuOperatePOS)){
                return;
            }
            List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.id, menuOperatePOS.stream().map(MenuOperatePO::getMenuinfoId).collect(Collectors.toList())));
            List<String> apps = menuInfoPOS.stream().map(MenuInfoPO::getApp).distinct().collect(Collectors.toList());
            //modify by yy 2020/7/2 只删除非默认操作
            queryWrapper.eq("DEFAULT_OPERATE", 0);
            menuoperateMapper.delete(queryWrapper);
            //清空操作URL
            menuOperateCodeUrlRefService.remove(new QueryWrapper<MenuOperateCodeUrlRefPO>().in(MenuOperateCodeUrlRefField.menuoperateCode,codesList));
            userUrlRefService.refreshRedis(apps);
        }
    }



    @Override
    public List<MenuOperatePermissionBO> getAssignMenuOperateUserFromRole(Long menuId, Long userId) {
        MenuOperateQuery childMoq = new MenuOperateQuery();
        if (!ObjectUtils.isEmpty(menuId)){
            MenuInfoPO m = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().select("LAY_REC").eq("ID", menuId));
            //查询该菜单的所有子菜单
            List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().select("ID").likeRight("LAY_REC", m.getLayRec()));
            List<Long> menuIds = menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
            childMoq.setMenuInfoIds(menuIds);
        }
        childMoq.setUserId(userId);
        childMoq.setCid(UserContext.getUserContext().getCompanyId());
        return menuoperateMapper.getAssignedPermissionUser(childMoq);
    }

    /**
     * @description: 查询已分配权限的操作(用户)
     * @param: menuId
     * @param: roleId
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.RolePermissionPO>
     * @author: 袁阳
     * @date: 2020/6/16
     */
    @Override
    public List<MenuOperatePermissionBO> getAssignMenuOperateUser(Long menuId) {
        MenuOperateQuery childMoq = new MenuOperateQuery();
        List<Long> menuIds = new ArrayList<>();
        if (!ObjectUtils.isEmpty(menuId)){
            MenuInfoPO m = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().select("LAY_REC").eq("ID", menuId));
            //查询该菜单的所有子菜单
            List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().select("ID").likeRight("LAY_REC", m.getLayRec()));
            menuIds = menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        }
        childMoq.setCid(UserContext.getUserContext().getCompanyId());
        List<MenuOperatePermissionBO> result = new ArrayList<>();
        if (!ObjectUtils.isEmpty(menuIds)){
            int batch = menuIds.size() / 1000;
            if (batch == 0) {
                childMoq.setMenuInfoIds(menuIds);
                result = menuoperateMapper.getAssignedPermissionUser(childMoq);
            } else {
                for (int i = 0; i < batch; i++) {
                    childMoq.setMenuInfoIds(menuIds.subList(i * 1000, i * 1000 + 1000));
                    result.addAll(menuoperateMapper.getAssignedPermissionUser(childMoq));
                }
                if (menuIds.size() % 1000 != 0) {
                    childMoq.setMenuInfoIds(menuIds.subList(batch * 1000, menuIds.size()));
                    result.addAll(menuoperateMapper.getAssignedPermissionUser(childMoq));
                }
            }
        } else{
            result = menuoperateMapper.getAssignedPermissionUser(childMoq);
        }
        result = distinctOperate(result);
        return result;
    }

    private List<MenuOperatePermissionBO> distinctOperate(List<MenuOperatePermissionBO> menuOperatePermissionBO){
        return menuOperatePermissionBO.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(()
                -> new TreeSet<>(Comparator.comparing(MenuOperatePermissionBO::getCode))), ArrayList::new));
    }

    /**
     * @description: 查询指定用户 没有的权限的操作
     * @param: menuOperateCodes
     * @param: userId
     * @return: java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @author: 袁阳
     * @date: 2020/7/13
     */
    @Override
    public List<MenuOperatePO> getMenuOperateWithoutRolePermission(List<String> menuOperateCodes, Long roleId) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(menuOperateCodes)) {
            queryWrapper.in("mo.CODE", menuOperateCodes);
        }else {
        	return new ArrayList<MenuOperatePO>();
        }
        if (!ObjectUtils.isEmpty(roleId)) {
            queryWrapper.apply("NOT EXISTS (select 1 from rbac_rolepermission rr where rr.MENUOPERATE_ID = mo.ID and rr.ROLE_ID = {0})", roleId);
        }
        return menuoperateMapper.getMenuOperateWithoutRolePermission(queryWrapper);
    }

    /**
     * @description: 获取单个操作
     * @param: code
     * @return: com.supcon.supfusion.rbac.dao.po.MenuOperatePO
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public MenuOperatePO getOne(String code) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        queryWrapper.eq("valid", 1);
        List<MenuOperatePO> menuOperatePO = menuoperateMapper.selectList(queryWrapper);
        if (!ObjectUtils.isEmpty(menuOperatePO)) {
            return menuOperatePO.get(0);
        }
        return null;
    }

    /**
     * @description: 根据编码或菜单ID查询菜单下的所有操作
     * @param: code
     * @param: id
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuOperatePO>
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public List<MenuOperatePO> findMenuOperateByMenuInfo(String code, Long id) {

        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(id)) {
            queryWrapper.eq("MENUINFO_ID", id);
        }
        if (!ObjectUtils.isEmpty(code)) {
            queryWrapper.eq("CODE", code);
        }
        return menuoperateMapper.selectList(queryWrapper);
    }

    @Override
    public List<MenuOperatePO> findMenuOperateByMenuCodeAndCids(String code, List<Long> cids, Long menuInfoId) {
        QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(code)) {
            queryWrapper.eq("mi.CODE", code);
        }
        if (!ObjectUtils.isEmpty(cids)) {
            queryWrapper.in("mo.cid", cids);
        }
        if (!ObjectUtils.isEmpty(menuInfoId)) {
            queryWrapper.eq("mo.MENUINFO_ID", menuInfoId);
        }
        queryWrapper.eq("mo.valid", 1);
        return menuoperateMapper.findMenuOperateByMenuInfo(queryWrapper);
    }

    @Override
    public void deleteByCodePhysics(List<String> codes) {
        if (!ObjectUtils.isEmpty(codes)) {
            QueryWrapper<MenuOperatePO> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("CODE", codes);
            queryWrapper.select("ID");
            List<MenuOperatePO> menuOperatePOS = menuoperateMapper.selectList(queryWrapper);
            List<Long> menuOperateIds = menuOperatePOS.stream().map(MenuOperatePO::getId).collect(Collectors.toList());
            menuoperateMapper.deleteByCodePhysics(queryWrapper);
            rolePermissionService.remove(new QueryWrapper<RolePermissionPO>().in("MENUOPERATE_ID", menuOperateIds));
            userPermissionService.remove(new QueryWrapper<UserPermissionPO>().in("MENUOPERATE_ID", menuOperateIds));
        }
    }

    @Override
    public void cascadeDeleteMenuOperate(List<MenuOperatePO> menuOperatePOList) {
        if (!ObjectUtils.isEmpty(menuOperatePOList)){
            List<Long> operateIdList = new ArrayList<>();
            List<String> operateCodeList = new ArrayList<>();
            for (MenuOperatePO menuOperatePO : menuOperatePOList) {
                operateIdList.add(menuOperatePO.getId());
                operateCodeList.add(menuOperatePO.getCode());
            }
            // 删除菜单操作数据
            removeByIds(menuOperatePOList.stream().map(MenuOperatePO::getId).collect(Collectors.toList()));
            if (!ObjectUtils.isEmpty(operateIdList)){
                QueryWrapper<MenuOperateCodeUrlRefPO> queryOperateUrl = new QueryWrapper<>();
                queryOperateUrl.in("MENUOPERATE_CODE", operateCodeList);
                // 删除菜单操作和URL关联表数据
                menuOperateCodeUrlRefMapper.delete(queryOperateUrl);
                // 删除用户和角色权限数据
                QueryWrapper<UserPermissionPO> queryOperateIdUser= new QueryWrapper<>();
                QueryWrapper<RolePermissionPO> queryOperateIdRole= new QueryWrapper<>();
                queryOperateIdUser.in("MENUOPERATE_ID", operateIdList);
                queryOperateIdRole.in("MENUOPERATE_ID", operateIdList);
                List<UserPermissionPO> listUserPermission = userPermissionMapper.selectList(queryOperateIdUser);
                List<RolePermissionPO> listRolePermission = rolePermissionMapper.selectList(queryOperateIdRole);
                //删除角色权限岗位,人员关联
                if (!ObjectUtils.isEmpty(listRolePermission)){
                    rolePPositionService.remove(new QueryWrapper<RolePPositionPO>().eq("ROLEPERMISSION_ID",listRolePermission.stream().map(RolePermissionPO::getId).collect(Collectors.toList())));
                    rolePStaffService.remove(new QueryWrapper<RolePStaffPO>().eq("ROLEPERMISSION_ID",listRolePermission.stream().map(RolePermissionPO::getId).collect(Collectors.toList())));
                }
                //删除用户权限岗位,人员关联
                if (!ObjectUtils.isEmpty(listUserPermission)){
                    userPPositionService.remove(new QueryWrapper<UserPPositionPO>().eq("USERPERMISSION_ID",listUserPermission.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
                    userPStaffService.remove(new QueryWrapper<UserPStaffPO>().eq("USERPERMISSION_ID",listUserPermission.stream().map(UserPermissionPO::getId).collect(Collectors.toList())));
                }
                userPermissionMapper.delete(queryOperateIdUser);
                rolePermissionMapper.delete(queryOperateIdRole);
            }
        }
    }

    @Override
    public List<MenuOperatePO> getByCode(String code, Long cid) {
        return getByCodeFromJpa(code, cid);
    }

    @Override
    public List<MenuOperatePO> getOperateListByUserIdMenuId(Long userId, String menuCode) {
        Long cid = UserContext.getUserContext().getCompanyId();
        log.info("getCurrentCid========{}",cid);
        if (null == cid) {
            Result<UserOrgDetailDTO> userOrgDetailDTO = userApiService.getUsersDetailById(userId);
            cid = userOrgDetailDTO.getData().getCompanyId();
        }
        List<MenuOperatePO> operateListByUserIdMenuId = menuoperateMapper.getOperateListByUserIdMenuId(userId, menuCode ,cid);
        return operateListByUserIdMenuId;
    }

    private List<MenuOperatePO> getByCodeFromJpa(String code, Long cid) {
        List<MenuOperatePO> op = null;
        try {
            if (cid != null ) {
                op = this.list(new QueryWrapper<MenuOperatePO>().eq("code",code).eq("cid",cid));

            }
            if (op == null || op.isEmpty()) {
                op = this.list(new QueryWrapper<MenuOperatePO>().eq("code",code));
            }
        } catch (Exception e) {
            log.debug("code Already exists or repeat multiple !");
            log.error(e.getMessage(), e);
        }
        return op;
    }

    private boolean checkDeletePermit(String code) {
        List<MenuOperatePO> menuOperatePO = menuoperateMapper.getOne(new QueryWrapper<MenuOperatePO>().eq("CODE", code).eq("VALID", 1));
        if (!ObjectUtils.isEmpty(menuOperatePO)) {
            QueryWrapper<UserPermissionPO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("MENUOPERATE_ID", menuOperatePO.get(0).getId());
            List<UserPermissionPO> list = userPermissionService.list(queryWrapper);
            return ObjectUtils.isEmpty(list);
        }
        return true;
    }
}
