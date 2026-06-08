package com.supcon.supfusion.organization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.organization.dao.mapper.group.GroupPersonMapper;
import com.supcon.supfusion.organization.dao.po.group.GroupPersonPO;
import com.supcon.supfusion.organization.service.GroupPersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GroupPersonServiceImpl extends ServiceImpl<GroupPersonMapper, GroupPersonPO> implements GroupPersonService {


    @Override
    public void addGroupPerson(List<GroupPersonPO> relations) {
        saveOrUpdateBatch(relations);
    }

    @Override
    public List<Long> queryPersonIdByGroupId(Long groupId) {
        List<GroupPersonPO> relations = list(new QueryWrapper<GroupPersonPO>().lambda().eq(GroupPersonPO::getGroupId, groupId).eq(GroupPersonPO::getValid, true));
        List<Long> personIds = new ArrayList<Long>();
        relations.stream().forEach(relation -> personIds.add(relation.getPersonId()));
        return personIds;
    }

    /**
     * 批量修改岗位人员关系
     *
     * @param list
     */
    @Override
    public void batchSaveOrUpdate(List<GroupPersonPO> list) {

        if (list == null) {
            return;
        }
        list.stream().forEach(relation -> {
            LambdaQueryWrapper<GroupPersonPO> eq = new QueryWrapper<GroupPersonPO>().lambda()
                    .eq(GroupPersonPO::getGroupId, relation.getGroupId())
                    .eq(GroupPersonPO::getPersonId, relation.getPersonId());
            GroupPersonPO one = getOne(eq);
            if (one == null) {
                save(relation);
            }else {
                one.setValid(true);
                updateById(one);
            }
        });
    }

    @Override
    public void deleteByPersonId(Long personId) {
        UpdateWrapper<GroupPersonPO> updateWrapper = new UpdateWrapper<GroupPersonPO>();
        updateWrapper.eq("person_id", personId);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

    /**
     * @see com.supcon.supfusion.organization.service.GroupPersonService#batchDeleteByPersonId(java.util.List)
     */
    @Override
    public void batchDeleteByPersonId(List<Long> personIds) {
        UpdateWrapper<GroupPersonPO> updateWrapper = new UpdateWrapper<GroupPersonPO>();
        updateWrapper.in("person_id", personIds);
        updateWrapper.set("valid", 0);
        update(updateWrapper);
    }

}
