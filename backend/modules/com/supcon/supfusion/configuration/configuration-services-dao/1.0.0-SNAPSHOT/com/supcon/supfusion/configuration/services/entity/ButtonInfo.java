package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractCodeEntity;
import lombok.Data;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
@javax.persistence.Entity
@Table(name = ButtonInfo.TABLE_NAME)
public class ButtonInfo extends AbstractCodeEntity implements Serializable {

    private static final long serialVersionUID = 7931947798133902884L;
    public static final String TABLE_NAME = "ec_buttoninfo";
    @Transient
    protected EcEnv ecEnv = EcEnv.product;
    private String name; //按钮名称
    private String url;  //按钮url
    private String iconCls; //按钮图标
    private String nameSpace;//按钮图标
    private String action;
    private String entityCode;
    private String viewCode;

    @Override
    protected String _getEntityName() {
        return ButtonInfo.class.getName();
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        String name = ButtonInfo.class.getName();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((getCode() == null) ? 0 : getCode().hashCode());
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
        if (!(obj instanceof ButtonInfo)) {
            return false;
        }
        ButtonInfo other = (ButtonInfo) obj;
        if (getCode() == null) {
            if (other.getCode() != null) {
                return false;
            }
        } else if (!getCode().equals(other.getCode())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ButtonInfo [code=" + getCode() + "]";
    }

}
