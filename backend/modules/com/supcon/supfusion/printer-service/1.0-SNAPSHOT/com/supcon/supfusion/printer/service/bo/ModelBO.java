package com.supcon.supfusion.printer.service.bo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ModelBO {

    private String modelName;

    private String modelCode;

    private String tableName;
}
