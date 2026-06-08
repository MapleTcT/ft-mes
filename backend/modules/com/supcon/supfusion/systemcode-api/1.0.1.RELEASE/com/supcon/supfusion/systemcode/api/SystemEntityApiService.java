package com.supcon.supfusion.systemcode.api;

import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityAddDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityResultDTO;
import com.supcon.supfusion.systemcode.api.dto.SystemEntityUpdateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 
 * 数据字典 , 字典项目编码管理服务接口
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
@FeignClient(name = "systemcode",contextId = "systemEntity")
//@ServiceApi(path = HttpConstants.URL_SERVICEAPI + HttpConstants.URL_SPLITER + "systemcode")
public interface SystemEntityApiService {

//    /**
//     * 查询系统编码字典列表
//     * @param keyword
//     * @param moduleId
//     * @param current
//     * @param pageSize
//     * @return
//     */
//    @GetMapping(value = "/entities")
//    @ResponseBody
//    PageResult<SystemEntityResultDTO> queryEntities(String keyword, String moduleId, @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("current") Integer current,
//                                                    @Min(value = 1, message = "systemCode.CURRENT_PAGE_MIN_1") @RequestParam("pageSize") Integer pageSize);

    /**
     * 查询指定系统编码字典数据
     * @param code
     * @return
     */
    @GetMapping(value = "/service-api/systemcode/entity")
    @ResponseBody
    Result<SystemEntityResultDTO> queryEntityByCode(@RequestParam("code") String code);

    /**
     * 创建系统编码字段数据
     * @param systemEntityAddDTO
     */
    @PostMapping(value = "/service-api/systemcode/entity")
    void addEntity(@Validated @RequestBody SystemEntityAddDTO systemEntityAddDTO);

    /**
     * 修改系统编码字段数据
     * @param systemEntityUpdateDTO
     */
    @PutMapping(value = "/service-api/systemcode/entity")
    void updateEntity(@Validated @RequestBody SystemEntityUpdateDTO systemEntityUpdateDTO);

    /**
     * 删除指定系统字典数据
     * @param code
     */
    @DeleteMapping(value = "/service-api/systemcode/entity/{code}")
    void deleteEntityByCode(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("code") String code);

    /**
     * 批量删除系统字典数据
     * @param codes
     */
    @PostMapping(value = "/service-api/systemcode/entities/{codes}")
    void batchDeleteEntities(@NotBlank(message = "systemCode.CODE_PARAM_NECESSARY") @PathVariable("codes") String codes);

    /**
     * 通过模块删除指定系统字典数据
     * @param moduleId
     */
    @DeleteMapping(value = "/service-api/systemcode/entity/moduleId/{moduleId}")
    void deleteEntityByModuleId(@NotBlank(message = "systemCode.MODULE_ID_PARAM_NECESSARY") @PathVariable("moduleId") String moduleId);

    /**
     * 通过模块批量查询系统字典数据
     * @param moduleIds
     */
    @GetMapping(value = "/service-api/systemcode/entitys/moduleId/{moduleIds}")
    @ResponseBody
    ListResult<SystemEntityResultDTO> getEntityByModuleIds(@NotBlank(message = "systemCode.MODULE_ID_PARAM_NECESSARY") @PathVariable("moduleIds") List<String> moduleIds);

    /**
     * 查询系统基础字典数据
     *
     */
    @GetMapping(value = "/service-api/systemcode/entitys/systemBase")
    @ResponseBody
    ListResult<SystemEntityResultDTO> getSystemBaseEntity();

}
