package com.supcon.supfusion.systemconfig.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author lifangyuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CatalogNameVO extends VO {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String catalogId;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String name;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String code;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String moduleCode;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CatalogNameVO> catalog;
}

