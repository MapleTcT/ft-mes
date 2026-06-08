package com.supcon.supfusion.configuration.services.entity;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import java.beans.Transient;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/24
 */
@MappedSuperclass
public abstract class CodeEntity implements IEntity<String> {
    /**
     *
     */
    private static final long serialVersionUID = -6019660022477808873L;
    protected String id;
    protected int version;

    @Transient
    protected abstract String _getEntityName();

    @Override
    @Id
    public String getId() {
        if (null == id) {
            id = getUniqueCode();
        }
        return id;
    }

    @Transient
    protected String getUniqueCode() {
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Version
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String name = _getEntityName();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        if (!(obj instanceof CodeEntity)) {
            return false;
        }

        if (_getEntityName().equals(((CodeEntity) obj)._getEntityName())) {
            CodeEntity other = (CodeEntity) obj;
            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.getId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return _getEntityName() + " [id=" + id + "]";
    }
}

