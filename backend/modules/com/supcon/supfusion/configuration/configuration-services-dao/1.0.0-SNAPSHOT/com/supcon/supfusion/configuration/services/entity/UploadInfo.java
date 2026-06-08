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
 * 上载记录对象
 *
 * @author zhushizhang
 * @version $Id$
 */
@Data
@javax.persistence.Entity
@Table(name = UploadInfo.TABLE_NAME)
public class UploadInfo implements Serializable, Cloneable {
    private static final long serialVersionUID = -4337055484990585769L;
    public static final String TABLE_NAME = "ec_upload_info";
    @Id
    @GenericGenerator(name = SnowFlakeIDGenerator.GENERATOR_NAME, strategy = SnowFlakeIDGenerator.STRATEGY)
    @GeneratedValue(generator = SnowFlakeIDGenerator.GENERATOR_NAME)
    private Long id;
    @Transient
    private String category;//模块分类
    @Column(name = "MODULE_CODE", length = 1000)
    private String moduleCode;//模块编码
    @Column(name = "MODULE_NAME", length = 1000)
    private String moduleName;//模块名称

    @Column(name = "UPLOAD_FILENAME", length = 1000)
    private String uploadFileName;//上载文件名称

    @Column(name = "UPLOAD_DATE", columnDefinition = "TIMESTAMP")
    private Date uploadDate;//上载时间

    @Column(name = "UPLOAD_STATE")
    private String uploadState;//上载状态
    @OneToOne
    @JoinColumn(name = "UPLOAD_STAFF", referencedColumnName = "ID")
    @Fetch(FetchMode.SELECT)
    private Staff uploadStaff;//上载人

    @Column(name = "OLD_VERSION", length = 1000)
    private String oldVersion;//包上载版本

    @Column(name = "CUR_VERSION", length = 1000)
    private String curVersion;//当前环境的版本

    @Column(name = "TOTAL_TIME", length = 100)
    private String totalTime;//上载总时长

    @Column(name = "ISALL")
    private Boolean isAll;//上载全部

    @Column(name = "ISCUSTOMCODE")
    private Boolean isCustomcode;//是否导入自定义代码

    @Column(name = "ISMETADATA")
    private Boolean isMetadata;//是否导入源数据

    @Column(name = "ISFLOW")
    private Boolean isFlow;//是否导入工作流

    @Column(name = "ISFILTERMETHOD")
    private Boolean isFilterMethod;//是否过滤onload、onSave事件

    @Column(name = "ISIMPORTTEMPLATE")
    private Boolean isImportTemplate;//是否导入Excel导入模板

    @Column(name = "ISUPLOADSCHEDULERJOB")
    private Boolean isUploadschedulerJob;//是否导入导入调度
    @Transient
    private Boolean isFirstImport;//是否第一次导入

    @Transient
    private String relations;//依赖模块
    @Transient
    private String references;    //引用模块

    @Transient
    private String relationsInternation;//依赖模块国际化值

    @Transient
    private String errorMsg;//错误信息显示

    @Transient
    private Integer level;//上载等级

    @Transient
    private Integer entityNum;//实体数量

    @ManyToOne
    @JoinColumn(name = "BATCHID", referencedColumnName = "ID")
    @Fetch(FetchMode.SELECT)
    private UploadInfoBatch uploadInfoBatch;//批量上载表的对象

    private String uploada;//寻找模块文件耗时
    private String uploadb;//解压模块文件耗时
    private String uploadc;//解析module文件耗时
    private String uploadd;//导入模板模块文件耗时
    private String uploade;//拷贝模块文件耗时
    private String uploadf;//导入系统编码模块文件耗时
    private String uploadg;//portlet模块文件耗时
    private String uploadh;//处理国际化模块文件耗时
    private String uploadi;//导入国际化模块文件耗时
    private String uploadj;//解压模块文件耗时
    private String uploadk;//自定义代码保存的时间
    private String uploadl;//备用字段L
    private String uploadm;//备用字段M
    private String uploadn;//备用字段N

    public String getUploadState() {
        switch (uploadState) {
            case "success":
                uploadState = "成功";
                break;
            case "failed":
                uploadState = "失败";
                break;
            default:
                break;
        }
        return uploadState;
    }

}