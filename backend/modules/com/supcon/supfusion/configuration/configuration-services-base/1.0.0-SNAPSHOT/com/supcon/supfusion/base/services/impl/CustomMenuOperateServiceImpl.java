package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.*;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.CustomMenuOperateService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.rbac.api.IPermissionApiService;
import com.supcon.supfusion.rbac.api.dto.MenuOperateGroupRestrictDTO;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service("customMenuOperateService")
@Slf4j
public class CustomMenuOperateServiceImpl implements CustomMenuOperateService {
    @Autowired
    private MenuOperatePODaoImpl menuOperatePODao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MenuOperateCodeUrlRefPODaoImpl menuOperateCodeUrlRefPODao;
    @Autowired
    private IPermissionApiService permissionApiService;
    @Override
    public void save(MenuOperate mo) {
//        menuOperateDao.save(mo);
        //MenuOperateDTO menuOperateDTO = new MenuOperateDTO();
        //BeanUtils.copyProperties(mo, menuOperateDTO);
        //menuOperateDTO.setMenuinfoId(mo.getMenuInfo().getId());
        //supfusionMenuOperateService.save(Collections.singletonList(menuOperateDTO));

//        JSONSerializer serializer = new JSONSerializer();
//        String json = serializer.serialize(mo);
//        MenuOperatePO menuOperatePO = new JSONDeserializer<MenuOperatePO>().deserialize(json, MenuOperatePO.class);
        MenuOperatePO menuOperatePO=null;
        if(mo.getId() == null){
            //long id = idGenerator.getNextId("rbac_menuoperate", "SEQ_ID");
            menuOperatePO= new MenuOperatePO();
            menuOperatePO.setId(IDGenerator.newInstance().generate().longValue());
            menuOperatePO.setValid(1);
            //menuOperatePO.setId(id);
        }else{
            menuOperatePO=menuOperatePODao.load(mo.getId());
        }
        if (!ObjectUtils.isEmpty(mo.getCode())){
            List<MenuOperatePO> dbOperates = menuOperatePODao.findByCriteria(Restrictions.eq("code",mo.getCode()),Restrictions.eq("flowVersion",mo.getFlowKey()));
            if (!ObjectUtils.isEmpty(dbOperates)){
                MenuOperatePO operatePO = dbOperates.get(0);
                if ((!ObjectUtils.isEmpty(operatePO.getEdited())) && operatePO.getEdited()){
                    return;
                }
                menuOperatePO = operatePO;
            }
        }
        menuOperatePO.setValid(mo.getValid() ? 1 : 0);
        menuOperatePO.setCid(mo.getCid());
        menuOperatePO.setAction(mo.getAction());
//        menuOperatePO.setApp(mo.getapp);
        if (null != mo.getCompany()) {
            menuOperatePO.setCid(mo.getCompany().getId());
        }
        menuOperatePO.setModuleCode(mo.getModuleCode());
        menuOperatePO.setCode(mo.getCode());
        menuOperatePO.setDefaultOperate(false);
        menuOperatePO.setDeploymentId(mo.getDeploymentId());
//        menuOperatePO.setEnableAssignDept(mo.getenableassign);
        menuOperatePO.setEnableAssignpos(mo.getEnableAssignPos());
        menuOperatePO.setEnableAssignstaff(mo.getEnableAssignStaff());
        menuOperatePO.setEnableCustomPermission(mo.getEnableOtherRestrict());
        menuOperatePO.setEnableDataPermission(mo.getEnableSpecialPermission());
        menuOperatePO.setEnableDealerpermission(mo.getEnableDealerPermission());
//        menuOperatePO.setEnableDeptrict(mo.getenabledep);
        menuOperatePO.setEnableGrouprestrict(mo.getEnableGroupRestrict());
        menuOperatePO.setEnableNorestrict(mo.getEnableNoRestrict());
        menuOperatePO.setEnablePosrestrict(mo.getEnablePosRestrict());
        menuOperatePO.setEntityCode(mo.getEntityCode());
        menuOperatePO.setFlowKey(mo.getFlowKey());
        menuOperatePO.setFlowVersion(mo.getFlowVersion());
        menuOperatePO.setForFlowPermission(mo.getForDataPermission());
        menuOperatePO.setIconCls(mo.getIconCls());
        menuOperatePO.setIgnorePermission(mo.getIgnorePermission());
        menuOperatePO.setIsAllowProxy(mo.getIsAllowProxy());
        menuOperatePO.setIsHidden(mo.getIsHidden());
        menuOperatePO.setIsOrrelation(mo.getIsOrRelation());
        menuOperatePO.setIsQuery(mo.getIsQuery());

        if (null != mo.getMenuInfo()) {
            menuOperatePO.setMenuinfoId(mo.getMenuInfo().getId());
        }
        menuOperatePO.setMemo(mo.getMemo());
        if (null != mo.getMenuOperateType()) {
            menuOperatePO.setMenuOperateType(mo.getMenuOperateType().toString());
        }
        menuOperatePO.setMsgAssembled(mo.getMsgAssembled());
        menuOperatePO.setName(mo.getName());
//        menuOperatePO.setNameDisplay(mo.getna);
        menuOperatePO.setNamespace(mo.getNamespace());
//        menuOperatePO.setNameZhCn();
        menuOperatePO.setPowerFlag(mo.getPowerFlag());
        if (null != mo.getSort()) {
            menuOperatePO.setSort(Double.parseDouble(mo.getSort().toString()));
        }
        if (null != mo.getTarget() && !"".equals(mo.getTarget().toString())) {
            menuOperatePO.setTarget(mo.getTarget().toString());
        }
        menuOperatePO.setUrl(mo.getUrl());
        if (ObjectUtils.isEmpty(mo.getVersion())){
            menuOperatePO.setVersion(0);
        }else{
            menuOperatePO.setVersion(mo.getVersion());
        }
        menuOperatePO.setViewCode(mo.getViewCode());
        menuOperatePODao.save(menuOperatePO);

    }

