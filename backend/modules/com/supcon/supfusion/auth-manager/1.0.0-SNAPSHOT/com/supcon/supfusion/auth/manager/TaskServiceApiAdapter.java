package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.flow.api.dto.TaskTotalsDTO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface TaskServiceApiAdapter {

    TaskTotalsDTO getTaskTotal(@RequestParam List<Long> ids);
}
