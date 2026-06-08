package com.supcon.supfusion.auth.openapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
public class UserDetailVO extends VO {

    private String username;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String userDesc;

    private Integer accountType;

    private Integer lockStatus;

//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    private String mobile;
//
//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    private String email;
//
//    @JsonInclude(JsonInclude.Include.NON_EMPTY)
//    private String faceUrl;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personCode;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String personName;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String createTime;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String modifyTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Role> userRoleList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Role implements Serializable {

        private String name;

        private String showName;
    }

}
