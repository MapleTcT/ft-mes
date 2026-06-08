package com.supcon.supfusion.systemcode.api;

import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.systemcode.api.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 
 * 数据字典某一类下具体的编码和值的服务接口
 *
 * RPC内部接口
 * <ul>
 *     <li>FeignClient的值必须和spring.application.name的值一致</li>
 *     <li>内部接口统一格式为：/internal-api/{spring.application.name}/{version}/**</li>
 * </ul>
 *
 * @author 
 * @date 20-5-11 下午2:14
 */
@FeignClient(name = "systemcode",contextId = "systemCode")
@Validated
public interface SystemCodeApiService {

    String API_PREFIX = "/service-api/systemcode";

    /**
     * 新增编码值数据
     *
     * @param systemCodeAddDTO
     */
    @PostMapping(value = API_PREFIX + "/value")
    void addValue(@Validated @RequestBody SystemCodeAddDTO systemCodeAddDTO);

    /**
     * 查询指定系统编码的编码值数据(列表形式)
     *
     * @param entityCode 系统编码
     * @param keyword    模糊查询关键字
     * @param current    翻页的页数
     * @param pageSize   每页返回的元素数量
     * @return
     */
    @GetMapping(value = API_PREFIX + "/values")
    @ResponseBody
    PageResult<SystemCodeResultDTO> queryValueList(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode,
                                                   @RequestParam(value = "keyword", required = false) String keyword,
                                                   @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("current") Integer current,
                                                   @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("pageSize") Integer pageSize);

    /**
     * 查询系统编码值列表数据,不带分页信息
     *
     * @param entityCode
     * @param keyword
     * @return
     */
    @GetMapping(value = API_PREFIX + "/values/list")
    @ResponseBody
    ListResult<SystemCodeResultDTO> queryValueListNoPage(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode, @RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "displayName", required = false) String displayName);

    /**
     * 查询指定系统编码的编码值数据(树形形式)
     *
     * @param entityCode 系统字典项编码
     * @return
     */
    @GetMapping(value = API_PREFIX + "/value/tree")
    @ResponseBody
    Result<SystemEntityDetailDTO> queryValueTree(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode);

    /**
     * 修改指定编码值数据
     *
     * @param systemCodeUpdateDTO
     */
    @PutMapping(value = API_PREFIX + "/value")
    void updateValue(@Validated @RequestBody SystemCodeUpdateDTO systemCodeUpdateDTO);

    /**
     * 删除指定编码值数据
     *
     * @param entityCode 系统字典项编码
     * @param code       值的编码
     */
    @DeleteMapping(value = API_PREFIX + "/{entityCode}/value/{code}")
    void deleteValue(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("entityCode") String entityCode,
                     @NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("code") String code);

    /**
     * 批量删除编码值
     *
     * @param entityCode
     * @param codes
     */
    @PostMapping(value = API_PREFIX + "/{entityCode}/values/{codes}")
    void batchDeleteValues(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("entityCode") String entityCode,
                           @NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("codes") String codes);

    /**
     * 查询指定节点以及该节点下的子节点
     *
     * @param parentId
     * @param keyword
     * @param current
     * @param pageSize
     * @return
     */
    @GetMapping(value = API_PREFIX + "/value/nodes")
    @ResponseBody
    PageResult<SystemCodeResultDTO> queryValueNodes(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode,
                                                    @RequestParam(value = "parentId", required = false) Long parentId,
                                                    @RequestParam(value = "keyword", required = false) String keyword,
                                                    @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("current") Integer current,
                                                    @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("pageSize") Integer pageSize);

    /**
     * 通过id查询指定的编码值
     *
     * @param id
     * @return
     */
    @GetMapping(value = API_PREFIX + "/value")
    @ResponseBody
    Result<SystemCodeResultDTO> queryValueById(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("id") Long id);

    /**
     * 查询code指定的编码值
     *
     * @param code
     * @return
     */
    @GetMapping(value = API_PREFIX + "/value/code")
    @ResponseBody
    Result<SystemCodeResultDTO> queryValueByCode(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("code") String code);

    /**
     * 查询code指定的编码值
     *
     * @param code
     * @return
     */
    @GetMapping(value = API_PREFIX + "/entity/value/code")
    @ResponseBody
    Result<SystemCodeResultDTO> queryValueByCode(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCode") String entityCode, @NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("code") String code);


    /**
     * 编码值排序
     *
     * @param systemCodeSortDTO
     */
    @PutMapping(value = API_PREFIX + "/value/sort")
    void modifyValueSort(@RequestBody SystemCodeSortDTO systemCodeSortDTO);

    /**
     * 批量新增系统编码和编码值数据
     *
     * @param list
     */
    @PostMapping(value = API_PREFIX + "/entities/values")
    void batchAddSystemCode(@Validated @RequestBody List<SystemEntityInfoDTO> list);

    /**
     * 升级操作处理系统编码和编码值数据
     *
     * @param list
     */
    @PutMapping(value = API_PREFIX + "/entities/values")
    void upgradeSystemCode(@Validated @RequestBody List<SystemEntityInfoDTO> list);

    /**
     * 批量删除某个APP下所有的系统编码和编码值数据
     *
     * @param appId
     */
    @DeleteMapping(value = API_PREFIX + "/entities/values/{appId}")
    void batchDeleteSystemCode(@NotBlank(message = "systemCode.MODULE_ID_PARAM_NECESSARY") @PathVariable("appId") String appId);

    /**
     * 通过系统编码批量查询编码列表,不带分页信息
     *
     * @param entityCodes
     * @return
     */
    @GetMapping(value = API_PREFIX + "/values/list/simple")
    @ResponseBody
    ListResult<SystemCodeResultDTO> queryValueListByEntityCodes(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @RequestParam("entityCodes") String entityCodes);

}
