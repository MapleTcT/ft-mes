package com.supcon.supfusion.authkeycloak.http;


import com.supcon.supfusion.authkeycloak.entity.CompanyEntity;
import lombok.Data;

import java.util.List;

@Data
public class CompanyResponseEntity {
    private static final long serialVersionUID = 2368049747710991182L;
    List<CompanyEntity> list;
    private Integer code;
    private String message;
}
