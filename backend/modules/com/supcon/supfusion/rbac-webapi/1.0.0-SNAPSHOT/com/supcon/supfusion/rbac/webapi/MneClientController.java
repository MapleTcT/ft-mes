package com.supcon.supfusion.rbac.webapi;


import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.rbac.common.enumerate.MneServiceType;
import com.supcon.supfusion.rbac.common.utils.StringUtils;
import com.supcon.supfusion.rbac.dao.MenuInfoMapper;
import com.supcon.supfusion.rbac.dao.field.MenuInfoField;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.service.IMenuInfoMneCodeService;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.MneClientService;
import com.supcon.supfusion.rbac.service.bo.MneQueryBO;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 袁阳
 * @since 2020-09-23
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "rbac" + HttpConstants.URL_SPLITER + "v1")
@Validated
public class MneClientController extends BaseController {

    @Autowired
    private IMenuInfoMneCodeService menuInfoMneCodeService;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private MenuInfoMapper menuInfoMapper;


    @GetMapping("/mneClient")
    @ApiOperation(value = "助记码查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "类型", required = true, paramType = "query"),
            @ApiImplicitParam(name = "searchContent", value = "搜索内容", required = false, paramType = "query"),
            @ApiImplicitParam(name = "specialNoCrossCompany", value = "这个属性特例用来在多组织情况下禁止用户分配权限时能选到其他公司，正常情况下这个参数为空", required = false, paramType = "query"),
    })
    public ListResult<Map<String,Object>> mneQuery(@RequestParam(value = "type") String type,@RequestParam(value = "searchContent",required = false) String searchContent,@RequestParam(value = "showRange",required = false) String showRange,@RequestParam(value = "conditionParams",required = false) String conditionParams,@RequestParam(value = "isCrossCompany",required = false) String isCrossCompany,@RequestParam(value = "specialNoCrossCompany",required = false) String specialNoCrossCompany,@RequestParam(value = "showNumber",required = false) String showNumber,@RequestParam(value = "fieldCode",required = false) String fieldCode,@RequestParam(value = "selectPeople",required = false) String selectPeople,@RequestParam(value = "deploymentId",required = false) String deploymentId,@RequestParam(value = "outcome",required = false) String outcome,@RequestParam(value = "sourceStaff",required = false) String sourceStaff) {
        //TODO 考虑到菜单助记码暂时条件简单且对业务不熟悉，暂不翻译乱七八糟的条件
        boolean flag = false;
        boolean customerRule = false;
        String conditionStr = "";
        MneQueryBO mneQueryBO = new MneQueryBO();
        if (!ObjectUtils.isEmpty(searchContent)){
            if(searchContent.contains("*")){
                customerRule = true;
            }
            searchContent=searchContent.toLowerCase();
        }else{
            searchContent = "*";
        }
        mneQueryBO.setSearchContent(searchContent);
        MneClientService mcs = null;
        if (type.equals(MneServiceType.MENUINFO.toString())){
            mcs = (MneClientService) menuInfoMneCodeService;
        }
        if (!ObjectUtils.isEmpty(mcs)){
            List<Map<String, Object>> maps = mcs.search(mneQueryBO, conditionStr);
            //菜单助记码搜索特殊处理
            if (type.equals(MneServiceType.MENUINFO.toString())){
                //获取当前用户有权限的菜单
                List<MenuInfoPO> userMenu = menuInfoService.findUserMenuList(UserContext.getUserContext().getUserId());
                if (!ObjectUtils.isEmpty(userMenu)){
                    //过滤掉用户没权限的菜单
                    return new ListResult<>(maps.stream().filter(map -> {
                        Optional<MenuInfoPO> infoPO = userMenu.stream().filter(menuInfoPO -> map.get("id").equals(menuInfoPO.getId())).findFirst();
                        return infoPO.isPresent();
                    }).collect(Collectors.toList()));
                }
            }
        }
        return new ListResult<>();
    }

    @GetMapping("/app_menus_search")
    @ApiOperation(value = "菜单助记码查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "content", value = "搜索内容", required = false, paramType = "query"),
            @ApiImplicitParam(name = "filter_sys", value = "是否过滤系统菜单", required = false, paramType = "query")
    })
    public ListResult<Map<String,Object>> app_menus_search(@RequestParam(value = "content",required = false) String content,
                                                           @RequestParam(value = "filter_sys",required = false) Boolean filter_sys) {
        MneQueryBO mneQueryBO = new MneQueryBO();
        if (ObjectUtils.isEmpty(content)){
            return new ListResult<>();
        }
        mneQueryBO.setSearchContent(content);
        MneClientService mneClientService = (MneClientService) menuInfoMneCodeService;
        List<Map<String, Object>> maps = mneClientService.search(mneQueryBO, "");
        if (filter_sys != null && !filter_sys) {
            return new ListResult<>(maps.stream().collect(Collectors.toList()));
        }
        List sys_menus = Arrays.asList(MenuInfoField.sys_menus);
        return new ListResult<>(maps.stream().filter(map ->
                !sys_menus.contains(map.get("app"))
        ).collect(Collectors.toList()));
    }
}

