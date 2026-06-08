package com.supcon.supfusion.configuration.services.entity;

import java.io.Serializable;

/**
 * @author fangzhibin
 * @since 2.2
 */
public interface IEcCodeEntity<PK extends Serializable> extends Serializable {

    /**
     * Property which represents id.
     */
    String P_CODE = "CODE";

    String P_VERSION = "VERSION";

    /**
     * Get primary key.
     *
     * @return primary key
     */
    PK getCode();

    /**
     * Set primary key.
     *
     * @param code primary key
     */
    void setCode(PK code);

    int getVersion();

    void setVersion(Integer version);


}

