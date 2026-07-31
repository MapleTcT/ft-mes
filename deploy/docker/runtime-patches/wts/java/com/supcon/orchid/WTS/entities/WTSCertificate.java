package com.supcon.orchid.WTS.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supcon.orchid.Qualify.entities.QualifyStaffCert;
import com.supcon.orchid.annotation.BAPCustomComponent;
import com.supcon.orchid.annotation.BAPEntity;
import com.supcon.orchid.annotation.BAPModelCode;
import com.supcon.orchid.audit.annotation.DataAudit;
import com.supcon.orchid.foundation.entities.Company;
import com.supcon.orchid.foundation.entities.Staff;
import com.supcon.orchid.orm.entities.ICId;
import com.supcon.orchid.orm.entities.jaxb.BAPFoundationAdapter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/** Recovered qualification-person part entity referenced by WTS ticket forms. */
@javax.persistence.Entity(name = WTSCertificate.JPA_NAME)
@Table(name = WTSCertificate.TABLE_NAME)
@BAPEntity(entityCode = "WTS_1.0.0_workTicket")
@BAPModelCode(code = "WTS_1.0.0_workTicket_Certificate")
@AttributeOverrides({
    @AttributeOverride(name = "id", column = @Column(name = "ID")),
    @AttributeOverride(name = "sort", column = @Column(name = "SORT")),
    @AttributeOverride(name = "tableInfoId", column = @Column(name = "TABLE_INFO_ID")),
    @AttributeOverride(name = "version", column = @Column(name = "VERSION"))
})
@BAPCustomComponent
@DataAudit
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WTSCertificate extends com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity {
    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "wts_certificates";
    public static final String JPA_NAME = "WTSCertificate";

    private Staff staff;
    private QualifyStaffCert staffCertificate;
    private WTSWorkTicket workTicket;
    private Company company;
    private String cidName;
    private String virtualId;

    @Override
    @Index(name = "IDX_WTS_CERTIFICATE_TABLE_ID")
    @Column(name = "TABLE_INFO_ID")
    public Long getTableInfoId() {
        return tableInfoId;
    }

    @ManyToOne
    @JoinColumn(name = "STAFF", referencedColumnName = "ID")
    @Fetch(FetchMode.SELECT)
    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    @ManyToOne
    @JoinColumn(name = "STAFF_CERTIFICATE", referencedColumnName = "ID")
    @Fetch(FetchMode.SELECT)
    public QualifyStaffCert getStaffCertificate() {
        return staffCertificate;
    }

    public void setStaffCertificate(QualifyStaffCert staffCertificate) {
        this.staffCertificate = staffCertificate;
    }

    @ManyToOne
    @JoinColumn(name = "WORK_TICKET", referencedColumnName = "ID")
    @Fetch(FetchMode.SELECT)
    public WTSWorkTicket getWorkTicket() {
        return workTicket;
    }

    public void setWorkTicket(WTSWorkTicket workTicket) {
        this.workTicket = workTicket;
    }

    @Override
    protected String _getEntityName() {
        return WTSCertificate.class.getName();
    }

    @Override
    @OneToOne(fetch = FetchType.EAGER, targetEntity = Company.class, optional = true)
    @JoinColumn(name = ICId.P_CID, insertable = false, updatable = false)
    @Fetch(FetchMode.SELECT)
    @XmlJavaTypeAdapter(BAPFoundationAdapter.class)
    public Company getCompany() {
        return company;
    }

    @Override
    public void setCompany(Company company) {
        this.company = company;
    }

    @Transient
    public String getCidName() {
        return cidName;
    }

    public void setCidName(String cidName) {
        this.cidName = cidName;
    }

    @Transient
    public String getVirtualId() {
        return virtualId;
    }

    public void setVirtualId(String virtualId) {
        this.virtualId = virtualId;
    }
}
