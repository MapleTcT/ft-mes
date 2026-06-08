package com.supcon.supfusion.notification.app.config.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import lombok.Data;

import java.util.List;
import java.util.Map;


/**
 * ${description}
 *
 * @author chenweinan
 * @create 2020/7/8 14:24
 */
@Data
public class ProtocolConfigListVO extends VO{
	
    private List<Map> protocols;

}
