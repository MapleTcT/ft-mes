package com.supcon.supfusion.signature.services.service.impl;

import com.supcon.supfusion.auth.api.UserApiService;
import com.supcon.supfusion.auth.api.dto.UserOrgDetailDTO;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.api.PersonApiService;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;
import com.supcon.supfusion.rbac.api.IRoleUserApiService;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import com.supcon.supfusion.signature.base.i18n.SignatureInternationalResource;
import com.supcon.supfusion.signature.dao.entity.Button;
import com.supcon.supfusion.signature.services.service.ButtonService;
import com.supcon.supfusion.signature.services.service.ElectronicSignatureService;
import com.supcon.supfusion.signature.services.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author zhang yafei
 */
@Slf4j
@Service
public class ElectronicSignatureServiceImpl implements ElectronicSignatureService {


    @Autowired
    UserService userService;

    @Autowired
    UserApiService userApiService;

    @Autowired
    ButtonService buttonService;

    @Autowired
    private PersonApiService personApiService;

    @Autowired
    private IRoleUserApiService iRoleUserApiService;

    @Autowired
    SignatureInternationalResource signatureInternationalResource;

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Map<String, Object> signatureAuthenticate(Boolean isFirstSigner, String username, String password, String buttonCode) {
        Locale locale = LocaleContextHolder.getLocale();
        HashMap<String, Object> result = new HashMap<>();
        Boolean isSuccess = false;
        username = username.toLowerCase();
        // 校验用户名是否存在
        isSuccess = userService.checkUserName(username);
        result.put("isSuccess", isSuccess);
        if (!isSuccess) {
            // 用户名不存在
            result.put("msg", signatureInternationalResource.getI18nValue("com.supcon.orchid.container.exceptions.STAFF_NOT_EXIST", locale));
            return result;
        }
        isSuccess = userService.checkUserPassword(username, password);
        result.put("isSuccess", isSuccess);
        if (!isSuccess) {
            // 密码错误
            result.put("msg", signatureInternationalResource.getI18nValue("container.orchidauthentication.passworderror", locale));
            return result;
        }
        if (isFirstSigner) {
            return result;
        }
        Button btn = buttonService.getButtonByCode(buttonCode);
        String powerType = btn.getPowerType();
        if (StringUtils.isBlank(powerType)) {
            // 签名类型错误
            result.put("msg", signatureInternationalResource.getI18nValue("foundation.signature.signatureType.error", locale));
            return result;
        }
        Result<UserOrgDetailDTO> userOrgDetailByName = userApiService.getUserOrgDetailByName(username);
        UserOrgDetailDTO data = userOrgDetailByName.getData();
        if (data == null) {
            result.put("msg", signatureInternationalResource.getI18nValue("ec.view.nofieldpermission", locale));
        }
        switch (powerType) {
            case "staff":
                if (null != btn.getSignerId() && !btn.getSignerId().isEmpty()) {
                    String[] staffIds = btn.getSignerId().split(",");
                    Long personId = data.getPersonId();
                    for (String staffId : staffIds) {
                        if (personId != null && personId.toString().equals(staffId)) {
                            result.put("checkPowerSuccess", true);
                            return result;
                        }
                    }
                }
                break;
            case "position":
                if (null != btn.getPositionId() && !btn.getPositionId().isEmpty()) {
                    String[] positionIds = btn.getPositionId().split(",");
                    ListResult<PositionDetailDTO> positionDetailDTOListResult = personApiService.queryPersonPositionsByPersonId(data.getPersonId());
                    for (String positionId : positionIds) {
                        for (PositionDetailDTO positionDetailDTO : positionDetailDTOListResult.getList()) {
                            Long userPositionId = positionDetailDTO.getId();
                            if (userPositionId != null && userPositionId.toString().equals(positionId)) {
                                result.put("checkPowerSuccess", true);
                                return result;
                            }
                        }

                    }
                }
                break;
            case "role":
                if (null != btn.getRoleId() && !btn.getRoleId().isEmpty()) {
                    String[] roleIds = btn.getRoleId().split(",");
                    List<RoleDTO> roleIdList = iRoleUserApiService.findRoleUserByUserId(data.getId(), data.getCompanyId());
                    for (String roleId : roleIds) {
                        for (RoleDTO roleListId : roleIdList) {
                            if (roleListId.getId().toString().equals(roleId)) {
                                result.put("checkPowerSuccess", true);
                                return result;
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
        result.put("checkPowerSuccess", false);
        // 无权限
        result.put("msg", username + signatureInternationalResource.getI18nValue("ec.view.nofieldpermission", locale));
        return result;
    }
}
