package com.supcon.supfusion.counter.service.bo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @Author kk.C
 * @Description: 生成编码的参数BO类
 * @Date 2020/10/15 15:57
 */
@Getter
@Setter
@ToString
public class CodeParameterBO {
    /**
     * 当前参照值
     */
    private StringBuilder reference;
    /**
     * 编码组成数组
     */
    private List<String> codeList;
    /**
     * 序号值
     */
    private String numberCode;

}
