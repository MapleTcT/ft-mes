package com.supcon.supfusion.base.services.impl;

import com.alibaba.nacos.api.config.filter.IFilterConfig;
import com.supcon.supfusion.base.dao.MenuInfoCompanyRefPODaoImpl;
import com.supcon.supfusion.base.dao.MenuInfoDaoImpl;
import com.supcon.supfusion.base.dao.MenuInfoPODaoImpl;
import com.supcon.supfusion.base.dao.MenuOperatePODaoImpl;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.CustomMenuInfoMneService;
import com.supcon.supfusion.base.services.CustomMenuInfoService;
import com.supcon.supfusion.base.services.CustomMenuOperateService;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import com.supcon.supfusion.rbac.api.dto.UpdateMenuInfoIdDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service("customMenuInfoService")
@Transactional
public class CustomMenuInfoServiceImpl implements CustomMenuInfoService {

    @Autowired
    private IMenuInfoApiService supfusionMenuInfoService;
    @Autowired
    private MenuInfoPODaoImpl menuInfoPODao;
    @Autowired
    private MenuInfoDaoImpl menuInfoDao;
    @Autowired
    private MenuInfoCompanyRefPODaoImpl menuInfoCompanyRefPODao;
    @Autowired
    private MenuOperatePODaoImpl menuOperatePODao;
    @Autowired
    private CustomMenuOperateService customMenuOperateService;
    @Autowired
    private CustomMenuInfoMneService customMenuInfoMneService;
    @Autowired
    private InternationalService internationalService;
    @Value("${integration.supos.enabled:false}")
    private Boolean supos;
    @Override
    public void deleteByIds(List<Long> ids) {
//        supfusionMenuInfoService.deleteMenuInfoByIds(ids);
        for (Long id : ids) {
            menuInfoPODao.delete(id);
        }
    }

    @Override
    public void deleteByEntityCode(String entityCode) {
      /*  //根据entityCode删除
        supfusionMenuInfoService.deleteMenuInfoByEntityCode(entityCode);*/

        menuInfoPODao.createQuery("delete MenuInfoPO mi where mi.entityCode=?", entityCode).executeUpdate();
    }

    @Override
    public void updateByEntityCode(String entityCode) {
//        supfusionMenuInfoService.updateMenuInfoByEntityCode(entityCode);

        String updateParentHql = "update MenuInfoPO set parentId=null where entityCode=?";
        menuInfoPODao.createQuery(updateParentHql, entityCode).executeUpdate();
    }

    @Override
    public void updateMenuInfoById(UpdateMenuInfoIdDTO updateMenuInfoIdDTO) {
//        supfusionMenuInfoService.updateMenuInfoById(updateMenuInfoIdDTO);
    }

