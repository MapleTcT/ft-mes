/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.supcon.supfusion.flow.common.dto.OodmSettingDTO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.engine.server.delegate.AbstractAutoServiceDelegate;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.flow.taskcenter.job.AutoTaskServiceJob;
import com.supcon.supfusion.flow.taskcenter.rpc.OodmServiceAdapter;

/**
 * @author: zhuangmh
 * @date: 2021年1月19日 下午7:40:32
 */
@Service("autoService")
public class AutoTaskService extends AbstractAutoServiceDelegate {

    @Autowired
    private AutoTaskServiceJob autoTaskServiceJob;
    @Autowired
    private OodmServiceAdapter oodmServiceAdapter;
    @Autowired
    private ProcessEngineService processEngineService;
    /*
     * @see com.supcon.supfusion.flow.engine.server.delegate.AbstractAutoServiceDelegate#doAutoTask(com.supcon.supfusion.flow.common.dto.OodmSettingDTO, boolean)
     */
    @Override
    public void doAutoTask(String processId, OodmSettingDTO oodmSetting, boolean async) {
        if (async) {
            autoTaskServiceJob.submit(oodmSetting);
        } else {
            String formData = processEngineService.getVariableValue(processId, Constants.FORM_DATA, String.class);
            JsonObject executeResult = oodmServiceAdapter.executeService(null, null, formData, oodmSetting);
            if (executeResult != null) {
                // TODO 不生效
                processEngineService.setVariable(processId, Constants.FORM_DATA, executeResult.toString());
            }
        }

    }

}
