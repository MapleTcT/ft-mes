package com.supcon.supfusion.base.services.impl;

import com.google.common.base.Enums;
import com.supcon.supfusion.base.dao.DataPermissionDaoImpl;
import com.supcon.supfusion.base.dao.DataPermissionStaffDaoImpl;
import com.supcon.supfusion.base.dao.DataPmsPositionDaoImpl;
import com.supcon.supfusion.base.dao.MenuInfoDaoImpl;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.enums.DataPermissionType;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.base.services.DataPermissionService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.base.services.MenuUserDealInfoService;
import com.supcon.supfusion.framework.scaffold.hibernate.utils.BeanUtil;
import com.supcon.supfusion.rbac.api.IMenuInfoApiService;
import com.supcon.supfusion.rbac.api.IPermissionApiService;
import com.supcon.supfusion.rbac.api.dto.FlowPermissionDTO;
import com.supcon.supfusion.rbac.api.dto.FlowPermissionPositionDTO;
import com.supcon.supfusion.rbac.api.dto.FlowPermissionStaffDTO;
import com.supcon.supfusion.rbac.api.enums.FlowPermissionType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.sql.Types;
import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/21
 */
@Slf4j
@Service
@Transactional
public class DataPermissionServiceImpl extends BaseServiceImpl implements DataPermissionService {

    @Autowired
    private InternationalService i18nService;

    @Value("${bap.company.single:false}")
    private Boolean singleCompany;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MenuInfoDaoImpl menuInfoDao;

    @Autowired
    private IPermissionApiService supfusionPermissionService;

    @Override
    public void savePermission(DataPermission dataPermission) {
        FlowPermissionDTO flowPermissionDTO = BeanUtil.copy(dataPermission, FlowPermissionDTO.class);

        DataPermissionType dataPermissionType = dataPermission.getDataPermissionType();
        switch (Enums.getField(dataPermissionType).getName()) {
            case "USER": flowPermissionDTO.setFlowPermissionType(FlowPermissionType.USER);
            break;
            case "WORKGROUP": flowPermissionDTO.setFlowPermissionType(FlowPermissionType.WORKGROUP);
                break;
            case "DEPTMENT": flowPermissionDTO.setFlowPermissionType(FlowPermissionType.DEPTMENT);
                break;
            case "ROLE": flowPermissionDTO.setFlowPermissionType(FlowPermissionType.ROLE);
                break;
            case "POSITION": flowPermissionDTO.setFlowPermissionType(FlowPermissionType.POSITION);
                break;
        }

        log.info("开始保存工作流权限");

        FlowPermissionDTO result = supfusionPermissionService.saveFlowPermission(flowPermissionDTO);

        if (null != result) {
            dataPermission.setId(result.getId());
        }
    }

    @Override
    public void savePermissionStaff(DataPermissionStaff dataPermissionStaff) {
        FlowPermissionStaffDTO permissionStaffDTO = BeanUtil.copy(dataPermissionStaff, FlowPermissionStaffDTO.class);

        permissionStaffDTO.setDatapermissionId(dataPermissionStaff.getDataPermissionId());
        supfusionPermissionService.saveFlowPermissionStaff(permissionStaffDTO);
    }

    @Override
    public void savePermissionPosition(DataPmsPosition dataPmsPosition) {
        FlowPermissionPositionDTO permissionPositionDTO = BeanUtil.copy(dataPmsPosition, FlowPermissionPositionDTO.class);

        permissionPositionDTO.setDatapermissionId(dataPmsPosition.getDataPermissionId());
        supfusionPermissionService.saveFlowPermissionPosition(permissionPositionDTO);
    }


    @Override
    public Set<Map<String, Object>> getFlowStart(String entityCode, Long userId) {
        return getFlowStartFromJpa(entityCode, userId);
    }

