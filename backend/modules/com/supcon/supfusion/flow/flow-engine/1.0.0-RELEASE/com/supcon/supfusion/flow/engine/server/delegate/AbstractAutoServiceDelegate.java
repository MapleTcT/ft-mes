/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.engine.server.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import com.supcon.supfusion.flow.common.dto.OodmSettingDTO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.engine.server.service.BpmnService;

/**
 * @author: zhuangmh
 * @date: 2021年1月19日 下午7:35:12
 */
public abstract class AbstractAutoServiceDelegate implements JavaDelegate {
    
    @Autowired
    @Lazy
    private BpmnService bpmnService;
    /**
     * @see org.flowable.engine.delegate.JavaDelegate#execute(org.flowable.engine.delegate.DelegateExecution)
     */
    @Override
    public void execute(DelegateExecution execution) {
        OodmSettingDTO oodmSetting = bpmnService.getOodmSettingsInternal(execution.getProcessDefinitionId(), execution.getCurrentActivityId());
        if (oodmSetting == null) {
            return;
        }
        String async = execution.getCurrentFlowElement().getAttributeValue(Constants.FLOWABLE_NAMESPACE_DEF, Constants.AUTOTASK_ASYNC);
        doAutoTask(execution.getProcessInstanceId(), oodmSetting, Boolean.valueOf(async));
    }
    /**
     * 调用在OODM配置的脚本
     * 具体处理类在com.supcon.supfusion.flow.taskcenter.service.AutoServiceDelegate
     * 
     * @param oodmSetting
     * @param async 是否异步处理
     */
    public abstract void doAutoTask(String processId, OodmSettingDTO oodmSetting, boolean async);
}
