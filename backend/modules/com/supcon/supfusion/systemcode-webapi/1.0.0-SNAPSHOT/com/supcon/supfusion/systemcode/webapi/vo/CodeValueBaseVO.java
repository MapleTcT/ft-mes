package com.supcon.supfusion.systemcode.webapi.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CodeValueBaseVO extends VO {

    private static final long serialVersionUID = 6335107183711459532L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 系统字典项编码
     */
    private String entityCode;

    /**
     * 值的编码
     */
    private String code;

    /**
     * 编码值
     */
    private String value;

    /**
     * 名称全路径
     */
    private String fullPathName;

    /**
     * 是否叶子节点
     */
    private boolean leaf;

    /**
     * 所属公司ID
     */
    private Long cid;

    /**
     * 父节点ID
     */
    private String parentId;

    /**
     * 父节点编码拼接
     */
    private String parentCodeStr;

    /**
     * 层级
     */
    private Integer layNo;

    /**
     * 层级全路径
     */
    private String layRec;

    /**
     * 序列号id
     */
    private Long seqId;

    /**
     * 备注
     */
    private String memo;

    private List<CodeValueBaseVO> children2;

}