    @Override
    public void delete(Long id) {
//        menuOperateDao.delete(id);
        //根据id删除
        //supfusionMenuOperateService.delete(id);
        menuOperatePODao.delete(id);
    }

    @Override
    public void deleteByMenuInfoIds(List<Long> menuInfoIds) {
//        supfusionMenuOperateService.deleteByMenuInfoIds(menuInfoIds);
        String opHql = "delete from rbac_menuoperate  where  menuInfo_id in (:menuInfoIds)";
        menuOperatePODao.createNativeQuery(opHql).setParameterList("menuInfoIds", menuInfoIds).executeUpdate();
    }

    @Override
    public void deleteByCodePhysics(List<String> codes) {
//        supfusionMenuOperateService.deleteByCodePhysics(codes);
        String opHql = "delete from rbac_menuoperate  where  code in (:codes)";
        log.info("删除操作，codes：{}", codes);
        menuOperatePODao.createNativeQuery(opHql).setParameterList("codes", codes).executeUpdate();
    }

    @Override
    public void updateOperateGroupRestrictByEntityCodeAndOther(MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO) {
//        supfusionMenuOperateService.updateOperateGroupRestrictByEntityCodeAndOther(menuOperateGroupRestrictDTO);
    }

    @Override
    public void updateOperateGroupRestrictByEntityCode(MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO) {
//        supfusionMenuOperateService.updateOperateGroupRestrictByEntityCode(menuOperateGroupRestrictDTO);
    }

