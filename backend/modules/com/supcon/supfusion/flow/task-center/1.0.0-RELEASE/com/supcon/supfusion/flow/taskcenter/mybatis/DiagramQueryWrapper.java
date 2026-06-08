/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.mybatis;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.supcon.supfusion.flow.common.dto.DiagramQueryContractDTO;
import com.supcon.supfusion.flow.common.enumeration.DiagramStatusEnum;
import com.supcon.supfusion.flow.common.po.DiagramPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;

import io.jsonwebtoken.lang.Collections;

/**
 * @author: zhuangmh
 * @date: 2020年5月19日 下午4:21:18
 */
public class DiagramQueryWrapper {

    private DiagramQueryWrapper() {
        throw new IllegalStateException("DiagramQueryWrapper is utility class, do not instantiate");
    }

    /**
     * 构建根据姓名和租户ID的查询器
     * 
     * @param name
     * @return LambdaQueryWrapper
     */
    public static LambdaQueryWrapper<DiagramPO> buildUniqueValidateQueryWrapper(String appId, Long companyId, String name, String tenantId) {
        LambdaQueryWrapper<DiagramPO> queryWrapper = Wrappers.<DiagramPO>lambdaQuery()
                .eq(DiagramPO::getProcessName, name)
                .eq(DiagramPO::getCid, companyId);
        if (StringUtils.isNotEmpty(appId)) {
            queryWrapper.and(i -> i.eq(DiagramPO::getAppId, appId));
        }
        if (tenantId != null) {
            queryWrapper.and(i -> i.eq(DiagramPO::getTenantId, tenantId));
        }
        return queryWrapper;
    }

    public static LambdaQueryWrapper<DiagramPO> buildIdQueryWrapper(long id) {
        return Wrappers.<DiagramPO>lambdaQuery().eq(DiagramPO::getId, id).and(i -> i.eq(DiagramPO::getValid, Constants.VALID));
    }

    /**
     * 查询当前流程编号的启用版本
     * 
     * @param processKey
     *            流程编号
     * @return
     */
    public static LambdaQueryWrapper<DiagramPO> buildEnableQueryWrapper(String appId, String processKey) {
        LambdaQueryWrapper<DiagramPO> queryWrapper = Wrappers.<DiagramPO>lambdaQuery()
                .eq(DiagramPO::getProcessKey, processKey)
                .and(i -> i.eq(DiagramPO::getValid, Constants.VALID))
                .and(i -> i.eq(DiagramPO::getEnabled, Constants.ENABLED));
        if (appId != null) {
            queryWrapper.and(i -> i.eq(DiagramPO::getAppId, appId));
        }
        return queryWrapper;
    }

    /**
     * 查询流程列表
     * 
     * @param queryContract
     * @return
     */
    public static LambdaQueryWrapper<DiagramPO> buildListQueryWrapper(DiagramQueryContractDTO queryContract) {
        String tenantId = RpcContext.getContext().getTenantId();
        LambdaQueryWrapper<DiagramPO> wrapper = Wrappers.<DiagramPO>lambdaQuery()
                .eq(DiagramPO::getValid, Constants.VALID)
                .eq(DiagramPO::getAppId, queryContract.getAppId());
        if (!Collections.isEmpty(queryContract.getProcessName())) {
            queryWrapperBuilder(wrapper, queryContract.getProcessName(), DiagramPO::getProcessName);
        }
        if (queryContract.getEnable() != null && queryContract.getEnable()) {
            wrapper.and(i -> i.eq(DiagramPO::getEnabled, Constants.ENABLED));
        } else if (queryContract.getHistory() == null || !queryContract.getHistory()) {
            wrapper.and(i -> i.eq(DiagramPO::getEnabled, Constants.ENABLED).or().eq(DiagramPO::getProcessStatus, DiagramStatusEnum.CREATION.getStatus()));
        }
        if (tenantId != null) {
            wrapper.and(i -> i.eq(DiagramPO::getTenantId, tenantId));
        }
        if (!Collections.isEmpty(queryContract.getCreator())) {
            queryWrapperBuilder(wrapper, queryContract.getCreator(), DiagramPO::getCreator);
        }
        if (!Collections.isEmpty(queryContract.getPublisher())) {
            queryWrapperBuilder(wrapper, queryContract.getPublisher(), DiagramPO::getPublisher);
        }
        if (queryContract.getMultiCompany() != null) {
            wrapper.and(i -> i.eq(DiagramPO::getMultiCompany, queryContract.getMultiCompany()));
        }
        return wrapper;
    }
    
    private  static <T> void queryWrapperBuilder(LambdaQueryWrapper<T> queryWrapper, List<String> queryParameter, SFunction<T,?> column) {
        queryWrapper.and(i -> {
            int size = queryParameter.size();
            for (int t = 0; t < size; t++) {
                if (t > 0) {
                    i.or();
                }
                i.like(column, queryParameter.get(t)).or();
            }
        });
    }
}
