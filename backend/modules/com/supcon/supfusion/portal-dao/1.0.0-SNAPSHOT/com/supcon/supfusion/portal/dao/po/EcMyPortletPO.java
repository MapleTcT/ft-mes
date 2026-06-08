package com.supcon.supfusion.portal.dao.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.cloud.common.pojo.PO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author kk.C
 * @Description: 用户自定义首页门户组态PO类
 * @Date 2020/11/30 16:31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = EcMyPortletPO.TABLE_NAME)
public class EcMyPortletPO extends PO {

    public static final String TABLE_NAME = "ec_my_portlet";

    @TableId
    private Long id;
    private Long userId;
    private String config;

}
