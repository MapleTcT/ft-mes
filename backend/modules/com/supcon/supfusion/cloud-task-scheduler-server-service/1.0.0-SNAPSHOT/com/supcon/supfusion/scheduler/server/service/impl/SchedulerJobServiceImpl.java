package com.supcon.supfusion.scheduler.server.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizException;
import com.supcon.supfusion.framework.cloud.common.result.*;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;
import com.supcon.supfusion.scheduler.manager.*;
import com.supcon.supfusion.scheduler.server.common.constant.ServiceConstant;
import com.supcon.supfusion.scheduler.server.dao.SchedulerJobMapper;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobLogPo;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobPo;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobLogService;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobService;
import com.supcon.supfusion.scheduler.server.service.Utils.DateUtil;
import com.supcon.supfusion.scheduler.server.service.Utils.EscapeUtil;
import com.supcon.supfusion.scheduler.server.service.bo.SchedulerJobBo;
import com.supcon.supfusion.scheduler.server.service.contants.SchedulerConstant;
import com.supcon.supfusion.scheduler.server.service.exception.TaskErrorEnum;
import com.supcon.supfusion.scheduler.server.service.job.ScheduleQuartzJob;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;

@Slf4j
@Data
@Service
public class SchedulerJobServiceImpl implements SchedulerJobService {
    public static final int I18N_LANGUAGE_LENGTH = 2;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private SchedulerJobMapper schedulerJobMapper;

