package com.supcon.supfusion.notification.admin.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value="协议配置参数")
public class NoticeTopicVO extends VO {
    @ApiModelProperty(value = "主题类型",name="noticeType",example="")
    private Long noticeType;
    @ApiModelProperty(value = "主题对应通知方式的模板")
    private List<Long> tmpIdList;
    @ApiModelProperty(value = "主题的接收范围",notes = "人员，角色，岗位，部门")
    private List<Map<String,List<Object>>> receiveRange;
    @ApiModelProperty(value = "主题编码")
    private String code;
    @ApiModelProperty(value = "主题名称")
    private String name;
    /***
     * 实体拷贝
     * @param vo
     * @return
     */
    public NoticeTopic entityCP(NoticeTopicVO vo){
        NoticeTopic entity = new NoticeTopic();

        entity.setType(vo.getNoticeType());

        entity.setCode(vo.getCode());
        entity.setName(vo.getName());
        entity.setTmpIdList(vo.getTmpIdList());
        entity.setReceiveRange(vo.getReceiveRange());
        if(entity.getCreator()==null){
            entity.setCreator("admin");
        }
        /***
         * {"id": 0,"is_valid": true,"modify_sign": true,"sort": 0,"source": "string","version": 0}
         */
        
        return entity;
    }

}
