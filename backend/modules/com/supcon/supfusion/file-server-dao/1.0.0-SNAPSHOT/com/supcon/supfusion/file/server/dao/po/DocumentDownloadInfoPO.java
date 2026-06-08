package com.supcon.supfusion.file.server.dao.po;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.supcon.supfusion.framework.scaffold.mybatis.entity.LogicDeleteBaseEntity;
import lombok.*;

import java.io.Serializable;
import java.util.Date;


@Data
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "file_server_document_down_info", autoResultMap=true)
public class DocumentDownloadInfoPO  implements Serializable {

	private static final long serialVersionUID = 7097916839851820072L;

	private long id;
	private long documentId;
	private String downloadStaffId; // 下载人 对应 员工编号
	private Date downloadTime; // 下载时间
	private String ipAddr;
	private String recordType; //记录类型
	//是否删除 1 使用 0 删除
	private String valid;
	//创建人
	private String creator;
	//修改人
	private Date modifier;
	//创建时间
	private Date createTime;
	//更新时间
	private Date modifyTime;
	//创建人员id
	private Long createStaffId;
	//修改人员id
	private Long modifyStaffId;
}
