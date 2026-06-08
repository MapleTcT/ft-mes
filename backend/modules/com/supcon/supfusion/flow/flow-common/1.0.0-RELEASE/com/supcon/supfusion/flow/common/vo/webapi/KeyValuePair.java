/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.common.vo.webapi;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author: zhuangmh
 * @date: 2020年6月17日 上午10:22:09
 */
@AllArgsConstructor
@Data
public class KeyValuePair<T> {
    
    private String key;
    private T value;
    
}
