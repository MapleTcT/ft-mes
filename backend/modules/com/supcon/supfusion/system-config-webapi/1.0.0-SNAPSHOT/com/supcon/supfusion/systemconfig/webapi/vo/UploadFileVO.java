package com.supcon.supfusion.systemconfig.webapi.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadFileVO extends VO {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String fileName;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String fileDownloadUri;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String fileType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long size;
}
