package com.supcon.supfusion.rbac.api.dto;

import com.supcon.supfusion.framework.cloud.common.pojo.DTO;
import lombok.Data;

import java.util.List;

@Data
public class RoleResourceDTO extends DTO {
    private String name;
    private String showname;
    private List<Resource> resources;
    private String createTime;
    private String createUsername;
    private String modifyTime;
    private String modifyUsername;
}
