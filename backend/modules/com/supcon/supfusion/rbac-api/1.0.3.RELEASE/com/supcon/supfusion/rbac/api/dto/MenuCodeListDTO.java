package com.supcon.supfusion.rbac.api.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class MenuCodeListDTO {
    /**
     * 菜单根节点code
     * 删除app则是app根菜单code
     * 删除folder则是folder根菜单code
     */
    @NotNull
    private String rootCode;
    /**
     * 全部菜单code列表
     */
    private List<String> codeList;
}
