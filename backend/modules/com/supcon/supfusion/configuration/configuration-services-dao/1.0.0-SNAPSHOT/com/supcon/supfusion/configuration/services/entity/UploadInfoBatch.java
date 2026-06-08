package com.supcon.supfusion.configuration.services.entity;

import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.id.SnowFlakeIDGenerator;
import lombok.Data;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * 上载记录批量对象
 * 
 * 
 * @author zhushizhang
 * @version $Id$
 */
@Data
@javax.persistence.Entity
@Table(name = UploadInfoBatch.TABLE_NAME)
public class UploadInfoBatch implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3367590993476086513L;
	public static final String TABLE_NAME = "ec_upload_info_batch";
	@Id
	@GenericGenerator(name = SnowFlakeIDGenerator.GENERATOR_NAME, strategy = SnowFlakeIDGenerator.STRATEGY)
	@GeneratedValue(generator = SnowFlakeIDGenerator.GENERATOR_NAME)
	private Long id;
	@Column(name = "DES")
	private String describe;//上载描述
	private Date uploadDate;//上载时间
	@OneToOne
	@JoinColumn(name = "UPLOAD_STAFF", referencedColumnName="ID")
	@Fetch(FetchMode.SELECT)
	private Staff uploadStaff;//上载人
	private String totalTime;//上载总时长
	private String uploadState;//上载状态
	private Integer moduleSize;//上载包数量
	private String uploada;//备用字段A
	private String uploadb;//备用字段B
	private String uploadc;//备用字段C
	private String uploadd;//备用字段D
	private String uploade;//备用字段E


}