    @Override
    public void save(MenuInfo menuInfo) {
        //保存menuInfo
        //MenuInfoDTO menuInfoDTO = new MenuInfoDTO();
        //BeanUtils.copyProperties(menuInfo, menuInfoDTO);
        //MenuInfoDTO result = supfusionMenuInfoService.save(menuInfoDTO);


        /*JSONSerializer serializer = new JSONSerializer();
        String json = serializer.serialize(menuInfo);
        MenuInfoPO menuInfoPO = new JSONDeserializer<MenuInfoPO>().deserialize(json, MenuInfoPO.class);
*/

        MenuInfoPO menuInfoPO=null;
        if ("menu_list".equals(menuInfo.getCode())){
            return;
        }
        if(null==menuInfo.getId()){
            //long id = idGenerator.getNextId("rbac_menuinfo", "SEQ_ID");
            menuInfoPO = new MenuInfoPO();
            menuInfoPO.setId(IDGenerator.newInstance().generate().longValue());
            menuInfoPO.setVersion(0);
            menuInfoPO.setValid(true);
            menuInfoPO.setStatus(menuInfo.getStatus());
            menuInfoPO.setCssClass(menuInfo.getCssClass());
            menuInfo.setId(menuInfoPO.getId());
            //menuInfoPO.setId(id);
        }else{
            menuInfoPO =menuInfoPODao.load(menuInfo.getId());
        }
        List<MenuInfoPO> dbMenu = menuInfoPODao.findByCriteria(Restrictions.eq("code", menuInfo.getCode()),Restrictions.eq("valid", true));
        if (!ObjectUtils.isEmpty(dbMenu)){
            menuInfoPO = dbMenu.get(0);
        }
        if ((!ObjectUtils.isEmpty(menuInfoPO.getEdited())) && menuInfoPO.getEdited()){
            return;
        }
        menuInfoPO.setValid((!ObjectUtils.isEmpty(menuInfo.getValid()) && menuInfo.getValid()) ? true :false);
        menuInfoPO.setActionUrl(menuInfo.getAction());
//        menuInfoPO.setApp(menuInfo.getapp);
        if (null != menuInfo.getCompany()) {
            menuInfoPO.setCid(menuInfo.getCompany().getId());
        }
        menuInfoPO.setCode(menuInfo.getCode());
//        menuInfoPO.setEdited(menuInfo.geted);
        menuInfoPO.setEnable(true);
        menuInfoPO.setEntityCode(menuInfo.getEntityCode());
        menuInfoPO.setFullPath(menuInfo.getFullPathName());
        /**
         * todo
         */
        menuInfoPO.setType(0);
        menuInfoPO.setIsHide(menuInfo.getIsHide());  
        menuInfoPO.setLayNo(menuInfo.getLayNo());
        menuInfoPO.setLeaf(menuInfo.getLeaf());
        menuInfoPO.setMemo(menuInfo.getMemo());
        menuInfoPO.setMenuType(ObjectUtils.isEmpty(menuInfo.getUrl()) ? 1 : 2);
        if (ObjectUtils.isEmpty(menuInfoPO.getModuleCode())){
            menuInfoPO.setModuleCode(menuInfo.getModuleCode());
        }
        //设置APP字段
        if (ObjectUtils.isEmpty(menuInfoPO.getApp()) && !ObjectUtils.isEmpty(menuInfoPO.getModuleCode())){
            menuInfoPO.setApp(menuInfoPO.getModuleCode().split("_")[0]);
        }
        menuInfoPO.setName(menuInfo.getName());
        List<MenuInfoPO> parents = menuInfoPODao.findByCriteria(Restrictions.eq("code", menuInfo.getParentCode()));
        MenuInfoPO parentMenuInfo;
        if (!ObjectUtils.isEmpty(parents)){
            parentMenuInfo = parents.get(0);
            if (parentMenuInfo.getId() == -1L){
                menuInfoPO.setLayRec(menuInfoPO.getId() + "");
                menuInfoPO.setFullPath(menuInfoPO.getId() + "");
                menuInfoPO.setFullPathName(menuInfoPO.getName() + "");
            }else{
                menuInfoPO.setLayRec(parentMenuInfo.getLayRec() + "-" + menuInfoPO.getId());
                menuInfoPO.setFullPath(parentMenuInfo.getFullPath() + "/" + menuInfoPO.getId());
                menuInfoPO.setFullPathName(parentMenuInfo.getFullPathName() + "/" + menuInfoPO.getName());
            }
        }else{
            menuInfoPO.setLayRec(menuInfoPO.getId() + "");
            menuInfoPO.setFullPath(menuInfoPO.getId() + "");
            menuInfoPO.setFullPathName(menuInfoPO.getName() + "");
        }
//        //layNo以layRec为主
//        menuInfoPO.setLayNo(menuInfoPO.getLayRec().split("-").length);
//        menuInfoPO.setNameDisplay(menuInfo.getname);
        menuInfoPO.setNamespace(menuInfo.getNamespace());
//        menuInfoPO.setNoRestrict(menuInfo.getres);
        if(null!=menuInfo.getParentId()){
        	menuInfoPO.setParentId(menuInfo.getParentId());
        }else{
        	menuInfoPO.setParentId(-1l);
        }
        /**
         * todo
         */

        menuInfoPO.setShowType(1);
        menuInfoPO.setSort(menuInfo.getSort());
        if (ObjectUtils.isEmpty(menuInfoPO.getSort()) || menuInfoPO.getSort() == 0){
            String hql = "select max(sort) from MenuInfoPO where PARENT_ID = ? and valid = 1";
            List<Object> max_sort = menuInfoPODao.findByHql(hql, menuInfoPO.getParentId());
            if (!ObjectUtils.isEmpty(max_sort) && max_sort.size() > 0){
                Double max_sort_index = 0D;
                if (!ObjectUtils.isEmpty(max_sort.get(0))){
                    max_sort_index = (Double) max_sort.get(0);
                }
                menuInfoPO.setSort(max_sort_index + 1D);
            }
        }else{
            menuInfoPO.setSort(menuInfoPO.getSort());
        }
        menuInfoPO.setSystemDefault(true);
        if (null != menuInfo.getTarget() && !"".equals(menuInfo.getTarget().toString())) {
            menuInfoPO.setTarget(menuInfo.getTarget().toString());
        }
        menuInfoPO.setUrl(menuInfo.getUrl());
        if (menuInfo.getUrl() != null) {
            menuInfoPO.setRoute(menuInfo.getUrl().replace("?", "/").replace("=", "/").replace("&", "/"));
        }
        menuInfoPO.setCid(menuInfo.getCid());
        if (null != menuInfo.getId()) {
            menuInfoPODao.merge(menuInfoPO);
        } else {
            menuInfoPODao.save(menuInfoPO);
        }
        menuInfo.setId(menuInfoPO.getId());
        menuInfo.setSort(menuInfoPO.getSort());
        //保存助记码
        customMenuInfoMneService.save(Collections.singletonList(menuInfoPO));
        //如果菜单没有适用范围，则适用范围设置为所有公司
        List<MenuInfoCompanyRefPO> menuInfoCompanyRefList=menuInfoCompanyRefPODao.findByCriteria(Restrictions.eq("menuinfoId", menuInfoPO.getId()));
        if(null==menuInfoCompanyRefList||menuInfoCompanyRefList.isEmpty()){
            MenuInfoCompanyRefPO companyRefPO=new MenuInfoCompanyRefPO();
            companyRefPO.setCompanyId(-1L);
            companyRefPO.setMenuinfoId(menuInfoPO.getId());
            companyRefPO.setId(IDGenerator.newInstance().generate().longValue());
            if (!supos) {
                companyRefPO.setAppId(menuInfoPO.getApp());
            }
            menuInfoCompanyRefPODao.save(companyRefPO);
        }
    }

