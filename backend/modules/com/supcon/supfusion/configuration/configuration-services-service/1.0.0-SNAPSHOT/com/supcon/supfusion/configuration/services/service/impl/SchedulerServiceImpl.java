package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.entity.Module;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.service.SchedulerService;
import com.supcon.supfusion.configuration.services.utils.UnicodeUtils;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.scheduler.server.api.dto.SchedulerJobDTO;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @Author kk.C
 * @Date 2021/1/4 13:09
 */
@Slf4j
@ServiceApiService
public class SchedulerServiceImpl implements SchedulerService {

    private static final String I18N_LANGUAGE_ZH = "zh_CN=";

    @Autowired
    private ModuleService moduleService;

    @Override
    public synchronized List<SchedulerJobDTO> importXml(String xml) {
        List<SchedulerJobDTO> schedulerJobDTOS = new ArrayList<>();
        try {
            Document document = DocumentHelper.parseText(xml);
            Element root = document.getRootElement();
            Iterator iterator = root.elementIterator();
            while (iterator.hasNext()) {
                Element element = (Element) iterator.next();
                if (!"CRON".equals(element.elementText("triggerType"))) {
                    continue;
                }
                SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
                List<Module> module = moduleService.getModuleByArtifact(element.elementText("moduleCode"));
                if (!ObjectUtils.isEmpty(module)) {
                    schedulerJobDTO.setModuleCode(element.elementText("moduleCode"));
                    schedulerJobDTO.setModelName(InternationalResource.get(module.get(0).getName()));
                }
                schedulerJobDTO.setCode(element.elementText("code"));
//                schedulerJobDTO.setJobKey(element.elementText("name")); //出现国际化key重复问题，注释这段,key重新在调度服务生成
                String jobName = UnicodeUtils.unicodeDecode(element.elementText("jobNameInternational"));
                schedulerJobDTO.setJobNameInternational(I18N_LANGUAGE_ZH + jobName);
                schedulerJobDTO.setJobName(jobName);
                schedulerJobDTO.setJobDesc(UnicodeUtils.unicodeDecode(element.elementText("description")));
                schedulerJobDTO.setServiceApi(element.elementText("jobContent"));
                schedulerJobDTO.setJobCron(element.elementText("cron"));
                //                schedulerJobDTO.setJobStatus(element.elementText("status"));
                schedulerJobDTO.setCallNo(Long.parseLong(element.elementText("hasRunTimes")));
                schedulerJobDTO.setUserName(element.elementText("tenantId"));
                schedulerJobDTO.setServiceParams(UnicodeUtils.unicodeDecode(element.elementText("serviceParams")));
                schedulerJobDTOS.add(schedulerJobDTO);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return schedulerJobDTOS;
    }
}
