package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.*;

/**
 * @Description: TODO
 * @Version V1.0
 * @Auther: huning
 * @Date: 2020/7/20
 */
@Data
@Entity
@Immutable
@Table(name = User.TABLE_NAME)
public class User extends AbstractAuditUniqueIdEntity implements Serializable {

    public static final String TABLE_NAME = "base_userinfo";

    private static final long serialVersionUID = -4281411999693166020L;
    private String name;// 用户名
    private String password;// 密码
    @OneToOne(optional = true)
    @JoinColumn(name = "STAFF_ID")
    @Fetch(FetchMode.SELECT)
    private Staff staff;
    @Transient
    private String language;// 语言


    @Override
    protected String _getEntityName() {
        return null;
    }
}
