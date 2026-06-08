/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.supcon.supfusion.flow.common.dto.OodmSettingDTO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.common.util.LocalContext;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.flow.taskcenter.rpc.OodmServiceAdapter;

/**
 * @author: zhuangmh
 * @date: 2020年6月10日 上午10:14:04
 */
@Component
public class AutoTaskServiceJob extends JobExecutor<OodmSettingDTO> {
    
    @Autowired
    private OodmServiceAdapter oodmServiceAdapter;
    @Autowired
    private ProcessEngineService processEngineService;
    /**
     * 异步执行自动服务脚本
     */
    @Override
    public void submit(OodmSettingDTO oodmSetting) {
        String processId = LocalContext.getContext().getProcessId();
        JOB_THREAD_POOL.execute(() -> {
            String formData = processEngineService.getVariableValue(processId, Constants.FORM_DATA, String.class);
            JsonObject executeResult = oodmServiceAdapter.executeService(null, null, formData, oodmSetting);
            if (executeResult != null) {
                processEngineService.setVariable(processId, Constants.FORM_DATA, executeResult.toString());
            }
        });
    }

}
