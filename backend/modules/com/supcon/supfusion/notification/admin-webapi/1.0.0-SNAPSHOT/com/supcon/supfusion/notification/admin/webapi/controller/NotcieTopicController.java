package com.supcon.supfusion.notification.admin.webapi.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.util.DateTimeUtil;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeRecieveRangeExt;
import com.supcon.supfusion.notification.admin.dao.entities.NoticeTopic;
import com.supcon.supfusion.notification.admin.manager.service.IManagerTopicService;
import com.supcon.supfusion.notification.admin.manager.service.IOrganizationStaffService;
import com.supcon.supfusion.notification.admin.manager.service.OrganizationService;
import com.supcon.supfusion.notification.admin.manager.service.RbacService;
import com.supcon.supfusion.notification.admin.service.NoticeTopicService;
import com.supcon.supfusion.notification.admin.service.bo.NoticeTopicBO;
import com.supcon.supfusion.notification.admin.service.bo.NoticeTopicListBO;
import com.supcon.supfusion.notification.admin.webapi.utils.NoticeTopicWapper;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTemplateVO;
import com.supcon.supfusion.notification.admin.webapi.vo.NoticeTopicVO;
import com.supcon.supfusion.notification.admin.webapi.vo.ReceiveRangeVO;
import com.supcon.supfusion.notification.common.bean.RangeType;
import com.supcon.supfusion.organization.api.dto.DepartmentDetailDTO;
import com.supcon.supfusion.organization.api.dto.PersonDetailDTO;
import com.supcon.supfusion.organization.api.dto.PositionDetailDTO;
import com.supcon.supfusion.rbac.api.dto.RoleDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ${description}
 *
 * @author huangxin2
 * @create 2020/5/7 20:03
 */
@ResponseBody
@Api(description = "NoticeTopic-API", tags = {"消息主题API"})
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "notification-admin" + HttpConstants.URL_SPLITER + "v1")
@Slf4j
public class NotcieTopicController {
    @Resource(name = "adminNoticeTopicServiceImpl")
    private NoticeTopicService topicService;
    @Autowired
    private NoticeTopicWapper topicWapper;
    @Autowired
    private IManagerTopicService managerTopicService;
    @Autowired
    private IOrganizationStaffService organizationStaffService;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private OrganizationService organizationService;

    /**
     * 分页查询对象
     *
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "分页查询消息主题")
    @GetMapping(value = "/notice/topic/topics")
    public PageResult<NoticeTopicVO> list(@ApiParam(value = "消息主题编码", required = false) @RequestParam(required = false) String code,
                                          @ApiParam(value = "消息主题名称", required = false) @RequestParam(required = false) String name,
                                          @ApiParam(value = "消息主题类型ID", required = false) @RequestParam(required = false) String topicTreeId,
                                          @ApiParam(value = "通知方式ID", required = false) @RequestParam(required = false) String noticeTypeIds,
                                          @ApiParam(value = "消息模板名称", required = false) @RequestParam(required = false) String templateName,
                                          @ApiParam(value = "页码", required = false) @RequestParam(required = false) Integer pageNo,
                                          @ApiParam(value = "分页大小", required = false) @RequestParam(required = false) Integer pageSize) throws Exception {
        PageResult<NoticeTopicVO> pageResult;
        if (pageNo != null && pageSize != null && pageNo > -1 && pageSize > -1) {
            Page<NoticeTopicListBO> pageList = topicService.queryPageList(code, name, topicTreeId == null ? null : Long.valueOf(topicTreeId), noticeTypeIds, templateName, pageNo, pageSize);

            /**
             * 所有topic统一处理接收范围
             */
            List<String> allStaffCodes = new ArrayList<>();
            List<String> allDeptCodes = new ArrayList<>();
            List<String> allPositionCodes = new ArrayList<>();
            List<String> allRoleCodes = new ArrayList<>();
            List<NoticeTopicListBO> noticeTopicListBOS = pageList.getRecords();
            noticeTopicListBOS.forEach(noticeTopicListBO -> {
                allStaffCodes.addAll(noticeTopicListBO.getStaffCodes());
                allDeptCodes.addAll(noticeTopicListBO.getDeptCodes());
                allPositionCodes.addAll(noticeTopicListBO.getPositionCodes());
                allRoleCodes.addAll(noticeTopicListBO.getRoleCodes());
            });

