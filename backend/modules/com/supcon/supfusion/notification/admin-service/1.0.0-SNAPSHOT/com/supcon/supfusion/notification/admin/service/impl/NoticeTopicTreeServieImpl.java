package com.supcon.supfusion.notification.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeBase;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTree;
import com.supcon.supfusion.notification.admin.dao.mappers.NoticeTopicTreeDao;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTreeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 18:05
 */
@Service("adminNoticeTopicTreeServieImpl")
public class NoticeTopicTreeServieImpl extends NoticeBaseServiceImpl<NoticeTopicTreeDao, NoticeTopicTree> implements NoticeTopicTreeService {

    @Resource(name = "adminNoticeTopicServiceImpl")
    private NoticeTopicService topicService;
    @Autowired(required = false)
    private MessageResourceWrapper messageResourceWrapper;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;


    @Override
    public List<NoticeTopicTree> queryListByNameOrCode(String code, String name) {
        String dbType = dataId.getDataId();
        Locale locale = LocaleContextHolder.getLocale();
        QueryWrapper<NoticeTopicTree> queryWrapper = new QueryWrapper<>();
        List<NoticeTopicTree> list = new ArrayList<NoticeTopicTree>();
        if (code != null) {
            queryWrapper.eq("code", code);
            addParentTree(queryWrapper, list);
        } else if (name != null) {
            //存在国际化key值的查询
            queryWrapper.isNotNull("i18n_key");
            List<NoticeTopicTree> keyLists = super.list(queryWrapper);
            if (null != keyLists && keyLists.size() > 0) {
                for (NoticeTopicTree keyList : keyLists) {
                    //转换国际化名称
                    if (messageResourceWrapper.getMessageNotBlank(keyList.getI18nKey()).contains(name)) {
                        list.add(keyList);
                    }
                }
            }
            QueryWrapper<NoticeTopicTree> topicWrapper = new QueryWrapper<>();
            //不存在国际化key值的查询
            String key = dbStringUtil.getString(name);
            if ("oracle".equals(dbType)) {
                topicWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
            } else {
                topicWrapper.like("name", key);
            }
            topicWrapper.isNull("i18n_key");
            addParentTree(topicWrapper, list);
            //去除重复数据
            Set<NoticeTopicTree> userSet = new TreeSet<>((o1, o2) -> o1.getId().compareTo(o2.getId()));
            userSet.addAll(list);
            list = new ArrayList<>(userSet);
        } else {
            list = super.list(queryWrapper);
        }
        getLayrec(list);
        return list;
    }


    /***
     * 递归获取所有节点
     * @param queryWrapper list
     *
     */
    private void addParentTree(QueryWrapper queryWrapper, List<NoticeTopicTree> list) {
        List<NoticeTopicTree> parentList = super.list(queryWrapper);
        if (null != parentList && parentList.size() > 0) {
            for (NoticeTopicTree topicTree : parentList) {
                list.add(topicTree);
                QueryWrapper<NoticeTopicTree> queryWrapper1 = new QueryWrapper<>();
                queryWrapper1.eq("id", topicTree.getParentId());
                addParentTree(queryWrapper1, list);
            }
        }

    }


    /***
     * 根据layrec排序
     * @param list treeList
     */
    private void getLayrec(List<NoticeTopicTree> list) {

        Collections.sort(list, (u1, u2) -> {
            //根据layrec升序排序
            if (u1.getLayRec() > u2.getLayRec()) {
                return 1;
            }
            if (u1.getLayRec().equals(u2.getLayRec())) {
                return 0;
            }
            return -1;
        });

    }

    @Override
    public List<NoticeTopicTree> queryListByKeyword(String keyword) {
        String dbType = dataId.getDataId();
        QueryWrapper<NoticeTopicTree> queryWrapper = new QueryWrapper<>();
        if (keyword != null) {
            String key = dbStringUtil.getString(keyword);
            if ("oracle".equals(dbType)) {
                queryWrapper.apply("name like {0} escape '\\'", "%" + key + "%");
            } else {
                queryWrapper.like("name", key);
            }
        } else {
            return new ArrayList<NoticeTopicTree>();
        }
        return super.list(queryWrapper);
    }

    @Override
    @Transactional
    public NoticeTopicTree addEntity(NoticeTopicTree entity) {
        QueryWrapper<NoticeTopicTree> queryWrapper = new QueryWrapper<>();
        if (entity.getName() != null) {
            queryWrapper.eq(NoticeTopicTree.getNameFieldName(), entity.getName());
        }
        List<NoticeTopicTree> result = super.list(queryWrapper);
        //主题名称去重判断
        if (null != result && result.size() > 0) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TOPIC_NAME);
        }
        entity.setId(IDGenerator.newInstance().generate().longValue());
        entity.setCode(entity.getId().toString());
        if (entity.getParentId() == 0) {
            entity.setLayRec(0);
        } else {
            NoticeTopicTree parent = super.queryEntity(entity.getParentId());
            if (parent != null) {
                entity.setLayRec(parent.getLayRec() + 1);
            }
        }
        if (super.save(entity)) {
            return entity;
        } else {
            return null;
        }
    }

    @Override
    @Transactional
    public String delEntity(String ids) {
        if (validRef(ids)) {
            return super.delEntity(ids);
        }
        return null;
    }

    /***
     * 验证模板是否依赖消息主题，返回依赖结果
     * @param ids
     * @return
     */
    private Boolean validRef(String ids) {
        Locale locale = LocaleContextHolder.getLocale();
        if (StringUtils.isNotBlank(ids)) {
            String[] idList = ids.split(",");
            StringBuilder result = new StringBuilder();
            for (String typeId : idList) {
                QueryWrapper<NoticeTopic> queryWrapper = new QueryWrapper<>();
                String typeName = super.getById(typeId).getName();
                queryWrapper.eq(NoticeTopic.getTopicTypeName(), typeId);
                //查出主题类型关联的主题
                List<NoticeTopic> topics = topicService.list(queryWrapper);
                if (null != topics && topics.size() > 0) {
                    result.append("[" + typeName + "]" + messageResourceWrapper.getMessageNotBlank("notificationAdmin.exist_topic"));
                }
            }
            if (StringUtils.isNotBlank(result)) {
                throw new NotificationAdminExecption(NotificationAdminError.ERROR_DELETE_TEMPLATE, result);
            }
        }
        return true;
    }

    @Override
    public String deleteById(String id) {
        StringBuffer str = new StringBuffer();
        str.append(id);
        //递归查询子节点
        queryById(id, str);
        if (null != str) {
            return delEntity(str.toString());
        }
        return null;
    }

    /***
     * 递归查询子节点
     * @param ids
     * @return
     */
    public void queryById(String id, StringBuffer ids) {
        QueryWrapper<NoticeTopicTree> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(NoticeTopicTree.getParentIdName(), id);
        List<NoticeTopicTree> topicTreeLists = super.list(queryWrapper);
        if (null != topicTreeLists && topicTreeLists.size() > 0) {
            for (NoticeTopicTree topicTreeList : topicTreeLists) {
                if (null != topicTreeList.getId()) {
                    ids.append("," + topicTreeList.getId());
                    queryById(topicTreeList.getId().toString(), ids);
                }

            }
        }

    }
}
