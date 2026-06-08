package com.supcon.supfusion.base.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lcs
 * @Date 2020-08-18 16:25
 */
@Data
public class StaffVO{
    private String code;// 员工编号
    private String name;// 姓名
    private Date createTime;
    private Date modifyTime;
    private Long id;
    private Integer version;
    private StaffVO modifyStaff;

}