    @Override
    @Transactional
    public void generateWFOperateUrl(Long deploymentId, String entityCode, Long menuId) {
        if (menuId == null) {
            return;
        }
        // 获取菜单下所有操作
        List<MenuOperatePO> menuOperateList = menuOperatePODao.findByCriteria(Restrictions.eq("menuinfoId", menuId), Restrictions.eq("valid", 1));
        if (menuOperateList.isEmpty()) {
            return;
        }
        // 工作流操作
        List<MenuOperatePO> deploymentMenuOperateList = menuOperateList.stream()
                .filter(m -> m.getDeploymentId() != null && m.getDeploymentId().equals(deploymentId))
                .collect(Collectors.toList());
        // 先删除之前已经生成的url
        Set<String> deploymentMenuOperateCodes = deploymentMenuOperateList.stream().map(MenuOperatePO::getCode).collect(Collectors.toSet());
        menuOperateCodeUrlRefPODao.createNativeQuery("delete from rbac_menuoperatecode_url_ref where menuoperate_code in(:menuoperateCodes)")
                .setParameter("menuoperateCodes", deploymentMenuOperateCodes)
                .executeUpdate();
        String moduleCode = entityCode.split("_")[0];
        List<MenuOperateCodeUrlRefPO> urls = new LinkedList<>();
        for (MenuOperatePO menuOperatePO : deploymentMenuOperateList) {
            String operateUrl = menuOperatePO.getUrl();
            if (operateUrl == null) {
                continue;
            }
            // 操作本身的url
            MenuOperateCodeUrlRefPO url = new MenuOperateCodeUrlRefPO();
            url.setId(IDGenerator.newInstance().generate().longValue());
            url.setMenuoperateCode(menuOperatePO.getCode());
            url.setUrl(operateUrl);
            url.setApp(moduleCode);
            url.setMethodType(0);
            url.setIsCustom(false);
            urls.add(url);

            // 按照controller补充url
            urls.add(generateNewUrl(operateUrl + "/save", url));
            urls.add(generateNewUrl(operateUrl + "/submit", url));
            String urlPrefix = operateUrl.substring(0, operateUrl.lastIndexOf("/"));
            urls.add(generateNewUrl(urlPrefix + "/editStates", url));
            urls.add(generateNewUrl(urlPrefix + "/data/[^/^]+", url));
            // 加入查询操作url
            Optional<MenuOperatePO> queryMenuOperate = menuOperateList.stream().filter(mo -> mo.getCode().endsWith("_self")).findFirst();
            if (queryMenuOperate.isPresent()) {
                String queryUrl = queryMenuOperate.get().getUrl();
                urls.add(generateNewUrl(queryUrl + "-query", url));
                urls.add(generateNewUrl(queryUrl + "-pending", url));
                String prefixUrl = queryUrl.substring(0, operateUrl.lastIndexOf("/"));
                urls.add(generateNewUrl(prefixUrl + "/selectData", url));
                urls.add(generateNewUrl(prefixUrl + "/downloadXls", url));
                urls.add(generateNewUrl(prefixUrl + "/printOnServer", url));
                urls.add(generateNewUrl(prefixUrl + "/batchPrintOnServer", url));
            }
        }
        for (MenuOperateCodeUrlRefPO url : urls) {
            menuOperateCodeUrlRefPODao.save(url);
        }
        // 刷新rbac缓存
        new Thread(()-> permissionApiService.refreshRedis(Collections.singletonList(moduleCode))).start();
    }

    private MenuOperateCodeUrlRefPO generateNewUrl(String newUrl, MenuOperateCodeUrlRefPO urlRefPO) {
        MenuOperateCodeUrlRefPO newUrlRef = new MenuOperateCodeUrlRefPO();
        newUrlRef.setId(IDGenerator.newInstance().generate().longValue());
        newUrlRef.setMenuoperateCode(urlRefPO.getMenuoperateCode());
        newUrlRef.setUrl(urlRefPO.getUrl());
        newUrlRef.setApp(urlRefPO.getApp());
        newUrlRef.setMethodType(urlRefPO.getMethodType());
        newUrlRef.setIsCustom(false);
        if (newUrl.contains("[^/^]+")){
            newUrlRef.setRegMatch(true);
        }else{
            newUrlRef.setRegMatch(false);
        }
        newUrlRef.setUrl(newUrl);
        return newUrlRef;
    }

