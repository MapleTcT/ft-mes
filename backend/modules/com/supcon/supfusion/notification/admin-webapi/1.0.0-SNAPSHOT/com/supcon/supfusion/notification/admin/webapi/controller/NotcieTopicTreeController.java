package com.supcon.supfusion.notification.admin.webapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.i18n.resource.utils.MessageResourceWrapper;
import com.supcon.supfusion.framework.scaffold.dbp.util.DataId;
import com.supcon.supfusion.framework.scaffold.dbp.util.DbStringUtil;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminDefinition;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminError;
import com.supcon.supfusion.notification.admin.common.execption.NotificationAdminExecption;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeProtocol;
import com.supcon.supfusion.notification.admin.service.NoticeTopicTreeService;
import com.supcon.supfusion.notification.admin.service.bo.NoticeTopicTreeBO;
import com.supcon.supfusion.notification.admin.webapi.utils.NoticeTopicTreeWapper;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTopicTreeVO;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopicTree;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 20:03
 */
@Slf4j
@ResponseBody
@Api(description = "NoticeTopicTree-API", tags = {"消息主题类型API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
public class NotcieTopicTreeController {
    @Resource(name = "adminNoticeTopicTreeServieImpl")
    private NoticeTopicTreeService topicTreeService;
    @Autowired
    private NoticeTopicTreeWapper topicTreeWapper;
    @Autowired(required = false)
    private MessageResourceWrapper messageResourceWrapper;
    @Autowired
    private DataId dataId;
    @Autowired
    private DbStringUtil dbStringUtil;

    /**
     * 分页查询对象
     *
     * @param code
     * @param name
     * @param id
     * @param pageNo
     * @param pageSize
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "分页查询消息主题类型")
    @GetMapping(value = "/notice/topictree/topictrees")
    public PageResult<NoticeTopicTreeVO> list(@ApiParam(value = "消息主题类型编码", required = false) @RequestParam(required = false) String code,
                                              @ApiParam(value = "消息主题类型名称", required = false) @RequestParam(required = false) String name,
                                              @ApiParam(value = "消息主题类型ID", required = false) @RequestParam(required = false) String id,
                                              @ApiParam(value = "消息主题类型父级ID", required = false) @RequestParam(required = false) String topicTreeId,
                                              @ApiParam(value = "页码", required = false) @RequestParam(required = false) Integer pageNo,
                                              @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) throws Exception {
        if (pageNo != null && pageSize != null && pageNo > -1 && pageSize > -1) {
            Page page = new Page<>(pageNo, pageSize);
            Page<NoticeTopicTree> entityPage = topicTreeService.queryPageList(code, name, id == null ? null : Long.valueOf(id), page, dataId.getDataId(), dbStringUtil, false);
            Page<NoticeTopicTreeVO> wapper = topicTreeWapper.pageCP(entityPage);
            return new PageResult(wapper.getRecords(), entityPage.getTotal(), entityPage.getSize(), entityPage.getCurrent());
        } else {
            PageResult<NoticeTopicTreeVO> pageResult = new PageResult<>();
            List<NoticeTopicTree> result = topicTreeService.queryListByNameOrCode(code, name);
            List<NoticeTopicTreeVO> wapper = topicTreeWapper.listCP(result);
            pageResult.setList(wapper);
            return pageResult;
        }
    }

    /**
     * 关键字查询
     *
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "关键字查询")
    @GetMapping(value = "/notice/topictree/keyword")
    public PageResult<NoticeTopicTreeVO> mnemonicList(@ApiParam(value = "关键字", required = false) @RequestParam(required = false) String keyword) throws Exception {
        PageResult<NoticeTopicTreeVO> pageResult = new PageResult<>();
        List<NoticeTopicTree> result = topicTreeService.queryListByKeyword(keyword);
        List<NoticeTopicTreeVO> wapper = topicTreeWapper.listCP(result);
        pageResult.setList(wapper);
        return pageResult;
    }

    /***
     * 新增
     * @param topicTreeBO
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/notice/topictree/add")
    public Result<NoticeTopicTreeVO> add(@ApiParam(value = "消息主题类型对象", required = true)
                                         @RequestBody NoticeTopicTreeBO topicTreeBO) throws Exception {
        NoticeTopicTree topicTree = topicTreeBO.entityCP(topicTreeBO);
        NoticeTopicTree entity = topicTreeService.addEntity(topicTree);
        NoticeTopicTreeVO wapper = topicTreeWapper.entityCP(entity);
        return new Result(wapper);
    }

    /***
     * 修改
     * @param topicTreeBO
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/notice/topictree/update")
    public Result update(@ApiParam(value = "消息主题类型对象", required = true)
                         @RequestBody NoticeTopicTreeBO topicTreeBO) throws Exception {
        NoticeTopicTree noticeTopicTree = topicTreeService.getOne(Wrappers.<NoticeTopicTree>query().eq(NoticeTopicTree.getIdFieldName(), topicTreeBO.getId()));
        if (noticeTopicTree == null)
            return new Result();
        Boolean modifySign = noticeTopicTree.getModify_sign();
        if (!modifySign) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_SYSTEM_DATA_CATNOT_MODIFY);
        }
        NoticeTopicTree topicTree = topicTreeBO.entityCP(topicTreeBO);
        try {
            NoticeTopicTree entity = topicTreeService.updateEntity(topicTree);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (e instanceof DuplicateKeyException) {
                throw new NotificationAdminExecption(NotificationAdminError.ERROR_DUPLICATE_TOPIC_NAME);
            } else {
                throw e;
            }
        }
        return new Result();
    }

    /***
     * 删除
     * @return
     * @throws Exception
     */
    @DeleteMapping("/notice/topictree/delete")
    public Result<String> delete(@ApiParam(value = "数据库ID", example = "1000", required = true) @RequestParam("ids") String id) throws Exception {
        NoticeTopicTree noticeTopicTree = topicTreeService.getOne(Wrappers.<NoticeTopicTree>query().eq(NoticeTopicTree.getIdFieldName(), id));
        if (noticeTopicTree == null)
            return new Result();
        Boolean modifySign = noticeTopicTree.getModify_sign();
        if (!modifySign) {
            throw new NotificationAdminExecption(NotificationAdminError.ERROR_SYSTEM_DATA_CATNOT_DELETE);
        }
        try {
            topicTreeService.deleteById(id);
        } catch (NotificationAdminExecption notificationAdminExecption) {
            if (notificationAdminExecption.getErrorDefinition().getCode() == NotificationAdminError.ERROR_DELETE_TEMPLATE.getCode()) {
                String name;
                if (StringUtils.hasText(noticeTopicTree.getI18nKey())) {
                    name = messageResourceWrapper.getMessageNotBlank(noticeTopicTree.getI18nKey());
                } else {
                    name = noticeTopicTree.getName();
                }

                throw new NotificationAdminExecption(new NotificationAdminDefinition() {
                    @Override
                    public Integer getCode() {
                        return NotificationAdminError.ERROR_TOPIC_TREE_DELETE.getCode();
                    }

                    @Override
                    public String getMessage() {
                        return String.format(messageResourceWrapper.getMessageNotBlank(NotificationAdminError.ERROR_TOPIC_TREE_DELETE.getInfo()), name);
                    }

                    @Override
                    public String getInfo() {
                        return null;
                    }
                });
            }
        }
        return new Result();
    }

}
