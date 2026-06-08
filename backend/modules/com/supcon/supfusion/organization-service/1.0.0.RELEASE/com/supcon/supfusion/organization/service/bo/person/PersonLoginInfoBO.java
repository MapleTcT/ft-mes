package com.supcon.supfusion.organization.service.bo.person;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.*;

/**
 * 人员新增信息
 *
 * @author shidongsheng
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PersonLoginInfoBO extends VO {

    /**
     * 性别的编码值name
     */
    private String gender;

    /**
     * 头像地址
     */
    private String avatarUrl;
}
