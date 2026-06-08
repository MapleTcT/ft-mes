package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;

@Data
@TableName(value = "sys_scripts_version", autoResultMap = true)
public class UpgradeFlagPO extends PO {
    /**复用sys_scripts_version ,判断2.9to3.0的升级状态*/
    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    @TableId(value = "application_name")
    private String applicationName;


    @TableId(value = "script_file_name")
    private String scriptFileName;


    @TableId(value = "current_version")
    private String currentVersion;

    @TableId(value = "create_time")
    private String createTime;
}