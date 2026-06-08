package com.supcon.supfusion.configuration.services.entity;

import java.io.Serializable;

/**
 * @Description:
 * @Version
 * @Auther: xiakaili
 * @Date: 2021/2/24
 */
public interface IEntity<PK extends Serializable> extends Serializable {

    /**
     * Property which represents id.
     */
    String P_ID = "ID";

    String P_VERSION = "VERSION";

    /**
     * Get primary key.
     *
     * @return primary key
     */
    PK getId();

    /**
     * Set primary key.
     *
     * @param id primary key
     */
    void setId(PK id);

    public int getVersion();

    public void setVersion(int version);


}

