package com.supcon.supfusion.configuration.services.openapi.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UploadInfoBatchVO extends VO implements Serializable {

    private Long id;
    private String describe;//上载描述
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadDate;//上载时间
    private Staff uploadStaff;//上载人
    private String totalTime;//上载总时长
    private String uploadState;//上载状态
    private Integer moduleSize;//上载包数量
    private String uploada;//备用字段A
    private String uploadb;//备用字段B
    private String uploadc;//备用字段C
    private String uploadd;//备用字段D
    private String uploade;//备用字段E

}
