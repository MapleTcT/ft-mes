package com.supcon.supfusion.authkeycloak.http;


import com.supcon.supfusion.authkeycloak.entity.UserEntity;
import lombok.Data;

@Data
public class ResponseEntity {
    private static final long serialVersionUID = 2368049747710991182L;
    private UserEntity data;
    private Integer code;
    private String message;

}
