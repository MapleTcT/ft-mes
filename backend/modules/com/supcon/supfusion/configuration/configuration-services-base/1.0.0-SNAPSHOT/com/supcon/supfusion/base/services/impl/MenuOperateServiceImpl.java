package com.supcon.supfusion.base.services.impl;

import com.google.common.collect.Lists;
import com.supcon.supfusion.base.dao.MenuOperateDaoImpl;
import com.supcon.supfusion.base.entities.Company;
import com.supcon.supfusion.base.entities.MenuInfo;
import com.supcon.supfusion.base.entities.MenuOperate;
import com.supcon.supfusion.base.enums.CompanyType;
import com.supcon.supfusion.base.services.CompanyService;
import com.supcon.supfusion.base.services.CustomMenuOperateService;
import com.supcon.supfusion.base.services.InternationalService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.rbac.api.IMenuOperateApiService;
import com.supcon.supfusion.rbac.api.dto.MenuOperateDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
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
public class MenuOperateServiceImpl implements MenuOperateService {

    @Autowired
    private IMenuOperateApiService supfusionMenuOperateService;

    @Autowired
    private MenuOperateDaoImpl menuOperateDao;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private InternationalService internationalService;

    @Autowired
    private CustomMenuOperateService customMenuOperateService;


    @Override
    public List<MenuOperate> getByEntityCode(String entityCode,Integer powerFlag) {
        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByEntityCode(entityCode,powerFlag);
        return dtoToEntity(menuOperateDTOList);
    }

    @Override
    public List<MenuOperate> findByEntityCodeAndNotId(String entityCode, Long id) {
        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByEntityCodeAndNotId(entityCode,id);
        return dtoToEntity(menuOperateDTOList);
    }


    @Override
    public List<MenuOperate> findByMenuInfoIdOrCids(Long menuInfoId, List<Long> cids) {
        //code,cids,menuinfoid
//        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByMenuCodeAndCids(null, cids, menuInfoId);
//        return dtoToEntity(menuOperateDTOList);
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(MenuOperate.class);
        if (!ObjectUtils.isEmpty(menuInfoId)){
            detachedCriteria.add(Restrictions.eq("menuInfo.id",menuInfoId));
        }
        if (!ObjectUtils.isEmpty(cids)){
            detachedCriteria.add(Restrictions.in("cid",cids));
        }
        detachedCriteria.add(Restrictions.eq("valid",true));
        return menuOperateDao.findByCriteria(detachedCriteria);
    }

    @Override
    public void deleteByMenuInfoIds(List<Long> menuInfoIds) {
//      supfusionMenuOperateService.deleteByMenuInfoIds(menuInfoIds);
        customMenuOperateService.deleteByMenuInfoIds(menuInfoIds);
    }

    @Override
    public MenuOperate load(Long id) {
        return menuOperateDao.get(id);
//        MenuOperateDTO menuOperateDTO = supfusionMenuOperateService.findMenuOperateById(id);
//        if (null != menuOperateDTO) {
//            return BeanUtil.copy(menuOperateDTO, MenuOperate.class);
//        }
//        return new MenuOperate();
    }

    @Override
    public void save(MenuOperate mo) {
//        menuOperateDao.save(mo);
//        MenuOperateDTO menuOperateDTO = BeanUtil.copy(mo, MenuOperateDTO.class);
//        menuOperateDTO.setMenuinfoId(mo.getMenuInfo().getId());
//        if (mo.getMenuOperateType() != null) {
//            menuOperateDTO.setMenuOperateType(mo.getMenuOperateType().toString());
//        }
//        supfusionMenuOperateService.save(Arrays.asList(menuOperateDTO));
//        internationalService.addInternational(mo.getName());
        customMenuOperateService.save(mo);
    }

    @Override
    public void delete(Long id) {
//        menuOperateDao.delete(id);
        //根据id删除
//        supfusionMenuOperateService.delete(id);
        customMenuOperateService.delete(id);

    }

    @Override
    public List<MenuOperate> getByCode(String code, Long cid) {

        return this.getByCodeFromJpa(code, cid);
    }

    @Override
    public List<MenuOperate> findMenuOperateByCodeAndCid(String code, List<Long> cids) {
        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByCodeAndCid(code, cids);
        return dtoToEntity(menuOperateDTOList);
    }

