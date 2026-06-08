package com.supcon.supfusion.notification.admin.openapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;


/**
 * ${description}
 *
 * @author chenweinan
 * @create 2020/7/8 14:24
 */
@Data
@ApiModel(value="通讯类型配置参数")
public class ProtocolConfigListVO extends VO{
	
	@ApiModelProperty(value = "协议类型列表", required = true)
    private List<Map> protocols;
    
    /***
     * 实体拷贝
     * @param vo
     * @return
     */
    public List<Map> entityCP(List<NoticeProtocol> protocols){
    	List<Map> list =new ArrayList<>();
    	Map map =new HashMap();
        for(NoticeProtocol  protocol:protocols) {
        	map.put("protocol", protocol.getProtocol());
        	map.put("name", protocol.getName());
        } 
        list.add(map);
        return list;
    }
}
