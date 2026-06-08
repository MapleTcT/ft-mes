package com.supcon.supfusion.scheduler.server.service.job;

import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.scheduler.server.common.constant.ServiceConstant;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobService;
import com.supcon.supfusion.scheduler.server.service.SchedulerJobLogService;
import com.supcon.supfusion.scheduler.server.service.Utils.DateUtil;
import com.supcon.supfusion.scheduler.server.service.contants.SchedulerConstant;
import com.supcon.supfusion.scheduler.server.dao.SchedulerJobMapper;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobLogPo;
import com.supcon.supfusion.scheduler.server.dao.po.SchedulerJobPo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class ScheduleQuartzJob implements Job {

    public static final String need_tome = "use：";
    public static final String million_Time = "milliseconds";

    @Autowired
    SchedulerJobService schedulerJobService;

    @Autowired
    private SchedulerJobMapper schedulerJobMapper;

    @Autowired
    @Qualifier("customRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private SchedulerJobLogService schedulerJobLogService;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Transactional
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {

        log.info("定时任务启动================================================");
        Map map = jobExecutionContext.getJobDetail().getJobDataMap();
        Object jobId = map.get("id");
        Object modelName = map.get("modelName");
        Object userName = map.get("userName");
        Object jobName = map.get("jobName");
        Object jobCron = map.get("jobCron");
        Object serviceApi = map.get("serviceApi");
        Object serviceParams = map.get("serviceParams");
        Object code = map.get("code");
        Object userId = map.get("userId");
        Object cid = map.get("cid");
        // 可在此执行定时任务的具体业务
        String requestUri;

        if (StringUtils.isEmpty(serviceParams)) {
            // 无参数
            requestUri = String.format("%s?userId=%s&cid=%s", serviceApi, userId, cid);
        } else {
            // 有参数
            requestUri = String.format("%s?%s&userId=%s&cid=%s", serviceApi, serviceParams, userId, cid);
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
        log.info("--执行了task {}, {}, {}, {}, {}, {}", userName, requestUri, modelName, jobName, jobCron, serviceParams);
        SchedulerJobLogPo schedulerJobLogPo = new SchedulerJobLogPo();
        schedulerJobLogPo.setJobName((String) jobName);
        schedulerJobLogPo.setUserName((String) userName);
        schedulerJobLogPo.setModelName((String) modelName);
        schedulerJobLogPo.setServiceApi((String) serviceApi);
        schedulerJobLogPo.setServiceParams((String) serviceParams);
        schedulerJobLogPo.setCode((String) code);
        //        schedulerJobLogPo.setCreateTime(DateUtil.getCurrentDate());
        long startTime = System.currentTimeMillis();
        Integer statusCodeValue = 0;
        JSONObject body;
        try {
            schedulerJobService.updateCallNoAndTriggerDate((Long) jobId);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Job-Key", "public");
            headers.set("Content-Type", "application/json");
            headers.set("X-Tenant-Id", userName.toString());
            ResponseEntity<JSONObject> resp = restTemplate.exchange(requestUri, HttpMethod.GET, new HttpEntity<>(null, headers), JSONObject.class);
            body = resp.getBody();
            if(ObjectUtils.isEmpty(body)){
                body = new JSONObject();
                body.put("code",resp.getStatusCodeValue());
                body.put("detail",resp.toString());
            }
            if (null != body) {
                statusCodeValue = body.getInteger("code");
                //判断是否请求成功
                if (SchedulerConstant.getRequest_success_code == statusCodeValue) {
                    schedulerJobLogPo.setJobStatus(SchedulerConstant.JOB_STATUS_SUCCESS);
                } else {
                    schedulerJobLogPo.setJobStatus(SchedulerConstant.JOB_STATUS_FAILURE);
                    String detail = (String) body.get("detail");
                    schedulerJobLogPo.setExceptionInfo(detail);
                }
                long times = System.currentTimeMillis() - startTime;
                schedulerJobLogPo.setJobMessage(jobName + " " + need_tome + times + million_Time);
            } else {
                log.error("调用其它服务接口失败");
                throw new Exception();
            }
        } catch (Exception e) {
            log.error("任务执行失败 - 租户：{} 名称：{} 方法：{}", userName, jobName, serviceApi);
            long times = System.currentTimeMillis() - startTime;
            schedulerJobLogPo.setJobMessage(jobName + need_tome + times + million_Time);
            // 任务状态 0：成功 1：失败
            schedulerJobLogPo.setJobStatus(SchedulerConstant.JOB_STATUS_FAILURE);
            schedulerJobLogPo.setExceptionInfo(e.getMessage());
            statusCodeValue = e.hashCode();
            log.error(e.getMessage());
        } finally {
            SchedulerJobPo job = schedulerJobMapper.selectById((Long) jobId);
            if (statusCodeValue == SchedulerConstant.getRequest_success_code) {
                job.setJobStatus(SchedulerConstant.JOB_STATUS_RUN);
            } else {
                job.setJobStatus(SchedulerConstant.JOB_STATUS_exception);
            }
            job.setLastTime(new Date());
            //            job.setModifyTime(DateUtil.getCurrentDate());
            schedulerJobMapper.updateById(job);
            long l = IDGenerator.newInstance().generate().longValue();
            schedulerJobLogPo.setId(l);
            schedulerJobLogService.addSchedulerJobLog(schedulerJobLogPo);
        }
    }

}
