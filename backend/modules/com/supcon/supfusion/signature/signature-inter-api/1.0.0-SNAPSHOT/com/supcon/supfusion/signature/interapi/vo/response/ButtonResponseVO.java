package com.supcon.supfusion.signature.interapi.vo.response;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.signature.interapi.vo.PersonVo;
import com.supcon.supfusion.signature.interapi.vo.PositionVo;
import com.supcon.supfusion.signature.interapi.vo.RoleVo;
import lombok.Data;

import java.util.List;

/**
 * @author zhang yafei
 */
@Data
public class ButtonResponseVO extends VO {

    private String code;
    private String name;
    private String displayName;
    private  String signerId;
    private List<PersonVo> persons;
    private  String positionId;
    private List<PositionVo> positions;
    private  String roleId;
    private List<RoleVo> roles;
    private String  releaseFelid;
    private  String powerType;
    private  Boolean signatureEnabled;
    private  String  signatureType;
    private String signatureDescrible;
    private String moduleCode;
    private String entityCode;

    public Boolean getSignatureEnabled() {
        return signatureEnabled == null ? false : signatureEnabled;
    }
}