    /*
    @Override
    public List<MenuOperate> getByEntityCode(String entityCode, Integer powerFlag) {
//        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByEntityCode(entityCode,powerFlag);
//        return dtoToEntity(menuOperateDTOList);
        return null;
    }

    @Override
    public List<MenuOperate> findByEntityCodeAndNotId(String entityCode, Long id) {
//        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByEntityCodeAndNotId(entityCode,id);
//        return dtoToEntity(menuOperateDTOList);
        return null;
    }


    @Override
    public List<MenuOperate> findByMenuInfoIdOrCids(Long menuInfoId, List<Long> cids) {
        //code,cids,menuinfoid
//        List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByMenuCodeAndCids(null, cids, menuInfoId);
//        return dtoToEntity(menuOperateDTOList);
        return null;
    }


    @Override
    public MenuOperate load(Long id) {
//        return menuOperateDao.get(id);
      *//*  MenuOperateDTO menuOperateDTO = supfusionMenuOperateService.findMenuOperateById(id);
        if (null != menuOperateDTO) {
            return BeanUtil.copy(menuOperateDTO, MenuOperate.class);
        }
        return new MenuOperate();*//*
        return null;
    }


    @Override
    public List<MenuOperate> getByCode(String code, Company company) {

//        return this.getByCodeFromJpa(code, company);
        return null;
    }

    @Override
    public List<MenuOperate> findMenuOperateByCodeAndCid(String code, List<Long> cids) {
       *//* List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByCodeAndCid(code, cids);
        return dtoToEntity(menuOperateDTOList);*//*
        return null;
    }

    private List<MenuOperate> dtoToEntity(List<MenuOperate> menuOperateDTOList) {
      *//*  if (!CollectionUtils.isEmpty(menuOperateDTOList)) {
            return menuOperateDTOList.stream().map(a -> {
                MenuOperate menuOperate = BeanUtil.copy(a, MenuOperate.class);
                MenuInfo menuInfo = new MenuInfo();
                menuInfo.setId(a.getId());
                menuOperate.setMenuInfo(menuInfo);
                return menuOperate;
            }).collect(Collectors.toList());
        }
        return Lists.newArrayList();*//*
        return null;
    }

    @Override
    public MenuOperate getFlowList(String entityCode) {
        // List<MenuOperate> mos = menuOperateDao.findByCriteria(Restrictions.eq("entityCode", entityCode), Restrictions.eq("valid", true), Restrictions.eq("powerFlag", true));
        // if (mos != null && !mos.isEmpty()) {
        //            return mos.get(0);
        //        }
        //        return null;
        List<MenuOperate> menuOperateList = this.getByEntityCode(entityCode, 1);
        if (!CollectionUtils.isEmpty(menuOperateList)) {
            return menuOperateList.get(0);
        }
        return null;
    }

    @Override
    public List<MenuOperate> findMenuOperates(Criterion... criterions) {
//        return menuOperateDao.findByCriteria(criterions);
        return null;
    }

    */
    /**
     * 根据code和menuInfoID获取
     *//*
    @Override
    public List<MenuOperate> findMenuOperatesByCodeOrMenuInfoId(String code, Long menuInfoId) {
    *//*    List<MenuOperateDTO> menuOperateDTOList = supfusionMenuOperateService.findMenuOperateByMenuInfo(code, menuInfoId);

        return dtoToEntity(menuOperateDTOList);*//*
        return null;
    }*/

    /* @Override
     public void deleteMenuOperateByPhysical(String code) {
         String opHql = "from MenuOperate mo where  mo.code=?0";
         List<MenuOperate> opList = menuOperateDao.findByHql(opHql, code);
         if (null != opList && !opList.isEmpty()) {
             for (MenuOperate sys : opList) {
                 // 删除指定岗位
                 String deleteRolePermPsHql = "delete RolePPosition rpp where rpp.rolePermission.id in (select rp.id from RolePermission rp where rp.menuOperate=?0) ";
                 String deleteUserPermPsHql = "delete UserPPosition rpp where rpp.userPermission.id in (select rp.id from UserPermission rp where rp.menuOperate=?0) ";

                 // 删除指定人员
                 String deleteRolePermAsStaffHql = "delete RolePStaff rps where rps.rolePermission.id in (select rp.id from RolePermission rp where rp.menuOperate=?0)";
                 String deleteUserPermAsStaffHql = "delete UserPStaff rps where rps.userPermission.id in (select rp.id from UserPermission rp where rp.menuOperate=?0)";

                 String deleteRolePermHql = "delete RolePermission rp where rp.menuOperate=?0";
                 String deleteUserPermHql = "delete UserPermission up where up.menuOperate=?0";
                 String mudiHql = "delete MenuUserDealInfo mudi where mudi.menuOperate = ?0";
                 menuOperateDao.createQuery(deleteRolePermPsHql, sys).executeUpdate();
                 menuOperateDao.createQuery(deleteRolePermAsStaffHql, sys).executeUpdate();
                 menuOperateDao.createQuery(deleteUserPermPsHql, sys).executeUpdate();
                 menuOperateDao.createQuery(deleteUserPermAsStaffHql, sys).executeUpdate();
                 menuOperateDao.createQuery(deleteRolePermHql, sys).executeUpdate();
                 menuOperateDao.createQuery(deleteUserPermHql, sys).executeUpdate();
                 menuOperateDao.createQuery(mudiHql, sys).executeUpdate();
                 menuOperateDao.deletePhysical(sys);
             }
             menuOperateDao.flush();
         }
     }
 */
    @Value("${bap.company.single:false}")
    private Boolean isSingleMode;

