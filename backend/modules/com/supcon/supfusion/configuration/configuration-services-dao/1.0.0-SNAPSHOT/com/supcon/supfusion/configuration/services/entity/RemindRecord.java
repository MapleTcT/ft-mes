/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.User;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;

@Data
@javax.persistence.Entity
@Table(name = RemindRecord.TABLE_NAME)
public class RemindRecord extends AbstractAuditUniqueIdEntity implements Serializable {

    private static final long serialVersionUID = 4598458002801085174L;
    public static final String TABLE_NAME = "ec_remind_record";
    private String tableNo;// 单据编号
    private String remindStaffName;// 被催办人
    @ManyToOne(targetEntity = User.class)
    @JoinColumn(name = "SEND_USER_ID")
    @Fetch(FetchMode.SELECT)
    private User sendUser;// 发起催办的人
    private String entityCode;// 实体code
    private Long tableInfoId;// 表单id
    private Date remindTime;//
    protected String activityName;// 活动名称，在一个流程内唯一
//    @BAPInternational(replace = true)
    private String taskDescription;// 活动描述
    private String processKey;
    private Integer processVersion;
    private String remindContent;


    @Override
    protected String _getEntityName() {
        return RemindRecord.class.getName();
    }

}