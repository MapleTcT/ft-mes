package com.supcon.supfusion.auth.service.bo;

import lombok.Data;

import java.util.List;

/**
 * 用户目录查询参数
 *
 * @author caokele
 */
@Data
public class UserDirectoryQueryBO extends UserDirectoryBO {
    private List<String> screenDirectoryNames;
    private List<String> screenDirectoryTypes;
}
