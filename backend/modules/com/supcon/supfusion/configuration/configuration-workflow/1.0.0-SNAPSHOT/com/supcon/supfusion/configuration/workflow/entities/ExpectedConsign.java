package com.supcon.supfusion.configuration.workflow.entities;

/**
 * @author qy
 * copy by dhy
 */

import com.supcon.supfusion.framework.scaffold.hibernate.id.SnowFlakeIDGenerator;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "WF_EXPECTED_CONSIGN")
public class ExpectedConsign {

    private static final long serialVersionUID = 8760969936036623450L;
    @Id
    @GenericGenerator(name = SnowFlakeIDGenerator.GENERATOR_NAME, strategy = SnowFlakeIDGenerator.STRATEGY)
    @GeneratedValue(generator = SnowFlakeIDGenerator.GENERATOR_NAME)
    private Long id;
    private Long userId;//委托人id--userId
    private Long consignorId;//被委托人的userId
    private String consignorName;
    private Long consignorStaffId;//被委托人的staffID
    private Date createDate;//委托时间
    private Date startDate;//委托的开始时间
    private Date endDate;//委托的结束时间
    private Boolean valid;
    private String memo;//委托说明
    private String flowVersion;//流程版本
    private String flowKey;//流程key
    private String activeCode;//活动编码，也是操作编码，二者相同
    @Transient
    private String consignorStaffName;//被委托人名称
    @Transient
    private String activeName;
    @Transient
    private String flowName;
    @Transient
    private String staffName;//委托人名称
    private String type; //all 委托全部
    private Boolean recallFlag;

}