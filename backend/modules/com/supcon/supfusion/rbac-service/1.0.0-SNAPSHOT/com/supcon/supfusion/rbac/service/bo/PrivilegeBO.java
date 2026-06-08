package com.supcon.supfusion.rbac.service.bo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PrivilegeBO implements Serializable {

    private static final long serialVersionUID = 6720340450894845232L;
    private String menuCode;
    private String operationCode;
    private Long userId ;
    private String userName;
    private Long id;

}
