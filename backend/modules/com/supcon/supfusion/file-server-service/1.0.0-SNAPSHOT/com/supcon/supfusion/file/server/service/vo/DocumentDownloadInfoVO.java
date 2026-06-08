package com.supcon.supfusion.file.server.service.vo;


import com.supcon.supfusion.file.server.dao.po.DocumentPO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
@ApiModel(description = "下载详情")
public class DocumentDownloadInfoVO implements Serializable {

    private static final long serialVersionUID = 7097916839851820072L;

    private long id;

    private long documentId;

    private DocumentPO documentPO;

    private String downloadStaffId; // 下载人 对应 员工编号

    private String downloadStaff; // 下载人

    private Long downloadTime; // 下载时间

    private String ipAddr;

    private String recordType; //记录类型

}
