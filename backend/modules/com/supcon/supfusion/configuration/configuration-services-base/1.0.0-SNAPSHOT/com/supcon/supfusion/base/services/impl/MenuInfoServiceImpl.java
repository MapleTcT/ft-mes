package com.supcon.supfusion.base.services.impl;

import com.google.common.collect.Lists;
import com.supcon.supfusion.base.dao.DeploymentDaoImpl;
import com.supcon.supfusion.base.dao.MenuInfoDaoImpl;
import com.supcon.supfusion.base.dao.MenuOperateDaoImpl;
import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.enums.MenuOperateType;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import com.supcon.supfusion.rbac.api.dto.MenuInfoDTO;
import com.supcon.supfusion.rbac.api.dto.MenuInfoJsonDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateJsonDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class MenuInfoServiceImpl implements MenuInfoService {

    @Autowired
    private MenuInfoDaoImpl menuInfoDao;

    @Autowired
    private MenuOperateDaoImpl menuOperateDao;
    @Autowired
    private MenuUserDealInfoService menuUserDealInfoService;

    @Autowired
    private IMenuInfoApiService supfusionMenuInfoService;

    @Autowired
    private MenuOperateService menuOperateService;

    @Autowired
    private DeploymentDaoImpl deploymentDao;

    @Autowired
    private InternationalService internationalService;
    @Autowired
    private CustomMenuInfoService customMenuInfoService;

    @Override
    public List<MenuInfo> getByAsc() {
        List<MenuInfoDTO> menuInfoDTOList = supfusionMenuInfoService.getMenuInfoAsc();
        return DtoToEntity(menuInfoDTOList);
    }

    @Override
    public MenuInfo getMenuInfoByCode(String code) {
//        try {
//            MenuInfoDTO menuInfoDTO = supfusionMenuInfoService.getMenuInfoByCode(code);
//            if (null != menuInfoDTO) {
//                return BeanUtil.copy(menuInfoDTO, MenuInfo.class);
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
        return menuInfoDao.findEntityByCriteria(Restrictions.eq("code", code), Restrictions.eq("valid", true));
    }

    @Override
    public MenuInfo getMenuInfoById(Long id) {
//        try {
//            MenuInfoDTO menuInfoDTO = supfusionMenuInfoService.getMenuInfoById(id);
//            if (null != menuInfoDTO) {
//                return BeanUtil.copy(menuInfoDTO, MenuInfo.class);
//            }
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
        return menuInfoDao.get(id);
    }

    @Override
    public List<MenuInfo> getByEntityCode(String entityCode) {
        List<MenuInfoDTO> menuInfoDTOList = supfusionMenuInfoService.getMenuInfoByEntityCode(entityCode);
        return DtoToEntity(menuInfoDTOList);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        supfusionMenuInfoService.deleteMenuInfoByIds(ids);
    }

    @Override
    public void updateByEntityCode(String entityCode) {
        supfusionMenuInfoService.updateMenuInfoByEntityCode(entityCode);
    }

    @Override
    public void deleteByEntityCode(String entityCode) {
        supfusionMenuInfoService.deleteMenuInfoByEntityCode(entityCode);
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
    }

    @Override
    public MenuInfo getMenusTree() {
        MenuInfo root = new MenuInfo();
        root.setId(-1L);
        List<MenuInfo> menuList = menuInfoDao.createCriteria(Restrictions.eq("valid", true)).addOrder(Order.asc("sort")).list();
        //正序获取
//        List<MenuInfo> menuList = this.getByAsc();
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
                    if (parent != null) {
                        parent.getChildren().add(m);
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
//        List<MenuInfoDTO> menuInfoDTOList = supfusionMenuInfoService.getMenuInfoByModuleCode(moduleCode);
        return menuInfoDao.createCriteria(Restrictions.eq("valid", true), Restrictions.like("moduleCode", "%" + moduleCode + "%")).list();
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
    public List<MenuInfo> getEntityMenus2(String entityCode) {
        List<MenuInfo> list = menuInfoDao.createCriteria(Restrictions.or(Restrictions.eq("entityCode", entityCode), Restrictions.and(Restrictions.isNull("entityCode"), Restrictions.like("entityCode", entityCode, MatchMode.START))), Restrictions.eq("valid", true))
                .addOrder(Order.asc("sort")).list();
        return list;
    }

    @Override
    public List<MenuInfo> getMenuInfoByURL(String url) {
        String hql = "from MenuInfo where url =?0";
        List<MenuInfo> list = menuInfoDao.findByHql(hql, url);
        return list;
    }

    @Override
    public void saveMenuInfoAndOperates(List<MenuInfo> menuInfos) {
        List<MenuInfoJsonDTO> menuInfoJsonDTOS = new ArrayList<>(menuInfos.size());
        for (MenuInfo menuInfo : menuInfos) {
            if (menuInfo.getId() == -1L) {
                continue;
            }
            MenuInfoJsonDTO menuInfoJsonDTO = BeanUtil.copy(menuInfo, MenuInfoJsonDTO.class);
            Set<MenuOperate> menuOperates = menuInfo.getMenuOperates();
            List<MenuOperateJsonDTO> menuOperateJsonDTOS = new ArrayList<>(menuOperates.size());
            for (MenuOperate mo : menuOperates) {
                MenuOperateJsonDTO copy = BeanUtil.copy(mo, MenuOperateJsonDTO.class);
                if (mo.getMenuOperateType() != null) {
                    if (MenuOperateType.ACTIVEOPERATE.equals(mo.getMenuOperateType()) || MenuOperateType.FLOWOPERATE.equals(mo.getMenuOperateType()) ||
                            MenuOperateType.NORMAL.equals(mo.getMenuOperateType())) {
                        copy.setMenuOperateType(mo.getMenuOperateType().toString());
                    } else {
                        copy.setMenuOperateType(null);
                    }
                } else {
                    copy.setMenuOperateType(null);
                }
                menuOperateJsonDTOS.add(copy);
            }
            menuInfoJsonDTO.setMenuOperates(menuOperateJsonDTOS);
            menuInfoJsonDTOS.add(menuInfoJsonDTO);
            log.info("add rbac menu: " + menuInfoJsonDTO.toString());
        }
        supfusionMenuInfoService.saveBachUrl(menuInfoJsonDTOS);
    }

    @Override
    public void save(MenuInfo menuInfo) {
        String internationalName = menuInfo.getName();
        if (internationalName.indexOf("$&#") > 0) {
            menuInfo.setName(internationalName.split("\\$&#")[0].substring(4));
        }
        customMenuInfoService.save(menuInfo);
//        MenuInfoDTO menuInfoDTO = BeanUtil.copy(menuInfo, MenuInfoDTO.class);
//        try {
//            MenuInfoDTO result = supfusionMenuInfoService.save(menuInfoDTO);
//        } catch (Exception e) {
//            log.error("add menuInfo[" + menuInfo.getCode() + "] error " + e.getMessage());
//        }
        internationalService.addInternational(internationalName);
    }

    @Override
    public void publishRefMenuOperate(String operateCode, String name, String entityCode, String viewCode,
                                      String url, boolean enableSpecialPermission, MenuInfo menuInfo) {
        String opHql = "from MenuOperate mo where mo.menuInfo=? and mo.valid=true and mo.code=?";
        MenuOperate menuOperate = menuOperateDao.findEntityByHql(opHql, menuInfo, operateCode);
//        List<MenuOperate> menuOperateList = menuOperateService.findMenuOperatesByCodeOrMenuInfoId(operateCode, menuInfo.getId());
//        MenuOperate menuOperate = menuOperateList.get(0);
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
        String sql = "select code  from  ec_other_restrict  other where other.VIEW_CODE= ? and other.VALID = 1  and  (other.JSON_CONDITION is not null or other.CONDITION_SQL is not null) ";
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
        menuOperateDao.flush();
        String opHql = "from MenuOperate mo where  mo.entityCode=?";
        List<MenuOperate> opList = menuOperateDao.findByHql(opHql, entityCode);
//        List<MenuOperate> opList = menuOperateService.getByEntityCode(entityCode,null);
        for (MenuOperate sys : opList) {
//            menuUserDealInfoService.deleteByMenuOperate(sys);
//            menuOperateDao.delete(sys);
            menuOperateService.delete(sys.getId());
        }

        String menuInfoHql = "from MenuInfo m where m.entityCode=?";
        List<MenuInfo> miList = menuInfoDao.findByHql(menuInfoHql, entityCode);

//        List<MenuInfo> miList = this.getByEntityCode(entityCode);

//        for (MenuInfo mi : miList) {
//            menuUserDealInfoService.deleteByMenuInfo(mi);
//            menuInfoDao.delete(mi);
//        }
        if (!ObjectUtils.isEmpty(miList)){
            customMenuInfoService.deleteByIds(miList.stream().map(MenuInfo::getId).collect(Collectors.toList()));
        }

//        if (!ObjectUtils.isEmpty(miList)) {
//            List<Long> ids = miList.stream().map(AbstractIdEntity::getId).collect(Collectors.toList());
//            this.deleteByIds(ids);
//        }

        menuOperateDao.flush();
    }

    @Override
    public void deleteMenuOperateByEntityPhysical(String entityCode) {
        String opHql = "delete MenuOperatePO mo where mo.menuinfoId in (select mi.id from MenuInfoPO mi where mi.entityCode=?0)";
        menuOperateDao.createQuery(opHql, entityCode).executeUpdate();
        menuOperateDao.flush();

        //根据entityCode查询menuInfoList
//        List<MenuInfo> menuInfoList = this.getByEntityCode(entityCode);
//        if (!ObjectUtils.isEmpty(menuInfoList)) {
//            List<Long> menuInfoIdList = menuInfoList.stream().map(AbstractIdEntity::getId).collect(Collectors.toList());
//             menuOperateService.deleteByMenuInfoIds(menuInfoIdList);
//        }


        String updateParentHql = "update MenuInfoPO set parentId=null where entityCode=?0";
        menuInfoDao.createQuery(updateParentHql, entityCode).executeUpdate();
//        this.updateByEntityCode(entityCode);

        menuInfoDao.createQuery("delete MenuInfoPO mi where mi.entityCode=?0", entityCode).executeUpdate();
//        this.deleteByEntityCode(entityCode);

        menuInfoDao.flush();
    }

    @Override
    public MenuInfo getParent(Long id) {
        MenuInfo menuInfo = menuInfoDao.load(id);
        if (menuInfo == null) {
            return null;
        }
        Long perantId = menuInfo.getParentId();
        if (perantId == null) {
            return null;
        }
        return menuInfoDao.load(perantId);
    }

    @Override
    public boolean isContainsWorkflow(String viewCode) {
        String assViewCode = "", type = "";
        String sqlQuery = "select v.ASS_VIEW_CODE, v.TYPE from runtime_view where v.CODE=?";
        List list = menuInfoDao.createNativeQuery(sqlQuery, viewCode).list();
        if (list.size() > 0) {// 查询的数据存在
            assViewCode = (String) ((Object[]) list.get(0))[0];
            type = ((Object[]) list.get(0))[1] == null ? "" : (String) ((Object[]) list.get(0))[1];
        }
        sqlQuery = "select * from base_menuinfo m inner join WF_DEPLOYMENT d on m.ID=d.MENU_INFO_ID and d.IS_CURRENT_VERSION=1 where m.CODE=?";
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
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
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
        sql.append("   from base_menuoperate mo ");
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

        List<Object[]> list = menuOperateDao.createNativeQuery(sql.toString(), args.toArray(new Object[args.size()])).list();
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
//            menuUserDealInfoService.deletePhysicalByMenuOperate(sys);
//            menuOperateDao.deletePhysical(sys);
            menuOperateService.deleteMenuOperateByPhysical(sys.getCode());
        }
        menuOperateDao.flush();
        menuOperateDao.clear();
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
//            menuUserDealInfoService.deletePhysicalByMenuOperate(sys);
//            menuOperateDao.deletePhysical(sys);
            menuOperateService.deleteMenuOperateByPhysical(sys.getCode());
        }
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
    }

    //此方式是远程调用微服务删除,事务没有意义.所以以无事务的方式运行
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String deleteRbacAllByModuleCode(String code) {
//        supfusionMenuInfoService.deleteMenuInfoByApps(code);
        return customMenuInfoService.deleteMenuInfoByApps(code);
    }
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public MenuInfo getMenuInfo(String code) {
        List<MenuInfo> list = menuInfoDao.findByCriteria(Restrictions.eq("code", code));
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }
}
