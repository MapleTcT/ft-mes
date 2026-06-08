package com.supcon.supfusion.base.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@NoArgsConstructor
@Entity
@Immutable
@Table(name = SystemCode.TABLE_NAME)
public class SystemCode  {
    public static final String TABLE_NAME = "base_systemcode";
    @Id
    private String id;
    private String	type;
    private String	code;
    private String	entityCode;
    private String	value;
    private String	valueZhCn;
    private String	memo;
    private Long sort;
    private Long cid;

    private Boolean	defaultFlag;
    @Column(name = "CODE_DESA", length = 2000)
    private String codeDesA;
    @Column(name = "CODE_DESB", length = 2000)
    private String codeDesB;
    @Column(name = "CODE_DESC", length = 2000)
    private String codeDesC;
    private Boolean valid = true;
    private Integer version;
    private Boolean leaf = false;
    private String parentId;
    private String layRec;
    private Long seqId;
    @Transient
    private Long poId;
    @Transient
    private String fullPath;

    @Transient
    private String displayName;
    @Transient
    private String name;
    @Transient
    private String parentCode;
    @Transient
    private String parentName;

    public SystemCode(String id) {
        this.id = id;
    }

    @OneToOne(fetch= FetchType.EAGER, targetEntity=Company.class)
    @JoinColumn(name="CID", insertable=false, updatable=false)
    @Fetch(FetchMode.SELECT)
    private Company company;

    @Transient
    private SystemCode parent;

    @Transient
    protected Set<SystemCode> children = new LinkedHashSet<SystemCode>();

    private String fullPathName;
    private Integer layNo = 0;
    @Transient
    public String getUniqueCode() {
        return getEntityCode()+"/"+getCode();
    }

    public boolean isValid() {
        return valid ? true : false;
    }

    protected String _getEntityName() {
        return SystemCode.class.getName();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueZhCn() {
        return valueZhCn;
    }

    public void setValueZhCn(String valueZhCn) {
        this.valueZhCn = valueZhCn;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Long getSort() {
        return sort;
    }

    public void setSort(Long sort) {
        this.sort = sort;
    }

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public Boolean getDefaultFlag() {
        return defaultFlag;
    }

    public void setDefaultFlag(Boolean defaultFlag) {
        this.defaultFlag = defaultFlag;
    }

    public String getCodeDesA() {
        return codeDesA;
    }

    public void setCodeDesA(String codeDesA) {
        this.codeDesA = codeDesA;
    }

    public String getCodeDesB() {
        return codeDesB;
    }

    public void setCodeDesB(String codeDesB) {
        this.codeDesB = codeDesB;
    }

    public String getCodeDesC() {
        return codeDesC;
    }

    public void setCodeDesC(String codeDesC) {
        this.codeDesC = codeDesC;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getLeaf() {
        return leaf;
    }

    public void setLeaf(Boolean leaf) {
        this.leaf = leaf;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getLayRec() {
        return layRec;
    }

    public void setLayRec(String layRec) {
        this.layRec = layRec;
    }

    public Long getSeqId() {
        return seqId;
    }

    public void setSeqId(Long seqId) {
        this.seqId = seqId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public SystemCode getParent() {
        return parent;
    }

    public void setParent(SystemCode parent) {
        this.parent = parent;
    }

    public Set<SystemCode> getChildren() {
        return children;
    }

    public void setChildren(Set<SystemCode> children) {
        this.children = children;
    }

    public String getFullPathName() {
        return fullPathName;
    }

    public void setFullPathName(String fullPathName) {
        this.fullPathName = fullPathName;
    }

    public Integer getLayNo() {
        return layNo;
    }

    public void setLayNo(Integer layNo) {
        this.layNo = layNo;
    }

    public Long getPoId() {
        return poId;
    }

    public void setPoId(Long poId) {
        this.poId = poId;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }
}
