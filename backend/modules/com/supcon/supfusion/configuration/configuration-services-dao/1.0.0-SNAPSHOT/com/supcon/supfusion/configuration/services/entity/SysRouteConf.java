package com.supcon.supfusion.configuration.services.entity;


import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueIdEntity;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@javax.persistence.Entity
@Table(name = SysRouteConf.TABLE_NAME)
public class SysRouteConf extends AbstractAuditUniqueIdEntity implements Serializable {

	private static final long serialVersionUID = -2630750998776267784L;

	static final String TABLE_NAME = "sys_zuul_route_info";

	/**
	 * The path (pattern) for the route, e.g. /foo/**.
	 */
	@Column(name = "PATH")
	private String path;

	/**
	 * The service ID (if any) to map to this route. You can specify a physical URL
	 * or a service, but not both.
	 */
	@Column(name = "SERVICE_ID")
	private String serviceId;

	/**
	 * A full physical URL to map to the route. An alternative is to use a service
	 * ID and service discovery to find the physical address.
	 */
	@Column(name = "URL")
	private String url;

	/**
	 * Flag to indicate that this route should be retryable (if supported).
	 * Generally retry requires a service ID and ribbon.
	 */
	@Column(name = "RETRYABLE")
	private Boolean retryable;

	/**
	 * Flag to determine whether the prefix for this route (the path, minus pattern
	 * patcher) should be stripped before forwarding.
	 */
	@Column(name = "STRIP_PREFIX")
	private boolean stripPrefix = true;
	
//	@Column(name = "VALID")
//	private Boolean valid;


	@Override
	protected String _getEntityName() {
		// TODO Auto-generated method stub
		return getClass().getName();
	}

}
