package com.supcon.supfusion.notification.admin.service.bo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeTopicBO extends NoticeBaseBO {
    @ApiModelProperty(value = "通知方式", name = "type", example = "protocol_id(LONG)")
    @JsonDeserialize(using = IDJsonDeserializer.class)
    private Long type;
    @ApiModelProperty(value = "主题对应通知方式的模板", example = "{\"1000\":\"1002\"}")
    private List<Long> tmpIdList;
    @ApiModelProperty(value = "主题的接收范围", notes = "人员，角色，岗位，部门")
    private List<Map<String, List<Object>>> receiveRange;

    /***
     * 实体拷贝
     * @param vo
     * @return
     */
    public NoticeTopic entityCP(NoticeTopicBO vo) {
        NoticeTopic entity = new NoticeTopic();

        entity.setType(vo.getType());

        entity.setCode(vo.getCode());
        entity.setName(vo.getName());
        entity.setMemo(vo.getMemo());
        entity.setTmpIdList(vo.getTmpIdList());
        entity.setReceiveRange(vo.getReceiveRange());
        if (entity.getCreator() == null) {
            entity.setCreator("admin");
        }
        /***
         * {"id": 0,"is_valid": true,"modify_sign": true,"sort": 0,"source": "string","version": 0}
         */
        if (vo.getId() != null) {
            entity.setId(vo.getId());
        }
        if (vo.getSort() != null) {
            entity.setSort(vo.getSort());
        }
        if (vo.getName() != null) {
            entity.setName(vo.getName());
        }
        if (vo.getVersion() != null) {
            entity.setVersion(vo.getVersion());
        }
        return entity;
    }

}
