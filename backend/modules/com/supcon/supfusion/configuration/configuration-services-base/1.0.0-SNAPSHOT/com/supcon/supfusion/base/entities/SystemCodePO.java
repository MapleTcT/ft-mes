package com.supcon.supfusion.base.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据字典某一类下具体的编码和值的描述类
 *
 * @author root
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SystemCodePO.TABLE_NAME)
public class SystemCodePO {
    public static final String TABLE_NAME = "sys_code";
    private static final long serialVersionUID = 6755105232352834077L;

    /**
     * 主键ID
     */
    @Id
    @Column(name="ID")
    private Long id;

    /**
     * 是否有效
     */
    @Column(name="VALID")
    private Integer valid;

    /**
     * 系统字典项编码
     */
    @Column(name="ENTITY_CODE")
    private String entityCode;

    /**
     * 值的编码
     */
    @Column(name="CODE")
    private String code;

    /**
     * 显示名称
     */
    @Column(name="DISPLAY_NAME")
    private String displayName;

    /**
     * 值的名称,国际化键
     */
    @Column(name="NAME")
    private String name;

    /**
     * 所属公司ID
     */
    @Column(name="CID")
    private Long cid;

    /**
     * 所属公司名称
     */
    @Transient
    private String companyName;

    /**
     * 父节点ID
     */
    @Column(name="PARENT_ID")
    private Long parentId;

    /**
     * 父节点名称
     */
    @Column(name="PARENT_NAME")
    private String parentName;

    @Transient
    private String parentCode;

    /**
     * 父节点显示名称
     */
    @Transient
    private String parentDisplayName;

    /**
     * 层级
     */
    @Column(name="LAY_NO")
    private Integer layNo;

    /**
     * 序列号id
     */
    @Column(name="SEQ_ID")
    private Long seqId;

    /**
     * 备注
     */
    @Column(name="MEMO")
    private String memo;

    /**
     * 描述C
     */
    @Column(name="DES_C")
    private String desC;

    /**
     * 描述B
     */
    @Column(name="DES_B")
    private String desB;

    /**
     * 描述A
     */
    @Column(name="DES_A")
    private String desA;

    /**
     * 顺序
     */
    @Column(name="SORT")
    private Double sort;

    /**
     * 是否默认
     */
    @Column(name="DEFAULT_FLAG")
    private Integer defaultFlag;

    /**
     * 类型
     */
    @Column(name="TYPE")
    private String type;

    @Column(name="LEAF")
    private Boolean leaf;

    @Column(name="LAY_REC")
    private String layRec;

    /**
     * 层级全路径
     */
    @Column(name="FULL_PATH")
    private String fullPath;

    @Column(name = "FULL_PATH_NAME")
    private String fullPathName;

    @Column(name="ROW_VERSION")
    private Long rowVersion;

    @Transient
    private String prevId;

    @Transient
    private String nextId;

    @Transient
    private String currentId;

    /**
     * 编码值子节点
     */
    @Transient
    private List<SystemCodePO> children;

    /**
     * 提供给baseService前端服务使用
     */
    @Transient
    private List<SystemCodePO> children2 = new ArrayList<>();
}