            Map<String, String> staffCodeName = new HashMap();
            Map<String, String> deptCodeName = new HashMap();
            Map<String, String> positionCodeName = new HashMap();
            Map<String, String> roleCodeName = new HashMap();
            if (allStaffCodes.size() > 0) {
                List<PersonDetailDTO> personDetailDTOS = organizationService.getStaffs(allStaffCodes);
                if (personDetailDTOS != null) {
                    personDetailDTOS.forEach(personDetailDTO -> staffCodeName.put(personDetailDTO.getCode(), personDetailDTO.getName()));
                }
            }
            if (allDeptCodes.size() > 0) {
                List<DepartmentDetailDTO> departmentDetailDTOS = organizationService.getDepartmentNames(allDeptCodes);
                if (departmentDetailDTOS != null) {
                    departmentDetailDTOS.forEach(departmentDetailDTO -> deptCodeName.put(departmentDetailDTO.getCode(), departmentDetailDTO.getName()));
                }
            }
            if (allPositionCodes.size() > 0) {
                List<PositionDetailDTO> positionDetailDTOS = organizationService.getPositionNames(allPositionCodes);
                if (positionDetailDTOS != null) {
                    positionDetailDTOS.forEach(positionDetailDTO -> positionCodeName.put(positionDetailDTO.getCode(), positionDetailDTO.getName()));
                }
            }
            if (allRoleCodes.size() > 0) {
                List<RoleDTO> roleDTOS = rbacService.getRoles(allRoleCodes);
                if (roleDTOS != null) {
                    roleDTOS.forEach(roleDTO -> roleCodeName.put(roleDTO.getCode(), roleDTO.getName()));
                }
            }
            noticeTopicListBOS.forEach(noticeTopicListBO -> {
                StringBuilder reveiver = new StringBuilder();
                if (noticeTopicListBO.getStaffCodes() != null && noticeTopicListBO.getStaffCodes().size() > 0) {
                    noticeTopicListBO.getStaffCodes().forEach(staffCode -> {
                        String staffName = staffCodeName.get(staffCode);
                        if (StringUtils.hasText(staffName)) {
                            reveiver.append(staffName);
                            reveiver.append(",");
                        }
                    });
                }
                if (noticeTopicListBO.getDeptCodes() != null && noticeTopicListBO.getDeptCodes().size() > 0) {
                    noticeTopicListBO.getDeptCodes().forEach(deptCode -> {
                        String deptName = deptCodeName.get(deptCode);
                        if (StringUtils.hasText(deptName)) {
                            reveiver.append(deptName);
                            reveiver.append(",");
                        }
                    });
                }
                if (noticeTopicListBO.getPositionCodes() != null && noticeTopicListBO.getPositionCodes().size() > 0) {
                    noticeTopicListBO.getPositionCodes().forEach(positionCode -> {
                        String positionName = positionCodeName.get(positionCode);
                        if (StringUtils.hasText(positionName)) {
                            reveiver.append(positionName);
                            reveiver.append(",");
                        }
                    });
                }
                if (noticeTopicListBO.getRoleCodes() != null && noticeTopicListBO.getRoleCodes().size() > 0) {
                    noticeTopicListBO.getRoleCodes().forEach(roleCode -> {
                        String roleName = roleCodeName.get(roleCode);
                        if (StringUtils.hasText(roleName)) {
                            reveiver.append(roleName);
                            reveiver.append(",");
                        }
                    });
                }
                if (reveiver.length() > 0) {
                    reveiver.deleteCharAt(reveiver.length() - 1);
                }
                noticeTopicListBO.setReceiver(reveiver.toString());
            });