    @Override
    public String getFlowPower(String flowKey, String flowVersion) {
        // 记录活动权限的map,记录活动编码，分组编码，岗位权限，无限制
        Map<String, Map<Long, Map<String, Object>>> activeMap = new HashMap<String, Map<Long, Map<String, Object>>>();
        // 记录活动权限中指定人员权限
        Map<String, Map<Long, List<Map<String, Object>>>> assignStaffMap = new HashMap<String, Map<Long, List<Map<String, Object>>>>();
        // 记录活动权限中指定岗位权限
        Map<String, Map<Long, List<Map<String, Object>>>> assignPositionMap = new HashMap<String, Map<Long, List<Map<String, Object>>>>();

        // 查询分组为用户的权限，type=USER
        String userSql = "SELECT DP.ID DATAPERMISSIONID,DP.ACTIVITY_CODE ACTIVITYCODE,DP.DATA_PERMISSION_TYPE DATAPERMISSIONTYPE,DP.GROUP_POWER_FLAG GROUPPOWERFLAG,DP.POSITION_POWER_FLAG POSITIONPOWERFLAG,DP.UNLIMITED_POWER UNLIMITEDPOWER,DP.TYPE_ID TYPEID,S.NAME STAFFNAME,us.NAME TYPENAME"
                + " FROM BASE_DATAPERMISSION DP,BASE_USERINFO us,BASE_STAFF s WHERE DP.FLOW_KEY=?"
                + " AND DP.DATA_PERMISSION_TYPE='USER' and US.ID=DP.TYPE_ID and US.STAFF_ID=S.ID and US.VALID=1 "
                + " and S.VALID=1 and S.WORK_STATUS!='STAFFSTATUSE_NATURE/STAFFSTATUTS_03'";

        List<Map<String, Object>> userList = jdbcTemplate.queryForList(userSql, flowKey);

        for (Map<String, Object> map : userList) {
            Long dpId = (map.get("DATAPERMISSIONID") != null) ? Long.valueOf(map.get("DATAPERMISSIONID").toString()) : -1L;
            String activeCode = (map.get("ACTIVITYCODE") != null) ? map.get("ACTIVITYCODE").toString() : "";
            Map<Long, Map<String, Object>> userMap = activeMap.get(activeCode);
            if (null == userMap) {
                userMap = new HashMap<Long, Map<String, Object>>();
            }
            userMap.put(dpId, map);
            activeMap.put(activeCode, userMap);
        }

        // 查询分组为部门的权限，type=DEPTMENT
        String deptSql = "SELECT  DP.ID DATAPERMISSIONID,DP.ACTIVITY_CODE ACTIVITYCODE,DP.DATA_PERMISSION_TYPE DATAPERMISSIONTYPE,DP.GROUP_POWER_FLAG GROUPPOWERFLAG,DP.POSITION_POWER_FLAG POSITIONPOWERFLAG,DP.UNLIMITED_POWER UNLIMITEDPOWER,DP.TYPE_ID TYPEID,DT.NAME TYPENAME "
                + " FROM BASE_DATAPERMISSION DP,BASE_DEPARTMENT DT WHERE DP.FLOW_KEY=?  "
                + " AND DP.DATA_PERMISSION_TYPE='DEPTMENT' AND DP.TYPE_ID=DT.ID AND DT.VALID=1";
        List<Map<String, Object>> deptList = jdbcTemplate.queryForList(deptSql, flowKey);

        for (Map<String, Object> map : deptList) {
            Long dpId = (map.get("DATAPERMISSIONID") != null) ? Long.valueOf(map.get("DATAPERMISSIONID").toString()) : -1L;
            String activeCode = (map.get("ACTIVITYCODE") != null) ? map.get("ACTIVITYCODE").toString() : "";
            Map<Long, Map<String, Object>> deptMap = activeMap.get(activeCode);
            if (null == deptMap) {
                deptMap = new HashMap<Long, Map<String, Object>>();
            }
            deptMap.put(dpId, map);
            activeMap.put(activeCode, deptMap);
        }

        // 查询分组为岗位的权限，type=POSITION
        String positionSql = "SELECT DP.ID DATAPERMISSIONID,DP.ACTIVITY_CODE ACTIVITYCODE,DP.DATA_PERMISSION_TYPE DATAPERMISSIONTYPE,DP.GROUP_POWER_FLAG GROUPPOWERFLAG,DP.POSITION_POWER_FLAG POSITIONPOWERFLAG,DP.UNLIMITED_POWER UNLIMITEDPOWER,DP.TYPE_ID TYPEID,PS.NAME TYPENAME"
                + " FROM BASE_DATAPERMISSION DP,BASE_POSITION PS WHERE DP.FLOW_KEY=?  "
                + " AND DP.DATA_PERMISSION_TYPE='POSITION' AND DP.TYPE_ID=PS.ID AND PS.VALID=1";
        List<Map<String, Object>> positionList = jdbcTemplate.queryForList(positionSql, flowKey);
        for (Map<String, Object> map : positionList) {
            Long dpId = (map.get("DATAPERMISSIONID") != null) ? Long.valueOf(map.get("DATAPERMISSIONID").toString()) : -1L;
            String activeCode = (map.get("ACTIVITYCODE") != null) ? map.get("ACTIVITYCODE").toString() : "";
            Map<Long, Map<String, Object>> positionMap = activeMap.get(activeCode);
            if (null == positionMap) {
                positionMap = new HashMap<Long, Map<String, Object>>();
            }
            positionMap.put(dpId, map);
            activeMap.put(activeCode, positionMap);
        }
        // 查询分组为角色的权限，type=ROLE
        String roleSql = "SELECT DP.ID DATAPERMISSIONID,DP.ACTIVITY_CODE ACTIVITYCODE,DP.DATA_PERMISSION_TYPE DATAPERMISSIONTYPE,DP.GROUP_POWER_FLAG GROUPPOWERFLAG,DP.POSITION_POWER_FLAG POSITIONPOWERFLAG,DP.UNLIMITED_POWER UNLIMITEDPOWER,DP.TYPE_ID TYPEID,R.NAME TYPENAME  "
                + " FROM BASE_DATAPERMISSION DP,BASE_ROLE R WHERE DP.FLOW_KEY=?  "
                + " AND DP.DATA_PERMISSION_TYPE='ROLE' AND DP.TYPE_ID=R.ID AND R.VALID=1 ";
        List<Map<String, Object>> roleList = jdbcTemplate.queryForList(roleSql, flowKey);

        for (Map<String, Object> map : roleList) {
            Long dpId = (map.get("DATAPERMISSIONID") != null) ? Long.valueOf(map.get("DATAPERMISSIONID").toString()) : -1L;
            String activeCode = (map.get("ACTIVITYCODE") != null) ? map.get("ACTIVITYCODE").toString() : "";
            Map<Long, Map<String, Object>> roleMap = activeMap.get(activeCode);
            if (null == roleMap) {
                roleMap = new HashMap<Long, Map<String, Object>>();
            }
            roleMap.put(dpId, map);
            activeMap.put(activeCode, roleMap);
        }
        // 查询分组为指定人员的权限，type=USER，关联BASE_DATAPERMISSIONSTAFF中的staffid
        String assignStaffSql = "select DP.ID DATAPERMISSIONID,DP.ACTIVITY_CODE ACTIVITYCODE,DP.DATA_PERMISSION_TYPE DATAPERMISSIONTYPE,DP.GROUP_POWER_FLAG GROUPPOWERFLAG,DP.POSITION_POWER_FLAG POSITIONPOWERFLAG,DP.UNLIMITED_POWER UNLIMITEDPOWER,S.ID STAFFID,S.NAME STAFFNAME"
                +

                " from base_dataPermission dp,BASE_DATAPERMISSIONSTAFF ds,BASE_STAFF s "
                +

                " where DP.FLOW_KEY=? and  DP.ID=DS.DATAPERMISSION_ID "
                + " and DS.STAFF_ID=S.ID and S.VALID=1  and S.WORK_STATUS!='STAFFSTATUSE_NATURE/STAFFSTATUTS_03'";

        List<Map<String, Object>> assignStaffList = jdbcTemplate.queryForList(assignStaffSql, flowKey);
        for (Map<String, Object> map : assignStaffList) {
            Long dpId = (map.get("DATAPERMISSIONID") != null) ? Long.valueOf(map.get("DATAPERMISSIONID").toString()) : -1L;
            String activeCode = (map.get("ACTIVITYCODE") != null) ? map.get("ACTIVITYCODE").toString() : "";
            Map<Long, List<Map<String, Object>>> assignsMap = assignStaffMap.get(activeCode);
            if (null == assignsMap) {
                assignsMap = new HashMap<Long, List<Map<String, Object>>>();
            }
            List<Map<String, Object>> list = assignsMap.get(dpId);
            if (null == list) {
                list = new ArrayList<Map<String, Object>>();
            }
            list.add(map);
            assignsMap.put(dpId, list);
            assignStaffMap.put(activeCode, assignsMap);
            // assignMap.put(dpId, map);
        }
        // 查询分组为指定岗位的权限，type=USER，关联BASE_DATAPERMISSIONPOSITION中的POSITIONCODE
        String assignPositionSql = "select DP.ID DATAPERMISSIONID,DP.ACTIVITY_CODE ACTIVITYCODE,DP.DATA_PERMISSION_TYPE DATAPERMISSIONTYPE,DP.GROUP_POWER_FLAG GROUPPOWERFLAG,DP.POSITION_POWER_FLAG POSITIONPOWERFLAG,DP.UNLIMITED_POWER UNLIMITEDPOWER,p.ID POSITIONID,p.NAME POSITIONNAME,dps.INCLUDE_LOWER INCLUDELOWER "
                + " from base_dataPermission dp,BASE_DATAPMSPOSITION dps,Base_position p "
                + " where DP.FLOW_KEY=?  and DP.ID=dps.DATAPERMISSION_ID  and dps.POSITION_ID=p.ID and p.VALID=1 ";
        List<Map<String, Object>> assignPositionList = jdbcTemplate.queryForList(assignPositionSql, flowKey);

        for (Map<String, Object> map : assignPositionList) {
            Long dpId = (map.get("DATAPERMISSIONID") != null) ? Long.valueOf(map.get("DATAPERMISSIONID").toString()) : -1L;
            String activeCode = (map.get("ACTIVITYCODE") != null) ? map.get("ACTIVITYCODE").toString() : "";
            Map<Long, List<Map<String, Object>>> assignPsMap = assignPositionMap.get(activeCode);
            if (null == assignPsMap) {
                assignPsMap = new HashMap<Long, List<Map<String, Object>>>();
            }
            List<Map<String, Object>> list = assignPsMap.get(dpId);
            if (null == list) {
                list = new ArrayList<Map<String, Object>>();
            }
            list.add(map);
            assignPsMap.put(dpId, list);
            assignPositionMap.put(activeCode, assignPsMap);

        }
        // 组织xml
        StringBuffer powerXml = new StringBuffer();
        powerXml.append("<taskPower>");
        for (Map.Entry<String, Map<Long, Map<String, Object>>> map : activeMap.entrySet()) {
            StringBuffer activeXml = new StringBuffer();
            String activeCode = map.getKey();
            Map<Long, Map<String, Object>> dpMap = map.getValue();
            activeXml.append("<active taskName='" + activeCode + "'>");
            for (Map.Entry<Long, Map<String, Object>> powerMap : dpMap.entrySet()) {
                Long pdId = powerMap.getKey();
                Map<String, Object> fieldMap = powerMap.getValue();

                String typeId = (fieldMap.get("TYPEID") != null) ? fieldMap.get("TYPEID").toString() : "";
                String typeName = (fieldMap.get("TYPENAME") != null) ? fieldMap.get("TYPENAME").toString() : "";
                String type = (fieldMap.get("DATAPERMISSIONTYPE") != null) ? fieldMap.get("DATAPERMISSIONTYPE").toString() : "";

                String staffName = (fieldMap.get("STAFFNAME") != null) ? fieldMap.get("STAFFNAME").toString() : "";
                if ("USER".equals(type)) {
                    typeName = staffName;
                }
                String positionPowerFlag = (fieldMap.get("POSITIONPOWERFLAG") != null) ? (fieldMap.get("POSITIONPOWERFLAG").toString()
                        .equals("1")) ? "true" : "false" : "";
                String groupPowerFlag = (fieldMap.get("GROUPPOWERFLAG") != null) ? (fieldMap.get("GROUPPOWERFLAG").toString()
                        .equals("1")) ? "true" : "false" : "";
/*				if (fieldMap.get("GROUPPOWERFLAG") != null) {
					groupPowerFlag = fieldMap.get("GROUPPOWERFLAG").toString();
				}*/
                // String
                // groupPowerFlag=(fieldMap.get("GROUPPOWERFLAG")!=null)?(fieldMap.get("GROUPPOWERFLAG").toString().equals("1"))?"true":"false":"false";
                String unlimitedPower = (fieldMap.get("UNLIMITEDPOWER") != null) ? (fieldMap.get("UNLIMITEDPOWER").toString().equals("1")) ? "true"
                        : "false"
                        : "false";
                activeXml.append("<power ");
                activeXml.append(" userType='" + type + "'");
                activeXml.append(" typeId='" + typeId + "'");
                activeXml.append(" typeName='" + typeName + "'");
                // if("USER".equals(type)){
                // activeXml.append(" staffName='"+staffName+"'");
                // }
                activeXml.append(" positionPower='" + positionPowerFlag + "'");
                activeXml.append(" groupPower='" + groupPowerFlag + "'");
                activeXml.append(" unLimitPower='" + unlimitedPower + "'");

                // 获取该权限对应的指定岗位
                Map<Long, List<Map<String, Object>>> activePowerPsMap = assignPositionMap.get(activeCode);
                if (null != activePowerPsMap) {
                    List<Map<String, Object>> assignPositions = activePowerPsMap.get(pdId);
                    if (null != assignPositions) {
                        activeXml.append(" assignPositionPower='true' ");
                        String assignPositionStr = "";
                        for (Map<String, Object> assignPositionObj : assignPositions) {
                            String assignPositionName = (assignPositionObj.get("POSITIONNAME") != null) ? assignPositionObj.get(
                                    "POSITIONNAME").toString() : "";
                            String assignPositionId = (assignPositionObj.get("POSITIONID") != null) ? assignPositionObj.get("POSITIONID")
                                    .toString() : "";
                            String includelower = (assignPositionObj.get("INCLUDELOWER") != null && assignPositionObj.get("INCLUDELOWER")
                                    .toString().equals("1")) ? "true" : "false";
                            // String
                            // assignPositionLayRec=(assignPositionObj.get("LAY_REC")!=null)?assignPositionObj.get("LAY_REC").toString():"";
                            assignPositionStr += ";" + assignPositionId + "," + assignPositionName + "," + includelower;
                        }
                        if (!"".equals(assignPositionStr)) {
                            assignPositionStr = assignPositionStr.substring(1);
                        }
                        activeXml.append(" assignPositions='" + assignPositionStr + "' ");
                    }
                }

                // 获取该权限对应的指定人员
                Map<Long, List<Map<String, Object>>> activePowerStaffMap = assignStaffMap.get(activeCode);
                if (null != activePowerStaffMap) {
                    List<Map<String, Object>> assignStaffs = activePowerStaffMap.get(pdId);
                    if (null != assignStaffs) {
                        activeXml.append(" assignStaffPower='true' ");
                        String assignStaffStr = "";
                        for (Map<String, Object> assignStaffObj : assignStaffs) {
                            String assignStaffName = (assignStaffObj.get("STAFFNAME") != null) ? assignStaffObj.get("STAFFNAME").toString()
                                    : "";
                            String assignStaffId = (assignStaffObj.get("STAFFID") != null) ? assignStaffObj.get("STAFFID").toString() : "";
                            assignStaffStr += ";" + assignStaffId + "," + assignStaffName;
                        }
                        if (!"".equals(assignStaffStr)) {
                            assignStaffStr = assignStaffStr.substring(1);
                        }
                        activeXml.append(" assignStaffs='" + assignStaffStr + "' ");
                    }
                }
                activeXml.append("/>");
            }
            activeXml.append("</active>");
            powerXml.append(activeXml.toString());
        }

        powerXml.append("</taskPower>");
        return powerXml.toString();
    }

