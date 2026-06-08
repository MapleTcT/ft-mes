package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * 实体配置：模块
 *
 * @author yaowei
 * @version $Id$
 */
@Data
@javax.persistence.Entity
//@Table(name = MsModule.TABLE_NAME)
public class MsModule extends AbstractAuditUniqueCodeEntity implements Serializable {
    private static final long serialVersionUID = 7635333342655214403L;
    public static final String TABLE_NAME = "ec_msmodule";

    private String name;
    private String showname;
    private String artifact;

    @Column(name = "DESCRIPTION", length = 2000)
    private String description;
    private String deployOrder;
    private Boolean isInherentedBase = false;// 是否固有基础类型
    private Boolean isNewGenerate = false;
    private Boolean projFlag;
    @Column(name = "IS_READ_ONLY", columnDefinition = "INTEGER", length = 1)
    private Boolean isReadOnly = false;
    @Column(name = "IS_HIDE", columnDefinition = "INTEGER", length = 1)
    private Boolean isHide = false;
    @Transient
    private String iconSkin;
    @Transient
    private Boolean isPublish = false;
    private String category;
    @Transient
    private Integer deployType; //1:快速发布 2：普通发布
    @Transient
    private Integer level;        //模块依赖层级
    @Transient
    private Integer entitySize;        //实体数量
    @Transient
    private Boolean isRelation;        //是否是被依赖模块
    private Integer cpuNUM;    //CPU核数
    private String ramNUM; //内存大小
    private Integer colony; //是否集群
    private Integer status; //状态


    public Boolean getIsInherentedBase() {
        return null == isInherentedBase ? false : isInherentedBase;
    }


//	private Set<Entity> entities = new HashSet<Entity>();

//	public void addEntity(Entity entity) {
//		entities.add(entity);
//	}

//	@OneToMany(mappedBy = "module", cascade = { CascadeType.ALL }, targetEntity = Entity.class)
//	@Fetch(FetchMode.SELECT)
//	@Where(clause = "valid = 1")
//	@OrderBy(clause = "code asc")
//	public Set<Entity> getEntities() {
//		return entities;
//	}
//
//	public void setEntities(Set<Entity> entities) {
//		this.entities = entities;
//	}


    @Override
    protected String _getEntityName() {
        return getClass().getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getCode() == null) ? 0 : getCode().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MsModule other = (MsModule) obj;
        if (getCode() == null) {
            if (other.getCode() != null) {
                return false;
            }
        } else if (!getCode().equals(other.getCode())) {
            return false;
        }
        return true;
    }


    public Boolean getIsNewGenerate() {
        return null == isNewGenerate ? false : isNewGenerate;
    }

    public Boolean getIsReadOnly() {
        return null == isReadOnly ? false : isReadOnly;
    }
    
    // private Company company;
    //
    // @Override
    // @Transient
    // public Company getCompany() {
    // return company;
    // }
    //
    // @Override
    // public void setCompany(Company company) {
    // this.company = company;
    // }

}