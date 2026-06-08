package com.supcon.supfusion.auth.dao.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.auth.common.constants.Constants;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ip黑白名单表
 *
 * @author caokele
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = Constants.AUTH_IP_BLACK_WHITE, autoResultMap = true)
public class IpBlackWhitePO extends BaseEntity {
    /**
     * 主键ID
     */
    private Long id;
    /**
     * 企业Id
     */
    private Long companyId;
    /**
     * 访问IP
     */
    private String ip;
    /**
     * 管控模式 0:黑名单 1:白名单
     */
    private Integer controlType;
}