    @Override
    public String deleteMenuInfoByApps(String app) {
        List<MenuInfoPO> menuInfoPOS = menuInfoPODao.findByCriteria(Restrictions.eq("app", app),Restrictions.eq("valid",true));
        if (ObjectUtils.isEmpty(menuInfoPOS)){
            return null;
        }
        List<Long> menuInfoIds = menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
        //检测菜单下面是否挂载了其他模块的菜单
        List<MenuInfoPO> other_module_menus = menuInfoPODao.findByCriteria(Restrictions.in("parentId", menuInfoIds), Restrictions.ne("app", app),Restrictions.eq("valid",true));
        if (!ObjectUtils.isEmpty(other_module_menus)){
            return internationalService.getI18nValue("ec.exceptions.delete.menu.faild");
        }
        deleteByIds(menuInfoIds);
        String deleteMenuInfoCompanySql = "delete from rbac_menuinfo_company_ref where MENUINFO_ID in (:ids)";
        menuInfoPODao.createNativeQuery(deleteMenuInfoCompanySql).setParameterList("ids",menuInfoIds).executeUpdate();
        List<MenuOperatePO> menuOperatePOS = menuOperatePODao.findByCriteria(Restrictions.in("menuinfoId", menuInfoIds));
        if (ObjectUtils.isEmpty(menuOperatePOS)){
            return null;
        }
        customMenuOperateService.deleteByMenuInfoIds(menuInfoIds);
        List<Long> menuOperateIds = menuOperatePOS.stream().map(MenuOperatePO::getId).collect(Collectors.toList());
        List<String> menuOperateCodes = menuOperatePOS.stream().map(MenuOperatePO::getCode).collect(Collectors.toList());
        String deleteOperateUrlSql = "delete from rbac_menuoperatecode_url_ref where MENUOPERATE_CODE in (:codes)";
        menuOperatePODao.createNativeQuery(deleteOperateUrlSql).setParameterList("codes",menuOperateCodes).executeUpdate();
        String permissionSql = "select id from rbac_userpermission where MENUOPERATE_ID in (:ids)";
        List<Long> userPermissionIds = menuOperatePODao.createNativeQuery(permissionSql).setParameterList("ids", menuOperateIds).list();

        int segments = userPermissionIds.size() / 1000;// 商
        segments = userPermissionIds.size() % 1000 == 0 ? segments : segments + 1; // 段数
        for (int i = 0; i < segments; i++) {
            List<Long> cutList = null;
            if (i == segments - 1) {
                cutList = userPermissionIds;
            }else{
                cutList = userPermissionIds.subList(0, 1000);
                userPermissionIds=userPermissionIds.subList(1000, userPermissionIds.size());
            }
            String deleteUserPermissionSql = "delete from rbac_userpermission where id in (:ids)";
            String deleteUserPermissionPosSql = "delete from rbac_userpposition where USERPERMISSION_ID in (:ids)";
            String deleteUserPermissionStaffSql = "delete from rbac_userpstaff where USERPERMISSION_ID in (:ids)";
            String deleteUserPermissionCusSql = "delete from rbac_user_custompermission_ref where USERPERMISSION_ID in (:ids)";
            String deleteUserPermissionDataSql = "delete from rbac_user_datapermission where USERPERMISSION_ID in (:ids)";
            menuOperatePODao.createNativeQuery(deleteUserPermissionSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteUserPermissionPosSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteUserPermissionStaffSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteUserPermissionCusSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteUserPermissionDataSql).setParameterList("ids",cutList).executeUpdate();
        }
        String rolePermissionSql = "select id from RBAC_ROLEPERMISSION where MENUOPERATE_ID in (:ids)";
        List<Long> rolePermissionIds = menuOperatePODao.createNativeQuery(rolePermissionSql).setParameterList("ids", menuOperateIds).list();
        segments = rolePermissionIds.size() / 1000;// 商
        segments = rolePermissionIds.size() % 1000 == 0 ? segments : segments + 1; // 段数
        for (int i = 0; i < segments; i++) {
            List<Long> cutList = null;
            if (i == segments - 1) {
                cutList = rolePermissionIds;
            }else{
                cutList = rolePermissionIds.subList(0, 1000);
                rolePermissionIds=rolePermissionIds.subList(1000, rolePermissionIds.size());
            }
            String deleteRolePermissionSql = "delete from rbac_rolepermission where id in (:ids)";
            String deleteRolePermissionPosSql = "delete from rbac_rolepposition where ROLEPERMISSION_ID in (:ids)";
            String deleteRolePermissionStaffSql = "delete from rbac_rolepstaff where ROLEPERMISSION_ID in (:ids)";
            String deleteRolePermissionCusSql = "delete from rbac_role_custompermission_ref where ROLEPERMISSION_ID in (:ids)";
            String deleteRolePermissionDataSql = "delete from rbac_role_datapermission where ROLEPERMISSION_ID in (:ids)";
            menuOperatePODao.createNativeQuery(deleteRolePermissionSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteRolePermissionPosSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteRolePermissionStaffSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteRolePermissionCusSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteRolePermissionDataSql).setParameterList("ids",cutList).executeUpdate();
        }
        //删除工作流权限
        String flowPermissionSql = "select id from RBAC_FLOW_PERMISSION where ACTIVITY_CODE in (:codes)";
        List<Long> flowPermissionIds = menuOperatePODao.createNativeQuery(flowPermissionSql).setParameterList("codes", menuOperateCodes).list();
        segments = flowPermissionIds.size() / 1000;// 商
        segments = flowPermissionIds.size() % 1000 == 0 ? segments : segments + 1; // 段数
        for (int i = 0; i < segments; i++) {
            List<Long> cutList = null;
            if (i == segments - 1) {
                cutList = flowPermissionIds;
            }else{
                cutList = flowPermissionIds.subList(0, 1000);
                flowPermissionIds=flowPermissionIds.subList(1000, flowPermissionIds.size());
            }
            String deleteFlowPermissionSql = "delete from rbac_flow_permission where id in (:ids)";
            String deleteFlowPermissionPosSql = "delete from rbac_flow_permission_position where FLOWPERMISSION_ID in (:ids)";
            String deleteFlowPermissionStaffSql = "delete from rbac_flow_permission_staff where FLOWPERMISSION_ID in (:ids)";
            menuOperatePODao.createNativeQuery(deleteFlowPermissionSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteFlowPermissionPosSql).setParameterList("ids",cutList).executeUpdate();
            menuOperatePODao.createNativeQuery(deleteFlowPermissionStaffSql).setParameterList("ids",cutList).executeUpdate();
        }
        return null;
    }

