package com.supcon.supfusion.systemcode.openapi;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeErrorEnum;
import com.supcon.supfusion.systemcode.common.exception.SystemCodeException;
import com.supcon.supfusion.systemcode.dao.po.SystemCodeSortPO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemCodeSortVO;
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

import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.systemcode.dao.po.SystemCodePO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityDetailPO;
import com.supcon.supfusion.systemcode.dao.po.SystemEntityPO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemCodeAddVO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemCodeResultVO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemCodeUpdateVO;
import com.supcon.supfusion.systemcode.openapi.vo.SystemEntityDetailVO;
import com.supcon.supfusion.systemcode.common.constants.Constants;
import com.supcon.supfusion.systemcode.service.SystemCodeService;
import com.supcon.supfusion.systemcode.service.SystemEntityService;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 字典项目某一类下具体的编码服务开放接口
 *
 * @author 
 * @date 20-5-11 下午14:15
 */
@Slf4j
@Setter
@Getter
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "systemcode" + HttpConstants.URL_SPLITER + "v1")
@Validated
public class SystemCodeController extends BaseController {

    @Autowired
    private SystemCodeService systemCodeService;

    @Autowired
    private SystemEntityService systemEntityService;

    /**
     * 新增编码值
     *
     * @return
     */
    @PostMapping(value = "/value")
    void addValue(@Validated @RequestBody SystemCodeAddVO systemCodeAddVo) {
        if (!Pattern.matches(Constants.PATTERN_CODE, systemCodeAddVo.getCode())) {
            throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
        }
        SystemCodePO systemCodePO = new SystemCodePO();
        BeanUtils.copyProperties(systemCodeAddVo, systemCodePO);
        systemCodeService.addValue(systemCodePO);
    }

    /**
     * 查询指定系统编码的编码值数据(列表形式)
     *
     * @param entityCode
     * @param keyword
     * @param current
     * @param pageSize
     * @return
     */
    @GetMapping(value = "/values")
    @ResponseBody
    PageResult<SystemCodeResultVO> queryValueList(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode, String keyword,
                                                  @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("current") Integer current, @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("pageSize") Integer pageSize) {

        PageResult<SystemCodePO> poPageResult = systemCodeService.queryValueList(entityCode, keyword, current, pageSize);
        PageResult<SystemCodeResultVO> voPageResult = new PageResult<>(null, poPageResult.getPagination().getTotal(), poPageResult.getPagination().getPageSize(), poPageResult.getPagination().getCurrent());
        List<SystemCodeResultVO> voList = new ArrayList<>(poPageResult.getList().size());
        poPageResult.getList().stream().forEach(po -> {
            SystemCodeResultVO vo = new SystemCodeResultVO();
            BeanUtils.copyProperties(po, vo);
            voList.add(vo);
        });
        voPageResult.setList(voList);
        return voPageResult;
    }

    /**
     * 查询系统编码值列表数据,不带分页信息
     *
     * @param entityCode
     * @param keyword
     * @return
     */
    @GetMapping(value = "/values/list")
    @ResponseBody
    ListResult<SystemCodeResultVO> queryValueListNoPage(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode, String keyword, String displayName) {

        List<SystemCodePO> systemCodePOList = systemCodeService.queryValueListNoPage(entityCode, keyword, displayName, "");
        List<SystemCodeResultVO> systemCodeResultVOList = new ArrayList<>();
        for (SystemCodePO systemCodePO : systemCodePOList) {
            SystemCodeResultVO systemCodeResultVO = new SystemCodeResultVO();
            BeanUtils.copyProperties(systemCodePO, systemCodeResultVO);
            systemCodeResultVOList.add(systemCodeResultVO);
        }
        return new ListResult<>(systemCodeResultVOList);
    }

