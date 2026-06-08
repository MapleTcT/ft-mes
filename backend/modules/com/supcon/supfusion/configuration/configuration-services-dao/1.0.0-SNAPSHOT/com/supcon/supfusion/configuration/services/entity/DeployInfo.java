package com.supcon.supfusion.configuration.services.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
//@Table(name = DeployInfo.TABLE_NAME)
public class DeployInfo implements Serializable {

    private static final long serialVersionUID = 3047829161895387082L;

    public static final String TABLE_NAME = "ec_deploy_info";

    @Id
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date modifyTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date deleteTime;

    private Long createStaffId;

    private Long modifyStaffId;

    private Long deleteStaffId;

    @Column(name = "MODULE_CODE")
    private String moduleCode;

    @Column(name = "MODULE_NAME")
    private String moduleName;

    private String status;

    private String tasks;

    private String logfilePrefix;

    @Column(name = "VALID", columnDefinition = "INTEGER", length = 1, nullable = false)
    private boolean valid = true;

    @Column(name = "DEPLOY_USER")
    private String deployUser;//发布人

    private Long totalTime;//任务总时

    private String curVersion;//模块发布时候的版本

    @Version
    private int version;


    public DeployInfo(String moduleCode) {
        this.moduleCode = moduleCode;
    }


    /**
     * todo
     *
     * @Column(name = "VALID", columnDefinition = "INTEGER", length = 1, nullable = false)
     * public boolean isValid() {
     * return this.valid;
     * }
     */


    public String toJsonString() {
        return "{id:" + id + ", moduleCode:'" + moduleCode
                + "', createTime:'" + getCreateTime()
                + "', tasks:'" + getTasks() + "', status:'" + getStatus() + "', logFilePrefixe:" + getLogfilePrefix() + "'}";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DeployInfo other = (DeployInfo) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }


}