    @Autowired
    @Qualifier("customRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private SchedulerI18nAdapter i18nAdapter;

    @Autowired
    private SchedulerJobLogService schedulerJobLogService;

    @Autowired
    private ModuleRegistryAdapter moduleRegistryAdapter;

    @Override
    public SchedulerJobBo JobPoToJobModel(SchedulerJobPo job) {
        if (job == null) {
            return null;
        }
        SchedulerJobBo SchedulerJobBo = new SchedulerJobBo();
        BeanUtils.copyProperties(job, SchedulerJobBo);
        return SchedulerJobBo;
    }

    @Override
    public JobDataMap JobModel2JobDataMap(SchedulerJobBo schedulerJobBo) {
        UserContext context = UserContext.getUserContext();
        if (schedulerJobBo == null)
            return null;
        JobDataMap map = new JobDataMap();
        map.put("id", schedulerJobBo.getId());
        map.put("userName", schedulerJobBo.getUserName());
        map.put("modelName", schedulerJobBo.getModelName());
        map.put("jobName", schedulerJobBo.getJobName());
        map.put("jobCron", schedulerJobBo.getJobCron());
        map.put("serviceApi", schedulerJobBo.getServiceApi());
        map.put("serviceParams", schedulerJobBo.getServiceParams());
        map.put("code", schedulerJobBo.getCode());
        map.put("userId", context.getUserId());
        map.put("cid", context.getCompanyId());
        return map;
    }

    /**
     * 获取所有可用定时任务
     */
    @Override
    public Page<SchedulerJobPo> scheduleGetAll(SchedulerJobBo schedulerJobBo) {

        QueryWrapper<SchedulerJobPo> qryWrapper = new QueryWrapper<>();

        if (null != schedulerJobBo.getId()) {
            qryWrapper.eq("id", schedulerJobBo.getId());
        }
        if (null != schedulerJobBo.getJobStatus()) {
            qryWrapper.eq("job_status", schedulerJobBo.getJobStatus());
        }
        if (null != schedulerJobBo.getServiceApi()) {
            qryWrapper.like("job_service_api", schedulerJobBo.getServiceApi());
        }
        if (null != schedulerJobBo.getCode()) {
            qryWrapper.like("code", schedulerJobBo.getCode());
        }
        if (null != schedulerJobBo.getUserName()) {
            qryWrapper.eq("user_name", schedulerJobBo.getUserName());
        }
        if (null != schedulerJobBo.getModelName() && null == schedulerJobBo.getModuleCode()) {
            qryWrapper.like("model_name", schedulerJobBo.getModelName());
        }
        if (null != schedulerJobBo.getModuleCode()) {
            //            List<String> moduleCodeList = new ArrayList<>();
            //            moduleCodeList.add(schedulerJobBo.getModuleCode());
            //            String moduleCode[] = schedulerJobBo.getModuleCode().split("_");
            //            if (com.baomidou.mybatisplus.core.toolkit.ObjectUtils.isNotEmpty(moduleCode)) {
            //                moduleCodeList.add(moduleCode[0]);
            //            }
            qryWrapper.eq("module_code", schedulerJobBo.getModuleCode());
            //            qryWrapper.in("module_code", moduleCodeList);
        }
        if (null != schedulerJobBo.getFilter()) {
            qryWrapper.eq("job_status", schedulerJobBo.getFilter());
        }
        if (null != schedulerJobBo.getServiceParams()) {
            qryWrapper.eq("job_service_params", schedulerJobBo.getServiceParams());
        }
        if (null != schedulerJobBo.getJobName()) {
            if (schedulerJobBo.getJobName().contains("/") || schedulerJobBo.getJobName().contains("_") || schedulerJobBo.getJobName().contains("%")) {
                qryWrapper.apply("job_name like '%" + EscapeUtil.escapeChar(schedulerJobBo.getJobName()) + "%' escape '\\'");
            } else {
                qryWrapper.like("job_name", schedulerJobBo.getJobName());
            }
        }
        if (null != schedulerJobBo.getSorter() && !schedulerJobBo.getSorter().isEmpty()) {
            String[] s = schedulerJobBo.getSorter().split("_");
            String column = s[0];
            String way = s[1];
            String orderColumn = getOrderColumn(column);
            if (way.startsWith("asc")) {
                qryWrapper.orderBy(true, true, orderColumn);
            } else {
                qryWrapper.orderBy(true, false, orderColumn);
            }
        } else {
            qryWrapper.orderBy(true, false, "id");
        }
        int pageNumber;
        int pageSize;

        if (null == schedulerJobBo.getCurrent() || null == schedulerJobBo.getPageSize()) {
            pageNumber = 1;
            pageSize = 10;
        } else {
            pageNumber = schedulerJobBo.getCurrent();
            pageSize = schedulerJobBo.getPageSize();
        }
        Page<SchedulerJobPo> page = new Page<>(pageNumber, pageSize);
        Page<SchedulerJobPo> result = schedulerJobMapper.selectPage(page, qryWrapper);
        result.getRecords().forEach(schedulerJob -> {
            String international = i18nAdapter.getRemoteMessage(schedulerJob.getJobKey());
            international = schedulerJob.getJobKey().equals(international) ? schedulerJob.getJobName() : international;
            schedulerJob.setJobName(international);
        });
        return result;

    }

    /**
     * 获取对应ID定时任务详情
     */
    @Override
    public SchedulerJobPo scheduleGetById(Long id) {
        return schedulerJobMapper.selectById(id);
    }

    /**
     * 开启定时任务
     *
     * @param schedulerJobBo
     */
    @Transactional
    @Override
    public void scheduleAdd(SchedulerJobBo schedulerJobBo) throws SchedulerException {
        QueryWrapper<SchedulerJobPo> qryWrapper = new QueryWrapper<>();
        qryWrapper.eq("model_name", schedulerJobBo.getModelName());
        //        qryWrapper.eq("user_name", schedulerJobBo.getUserName());
        qryWrapper.eq("job_name", schedulerJobBo.getJobName());
        qryWrapper.eq("job_cron", schedulerJobBo.getJobCron());
        if (null != schedulerJobBo.getServiceParams()) {
            qryWrapper.eq("job_service_params", schedulerJobBo.getServiceParams());
        }
        Integer integer = schedulerJobMapper.selectCount(qryWrapper);
        if (integer > 0) {
            throw new BizException(TaskErrorEnum.TASK_HAS_EXIST);
        }
        qryWrapper.clear();
        qryWrapper.eq("code", schedulerJobBo.getCode());
        integer = schedulerJobMapper.selectCount(qryWrapper);
        if (integer > 0) {
            throw new BizException(TaskErrorEnum.JOBCODE_EXIST);
        }
        //校验cron表达式
        if (!CronExpression.isValidExpression(schedulerJobBo.getJobCron())) {
            log.error("cron expression failure");
            throw new BizException(TaskErrorEnum.CRON_ERROR);
        }
        //添加国际化
        addOrUpdateI18nMessage(schedulerJobBo);
        //实体新增
        SchedulerJobPo schedulerJobPo = new SchedulerJobPo();
        BeanUtils.copyProperties(schedulerJobBo, schedulerJobPo);
        schedulerJobPo.setJobDesc(schedulerJobBo.getJobDesc() == null ? "" : schedulerJobBo.getJobDesc());
        //        schedulerJobPo.setCreateTime(DateUtil.getCurrentDate());
        schedulerJobPo.setJobStatus(SchedulerConstant.JOB_STATUS_PENDING);
        schedulerJobPo.setCallNo(0L);

        //        if (null != schedulerJobPo.getCode()) {
        //            Map<String, Object> map = new HashMap<>();
        //            map.put("code", schedulerJobPo.getCode());
        //            List<SchedulerJobPo> ls = schedulerJobMapper.selectByMap(map);
        //            if (ls.size() > 0) {
        //                log.error("job code has exist");
        //                BizException bizException = new BizException(TaskErrorEnum.JOBNAME_EXIST);
        //                throw bizException;
        //            }
        //        }
        try {
            schedulerJobPo.setNextTime(getNextValidTriggerDate(schedulerJobBo.getJobCron()));
        } catch (ParseException e) {
            log.error("Parse job next time failure  " + DateUtil.getStackTrace(e));
        }
        long l = IDGenerator.newInstance().generate().longValue();
        // schedulerJobPo.setLastTime(null);
        schedulerJobPo.setId(l);
        schedulerJobMapper.insert(schedulerJobPo);
        //开启任务
        schedulerJobBo.setId(schedulerJobPo.getId());
        startJob(scheduler, schedulerJobBo);
    }

    @Override
    public void updateCallNoAndTriggerDate(Long id) {
        SchedulerJobPo schedulerJobPo = schedulerJobMapper.selectById(id);
        if (ObjectUtils.isEmpty(schedulerJobPo)) {
            return;
        }
        schedulerJobPo.setCallNo(schedulerJobPo.getCallNo() == null ? 1 : (schedulerJobPo.getCallNo() + 1));
        if (null == schedulerJobPo.getLastTime()) {
            schedulerJobPo.setLastTime(new Date());
        } else {
            schedulerJobPo.setLastTime(schedulerJobPo.getLastTime());
        }
        try {
            schedulerJobPo.setNextTime(getNextValidTriggerDate(schedulerJobPo.getJobCron()));
        } catch (ParseException e) {
            log.error("Parse job next time failure  " + DateUtil.getStackTrace(e));
        }
        schedulerJobMapper.updateById(schedulerJobPo);
    }

    public Date getNextValidTriggerDate(String cronExp) throws ParseException {
        if (!CronExpression.isValidExpression(cronExp)) {
            return null;
        }
        CronExpression cronExpression = new CronExpression(cronExp);
        log.info("systemTime " + DateUtil.getCurrentDate());
        Date date = new Date();
        return cronExpression.getNextValidTimeAfter(date);
    }

    /**
     * 更新trigger
     *
     * @param schedulerJobBo
     */
    @Transactional
    @Override
    public void scheduleUpdateTrigger(SchedulerJobBo schedulerJobBo) {

        if (ObjectUtils.isEmpty(schedulerJobBo.getId()) || ObjectUtils.isEmpty(schedulerJobBo.getJobCron())) {
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        //校验cron表达式
        if (!CronExpression.isValidExpression(schedulerJobBo.getJobCron())) {
            log.error("cron expression failure");
            throw new BizException(TaskErrorEnum.CRON_ERROR);
        }
        SchedulerJobPo SchedulerJobPo = schedulerJobMapper.selectById(schedulerJobBo.getId());
        if (ObjectUtils.isEmpty(SchedulerJobPo)) {
            throw new BizException(TaskErrorEnum.TASK_NO_EXIST);
        }
        try {
            SchedulerJobPo.setJobDesc(schedulerJobBo.getJobDesc());
            String initCron = SchedulerJobPo.getJobCron();
            // 持久化更新
            SchedulerJobPo.setJobCron(schedulerJobBo.getJobCron());
            SchedulerJobPo.setNextTime(getNextValidTriggerDate(schedulerJobBo.getJobCron()));
            //            SchedulerJobPo.setModifyTime(DateUtil.getCurrentDate());
            // 更新触发器
            if (schedulerJobBo.getJobCron() != null && !schedulerJobBo.getJobCron().equals(initCron)) {
                updateTrigger(SchedulerJobPo, schedulerJobBo);
                SchedulerJobPo.setLastTime(null);
                SchedulerJobPo.setJobStatus(SchedulerConstant.JOB_STATUS_PENDING);
            }
            schedulerJobMapper.updateById(SchedulerJobPo);
        } catch (Exception e) {
            log.error("update task trigger failure " + DateUtil.getStackTrace(e));
            throw new BizException(TaskErrorEnum.UPDATE_TRIGGER_FAILURE);
        }
    }

    /**
     * 更新定时任务
     *
     * @param schedulerJobBo
     */
    @Transactional
    @Override
    public void scheduleUpdateJob(SchedulerJobBo schedulerJobBo) {
        if (ObjectUtils.isEmpty(schedulerJobBo.getId())) {
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        //        if(Optional.ofNullable(schedulerJobMapper.selectById(schedulerJobBo.getId())).isPresent()){
        //
        //        }
        //检查数据库是否存在重复的job
        //        SchedulerJobResponseDTO scheduleJobResBonse = scheduleGetAll(schedulerJobBo);
        //        Integer total = scheduleJobResBonse.getTotal();
        //        if (total > 0) {
        //            throw new BizException(TaskErrorEnum.TASK_HAS_EXIST);
        //        }
        SchedulerJobPo job = schedulerJobMapper.selectById(schedulerJobBo.getId());

        if (ObjectUtils.isEmpty(job)) {
            throw new BizException(TaskErrorEnum.TASK_NO_EXIST);
        }
        if (!ObjectUtils.isEmpty(schedulerJobBo.getJobCron())) {
            //校验cron表达式
            if (!CronExpression.isValidExpression(schedulerJobBo.getJobCron())) {
                log.error("cron expression failure");
                throw new BizException(TaskErrorEnum.CRON_ERROR);
            }
        }
        job.setLastTime(null);
        addOrUpdateI18nMessage(schedulerJobBo);
        try {
            job.setJobStatus(SchedulerConstant.JOB_STATUS_PENDING);
            job.setNextTime(getNextValidTriggerDate(schedulerJobBo.getJobCron()));
            // 持久化更新
            if (!StringUtils.equals(schedulerJobBo.getJobCron(), job.getJobCron())) {
                job.setJobCron(schedulerJobBo.getJobCron());
            }
            if (!StringUtils.equals(schedulerJobBo.getModelName(), job.getModelName())) {
                job.setModelName(schedulerJobBo.getModelName());
            }
            if (!StringUtils.equals(schedulerJobBo.getJobName(), job.getJobName())) {
                job.setJobName(schedulerJobBo.getJobName());
            }
            if (!StringUtils.equals(schedulerJobBo.getJobDesc(), job.getJobDesc())) {
                job.setJobDesc(schedulerJobBo.getJobDesc());
            }
            if (!StringUtils.equals(schedulerJobBo.getServiceApi(), job.getServiceApi())) {
                job.setServiceApi(schedulerJobBo.getServiceApi());
            }
            if (!StringUtils.equals(schedulerJobBo.getServiceParams(), job.getServiceParams())) {
                job.setServiceParams(schedulerJobBo.getServiceParams());
            }
            //            job.setModifyTime(DateUtil.getCurrentDate());
            log.info("update job");
            schedulerJobMapper.updateById(job);
            // 更新任务
            updateJobDetail(schedulerJobBo);
        } catch (Exception e) {
            log.error("update task failure " + DateUtil.getStackTrace(e));
            throw new BizException(TaskErrorEnum.UPDATE_TASK_FAILURE);
        }
    }

    /**
     * 任务 - 暂停
     */
    @Transactional
    @Override
    public void schedulePause(List<Long> list) {
        if (list.size() == 0) {
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        //        list.forEach(ls -> {
        for (Long id : list) {
            try {
                SchedulerJobPo job = schedulerJobMapper.selectById(id);
                if (ObjectUtils.isEmpty(job)) {
                    //                    throw new BizException(TaskErrorEnum.TASK_NO_EXIST);
                    continue;
                }
                JobKey jobKey = new JobKey(job.getCode(), job.getUserName());
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                if (jobDetail == null) {
                    //nothing;
                } else {
                    scheduler.pauseJob(jobKey);
                }
                job.setJobStatus(SchedulerConstant.JOB_STATUS_STOP);
                schedulerJobMapper.updateById(job);
            } catch (Exception e) {
                log.error("pause task failure " + DateUtil.getStackTrace(e));
                throw new BizException(TaskErrorEnum.PAUSE_TASK_FAILURE);
            }
        }

    }

    /**
     * 任务 - 恢复
     */
    @Transactional
    @Override
    public void scheduleResume(List<Long> list) {
        if (list.size() == 0) {
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        for (Long id : list) {
            try {
                SchedulerJobPo job = schedulerJobMapper.selectById(id);
                if (ObjectUtils.isEmpty(job)) {
                    log.warn("active job ,but job don't exist");
                    continue;
                }
                JobKey jobKey = new JobKey(job.getCode(), job.getUserName());
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                if (jobDetail == null) {
                    //重启动 任务
                    startJob(scheduler, JobPoToJobModel(job));
                } else {
                    scheduler.resumeJob(jobKey);
                }
                job.setJobStatus(SchedulerConstant.JOB_STATUS_PENDING);
                schedulerJobMapper.updateById(job);
            } catch (Exception e) {
                log.error("activate task failure " + DateUtil.getStackTrace(e));
                throw new BizException(TaskErrorEnum.ACTIVATE_TASK_FAILURE);
            }
        }
    }

    /**
     * 任务 - 删除一个定时任务
     */
    @Transactional
    @Override
    public void scheduleDelete(List<Long> list) {
        if (ObjectUtils.isEmpty(list) && list.size() == 0) {
            throw new BizException(TaskErrorEnum.PARAMETER_ERROR);
        }
        //        list.forEach(ls -> {
        for (Long id : list) {
            //Integer i = Integer.parseInt(ls);
            SchedulerJobPo job = schedulerJobMapper.selectById(id);
            try {
                if (ObjectUtils.isEmpty(job)) {
                    log.error("job don't exist when delete job ");
                    //                    throw new BizException(TaskErrorEnum.TASK_NO_EXIST);
                    continue;
                }
                JobKey jobKey = new JobKey(job.getCode(), job.getUserName());
                JobDetail jobDetail = null;

                jobDetail = scheduler.getJobDetail(jobKey);

                if (jobDetail == null) {
                    //do nothing
                } else {
                    scheduler.deleteJob(jobKey);
                }
                schedulerJobMapper.deleteById(job);
            } catch (Exception e) {
                log.error("delete task failure " + DateUtil.getStackTrace(e));
                throw new BizException(TaskErrorEnum.DELETE_TASK_FAILURE);
            }
        }

    }

    private void updateJobDetail(SchedulerJobBo schedulerJobBo) {
        try {
            // 获取任务
            JobKey jobKey = new JobKey(schedulerJobBo.getCode(), schedulerJobBo.getUserName());
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            // 删除旧任务
            if (jobDetail != null) {
                scheduler.deleteJob(jobKey);
            }
            // 开启新任务
            startJob(scheduler, schedulerJobBo);
        } catch (SchedulerException e) {
            log.error("updateJob Detail task failure " + DateUtil.getStackTrace(e));
            throw new BizException(TaskErrorEnum.SYSTEM_ERROR);
        }

    }

    /**
     * 一键执行
     *
     * @param schedulerJobBo
     * @return
     */
    @Override
    public String schedulerImmediateExcute(SchedulerJobBo schedulerJobBo) {
        SchedulerJobLogPo schedulerJobLogPo = new SchedulerJobLogPo();
        //        schedulerJobLogPo.setJobName(schedulerJobBo.getJobName());
        //        schedulerJobLogPo.setUserName((String) userName);
        //        schedulerJobLogPo.setModelName((String) modelName);
        //        schedulerJobLogPo.setServiceApi((String) serviceApi);
        //        schedulerJobLogPo.setServiceParams((String) serviceParams);
        //        schedulerJobLogPo.setCode((String) code);
        BeanUtils.copyProperties(schedulerJobBo, schedulerJobLogPo);
        HttpHeaders headers = new HttpHeaders();
        String serviceApi = schedulerJobBo.getServiceApi();
        headers.set("Content-Type", "application/json");
        headers.set("X-Tenant-Id", schedulerJobBo.getUserName());
        String serviceParams = schedulerJobBo.getServiceParams();
        String requestUri = null;
        long startTime = System.currentTimeMillis();
        try {
            if (!StringUtils.isEmpty(serviceParams)) {
                requestUri = String.format("%s?%s", serviceApi, serviceParams);
            } else {
                requestUri = String.format("%s", serviceApi);
            }
            if ((!requestUri.startsWith(ServiceConstant.DOMAIN_START) && !requestUri.startsWith(ServiceConstant.DOMAIN_STARTS))) {
                //                requestUri = String.format("/%s%s", domainName, requestUri);
                if (!requestUri.startsWith("/")) {
                    requestUri = "/" + requestUri;
                }
                List<ServiceInstance> serviceInstances = discoveryClient.getInstances("supos-gateway");
                ServiceInstance serviceInstance = null;
                if (!CollectionUtils.isEmpty(serviceInstances)) {
                    serviceInstance = serviceInstances.get(0);
                }
                requestUri = ServiceConstant.DOMAIN_START + serviceInstance.getHost() + ":" + serviceInstance.getPort() + requestUri;
            }
            log.info("一键执行：{},{}", schedulerJobBo.getJobName(), requestUri);
            ResponseEntity<JSONObject> resp = restTemplate.exchange(requestUri, HttpMethod.GET, new HttpEntity<>(null, headers), JSONObject.class);
            JSONObject body = resp.getBody();
            if (ObjectUtils.isEmpty(body)) {
                body = new JSONObject();
                body.put("code", resp.getStatusCodeValue());
                body.put("detail", resp.toString());
            }
            int code = body.getInteger("code");

            //判断是否请求成功
            if (SchedulerConstant.getRequest_success_code == code) {
                schedulerJobLogPo.setJobStatus(SchedulerConstant.JOB_STATUS_SUCCESS);
            } else {
                schedulerJobLogPo.setJobStatus(SchedulerConstant.JOB_STATUS_FAILURE);
                String detail = (String) body.get("detail");
                schedulerJobLogPo.setExceptionInfo(detail);
            }
            long times = System.currentTimeMillis() - startTime;
            schedulerJobLogPo.setJobMessage(schedulerJobLogPo.getJobName() + " " + ScheduleQuartzJob.need_tome + times + ScheduleQuartzJob.million_Time);
        } catch (Exception e) {
            log.error("一键执行：{},{},{},failure", schedulerJobBo.getJobName(), requestUri, schedulerJobBo.getServiceParams());
            long times = System.currentTimeMillis() - startTime;
            schedulerJobLogPo.setJobMessage(schedulerJobLogPo.getJobName() + ScheduleQuartzJob.need_tome + times + ScheduleQuartzJob.million_Time);
            // 任务状态 0：成功 1：失败
            schedulerJobLogPo.setJobStatus(SchedulerConstant.JOB_STATUS_FAILURE);
            schedulerJobLogPo.setExceptionInfo(e.getMessage());
            log.error(e.getMessage());
        } finally {
            SchedulerJobPo schedulerJobPo = schedulerJobMapper.selectById(schedulerJobBo.getId());
            schedulerJobPo.setCallNo(schedulerJobPo.getCallNo() == null ? 1 : (schedulerJobPo.getCallNo() + 1));
            schedulerJobPo.setLastTime(new Date());
            schedulerJobMapper.updateById(schedulerJobPo);
            long l = IDGenerator.newInstance().generate().longValue();
            schedulerJobLogPo.setId(l);
            schedulerJobLogService.addSchedulerJobLog(schedulerJobLogPo);
        }
        return "success";
    }

    private void updateTrigger(SchedulerJobPo job, SchedulerJobBo schedulerJobBo) {
        String result = schedulerJobBo.getJobCron();
        BeanUtils.copyProperties(job, schedulerJobBo);
        schedulerJobBo.setJobCron(result);
        updateJobDetail(schedulerJobBo);
    }

    // 开启任务
    private void startJob(Scheduler scheduler, SchedulerJobBo schedulerJobBo) {
        // 通过JobBuilder构建JobDetail实例，JobDetail规定只能是实现Job接口的实例
        // 在map中可传入自定义参数，在job中使用
        // JobDetail 是具体Job实例
        //        JobDetail jobDetail = JobBuilder.newJob(ScheduleQuartzJob.class).withIdentity(schedulerJobBo.getJobName(), schedulerJobBo.getUserName()).usingJobData(JobModel2JobDataMap(schedulerJobBo)).build();
        JobDetail jobDetail = newJob(ScheduleQuartzJob.class).withIdentity(schedulerJobBo.getCode(), schedulerJobBo.getUserName()).usingJobData(JobModel2JobDataMap(schedulerJobBo)).build();
        // 基于表达式构建触发器
        //        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(schedulerJobBo.getJobCron());
        //        Trigger trigger = newTrigger().withIdentity(schedulerJobBo.getCode(), schedulerJobBo.getUserName()).withSchedule(simpleSchedule().withIntervalInSeconds(2).repeatForever()).build();
        // CronTrigger表达式触发器 继承于Trigger
        // TriggerBuilder 用于构建触发器实例
        try {
            //            CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(schedulerJobBo.getJobName(), schedulerJobBo.getUserName()).withSchedule(cronScheduleBuilder.withMisfireHandlingInstructionDoNothing()).build();
            CronTrigger cronTrigger = newTrigger().withIdentity(schedulerJobBo.getCode(), schedulerJobBo.getUserName()).withSchedule(cronSchedule(schedulerJobBo.getJobCron())).build();
            scheduler.scheduleJob(jobDetail, cronTrigger);
            scheduler.start();
        } catch (SchedulerException e) {
            log.error("start task failure " + DateUtil.getStackTrace(e));

        }
    }

    private String getOrderColumn(String column) {
        String result = null;
        if ("modelName".equals(column)) {
            result = "model_name";
        }
        if ("callNo".equals(column)) {
            result = "model_name";
        }
        if ("nextTime".equals(column)) {
            result = "next_time";
        }
        if ("lastTime".equals(column)) {
            result = "last_time";
        }
        return result;
    }

    /**
     * @return void
     * @Author kk.C
     * @Description 写入或更新国际化信息
     * @Date 2020/11/03 16.34
     * @Param [ecPortletBO]
     **/
    private SchedulerJobBo addOrUpdateI18nMessage(SchedulerJobBo schedulerJobBo) {
        Map<String, Object> i18nMap = new HashMap<>();
        Map<String, String> languageMap = new HashMap<>();
        //        String[] moduleCode = schedulerJobBo.getJobKey().split("\\.");
        String[] moduleCode = schedulerJobBo.getModuleCode().split("_");
        String jobKey = null;
        if (!StringUtils.isEmpty(schedulerJobBo.getJobKey())) {
            jobKey = schedulerJobBo.getJobKey().replace(schedulerJobBo.getModuleCode(), moduleCode[0]);
        } else {
            jobKey = i18nAdapter.initI18nKey(moduleCode[0]);
        }
        schedulerJobBo.setJobKey(jobKey);
        i18nMap.put("moduleCode", moduleCode[0]);
        i18nMap.put("i18n_key", jobKey);
        String titleInternational = schedulerJobBo.getJobNameInternational();
        if (!StringUtils.isEmpty(titleInternational)) {
            if (titleInternational.contains("$&#")) {
                String[] languages = titleInternational.split("[$]&#");
                for (String title : languages) {
                    String[] language = title.split("=");
                    if (language.length == I18N_LANGUAGE_LENGTH) {
                        languageMap.put(language[0], language[1]);
                    }
                }
            } else {
                String[] language = titleInternational.split("=");
                if (language.length == I18N_LANGUAGE_LENGTH) {
                    languageMap.put(language[0], language[1]);
                }
            }
        }
        i18nMap.put("i18n_value", languageMap);
        if (!ObjectUtils.isEmpty(languageMap)) {
            Result result = i18nAdapter.messageResourceAddOrUpdateOne(i18nMap);
            if (null != (result.getCode())) {
                schedulerJobBo.setJobName(schedulerJobBo.getJobKey());
            }
        }
        return schedulerJobBo;
    }

    @Override
    public Collection<ModuleDTO> queryModules(String keyword, Boolean isAccurate) {
        if (null == isAccurate) {
            isAccurate = false;
        }
        Collection<ModuleDTO> moduleDTOS = moduleRegistryAdapter.queryModules();
        if (StringUtils.isEmpty(keyword)) {
            return moduleDTOS;
        }
        Collection<ModuleDTO> resultList = new ArrayList<>();
        if (isAccurate) {
            for (ModuleDTO moduleDTO : moduleDTOS) {
                if (moduleDTO.getModuleCode().equals(keyword)) {
                    resultList.add(moduleDTO);
                } else if (moduleDTO.getModuleName().equals(keyword)) {
                    resultList.add(moduleDTO);
                }
            }
        } else {
            for (ModuleDTO moduleDTO : moduleDTOS) {
                if (moduleDTO.getModuleCode().contains(keyword)) {
                    resultList.add(moduleDTO);
                } else if (moduleDTO.getModuleName().contains(keyword)) {
                    resultList.add(moduleDTO);
                }
            }
        }
        return resultList;
    }
}
