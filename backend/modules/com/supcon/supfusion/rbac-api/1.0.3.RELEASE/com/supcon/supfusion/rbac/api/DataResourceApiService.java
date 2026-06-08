package com.supcon.supfusion.rbac.api;

import com.supcon.supfusion.rbac.api.dto.UserDataResourceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.supcon.supfusion.rbac.api.Constants.Constants.API_PREFIX;

@FeignClient(name = "rbac", contextId = "DataResourceApiService")
public interface DataResourceApiService {

    @GetMapping(value = API_PREFIX + "/{userId}/data/resources/{resServiceCode}")
    @ResponseBody
    UserDataResourceDTO queryDataResourceByUser(@PathVariable Long userId,
                                                @PathVariable String resServiceCode,
                                                @RequestParam(required = true) Long cid,
                                                @RequestParam(required = false) String resKey,
                                                @RequestParam(required = false) String resType);
}
