package com.supcon.supfusion.auth.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.BO;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
public class RoleDetailBO extends BO {

    private String name;

    private String showName;

    private String description;

    private String createTime;

    private String createUsername;

    private String modifyTime;

    private String modifyUsername;

    private List<RoleDetailBO.Resource> resources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Resource implements Serializable {

        private String resourceOrder;
        private String name;
        private String description;
        private String resourceFunctionType;
        private String resourceCode;
        private Long parentId;
        private Integer hide;

    }

}
