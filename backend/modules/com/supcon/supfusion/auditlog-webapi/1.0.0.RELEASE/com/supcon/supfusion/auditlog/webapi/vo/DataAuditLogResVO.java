package com.supcon.supfusion.auditlog.webapi.vo;

import com.supcon.supfusion.systemcode.api.dto.SystemCodeResultDTO;
import lombok.Data;

/**
 * @Author: xionghangfeng
 * @Description:
 * @Date: 2021/3/18 15:12
 * @Version: 1.0
 */
@Data
public class DataAuditLogResVO {
    /**
     * 链路跟踪ID
     */
    private Long traceId;

    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 操作用户名称
     */
    private String operateUserName;

    /**
     * 操作时间
     */
    private String operateTime;

    /**
     * 操作类型
     */
    private SystemCodeResultDTO operateType;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件地址
     */
    private String fileUrl;
}