   /* @Override
    public List<MenuInfo> getByAsc() {
//        Map<String, String> map = BAPHttpUtils.executeRequest(MenuInfoUrl + "/getMenuInfoAsc", HttpGet.METHOD_NAME);
//        return mapToList(map);
        return null;
    }

    public List<MenuInfo> mapToList(Map<String, String> map) {
        return new ArrayList<MenuInfo>();
    }

    public MenuInfo mapToMenuInfo(Map<String, String> map) {
        return new MenuInfo();
    }

    @Override
    public List<MenuInfo> getByLikeModuleCode(String moduleCode) {
//        Map<String, String> map = BAPHttpUtils.executeRequest(MenuInfoUrl + "/getMenuInfoByModuleCode" + "/getMenuInfoAsc", HttpGet.METHOD_NAME);
//        return mapToList(map);
        return null;
    }

    @Override
    public MenuInfo getMenuInfoByCode(String code) {
//        Map<String, String> map = BAPHttpUtils.executeRequest(MenuInfoUrl + "/getMenuInfoByModuleCode" + "/getMenuInfoAsc", HttpGet.METHOD_NAME);
//        return mapToMenuInfo(map);
        return null;

     *//*   MenuInfoDTO menuInfoDTO = supfusionMenuInfoService.getMenuInfoByCode(code);
        if (null != menuInfoDTO) {
            return BeanUtil.copy(menuInfoDTO, MenuInfo.class);
        }
        return null;*//*
    }

    @Override
    public MenuInfo getMenuInfoById(Long id) {
//        Map<String, String> map = BAPHttpUtils.executeRequest(MenuInfoUrl + "/getMenuInfoById", HttpGet.METHOD_NAME);
//        return mapToMenuInfo(map);
        return null;

        *//*MenuInfoDTO menuInfoDTO = supfusionMenuInfoService.getMenuInfoById(id);
        if (null != menuInfoDTO) {
            return BeanUtil.copy(menuInfoDTO, MenuInfo.class);
        }
        return new MenuInfo();*//*
    }

    @Override
    public List<MenuInfo> getByEntityCode(String entityCode) {

//        Map<String, String> map = BAPHttpUtils.executeRequest(MenuInfoUrl + "/getMenuInfoByEntityCode", HttpGet.METHOD_NAME);
//        return mapToList(map);
        return null;
      *//*  List<MenuInfoDTO> menuInfoDTOList = supfusionMenuInfoService.getMenuInfoByEntityCode(entityCode);
        return DtoToEntity(menuInfoDTOList);*//*
    }


    private List<MenuInfo> DtoToEntity(List<MenuInfoDTO> menuInfoDTOList) {
        if (!CollectionUtils.isEmpty(menuInfoDTOList)) {
            return menuInfoDTOList.stream().map(a -> BeanUtil.copy(a, MenuInfo.class)).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }


    @Override
    public MenuInfo load(Long id) {
//        return menuInfoDao.get(id);
        //根据id获取
        return this.getMenuInfoById(id);
    }

    @Override
    public MenuInfo get(String code) {
        //       return menuInfoDao.findEntityByCriteria(Restrictions.eq("code", code), Restrictions.eq("valid", true));
        // 根据code获取
        return this.getMenuInfoByCode(code);
    }*/


