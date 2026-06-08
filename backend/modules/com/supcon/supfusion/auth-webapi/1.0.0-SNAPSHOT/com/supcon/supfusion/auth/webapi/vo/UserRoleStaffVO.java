package com.supcon.supfusion.auth.webapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserRoleStaffVO extends VO {
    private Long cid;
    private String username;
    private String email;
    private String phone;
    private String staffCode;
    private Long staffId;
    private String staffName;
    private String timeZone;
    private Integer lockStatus;
    private Integer userType;
    private List<Role> userRoleList;
    private Integer needChangePassword;
    private String uploadUrl = "";
    private Boolean underControlled = false;
    private List<Integer> noticeConfigList = new ArrayList<>();

    private String thirdSource;

    private String thirdIdentity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Role extends VO {
        private Long cid;
        private Date createTime;
        private String name;
        private Long roleId;
        private String showName;
        private Boolean underControlled;

    }

}
