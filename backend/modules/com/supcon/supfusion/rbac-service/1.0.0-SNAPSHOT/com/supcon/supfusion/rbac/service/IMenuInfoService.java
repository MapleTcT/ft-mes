package com.supcon.supfusion.rbac.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.rbac.api.dto.MenuInfoJsonDTO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.MenuSuposPO;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 菜单表 服务类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-05
 */
public interface IMenuInfoService extends IService<MenuInfoPO> {

    Boolean saveMenuInfo(MenuInfoPO menuInfoPO);

    Boolean saveBatchMenuInfo(List<MenuInfoPO> menuInfoPOS);

    Boolean updateMenuInfo(MenuInfoPO menuInfoPO, UpdateWrapper<MenuInfoPO> updateWrapper);

    Boolean updateBatchMenuInfoById(List<MenuInfoPO> menuInfoPOS);

    Boolean removeMenuInfoByIds(List<Long> ids);

    Boolean removeMenuInfo(QueryWrapper<MenuInfoPO> queryWrapper);

    /**
     * 查询菜单树形结构数据
     * @return
     */
    MenuInfoPO queryMenuTree();

    /**
     * 查询菜单结构数据
     * @return
     */
    List<MenuInfoPO> queryMenus(boolean enableStatus,boolean restrict);

    /**
     * 查询菜单结构数据
     * @return
     */
    List<MenuInfoPO> queryMenusRef(boolean enableStatus,boolean restrict);

    /**
     * 查询菜单配置数据
     * @return
     */
    List<MenuInfoPO> queryMenuConfigure(boolean enableStatus,boolean restrict);

    /**
     * 模糊查询菜单和指定ID查询
     * @param keyword
     * @param id
     * @param enable
     * @return
     */
    List<MenuInfoPO> queryMenuList(String keyword,Long id,String enable);

    /**
     * 模糊查询菜单和指定ID查询
     * @param keyword
     * @param id
     * @param enable
     * @return
     */
    List<MenuInfoPO> queryMenuConfigureList(String keyword,Long id,String enable);

    /**
     * 校验菜单是否存在
     * @param code
     * @return
     */
    boolean validateMenuExist(String code);

    /**
     * 通过编码查询指定菜单
     * @param code
     * @return
     */
    MenuInfoPO queryMenuByCode(String code);

    /**
     * 创建菜单
     * @param menuInfoPO
     */
    void addMenu(MenuInfoPO menuInfoPO);

    /**
     * 修改菜单
     * @param menuInfoPO
     */
    void updateMenu(MenuInfoPO menuInfoPO);

    /**
     * 批量删除菜单
     * @param list
     */
    void batchDeleteMenus(List<String> list);

    /**
     * 修改菜单顺序
     * @param jsonObject
     */
    void modifyMenuSort(JSONObject jsonObject);

    /**
     * @description: 查询登陆用户权限菜单
     * @param: userId
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/6/18
     */
    List<MenuInfoPO> findUserMenu(Long userId,Integer status,Boolean enable);
    List<MenuInfoPO> findUserMenuFlat(Long userId,Integer status,Boolean enable);
    List<MenuSuposPO> findUserSuposMenuFlat(Long userId,Integer status,Boolean enable);

    void saveBachUrlByJson(String json);

    String saveBachUrl(List<MenuInfoJsonDTO> menuInfoJsonDTO, boolean needBak);

    void setFullPath(MenuInfoPO menuInfoPO, Map<Long, MenuInfoPO> menuMapById);

    /**
     * @description: 外部APP调用接口导入菜单、操作、url对应数据
     * @param: xml
     * @return: void
     * @author: fjh
     * @date: 2020/7/8
     */
    void saveBachUrlByXml(String xml,Long cid)throws Exception;

    void modifyMenuEnableStatus(MenuInfoPO menuInfo);

    String filterPathParams(String url);

    void recoverMenuInfo(String code);

    String getModuleName(String moduleId);

    Collection<ModuleDTO> queryModules();

    void updateMenuInfoByEntityCode(String entityCode);

    JSONObject checkMenu(String url);

    /**
     * 修改菜单
     * @param menuInfoPO
     */
    void updateMenuInfo(MenuInfoPO menuInfoPO);

    /**
     * 根据code批量删除菜单
     * @param idList
     */
    void batchDeleteMenusAll(List<Long> idList);

    void deleteMenuRelevance(List<MenuInfoPO> menuInfoPOList);

    void deleteMenuRelevanceByMenuIdList(List<Long> menuIdList);
    void deleteMenuRelevanceByMenuIdList(Set<Long> menuIdList);
    /**
     * 根据APP批量删除菜单
     * @param appIdList
     */
    void batchDeleteMenusByApp(List<String> appIdList, String source);

    void deleteMenuInfoByCode(String code);

    void generateDefaultOperate(List<Long> menuInfoIds);

    /**
     * 查询菜单结构数据
     * @return
     */
    List<MenuInfoPO> queryRuntimeMenus(String appId);

    Page<MenuInfoPO> findMenusByKeyword(String keyword, Integer size, String restrict, String enable);

    Page<MenuInfoPO> findMenuConfigureByKeyword(String keyword, Integer size, String restrict, String enable);

    void cascadeDeleteMenuById(List<MenuInfoPO> menuInfoPOList);

    void deletePhysics(List<Long> ids);

    /**
     * @description: 查询登陆用户权限菜单 列表结构
     * @param: userId
     * @return: java.util.List<com.supcon.supfusion.rbac.dao.po.MenuInfoPO>
     * @author: 袁阳
     * @date: 2020/6/18
     */
    List<MenuInfoPO> findUserMenuList(Long userId);

    void deleteByCode(String code,boolean forceDeleteMenuOperate);

//    void deleteMenuInfoByCodeList(String rootCode, List<String> codeList);

    void getAddMenuIds(List<Long> originalMenuIdList, List<Long> menuIdPOList, Map<Long, MenuInfoPO> menuObjectHashMap, HashSet<Long> summaryIds);

    void getReduceMenuIds(List<Long> originalMenuIdList, List<Long> menuIdPOList);

    /**
     *  根据appid 查询所有菜单
     */
    List<Long> getMenusByAppIds(Set<String> appIds);

    /**
     * 获得用户菜单权限
     *
     * @param userId
     * @param status
     * @return
     */
    List<MenuInfoPO> findUserPermissionMenus(Long userId,Integer status);

    /**
     * 查询用户菜单权限（平铺）
     *
     * @param userId
     * @param status
     * @return
     */
    List<MenuInfoPO> findUserPermissionMenusFlat(Long userId,Integer status);

    /**
     * 查询用户菜单权限（树）
     *
     * @param userId
     * @param status
     * @return
     */
    List<MenuSuposPO> findUserPermissionMenusTree(Long userId,Integer status);

    /**
     * 判断是否是系统管理员
     * 即是否拥有系统管理菜单的权限
     *
     * @param userId
     * @return
     */
    boolean isAdmin(long userId);
}
