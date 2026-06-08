package com.supcon.supfusion.signature.interapi.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignatureLogConditionQueryVo extends VO {
    private static final long serialVersionUID = -4449448847035998982L;
    private List<String> businessKey;
    private List<String> entityName;
    private List<String> modelName;
    private List<String> moduleCode;
    private List<String> userName;

    private List<String> userId;
    private List<String> entityCode;
    private List<String> modelCode;
    private List<String> buttonCode;
    private List<String> moduleName;

    private List<String> firstReason;
    private List<String> secondReason;
    private List<String> secondUserName;
    private List<String> firstUserName;
    private List<String> secondStaffName;
    private List<String> firstStaffName;
    private List<String> firstStaffId;

    private List<String> secondStaffId;
    private List<String> signatureType;
    private List<String> firstSignTimeStr;
    private List<String> secondSignTimeStr;
    private String firstSignTimeEnum;
    private String secondSignTimeEnum;
    private List<String> ids;
    private Boolean isAll = false;
    private int current = 1;
    private int pageSize = 20;
}
