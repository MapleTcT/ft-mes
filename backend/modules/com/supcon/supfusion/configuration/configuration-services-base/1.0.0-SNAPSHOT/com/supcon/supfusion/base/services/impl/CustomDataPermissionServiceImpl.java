package com.supcon.supfusion.base.services.impl;

import com.supcon.supfusion.base.dao.FlowPermissionDaoImpl;
import com.supcon.supfusion.base.dao.FlowPermissionPositionDaoImpl;
import com.supcon.supfusion.base.dao.FlowPermissionStaffDaoImpl;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.CustomDataPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service("customDataPermissionService")
public class CustomDataPermissionServiceImpl implements CustomDataPermissionService {

    @Autowired
    private FlowPermissionDaoImpl flowPermissionDao;
    @Autowired
    private FlowPermissionStaffDaoImpl flowPermissionStaffDao;
    @Autowired
    private FlowPermissionPositionDaoImpl flowPermissionPositionDao;

    @Override
    public void savePermission(DataPermission dataPermission) {
       /* FlowPermissionDTO flowPermissionDTO = new FlowPermissionDTO();
        BeanUtils.copyProperties(dataPermission, flowPermissionDTO);

        DataPermissionType dataPermissionType = dataPermission.getDataPermissionType();
        switch (Enums.getField(dataPermissionType).getName()) {
            case "USER":
                flowPermissionDTO.setFlowPermissionType(FlowPermissionType.USER);
                break;
            case "WORKGROUP":
                flowPermissionDTO.setFlowPermissionType(FlowPermissionType.WORKGROUP);
                break;
            case "DEPTMENT":
                flowPermissionDTO.setFlowPermissionType(FlowPermissionType.DEPTMENT);
                break;
            case "ROLE":
                flowPermissionDTO.setFlowPermissionType(FlowPermissionType.ROLE);
                break;
            case "POSITION":
                flowPermissionDTO.setFlowPermissionType(FlowPermissionType.POSITION);
                break;
        }

        FlowPermissionDTO result = supfusionPermissionService.saveFlowPermission(flowPermissionDTO);

        if (null != result) {
            dataPermission.setId(result.getId());
        }*/

        FlowPermissionPO flowPermissionPO = null;
        if (null == dataPermission.getId()) {
            //long id = idGenerator.getNextId("rbac_menuinfo", "SEQ_ID");
            flowPermissionPO = new FlowPermissionPO();
            //menuInfoPO.setId(id);
        } else {
            flowPermissionPO = flowPermissionDao.load(dataPermission.getId());
        }
//        FlowPermissionPO flowPermissionPO = new FlowPermissionPO();
//        flowPermissionPO.setId(dataPermission.getId());
        if (!ObjectUtils.isEmpty(dataPermission.getVersion())) {
            flowPermissionPO.setVersion(dataPermission.getVersion());
        } else {
            flowPermissionPO.setVersion(0);
        }
        flowPermissionPO.setEntityCode(dataPermission.getEntityCode());
        flowPermissionPO.setPurviewDistribution(dataPermission.getPurviewDistribution());
        flowPermissionPO.setPurviewState(dataPermission.getPurviewState());
        flowPermissionPO.setMemo(dataPermission.getMemo());
        if (dataPermission.getUnlimitedPower() == null) {
            flowPermissionPO.setUnlimitedPower(false);
        } else {
            flowPermissionPO.setUnlimitedPower(dataPermission.getUnlimitedPower());
        }
        if (dataPermission.getGroupPowerFlag() == null) {
            flowPermissionPO.setGroupPowerFlag(false);
        } else {
            flowPermissionPO.setGroupPowerFlag(dataPermission.getGroupPowerFlag());
        }
        if (dataPermission.getAssignStaffFlag() == null) {
            flowPermissionPO.setAssignStaffFlag(false);
        } else {
            flowPermissionPO.setAssignStaffFlag(dataPermission.getAssignStaffFlag());
        }
        if (dataPermission.getAssignPosFlag() == null) {
            flowPermissionPO.setAssignPosFlag(false);
        } else {
            flowPermissionPO.setAssignPosFlag(dataPermission.getAssignPosFlag());
        }
        if (dataPermission.getPositionPowerFlag() == null) {
            flowPermissionPO.setPositionPowerFlag(false);
        } else {
            flowPermissionPO.setPositionPowerFlag(dataPermission.getPositionPowerFlag());
        }
        flowPermissionPO.setDataPermissionType(dataPermission.getDataPermissionType());
        flowPermissionPO.setTypeId(dataPermission.getTypeId());
        flowPermissionPO.setActivityCode(dataPermission.getActivityCode());
        flowPermissionPO.setFlowVersion(dataPermission.getFlowVersion());
        flowPermissionPO.setFlowKey(dataPermission.getFlowKey());
        flowPermissionDao.save(flowPermissionPO);
        dataPermission.setId(flowPermissionPO.getId());
    }

    @Override
    public void savePermissionStaff(DataPermissionStaff dataPermissionStaff) {
       /* FlowPermissionStaffDTO flowPermissionStaffDTO = new FlowPermissionStaffDTO();
        BeanUtils.copyProperties(dataPermissionStaff, flowPermissionStaffDTO);
        flowPermissionStaffDTO.setDatapermissionId(dataPermissionStaff.getDataPermission().getId());

        supfusionPermissionService.saveFlowPermissionStaff(flowPermissionStaffDTO);*/

        FlowPermissionStaffPO flowPermissionStaffPO = null;
        if (null == dataPermissionStaff.getId()) {
            flowPermissionStaffPO = new FlowPermissionStaffPO();
        } else {
            flowPermissionStaffPO = flowPermissionStaffDao.load(dataPermissionStaff.getId());
        }
        flowPermissionStaffPO.setVersion(dataPermissionStaff.getVersion());
        flowPermissionStaffPO.setStaffId(dataPermissionStaff.getStaffId());
        if (null != dataPermissionStaff.getDataPermissionId()) {
            flowPermissionStaffPO.setDatapermissionId(dataPermissionStaff.getDataPermissionId());
        }
        flowPermissionStaffDao.save(flowPermissionStaffPO);

    }

    @Override
    public void savePermissionPosition(DataPmsPosition dataPmsPosition) {
      /*  FlowPermissionPositionDTO permissionPositionDTO = new FlowPermissionPositionDTO();
        BeanUtils.copyProperties(dataPmsPosition, permissionPositionDTO);
        permissionPositionDTO.setDatapermissionId(dataPmsPosition.getDataPermission().getId());
        permissionPositionDTO.setDatapermissionId(dataPmsPosition.getDataPermission().getId());

        supfusionPermissionService.saveFlowPermissionPosition(permissionPositionDTO);*/

        FlowPmsPositionPO flowPmsPositionPO = null;
        if (null == dataPmsPosition.getId()) {
            flowPmsPositionPO = new FlowPmsPositionPO();
        } else {
            flowPmsPositionPO = flowPermissionPositionDao.load(dataPmsPosition.getId());
        }
        flowPmsPositionPO.setIncludeLower(dataPmsPosition.getIncludeLower());
        flowPmsPositionPO.setVersion(dataPmsPosition.getVersion());
        flowPmsPositionPO.setPositionId(dataPmsPosition.getPositionId());
        if (null != dataPmsPosition.getDataPermissionId()) {
            flowPmsPositionPO.setDatapermissionId(dataPmsPosition.getDataPermissionId());
        }
        flowPermissionPositionDao.save(flowPmsPositionPO);
    }


}
