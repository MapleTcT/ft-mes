package com.supcon.supfusion.auth.service.bo.bap;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * bap系统编码
 *
 * @author caokele
 */
@Data
@Accessors(chain = true)
public class BapSystemCodeBO {
    private Long id;
    /**
     * 值
     */
    private String value;
    /**
     * 类型
     */
    private String type;
    /**
     * 编码
     */
    private String code;
    /**
     * 实体编码
     */
    private String entityCode;

    /**
     * 是否是叶子节点
     */
    private boolean attribute;
    /**
     * 备注
     */
    private String memo;
    /**
     * 对应的中国区域的值
     */
    private String zhCnValue;

    private String codeDesC;
    private String codeDesB;
    private String codeDesA;
    private Boolean defaultFlag;
}