    @Autowired
    private DataPermissionDaoImpl dataPermissionDao;
    @Autowired
    private DataPmsPositionDaoImpl dataPermissionPositionDao;
    @Autowired
    private DataPermissionStaffDaoImpl dataPermissionStaffDao;
    @Autowired
    private CustomDataPermissionService customDataPermissionService;
    @Autowired
    private IPermissionApiService permissionApiService;

    @Override
    public void updateMenuUserInfo(String flowKey, String flowVersion, String activeArr, String operatePowers, String entityCode, Long menuId) {
        // 先删除该流程的所有权限
        String[] taskArr = activeArr.split(",");
        String tasks = "";

        for (String task : taskArr) {
            tasks += ",\'" + task + "\'";
        }
        if (!"".equals(tasks)) {
            tasks = tasks.substring(1);
        }
      /*  String delSql = "delete from BASE_DATAPERMISSION where FLOW_KEY=? and ACTIVITY_CODE in (" + tasks + ")";
        String delAssignStaffSql = "delete from BASE_DATAPERMISSIONSTAFF where DATAPERMISSION_ID in(select id from BASE_DATAPERMISSION where FLOW_KEY=? and ACTIVITY_CODE in ("
                + tasks + "))";
        String delAssignPosiontSql = "delete from BASE_DATAPMSPOSITION where DATAPERMISSION_ID in(select id from BASE_DATAPERMISSION where FLOW_KEY=? and ACTIVITY_CODE in ("
                + tasks + "))";*/

        String delSql =  "delete from rbac_flow_permission where FLOW_KEY=? and ACTIVITY_CODE in (" + tasks + ")";
        String delAssignStaffSql = "delete from rbac_flow_permission_staff where flowpermission_id in(select id from rbac_flow_permission where FLOW_KEY=? and ACTIVITY_CODE in ("
                + tasks + "))";
        String delAssignPosiontSql = "delete from rbac_flow_permission_position where flowpermission_id in(select id from rbac_flow_permission where FLOW_KEY=? and ACTIVITY_CODE in ("
                + tasks + "))";
        jdbcTemplate.update(delAssignStaffSql, flowKey);
        jdbcTemplate.update(delAssignPosiontSql, flowKey);
        jdbcTemplate.update(delSql, flowKey);

       /* String[] taskArray = activeArr.split(",");
        List<String> taskList = Arrays.asList(taskArray);

        //删除permission
        supfusionPermissionService.deleteFlowPermission(flowKey, taskList);
        //删除permissionStaff
        supfusionPermissionService.deleteFlowPermissionStaff(flowKey, taskList);
        //删除permissionPosition
        supfusionPermissionService.deleteFlowPermissionPosition(flowKey, taskList);*/

        if (null != operatePowers && !"".equals(operatePowers)) {

            String[] operatePowerArr = operatePowers.split(";");
            int dataCount = 0;
            for (int j = 0; j < operatePowerArr.length; j++) {
                dataCount++;
                String operatePower = operatePowerArr[j];
                Object[] powerArr = operatePower.split("\\$\\$");
                DataPermission dataPermission = new DataPermission();
                String activityCode = powerArr[0].toString();// 活动code
                dataPermission.setActivityCode(activityCode);
                dataPermission.setFlowKey(flowKey);
                // dataPermission.setFlowVersion(flowVersion);
                dataPermission.setEntityCode(entityCode);
                if (activityCode.toUpperCase().startsWith("START")) {
                    dataPermission.setPurviewState(2);
                } else {
                    dataPermission.setPurviewState(1);
                }
                dataPermission.setPurviewDistribution(3);
                // dataPermission.setPurviewState(1);
                // 存在分配权限
                if (powerArr != null && powerArr.length > 3) {
                    String powerType = powerArr[1].toString();// 权限类型
                    dataPermission.setDataPermissionType(DataPermissionType.valueOf(powerType.trim()));
                    Long typeId = Long.valueOf(powerArr[2].toString());// 类型编码
                    dataPermission.setTypeId(typeId);
                    Boolean positionPowerFlag = Boolean.valueOf(powerArr[3].toString());// 岗位权限
                    dataPermission.setPositionPowerFlag(positionPowerFlag);
                    Boolean groupPowerFlag = Boolean.valueOf(powerArr[4].toString());// 组权限
                    dataPermission.setGroupPowerFlag(groupPowerFlag);
                    dataPermission.setAssignPosFlag(false);
                    dataPermission.setAssignStaffFlag(false);
                 /*   //判断一下是否有指定岗位
                    if (powerArr.length >= 6) {
                        dataPermission.setAssignPosFlag(true);
                    }
                    //判断一下是否有指定人员
                    if (powerArr.length >= 7) {
                        dataPermission.setAssignStaffFlag(true);
                    }*/
//                    dataPermissionDao.save(dataPermission);

                  /*  //保存permission
                    customDataPermissionService.savePermission(dataPermission);*/

                    if (powerArr.length >= 6) {
                        String assignPositionStr = powerArr[5].toString();// 指定岗位
                        if (!"false".equals(assignPositionStr)) {
                            dataPermission.setAssignPosFlag(true);
                        }
                    }
                    if (powerArr.length >= 7) {
                        String assignStaffStr = powerArr[6].toString();// 指定人员
                        if (!"false".equals(assignStaffStr)) {
                            dataPermission.setAssignStaffFlag(true);
                        }
                    }

                    //保存permission
                    customDataPermissionService.savePermission(dataPermission);
                    Long dataPermissionId = dataPermission.getId();

                    if (powerArr.length >= 6) {
                        String assignPositionStr = powerArr[5].toString();// 指定岗位
                        if (!"false".equals(assignPositionStr)) {
                            String[] assignPositions = assignPositionStr.split("\\|\\|");
                            for (int m = 0; m < assignPositions.length; m++) {
                                dataCount++;
                                DataPmsPosition dataPmsPosition = new DataPmsPosition();
                                String assignPosition = assignPositions[m];
                                String[] arr = assignPosition.split(",");

                                dataPmsPosition.setDataPermissionId(dataPermissionId);
                                dataPmsPosition.setPositionId(Long.valueOf(arr[0]));
                                dataPmsPosition.setVersion(0);
                                if (arr != null && arr.length > 2) {
                                    dataPmsPosition.setIncludeLower(Boolean.valueOf(arr[2]));
                                } else {
                                    dataPmsPosition.setIncludeLower(false);
                                }
                                //保存permissionPosition
                                customDataPermissionService.savePermissionPosition(dataPmsPosition);

                            }
                        }
                    }

                    if (powerArr.length >= 7) {
                        String assignStaffStr = powerArr[6].toString();// 指定人员
                        if (!"false".equals(assignStaffStr)) {
                            String[] assignStaffArr = assignStaffStr.split("\\|\\|");
                            for (int n = 0; n < assignStaffArr.length; n++) {
                                dataCount++;
                                DataPermissionStaff dataPermissionStaff = new DataPermissionStaff();
                                String assignStaff = assignStaffArr[n];
                                String[] assignStaffIdS = assignStaff.split(",");
                                dataPermissionStaff.setStaffId(Long.valueOf(assignStaffIdS[0]));
                                dataPermissionStaff.setDataPermissionId(dataPermissionId);
                                //保存permissionStaff
                                customDataPermissionService.savePermissionStaff(dataPermissionStaff);
                            }
                        }
                    }

                } else {
                    String powerType = powerArr[1].toString();// 权限类型
                    dataPermission.setDataPermissionType(DataPermissionType.valueOf(powerType.trim()));
                    Long typeId = Long.valueOf(powerArr[2].toString());// 类型编码
                    dataPermission.setTypeId(typeId);
                    dataPermission.setUnlimitedPower(true);
                    dataPermission.setAssignPosFlag(false);
                    dataPermission.setAssignStaffFlag(false);
                    //保存permission
                    customDataPermissionService.savePermission(dataPermission);

                }

                // 防止缓存太大导致溢出
                if (dataCount > 49) {
                    dataPermissionDao.flush();
                    dataPermissionPositionDao.flush();
                    dataPermissionStaffDao.flush();
                    dataCount = 0;
                }

            }
        }
        //刷新redis
        if (!ObjectUtils.isEmpty(menuId)){
            MenuInfo menuInfo = menuInfoDao.get(menuId);
            String moduleCode = menuInfo.getModuleCode();
            new Thread(()-> permissionApiService.refreshRedis(Collections.singletonList(moduleCode.split("_")[0]))).start();
        }
    }

    @Autowired
    private MenuOperateService menuOperateService;
    @Autowired
    private MenuUserDealInfoService menuUserDealInfoService;

