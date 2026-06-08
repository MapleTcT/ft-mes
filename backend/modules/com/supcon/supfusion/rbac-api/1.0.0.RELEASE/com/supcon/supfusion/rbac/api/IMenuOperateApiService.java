package com.supcon.supfusion.rbac.api;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.rbac.api.Constants.Constants;
import com.supcon.supfusion.rbac.api.dto.MenuOperateDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateGroupRestrictDTO;
import com.supcon.supfusion.rbac.api.dto.MenuOperateUpdateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.supcon.supfusion.rbac.api.Constants.Constants.API_PREFIX;

/**
 *
 * 角色服务相关接口
 *
 * RPC内部接口
 * <ul>
 *     <li>FeignClient的值必须和spring.application.name的值一致</li>
 *     <li>内部接口统一梠式为：/service-api/{spring.application.name}/{version}/**</li>
 * </ul>
 *
 * @author
 * @date 20-5-11 下午2:14
 */
@Validated
@FeignClient(name = "rbac",contextId = "menuOperate")
public interface IMenuOperateApiService {

    /**
     * @description: 根据实体编码查询操作
     * @param: entityCode
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuOperateDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOOPERATE + "/findMenuOperateByEntityCode")
    @ResponseBody
    List<MenuOperateDTO> findMenuOperateByEntityCode(@RequestParam("entityCode") String entityCode,@RequestParam(value = "powerFlag",required = false) Integer powerFlag);

    /**
     * @description: 根据 操作编码和公司ID查询操作
     * @param: code
     * @param: cids
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuOperateDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOOPERATE + "/findMenuOperateByCodeAndCid")
    @ResponseBody
    List<MenuOperateDTO> findMenuOperateByCodeAndCid(@RequestParam("code") String code,@RequestParam("cids") List<Long> cids);

    /**
     * @param: id
     * @description: 根据ID删除操作
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOOPERATE + "/delete")
    @ResponseBody
    void delete(@RequestParam("id") Long id);

    /**
     * @description: 根据CODE删除操作
     * @param: code
     * @return: void
     * @author: 袁阳
     * @date: 2020/9/1
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOOPERATE + "/deleteByCode")
    @ResponseBody
    void deleteByCode(@RequestParam("code") String code);

    /**
     * @description: 根据CODE批量物理删除操作
     * @param: code
     * @return: void
     * @author: 袁阳
     * @date: 2020/9/1
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOOPERATE + "/deleteByCodePhysics")
    @ResponseBody
    void deleteByCodePhysics(@RequestParam("codes") List<String> codes);

    @PutMapping(API_PREFIX + Constants.MENUOOPERATE + "/changeOperateGroupRestrict")
    @ResponseBody
    void changeOperateGroupRestrict(@RequestBody MenuOperateUpdateDTO menuOperateUpdateDTO);

    /**
     * @description: 根据 菜单ID和操作编码查询操作
     * @param: code
     * @param: id
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuOperateDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOOPERATE + "/findMenuOperateByMenuInfo")
    @ResponseBody
    List<MenuOperateDTO> findMenuOperateByMenuInfo(@RequestParam(value = "code",required = false) String code,@RequestParam(value = "id",required = false) Long id);

    /**
     * @description: 查询对应实体编码非ID的操作
     * @param: entityCode
     * @param: id
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuOperateDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOOPERATE + "/findMenuOperateByEntityCodeAndNotId")
    @ResponseBody
    List<MenuOperateDTO> findMenuOperateByEntityCodeAndNotId(@RequestParam("entityCode") String entityCode,@RequestParam("id") Long id);

    /**
     * @description: 更新保存操作
     * @param: menuOperateDTOS
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PutMapping(API_PREFIX + Constants.MENUOOPERATE + "/save")
    @ResponseBody
    void save(@RequestBody List<MenuOperateDTO> menuOperateDTOS);

    /**
     * @description:  根据ID查询操作
     * @param: id
     * @return: com.supcon.supfusion.rbac.api.dto.MenuOperateDTO
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOOPERATE + "/findMenuOperateById")
    @ResponseBody
    MenuOperateDTO findMenuOperateById(@RequestParam("id") Long id);

    /**
     * @description: 根据菜单编码、公司ID、菜单ID查询操作
     * @param: code
     * @param: cids
     * @param: menuInfoId
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuOperateDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOOPERATE + "/findMenuOperateByMenuCodeAndCids")
    @ResponseBody
    List<MenuOperateDTO> findMenuOperateByMenuCodeAndCids(@RequestParam(value = "code",required = false) String code,@RequestParam(value = "cids",required = false) List<Long> cids,@RequestParam(value = "menuInfoId",required = false) Long menuInfoId);

    /**
     * @description: 根据菜单ID删除操作
     * @param: id
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOOPERATE + "/deleteByMenuInfoIds")
    @ResponseBody
    void deleteByMenuInfoIds(@RequestParam("ids") List<Long> id);

    /**
     * 根据实体编码和一些有关实体编码的条件更新操作组限制
     * @param menuOperateGroupRestrictDTO
     */
    @PutMapping(API_PREFIX + Constants.MENUOOPERATE + "/updateOperateGroupRestrictByEntityCodeAndOther")
    @ResponseBody
    void updateOperateGroupRestrictByEntityCodeAndOther(@RequestBody MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO);

    /**
     * 根据实体编码更新操作组限制
     * @param menuOperateGroupRestrictDTO
     */
    @PutMapping(API_PREFIX + Constants.MENUOOPERATE + "/updateOperateGroupRestrictByEntityCode")
    @ResponseBody
    void updateOperateGroupRestrictByEntityCode(@RequestBody MenuOperateGroupRestrictDTO menuOperateGroupRestrictDTO);
}
