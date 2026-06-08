package com.supcon.supfusion.notification.admin.openapi.vo;

import com.supcon.supfusion.framework.cloud.common.pojo.VO;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * ${description}
 *
 * @author chenweinan
 * @create 2020/7/8 14:24
 */
@Data
@ApiModel(value="协议配置参数")
public class NoticeTemplateVO extends VO {


    @ApiModelProperty(value = "通知方式",name="protocol")
    private Long protocol;


    @ApiModelProperty(value = "模板内容",name="template",example="这是消息模板")
    private String template;
    
    @ApiModelProperty(value = "模板编码",name="code")
    private String code;
    
    @ApiModelProperty(value = "模板名称",name="name")
    private String name;
    
    @ApiModelProperty(value = "模板备注",name="memo")
    private String memo;
    
    /***
     * 实体拷贝
     * @param vo
     * @return
     */
    public NoticeTemplate entityCP(NoticeTemplateVO vo){
        NoticeTemplate entity = new NoticeTemplate();

        entity.setNoticeType(vo.getProtocol());
        entity.setTemplate(vo.getTemplate());

        entity.setCode(vo.getCode());
        entity.setName(vo.getName());
        entity.setMemo(vo.getMemo());
        /***
         * {"id": 0,"is_valid": true,"modify_sign": true,"sort": 0,"source": "string","version": 0}
         */
        if(vo.getName()!=null){
            entity.setName(vo.getName());
        }

        return entity;
    }

}
