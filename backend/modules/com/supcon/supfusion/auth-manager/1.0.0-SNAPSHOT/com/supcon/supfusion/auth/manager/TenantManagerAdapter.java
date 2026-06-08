package com.supcon.supfusion.auth.manager;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.tenant.api.TenantManagerService;
import com.supcon.supfusion.tenant.api.dto.TenantDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class TenantManagerAdapter {

    @ServiceApiReference
    private TenantManagerService tenantManagerService;

    public PageResult<TenantDTO> findByPage(Integer pageNo, Integer pageSize) {
        return tenantManagerService.findByPage(pageNo, pageSize);
    }
}
