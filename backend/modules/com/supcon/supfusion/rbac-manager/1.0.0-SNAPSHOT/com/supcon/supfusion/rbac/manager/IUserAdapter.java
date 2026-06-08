package com.supcon.supfusion.rbac.manager;

import com.supcon.supfusion.auth.api.dto.UserDetailDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;

import java.util.List;
import java.util.Map;

public interface IUserAdapter {
    Map<Long, UserDetailDTO> getUserIdByPersonId(String personIds);

    List<Long> getCompanyIdByUserName(String userName);

    List<Long> getCompanyIdByUserId(Long userId);

    Result<Boolean> bindRole(Long roleId,List<Long> userIds,boolean add);
}
