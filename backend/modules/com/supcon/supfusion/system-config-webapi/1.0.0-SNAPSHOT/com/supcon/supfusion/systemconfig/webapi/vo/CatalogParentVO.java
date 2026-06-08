package com.supcon.supfusion.systemconfig.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.swing.*;
import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CatalogParentVO extends VO {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String parentId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String parentName;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String parentCode;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String moduleCode;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String id;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String name;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String code;

}
