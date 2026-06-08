package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.*;

/**
 * @author lifangyuan
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = DashboardConfigPO.TABLE_NAME, autoResultMap = true)
public class DashboardConfigPO extends PO {
    public static final String TABLE_NAME = "auth_user_config_dashboard";
    private Long userId;
    private String mkey;
    private String fields;
    private String configInfo;
}
