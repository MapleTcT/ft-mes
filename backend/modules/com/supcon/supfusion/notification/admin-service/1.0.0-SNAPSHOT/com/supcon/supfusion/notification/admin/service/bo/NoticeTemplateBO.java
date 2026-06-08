package com.supcon.supfusion.notification.admin.service.bo;

import com.supcon.supfusion.notification.admin.dao.entities.NoticeTemplate;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeTemplateBO extends NoticeBaseBO {
    @ApiModelProperty(value = "通知方式")
    @NotEmpty(message = "通知方式不能为空")
    private Long noticeType;

    @NotEmpty(message = "模板内容不能为空")
    @ApiModelProperty(value = "模板内容")
    private String template;

    @ApiModelProperty(value = "模板默认参数")
    private String params;

    /***
     * 实体拷贝
     * @param vo
     * @return
     */
    public NoticeTemplate entityCP(NoticeTemplateBO vo){
        NoticeTemplate entity = new NoticeTemplate();

        entity.setNoticeType(vo.getNoticeType());
        entity.setTemplate(vo.getTemplate());
        entity.setParams(vo.getParams());

        entity.setCode(vo.getCode());
        entity.setName(vo.getName());
        entity.setMemo(vo.getMemo());
        if(entity.getCreator()==null){
            entity.setCreator("admin");
        }
        /***
         * {"id": 0,"is_valid": true,"modify_sign": true,"sort": 0,"source": "string","version": 0}
         */
        if(vo.getId()!=null){
            entity.setId(vo.getId());
        }
        if(vo.getSort()!=null){
            entity.setSort(vo.getSort());
        }
        if(vo.getName()!=null){
            entity.setName(vo.getName());
        }
        if(vo.getVersion()!=null){
            entity.setVersion(vo.getVersion());
        }
        return entity;
    }

}
