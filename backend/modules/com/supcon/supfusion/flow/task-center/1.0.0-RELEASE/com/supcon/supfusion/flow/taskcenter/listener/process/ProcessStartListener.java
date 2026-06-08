/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.listener.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.supcon.supfusion.flow.common.po.DiagramPO;
import com.supcon.supfusion.flow.common.po.ProcessPO;
import com.supcon.supfusion.flow.common.util.Constants;
import com.supcon.supfusion.flow.dao.DiagramMapper;
import com.supcon.supfusion.flow.dao.ProcessMapper;
import com.supcon.supfusion.flow.engine.server.listener.AbstractProcessStartListener;
import com.supcon.supfusion.flow.engine.server.service.ProcessEngineService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;

/**
 * @author: zhuangmh
 * @date: 2020年11月4日 下午5:09:51
 */
@Component
public class ProcessStartListener extends AbstractProcessStartListener {

    @Autowired
    private ProcessEngineService processEngineService;
    @Autowired
    private ProcessMapper processMapper;
    @Autowired
    private DiagramMapper diagramMapper; 
    /**
     * @see com.supcon.supfusion.flow.engine.server.listener.AbstractProcessStartListener#createProcess(java.lang.String)
     */
    @Override
    public void createProcess(String processId, String processKey, String processName, int processVersion) {
        Long userId = UserContext.getUserContext().getUserId();
        String staffName = UserContext.getUserContext().getStaffName();
        String tenantId = RpcContext.getContext().getTenantId();
        DiagramPO diagram = diagramMapper.selectSingle(processKey, processVersion, tenantId);
        String formNo = processEngineService.getVariableValue(processId, Constants.FORM_NO, String.class);
        ProcessPO process = new ProcessPO();
        process.setId(Long.parseLong(processId));
        process.setStaffName(staffName);
        if (diagram != null) {
            process.setAppId(diagram.getAppId());
            process.setCid(diagram.getCid());
        }
        process.setTableNo(formNo);
        process.setProcessKey(processKey);
        process.setProcessName(processName);
        process.setProcessVersion(processVersion);
        process.setTenantId(tenantId);
        process.setUserId(userId);
        processMapper.insert(process);
    }

}
