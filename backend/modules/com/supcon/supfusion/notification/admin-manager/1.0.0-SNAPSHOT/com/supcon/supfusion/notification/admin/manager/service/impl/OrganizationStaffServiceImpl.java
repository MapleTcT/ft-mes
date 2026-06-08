package com.supcon.supfusion.notification.admin.manager.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiReference;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.notification.common.bean.RangeType;
import com.supcon.supfusion.notification.admin.manager.service.IOrganizationStaffService;
import com.supcon.supfusion.notification.admin.service.NoticeReceiveRangeExtService;
import com.supcon.supfusion.notification.admin.service.NoticeReceiveRangeService;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRange;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRangeExt;
import com.supcon.supfusion.notification.admin.dao.mappers.organization.NoticeReceiveRangeExtDao;
import com.supcon.supfusion.organization.api.PersonApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/6/11 11:13
 */
@Service
@Slf4j
public class OrganizationStaffServiceImpl implements IOrganizationStaffService {
    private final static Logger LOGGER = LoggerFactory.getLogger(OrganizationStaffServiceImpl.class);

    @ServiceApiReference
    private PersonApiService personApiService;
    @Resource(name = "adminNoticeReceiveRangeExtServiceImpl")
    private NoticeReceiveRangeExtService rangeExtService;
    @Resource(name = "adminNoticeReceiveRangeExtDao")
    private NoticeReceiveRangeExtDao noticeReceiveRangeExtDao;
    @Resource(name = "adminNoticeReceiveRangeServiceImpl")
    private NoticeReceiveRangeService rangeService;

    @Override
    public void saveReceiveRange(Long topicId, List<Map<String, List<Object>>> receiveRangeMap) {

        for (Map<String, List<Object>> receiveRange : receiveRangeMap) {
            for (Map.Entry<String, List<Object>> entry : receiveRange.entrySet()) {
                String rangeType = entry.getKey();
                switch (RangeType.byValueOf(rangeType)) {
                    case STAFF:
                        this.dealStaffRange(topicId, entry.getValue());
                        break;
                    case ROLE:
                        this.dealRoleRange(topicId, entry.getValue());
                        break;
                    case POSITION:
                        this.dealPositionRange(topicId, entry.getValue());
                        break;
                    case DEPARTMENT:
                        this.dealDepartmentRange(topicId, entry.getValue());
                        break;
                    case UNKNOWN:
                        LOGGER.error("消息主题中接收范围类型错误，请确认为人员、角色、岗位或部门");
                        break;
                    default:
                        break;
                }
            }
        }

    }

    /***
     * 构建topic对象中receiveRange字段
     * @param topicId
     * @return
     */
    @Override
    public Map<String, List<NoticeRecieveRangeExt>> queryReceiveRange(Long topicId) {
        if (topicId == null) {
            return null;
        }
        Map<String, List<NoticeRecieveRangeExt>> resultMap = new LinkedHashMap<>();
        List<NoticeRecieveRange> rangeList = rangeService.queryListByTopic(topicId);
        if (rangeList == null || rangeList.size() == 0) {
            return resultMap;
        }
        for (NoticeRecieveRange noticeRecieveRange : rangeList) {
            RangeType rangeType = RangeType.byValueOf(noticeRecieveRange.getRangeType());
            List<NoticeRecieveRangeExt> noticeRecieveRangeExts = noticeReceiveRangeExtDao.selectList(Wrappers.<NoticeRecieveRangeExt>query().eq(NoticeRecieveRangeExt.getRangeIdFieldName(), noticeRecieveRange.getId()));
            if (RangeType.STAFF.tableValue().equals(noticeRecieveRange.getRangeType())) {
                resultMap.put(RangeType.STAFF.value(), noticeRecieveRangeExts);
            } else if (RangeType.DEPARTMENT.tableValue().equals(noticeRecieveRange.getRangeType())) {
                resultMap.put(RangeType.DEPARTMENT.value(), noticeRecieveRangeExts);
            } else if (RangeType.ROLE.tableValue().equals(noticeRecieveRange.getRangeType())) {
                resultMap.put(RangeType.ROLE.value(), noticeRecieveRangeExts);
            } else if (RangeType.POSITION.tableValue().equals(noticeRecieveRange.getRangeType())) {
                resultMap.put(RangeType.POSITION.value(), noticeRecieveRangeExts);
            }
        }
        log.info("organizationStaffService.queryReceiveRange return :{}", JSONObject.toJSONString(resultMap));
        return resultMap;
    }

    @Override
    public List<NoticeRecieveRange> deleteReceiveRangeByTopicIds(String topicIds) {
        List<NoticeRecieveRange> rangeList = new ArrayList<>();
        if (StringUtils.isNotBlank(topicIds)) {
            String[] idList = topicIds.split(",");
            int size = idList.length;
            for (int i = 0; i < size; i++) {
                rangeList.addAll(this.deleteReceiveRange(Long.valueOf(idList[i])));
            }
        }
        return rangeList;
    }

    /***
     * 删除接收范围
     * @param topicId
     * @return
     */
    @Override
    public List<NoticeRecieveRange> deleteReceiveRange(Long topicId) {
        List<NoticeRecieveRange> rangeList = rangeService.queryListByTopic(topicId);
        if (rangeList.size() == 0) {
            return null;
        }
        Long[] recevieRanges = new Long[rangeList.size()];
        for (int i = 0, size = rangeList.size(); i < size; i++) {
            recevieRanges[i] = rangeList.get(i).getId();
        }
        if (rangeExtService.deleteListByRange(recevieRanges)) {
            if (!rangeService.deleteListByTopic(topicId)) {
                LOGGER.error("接收范围表删除失败,删除主题：{}", topicId);
            }
        } else {
            LOGGER.error("接收范围扩展表删除失败，删除列表：{}", JSON.toJSONString(recevieRanges));
        }
        return rangeList;
    }

    private void dealStaffRange(Long topicId, List<Object> rangeList) {
        dealRange(topicId, rangeList, RangeType.STAFF);
    }

    private void dealRoleRange(Long topicId, List<Object> rangeList) {
        dealRange(topicId, rangeList, RangeType.ROLE);
    }

    private void dealDepartmentRange(Long topicId, List<Object> rangeList) {
        dealRange(topicId, rangeList, RangeType.DEPARTMENT);
    }

    private void dealPositionRange(Long topicId, List<Object> rangeList) {
        dealRange(topicId, rangeList, RangeType.POSITION);
    }


    private void dealRange(Long topicId, List<Object> rangeList, RangeType rangeType) {
        NoticeRecieveRange recieveRange = new NoticeRecieveRange();
        recieveRange.setNoticeTopicId(topicId.toString());
        recieveRange.setId(IDGenerator.newInstance().generate().longValue());
        recieveRange.setRangeType(rangeType.tableValue());

        List<NoticeRecieveRangeExt> rangeExtList = new ArrayList<>();
        for (Object object : rangeList) {
            JSONObject range = JSONObject.parseObject(JSON.toJSONString(object));
            //接收范围的补充字段
            NoticeRecieveRangeExt recieveRangeExt = new NoticeRecieveRangeExt();
            recieveRangeExt.setId(IDGenerator.newInstance().generate().longValue());
            recieveRangeExt.setReceiverId(range.getLong("id"));
            recieveRangeExt.setReceiverCode(range.getString("code"));
            recieveRangeExt.setNoticeTopicRangeId(recieveRange.getId());
            recieveRangeExt.setContainChildren(0);
            rangeExtList.add(recieveRangeExt);
        }
        rangeService.save(recieveRange);
        rangeExtService.saveBatch(rangeExtList);
    }

}
