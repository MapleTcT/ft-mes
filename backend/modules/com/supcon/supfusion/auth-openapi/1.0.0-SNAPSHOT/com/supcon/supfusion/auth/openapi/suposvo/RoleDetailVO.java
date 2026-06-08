package com.supcon.supfusion.auth.openapi.suposvo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@ToString
public class RoleDetailVO extends VO {

    private String name;

    private String showName;

    private String description;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<RoleDetailVO.Resource> resources;

    private String createTime;

    private String createUsername;

    private String modifyTime;

    private String modifyUsername;

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
