package com.supcon.supfusion.configuration.services.openapi.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.configuration.services.entity.UploadInfoBatch;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(value = "UploadInfoVO", description = "UploadInfoVO")
public class UploadInfoVO extends VO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private int version;
    private String category;//模块分类
    private String moduleCode;//模块编码
    private String moduleName;//模块名称
    private String uploadFileName;//上载文件名称
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadDate;//上载时间
    private String uploadState;//上载状态
    private Staff uploadStaff;//上载人
    private String oldVersion;//包上载版本
    private String curVersion;//当前环境的版本
    private String totalTime;//上载总时长
    private Boolean isAll;//上载全部
    private Boolean isCustomcode;//是否导入自定义代码
    private Boolean isMetadata;//是否导入源数据
    private Boolean isFlow;//是否导入工作流
    private Boolean isFilterMethod;//是否过滤onload、onSave事件
    private Boolean isImportTemplate;//是否导入Excel导入模板
    private Boolean isUploadschedulerJob;//是否导入导入调度
    private Boolean isFirstImport;//是否第一次导入
    private String relations;//依赖模块
    private String references;    //引用模块
    private String relationsInternation;//依赖模块国际化值
    private String errorMsg;//错误信息显示
    private Integer level;//上载等级
    private Integer entityNum;//实体数量
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
}
