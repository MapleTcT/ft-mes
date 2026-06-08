package com.supcon.supfusion.notification.admin.service.bo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonDeserializer;
import com.supcon.supfusion.framework.cloud.common.json.converters.IDJsonSerializer;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTree;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/13 14:24
 */
@Getter
@Setter
@ToString
public class NoticeTopicTreeBO extends NoticeBaseBO {

    @ApiModelProperty(value = "父级消息主题类型", name = "parent_id", example = "parent_id(LONG)")
    @JsonDeserialize(using = IDJsonDeserializer.class)
    private Long parentId;

    /*    @ApiModelProperty(value = "子集消息主题类型",name="topicTreeChlid",example="list[topic_type_id(LONG)]")
        private Set<Long> topicTreeChlid;*/
    //层级结构
    @ApiModelProperty(value = "模板默认参数", name = "layRec", example = "1000-1010-1050")
    private Integer layRec;

    /***
     * 实体拷贝
     * @param vo
     * @return
     */
    public NoticeTopicTree entityCP(NoticeTopicTreeBO vo) {
        NoticeTopicTree entity = new NoticeTopicTree();

        entity.setParentId(vo.getParentId());
        //entity.setTopicTreeChlid(vo.getTopicTreeChlid());
        entity.setLayRec(vo.getLayRec());
        //主题类型的code使用ID字段
        if (entity.getId() != null) {
            entity.setCode(entity.getId().toString());
        }
        entity.setName(vo.getName());
        entity.setMemo(vo.getMemo());
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
