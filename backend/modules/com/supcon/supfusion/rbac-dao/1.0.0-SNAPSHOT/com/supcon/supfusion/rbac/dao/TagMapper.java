package com.supcon.supfusion.rbac.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.supcon.supfusion.rbac.dao.po.TagPO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 标签表 Mapper 接口
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-10
 */
public interface TagMapper extends BaseMapper<TagPO> {

    @Select("SELECT id,name FROM rbac_tag WHERE objectid = #{objectId}")
    TagPO findTagName(@Param("objectId") Long objectId);


    @Select("SELECT tag.name, GROUP_CONCAT(OBJECTID separator ',') objectIds from rbac_tag tag " +
            "JOIN rbac_role role ON role.id = tag.objectId " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(column = "objectIds",property = "objectIds",javaType = String.class),
    })
    List<Map<String,Object>> findObjectIdGroupByTagName(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT tag.name,objectIds = stuff((SELECT ',' + cast(OBJECTID as varchar(20))  FROM rbac_tag t where t.NAME = tag.NAME FOR xml path('')),1,1,'') " +
            "from rbac_tag tag " +
            "JOIN rbac_role role ON role.id = tag.objectId " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(column = "objectIds",property = "objectIds",javaType = String.class),
    })
    List<Map<String,Object>> findObjectIdGroupByTagNameSqlServer(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Select("SELECT tag.name,WM_CONCAT(OBJECTID) objectIds " +
            "from rbac_tag tag " +
            "JOIN rbac_role role ON role.id = tag.objectId " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(column = "objectIds",property = "objectIds",javaType = String.class),
    })
    List<Map<String,Object>> findObjectIdGroupByTagNameOracle(@Param(Constants.WRAPPER) Wrapper wrapper);

    @Update("update rbac_tag set valid = 0 ${ew.customSqlSegment}")
    void deleteTag(@Param(Constants.WRAPPER) Wrapper wrapper);
}