  /*  @Override
    public List<MenuOperate> getByCodes(List<String> codes, Company company) {
        List<MenuOperate> op = null;
        if (isSingleMode != null && isSingleMode) {
            op = menuOperateDao.findByCriteria(Restrictions.in("code", codes), Restrictions.eq("cid", company.getId()));
        } else {
            op = menuOperateDao.findByCriteria(Restrictions.in("code", codes), Restrictions.eq("cid", company.getId()));
        }
        List<MenuOperate> list = new ArrayList<MenuOperate>();

        if (null != op && op.size() > 0) {
            list.addAll(op);
        }
        return list;
    }
*/   /* @Override
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
//            List<MenuOperate> mos = menuOperateDao.findByCriteria(Restrictions.eq("entityCode", entityCode), Restrictions.eq("powerFlag", true), Restrictions.eq("valid", true), Restrictions.not(Restrictions.eq("id", id)));
            List<MenuOperate> menuOperateList = Lists.newArrayList();
            if (null != menuOperate.getId()) {
                menuOperateList = this.findByEntityCodeAndNotId(entityCode, id);
            } else {
                menuOperateList = this.getByEntityCode(entityCode,1);
            }
            if (!CollectionUtils.isEmpty(menuOperateList)) {
                List<MenuOperateDTO> menuOperateDTOList = menuOperateList.stream().map(operate -> {
                    operate.setPowerFlag(false);
                    MenuOperateDTO menuOperateDTO = BeanUtil.copy(operate, MenuOperateDTO.class);
                    menuOperateDTO.setMenuinfoId(operate.getMenuInfo().getId());
                    return menuOperateDTO;
                }).collect(Collectors.toList());
                supfusionMenuOperateService.save(menuOperateDTOList);
            }

           *//* for (MenuOperate mo : mos) {
                mo.setPowerFlag(false);
                menuOperateDao.save(mo);

            }*//*
        }
    }

    private List<MenuOperate> getByCodeFromJpa(String code, Company company) {
        List<MenuOperate> op = null;
        try {
            op = menuOperateDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("code", code), Restrictions.eq("cid", company.getId()));
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
        List<MenuOperate> op = null;
        if (company != null && company.getType() != null && company.getType().equals(CompanyType.ORGANIZATION)) {
//            op = menuOperateDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("menuInfo.id", menuInfo.getId()), Restrictions.eq("cid", company.getId()));
            //根据menuInfoId或者cid获取
            op = this.findByMenuInfoIdOrCids(menuInfo.getId(), Arrays.asList(company.getId()));
        } else if (company != null) {
            Company groupCom = companyService.getCompanyByCode(company.getCode());
//            op = menuOperateDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.eq("menuInfo.id", menuInfo.getId()),
//                    Restrictions.or(Restrictions.eq("cid", company.getId()), Restrictions.eq("cid", groupCom.getId())));
            op = this.findByMenuInfoIdOrCids(menuInfo.getId(), Arrays.asList(company.getId(), groupCom.getId()));


        }
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
    }*/
}
