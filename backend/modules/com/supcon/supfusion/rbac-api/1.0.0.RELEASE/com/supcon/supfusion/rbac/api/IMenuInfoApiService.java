package com.supcon.supfusion.rbac.api;

import com.supcon.supfusion.framework.cloud.annotation.ServiceApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.rbac.api.Constants.Constants;
import com.supcon.supfusion.rbac.api.dto.MenuInfoDTO;
import com.supcon.supfusion.rbac.api.dto.MenuInfoJsonDTO;
import com.supcon.supfusion.rbac.api.dto.UpdateMenuInfoIdDTO;
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
@FeignClient(name = "rbac",contextId = "menuInfo")
public interface IMenuInfoApiService {

    /**
     * @description: 根据json创建更新菜单、操作、URL
     * @param: json
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PutMapping(API_PREFIX + Constants.MENUOINFO + "/saveByJson")
    @ResponseBody
    void saveBachUrlByJson(@RequestBody String json);


    /**
     * @description: 根据json创建更新菜单、操作、URL
     * @param: json
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     * @param menuInfoJsonDTO
     */
    @PutMapping(API_PREFIX + Constants.MENUOINFO + "/saveBachUrl")
    @ResponseBody
    String saveBachUrl(@RequestBody List<MenuInfoJsonDTO> menuInfoJsonDTO);

    /**
     * @description: 根据code查询菜单
     * @param: code
     * @return: com.supcon.supfusion.rbac.api.dto.MenuInfoDTO
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOINFO + "/getMenuInfoByCode")
    @ResponseBody
    MenuInfoDTO getMenuInfoByCode(@RequestParam("code") String code);

    /**
     * @description: 根据ID查询菜单
     * @param: id
     * @return: com.supcon.supfusion.rbac.api.dto.MenuInfoDTO
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOINFO + "/getMenuInfoById")
    @ResponseBody
    MenuInfoDTO getMenuInfoById(@RequestParam("id") Long id);

    /**
     * @description: 保存修改菜单
     * @param: menuInfoDTO
     * @return: com.supcon.supfusion.rbac.api.dto.MenuInfoDTO
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PutMapping(API_PREFIX + Constants.MENUOINFO + "/save")
    @ResponseBody
    MenuInfoDTO save(@RequestBody MenuInfoDTO menuInfoDTO);

    /**
     * @description: 查询全部菜单根据sort正序
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuInfoDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOINFO + "/getMenuInfoAsc")
    @ResponseBody
    List<MenuInfoDTO> getMenuInfoAsc();

    /**
     * @description: 根据模块编码查询菜单
     * @param: moduleCode
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuInfoDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOINFO + "/getMenuInfoByModuleCode")
    @ResponseBody
    List<MenuInfoDTO> getMenuInfoByModuleCode(@RequestParam("moduleCode") String moduleCode);

    /**
     * @description: 根据实体编码查询菜单
     * @param: entityCode
     * @return: java.util.List<com.supcon.supfusion.rbac.api.dto.MenuInfoDTO>
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @GetMapping(API_PREFIX + Constants.MENUOINFO + "/getMenuInfoByEntityCode")
    @ResponseBody
    List<MenuInfoDTO> getMenuInfoByEntityCode(@RequestParam("entityCode") String entityCode);

    /**
     * @description: 根据ID删除菜单 批量
     * @param: ids
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOINFO + "/deleteMenuInfoByIds")
    @ResponseBody
    void deleteMenuInfoByIds(@RequestParam("ids") List<Long> ids);

    /**
     * @description: 修改所有对应实体编码 的parentID为null
     * @param: entityCode
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @PutMapping(API_PREFIX + Constants.MENUOINFO + "/updateMenuInfoByEntityCode")
    @ResponseBody
    void updateMenuInfoByEntityCode(@RequestBody String entityCode);

    /**
     * @description: 删除所有对应实体编码的菜单
     * @param: entityCode
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/28
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOINFO + "/deleteMenuInfoByEntityCode")
    @ResponseBody
    void deleteMenuInfoByEntityCode(@RequestParam("entityCode") String entityCode);

    /**
     * 公司被删除 删除菜单公司关联 删除公司拥有的菜单
     * @return
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOINFO + "/deleteCompanyRef")
    @ResponseBody
    Result<Boolean> deleteCompanyRef(@RequestParam("cid") Long cid);

    /**
     * 根据apps 删除菜单
     * @param apps
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOINFO + "/apps/{apps}")
    @ResponseBody
    void deleteMenuInfoByApps(@PathVariable("apps") String apps);

    /**
     * 物理删菜单
     * @param ids
     */
    @DeleteMapping(API_PREFIX + Constants.MENUOINFO + "/deletePhysics")
    @ResponseBody
    void deletePhysics(@RequestParam("ids") List<Long> ids);

    /**
     * 根据ID
     * @param updateMenuInfoIdDTO
     */
    @PutMapping(API_PREFIX + Constants.MENUOINFO + "/updateMenuInfoById")
    @ResponseBody
    void updateMenuInfoById(@RequestBody UpdateMenuInfoIdDTO updateMenuInfoIdDTO);

    @GetMapping(API_PREFIX + Constants.MENUOINFO + "/rollBack")
    @ResponseBody
    void rollBack(@RequestParam("uuid") String uuid);

    @DeleteMapping(API_PREFIX + Constants.MENUOINFO + "/removeTemp")
    @ResponseBody
    void removeTemp(@RequestParam("uuid") String uuid);

    @GetMapping(API_PREFIX + Constants.MENUOINFO + "/findPermissionMenu")
    @ResponseBody
    List<MenuInfoDTO> findPermissionMenu(@RequestParam("userId") Long userId);
}
