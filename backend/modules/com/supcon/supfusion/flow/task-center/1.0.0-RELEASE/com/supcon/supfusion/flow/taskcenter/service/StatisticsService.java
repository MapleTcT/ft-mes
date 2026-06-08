/**
 * Licensed to the Deep Blue SUPCON
 */
package com.supcon.supfusion.flow.taskcenter.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.flow.common.enumeration.CategoryEnum;
import com.supcon.supfusion.flow.common.po.PendingTaskPO;
import com.supcon.supfusion.flow.common.vo.webapi.StatisticsVO;
import com.supcon.supfusion.flow.dao.PendingTaskMapper;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;

/**
 * @author: zhuangmh
 * @date: 2020年9月2日 上午11:00:47
 */
@Service
public class StatisticsService {
    
    @Autowired
    private PendingTaskMapper pendingTaskMapper;
    /**
     * 待办的维度统计
     * @return
     */
    public PageResult<StatisticsVO> statistics(CategoryEnum category, int current, int size) {
        Long userId = UserContext.getUserContext().getUserId();
        if (userId == null) {
            return new PageResult<>(new ArrayList<>(), 0, size, current);
        }
        Page<PendingTaskPO> page = new Page<>(current, size);
        // 加MAX函数为了解决 ORA-00979
        QueryWrapper<PendingTaskPO> query = new QueryWrapper<PendingTaskPO>().select("MAX(task_description_zh_cn) as taskDescriptionZhCn, MAX(process_name) as processName, count(*) as count").eq("user_id", userId).groupBy(category.getColumn());
        Page<PendingTaskPO> tasks = pendingTaskMapper.selectPage(page, query);
        int total = pendingTaskMapper.countStatistic(category.getName(), userId);
        return new PageResult<>(voTransfer(tasks.getRecords()), total, size, current);
    }
    
    private List<StatisticsVO> voTransfer(List<PendingTaskPO> pos) {
        List<StatisticsVO> vos = new LinkedList<>();
        for (PendingTaskPO po : pos) {
            StatisticsVO vo = new StatisticsVO();
            vo.setCount(po.getCount());
            vo.setProcessName(po.getProcessName());
            vo.setTaskName(po.getTaskDescriptionZhCn());
            vos.add(vo);
        }
        return vos;
    }
    
}
