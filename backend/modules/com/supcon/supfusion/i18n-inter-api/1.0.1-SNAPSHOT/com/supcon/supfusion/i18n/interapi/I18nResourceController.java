package com.supcon.supfusion.i18n.interapi;

import java.util.*;

import com.supcon.supfusion.framework.boot.scaffold.dbp.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.xiaoymin.knife4j.annotations.DynamicParameter;
import com.github.xiaoymin.knife4j.annotations.DynamicParameters;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.dto.I18nQueryDTO;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.LocaleCustomUtil;
import com.supcon.supfusion.i18n.common.until.StringUtil;
import com.supcon.supfusion.i18n.dao.vo.I18nResourceVO;
import com.supcon.supfusion.i18n.service.I18nResourceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

/**
 * 国际化 键值对 inter-api 接口
 */
@Slf4j
@Api(tags = "inter-api 国际化键值对操作相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
public class I18nResourceController {

    @Autowired
    private I18nResourceService i18nResourceService;
    @Autowired
    private DataSourceConnectionProperties dataSourceConnectionProperties;


    /*
     * 进入新增  返回所有模块名
     */
    @ApiOperation(value = "获取所有模块信息")
    @GetMapping(value = "/resource/modules")
    public PageResult modules() {
        return i18nResourceService.getAllModel();
    }

    /*
     *选中某种类型是创建国际化key(根据模块名称创建国际化key值)  根据模块名称创建国际化key值
     */
    @ApiOperation(value = "获取指定模块的国际化key")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块code", required = true, paramType = "query"),
    })
    @GetMapping(value = {"/resource/initkey", "/resource/initkey/element"})
    public PageResult getkey(@RequestParam(value = "moduleCode") String moduleCode) {
        return i18nResourceService.createNewInternationalByValue("custom", moduleCode);
    }

    /*
     *保存信息
     * 逻辑删除旧有的
     * 新建一条
     */
    @ApiOperation(value = "保存国际化键值对")
    @DynamicParameters(name = "i18nMap", properties = {
            @DynamicParameter(name = "i18n_key", value = "国际化key", example = "workflow.base.calendar.set1898989", required = true),
            @DynamicParameter(name = "i18n_value", value = "国际化值", example = "{\"en_US\": \"calendar\",\"zh_HK\":\"日历\",\"zh_CN\":\"日历\"}", required = true),
            @DynamicParameter(name = "moduleCode", value = "所属模块code", example = "workflow", required = false),
    })
    @PostMapping(value = "/resource/code")
    public PageResult addition(@RequestBody @ApiParam(value = "国际化键值对map集合") Map i18nMap) {
        return i18nResourceService.addI18nResource(i18nMap);
    }

    /*
     * 修改信息
     * 保留了修改记录
     */
    @ApiOperation(value = "修改国际化键值对")
    @DynamicParameters(name = "i18nMap", properties = {
            @DynamicParameter(name = "i18n_key", value = "国际化key", example = "workflow.base.calendar.set1898989", required = true),
            @DynamicParameter(name = "i18n_value", value = "国际化值", example = "{\"en_US\": \"calendar\",\"zh_HK\":\"日历\",\"zh_CN\":\"日历\"}", required = true),
            @DynamicParameter(name = "moduleCode", value = "所属模块code", example = "workflow", required = true),
    })
    @PutMapping(value = {"/resource/code", "/resource/code/element"})
    public PageResult update(@RequestBody @ApiParam(value = "国际化键值对和模块code的map集合") Map i18nMap) {
        if (i18nMap.get(Constants.MODULE_CODE) == null) {
            throw new I18nException(I18nErrorEnum.FILE_NO_MODULE_ERROR);
        }
        if (i18nMap.get(Constants.I18N_KEY) == null) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST_I18N_KEY);
        }
        if (i18nMap.get(Constants.I18N_VALUE) == null) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST_I18N_VALUE);
        }
        return i18nResourceService.updateI18nResource(i18nMap);
    }

    /*
     *功能：删除某条记录（删除国际化数据列表）
     *@Deprecated 界面操作已经去掉删除按钮
     */
    @Deprecated
    @ApiOperation(value = "删除国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "i18n_key", value = "国际化key", required = true, paramType = "path"),
    })
    @DeleteMapping(value = "/resource/code/{i18n_key:.+}")
    public PageResult deletion(@PathVariable String i18n_key) {
        PageResult result = i18nResourceService.deleteI18nResourceByKey(i18n_key);
        return result;
    }

    /*
     * 模糊搜索/分页查询
     */
    @ApiOperation(value = "模糊分页搜索国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页参数和搜索条件参数集合", required = true, paramType = "body",
                    example = "{\"current\":1,\"pageSize\":20,\"i18n_key\":\"appcode,add\",\"i18n_values\":{\"zh_CN\":[\"支持\",\"获取\"],\"en_US\":[\"insert\"]}}"),})
    @PostMapping(value = "/resource/code/page")
    public PageResult<I18nResourceVO> query2(@RequestBody @ApiParam(value = "分页参数和搜索条件参数集合") Map<String, Object> page) {
    	I18nQueryDTO queryDto = new I18nQueryDTO();
    	if (page.get(Constants.I18N_KEY) != null && !page.get(Constants.I18N_KEY).equals(Constants.STR_NO_SPACE)) {
            String str = (String) page.get(Constants.I18N_KEY);
            queryDto.setI18nKeys(str.split(Constants.STR_POINT_DOU));
        }
        if (page.get(Constants.I18N_VALUES) != null) {
        	Map<String, List<String>> valuesMap = (Map<String, List<String>>) page.get(Constants.I18N_VALUES);
        	queryDto.setLanguageMap(valuesMap);
        }
        Integer pageSize = (Integer) page.get(Constants.PAGE_SIZE);
        Integer pageNo = (Integer) page.get(Constants.PAGE_NO);
        queryDto.setTenantId(RpcContext.getContext().getTenantId());
        
        pageSize = pageSize == null ? Constants.PAGE_SIZE_NUM_DEFA : pageSize;
        pageNo = pageNo == null ? Constants.ONE_INT : pageNo;
        return i18nResourceService.queryByMap(queryDto, new Pagination(0, pageSize, pageNo));
    }


    /*
     *  单独根据value/key 查询 符合要求的国际化 键值对 分页查询 模糊搜索
     */
    @ApiOperation(value = "根据单个value分页查询符合要求的国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页和条件参数", required = true, paramType = "query", dataType = "map",
                    example = "{'current:1,'pageSize':20,'i18n_key':'appCod','i18n_value':'Abc'}"),
    })
    @GetMapping(value = {"/resource/code/page/one", "/resource/code/page/one/element"})
    public PageResult query3(@RequestParam Map<String, Object> page) {
        if (page.get(Constants.PAGE_NO) == null) {
            page.put(Constants.PAGE_NO, Constants.ONE_INT);
        }
        if (page.get(Constants.PAGE_SIZE) == null) {
            page.put(Constants.PAGE_SIZE, Constants.PAGE_SIZE_NUM_DEFA);
        }
        String i18nKey = "";
        if (page.get(Constants.I18N_KEY) != null && !page.get(Constants.I18N_KEY).equals(Constants.STR_NO_SPACE)) {
            i18nKey = (String) page.get(Constants.I18N_KEY);
        }
        String i18nValue = "";
        if (page.get(Constants.I18N_VALUE) != null && !page.get(Constants.I18N_VALUE).toString().equals(Constants.STR_NO_SPACE)) {
            i18nValue = page.get(Constants.I18N_VALUE).toString();
        }
        int pageSize = Constants.ZERO_INT;
        int pageNo = Constants.ONE_INT;
        try {
            pageSize = Integer.parseInt((String) page.get(Constants.PAGE_SIZE));
            pageNo = Integer.parseInt((String) page.get(Constants.PAGE_NO));
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
        return i18nResourceService.queryByKeyOrValue(i18nKey, i18nValue, pageNo, pageSize);
    }

    /*
     *  单独根据key(moduleName_*)  精确查询
     */
    @ApiOperation(value = "根据单个key精确查询国际化")
    @GetMapping(value = {"/resource/get/by/key/element"})
    public Result getElementByKey(@RequestParam("key") String key) {
        if (key == null) {
            throw  new I18nException(I18nErrorEnum.PARAM_LOST_I18N_KEY);
        }
        I18nResourceVO i18nResourceVO = i18nResourceService.queryByI18nKey(key);
        return Result.data(i18nResourceVO);
    }
    /*
     *  前端输入查询条件时 下拉框模糊匹配已经存在的数据 key 对应语言的value
     *  后面的
     *  国际化资源列表,联想功能
     */
    @ApiOperation(value = "输入国际化key/value,模糊搜索系统中匹配的key/value 联想功能")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "国际化key/value和分页的参数集合", required = true, paramType = "query",
                    example = "{'current:1,'pageSize':100,'i18n_key':'','i18n_value':'Get','langu_code':'zh_CN'}"),
    })
    @GetMapping(value = "/resource/code/page/sugrec")
    public PageResult query5(@RequestParam Map<String, Object> page) {
    	int pageNo = Constants.ONE_INT;
    	int pageSize = 100;
    	if (page.get(Constants.PAGE_NO) != null) {
    		pageNo = Integer.parseInt(page.get(Constants.PAGE_NO).toString());
        }
        if (page.get(Constants.PAGE_SIZE) != null) {
        	pageSize = Integer.parseInt(page.get(Constants.PAGE_SIZE).toString());
        }
        //如果既没有key 又没有value
        if (!((page.get(Constants.I18N_KEY) != null && !page.get(Constants.I18N_KEY).equals(Constants.STR_NO_SPACE)) ||
                ((page.get(Constants.I18N_VALUE) != null && !page.get(Constants.I18N_VALUE).toString().equals(Constants.STR_NO_SPACE)) &&
                        (page.get(Constants.LANGU_CODE) != null && !page.get(Constants.LANGU_CODE).toString().equals(Constants.STR_NO_SPACE))))) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        String i18nKey = "";
        if (page.get(Constants.I18N_KEY) != null) {
        	i18nKey = page.get(Constants.I18N_KEY).toString();
        }
        String i18nValue = "";
        if (page.get(Constants.I18N_VALUE) != null) {
        	i18nValue = page.get(Constants.I18N_VALUE).toString();
        }
        String language = "";
        if (page.get(Constants.LANGU_CODE) != null) {
        	language = page.get(Constants.LANGU_CODE).toString();
        }
        judgeParam(page);
        return i18nResourceService.queryKeyOrValueByKeyOrValue(i18nKey, i18nValue, language, pageNo, pageSize);
    }

    private void judgeParam(Map<String, Object> page) {
        String i18n_key = null;
        String dbType = Optional.ofNullable(dataSourceConnectionProperties.getSystem())
                .map(SystemConnectionProperties::getDbType)
                .orElse(null);
        if (page.get(Constants.I18N_KEY) != null && !page.get(Constants.I18N_KEY).equals(Constants.STR_NO_SPACE)) {
            char[] i18nKeyChar = page.get(Constants.I18N_KEY).toString().toCharArray();

            i18n_key = StringUtil.getString(i18nKeyChar, i18n_key, dbType);
            page.put(Constants.I18N_KEY, i18n_key);
        } else {
            String i18n_value = Constants.STR_NO_SPACE;
            char[] i18nValueChar = page.get(Constants.I18N_VALUE).toString().toCharArray();
            i18n_value = StringUtil.getString(i18nValueChar, i18n_value, dbType);
            page.put(Constants.LANGU_CODE, page.get(Constants.LANGU_CODE).toString());
            page.put(Constants.I18N_VALUE, i18n_value);
        }
        int pageSize = Constants.ZERO_INT;
        int pageNo = Constants.ONE_INT;
        try {
            pageSize = Integer.parseInt((String) page.get(Constants.PAGE_SIZE));
            pageNo = Integer.parseInt((String) page.get(Constants.PAGE_NO));
        } catch (
                NumberFormatException e) {
            log.error(e.getMessage());
        }
        int offset = (pageNo - Constants.ONE_INT) * pageSize;
        page.put(Constants.OFFSET, offset);
        page.put(Constants.LIMIT, pageSize);
    }


    /*
     *  判断当前国际化key系统中是否已存在
     */
    @ApiOperation(value = "判断当前国际化key系统中是否已存在")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "i18n_key", value = "国际化key", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/code/page/key/exist")
    public List<I18nResourceVO> exist(@RequestParam String i18n_key) {
        String i18nKey;
        if (i18n_key != null && !i18n_key.equals(Constants.STR_NO_SPACE)) {
            i18nKey = i18n_key;
        } else {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        return i18nResourceService.queryKeyExist(i18nKey);
    }

    /*
     *  根据单个国际化key精确查询所有语言国际化value
     */
    @ApiOperation(value = "根据单个国际化key精确查询所有语言国际化value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "i18n_key", value = "国际化key", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/code/page/key/find")
    public List<I18nResourceVO> fineOneI18NKeyValues(@RequestParam("i18n_key") String i18nKey) {
        if (i18nKey == null || i18nKey.equals(Constants.STR_NO_SPACE)) {
        	throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        return i18nResourceService.getValueByKey(i18nKey);
    }

    /*
     *  输入指定的国际化key和语言类型返回对应的国际化value 如果不传语言返回默认语言类型的国际化value
     */
    @ApiOperation(value = "单个国际化key返回指定语言类型的国际化value，没有指定语言则返回默认语言类型的国际化value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "key", value = "国际化key", required = true, paramType = "query"),
            @ApiImplicitParam(name = "language", value = "语言类型", required = false, paramType = "query"),
    })
    @GetMapping(value = "/resource/code/page/key")
    public Result<String> searchOneI18NKeyValues(@RequestParam(value = "key", required = true) String key,
                                         @RequestParam(value = "language", required = false) String language) {
        if (key != null && !key.equals(Constants.STR_NO_SPACE)) {
            language =  LocaleCustomUtil.localeChange(language);
            return i18nResourceService.searchOneI18NKeyValues(key, language);
        } else {
            return null;
        }
    }

    /*
     *  输入多个国际化key和语言类型返回对应的多个国际化value 如果不传语言返回默认语言类型的国际化value
     */
    @ApiOperation(value = "多个国际化key返回指定语言类型的多个国际化value，没有指定语言则返回默认语言类型的国际化value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "国际化key数组,和语言类型", required = true, paramType = "query"),
    })
    @PostMapping(value = "/resource/code/page/keys")
    public Result searchI18NKeysValues(@RequestBody Map map) {
        List<String> keys = (List<String>) map.get("keys");
        String language = (String) map.get("language");
        language =  LocaleCustomUtil.localeChange(language);
        if (keys != null && keys.size() > 0) {
            return i18nResourceService.searchI18NKeysValues(keys, language);
        } else {
            return null;
        }
    }
    
    /*
     *  根据单个国际化key查询国际化value
     */
    @ApiOperation(value = "根据单个国际化key查询国际化value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "key", value = "国际化key", required = true, paramType = "query"),
    })
    @GetMapping(value = "/internationalConvert")
    public Map<String, Object> internationalConvert(@RequestParam String key) {
        if (key == null || key.equals(Constants.STR_NO_SPACE)) {
        	throw new I18nException(I18nErrorEnum.PARAM_LOST);
        } 
        String i18nValue = i18nResourceService.queryOneByOneKey(key);
        Map<String, Object> map = new HashMap<>(8);
    	map.put("success", true);
        map.put("data", i18nValue);
        map.put("msg", "操作成功");
        return map;
    }

    /*
     *根据多个国际化key查询国际化value
     */
    @ApiOperation(value = "根据多个国际化key查询国际化value")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keysArr", value = "国际化key数组", required = true, paramType = "query"),
    })
    @PostMapping(value = "/internationalConverts")
    public Map<String, Object> fineOneI18NKeyValues(@RequestBody String[] keysArr) {
        if (keysArr != null && keysArr.length>0) {
            Map<String, String> i18nMap = i18nResourceService.queryKeyValuesByKeys1(keysArr);
                    //queryKeyValuesByKeys(keysArr);
            Map<String, Object> map = new HashMap<>(8);
        	map.put("success", true);
            map.put("data", i18nMap);
            map.put("msg", "操作成功");
            return map;
        } else {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
    }

    /**
     * 临时将模块数据库中的值刷新到缓存并更新redis 中的Index;
     *
     */
    @ApiOperation(value = "临时将模块数据库中的值刷新到缓存并更新redis 中的Index")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "模块Code", required = true, paramType = "query"),
            @ApiImplicitParam(name = "language", value = "模块语言类型", required = true, paramType = "query"),
    })
    @GetMapping(value = "/flushDataToCacheByTemp")
    public Map<String, Object> flushDataToCacheByTemp(@RequestParam String moduleCode, @RequestParam String language) {
        if (moduleCode != null && language != null) {
            Map<String, String> i18nMap = i18nResourceService.putResourceToCache(moduleCode, language);
            Map<String, Object> map = new HashMap<>(8);
            map.put("success", true);
            map.put("data", i18nMap);
            map.put("msg", "操作成功");
            return map;
        } else {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
    }
}