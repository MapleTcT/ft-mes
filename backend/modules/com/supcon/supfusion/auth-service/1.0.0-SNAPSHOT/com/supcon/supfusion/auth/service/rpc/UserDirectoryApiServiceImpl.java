package com.supcon.supfusion.auth.service.rpc;

import com.supcon.supfusion.auth.api.UserDirectoryApiService;
import com.supcon.supfusion.auth.api.dto.UserDirectoryAuthenticateDTO;
import com.supcon.supfusion.auth.service.UserDirectoryService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.controller.BaseController;

import javax.annotation.Resource;

/**
 * @author caokele
 */
@ServiceApiService
public class UserDirectoryApiServiceImpl extends BaseController implements UserDirectoryApiService {

    @Resource
    private UserDirectoryService userDirectoryService;

    @Override
    public void authenticateUserDirectory(UserDirectoryAuthenticateDTO userDirectoryAuthenticateDTO) {
        userDirectoryService.authenticateUserDirectory(userDirectoryAuthenticateDTO.getUserName(), userDirectoryAuthenticateDTO.getPassword());
    }
}
