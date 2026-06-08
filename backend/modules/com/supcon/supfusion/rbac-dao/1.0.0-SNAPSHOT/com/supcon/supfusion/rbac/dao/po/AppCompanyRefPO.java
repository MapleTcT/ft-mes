package com.supcon.supfusion.rbac.dao.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;

@Data
@TableName(value = "rbac_app_company_ref", autoResultMap=true)
public class AppCompanyRefPO extends PO {

    private static final long serialVersionUID = -1250611291704649587L;

    /**
     * 主键ID
     */
    @TableId(value = "ID")
    private Long id;

    /**
     * 公司ID
     */
    @TableField("CID")
    private Long cid;

    /**
     * appId
     */
    @TableField("APPID")
    private String appId;
}
