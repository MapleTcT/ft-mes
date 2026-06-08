/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.api;

import com.supcon.supfusion.flow.api.dto.TaskTotalsDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: zhuangmh
 * @date: 2020年9月29日 下午2:30:25
 */
@FeignClient(name = "workflow" ,contextId = "task")
public interface TaskServiceApi {

    @GetMapping("/service-api/flow-service/verificationProcessOwner")
    @ResponseBody
    Result<Boolean> verificationProcessOwner(@RequestParam("userId") Long userId , @RequestParam("pendingId") Long pendingId);

    /**
     * 查询单个/多个人的待办总数
     * @param userIds
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/service-api/flow-service/task/total")
    TaskTotalsDTO getTaskTotal(@RequestParam("userIds") List<Long> userIds);
    
}
