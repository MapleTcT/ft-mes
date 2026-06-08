package com.supcon.supfusion.configuration.workflow.entities;

import com.supcon.supfusion.framework.scaffold.hibernate.id.SnowFlakeIDGenerator;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "WF_FLOW_HISTORY")
public class FlowHistory {


	@Id
	@GenericGenerator(name = SnowFlakeIDGenerator.GENERATOR_NAME, strategy = SnowFlakeIDGenerator.STRATEGY)
	@GeneratedValue(generator = SnowFlakeIDGenerator.GENERATOR_NAME)
	private Long id;
	private String processKey; //流程key
	private int processVersion;
	@Column(name = "FLOW_XML")
	private String flowXML;
	private Date publishTime;
	private Long deploymentId;
	private Long staffId;
	private String publishType;
	
}