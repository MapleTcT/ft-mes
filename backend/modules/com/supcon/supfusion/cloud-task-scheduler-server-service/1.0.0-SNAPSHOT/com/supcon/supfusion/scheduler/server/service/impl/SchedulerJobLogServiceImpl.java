package com.supcon.supfusion.scheduler.server.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.scheduler.server.dao.SchedulerJobLogMapper;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobLogPo;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobLogService;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobLogBo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author 刘旺
 * @version V1.0
 * @Package com.supcon.mare.scheduler.service
 * @date 2020/7/20 13:51
 * @Copyright © 2020 中控（西安）
 */
@Slf4j
@Data
@Service
public class SchedulerJobLogServiceImpl implements SchedulerJobLogService {

    @Autowired
    private SchedulerJobLogMapper schedulerJobLogMapper;

    @Override
    public void addSchedulerJobLog(SchedulerJobLogPo schedulerJobLogPo) {
        schedulerJobLogMapper.insert(schedulerJobLogPo);
    }

    @Override
    public Page<SchedulerJobLogPo> getSchedulerJobLog(SchedulerJobLogBo SchedulerJobLogBo) {
        Boolean fuzzySearch = SchedulerJobLogBo.getFuzzySearch();
        QueryWrapper<SchedulerJobLogPo> qryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(SchedulerJobLogBo.getUserName())) {
            qryWrapper.eq("user_name", SchedulerJobLogBo.getUserName());
        }
        if (!StringUtils.isEmpty( SchedulerJobLogBo.getJobName())) {
            if (fuzzySearch) {
                qryWrapper.like("job_name", SchedulerJobLogBo.getJobName());
            } else {
                qryWrapper.eq("job_name", SchedulerJobLogBo.getJobName());
            }
        }
        if (!StringUtils.isEmpty(SchedulerJobLogBo.getCode())) {
            if (fuzzySearch) {
                qryWrapper.like("code", SchedulerJobLogBo.getCode());
            } else {
                qryWrapper.eq("code", SchedulerJobLogBo.getCode());
            }
        }
        if (!StringUtils.isEmpty( SchedulerJobLogBo.getModelName())) {
            if (fuzzySearch) {
                qryWrapper.like("model_name", SchedulerJobLogBo.getModelName());
            } else {
                qryWrapper.eq("model_name", SchedulerJobLogBo.getModelName());
            }
        }
        if (!StringUtils.isEmpty(SchedulerJobLogBo.getServiceApi())) {
            if (fuzzySearch) {
                qryWrapper.like("job_service_api", SchedulerJobLogBo.getServiceApi());
            } else {
                qryWrapper.eq("job_service_api", SchedulerJobLogBo.getServiceApi());
            }
        }

        if (null != SchedulerJobLogBo.getFilter()) {
            qryWrapper.eq("job_status", SchedulerJobLogBo.getFilter());
        } else {
            if (null != SchedulerJobLogBo.getJobStatus()) {
                qryWrapper.eq("job_status", SchedulerJobLogBo.getJobStatus());
            }
        }
        int pageNumber;
        int pageSize;
        if (null == SchedulerJobLogBo.getCurrent() || null == SchedulerJobLogBo.getPageSize()) {
            pageNumber = 1;
            pageSize = 20;
        } else {
            pageNumber = SchedulerJobLogBo.getCurrent();
            pageSize = SchedulerJobLogBo.getPageSize();
        }
        if (null != SchedulerJobLogBo.getSorter() && !SchedulerJobLogBo.getSorter().isEmpty()) {
            String[] s = SchedulerJobLogBo.getSorter().split("_");
            String column = s[0];
            String way = s[1];
            if (way.startsWith("asc")) {
                qryWrapper.orderBy(true, true, "create_time");
            } else {
                qryWrapper.orderBy(true, false, "create_time");
            }
        } else {
            qryWrapper.orderBy(true, false, "id");
        }
        Page<SchedulerJobLogPo> page = new Page<>(pageNumber, pageSize);
        Page<SchedulerJobLogPo> result = schedulerJobLogMapper.selectPage(page, qryWrapper);
        return result;
    }

}