    private List<MenuOperate> dtoToEntity(List<MenuOperateDTO> menuOperateDTOList) {
        if (!CollectionUtils.isEmpty(menuOperateDTOList)) {
            return menuOperateDTOList.stream().map(a -> {
                MenuOperate menuOperate = BeanUtil.copy(a, MenuOperate.class);
                MenuInfo menuInfo = new MenuInfo();
                menuInfo.setId(a.getMenuinfoId());
                menuOperate.setMenuInfo(menuInfo);
                return menuOperate;
            }).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    @Override
    public MenuOperate getFlowList(String entityCode) {
         List<MenuOperate> mos = menuOperateDao.findByCriteria(Restrictions.eq("entityCode", entityCode), Restrictions.eq("valid", true), Restrictions.eq("powerFlag", true));
         if (mos != null && !mos.isEmpty()) {
                    return mos.get(0);
                }
                return null;
//        List<MenuOperate> menuOperateList = this.getByEntityCode(entityCode,1);
//        if (!CollectionUtils.isEmpty(menuOperateList)) {
//            return menuOperateList.get(0);
//        }
//        return null;
    }

    @Override
    public List<MenuOperate> findMenuOperates(Criterion... criterions) {
        return menuOperateDao.findByCriteria(criterions);
    }

    /**
     * 根据code和menuInfoID获取
     */
    @Override
    public List<MenuOperate> findMenuOperatesByCodeOrMenuInfoId(String code, Long menuInfoId) {
//        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByMenuInfo(code, menuInfoId);
//        return dtoToEntity(menuOperateDTOList);
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(MenuOperate.class);
        if (!ObjectUtils.isEmpty(code)){
            detachedCriteria.add(Restrictions.eq("code",code));
        }
        if (!ObjectUtils.isEmpty(menuInfoId)){
            detachedCriteria.add(Restrictions.eq("menuInfo.id",menuInfoId));
        }
        return menuOperateDao.findByCriteria(detachedCriteria);
    }

    @Override
    public void deleteMenuOperateByPhysical(String code) {
        String opHql = "from MenuOperate mo where  mo.code=?0";
        List<MenuOperate> opList = menuOperateDao.findByHql(opHql, code);
        if (null != opList && !opList.isEmpty()) {
            for (MenuOperate sys : opList) {
                // 删除指定岗位
                String deleteRolePermPsHql = "delete from rbac_rolepposition  where ROLEPERMISSION_ID in (select id from rbac_rolepermission rp where rp.MENUOPERATE_ID=?1) ";
                String deleteUserPermPsHql = "delete from rbac_userpposition  where USERPERMISSION_ID in (select id from rbac_userpermission rp where rp.MENUOPERATE_ID=?1) ";

                // 删除指定人员
                String deleteRolePermAsStaffHql = "delete from rbac_rolepstaff  where ROLEPERMISSION_ID in (select rp.id from rbac_rolepermission rp where rp.MENUOPERATE_ID=?1)";
                String deleteUserPermAsStaffHql = "delete from rbac_userpstaff  where USERPERMISSION_ID in (select rp.id from rbac_userpermission rp where rp.MENUOPERATE_ID=?1)";

                //删除操作URL
                String deleteOperateUrlSql = "delete from rbac_menuoperatecode_url_ref where MENUOPERATE_CODE = ?1";
                String deleteRolePermHql = "delete from rbac_rolepermission  where MENUOPERATE_ID=?1";
                String deleteUserPermHql = "delete from rbac_userpermission  where MENUOPERATE_ID=?1";
//                String mudiHql = "delete MenuUserDealInfo mudi where mudi.menuOperate = ?0";
                menuOperateDao.createNativeQuery(deleteRolePermPsHql, sys.getId()).executeUpdate();
                menuOperateDao.createNativeQuery(deleteRolePermAsStaffHql, sys.getId()).executeUpdate();
                menuOperateDao.createNativeQuery(deleteUserPermPsHql, sys.getId()).executeUpdate();
                menuOperateDao.createNativeQuery(deleteUserPermAsStaffHql, sys.getId()).executeUpdate();
                menuOperateDao.createNativeQuery(deleteRolePermHql, sys.getId()).executeUpdate();
                menuOperateDao.createNativeQuery(deleteUserPermHql, sys.getId()).executeUpdate();
                menuOperateDao.createNativeQuery(deleteOperateUrlSql, code).executeUpdate();
//                menuOperateDao.createQuery(mudiHql, sys).executeUpdate();
//                menuOperateDao.deletePhysical(sys);
                customMenuOperateService.deleteByCodePhysics(Collections.singletonList(sys.getCode()));
            }
            menuOperateDao.flush();
        }
    }

    @Value("${bap.company.single:false}")
    private Boolean isSingleMode;

    @Override
    public List<MenuOperate> getByCodes(List<String> codes, Long companyId) {
        List<MenuOperate> op = null;
        if (isSingleMode != null && isSingleMode) {
            op = menuOperateDao.findByCriteria(Restrictions.in("code", codes), Restrictions.eq("cid", companyId));
        } else {
            op = menuOperateDao.findByCriteria(Restrictions.in("code", codes), Restrictions.eq("cid", companyId));
        }
        List<MenuOperate> list = new ArrayList<MenuOperate>();

        if (null != op && op.size() > 0) {
            list.addAll(op);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> getS2BillInfo(Long userId, Long menuInfoId) {
        StringBuilder sql = new StringBuilder(
                "select m.MenuUser_PowerCode POWERCODE, a.Activity_ID ACTIVEID, t.Transition_ID TRANSITIONID, t.Transition_TableCode FLOWTABLECODE, t.Transition_Des NAME ");
        sql.append(" from S2BASE_MENUUSER m, S2BASE_ACTIVITY a, S2BASE_TRANSITION t ")
                .append(" where m.MenuUser_PowerCode = a.Activity_UID and a.Activity_ID = t.Transition_OutActive ")
                .append(" and m.MenuUser_UserID = ? and m.MenuUser_State = 1 and m.MenuUser_PowerFlag = 1 and m.MenuUser_MenuID = ? ")
                .append(" and a.Activity_State = 1 and a.Activity_IfCurrent = 1 and t.Transition_State = 1 and a.Activity_Type IN (0, 2) ")
                .append(" and (t.Transition_Des = '制定' or t.Transition_ExtFieldH = 'true')");
        return menuOperateDao.createNativeQuery(sql.toString(), new Object[]{userId, menuInfoId}).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).list();
    }

    @Override
    public List<MenuOperate> getOperateByCodeAndFlowKey(String code, String flowKey, Company company) {
        List<MenuOperate> op = null;
        if (company != null && company.getType() != null && company.getType().equals(CompanyType.ORGANIZATION)) {
            String hql = " from MenuOperate mo where mo.code = ? and mo.deploymentId in (select d.id from Deployment d where d.processKey = ? and d.isCurrentVersion = true and d.valid = true) and mo.valid = true";
            op = menuOperateDao.findByHql(hql, new Object[]{code, flowKey});
        } else if (company != null) {
            String hql = " from MenuOperate mo where mo.code = ? and mo.deploymentId in (select d.id from Deployment d where d.processKey = ? and d.isCurrentVersion = true and d.valid = true) and mo.valid = true";
            op = menuOperateDao.findByHql(hql, new Object[]{code, flowKey});
        }
        List<MenuOperate> list = new ArrayList<MenuOperate>();
        if (op != null) {
            for (MenuOperate me : op) {
                list.add((MenuOperate) me);
            }
        }
        return list;
    }

    @Override
    public List<MenuOperate> getMenuOperateByNamespace(String namespace) {
        List<MenuOperate> mos = menuOperateDao.findByCriteria(Restrictions.eq("namespace", namespace), Restrictions.eq("valid", true));
        List<MenuOperate> list = new ArrayList<MenuOperate>();
        if (null != mos) {
            for (MenuOperate me : mos) {
                list.add((MenuOperate) me);
            }
            return list;
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<MenuOperate> getByPage(Page<MenuOperate> page, String sql, Map<String, Object> paramsMap, Object... objects) {
        SQLQuery query = menuOperateDao.createNativeQuery(sql, objects).addEntity(MenuOperate.class);
        if (null != paramsMap && !paramsMap.isEmpty()) {
            for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Collection) {
                    query.setParameterList(entry.getKey(), (Collection) value);
                } else {
                    query.setParameter(entry.getKey(), value);
                }
            }
        }
        List<MenuOperate> list = query.list();
        if (null != list && !list.isEmpty()) {
            page.setTotalCount(list.size());
            int startIndex = page.getFirst() - 1;
            int endIndex = startIndex + page.getPageSize();
            if (endIndex > list.size() - 1) {
                endIndex = list.size();
            }
            List<MenuOperate> subList = list.subList(startIndex, endIndex);
            page.setResult(subList);
        } else {
            page.setTotalCount(0);
            page.setResult(Collections.EMPTY_LIST);
        }

        return page;
    }

    @Override
    public void updateOtherMenuOperate(MenuOperate menuOperate) {
        boolean powerFlag = menuOperate.getPowerFlag() == null ? false : menuOperate.getPowerFlag();
        if (powerFlag) {
            Long id = menuOperate.getId();
            String entityCode = menuOperate.getEntityCode();
            List<MenuOperate> mos = menuOperateDao.findByCriteria(Restrictions.eq("entityCode", entityCode), Restrictions.eq("powerFlag", true), Restrictions.eq("valid", true), Restrictions.not(Restrictions.eq("id", id)));
//            List<MenuOperate> menuOperateList = Lists.newArrayList();
//            if (null != menuOperate.getId()) {
//                menuOperateList = this.findByEntityCodeAndNotId(entityCode, id);
//            } else {
//                menuOperateList = this.getByEntityCode(entityCode,1);
//            }
//            if (!CollectionUtils.isEmpty(menuOperateList)) {
//                List<MenuOperateDTO> menuOperateDTOList = menuOperateList.stream().map(operate -> {
//                    operate.setPowerFlag(false);
//                    MenuOperateDTO menuOperateDTO = BeanUtil.copy(operate, MenuOperateDTO.class);
//                    menuOperateDTO.setMenuinfoId(operate.getMenuInfo().getId());
//                    return menuOperateDTO;
//                }).collect(Collectors.toList());
//                supfusionMenuOperateService.save(menuOperateDTOList);
//            }

            for (MenuOperate mo : mos) {
                mo.setPowerFlag(false);
                customMenuOperateService.save(mo);
            }
        }
    }

    private List<MenuOperate> getByCodeFromJpa(String code, Long cid) {
        List<MenuOperate> op = null;
        try {
            op = menuOperateDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("code", code), Restrictions.eq("cid", cid));
            if (op == null || op.isEmpty()) {
                op = menuOperateDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("code", code));
            }
        } catch (Exception e) {
            log.debug("code Already exists or repeat multiple !");
            log.error(e.getMessage(), e);
        }
        List<MenuOperate> list = new ArrayList<MenuOperate>();
        if (null != op) {
            for (MenuOperate me : op) {
                list.add((MenuOperate) me);
            }
        }
        return list;
    }

    @Override
    public Boolean checkWhetherIsConfiged(String viewCode) {
        //and  (other.JSON_CONDITION is not null or other.CONDITION_SQL is not null)
        String sql = "select code  from  EC_OTHER_RESTRICT  other where other.VIEW_CODE= ? and other.VALID = 1  and  (other.JSON_CONDITION is not null or other.CONDITION_SQL is not null) ";
        List<String> otherConditions = menuOperateDao.createNativeQuery(sql, new Object[]{viewCode}).list();
        if (otherConditions.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public List<MenuOperate> getAllOperateByMenu(MenuInfo menuInfo, Company company) {
        Assert.notNull(menuInfo);
        List<MenuOperate> op = this.findByMenuInfoIdOrCids(menuInfo.getId(), Arrays.asList(company.getId()));
        List<MenuOperate> list = new ArrayList<MenuOperate>();
        if (op != null && !op.isEmpty()) {
            for (MenuOperate me : op) {
                list.add((MenuOperate) me);
            }
        }
        menuOperateDao.flush();
        return list;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public MenuOperate getMenuOperate(Long deploymentId, String code) {
        String hql = "from MenuOperate where deploymentId=? and code=?";
        MenuOperate menuOperate = menuOperateDao.findEntityByHql(hql, deploymentId, code);
        return menuOperate;
    }

    @Override
    public MenuOperate getMenuOperate(String code) {
        String hql="from  MenuOperate o  where o.code=?  and o.valid=true";
        List<MenuOperate>  operates=menuOperateDao.findByHql(hql, new Object[]{code});
        if(null!=operates&&operates.size()>0) {
            return operates.get(0);
        }
        return null;
    }
}