            pageResult = new PageResult(pageList.getRecords(), pageList.getTotal(), pageList.getSize(), pageList.getCurrent());
        } else {
            pageResult = new PageResult();
            List<NoticeTopic> result = topicService.queryList(code, name, topicTreeId == null ? null : Long.valueOf(topicTreeId));
            List<NoticeTopicVO> wapper = topicWapper.listCP(result);
            pageResult.setList(wapper);
        }
        return pageResult;
    }

    /**
     * 关键字查询
     *
     * @param code
     * @param name
     * @param templateName
     * @param receiver
     * @return
     * @throws Exception
     */
    @ApiOperation(value = "关键字查询")
    @GetMapping(value = "/notice/topic/keyword")
    public ListResult<NoticeTemplateVO> mnemonicList(@ApiParam(value = "消息主题编码", required = false) @RequestParam(required = false) String code,
                                                     @ApiParam(value = "消息主题名称", required = false) @RequestParam(required = false) String name,
                                                     @ApiParam(value = "消息模板名称", required = false) @RequestParam(required = false) String templateName,
                                                     @ApiParam(value = "消息主题类型ID", required = false) @RequestParam(required = false) String topicTreeId,
                                                     @ApiParam(value = "接收人", required = false) @RequestParam(required = false) String receiver) throws Exception {
        List<NoticeTopic> result = topicService.queryListByKeyword(code, name, templateName, receiver, topicTreeId);
        List<NoticeTopicVO> wapper = topicWapper.listCP(result);
        return new ListResult(wapper);
    }

    /***
     * 新增
     * @param topicBO
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/notice/topic/add")
    public Result add(@ApiParam(value = "消息主题对象", required = true)
                      @RequestBody NoticeTopicBO topicBO) throws Exception {
        NoticeTopic topic = topicBO.entityCP(topicBO);
        /*NoticeTopic result = topicService.addEntity(topic);*/
        managerTopicService.addTopicAndRangeType(topic);
        return new Result();
    }

    /***
     * 修改
     * @param topicBO
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/notice/topic/update")
    public Result update(@ApiParam(value = "消息主题对象", required = true)
                         @RequestBody NoticeTopicBO topicBO) throws Exception {
        NoticeTopic topic = topicBO.entityCP(topicBO);
//        NoticeTopic entity = topicService.updateEntity(topic, topicBO.getTopicTmplMap());
        NoticeTopic entity = managerTopicService.updateTopicAndRangeType(topic);
        return new Result();
    }

    /***
     * 删除
     * @param ids 多条id逗号分隔
     * @return
     * @throws Exception
     */
    @DeleteMapping("/notice/topic/delete")
    public Result<String> delete(@ApiParam(value = "数据库ID", example = "1000,1003,1004", required = true) @RequestParam String ids) throws Exception {
//        String result = topicService.delEntity(codes);
        String result = managerTopicService.delTopicAndRangeType(ids);
        return new Result(result);
    }


    @ApiOperation(value = "校验消息主题编码是否重复", notes = "大小写不敏感")
    @GetMapping("/notice/topic/validcode")
    public Result<Boolean> validcode(@ApiParam(value = "消息主题编码") @RequestParam String code) {
        Boolean result = topicService.validTopicCode(code);
        return new Result<Boolean>(result);
    }

    @ApiOperation(value = "查询消息主题的接收范围信息")
    @GetMapping(value = "/notice/receiverange/rangemap")
    public Result<List<Map<String, List<ReceiveRangeVO>>>> receiverangeMap(@ApiParam(value = "消息主题ID", required = true) @RequestParam String topicId) {
        Map<String, List<NoticeRecieveRangeExt>> map = organizationStaffService.queryReceiveRange(topicId != null ? Long.valueOf(topicId) : null);
        List<Map<String, List<ReceiveRangeVO>>> rangeList = new ArrayList<Map<String, List<ReceiveRangeVO>>>();
        if (map != null && map.size() > 0) {
            for (String range : map.keySet()) {
                List<NoticeRecieveRangeExt> noticeRecieveRangeExts = map.get(range);
                if (noticeRecieveRangeExts == null || noticeRecieveRangeExts.size() == 0) {
                    continue;
                }
                List<String> codes = new ArrayList<>();
                Map<String, List<ReceiveRangeVO>> receiverMap = new LinkedHashMap<>();
                noticeRecieveRangeExts.forEach(noticeRecieveRangeExt -> {
                    codes.add(noticeRecieveRangeExt.getReceiverCode());
                });
                if (codes == null || codes.size() == 0) {
                    continue;
                }

                List<ReceiveRangeVO> receiveRangeVOS = new ArrayList<>();
                if (RangeType.STAFF.value().equals(range)) {
                    List<PersonDetailDTO> personDetailDTOS = organizationService.getStaffs(codes);
                    if (personDetailDTOS != null && personDetailDTOS.size() > 0) {
                        personDetailDTOS.forEach(personDetailDTO -> {
                            receiveRangeVOS.add(new ReceiveRangeVO(personDetailDTO.getId(), personDetailDTO.getCode(), personDetailDTO.getName()));
                        });
                    }
                } else if (RangeType.DEPARTMENT.value().equals(range)) {
                    List<DepartmentDetailDTO> departmentDetailDTOS = organizationService.getDepartmentNames(codes);
                    if (departmentDetailDTOS != null && departmentDetailDTOS.size() > 0) {
                        departmentDetailDTOS.forEach(departmentDetailDTO -> {
                            receiveRangeVOS.add(new ReceiveRangeVO(departmentDetailDTO.getId(), departmentDetailDTO.getCode(), departmentDetailDTO.getName()));
                        });
                    }
                } else if (RangeType.POSITION.value().equals(range)) {
                    List<PositionDetailDTO> positionDetailDTOS = organizationService.getPositionNames(codes);
                    if (positionDetailDTOS != null && positionDetailDTOS.size() > 0) {
                        positionDetailDTOS.forEach(positionDetailDTO -> {
                            receiveRangeVOS.add(new ReceiveRangeVO(positionDetailDTO.getId(), positionDetailDTO.getCode(), positionDetailDTO.getName()));
                        });
                    }
                } else if (RangeType.ROLE.value().equals(range)) {
                    List<RoleDTO> roleDTOS = rbacService.getRoles(codes);
                    if (roleDTOS != null && roleDTOS.size() > 0) {
                        roleDTOS.forEach(roleDTO -> {
                            receiveRangeVOS.add(new ReceiveRangeVO(roleDTO.getId(), roleDTO.getCode(), roleDTO.getName()));
                        });
                    }
                }
                receiveRangeVOS.sort((last, next) -> next.getId().compareTo(last.getId()));
                receiverMap.put(range, receiveRangeVOS);
                rangeList.add(receiverMap);
            }
        }
        return new Result(rangeList);
    }

}
