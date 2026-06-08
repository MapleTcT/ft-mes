package com.supcon.supfusion.configuration.services.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.framework.scaffold.hibernate.entity.impl.AbstractAuditUniqueCodeEntity;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;
import java.util.Date;

/**
 * 备份视图实体
 *
 * @author fangzhibin
 */
@Entity
//@Table(name = BackupView.TABLE_NAME)
@Data
public class BackupView extends AbstractAuditUniqueCodeEntity implements Serializable {

    private static final long serialVersionUID = -1562619343557360823L;
    public static final String TABLE_NAME = "ec_backup_view";

    @ManyToOne
    @JoinColumn(name = "VIEW_CODE")
    @Fetch(FetchMode.SELECT)
    private View view;
    @OneToOne
    @JoinColumn(name = "PUBLISH_STAFF", referencedColumnName="ID")
    @Fetch(FetchMode.SELECT)
    private Staff publishStaff;//上载人
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Date publishDate;

    @Lob
    private String config;

    @Lob
    private String fieldConfig;


    @Override
    protected String _getEntityName() {
        return BackupView.class.getName();
    }


    public View getView() {
        return view;
    }
}
