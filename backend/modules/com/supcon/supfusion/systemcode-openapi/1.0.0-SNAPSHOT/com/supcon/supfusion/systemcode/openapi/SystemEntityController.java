package com.supcon.supfusion.systemcode.openapi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.systemcode.common.constants.Constants;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeErrorEnum;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeException;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemEntityAddVO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemEntityResultVO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemEntityUpdateVO;
import com.supcon.supfusion.systemcode.service.SystemEntityService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 字典项目服务开放接口
 *
 * @author 
 * @date 20-5-11 下午14:15
 */
@Slf4j
@Setter
@Getter
@Validated
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "systemcode" + HttpConstants.URL_SPLITER + "v1")
public class SystemEntityController extends BaseController {

    @Autowired
    private SystemEntityService systemEntityService;

    /**
     * 查询系统字典列表
     *
     * @param keyword
     * @param moduleId
     * @param current
     * @param pageSize
     * @return
     */
    @GetMapping("/entities")
    @ResponseBody
    public PageResult<SystemEntityResultVO> queryEntities(String keyword, String moduleId, @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("current") Integer current,
                                                          @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("pageSize") Integer pageSize) {
        PageResult<SystemEntityPO> systemEntityPageResult = systemEntityService.queryEntities(keyword, moduleId, current, pageSize);
        Collection<SystemEntityPO> systemEntityPOList = systemEntityPageResult.getList();
        List<SystemEntityResultVO> systemEntityResultVOList = JSONArray.parseArray(JSONObject.toJSONString(systemEntityPOList), SystemEntityResultVO.class);
        return new PageResult<>(systemEntityResultVOList, systemEntityPageResult.getPagination().getTotal(), systemEntityPageResult.getPagination().getPageSize(), systemEntityPageResult.getPagination().getCurrent());
    }

    /**
     * 查询指定系统字典数据
     *
     * @param code
     * @return
     */
    @GetMapping(value = "/entity")
    @ResponseBody
    Result<SystemEntityResultVO> queryEntityByCode(@RequestParam("code") String code) {
        SystemEntityPO systemEntityPO = systemEntityService.queryEntityByCode(code);
        SystemEntityResultVO systemEntityResultVO = new SystemEntityResultVO();
        if (Objects.nonNull(systemEntityPO)) {
            BeanUtils.copyProperties(systemEntityPO, systemEntityResultVO);
        }
        return Result.data(systemEntityResultVO);
    }

    /**
     * 创建系统字典数据
     *
     * @param systemEntityAddVO
     */
    @PostMapping(value = "/entity")
    void addEntity(@Validated @RequestBody SystemEntityAddVO systemEntityAddVO) {
        if (!Pattern.matches(Constants.PATTERN_CODE, systemEntityAddVO.getCode())) {
            throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
        }
        SystemEntityPO systemEntityPO = new SystemEntityPO();
        BeanUtils.copyProperties(systemEntityAddVO, systemEntityPO);
        systemEntityService.addEntity(systemEntityPO);
    }

    /**
     * 修改数据字典数据
     *
     * @param systemEntityUpdateVO
     */
    @PutMapping(value = "/entity")
    void updateEntity(@Validated @RequestBody SystemEntityUpdateVO systemEntityUpdateVO) {
        if (!Pattern.matches(Constants.PATTERN_CODE, systemEntityUpdateVO.getCode())) {
            throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
        }
        SystemEntityPO systemEntityPO = new SystemEntityPO();
        BeanUtils.copyProperties(systemEntityUpdateVO, systemEntityPO);
        systemEntityService.updateEntity(systemEntityPO);
    }

    /**
     * 删除指定系统编码数据
     *
     * @param code 系统字典的编码
     */
    @DeleteMapping(value = "/entity/{code}")
    void deleteEntityByCode(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("code") String code) {
        systemEntityService.deleteEntityByCode(code);
    }

    /**
     * 批量删除系统编码数据
     *
     * @param codes 系统字典编码数组
     */
    @DeleteMapping(value = "/entities/{codes}")
    void batchDeleteEntities(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("codes") String codes) {
        String[] codeArray = codes.split(",");
        if (codeArray == null || codeArray.length <= 0) {
            throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_DELETE_DATA_IS_NOT_EMPTY);
        }
        List<String> codeList = Arrays.asList(codeArray);
        systemEntityService.batchDeleteEntities(codeList);
    }
}
