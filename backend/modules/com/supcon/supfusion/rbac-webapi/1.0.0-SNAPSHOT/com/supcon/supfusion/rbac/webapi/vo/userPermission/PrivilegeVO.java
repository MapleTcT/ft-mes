package com.supcon.supfusion.rbac.webapi.vo.userPermission;

import lombok.Data;

import java.io.Serializable;

@Data
public class PrivilegeVO implements Serializable {

    private static final long serialVersionUID = -1998230510210029735L;
    private String menuCode;
    private String operationCode;
    private Long userId ;
    private String userName;
    private Long id;

}
