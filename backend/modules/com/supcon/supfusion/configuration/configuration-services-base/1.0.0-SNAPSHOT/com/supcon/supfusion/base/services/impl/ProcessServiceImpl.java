package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.DeploymentDaoImpl;
import com.supcon.supfusion.base.dao.PendingDaoImpl;
import com.supcon.supfusion.base.dao.SuperviseDaoImpl;
import com.supcon.supfusion.base.dao.TransitionStaffDaoImpl;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.enums.DataPermissionType;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.*;
import org.dom4j.Document;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/27
 */
@Slf4j
@Service
@Transactional
public class ProcessServiceImpl extends BaseServiceImpl implements ProcessService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private UserService userService;
    @Autowired
    private PositionService positionService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private DeploymentDaoImpl deploymentDao;
    @Autowired
    private TransitionStaffDaoImpl transitionStaffDao;
    @Autowired
    private SuperviseDaoImpl superviseDao;
    @Autowired
    private DataPermissionService dataPermissionService;
    @Autowired
    private InstanceService instanceService;

    public static String DEFAULT_EMAIL_TITLE;
    public static String DEFAULT_EMAIL_CONTENT;
    public static String DEFAULT_JABBER_CONTENT;
    public static String DEFAULT_SMS_CONTENT;
    public static String DEFAULT_APP_CONTENT;

    public static String REMIND_EMAIL_TITLE;
    public static String REMIND_EMAIL_CONTENT;
    public static String REMIND_JABBER_CONTENT;
    public static String REMIND_SMS_CONTENT;

    @Override
    public List<Deployment> findDeployments(String... entityCodes) {
        return null;
    }

    @Override
    @Transactional(readOnly = true,propagation= Propagation.SUPPORTS)
    public Page<Deployment> findDeployments(Page<Deployment> page, String entityCode) {
        Assert.notNull(page);
        return deploymentDao.findByPage(page, " from Deployment where entityCode = ? and valid=true order by processKey desc,processVersion desc,id DESC",
                entityCode);

    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Deployment> findCurrentDeployments(Page<Deployment> page, String entityCode) {
        List<Criterion> criterions = new ArrayList<Criterion>();
        criterions.add(Restrictions.eq("entityCode", entityCode));
//        Assert.notNull(deployments);
        return deploymentDao.findByPage(page, "from Deployment where entityCode = ? and (isCurrentVersion = true or processXml is null) and valid = true order by processKey desc,"
                + "processVersion desc,id DESC", new Object[] { entityCode });
    }

    @Override
    public void deleteFlow(Deployment deployment, boolean flag, boolean flowFlag) {

    }


    @Override
    public boolean repeat(String processKey, String entityCode) {
        List<Map<String, Object>> recordList = null;
        String sql="select id from wf_deployment where process_key=? and entity_code!=? and valid =1";
        Object[] params = null;
        if(null != entityCode && !StringUtils.isEmpty(entityCode)){
            params = new Object[2];
            params[0]=processKey;
            params[1]=entityCode;
        } else {
            sql="select id from wf_deployment where process_key=? and valid =1";
            params = new Object[1];
            params[0]=processKey;
        }
        recordList = jdbcTemplate.queryForList(sql, params);
        if(recordList.size()>0){
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true,propagation=Propagation.SUPPORTS)
    public Deployment getDeployment(long id) {
        return deploymentDao.get(id);
    }

    @Override
    public String powerXml(Deployment dp, String powerXml) {
        String operatePowers = dp.getOperatePowers();
        if (null != operatePowers && !"".equals(operatePowers)) {
            if (powerXml.equals("")) {
                powerXml = "<taskPower>";
            } else {
                powerXml = powerXml.replace("</taskPower>", "");
            }

            StringBuffer powerXmlSB = new StringBuffer();
            powerXmlSB.append(powerXml);
            String[] powers = operatePowers.split(";");
            Map<String, StringBuffer> activeMap = new HashMap<String, StringBuffer>();
            for (String power : powers) {
                StringBuffer activePower = new StringBuffer();
                String[] arr = power.split("\\$\\$");
                StringBuffer tempPowerXmlSB = activeMap.get(arr[0]);
                if (null != tempPowerXmlSB) {
                    activePower.append(tempPowerXmlSB.toString());
                }
                // powerXmlSB.append("<active taskName=\""+arr[0]+"\">");

                activePower.append("<power userType=\"" + arr[1] + "\"");
                activePower.append(" typeId=\"" + arr[2] + "\"");
                String name = "";
                Long typeId = Long.valueOf(arr[2]);
                if ("USER".equals(arr[1])) {
                    User user = userService.load(typeId);
                    name = user.getStaff().getName();
                } else if ("POSITION".equals(arr[1])) {
                    Position position = positionService.load(typeId);
                    name = position.getName();
                } else if ("ROLE".equals(arr[1])) {
                    Role role = roleService.load(typeId);
                    name = role.getName();
                } else if ("DEPARTMENT".equals(arr[1])) {
                    Department dept = departmentService.load(typeId);
                    name = dept.getName();
                }
                activePower.append(" typeName=\"" + name + "\"");
                if (arr.length <= 3) {
                    activePower.append(" unLimitPower=\"true\"/>");
                    // powerXmlSB.append("</active>");
                    activeMap.put(arr[0], activePower);
                    continue;
                } else {
                    activePower.append(" unLimitPower=\"false\"");

                }
                if (arr.length > 3) {
                    activePower.append(" positionPower=\"" + arr[3] + "\"");
                }
                if (arr.length > 4) {
                    activePower.append(" groupPower=\"" + arr[4] + "\"");
                }
                if (arr.length > 5) {
                    String assignPositionStr = arr[5];
                    if (assignPositionStr.equals("false")) {
                        activePower.append(" assignPositionPower=\"false\"");

                    } else {
                        String assignPositions = assignPositionStr.replace("||", ";");
                        activePower.append(" assignPositionPower=\"true\"");
                        activePower.append(" assignPositions=\"" + assignPositions + "\"");
                    }
                }
                if (arr.length > 6) {
                    String assignStaffStr = arr[6];
                    if (assignStaffStr.equals("false")) {
                        activePower.append(" assignStaffPower=\"false\"");
                    } else {
                        activePower.append(" assignStaffPower=\"true\"");
                        String assignStaffs = assignStaffStr.replace("||", ";");
                        activePower.append(" assignStaffs=\"" + assignStaffs + "\"");
                    }
                }
                activePower.append("/>");
                // powerXmlSB.append("</active>");
                activeMap.put(arr[0], activePower);
            }
            for (Map.Entry<String, StringBuffer> entry : activeMap.entrySet()) {
                String activeCode = entry.getKey();
                StringBuffer xml = entry.getValue();
                powerXmlSB.append("<active taskName=\"" + activeCode + "\">");
                powerXmlSB.append(xml.toString());
                powerXmlSB.append("</active>");
            }
            powerXmlSB.append("</taskPower>");

            powerXml = powerXmlSB.toString();
        }
        return powerXml;
    }

    @Override
    @Transactional
    public String getTranstionSelectStaffs(Long deploymentId) {
        List<TransitionStaff> list=transitionStaffDao.findByCriteria(Restrictions.eq("deploymentId", deploymentId),Restrictions.eq("valid", true));
        if(list!=null&&list.size()>0){
            StringBuilder sb=new StringBuilder();
            for(TransitionStaff ts :list){
                DataPermissionType type=ts.getType();
                Long typeId=Long.valueOf(ts.getTypeId());
                String name="";
                if(type.equals(DataPermissionType.USER)){
                    User user=userService.load(typeId);
                    Staff staff=user.getStaff();
                    if(staff!=null){
                        name=staff.getName();
                    }
                }else if(type.equals(DataPermissionType.DEPTMENT)){
                    Department department=departmentService.load(typeId);
                    name=department.getName();
                }else if(type.equals(DataPermissionType.POSITION)){
                    Position position=positionService.load(typeId);
                    name=position.getName();
                }else if(type.equals(DataPermissionType.ROLE)){
                    Role role=roleService.load(typeId);
                    name=role.getName();
                }
                String sort=(ts.getSort()!=null)?ts.getSort().toString():"";
                String groupName=(ts.getGroupName()!=null)?ts.getGroupName().toString():"";
                String str=ts.getOutcome()+","+ts.getType().toString()+","+ts.getTypeId()+","+name+","+groupName+","+sort;
                if(sb.length()>0){
                    sb.append(";");
                }
                sb.append(str);
            }
            return sb.toString();
        }

        return "";
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Staff> getSupervises(Long deploymentId) {
        String hql="from Supervise as s where s.deploymentId=?0";
        Object[] params = new Object[1];
        params[0] = deploymentId;
        List<Supervise> list=superviseDao.createQuery(hql, params).list();
        if(list==null){
            return null;
        }
        List<Staff> staffList=new ArrayList<>();

        for(Supervise sp:list){
            staffList.add(sp.getStaff());
        }
        return staffList;
    }

    @Override
    public void saveDeployment(Deployment deployment, String operatePower, String actives, String updatePowerString, String superviseNamesMultiIDs, String selectStaffs, String linkRangeChage) {
        if(null == deployment.getMenuInfoId() && null != deployment.getMenuCode()){
            List<MenuInfo> menuInfos = menuInfoService.getMenuInfoList(deployment.getMenuCode());
            if(null != menuInfos && !menuInfos.isEmpty()){
                if(menuInfos.size() == 1){
                    deployment.setMenuInfoId(menuInfos.get(0).getId());
                } else {
                    Company company = getCurrentCompany();
                    if(null != company){
                        Map<Long,MenuInfo> menuInfoMap = new HashMap<Long, MenuInfo>(); //key: cid
                        for(MenuInfo menuInfo : menuInfos){
                            menuInfoMap.put(menuInfo.getCid(), menuInfo);
                        }
                        if(null != menuInfoMap.get(company.getId())){ //如果存在cid为当前公司的菜单则取当前公司
                            deployment.setMenuInfoId(menuInfoMap.get(company.getId()).getId());
                        } else {
                            deployment.setMenuInfoId(menuInfoMap.get(Long.valueOf(1)).getId());
                        }
                    } else {
                        deployment.setMenuInfoId(menuInfos.get(0).getId());
                    }
                }
            }
        }
        deploymentDao.save(deployment);
        // 生成权限
        // if (operatePower != null && !operatePower.equals("")) {
        List<Map<String, String>> addpermissions = null;
        List<Map<String, String>> delpermissions = null;
        actives = null == actives ? "" : actives;
        String[] taskArr = actives.split(",");
        String startActivityCode = "";

        for (String task : taskArr) {
            if (task.toUpperCase().startsWith("START")) {
                startActivityCode = task;
            }
        }
        // 如果单纯只保存基础信息，无需更新菜单权限信息
        if (!StringUtils.isEmpty(actives)) {
            dataPermissionService.updateMenuUserInfo(deployment.getProcessKey(), String.valueOf(deployment.getProcessVersion()), actives,
                    operatePower, deployment.getEntityCode(), deployment.getMenuInfoId());
            dataPermissionService.saveWorkFlowPermissionChanges(deployment.getId(), updatePowerString);
        }
        // 生成国际化
//        internationalService.saveKeyDesc(deployment.getKeyDescs());
        //设置督办人
        saveSupervise(deployment.getId(),superviseNamesMultiIDs);
        //设置迁移线选人
        saveTranstionSelectStaffs(deployment.getId(),selectStaffs);
        //把该流程的所有版本的移动支持一起更新
        updateMobileApprove(deployment);
        //把该流程的所有版本的电子签名配置一起更新
        updateSignatureConfig(deployment);
        clearTransitionDefaultSelectStaff(deployment.getProcessKey(),linkRangeChage);
    }

    @Override
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public String handleFlowXml(String flowXml) {
        String xmlStr = null;
        Document document;
        try {
            document = DocumentHelper.parseText(flowXml);
            String[] nodes = { "start", "task","notification", "auto", "decision","countersign", "fork", "join", "sub-process", "end","end-cancel" };
            for (int i = 0; i < nodes.length; i++) {
                Element root = document.getRootElement();
                List list = root.elements(nodes[i]);
                Iterator<Element> iter = list.iterator();
                //分发后，会签后

                while (iter.hasNext()) {
                    Element element = iter.next();
                    List transitionList = element.elements("transition");
                    if (transitionList != null) {
                        Iterator<Element> transitionIter = transitionList.iterator();
                        while (transitionIter.hasNext()) {
                            //删除selectStaff,requiredStaff,defaultSelectStaff
                            Element transitionElement = transitionIter.next();
                            //分发后，会签后
                            if(nodes[i].equals("fork")||nodes[i].equals("countersign")){
                                Attribute selectStaffAttr= transitionElement.attribute("selectStaff");
                                if(selectStaffAttr!=null){
                                    transitionElement.remove(selectStaffAttr);
                                }
                                Attribute requiredStaffAttr= transitionElement.attribute("requiredStaff");
                                if(requiredStaffAttr!=null){
                                    transitionElement.remove(requiredStaffAttr);
                                }
                                Attribute defaultSelectStaffAttr= transitionElement.attribute("defaultSelectStaff");
                                if(defaultSelectStaffAttr!=null){
                                    transitionElement.remove(defaultSelectStaffAttr);
                                }
                                //聚合前
                            }else if(transitionElement.attributeValue("to").contains("join")){
                                Attribute selectStaffAttr= transitionElement.attribute("selectStaff");
                                if(selectStaffAttr!=null){
                                    transitionElement.remove(selectStaffAttr);
                                }
                                Attribute requiredStaffAttr= transitionElement.attribute("requiredStaff");
                                if(requiredStaffAttr!=null){
                                    transitionElement.remove(requiredStaffAttr);
                                }
                                Attribute defaultSelectStaffAttr= transitionElement.attribute("defaultSelectStaff");
                                if(defaultSelectStaffAttr!=null){
                                    transitionElement.remove(defaultSelectStaffAttr);
                                }
                            }
                        }
                    }

                }

            }
            xmlStr = document.asXML().toString();
        } catch (DocumentException e) {
            log.error(e.getMessage());
//            throw new BAPException(BAPException.Code.DOCUMENT_ERROR);
        } finally {
            document = null;
        }
        return xmlStr;
    }

    @Override
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public String analyticXml(String xmlStr, String language, String... env) {
        Document document;
        try {
            document = DocumentHelper.parseText(xmlStr);
            String[] nodes = { "start", "task","notification", "auto", "decision","countersign", "fork", "join", "sub-process", "end","end-cancel" };
            for (int i = 0; i < nodes.length; i++) {
                Element root = document.getRootElement();
                List list = root.elements(nodes[i]);
                Iterator<Element> iter = list.iterator();
                while (iter.hasNext()) {
                    Element element = iter.next();
                    //处理迁移线的国际化
                    List transitionList = element.elements("transition");
                    if (transitionList != null) {
                        Iterator<Element> transitionIter = transitionList.iterator();
                        while (transitionIter.hasNext()) {
                            Element transitionElement = transitionIter.next();
                            transformInternational(transitionElement, language);
                        }
                    }
                    //处理视图的国际化
                    List viewList = element.elements("open-action");
                    if (viewList != null) {
                        Iterator<Element> viewIter = viewList.iterator();
                        while (viewIter.hasNext()) {
                            Element viewElement = viewIter.next();
                            String viewCode = viewElement.attributeValue("viewCode");
                            if(viewCode!=null&&viewCode.length()>0){
                                String viewName=getViewName(viewCode,language, env);
                                if(!viewName.equals("")){
                                    viewElement.attribute("name").setValue(viewName);
                                }
                            }
                        }
                    }
                    //开始活动的视图要特殊处理
                    if(nodes[i].equals("start")){
                        String viewCode = element.attributeValue("viewCode");
                        if(viewCode!=null&&viewCode.length()>0){
                            String viewName=getViewName(viewCode,language, env);
                            if(!viewName.equals("")){
                                element.attribute("viewName").setValue(viewName);
                            }
                        }
                    }

                    transformInternational(element, language);
                }
            }
            xmlStr = document.asXML().toString();
        } catch (DocumentException e) {
            log.error(e.getMessage());
//            throw new BAPException(BAPException.Code.DOCUMENT_ERROR);
        } finally {
            document = null;
        }
        return xmlStr;
    }

    @Autowired
    private InternationalService i18nService;

    public void transformInternational(Element element, String language) {
        String desc = "";
        String key = element.attributeValue("internationalKey");
        if (key != null && key.length() > 0) {
            desc = i18nService.getI18nValue(key);
        }

        if(!desc.equals(key)&&key!=null){
            element.attribute("desc").setValue(desc);

        }
    }

    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public String getViewName(String code,String language, String... env){
        String viewName="";
        String tableName = "runtime_view";
        if(null != env && env[0].equals("ec")){
            tableName = "ec_view";
        }
        String sql="select v.display_name from " + tableName + " v where v.code=?";
        Object[] params = new Object[1];
        params[0]=code;
        List<Map<String, Object>> recordList = jdbcTemplate.queryForList(sql, params);
        String title ="";
        if(recordList.size()!=0){
            title = recordList.get(0).get("display_name").toString();
        }
        viewName = i18nService.getI18nValue(title);
        if(viewName.equals(title)){
            viewName="";
        }
        return viewName;
    }

    public void saveSupervise(Long deploymentId,String superviseNamesMultiIDs){

        String deleteSql="delete from wf_supervise where deployment_id=?";
        jdbcTemplate.update(deleteSql, deploymentId);
        if(superviseNamesMultiIDs==null||superviseNamesMultiIDs.equals("")){
            return ;
        }
        List<String> sqlArray=new ArrayList<String>();
        String[] ids=superviseNamesMultiIDs.split(",");
        for(String idStr:ids){
            IDGenerator iDGenerator = new IDGenerator(0, 0);
            Long maxID = iDGenerator.generate().longValue();
            String sql="insert into wf_supervise (version,DEPLOYMENT_ID,STAFF,valid,id) values (0,"+deploymentId+","+idStr+",1,"+maxID+")";
            sqlArray.add(sql);
        }
        if(sqlArray.size()>0){
            String sqls[]=new String[sqlArray.size()];
            sqlArray.toArray(sqls);
            jdbcTemplate.batchUpdate(sqls);
        }
    }

    @Transactional
    public void saveTranstionSelectStaffs(Long deploymentId,String selectStaffs){
        transitionStaffDao.bulkExecute("delete from TransitionStaff where deploymentId=?0", deploymentId);
        if(selectStaffs==null||selectStaffs.length()==0){
            return ;
        }
        String[] selectStaffArr=selectStaffs.split(";");
        for(String selectStr:selectStaffArr){
            TransitionStaff ts=new TransitionStaff();
            String[] arr=selectStr.split(",");
            String outcome=arr[0];
            DataPermissionType type=DataPermissionType.valueOf(arr[1].trim());
            Long typeId=Long.valueOf(arr[2]);
            ts.setDeploymentId(deploymentId);
            ts.setOutcome(outcome);
            if(arr.length>3&&arr[3]!=null&&arr[3].length()>0){
                ts.setGroupName(arr[3]);
            }
            if(arr.length>4&&arr[4]!=null&&arr[4].length()>0){
                Double order=null;
                try {
                    order=Double.valueOf(arr[4]);
                } catch (Exception e) {
                }
                ts.setSort(order);
            }
            ts.setType(type);
            ts.setTypeId(typeId);
            ts.setValid(true);
            ts.setVersion(0);
            transitionStaffDao.save(ts);
        }
    }

    @Transactional
    public void updateMobileApprove(Deployment deployment){
        deploymentDao.bulkExecute("update Deployment set mobileapprove = ?0 where processKey = ?1 and valid = true", deployment.getMobileapprove(),
                deployment.getProcessKey());
    }

    @Transactional
    public void updateSignatureConfig(Deployment deployment){
        deploymentDao.bulkExecute("update Deployment set signatureEnable = ?0 where processKey = ?1 and valid = true", deployment.getSignatureEnable(),
                deployment.getProcessKey());
    }

    @Autowired
    private PendingDaoImpl pendingDao;

    public void clearTransitionDefaultSelectStaff(String flowKey,String links){
        if(flowKey==null || flowKey.length()==0 || links==null || links.length()==0){
            return ;
        }
        String deleteSql="delete from base_cookie where TYPE like ?0 ";
        String link[] = links.split(";");
        for(int i=0;i<link.length;i++){
            String type="DEFAULT_SELECTSTAFF_"+flowKey+"_"+link[i];
            pendingDao.createNativeQuery(deleteSql, type).executeUpdate();
        }
    }

}
