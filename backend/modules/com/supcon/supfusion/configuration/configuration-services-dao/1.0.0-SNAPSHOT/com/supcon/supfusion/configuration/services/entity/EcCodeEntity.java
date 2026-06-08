package com.supcon.supfusion.configuration.services.entity;


import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Version;


/**
 * 统一EntityConfig模块的实体主键的Entity基类.
 *
 * 统一定义主键Code的属性名称、数据类型、列名映射. <br/>
 * 子类可以重载{@link #getCode()}方法重定义Code的列名映射.<br/>
 * Version使用@Version来进行乐观锁控制.<br/>
 *
 * @author fangzhibin
 * @since 2.2
 */
@MappedSuperclass
public abstract class EcCodeEntity implements IEcCodeEntity<String>, IEcEnv{

    private static final long serialVersionUID = -6953981240825186613L;
    protected String code;
    protected EcEnv ecEnv = EcEnv.product;
    protected int version;

    @Transient
    protected abstract String _getEntityName();

    @Override
    @Id
    @Column(name = "CODE", length=2000)
    public String getCode() {
        if (code == null || code.length() == 0) {
            return null;
        }
        return code;
    }

    @Override
    public void setCode(String code) {
        if (code == null || code.length() == 0) {
            this.code = null;
        }
        this.code = code;
    }

    @Override
    @Enumerated(EnumType.STRING)
    public EcEnv getEcEnv() {
        return ecEnv;
    }

    @Override
    public void setEcEnv(EcEnv ecEnv) {
        this.ecEnv = ecEnv;
    }

    @Override
    @Version
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String name = _getEntityName();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((code == null) ? 0 : code.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EcCodeEntity)) {
            return false;
        }

        if (_getEntityName().equals(((EcCodeEntity) obj)._getEntityName())) {
            EcCodeEntity other = (EcCodeEntity) obj;
            if (code == null) {
                if (other.code != null) {
                    return false;
                }
            } else if (!code.equals(other.getCode())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return _getEntityName() + " [code=" + code + "]";
    }
}

