package com.supcon.supfusion.auth.openapi.suposvo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
public class UserDetailVO extends VO {

    private String username;

    private String userDesc;

    private String email;

    private List<Role> userRoleList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Role implements Serializable {

        private String name;

        private String showName;

        private String description;

        private String createTime;

        private String createUsername;

        private String modifyTime;

        private String modifyUsername;

    }

}