    /**
     * 查询指定系统编码的编码值数据(树形形式)
     *
     * @param entityCode 系统字典项编码
     * @return
     */
    @GetMapping(value = "/value/tree")
    @ResponseBody
    Result<SystemEntityDetailVO> queryValueTree(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode) {

        SystemEntityPO systemEntityPO = systemEntityService.queryEntityByCode(entityCode);
        SystemEntityDetailPO systemEntityDetailPO = new SystemEntityDetailPO();
        BeanUtils.copyProperties(systemEntityPO, systemEntityDetailPO);
        systemCodeService.queryValueTree(systemEntityDetailPO);

        SystemEntityDetailVO systemEntityDetailVO = new SystemEntityDetailVO();
        BeanUtils.copyProperties(systemEntityDetailPO, systemEntityDetailVO);
        return new Result<>(systemEntityDetailVO);
    }

    /**
     * 查询指定系统编码的编码值的字节点数据(列表形式)
     *
     * @param entityCode
     * @param parentId
     * @param keyword
     * @param current
     * @param pageSize
     * @return
     */
    @GetMapping(value = "/value/nodes")
    @ResponseBody
    PageResult<SystemCodeResultVO> queryValueNodes(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode, Long parentId, String keyword,
                                                   @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("current") Integer current, @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("pageSize") Integer pageSize) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        PageResult<SystemCodePO> poPageResult = systemCodeService.queryValueNodes(entityCode, parentId, keyword, current, pageSize);
        List<SystemCodeResultVO> voList = new ArrayList<>(poPageResult.getList().size());
        poPageResult.getList().stream().forEach(po -> {
            SystemCodeResultVO vo = new SystemCodeResultVO();
            BeanUtils.copyProperties(po, vo);
            voList.add(vo);
        });
        return new PageResult<>(voList, poPageResult.getPagination().getTotal(), poPageResult.getPagination().getPageSize(), poPageResult.getPagination().getCurrent());
    }

    @GetMapping(value = "/value")
    @ResponseBody
    Result<SystemCodeResultVO> queryValueById(@NotBlank(message = "systemCode.ID_PARAM_NECESSARY") @RequestParam("id") Long id) {
        SystemCodePO systemCodePO = systemCodeService.queryValueById(id);
        SystemCodeResultVO systemCodeResultVO = new SystemCodeResultVO();
        BeanUtils.copyProperties(systemCodePO, systemCodeResultVO);
        return new Result<>(systemCodeResultVO);
    }

    /**
     * 修改指定编码值数据
     *
     * @param systemCodeUpdateVO
     */
    @PutMapping(value = "/value")
    void updateValue(@Validated @RequestBody SystemCodeUpdateVO systemCodeUpdateVO) {
        if (!Pattern.matches(Constants.PATTERN_CODE, systemCodeUpdateVO.getCode())) {
            throw new SystemCodeException(SystemCodeErrorEnum.CODE_INPUT_FORMAT_ERROR);
        }
        SystemCodePO systemCodePo = new SystemCodePO();
        BeanUtils.copyProperties(systemCodeUpdateVO, systemCodePo);
        systemCodeService.updateValue(systemCodePo);
    }

    @PutMapping(value = "/value/sort")
    void modifyValueSort(@RequestBody SystemCodeSortVO systemCodeSortVO) {
        SystemCodeSortPO systemCodeSortPO = new SystemCodeSortPO();
        BeanUtils.copyProperties(systemCodeSortVO, systemCodeSortPO);
        systemCodeService.modifyValueSort(systemCodeSortPO);
    }

    /**
     * 删除指定编码值数据
     *
     * @param entityCode 系统字典项编码
     * @param code       系统编码
     */
    @DeleteMapping(value = "/{entityCode}/value/{code}")
    void deleteValue(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("entityCode") String entityCode, @NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("code") String code) {
        systemCodeService.deleteValue(entityCode, code);
    }

    /**
     * 批量删除编码值数据
     *
     * @param entityCode
     * @param codes
     */
    @DeleteMapping(value = "/{entityCode}/values/{codes}")
    void batchDeleteValues(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("entityCode") String entityCode, @NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("codes") String codes) {
        String[] codeArray = codes.split(",");
        if (codeArray == null || codeArray.length <= 0) {
            throw new SystemCodeException(SystemCodeErrorEnum.SYSTEM_DELETE_DATA_IS_NOT_EMPTY);
        }
        List<String> codeList = Arrays.asList(codeArray);
        systemCodeService.batchDeleteValues(entityCode, codeList);
    }
}