   /* @Override
    public MenuInfo getMenusTree() {
        MenuInfo root = new MenuInfo();
        root.setId(-1L);
//        List<MenuInfo> menuList = menuInfoDao.createCriteria(Restrictions.eq("valid", true)).addOrder(Order.asc("sort")).list();
        //正序获取
        List<MenuInfo> menuList = this.getByAsc();
        if (menuList != null && menuList.size() > 0) {
            Map<Long, MenuInfo> map = new LinkedHashMap<Long, MenuInfo>(menuList.size());
            for (MenuInfo menu : menuList) {
                if (ObjectUtils.isEmpty(menu.getLayRec())) {
                    menu.setLayRec("11");
                }
                map.put(menu.getId(), menu);
            }
            List<MenuInfo> tree = new ArrayList<MenuInfo>();
            for (MenuInfo m : menuList) {
                if (m.getParentId() == null || m.getParentId().longValue() == -1L) {
                    m.setParent(root);
                    tree.add(m);
                } else {
                    MenuInfo parent = map.get(m.getParentId());
                    m.setParent(parent);
                    if (parent != null) {
                        parent.setLeaf(false);
                    }
                }
            }
            root.setChildren(tree);
        }
        return root;
    }

    @Override
    public List<MenuInfo> getMenuInfoByModul(String moduleCode) {
        moduleCode = moduleCode.substring(0, moduleCode.lastIndexOf("_") == -1 ? moduleCode.length() : moduleCode.lastIndexOf("_"));
//        List<MenuInfo> list = menuInfoDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.like("moduleCode", "%" + moduleCode + "%"));
        //        return list;
        return this.getByLikeModuleCode(moduleCode);
    }

    @Override
    public List<MenuOperate> getOperateList(MenuInfo menuInfo) {
        return this.getOperateListFromJpa(menuInfo);
    }

    @Override
    public List<MenuInfo> getEntityMenus(String entityCode) {
        List<MenuInfo> list = menuInfoDao.createCriteria(Restrictions.eq("entityCode", entityCode), Restrictions.eq("valid", true))
                .addOrder(Order.asc("sort")).list();
        return list;
    }

    @Override
    public List<MenuInfo> getMenuInfoByURL(String url) {
        String hql = "from MenuInfo where url =?";
        List<MenuInfo> list = menuInfoDao.findByHql(hql, url);
        return list;
    }

    @Override
    public void save(MenuInfo menuInfo) {
//        menuInfoDao.save(menuInfo);
        MenuInfoDTO menuInfoDTO = BeanUtil.copy(menuInfo, MenuInfoDTO.class);
        MenuInfoDTO result = supfusionMenuInfoService.save(menuInfoDTO);
        menuInfo.setId(result.getId());
    }

    @Override
    public void save(MenuInfo menuInfo, boolean isMne) {
//        menuInfoDao.save(menuInfo);
        MenuInfoDTO menuInfoDTO = BeanUtil.copy(menuInfo, MenuInfoDTO.class);
        MenuInfoDTO result = supfusionMenuInfoService.save(menuInfoDTO);
        menuInfo.setId(result.getId());
    }

    @Override
    public void publishRefMenuOperate(String operateCode, String name, String entityCode, String viewCode,
                                      String url, boolean enableSpecialPermission, MenuInfo menuInfo) {
//        String opHql = "from MenuOperate mo where mo.menuInfo=? and mo.valid=true and mo.code=?";
//        MenuOperate menuOperate = menuOperateDao.findEntityByHql(opHql, menuInfo, operateCode);
        List<MenuOperate> menuOperateList = menuOperateService.findMenuOperatesByCodeOrMenuInfoId(operateCode, menuInfo.getId());
        MenuOperate menuOperate = menuOperateList.get(0);
        if (null == menuOperate) {
            menuOperate = new MenuOperate();
            menuOperate.setCode(operateCode);
            menuOperate.setEntityCode(entityCode);
            menuOperate.setCid(1000L);
            menuOperate.setEnableAssignPos(true);
            menuOperate.setEnableAssignStaff(true);
            menuOperate.setEnableGroupRestrict(false);
            menuOperate.setEnableNoRestrict(true);
            menuOperate.setEnablePosRestrict(true);
            menuOperate.setForDataPermission(true);
            menuOperate.setViewCode(viewCode);
            menuOperate.setIsQuery(true);
        }
        String sql = "select code  from  EC_OTHER_RESTRICT  other where other.VIEW_CODE= ? and other.VALID = 1  and  (other.JSON_CONDITION is not null or other.CONDITION_SQL is not null) ";
        List<String> otherConditions = menuOperateDao.createNativeQuery(sql, new Object[]{viewCode}).list();
        if (otherConditions.size() > 0) {
            menuOperate.setEnableOtherRestrict(true);
        } else {
            menuOperate.setEnableOtherRestrict(false);
        }
        //设置特殊资源标志位
        if (enableSpecialPermission) {
            menuOperate.setEnableSpecialPermission(true);
        } else {
            menuOperate.setEnableSpecialPermission(false);
        }
        menuOperate.setMenuInfo((MenuInfo) menuInfo);
        menuOperate.setName(name);
        menuOperate.setValid(true);
//        menuOperateDao.save(menuOperate);
        menuOperateService.save(menuOperate);
    }

    @Override
    public void deleteMenuOperateByEntity(String entityCode) {
//        menuOperateDao.flush();
//        String opHql = "from MenuOperate mo where  mo.entityCode=?";
//        List<MenuOperate> opList = menuOperateDao.findByHql(opHql, entityCode);
        List<MenuOperate> opList = menuOperateService.getByEntityCode(entityCode, null);
        for (MenuOperate sys : opList) {
//            menuUserDealInfoService.deleteByMenuOperate(sys);
//            menuOperateDao.delete(sys);
            menuOperateService.delete(sys.getId());
        }

//        String menuInfoHql = "from MenuInfo m where m.entityCode=?";
//        List<MenuInfo> miList = menuInfoDao.findByHql(menuInfoHql, entityCode);

        List<MenuInfo> miList = this.getByEntityCode(entityCode);

//        for (MenuInfo mi : miList) {
////            menuUserDealInfoService.deleteByMenuInfo(mi);
//            menuInfoDao.delete(mi);
//        }

        if (!ObjectUtils.isEmpty(miList)) {
            List<Long> ids = miList.stream().map(AbstractIdEntity::getId).collect(Collectors.toList());
            this.deleteByIds(ids);
        }

        menuOperateDao.flush();
    }

    @Override
    public void deleteMenuOperateByEntityPhysical(String entityCode) {
//        String opHql = "delete MenuOperate mo where mo.menuInfo.id in (select mi.id from MenuInfo mi where mi.entityCode=?0)";
//        menuOperateDao.createQuery(opHql, entityCode).executeUpdate();
//        menuOperateDao.flush();

        //根据entityCode查询menuInfoList
        List<MenuInfo> menuInfoList = this.getByEntityCode(entityCode);
        if (!ObjectUtils.isEmpty(menuInfoList)) {
            List<Long> menuInfoIdList = menuInfoList.stream().map(AbstractIdEntity::getId).collect(Collectors.toList());
            menuOperateService.deleteByMenuInfoIds(menuInfoIdList);
        }


//        String updateParentHql = "update MenuInfo set parentId=null where entityCode=?0";
//        menuInfoDao.createQuery(updateParentHql, entityCode).executeUpdate();
        this.updateByEntityCode(entityCode);

//        menuInfoDao.createQuery("delete MenuInfo mi where mi.entityCode=?0", entityCode).executeUpdate();
        this.deleteByEntityCode(entityCode);

        menuInfoDao.flush();
    }

    @Override
    public MenuInfo getParent(Long id) {
        MenuInfo menuInfo = menuInfoDao.load(id);
        Long perantId = menuInfo.getParentId();
        if (perantId == null) {
            return null;
        }
        return menuInfoDao.load(perantId);
    }

    @Override
    public boolean isContainsWorkflow(String viewCode) {
        String assViewCode = "", type = "";
        String sqlQuery = "select v.ASS_VIEW_CODE, v.TYPE from RUNTIME_VIEW v where v.CODE=?";
        List list = menuInfoDao.createNativeQuery(sqlQuery, viewCode).list();
        if (list.size() > 0) {// 查询的数据存在
            assViewCode = (String) ((Object[]) list.get(0))[0];
            type = ((Object[]) list.get(0))[1] == null ? "" : (String) ((Object[]) list.get(0))[1];
        }
        sqlQuery = "select * from BASE_MENUINFO m inner join WF_DEPLOYMENT d on m.ID=d.MENU_INFO_ID and d.IS_CURRENT_VERSION=1 where m.CODE=?";
        if (type != null && !"".equals(type)) {
            List returnList = null;
            if ("REFERENCE".equals(type) && assViewCode != null && !"".equals(assViewCode)) {
                returnList = menuInfoDao.createNativeQuery(sqlQuery, assViewCode).list();
            } else {
                returnList = menuInfoDao.createNativeQuery(sqlQuery, viewCode).list();
            }
            if (returnList != null && returnList.size() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void batchDealMenuInfoMne(String moduleCode, String artifact) {
        List<MenuInfo> menuInfos = new ArrayList<MenuInfo>();
        if (null != moduleCode && !"".equals(moduleCode)) {
            if (null != artifact) {
                List<MenuInfo> menuInfosByArtifact = this.getMenuInfoByModul(artifact);
                menuInfos.addAll(menuInfosByArtifact);
            }
            if (null != moduleCode) {
                List<MenuInfo> menuInfosByCode = this.getMenuInfoByModul(moduleCode);
                menuInfos.addAll(menuInfosByCode);
            }
        }
    }

    @Override
    public List<MenuInfo> getMenuInfoList(String menuCode) {
        return menuInfoDao.findByCriteria(Restrictions.eq("code", menuCode), Restrictions.eq("valid", true));
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Collection<MenuInfo> getChildren(MenuInfo menuInfo, Company... companys) {
        List<MenuInfo> illist = new ArrayList<MenuInfo>();
        List<MenuInfo> list = new ArrayList<MenuInfo>();
        List<Long> cids = new ArrayList<Long>();
        Boolean groupOnly = true;
        if (companys != null && companys.length > 0) {
            for (Company com : companys) {
                if (com != null) {
                    cids.add(com.getId());
                }
            }
        }
        if (null == menuInfo || menuInfo.getId().equals(-1L)) {
            if (cids.isEmpty()) {
                list = menuInfoDao
                        .createCriteria(Restrictions.or(Restrictions.isNull("parentId"), Restrictions.eq("parentId", new Long(-1))),
                                Restrictions.eq("valid", true), Restrictions.or(Restrictions.ne("absoluteHidden", true), Restrictions.isNull("absoluteHidden"))).addOrder(Order.asc("sort")).addOrder(Order.asc("id")).list();

            } else {
                list = menuInfoDao
                        .createCriteria(
                                (!groupOnly ? Restrictions.or(Restrictions.isNull("groupOnly"), Restrictions.eq("groupOnly", false)) : Restrictions.or(
                                        Restrictions.isNull("groupOnly"), Restrictions.and(Restrictions.eq("groupOnly", true), Restrictions.eq("cid", 1l)))),
                                Restrictions.in("cid", cids), Restrictions.or(Restrictions.isNull("parentId"), Restrictions.eq("parentId", new Long(-1))),
                                Restrictions.eq("valid", true), Restrictions.or(Restrictions.ne("absoluteHidden", true), Restrictions.isNull("absoluteHidden"))).addOrder(Order.asc("sort")).addOrder(Order.asc("id")).list();
            }
        } else {
            if (cids.isEmpty()) {
                list = menuInfoDao.createCriteria(Restrictions.eq("parentId", menuInfo.getId()), Restrictions.eq("valid", true), Restrictions.or(Restrictions.ne("absoluteHidden", true), Restrictions.isNull("absoluteHidden"))).addOrder(Order.asc("sort")).addOrder(Order.asc("id"))
                        .list();

            } else {
                list = menuInfoDao
                        .createCriteria(
                                (!groupOnly ? Restrictions.or(Restrictions.isNull("groupOnly"), Restrictions.eq("groupOnly", false)) : Restrictions.or(
                                        Restrictions.isNull("groupOnly"), Restrictions.and(Restrictions.eq("groupOnly", true), Restrictions.eq("cid", 1l)))),
                                Restrictions.in("cid", cids), Restrictions.eq("parentId", menuInfo.getId()), Restrictions.eq("valid", true), Restrictions.or(Restrictions.ne("absoluteHidden", true), Restrictions.isNull("absoluteHidden")))
                        .addOrder(Order.asc("sort")).addOrder(Order.asc("id")).list();
            }

        }
        if (list != null && list.size() > 0) {
            illist.addAll(list);
        }
        return illist;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<String> getMenuOperateByFlowKey(String key, String version) {
        String hql = " select m.code from MenuOperate m where m.flowKey=? and m.flowVersion=?";
        Object[] obj = new Object[2];
        obj[0] = key;
        obj[1] = version;
        List<String> list = menuOperateDao.findByHql(hql, obj);
        return list;
    }

    private List<MenuOperate> getOperateListFromJpa(MenuInfo menuInfo) {
        List<MenuOperate> retList = new ArrayList<MenuOperate>();
        List<MenuOperate> iRetList = new ArrayList<MenuOperate>();
        MenuOperate operate = null;
        List<Object> args = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();
        sql.append(" select mo.id, ");
        sql.append("        mo.action, ");
        sql.append("        mo.code, ");
        sql.append("        mo.deployment_id, ");
        sql.append("        mo.flow_key, ");
        sql.append("        mo.flow_version, ");
        sql.append("        mo.icon_cls, ");
        sql.append("        mo.memo, ");
        sql.append("        mo.type, ");
        sql.append("        mo.module, ");
        sql.append("        mo.msg_assembled, ");
        sql.append("        mo.name, ");
        sql.append("        wfp.name as flow_name, ");
        sql.append("        mo.namespace, ");
        sql.append("        mo.sort, ");
        sql.append("        mo.target, ");
        sql.append("        mo.url, ");
        sql.append("        mo.valid, ");
        sql.append("        mo.version, ");
        sql.append("        mo.cid, ");
        sql.append("        mo.menuinfo_id ,");
        sql.append("        mo.power_flag, ");
        sql.append("        mo.ENABLE_ASSIGNPOS, ");
        sql.append("        mo.ENABLE_ASSIGNSTAFF, ");
        sql.append("        mo.ENABLE_GROUPRESTRICT, ");
        sql.append("        mo.ENABLE_NORESTRICT, ");
        sql.append("        mo.ENABLE_POSRESTRICT,");
        sql.append("        mo.ENABLE_DEALERPERMISSION,");
        sql.append("        mo.ST_FLAG,");
        sql.append("        mo.IGNORE_PERMISSION,");
        sql.append("        mo.ENABLE_OTHERRESTRICT,");
        sql.append("        mo.ENABLE_SPECIALPERMISSION,");
        sql.append("        mo.VIEW_CODE,");
        sql.append("        mo.IS_HIDDEN");
        sql.append("   from BASE_MENUOPERATE mo ");
        sql.append("   left join WF_DEPLOYMENT wfp ");
        sql.append("   on mo.flow_key = wfp.process_key ");
        sql.append("   and mo.flow_version = wfp.process_version ");
        sql.append("  where (wfp.is_current_version = 1 or mo.flow_key is null) ");
        sql.append("    and mo.valid = 1 ");

        if (menuInfo != null) {
            sql.append(" and mo.menuinfo_id = ?");
            args.add(menuInfo.getId());
        }
        sql.append(" order by mo.sort,wfp.name asc");

        //根据menuInfoId获取menuOperate
        List<MenuOperate> menuOperateList = menuOperateService.findMenuOperatesByCodeOrMenuInfoId(null, menuInfo.getId());

        List<Deployment> deploymentList = deploymentDao.findByCriteria(Restrictions.eq("valid", true));

        List<Object[]> list = Lists.newArrayList();
        for (MenuOperate menuOperate : menuOperateList) {
//            for (Deployment deployment : deploymentList) {
//                if (menuOperate.getFlowKey() == deployment.getProcessKey() &&
//                        String.valueOf(deployment.getProcessVersion()) == menuOperate.getFlowVersion()) {
//                if (null == menuOperate.getFlowKey() || deployment.getIsCurrentVersion()) {
            ArrayList<Object> objects = new ArrayList<>();
            objects.add(menuOperate.getId());
            objects.add(menuOperate.getAction());
            objects.add(menuOperate.getCode());
            objects.add(menuOperate.getDeploymentId());
            objects.add(menuOperate.getFlowKey());
            objects.add(menuOperate.getFlowVersion());
            objects.add(menuOperate.getIconCls());
            objects.add(menuOperate.getMemo());
            objects.add(menuOperate.getMenuOperateType());
            objects.add(menuOperate.getModule());
            objects.add(menuOperate.getMsgAssembled());
            objects.add(menuOperate.getName());
//                    objects.add(deployment.getName());
            objects.add("flowname");
            objects.add(menuOperate.getNamespace());
            objects.add(menuOperate.getSort());
            objects.add(menuOperate.getTarget());
            objects.add(menuOperate.getUrl());
            objects.add(booleanToInt(menuOperate.getValid()));
            objects.add(menuOperate.getVersion());
            objects.add(menuOperate.getCid());
            objects.add(menuOperate.getMenuInfo().getId());
            objects.add(booleanToInt(menuOperate.getPowerFlag()));
            objects.add(booleanToInt(menuOperate.getEnableAssignPos()));
            objects.add(booleanToInt(menuOperate.getEnableAssignStaff()));
            objects.add(booleanToInt(menuOperate.getEnableGroupRestrict()));
            objects.add(booleanToInt(menuOperate.getEnableNoRestrict()));
            objects.add(booleanToInt(menuOperate.getEnablePosRestrict()));
            objects.add(booleanToInt(menuOperate.getEnableDealerPermission()));
//                    objects.add(menuOperate.getStFlag());
            objects.add("stFlag");
            objects.add(booleanToInt(menuOperate.getIgnorePermission()));
            objects.add(booleanToInt(menuOperate.getEnableOtherRestrict()));
            objects.add(booleanToInt(menuOperate.getEnableSpecialPermission()));
            objects.add(menuOperate.getViewCode());
            objects.add(booleanToInt(menuOperate.getIsHidden()));
            list.add(objects.toArray());
//                }
//                }
//            }
        }


//        List<Object[]> list = menuOperateDao.createNativeQuery(sql.toString(), args.toArray(new Object[args.size()])).list();
        for (Object[] objs : list) {
            operate = new MenuOperate();
            if (objs[19] != null) {
                Company company = new Company();
                company.setId(((Number) objs[19]).longValue());
                operate.setCompany(company);
            }
            operate.setMenuInfo((MenuInfo) menuInfo);
            operate.setId(((Number) objs[0]).longValue());
            if (objs[1] != null) {
                operate.setAction((String) objs[1]);
            }
            if (objs[2] != null) {
                operate.setCode((String) objs[2]);
            }
            if (objs[3] != null) {
                operate.setDeploymentId(((Number) objs[3]).longValue());
            }
            if (objs[4] != null) {
                operate.setFlowKey((String) objs[4]);
            }
            if (objs[5] != null) {
                operate.setFlowVersion((String) objs[5]);
            }
            if (objs[6] != null) {
                operate.setIconCls((String) objs[6]);
            }
            if (objs[7] != null) {
                operate.setMemo((String) objs[7]);
            }
            if (objs[8] != null) {
                operate.setMenuOperateType(MenuOperateType.valueOf((String) objs[8]));
            }
            if (objs[9] != null) {
                operate.setModule((String) objs[9]);
            }
            if (objs[10] != null) {
                operate.setMsgAssembled(((Number) objs[10]).intValue());
            }
            if (objs[11] != null) {
                operate.setName((String) objs[11]);
            }
            if (objs[12] != null) {
                operate.setFlowName((String) objs[12]);
            }
            if (objs[13] != null) {
                operate.setNamespace((String) objs[13]);
            }
            if (objs[14] != null) {
                operate.setSort(((Number) objs[14]).intValue());
            }
//            if (objs[15] != null) {
//                operate.setTarget(OperateTarget.valueOf((String) objs[15]));
//            }
            if (objs[16] != null) {
                operate.setUrl((String) objs[16]);
            }
            if (objs[17] != null) {
                operate.setValid(((Number) objs[17]).intValue() == 1);
            }
            if (objs[18] != null) {
                operate.setVersion(((Number) objs[18]).intValue());
            }
            if (objs[21] != null) {
                operate.setPowerFlag(((Number) objs[21]).intValue() == 1);
            }
            if (objs[22] != null) {
                operate.setEnableAssignPos(((Number) objs[22]).intValue() == 1);
            }
            if (objs[23] != null) {
                operate.setEnableAssignStaff(((Number) objs[23]).intValue() == 1);
            }
            if (objs[24] != null) {
                operate.setEnableGroupRestrict(((Number) objs[24]).intValue() == 1);
            }
            if (objs[25] != null) {
                operate.setEnableNoRestrict(((Number) objs[25]).intValue() == 1);
            }
            if (objs[26] != null) {
                operate.setEnablePosRestrict(((Number) objs[26]).intValue() == 1);
            }
            if (objs[27] != null) {
                operate.setEnableDealerPermission(((Number) objs[27]).intValue() == 1);
            }
//            if (objs[28] != null) {
//                operate.setStFlag(systemCodeService.getSystemCode(objs[28].toString()));
//            }
            if (objs[29] != null && "1".equals(objs[29].toString())) {
                operate.setIgnorePermission(Boolean.TRUE);
            }
            if (objs[30] != null) {
                operate.setEnableOtherRestrict(((Number) objs[30]).intValue() == 1);
            }
            if (objs[31] != null) {
                operate.setEnableSpecialPermission(((Number) objs[31]).intValue() == 1);
            }
            if (objs[32] != null) {
                operate.setViewCode((String) objs[32]);
            }
            if (objs[33] != null && "1".equals(objs[33].toString())) {
                operate.setIsHidden(Boolean.TRUE);
            }
            retList.add(operate);
        }
        iRetList.addAll(retList);
        return iRetList;
    }

    private int booleanToInt(Boolean valid) {
        return null == valid || !valid ? 0 : 1;
    }


    @Override
    public void deleteMenuOperateByFlowPhysical(String key, String version) {
        // 物理删除菜单
        String opHql = "from MenuOperate mo where mo.flowKey=? and mo.flowVersion=?";
        Object[] obj = new Object[2];
        obj[0] = key;
        obj[1] = version;
        List<MenuOperate> opList = menuOperateDao.findByHql(opHql, obj);
        for (MenuOperate sys : opList) {
            menuUserDealInfoService.deletePhysicalByMenuOperate(sys);
            menuOperateDao.deletePhysical(sys);
        }
        menuOperateDao.flush();
        menuOperateDao.clear();
//        cache.removeAll();
//        menuIdToOperateListCache.removeAll();
//        try (Jedis jedis = jedisPool.getResource()) {
//            RedisUtil.deleteKeysByScan(jedis, CACHE_NAME + RedisUtil.CACHE_KEY_SPLIT + "*");
//            RedisUtil.deleteKeysByScan(jedis, MENUID_OPERATELIST_CACHE_NAME + RedisUtil.CACHE_KEY_SPLIT + "*");
//        }
//        DistributedLockUtils.removeAll(foundationCache);
//        DistributedLockUtils.removeAll(menuOperateCache);
    }


    @Override
    public void deleteMenuOperateByFlow(String key, String version) {
        // 物理删除菜单
        String opHql = "from MenuOperate mo where mo.flowKey=? and mo.flowVersion=?";
        Object[] obj = new Object[2];
        obj[0] = key;
        obj[1] = version;
        List<MenuOperate> opList = menuOperateDao.findByHql(opHql, obj);
        for (MenuOperate sys : opList) {
            menuUserDealInfoService.deletePhysicalByMenuOperate(sys);
            menuOperateDao.deletePhysical(sys);
        }
//        cache.removeAll();
//        menuIdToOperateListCache.removeAll();
//        try (Jedis jedis = jedisPool.getResource()) {
//            RedisUtil.deleteKeysByScan(jedis, CACHE_NAME + RedisUtil.CACHE_KEY_SPLIT + "*");
//            RedisUtil.deleteKeysByScan(jedis, MENUID_OPERATELIST_CACHE_NAME + RedisUtil.CACHE_KEY_SPLIT + "*");
//        }
//        DistributedLockUtils.removeAll(foundationCache);
//        DistributedLockUtils.removeAll(menuOperateCache);
    }

    @Override
    public MenuOperate getMenuOperateByFlow(String processKey, String processVersion, String activeCode) {
        List<MenuOperate> list = menuOperateDao.findByCriteria(Restrictions.eq("flowKey", processKey), Restrictions.eq("flowVersion", processVersion),
                Restrictions.eq("code", activeCode), Restrictions.eq("valid", true));
        if (list.size() > 0) {
            return list.get(0);
        }
        menuOperateDao.findByCriteria(Restrictions.eq("flowKey", processKey), Restrictions.eq("flowVersion", processVersion));
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void saveMenuOperate(MenuOperate operate) {
//        menuOperateDao.save(operate);
        menuOperateService.save(operate);
    }*/

}
