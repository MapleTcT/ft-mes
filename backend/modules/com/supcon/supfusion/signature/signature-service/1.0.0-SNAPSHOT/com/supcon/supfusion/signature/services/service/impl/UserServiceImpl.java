package com.supcon.supfusion.signature.services.service.impl;


import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.PasswordDTO;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.signature.services.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zhang yafei
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserApiService userApiService;


    @Override
    public Boolean checkUserName(String name) {
        boolean checkFlag = false;
        try {
            Result<UserOrgDetailDTO> userOrgDetailByName = userApiService.getUserOrgDetailByName(name);
            if (userOrgDetailByName != null
                    && userOrgDetailByName.getData() != null
                    && StringUtils.isNotBlank( userOrgDetailByName.getData().getUserName())) {
                checkFlag = true;
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return checkFlag;
    }

    @Override
    // @AuditLog(desc="检查用户密码是否正确",operType="5")
    public Boolean checkUserPassword(String name, String password) {
        try {
            PasswordDTO passwordDTO = new PasswordDTO(name, password);
            Result<Boolean> result = userApiService.checkPassword(passwordDTO);
            if (result != null) {
                return result.getData() == null ? false : result.getData();
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
        return false;
    }
}
