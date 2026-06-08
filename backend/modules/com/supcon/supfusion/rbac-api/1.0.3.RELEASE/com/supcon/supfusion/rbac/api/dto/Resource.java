package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

@Data
public class Resource {
    private Long id;
    private Long parentId;
    private String resourceOrder;
    private String name;
    private String description;
    private String resourceFunctionType;
    private String resourceCode;
    private String parentCode;
    private Integer hide;
}
