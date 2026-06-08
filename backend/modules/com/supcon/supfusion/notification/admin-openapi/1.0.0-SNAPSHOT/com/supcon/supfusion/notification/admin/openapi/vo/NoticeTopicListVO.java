package com.supcon.supfusion.notification.admin.openapi.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;

/**
 * ${description}
 *
 * @author chenweinan
 * @create 2020/7/8 14:24
 */
@Data
@ApiModel(value="协议配置参数")
public class NoticeTopicListVO extends VO{
	
	@ApiModelProperty(value = "主题列表", required = true)
    private List<Map> topics;
    
    /***
     * 实体拷贝
     * @param vo
     * @return
     */
    public List<Map> entityCP(List<NoticeTopic> topic){
    	List<Map> list =new ArrayList<>();
    	Map map =new HashMap();
        for(NoticeTopic noticeTopic :topic) {
        	map.put("topicCode", noticeTopic.getCode());
        	map.put("topicName", noticeTopic.getName());
        } 
        list.add(map);
        return list;
    }
}