    @Override
    public void saveWorkFlowPermissionChanges(Long deploymentId, String updatePowerString)
    {
        try {
            User user = getCurrentUser();
            String language = getUserLanguage();
            if (updatePowerString == null || updatePowerString.length() == 0) {
                return;
            }
            if (updatePowerString.endsWith(";")) {
                updatePowerString = updatePowerString.substring(0, updatePowerString.length() - 1);
            }
            String[] permissionArray = updatePowerString.split(";");//每一条
            List<Map<String, Object>> permissionDatas = new ArrayList<Map<String, Object>>();
            for (String permissionStr : permissionArray) {
                String[] permission = permissionStr.split(":", 2);
                Map<String, Object> map = new HashMap<String, Object>();
                //判断处理对象
                String[] p = permission[1].split("\\$\\$");
                String operateCode = p[0];
                MenuOperate menuOperate = menuOperateService.getMenuOperate(deploymentId, operateCode);
                if (menuOperate == null) {//可能活动已经被删除了
                    continue;
                }
                map.put("menuOperate", menuOperate);

                String type = p[1];
                if ("ROLE".equals(type)) {
                    map.put("type", 0);
                } else if ("USER".equals(type)) {
                    map.put("type", 1);
                } else if ("POSITION".equals(type)) {
                    map.put("type", 2);
                } else if ("DEPTMENT".equals(type)) {
                    map.put("type", 3);
                }
                map.put("id", p[2]);
                //判断处理类型
                if ("add".equals(permission[0])) {
                    map.put("dealType", 2);
                    if ("ROLE".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.role.addrolepermission"));
                    } else if ("USER".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.userPermission.addUserPower"));
                    } else if ("POSITION".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.role.addpositionpermission"));
                    } else if ("DEPTMENT".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.role.adddepartmentpermission"));
                    }
                } else if ("update".equals(permission[0])) {
                    map.put("dealType", 1);
                    String[] q = null;
                    String[] permissonUpdate = null;
                    if (permission[1].indexOf("from:$$") > -1) {
                        q = permission[1].split("from:\\$\\$");
                    } else {
                        q = permission[1].split("from:");
                    }

                    if (q[1].indexOf(" to:$$") > -1) {
                        permissonUpdate = q[1].split(" to:\\$\\$", -1);
                    } else {
                        permissonUpdate = q[1].split(" to:", -1);
                    }
                    String head = "";
                    if ("ROLE".equals(type)) {
                        head = i18nService.getI18nValue("foundation.role.oldrolepermission");
                    } else if ("USER".equals(type)) {
                        head = i18nService.getI18nValue("foundation.role.olduserpermission");
                    } else if ("POSITION".equals(type)) {
                        head = i18nService.getI18nValue("foundation.role.oldpositionpermission");
                    } else if ("DEPARTMENT".equals(type)) {
                        head = i18nService.getI18nValue("foundation.role.olddepartmentpermission");
                    }
                    map.put("dealInfo", head + "：<br/>" + permissionDealInfo(permissonUpdate[0]) + "<br/>" + i18nService.getI18nValue("foundation.role.modifyInformation") + "：<br/>" + permissionDealInfo(permissonUpdate[1]));
                } else if ("delete".equals(permission[0])) {
                    map.put("dealType", 0);
                    if ("ROLE".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.role.deleterolepermission"));
                    } else if ("USER".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.role.deleteuserpermission"));
                    } else if ("POSITION".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.role.deletepositionpermission"));
                    } else if ("DEPTMENT".equals(type)) {
                        map.put("dealInfo", i18nService.getI18nValue("foundation.role.deletedepartmentpermission"));
                    }
                }

                permissionDatas.add(map);
            }
            menuUserDealInfoService.savePermissionChangesLoggerForWorkFlow(permissionDatas);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
//            throw new Exception("记录权限更改日志失败");
        }
    }


    public String permissionDealInfo(String permissionStr) {
        User user = getCurrentUser();
        String language = getUserLanguage();
        if ("".equals(permissionStr)) {
            return i18nService.getI18nValue("foundation.rolepermission.noRestrict");
        }
        String str = "";
        String ap[] = permissionStr.split("\\$\\$");
        if ("true".equals(ap[0])) {
            str += i18nService.getI18nValue("foundation.rolepermission.posRestrict") + " ：<br/>";
        }
        if ("1".equals(ap[1])) {
            str += i18nService.getI18nValue("foundation.permissionQuery.groupRestriction.onlyLeader") + "<br/>";
        } else if ("2".equals(ap[1])) {
            str += i18nService.getI18nValue("foundation.permissionQuery.groupRestriction.all") + "<br/>";
        }
        if (!"false".equals(ap[2])) {
            str += i18nService.getI18nValue("foundation.rolepermission.assignPos") + " :";
            String positionPower[] = ap[2].split("\\|\\|");
            for (String pp : positionPower) {
                String p[] = pp.split(",");
                if (p.length > 2) {
                    str += p[1];
                    if ("true".equals(p[2])) {
                        str += "(" + i18nService.getI18nValue("foundation.staff.havingDownPosition") + ")";
                    }
                }
                str += "<br/>";
            }
        }
        if (!"false".equals(ap[3])) {
            str += i18nService.getI18nValue("foundation.userpermission.assignStaff") + " :";
            String userPower[] = ap[2].split("\\|\\|");
            for (String pp : userPower) {
                String p[] = pp.split(",");
                if (p.length > 1) {
                    str += p[1];
                    str += "<br/>";
                }
            }
        }
        return str;
    }

    private Set<Map<String, Object>> getFlowStartFromJpa(String entityCode, Long userId) {
        // 查询分组为用户的权限，type=USER
        Set<Map<String, Object>> list = new HashSet<>();
        String userSql = "SELECT distinct DP.ACTIVITY_CODE ACTIVITYCODE ,DP.FLOW_KEY FLOWKEY,dm.DESCRIPTION DESCRIPTION,dm.NAME NAME FROM BASE_DATAPERMISSION DP,WF_DEPLOYMENT dm WHERE dp.ENTITY_CODE=? and DM.PROCESS_KEY=DP.FLOW_KEY "
                + "  and DM.IS_CURRENT_VERSION=1  AND DP.DATA_PERMISSION_TYPE='USER' and DP.PURVIEW_STATE=2 and DP.TYPE_ID=?";
        String positionSql = "SELECT distinct DP.ACTIVITY_CODE ACTIVITYCODE ,DP.FLOW_KEY FLOWKEY,dm.DESCRIPTION DESCRIPTION ,dm.NAME NAME"
                + " FROM BASE_DATAPERMISSION DP,BASE_POSITION p,BASE_POSITIONWORK PW,BASE_USERINFO US,WF_DEPLOYMENT dm  WHERE dp.ENTITY_CODE=? "
                + " AND DM.PROCESS_KEY=DP.FLOW_KEY  and DM.IS_CURRENT_VERSION=1  and DP.DATA_PERMISSION_TYPE='POSITION' and DP.PURVIEW_STATE=2 and DP.TYPE_ID=P.ID  "
                + " AND P.VALID=1 AND  PW.POSITION_ID=P.ID AND PW.STAFF_ID=US.STAFF_ID AND PW.VALID=1 AND US.ID=? ";
        String roleSql = " SELECT distinct DP.ACTIVITY_CODE ACTIVITYCODE ,DP.FLOW_KEY FLOWKEY,dm.DESCRIPTION DESCRIPTION ,dm.NAME NAME"
                + " FROM BASE_DATAPERMISSION DP,BASE_ROLE R,BASE_ROLEUSER RU,BASE_USERINFO US ,WF_DEPLOYMENT dm WHERE dp.ENTITY_CODE=? "
                + " AND DM.PROCESS_KEY=DP.FLOW_KEY  and DM.IS_CURRENT_VERSION=1  and DP.DATA_PERMISSION_TYPE='ROLE' and DP.PURVIEW_STATE=2 and DP.TYPE_ID=R.ID "
                + " AND R.VALID=1 AND  RU.ROLE_ID=R.ID AND RU.VALID=1 AND RU.USER_ID=?";
        String deptSql = "  SELECT distinct DP.ACTIVITY_CODE ACTIVITYCODE ,DP.FLOW_KEY FLOWKEY,dm.DESCRIPTION DESCRIPTION ,dm.NAME NAME"
                + " FROM BASE_DATAPERMISSION DP,BASE_DEPARTMENT D,BASE_DEPARTMENTWORK DW,BASE_USERINFO US ,WF_DEPLOYMENT dm WHERE dp.ENTITY_CODE=? "
                + " AND DM.PROCESS_KEY=DP.FLOW_KEY  and DM.IS_CURRENT_VERSION=1  and  DP.DATA_PERMISSION_TYPE='DEPTMENT' and DP.PURVIEW_STATE=2 and DP.TYPE_ID=D.ID "
                + " AND D.VALID=1 AND DW.DEPARTMENT_ID=D.ID AND DW.STAFF_ID=US.STAFF_ID AND DW.VALID=1 AND US.ID=?";

        List<Map<String, Object>> userList = jdbcTemplate.queryForList(userSql, entityCode, userId);
        if (userList.size() > 0) {
            for (Map<String, Object> map : userList) {
                list.add(map);
            }
        }
        List<Map<String, Object>> positionList = jdbcTemplate.queryForList(positionSql, entityCode, userId);
        if (positionList.size() > 0) {
            for (Map<String, Object> map : positionList) {
                list.add(map);
            }
        }
        List<Map<String, Object>> roleList = jdbcTemplate.queryForList(roleSql, entityCode, userId);
        if (roleList.size() > 0) {
            for (Map<String, Object> map : roleList) {
                list.add(map);
            }
        }
        List<Map<String, Object>> deptList = jdbcTemplate.queryForList(deptSql, entityCode, userId);
        if (deptList.size() > 0) {
            for (Map<String, Object> map : deptList) {
                list.add(map);
            }
        }
        return list;
    }

    @Autowired
    private PositionService positionService;
    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Long> getPowerUserList(Long userId, String activityCode, String flowKey, String flowVersion,
                                       Long positionId, String groupIds, Boolean crossCompanyFlag) {
        Position position = positionService.load(positionId);
        Long cid = position.getCompany().getId();
        String positionLayRec = position.getLayRec();
        List<Position> parents = positionService.getAllParents(positionLayRec);
        String positionIds = "";
        Map<Long, Long> positionMap = new HashMap<Long, Long>();
        positionMap.put(position.getId(), position.getId());
        for (Position ps : parents) {
            positionIds += "," + ps.getId().toString();
            Long keyCode = ps.getId();
            positionMap.put(keyCode, ps.getId());
        }

        if (!"".equals(positionIds)) {
            positionIds = positionIds.substring(1);
        }
        List<Long> userList = new LinkedList<Long>();
        List<DataPermission> dataPermissions = dataPermissionDao.findByCriteria(Restrictions.eq("flowKey", flowKey),
                Restrictions.eq("activityCode", activityCode));
        if (dataPermissions.size() < 1) {
            return Collections.EMPTY_LIST;
        }

        // 仅岗位限制
        if (!"".equals(positionIds)) {
            List<Long> positionUserList = positionPower(flowKey, activityCode, positionId, positionIds, userId);
            if (positionUserList.size() > 0) {
                userList.addAll(positionUserList);
            }
        }
        // 仅组限制
        if (!"".equals(groupIds)) {
            List<Long> groupUserList = groupPower(flowKey, activityCode, groupIds);
            if (groupUserList.size() > 0) {
                userList.addAll(groupUserList);
            }

            // 岗位限制 and组限制
            List<Long> positionAndGroupUserList = positionAndGroupPower(flowKey, activityCode, positionId, positionIds,
                    userId, groupIds);
            if (positionAndGroupUserList.size() > 0) {
                userList.addAll(positionAndGroupUserList);
            }
        }
        // 无限制
        List<Long> unlismitedUserList = unlimitedPower(flowKey, activityCode, crossCompanyFlag, cid);
        if (unlismitedUserList.size() > 0) {
            userList.addAll(unlismitedUserList);
        }
        // 指定人员
        List<Long> assignStaffUserList = assignStaffpower(flowKey, activityCode, userId, crossCompanyFlag, cid);
        if (assignStaffUserList.size() > 0) {
            userList.addAll(assignStaffUserList);
        }
        // 指定岗位
        List<Long> assignPositionUserList = assignPositionpower(flowKey, activityCode, positionId, positionMap,
                crossCompanyFlag, cid);
        if (assignPositionUserList.size() > 0) {
            userList.addAll(assignPositionUserList);
        }

        // 去掉重复的username
        Map<Long, Long> userMap = new HashMap<Long, Long>();
        if (userList.size() > 0) {
            for (Long name : userList) {
                userMap.put(name, name);
            }
        }
        userList.clear();
        for (Map.Entry<Long, Long> singleName : userMap.entrySet()) {
            userList.add(singleName.getKey());
        }

        return userList;
    }

