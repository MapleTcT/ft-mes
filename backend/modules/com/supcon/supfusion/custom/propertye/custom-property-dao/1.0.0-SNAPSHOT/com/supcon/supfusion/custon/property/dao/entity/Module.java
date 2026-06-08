package com.supcon.supfusion.custon.property.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.custon.property.common.enums.EcEnv;
import com.supcon.supfusion.custon.property.common.i18n.NameInternationalSerialzer;
import com.supcon.supfusion.custon.property.dao.entity.base.LogicBasePO;
import lombok.Data;

import java.util.Date;

/**
 * 实体配置：模块
 * 
 * 
 * @author yaowei
 * @version $Id$
 */

@Data
@TableName(value = "ec_module", autoResultMap=true)
public class Module extends LogicBasePO {

	private static final long serialVersionUID = 7635333342655214403L;
	private EcEnv ecEnv = EcEnv.product;

	@TableId
	private String code;

	@JsonSerialize(using = NameInternationalSerialzer.class)
	private String name;

	private String copyModuleCode;

	private String artifact;

	private String projectVersion;// 当前版本

	private String initialVersion;// 初始版本

	private String description;

	private String deployOrder;

	private Boolean isInherentedBase = false;// 是否固有基础类型

	private Boolean isNewGenerate = false;

	private Boolean projFlag;

	private Boolean isReadOnly = false;

	private Boolean isHide = false;

	private String category;

	private Date publishTime;

	private String type = "Mis";    // 微服务类型:Mis，老的bap模块为null

	private String acronym;		//缩略名称，做为数据库前缀

	private Boolean isProto = false;

	private Boolean mainModule = false;


}