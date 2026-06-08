package com.supcon.supfusion.auth.manager.Impl;

import com.supcon.supfusion.auth.manager.TaskServiceApiAdapter;
import com.supcon.supfusion.flow.api.TaskServiceApi;
import com.supcon.supfusion.flow.api.dto.TaskTotalsDTO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TaskServiceApiAdapterImpl implements TaskServiceApiAdapter {

    @Resource
    private TaskServiceApi taskServiceApi;

    @Override
    public TaskTotalsDTO getTaskTotal(List<Long> ids) {
        return taskServiceApi.getTaskTotal(ids);
    }
}