    private List<Long> positionPower(String flowKey, String activityCode, Long positionId, String positionIds,
                                     Long userId) {
        List<Long> userList = new ArrayList<Long>();

        // 判断岗位权限下有哪些分组（人员，角色、部门、岗位、组）
        String checkSql = "select count(dp.DATA_PERMISSION_TYPE) count ,dp.DATA_PERMISSION_TYPE TYPE from "
                + "BASE_DATAPERMISSION dp where dp.ACTIVITY_CODE=? and dp.POSITION_POWER_FLAG=1 and dp.FLOW_KEY=? "
                + " and (dp.GROUP_POWER_FLAG=0 or dp.GROUP_POWER_FLAG is null) group by dp.DATA_PERMISSION_TYPE";
        List<Map<String, Object>> checkType = jdbcTemplate.queryForList(checkSql,
                new Object[]{activityCode, flowKey}, new int[]{Types.VARCHAR, Types.VARCHAR});
        Map<String, Integer> pemissionTypeMap = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> map : checkType) {
            String type = map.get("TYPE").toString();
            int count = Integer.valueOf(map.get("count").toString());
            pemissionTypeMap.put(type, count);
        }
        // 判断是否有人员分组
        if (pemissionTypeMap.get("USER") != null && pemissionTypeMap.get("USER") > 0) {
            String userSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_POSITION ps ,BASE_POSITIONWORK pw,BASE_STAFF sf ,BASE_USERINFO us where  "
                    + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='USER'"
                    + " and dp.TYPE_ID=us.ID" + " and dp.POSITION_POWER_FLAG=1" + " and (dp.GROUP_POWER_FLAG =0 "
                    + " or dp.GROUP_POWER_FLAG is null)" + " and pw.POSITION_ID=ps.ID" + " and pw.STAFF_ID=sf.ID"
                    + " and us.STAFF_ID=sf.ID" + " and us.VALID=1" + " and ps.VALID=1" + " and pw.VALID=1"
                    + " and sf.VALID=1" + " and (ps.ID in (" + positionIds + ") or ps.ID=" + positionId
                    + " and us.ID=?)";

            List<Long> userStaffIds = jdbcTemplate.queryForList(userSql, new Object[]{activityCode, flowKey, userId},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (userStaffIds.size() > 0) {
                userList.addAll(userStaffIds);
            }
        }
        // 是否有岗位分组
        if (pemissionTypeMap.get("POSITION") != null && pemissionTypeMap.get("POSITION") > 0) {
            String positionSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_POSITION ps,BASE_POSITIONWORK pw,BASE_USERINFO us where "
                    + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='POSITION'"
                    + " and dp.TYPE_ID=ps.ID" + " and dp.POSITION_POWER_FLAG=1" + " and (dp.GROUP_POWER_FLAG =0 "
                    + " or dp.GROUP_POWER_FLAG is null)" + " and pw.STAFF_ID=us.STAFF_ID" + " and pw.POSITION_ID=ps.ID"
                    + " and us.VALID=1" + " and ps.VALID=1" + " and pw.VALID=1 and (ps.ID in (" + positionIds
                    + ") or ps.ID=" + positionId + " and us.ID=?)";
            List<Long> psStaffIds = jdbcTemplate.queryForList(positionSql,
                    new Object[]{activityCode, flowKey, userId},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (psStaffIds.size() > 0) {
                userList.addAll(psStaffIds);
            }
        }
        // 判断是否有部门分组
        if (pemissionTypeMap.get("DEPTMENT") != null && pemissionTypeMap.get("DEPTMENT") > 0) {
            String departmentSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_POSITION ps,BASE_DEPARTMENT dt,BASE_POSITIONWORK pw,BASE_USERINFO us where "
                    + "  dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='DEPTMENT'"
                    + " and dp.TYPE_ID=dt.ID" + " and dp.POSITION_POWER_FLAG=1" + " and (dp.GROUP_POWER_FLAG =0 "
                    + " or dp.GROUP_POWER_FLAG is null)" + " and ps.DEPARTMENT_ID=dt.ID"
                    + " and pw.STAFF_ID=us.STAFF_ID" + " and pw.POSITION_ID=ps.ID" + " and us.VALID=1"
                    + " and dt.VALID=1  and ps.VALID=1" + " and pw.VALID=1" + " and (ps.ID in (" + positionIds
                    + ") or ps.ID=" + positionId + " and us.ID=?)";
            List<Long> dtStaffIds = jdbcTemplate.queryForList(departmentSql,
                    new Object[]{activityCode, flowKey, userId},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (dtStaffIds.size() > 0) {
                userList.addAll(dtStaffIds);
            }
        }
        // 判断是否有角色分组
        if (pemissionTypeMap.get("ROLE") != null && pemissionTypeMap.get("ROLE") > 0) {
            String roleSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_POSITION ps,BASE_POSITIONWORK pw,BASE_STAFF sf,BASE_ROLE rl ,BASE_ROLEUSER ru,BASE_USERINFO us where "
                    + "  dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='ROLE'"
                    + " and dp.TYPE_ID=rl.ID" + " and dp.POSITION_POWER_FLAG=1" + " and (dp.GROUP_POWER_FLAG =0 "
                    + " or dp.GROUP_POWER_FLAG is null)" + " and rl.ID=ru.ROLE_ID" + " and ru.USER_ID=US.ID"
                    + " and sf.ID=us.STAFF_ID" + " and ps.ID=pw.POSITION_ID" + " and pw.STAFF_ID=us.STAFF_ID"
                    + " and rl.VALID=1" + " and ps.VALID=1" + " and pw.VALID=1" + " and ru.VALID=1"
                    + " and us.VALID=1 and (ps.ID in (" + positionIds + ") or ps.ID=" + positionId + " and us.ID=?)";

            List<Long> rlStaffIds = jdbcTemplate.queryForList(roleSql, new Object[]{activityCode, flowKey, userId},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (rlStaffIds.size() > 0) {
                userList.addAll(rlStaffIds);
            }

        }
        // 判断是否存在组分组
        if (pemissionTypeMap.get("WORKGROUP") != null && pemissionTypeMap.get("WORKGROUP") > 0) {
            String groupSql = "select distinct us.ID from BASE_DATAPERMISSION DP,BASE_GROUPMEMBER GM,BASE_USERINFO US,BASE_POSITION ps,BASE_POSITIONWORK pw "
                    + " WHERE DP.TYPE_ID=GM.GROUP_ID and PS.ID=PW.POSITION_ID and PW.STAFF_ID=US.STAFF_ID "
                    + " AND GM.MEMBER_ID=US.STAFF_ID AND DP.DATA_PERMISSION_TYPE='WORKGROUP' "
                    + " AND  DP.POSITION_POWER_FLAG=1 " + " AND (dp.GROUP_POWER_FLAG =0 "
                    + " or dp.GROUP_POWER_FLAG is null)" + " AND dp.ACTIVITY_CODE= ? " + " AND dp.FLOW_KEY= ? "
                    + " AND GM.VALID=1 " + " AND US.VALID=1 " + " AND PS.VALID=1 " + " AND PW.VALID=1 "
                    + " AND (ps.ID in (" + positionIds + ") or ps.ID=" + positionId + " and us.ID=?)";

            List<Long> groupStaffIds = jdbcTemplate.queryForList(groupSql,
                    new Object[]{activityCode, flowKey, userId},
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (groupStaffIds.size() > 0) {
                userList.addAll(groupStaffIds);
            }

        }

        return userList;
    }

    private List<Long> groupPower(String flowKey, String activityCode, String groupIds) {
        List<Long> userList = new ArrayList<Long>();
        // 判断组限制-组长,组员限制有哪些分组（人员，角色、部门、岗位、组）
        String checkGroupPowerSql = "select count(dp.DATA_PERMISSION_TYPE) COUNT,dp.DATA_PERMISSION_TYPE TYPE,DP.GROUP_POWER_FLAG FLAG from "
                + "BASE_DATAPERMISSION dp where " + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? "
                + " and (DP.POSITION_POWER_FLAG=0 or DP.POSITION_POWER_FLAG is null ) "
                + " and (DP.GROUP_POWER_FLAG=1 or DP.GROUP_POWER_FLAG=2) group by dp.DATA_PERMISSION_TYPE,DP.GROUP_POWER_FLAG";
        List<Map<String, Object>> checkGroupPowerUserType = jdbcTemplate.queryForList(checkGroupPowerSql,
                new Object[]{activityCode, flowKey}, new int[]{Types.VARCHAR, Types.VARCHAR});
        Map<Integer, Map<String, Integer>> groupMap = new LinkedHashMap<Integer, Map<String, Integer>>();
        // Map<String,Integer> groupPowerMap=new LinkedHashMap<String, Integer>();
        for (Map<String, Object> map : checkGroupPowerUserType) {
            String type = map.get("TYPE").toString();
            int count = Integer.valueOf(map.get("COUNT").toString());
            int flag = Integer.valueOf(map.get("FLAG").toString());
            Map<String, Integer> groupMapType = groupMap.get(flag);
            if (null == groupMapType) {
                groupMapType = new LinkedHashMap<String, Integer>();
            }
            groupMapType.put(type, count);
            groupMap.put(flag, groupMapType);

        }
        for (Map.Entry<Integer, Map<String, Integer>> entry : groupMap.entrySet()) {
            int groupFlagFlag = entry.getKey();
            Map<String, Integer> map = entry.getValue();
            // 判断是否有人员分组---1=组长限制，2=组员限制
            if (map.get("USER") != null && map.get("USER") > 0) {
                String userSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_USERINFO us,BASE_GROUPMEMBER gm where  "
                        + " dp.ACTIVITY_CODE=?  and dp.FLOW_KEY=?  and dp.DATA_PERMISSION_TYPE='USER' and gm.VALID=1"
                        + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag + ""
                        + " and (DP.POSITION_POWER_FLAG=0 or DP.POSITION_POWER_FLAG is null )  and gm.MEMBER_ID=us.STAFF_ID "
                        + " and dp.TYPE_ID=us.ID  and us.VALID=1  and gm.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    userSql += " and gm.type=0";
                }
                List<Long> groupUserStaffs = jdbcTemplate.queryForList(userSql, new Object[]{activityCode, flowKey},
                        new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
                if (groupUserStaffs.size() > 0) {
                    userList.addAll(groupUserStaffs);
                }
            }
            // 是否有岗位分组
            if (map.get("POSITION") != null && map.get("POSITION") > 0) {
                String positionSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_POSITION ps,BASE_POSITIONWORK pw,BASE_USERINFO us ,BASE_GROUPMEMBER gm where "
                        + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='POSITION'"
                        + " and dp.TYPE_ID=ps.ID" + " and pw.STAFF_ID=us.STAFF_ID" + " and pw.POSITION_ID=ps.ID"
                        + " and us.VALID=1" + " and pw.VALID=1" + " and gm.VALID=1"
                        + " and (DP.POSITION_POWER_FLAG=0 or DP.POSITION_POWER_FLAG is null ) "
                        + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag + "" + " and gm.MEMBER_ID=us.STAFF_ID "
                        + " and ps.VALID=1" + " and gm.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    positionSql += " and gm.type=0";
                }
                List<Long> psStaffIds = jdbcTemplate.queryForList(positionSql, new Object[]{activityCode, flowKey},
                        new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
                if (psStaffIds.size() > 0) {
                    userList.addAll(psStaffIds);
                }
            }
            // 判断是否有部门分组
            if (map.get("DEPTMENT") != null && map.get("DEPTMENT") > 0) {
                String departmentSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_DEPARTMENTWORK ptwk,BASE_DEPARTMENT dt,BASE_USERINFO us,BASE_GROUPMEMBER gm where "
                        + "  dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='DEPTMENT'"
                        + " and dp.TYPE_ID=dt.ID"
                        + " and (DP.POSITION_POWER_FLAG=0 or DP.POSITION_POWER_FLAG is null ) "
                        + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag + "" + " and ptwk.DEPARTMENT_ID=dt.ID"
                        + " and ptwk.STAFF_ID=us.STAFF_ID" + " and gm.MEMBER_ID=us.STAFF_ID " + " and gm.VALID=1"
                        + " and us.VALID=1" + " and dt.VALID=1   and ptwk.VALID=1  and gm.GROUP_ID in (" + groupIds
                        + ") ";
                if (groupFlagFlag == 1) {
                    departmentSql += " and gm.type=0";
                }
                List<Long> dtStaffIds = jdbcTemplate.queryForList(departmentSql, new Object[]{activityCode, flowKey},
                        new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
                if (dtStaffIds.size() > 0) {
                    userList.addAll(dtStaffIds);
                }
            }
            // 判断是否有角色分组
            if (map.get("ROLE") != null && map.get("ROLE") > 0) {
                String roleSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_ROLE rl ,BASE_ROLEUSER ru,BASE_USERINFO us ,BASE_GROUPMEMBER gm where "
                        + "  dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='ROLE'"
                        + " and dp.TYPE_ID=rl.ID"
                        + " and (DP.POSITION_POWER_FLAG=0 or DP.POSITION_POWER_FLAG is null ) "
                        + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag + "" + " and rl.ID=ru.ROLE_ID"
                        + " and ru.USER_ID=US.ID" + " and gm.MEMBER_ID=us.STAFF_ID " + " and rl.VALID=1"
                        + " and ru.VALID=1" + " and us.VALID=1 " + " and gm.VALID=1 " + " and gm.GROUP_ID in ("
                        + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    roleSql += " and gm.type=0";
                }
                List<Long> rlStaffIds = jdbcTemplate.queryForList(roleSql, new Object[]{activityCode, flowKey},
                        new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
                if (rlStaffIds.size() > 0) {
                    userList.addAll(rlStaffIds);
                }
            }
            // 判断是否存在组分组
            if (map.get("WORKGROUP") != null && map.get("WORKGROUP") > 0) {
                String groupSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_GROUPMEMBER gm ,BASE_USERINFO us,BASE_GROUPMEMBER gmer where "
                        + "  dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='WORKGROUP'"
                        + " and dp.TYPE_ID=gm.GROUP_ID"
                        + " and (DP.POSITION_POWER_FLAG=0 or DP.POSITION_POWER_FLAG is null ) "
                        + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag + "" + " and gm.MEMBER_ID=us.STAFF_ID"
                        + " and gmer.MEMBER_ID= us.STAFF_ID" + " and gm.VALID=1" + " and us.VALID=1"
                        + " and gmer.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    groupSql += " and gm.type=0";
                }
                List<Long> groupStaffIds = jdbcTemplate.queryForList(groupSql, new Object[]{activityCode, flowKey},
                        new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
                if (groupStaffIds.size() > 0) {
                    userList.addAll(groupStaffIds);
                }
            }
        }

        return userList;
    }

    private List<Long> positionAndGroupPower(String flowKey, String activityCode, Long positionId, String positionIds,
                                             Long userId, String groupIds) {
        List<Long> userList = new ArrayList<Long>();
        // Map<Integer, Map<String,Integer>> groupMap=new LinkedHashMap<Integer,
        // Map<String,Integer>>();
        // 判断是否同时分了岗位限制和组限制
        String checkPGSql = "select count(dp.DATA_PERMISSION_TYPE) COUNT,dp.DATA_PERMISSION_TYPE TYPE,DP.GROUP_POWER_FLAG FLAG from "
                + "BASE_DATAPERMISSION dp where dp.ACTIVITY_CODE=? and dp.FLOW_KEY=?  and dp.POSITION_POWER_FLAG=1"
                + " and (DP.GROUP_POWER_FLAG=1 or DP.GROUP_POWER_FLAG=2) group by dp.DATA_PERMISSION_TYPE,DP.GROUP_POWER_FLAG";

        List<Map<String, Object>> checkPGType = jdbcTemplate.queryForList(checkPGSql,
                new Object[]{activityCode, flowKey}, new int[]{Types.VARCHAR, Types.VARCHAR});

        Map<Integer, Map<String, Integer>> gpMap = new LinkedHashMap<Integer, Map<String, Integer>>();

        for (Map<String, Object> map : checkPGType) {
            String type = map.get("TYPE").toString();
            int count = Integer.valueOf(map.get("COUNT").toString());
            int flag = Integer.valueOf(map.get("FLAG").toString());
            Map<String, Integer> groupMapType = gpMap.get(flag);
            if (null == groupMapType) {
                groupMapType = new LinkedHashMap<String, Integer>();
            }
            groupMapType.put(type, count);
            gpMap.put(flag, groupMapType);

        }
        for (Map.Entry<Integer, Map<String, Integer>> entry : gpMap.entrySet()) {
            int groupFlagFlag = entry.getKey();
            Map<String, Integer> map = entry.getValue();
            // 判断是否有人员分组---1=组长限制，2=组员限制 （同时分了组与岗位）
            if (map.get("USER") != null && map.get("USER") > 0) {
                String userSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_USERINFO us,BASE_GROUPMEMBER gm,"
                        + " BASE_POSITION ps ,BASE_POSITIONWORK  pw,BASE_STAFF sf where   dp.ACTIVITY_CODE=?  and dp.FLOW_KEY=? "
                        + " and dp.DATA_PERMISSION_TYPE='USER' and gm.VALID=1 and pw.POSITION_ID=ps.ID"
                        + " and pw.STAFF_ID=sf.ID and us.STAFF_ID=sf.ID and dp.POSITION_POWER_FLAG=1"
                        + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag
                        + " and gm.MEMBER_ID=us.STAFF_ID  and dp.TYPE_ID=us.ID "
                        + " and us.VALID=1  and ps.VALID=1 and pw.VALID=1 and sf.VALID=1 and (ps.ID in (" + positionIds
                        + ") or ps.ID=" + positionId + " and us.ID=?) and gm.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    userSql += " and gm.type=0";
                }
                List<Long> groupUserStaffs = jdbcTemplate.queryForList(userSql,
                        new Object[]{activityCode, flowKey, userId},
                        new int[]{Types.VARCHAR, Types.VARCHAR, Types.BIGINT}, Long.class);
                if (groupUserStaffs.size() > 0) {
                    userList.addAll(groupUserStaffs);
                }
            }
            // 是否有岗位分组（同时分了组与岗位）
            if (map.get("POSITION") != null && map.get("POSITION") > 0) {
                String positionSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_POSITION ps,BASE_POSITIONWORK pw,BASE_USERINFO us ,BASE_GROUPMEMBER gm where "
                        + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='POSITION'"
                        + " and dp.TYPE_ID=ps.ID" + " and pw.STAFF_ID=us.STAFF_ID" + " and pw.POSITION_ID=ps.ID"
                        + " and us.VALID=1" + " and pw.VALID=1" + " and gm.VALID=1" + " and dp.POSITION_POWER_FLAG=1"
                        + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag + "" + " and gm.MEMBER_ID=us.STAFF_ID "
                        + " and ps.VALID=1" + " and (ps.ID in (" + positionIds + ") or ps.ID=" + positionId
                        + " and us.ID=?) and gm.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    positionSql += " and gm.type=0";
                }
                List<Long> psStaffIds = jdbcTemplate.queryForList(positionSql,
                        new Object[]{activityCode, flowKey, userId},
                        new int[]{Types.VARCHAR, Types.VARCHAR, Types.BIGINT}, Long.class);
                if (psStaffIds.size() > 0) {
                    userList.addAll(psStaffIds);
                }
            }
            // 判断是否有部门分组（同时分了组与岗位）
            if (map.get("DEPTMENT") != null && map.get("DEPTMENT") > 0) {
                String departmentSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_DEPARTMENTWORK ptwk,"
                        + " BASE_DEPARTMENT dt,BASE_USERINFO us,BASE_GROUPMEMBER gm, "
                        + " BASE_POSITION ps ,BASE_POSITIONWORK  pw,BASE_STAFF sf where    dp.ACTIVITY_CODE=? "
                        + " and dp.FLOW_KEY=?  and dp.DATA_PERMISSION_TYPE='DEPTMENT' and dp.TYPE_ID=dt.ID"
                        + " and dp.POSITION_POWER_FLAG=1 and dp.GROUP_POWER_FLAG=" + groupFlagFlag + ""
                        + " and ptwk.DEPARTMENT_ID=dt.ID" + " and ptwk.STAFF_ID=us.STAFF_ID"
                        + " and gm.MEMBER_ID=us.STAFF_ID " + " and pw.POSITION_ID=ps.ID" + " and pw.STAFF_ID=sf.ID"
                        + " and us.STAFF_ID=sf.ID" + " and gm.VALID=1" + " and us.VALID=1" + " and dt.VALID=1  "
                        + " and ptwk.VALID=1 " + " and ps.VALID=1" + " and pw.VALID=1" + " and sf.VALID=1"
                        + " and (ps.ID in (" + positionIds + ") or ps.ID=" + positionId
                        + " and us.ID=?) and gm.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    departmentSql += " and gm.type=0";
                }
                List<Long> dtStaffIds = jdbcTemplate.queryForList(departmentSql,
                        new Object[]{activityCode, flowKey, userId},
                        new int[]{Types.VARCHAR, Types.VARCHAR, Types.BIGINT}, Long.class);
                if (dtStaffIds.size() > 0) {
                    userList.addAll(dtStaffIds);
                }
            }
            // 判断是否有角色分组（同时分了组与岗位）
            if (map.get("ROLE") != null && map.get("ROLE") > 0) {
                String roleSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_ROLE rl ,BASE_ROLEUSER ru,BASE_USERINFO us ,BASE_GROUPMEMBER gm "
                        + " ,BASE_POSITION ps ,BASE_POSITIONWORK  pw,BASE_STAFF sf where " + "  dp.ACTIVITY_CODE=? "
                        + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='ROLE'" + " and dp.TYPE_ID=rl.ID"
                        + " and dp.POSITION_POWER_FLAG=1" + " and pw.STAFF_ID=us.STAFF_ID"
                        + " and gm.MEMBER_ID=us.STAFF_ID " + " and pw.POSITION_ID=ps.ID" + " and dp.GROUP_POWER_FLAG="
                        + groupFlagFlag + "" + " and rl.ID=ru.ROLE_ID" + " and ru.USER_ID=US.ID"
                        + " and gm.MEMBER_ID=us.STAFF_ID " + " and rl.VALID=1" + " and ru.VALID=1" + " and us.VALID=1 "
                        + " and ps.VALID=1" + " and pw.VALID=1" + " and sf.VALID=1" + " and gm.VALID=1"
                        + " and (ps.ID in (" + positionIds + ") or ps.ID=" + positionId + " and us.ID=?)"
                        + " and gm.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    roleSql += " and gm.type=0";
                }
                List<Long> rlStaffIds = jdbcTemplate.queryForList(roleSql,
                        new Object[]{activityCode, flowKey, userId},
                        new int[]{Types.VARCHAR, Types.VARCHAR, Types.BIGINT}, Long.class);
                if (rlStaffIds.size() > 0) {
                    userList.addAll(rlStaffIds);
                }
            }
            // 判断是否存在组分组（同时分了组与岗位）
            if (map.get("WORKGROUP") != null && map.get("WORKGROUP") > 0) {
                String groupSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_GROUPMEMBER gm ,BASE_USERINFO us,BASE_GROUPMEMBER gmer,BASE_POSITION ps ,BASE_POSITIONWORK  pw,BASE_STAFF sf where "
                        + "  dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='WORKGROUP'"
                        + " and dp.TYPE_ID=gm.GROUP_ID" + " and dp.POSITION_POWER_FLAG=1"
                        + " and pw.STAFF_ID=us.STAFF_ID" + " and gm.MEMBER_ID=us.STAFF_ID "
                        + " and pw.POSITION_ID=ps.ID" + " and dp.GROUP_POWER_FLAG=" + groupFlagFlag + ""
                        + " and gm.MEMBER_ID=us.STAFF_ID" + " and gmer.MEMBER_ID= us.STAFF_ID" + " and gm.VALID=1"
                        + " and gmer.VALID=1" + " and us.VALID=1" + " and ps.VALID=1" + " and pw.VALID=1"
                        + " and sf.VALID=1" + " and (ps.ID in (" + positionIds + ") or ps.ID=" + positionId
                        + " and us.ID=?) and gmer.GROUP_ID in (" + groupIds + ") ";
                if (groupFlagFlag == 1) {
                    groupSql += " and gm.type=0";
                }
                List<Long> groupStaffIds = jdbcTemplate.queryForList(groupSql,
                        new Object[]{activityCode, flowKey, userId},
                        new int[]{Types.VARCHAR, Types.VARCHAR, Types.BIGINT}, Long.class);
                if (groupStaffIds.size() > 0) {
                    userList.addAll(groupStaffIds);
                }
            }
        }
        // /岗位与组----结束
        return userList;
    }

    private List<Long> unlimitedPower(String flowKey, String activityCode, Boolean crossCompanyFlag, Long cid) {
        List<Long> userList = new ArrayList<Long>();

        // 判断无限制下有哪些分组（人员，角色、部门、岗位、组）
        String checkUnlimitedPowerSql = "select count(dp.DATA_PERMISSION_TYPE) count,dp.DATA_PERMISSION_TYPE TYPE from "
                + "BASE_DATAPERMISSION dp where dp.ACTIVITY_CODE=? and dp.FLOW_KEY=? "
                + " and DP.UNLIMITED_POWER=1 group by dp.DATA_PERMISSION_TYPE";
        List<Map<String, Object>> checkPowerUserType = jdbcTemplate.queryForList(checkUnlimitedPowerSql,
                new Object[]{activityCode, flowKey}, new int[]{Types.VARCHAR, Types.VARCHAR});
        Map<String, Integer> unlimitedPowerMap = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> map : checkPowerUserType) {
            String type = map.get("TYPE").toString();
            int count = Integer.valueOf(map.get("count").toString());
            unlimitedPowerMap.put(type, count);
        }
        // 判断是否有人员分组
        if (unlimitedPowerMap.get("USER") != null && unlimitedPowerMap.get("USER") > 0) {
            String userSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_USERINFO us where   dp.ACTIVITY_CODE=? "
                    + " and dp.FLOW_KEY=?  and dp.DATA_PERMISSION_TYPE='USER' and dp.UNLIMITED_POWER=1"
                    + " and dp.TYPE_ID=us.ID and us.VALID=1 ";
            if (!crossCompanyFlag) {
                userSql += " and us.staff_id in (select cs.staff_id from base_companystaff cs where cs.valid=1 and cs.cid="
                        + cid + ")";

            }
            List<Long> unlimitedStaffs = jdbcTemplate.queryForList(userSql, new Object[]{activityCode, flowKey},
                    new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (unlimitedStaffs.size() > 0) {
                userList.addAll(unlimitedStaffs);
            }
        }
        // 是否有岗位分组
        if (unlimitedPowerMap.get("POSITION") != null && unlimitedPowerMap.get("POSITION") > 0) {
            String positionSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_POSITION ps,BASE_POSITIONWORK pw,BASE_USERINFO us where "
                    + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='POSITION'"
                    + " and dp.TYPE_ID=ps.ID" + " and pw.STAFF_ID=us.STAFF_ID" + " and pw.POSITION_ID=ps.ID"
                    + " and us.VALID=1" + " and pw.VALID=1" + " and dp.UNLIMITED_POWER=1 and ps.VALID=1 ";
            if (!crossCompanyFlag) {
                positionSql += " and ps.cid=" + cid;

            }
            List<Long> psStaffIds = jdbcTemplate.queryForList(positionSql, new Object[]{activityCode, flowKey},
                    new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (psStaffIds.size() > 0) {
                userList.addAll(psStaffIds);
            }

        }
        // 判断是否有部门分组
        if (unlimitedPowerMap.get("DEPTMENT") != null && unlimitedPowerMap.get("DEPTMENT") > 0) {
            String departmentSql = "select distinct us.ID from BASE_DATAPERMISSION dp,BASE_DEPARTMENTWORK ptwk,BASE_DEPARTMENT dt,BASE_USERINFO us where "
                    + "  dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.DATA_PERMISSION_TYPE='DEPTMENT'"
                    + " and dp.TYPE_ID=dt.ID" + " and dp.UNLIMITED_POWER=1" + " and ptwk.DEPARTMENT_ID=dt.ID"
                    + " and ptwk.STAFF_ID=us.STAFF_ID" + " and us.VALID=1" + " and dt.VALID=1  and ptwk.VALID=1";
            if (!crossCompanyFlag) {
                departmentSql += " and dt.cid=" + cid;

            }
            List<Long> dtStaffIds = jdbcTemplate.queryForList(departmentSql, new Object[]{activityCode, flowKey},
                    new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (dtStaffIds.size() > 0) {
                userList.addAll(dtStaffIds);
            }
        }
        // 判断是否有角色分组
        if (unlimitedPowerMap.get("ROLE") != null && unlimitedPowerMap.get("ROLE") > 0) {
            String roleSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_ROLE rl ,BASE_ROLEUSER ru,BASE_USERINFO us where "
                    + "  dp.ACTIVITY_CODE=?  and dp.FLOW_KEY=?  and dp.DATA_PERMISSION_TYPE='ROLE' and dp.TYPE_ID=rl.ID"
                    + " and dp.UNLIMITED_POWER=1 and rl.ID=ru.ROLE_ID and ru.USER_ID=US.ID and rl.VALID=1"
                    + " and ru.VALID=1 and us.VALID=1";
            if (!crossCompanyFlag) {
                roleSql += " and rl.cid=" + cid;

            }
            List<Long> rlStaffIds = jdbcTemplate.queryForList(roleSql, new Object[]{activityCode, flowKey},
                    new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (rlStaffIds.size() > 0) {
                userList.addAll(rlStaffIds);
            }
        }
        // 判断是否存在组分组
        if (unlimitedPowerMap.get("WORKGROUP") != null && unlimitedPowerMap.get("WORKGROUP") > 0) {
            String groupSql = " select distinct us.ID from BASE_DATAPERMISSION dp,BASE_GROUPMEMBER gm ,BASE_USERINFO us where "
                    + "  dp.ACTIVITY_CODE=?  and dp.FLOW_KEY=?  and dp.DATA_PERMISSION_TYPE='WORKGROUP'"
                    + " and dp.TYPE_ID=gm.GROUP_ID and dp.UNLIMITED_POWER=1 and gm.MEMBER_ID=us.STAFF_ID and gm.VALID=1"
                    + " and us.VALID=1";
            if (!crossCompanyFlag) {
                groupSql += " and gm.GROUP_ID in (select g.id from BASE_GROUP g where g.cid=" + cid + " and g.valid=1)";
            }
            List<Long> groupStaffIds = jdbcTemplate.queryForList(groupSql, new Object[]{activityCode, flowKey},
                    new int[]{Types.VARCHAR, Types.VARCHAR}, Long.class);
            if (groupStaffIds.size() > 0) {
                userList.addAll(groupStaffIds);
            }
        }
        return userList;
    }

    private List<Long> assignStaffpower(String flowKey, String activityCode, Long userId, Boolean crossCompanyFlag,
                                        Long cid) {
        List<Long> userList = new ArrayList<Long>();
        // 判断角色分组下哪些有指定人员限制

        String assignStaffByRoleSql = " select distinct aus.ID from BASE_DATAPERMISSION dp,BASE_DATAPERMISSIONSTAFF ds,BASE_ROLEUSER ru,BASE_USERINFO us,BASE_USERINFO aus where "
                + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=?  " + " and dp.ID=ds.DATAPERMISSION_ID "
                + " and dp.DATA_PERMISSION_TYPE='ROLE' " + " and DS.STAFF_ID=us.STAFF_ID " + " and ru.USER_ID=AUS.ID "
                + "  and RU.ROLE_ID=dp.TYPE_ID   and RU.VALID=1  and us.VALID=1  and AUS.VALID=1  and us.ID=? ";
        if (!crossCompanyFlag) {
            assignStaffByRoleSql = " select distinct aus.ID from BASE_DATAPERMISSION dp,BASE_DATAPERMISSIONSTAFF ds,BASE_ROLEUSER ru,BASE_ROLE ro,BASE_USERINFO us,BASE_USERINFO aus where "
                    + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=?  " + " and dp.ID=ds.DATAPERMISSION_ID "
                    + " and dp.DATA_PERMISSION_TYPE='ROLE' " + " and DS.STAFF_ID=us.STAFF_ID "
                    + " and ru.USER_ID=AUS.ID " + "  and RU.ROLE_ID=dp.TYPE_ID  " + "  and RU.ROLE_ID=ro.ID  "
                    + "  and ro.CID = " + cid + " and RU.VALID=1 " + " and us.VALID=1  and AUS.VALID=1  and us.ID=? ";
        }
        List<Long> assingStaffByRoleUsers = jdbcTemplate.queryForList(assignStaffByRoleSql,
                new Object[]{activityCode, flowKey, userId},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.INTEGER}, Long.class);
        if (assingStaffByRoleUsers.size() > 0) {
            userList.addAll(assingStaffByRoleUsers);
        }
        // 判断用户分组下哪些有指定人员限制
        String assignStaffByUserSql = "select distinct aus.ID from BASE_DATAPERMISSION dp,BASE_DATAPERMISSIONSTAFF ds,BASE_USERINFO us, BASE_USERINFO aus where "
                + " dp.ACTIVITY_CODE=? " + " and dp.FLOW_KEY=? " + " and dp.ID=ds.DATAPERMISSION_ID"
                + " and dp.DATA_PERMISSION_TYPE='USER'" + " and DS.STAFF_ID=us.STAFF_ID" + " and aus.ID=dp.TYPE_ID "
                + " and us.VALID=1" + " and us.ID=?";
        if (!crossCompanyFlag) {
            assignStaffByUserSql += " and us.STAFF_ID in (select cs.staff_id from base_companystaff cs where cs.valid=1 and cs.cid="
                    + cid + ")";

        }
        List<Long> assingStaffUsers = jdbcTemplate.queryForList(assignStaffByUserSql,
                new Object[]{activityCode, flowKey, userId},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.INTEGER}, Long.class);
        if (assingStaffUsers.size() > 0) {
            userList.addAll(assingStaffUsers);
        }

        return userList;
    }

    private List<Long> assignPositionpower(String flowKey, String activityCode, Long positionId,
                                           Map<Long, Long> positionMap, Boolean crossCompanyFlag, Long cid) {
        List<Long> userList = new ArrayList<Long>();
        // 判断USER分组下哪些有指定岗位限制
        String assignPositionSql = "select distinct ds.INCLUDE_LOWER INCLUDELOWER,ds.POSITION_ID POSITIONID,"
                + " dp.TYPE_ID,us.ID UESRID from BASE_DATAPERMISSION dp, BASE_DATAPMSPOSITION ds,BASE_USERINFO us  where "
                + " dp.ACTIVITY_CODE=?  and dp.FLOW_KEY=?  and dp.TYPE_ID=us.ID  and us.VALID=1 "
                + " and dp.DATA_PERMISSION_TYPE='USER' and dp.ID=ds.DATAPERMISSION_ID";
        if (!crossCompanyFlag) {
            assignPositionSql += " and us.STAFF_ID in (select cs.staff_id from base_companystaff cs where cs.valid=1 and cs.cid="
                    + cid + ")";

        }
        List<Map<String, Object>> assingPositionInfos = jdbcTemplate.queryForList(assignPositionSql,
                new Object[]{activityCode, flowKey}, new int[]{Types.VARCHAR, Types.VARCHAR});
        if (assingPositionInfos.size() > 0) {
            Long checkPositionId = positionId;
            for (Map<String, Object> map : assingPositionInfos) {
                Long pId = Long.decode(map.get("POSITIONID").toString());
                Long assignUserId = Long.valueOf(map.get("UESRID").toString());
                Boolean includelower = (((Number) map.get("INCLUDELOWER")).intValue()) == 1;
                if (includelower) {
                    if (positionMap.get(pId) != null) {
                        userList.add(assignUserId);
                    }
                } else {
                    if (pId.equals(checkPositionId)) {
                        userList.add(assignUserId);
                    }
                }
            }
        }
        // 判断ROLE分组下哪些有指定岗位限制
        String assignPositionByRoleSql = " select distinct ds.INCLUDE_LOWER INCLUDELOWER,ds.POSITION_ID POSITIONID, "
                + " dp.TYPE_ID,us.ID USERID from BASE_DATAPERMISSION dp, BASE_DATAPMSPOSITION ds,BASE_ROLEUSER ru,BASE_USERINFO us  where "
                + " dp.ACTIVITY_CODE=?  and dp.FLOW_KEY=?  and dp.DATA_PERMISSION_TYPE='ROLE' "
                + " and dp.ID=ds.DATAPERMISSION_ID  and dp.TYPE_ID=RU.ROLE_ID  and RU.USER_ID=US.ID  and us.VALID=1  "
                + " and RU.VALID=1 ";
        if (!crossCompanyFlag) {
            assignPositionByRoleSql = " select distinct ds.INCLUDE_LOWER INCLUDELOWER,ds.POSITION_ID POSITIONID, "
                    + " dp.TYPE_ID,us.ID USERID from BASE_DATAPERMISSION dp, BASE_DATAPMSPOSITION ds,BASE_ROLE ro,BASE_ROLEUSER ru,BASE_USERINFO us  where "
                    + " dp.ACTIVITY_CODE=?  and dp.FLOW_KEY=?  and dp.DATA_PERMISSION_TYPE='ROLE' "
                    + " and dp.ID=ds.DATAPERMISSION_ID  and dp.TYPE_ID=RU.ROLE_ID  and RU.USER_ID=US.ID "
                    + " and ro.ID=RU.ROLE_ID  and ro.CID= " + cid + " and us.VALID=1   and RU.VALID=1 ";

        }
        List<Map<String, Object>> assingPositionInfosByRole = jdbcTemplate.queryForList(assignPositionByRoleSql,
                new Object[]{activityCode, flowKey}, new int[]{Types.VARCHAR, Types.VARCHAR});
        if (assingPositionInfosByRole.size() > 0) {
            Long checkPositionId = positionId;
            for (Map<String, Object> map : assingPositionInfosByRole) {
                Long pId = Long.decode(map.get("POSITIONID").toString());
                Long assignUserId = Long.valueOf(map.get("USERID").toString());
                Boolean includelower = (((Number) map.get("INCLUDELOWER")).intValue()) == 1;
                if (includelower) {
                    if (positionMap.get(pId) != null) {
                        userList.add(assignUserId);
                    }
                } else {
                    if (pId.equals(checkPositionId)) {
                        userList.add(assignUserId);
                    }
                }
            }
        }

        return userList;
    }
}
