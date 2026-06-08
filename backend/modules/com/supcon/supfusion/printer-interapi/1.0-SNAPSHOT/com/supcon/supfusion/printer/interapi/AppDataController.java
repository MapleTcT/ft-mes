package com.supcon.supfusion.printer.interapi;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.printer.interapi.vo.PageDataQueryVO;
import com.supcon.supfusion.printer.interapi.vo.PageDataVO;
import com.supcon.supfusion.printer.service.PrinterAppDataService;
import com.supcon.supfusion.printer.service.bo.PageDataBO;
import com.supcon.supfusion.printer.service.bo.PageDataQueryBO;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * app信息控制类
 * @author yuyimao
 * @date 2020/10/21 3:04 下午
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER  + "printer" + HttpConstants.URL_SPLITER + "v1")
@Api(tags = "app信息", value = "app信息", hidden = true)
public class AppDataController extends BaseController {
    @Autowired
    private PrinterAppDataService printerAppDataService;

    /**
     * 获取app列表接口
     * @param pageDataQueryVO
     * @return
     */
    @GetMapping("/apps")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="获取app列表接口", httpMethod="GET")
    public Result<?> getAppList(PageDataQueryVO pageDataQueryVO) {
        PageDataQueryBO pageDataQueryBO = new PageDataQueryBO();
        BeanUtils.copyProperties(pageDataQueryVO, pageDataQueryBO);
        List<PageDataBO> pageDataBOList = printerAppDataService.getAppList(pageDataQueryBO);
        List<PageDataVO> pageDataVOList = null;
        if(StringUtils.isNotBlank(pageDataQueryVO.getName())){
            pageDataVOList = pageDataBOList.stream().filter(p -> p.getName().contains(pageDataQueryVO.getName()))
                    .map(p -> {
                        PageDataVO pageDataVO = new PageDataVO();
                        BeanUtils.copyProperties(p, pageDataVO);
//                        pageDataVO.setCode(pageDataQueryVO.getSource() + "-" + p.getCode());
                        pageDataVO.setPCode(p.getParentCode());
                        return pageDataVO;
                    }).collect(Collectors.toList());

        }else {
            List<PageDataBO> pageDataBOParentList = pageDataBOList.stream().filter(p -> p.getLevel() == 1).collect(Collectors.toList());
            List<PageDataBO> pageDataBOChildrenList = pageDataBOList.stream().filter(p -> p.getLevel() != 1).collect(Collectors.toList());
            pageDataVOList = getAppDataTree(pageDataBOParentList, pageDataBOChildrenList, pageDataQueryVO.getSource());
        }
        return new Result<>(pageDataVOList);
    }

    /**
     * 获取page列表接口
     * @param pageDataQueryVO
     * @return
     */
    @GetMapping("/pages")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value="获取page列表接口", httpMethod="GET")
    public Result<?> getPageList(PageDataQueryVO pageDataQueryVO) {
        PageDataQueryBO pageDataQueryBO = new PageDataQueryBO();
        BeanUtils.copyProperties(pageDataQueryVO, pageDataQueryBO);
        List<PageDataBO> pageDataBOList = printerAppDataService.getPageList(pageDataQueryBO);
        if(CollectionUtils.isEmpty(pageDataBOList)){
            return new Result<>();
        }
//        List<PageDataVO> pageDataVOList = pageDataBOList.stream()
//                .map(p -> {
//                    PageDataVO pageDataVO = new PageDataVO();
//                    BeanUtils.copyProperties(p, pageDataVO);
////                    pageDataVO.setCode(pageDataQueryVO.getSource() + "-" + p.getCode());
//                    pageDataVO.setPCode(p.getParentCode());
//                    return pageDataVO;
//                }).collect(Collectors.toList());


        List<PageDataBO> pageDataBOParentList = pageDataBOList.stream().filter(p -> p.getLevel() == 1).collect(Collectors.toList());
        List<PageDataBO> pageDataBOChildrenList = pageDataBOList.stream().filter(p -> p.getLevel() != 1).collect(Collectors.toList());
        List<PageDataVO> pageDataVOList = pageDataVOList = getAppDataTree(pageDataBOParentList, pageDataBOChildrenList, pageDataQueryVO.getSource());

        return new Result<>(pageDataVOList);
    }


    /**
     *获取app树
     * @param childrenList 不包含最高层次app节点的app树
     * @param parentList 最高层次app节点的app树
     * @return
     */
    private List<PageDataVO> getAppDataTree(List<PageDataBO> parentList, List<PageDataBO> childrenList, Integer source){
        List<PageDataVO> response = new ArrayList<PageDataVO>();
        for (PageDataBO parent : parentList) {
            PageDataVO pageDataVO = new PageDataVO();
            BeanUtils.copyProperties(parent, pageDataVO);
//            pageDataVO.setCode(source + "-" + parent.getCode());
            pageDataVO.setPCode(parent.getParentCode());
            List<PageDataVO> appTreeList = iterateNodes(childrenList, parent.getCode(), source);
            pageDataVO.setChildren(appTreeList);
            response.add(pageDataVO);
        }
        return response;
    }

    /**
     *app树查询方法
     * @param childrenTree 不包含最高层次app节点的app树
     * @param pCode 父类code
     * @return
     */
    private List<PageDataVO> iterateNodes(List<PageDataBO> childrenTree, String pCode, Integer source){
        List<PageDataVO> result = new ArrayList<PageDataVO>();
        for (PageDataBO pageDataBO : childrenTree) {
            PageDataVO pageDataVO = new PageDataVO();
            BeanUtils.copyProperties(pageDataBO, pageDataVO);
//            pageDataVO.setCode(source + "-" + pageDataBO.getCode());
            pageDataVO.setPCode(pageDataBO.getParentCode());
            String code = pageDataBO.getCode();
            String parentCode = pageDataBO.getParentCode();
            if(parentCode.equals(pCode)){
                List<PageDataVO> iterateFactoryTree = iterateNodes(childrenTree,code, source);
                pageDataVO.setChildren(iterateFactoryTree);
                result.add(pageDataVO);
            }
        }
        return result;
    }
}
