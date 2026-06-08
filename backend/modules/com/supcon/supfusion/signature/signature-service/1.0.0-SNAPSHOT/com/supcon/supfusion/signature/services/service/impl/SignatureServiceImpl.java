package com.supcon.supfusion.signature.services.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.supcon.supfusion.signature.dao.dto.ModelTableDto;
import com.supcon.supfusion.signature.dao.entity.WfDeployment;
import com.supcon.supfusion.signature.dao.entity.WfTask;
import com.supcon.supfusion.signature.dao.entity.WfTransition;
import com.supcon.supfusion.signature.dao.mappers.WfDeploymentMapper;
import com.supcon.supfusion.signature.dao.mappers.WfTaskMapper;
import com.supcon.supfusion.signature.dao.mappers.WfTransitionMapper;
import com.supcon.supfusion.signature.services.service.ModelService;
import com.supcon.supfusion.signature.services.service.SignatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import springfox.documentation.swagger2.mappers.ModelMapper;


import java.util.List;
import java.util.Map;

/**
 * @author zhang yafei
 */
@Slf4j
@Service
public class SignatureServiceImpl implements SignatureService {

    @Autowired
    private ModelService modelService;

    @Autowired
    private WfDeploymentMapper wfDeploymentMapper;

    @Autowired
    private WfTaskMapper wfTaskMapper;

    @Autowired
    private WfTransitionMapper wfTransitionMapper;



    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Boolean getDeployment(Long deploymentId) {
        WfDeployment wfDeployment = wfDeploymentMapper.selectById(deploymentId);
        if (wfDeployment != null) {
            return wfDeployment.getSignatureEnable() == null ? false:wfDeployment.getSignatureEnable();
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public WfTask getTask(String code, Long deploymentId) {
        WfTask wfTask = wfTaskMapper.selectOne(new LambdaQueryWrapper<WfTask>().eq(WfTask::getCode, code)
                .eq(WfTask::getDeploymentId, deploymentId));

        return wfTask;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public WfTransition getTransition(String code, Long deploymentId) {
        WfTransition wfTransition = wfTransitionMapper.selectOne(new LambdaQueryWrapper<WfTransition>()
                .eq(WfTransition::getCode, code)
                .eq(WfTransition::getDeploymentId, deploymentId));

        return wfTransition;
    }

    /**
     * BAP-DZBY-XA
     *
     * @author chaibohai
     * 在单据生效时无法拿到流程的信息
     * 所以根据实体tableInfoId,和模块编码。过期当前流程签名信息
     * <p>
     * 此方法参考getFowXML方法写的
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public Object[] getSignatureEnable(Long tableInfoId, String modelCode) {
        // 默认情况下从 主表中读取数据。因为dealinfo被钱勇插入了垃圾数据，导致原先的算法可能会导致无法打开流程图的问题。
        String dealInfoTableName = modelService.getModel(modelCode).getTableName();
        String columnName = modelService.getPropertyColumnName(modelCode, "tableInfoId", null);
        // 避免使用key关键字（sqlserver下是关键字，会被）
//        String sql = "select PROCESS_KEY as pKey,PROCESS_VERSION as pVersion from " + dealInfoTableName + " where " + tableInfoIdcolName + " = ?";
        List<ModelTableDto> arr = modelService.getModelTableByTableInfoId(dealInfoTableName, columnName,tableInfoId);
        if (arr.size() > 0) {
            String key =  arr.get(0).getProcessKey();
            if (null == key || key.length() == 0) {
                // 因为group里的有很多历史数据的 process_key 和 process_version 字段的内容为空，因此如果无法从 ec_table_info里找到内容，则再从 dealinfo 表里找。
//                sql = "select PROCESS_KEY as pKey,PROCESS_VERSION as pVersion from " + getDealInfoTable(dealInfoTableName) + " where TABLE_INFO_ID = ? order by ID ASC";
                arr = modelService.getModelTableByDealInfoId(getDealInfoTable(dealInfoTableName), tableInfoId);
            }
        }
        if (arr.size() > 0) {
            ModelTableDto modelTableDto = arr.get(0);
            String key = modelTableDto.getProcessKey();
            Integer version = modelTableDto.getProcessVersion();

            if (key != null && key.length() > 0 && version != null) {
                LambdaQueryWrapper<WfDeployment> wfDeploymentLambdaQueryWrapper = new LambdaQueryWrapper<>();
                wfDeploymentLambdaQueryWrapper.eq(WfDeployment::getProcessKey,key)
                        .eq(WfDeployment::getProcessVersion,version);

                List<WfDeployment> wfDeployments = wfDeploymentMapper.selectList(wfDeploymentLambdaQueryWrapper);
//                List<Map<String, Object>> list = jdbcTemplate.queryForList("select d.signatureEnable,d.name from wf_deployment d where d.processKey=?0 and d.processVersion=?1 ", params);
                if (null != wfDeployments && wfDeployments.size() > 0) {
                    WfDeployment wfDeployment = wfDeployments.get(0);
                    if (null !=wfDeployment) {
                        Boolean isSignature = wfDeployment.getSignatureEnable();
                        String processName = wfDeployment.getName();
                        return new Object[]{isSignature, processName};
                    }
                }
            }
        }
        return new Object[]{false, ""};
    }

    public static String getDealInfoTable(String businessTable) {
        if (businessTable == null) {
            return null;
        }
        String tableName = businessTable.trim() + "_DI";
        return tableName.toLowerCase();
    }
}
