package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.rbac.dao.MenuOperateCodeUrlRefMapper;
import com.supcon.supfusion.rbac.dao.field.MenuOperateCodeUrlRefField;
import com.supcon.supfusion.rbac.dao.po.MenuOperateCodeUrlRefPO;
import com.supcon.supfusion.rbac.service.IMenuOperateCodeUrlRefService;
import com.supcon.supfusion.rbac.service.IUserUrlRefService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单操作编码URL关联表 服务实现类
 * </p>
 *
 * @author 袁阳
 * @since 2020-06-22
 */
@Slf4j
@Service
@Transactional
public class MenuOperateCodeUrlRefServiceImpl extends ServiceImpl<MenuOperateCodeUrlRefMapper, MenuOperateCodeUrlRefPO> implements IMenuOperateCodeUrlRefService {

    @Autowired
    MenuOperateCodeUrlRefMapper menuOperateCodeUrlRefMapper;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Qualifier("rbacRedisTemplate")
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * @description: 删除模块下的所有URL
     * @param: appId
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void deleteByAppId(String appId) {
        menuOperateCodeUrlRefMapper.deleteByAppId(appId);
    }

    /**
     * @description: 更新模块下的所有URL
     * @param: app
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void updateUrl(String app) {
        //查找不需要匹配的URL
        List<MenuOperateCodeUrlRefPO> completeMatch = menuOperateCodeUrlRefMapper.selectList(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq(MenuOperateCodeUrlRefField.regMatch, false).eq(!ObjectUtils.isEmpty(app),MenuOperateCodeUrlRefField.app,app));
        //根据请求方式分组
        Map<Integer, List<MenuOperateCodeUrlRefPO>> groupByType = completeMatch.stream().filter(menuOperateCodeUrlRefPO -> !ObjectUtils.isEmpty(menuOperateCodeUrlRefPO.getMethodType())).collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getMethodType));
        Map<String,Map<String, Object>> methodTypeMap = new HashMap<>();
        methodTypeMap.put("completeMatchUrl_GET",new HashMap<>());
        methodTypeMap.put("completeMatchUrl_POST",new HashMap<>());
        methodTypeMap.put("completeMatchUrl_DELETE",new HashMap<>());
        methodTypeMap.put("completeMatchUrl_PUT",new HashMap<>());
        groupByType.forEach((key,value) -> {
            //根据服务名分组
            Map<String, List<MenuOperateCodeUrlRefPO>> groupByAppId = value.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getApp));
            groupByAppId.forEach((mapAppId,mocUrls) -> {
                Set<String> set = mocUrls.stream().map(MenuOperateCodeUrlRefPO::getUrl).collect(Collectors.toSet());
                //GET
                if (key == 0){
                    Map<String, Object> getMap = ObjectUtils.isEmpty(methodTypeMap.get("completeMatchUrl_GET")) ? new HashMap<>() : methodTypeMap.get("completeMatchUrl_GET");
                    getMap.put(mapAppId,set);
                    methodTypeMap.put("completeMatchUrl_GET",getMap);
                }
                //POST
                if (key == 1){
                    Map<String, Object> postMap = ObjectUtils.isEmpty(methodTypeMap.get("completeMatchUrl_POST")) ? new HashMap<>() : methodTypeMap.get("completeMatchUrl_POST");
                    postMap.put(mapAppId,set);
                    methodTypeMap.put("completeMatchUrl_POST",postMap);
                }
                //PUT
                if (key == 2){
                    Map<String, Object> putMap = ObjectUtils.isEmpty(methodTypeMap.get("completeMatchUrl_PUT")) ? new HashMap<>() : methodTypeMap.get("completeMatchUrl_PUT");
                    putMap.put(mapAppId,set);
                    methodTypeMap.put("completeMatchUrl_PUT",putMap);
                }
                //DELETE
                if (key == 3){
                    Map<String, Object> delMap = ObjectUtils.isEmpty(methodTypeMap.get("completeMatchUrl_DELETE")) ? new HashMap<>() : methodTypeMap.get("completeMatchUrl_DELETE");
                    delMap.put(mapAppId,set);
                    methodTypeMap.put("completeMatchUrl_DELETE",delMap);
                }
            });
        });

        redisAdd(app,"completeMatchUrl",methodTypeMap);

        //查找需要匹配的URL
        List<MenuOperateCodeUrlRefPO> regMatch = menuOperateCodeUrlRefMapper.selectList(new QueryWrapper<MenuOperateCodeUrlRefPO>().eq(MenuOperateCodeUrlRefField.regMatch, true).eq(!ObjectUtils.isEmpty(app),MenuOperateCodeUrlRefField.app,app));

        //根据请求方式分组
        Map<Integer, List<MenuOperateCodeUrlRefPO>> reg_groupByType = regMatch.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getMethodType));
        Map<String,Map<String, Object>> reg_methodTypeMap = new HashMap<>();
        reg_methodTypeMap.put("regMatchUrl_GET",new HashMap<>());
        reg_methodTypeMap.put("regMatchUrl_POST",new HashMap<>());
        reg_methodTypeMap.put("regMatchUrl_DELETE",new HashMap<>());
        reg_methodTypeMap.put("regMatchUrl_PUT",new HashMap<>());
        reg_groupByType.forEach((key,value) -> {
            //根据服务名分组
            Map<String, List<MenuOperateCodeUrlRefPO>> groupByAppId = value.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getApp));
            groupByAppId.forEach((mapAppId,mocUrls) -> {
                List<String> list = mocUrls.stream().map(MenuOperateCodeUrlRefPO::getUrl).collect(Collectors.toList());
                //GET
                if (key == 0){
                    Map<String, Object> getMap = ObjectUtils.isEmpty(reg_methodTypeMap.get("regMatchUrl_GET")) ? new HashMap<>() : reg_methodTypeMap.get("regMatchUrl_GET");
                    getMap.put(mapAppId,list);
                    reg_methodTypeMap.put("regMatchUrl_GET",getMap);
                }
                //POST
                if (key == 1){
                    Map<String, Object> postMap = ObjectUtils.isEmpty(reg_methodTypeMap.get("regMatchUrl_POST")) ? new HashMap<>() : reg_methodTypeMap.get("regMatchUrl_POST");
                    postMap.put(mapAppId,list);
                    reg_methodTypeMap.put("regMatchUrl_POST",postMap);
                }
                //PUT
                if (key == 2){
                    Map<String, Object> putMap = ObjectUtils.isEmpty(reg_methodTypeMap.get("regMatchUrl_PUT")) ? new HashMap<>() : reg_methodTypeMap.get("regMatchUrl_PUT");
                    putMap.put(mapAppId,list);
                    reg_methodTypeMap.put("regMatchUrl_PUT",putMap);
                }
                //DELETE
                if (key == 3){
                    Map<String, Object> delMap = ObjectUtils.isEmpty(reg_methodTypeMap.get("regMatchUrl_DELETE")) ? new HashMap<>() : reg_methodTypeMap.get("regMatchUrl_DELETE");
                    delMap.put(mapAppId,list);
                    reg_methodTypeMap.put("regMatchUrl_DELETE",delMap);
                }
            });
        });
        redisAdd(app,"regMatchUrl",reg_methodTypeMap);
    }

    /**
     * @description: 更新redis里数据
     * @param: appId
     * @param: prefix
     * @param: methodTypeMap
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    private void redisAdd(String appId,String prefix,Map<String,Map<String, Object>> methodTypeMap){
        //如果有appId,则清空对应app的redis数据,如果没有,则全清
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        if (!ObjectUtils.isEmpty(appId)){

            ops.put(prefix + "_GET",appId,methodTypeMap.get(prefix + "_GET").get(appId));
            ops.put(prefix + "_POST",appId,methodTypeMap.get(prefix + "_POST").get(appId));
            ops.put(prefix + "_PUT",appId,methodTypeMap.get(prefix + "_PUT").get(appId));
            ops.put(prefix + "_DELETE",appId,methodTypeMap.get(prefix + "_DELETE").get(appId));
        }else{

            if (!ObjectUtils.isEmpty(methodTypeMap.get(prefix + "_GET"))){
                methodTypeMap.get(prefix + "_GET").forEach((mapAppId,value) -> {
                    ops.put(prefix + "_GET",mapAppId,value);
                });
            }
            if (!ObjectUtils.isEmpty(methodTypeMap.get(prefix + "_POST"))){
                methodTypeMap.get(prefix + "_POST").forEach((mapAppId,value) -> {
                    ops.put(prefix + "_POST",mapAppId,value);
                });
            }
            if (!ObjectUtils.isEmpty(methodTypeMap.get(prefix + "_PUT"))){
                methodTypeMap.get(prefix + "_PUT").forEach((mapAppId,value) -> {
                    ops.put(prefix + "_PUT",mapAppId,value);
                });
            }
            if (!ObjectUtils.isEmpty(methodTypeMap.get(prefix + "_DELETE"))){
                methodTypeMap.get(prefix + "_DELETE").forEach((mapAppId,value) -> {
                    ops.put(prefix + "_DELETE",mapAppId,value);
                });
            }
        }
    }

    /**
     * @description: 删除指定的操作URL
     * @param: menuOperateCodeUrlRefPOS
     * @return: void
     * @author: 袁阳
     * @date: 2020/7/15
     */
    @Override
    public void removeRedisUrl(List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS) {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        Map<String, List<MenuOperateCodeUrlRefPO>> collect = menuOperateCodeUrlRefPOS.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getApp));
        collect.forEach((appName,list) -> {
            ArrayList<String> regMatchUrl_delete = (ArrayList<String>)ops.get("regMatchUrl_DELETE", appName);
            ArrayList<String> regMatchUrl_get = (ArrayList<String>) ops.get("regMatchUrl_GET", appName);
            ArrayList<String> regMatchUrl_put = (ArrayList<String>) ops.get("regMatchUrl_PUT", appName);
            ArrayList<String> regMatchUrl_post = (ArrayList<String>) ops.get("regMatchUrl_POST", appName);
            HashSet<String> completeMatchUrl_delete = (HashSet<String>) ops.get("completeMatchUrl_DELETE", appName);
            HashSet<String> completeMatchUrl_get = (HashSet<String>) ops.get("completeMatchUrl_GET", appName);
            HashSet<String> completeMatchUrl_put = (HashSet<String>) ops.get("completeMatchUrl_PUT", appName);
            HashSet<String> completeMatchUrl_post = (HashSet<String>) ops.get("completeMatchUrl_POST", appName);
            //删除对应数据
            list.forEach(menuOperateCodeUrlRefPO -> {
                switch (menuOperateCodeUrlRefPO.getMethodType()){
                    case 0:
                        if (!ObjectUtils.isEmpty(regMatchUrl_get) && menuOperateCodeUrlRefPO.getRegMatch()){
                            regMatchUrl_get.remove(menuOperateCodeUrlRefPO.getUrl());
                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_get)){
                            completeMatchUrl_get.remove(menuOperateCodeUrlRefPO.getUrl());
                        }
                        break;
                    case 1:
                        if (!ObjectUtils.isEmpty(regMatchUrl_post) && menuOperateCodeUrlRefPO.getRegMatch()){
                            regMatchUrl_post.remove(menuOperateCodeUrlRefPO.getUrl());
                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_post)){
                            completeMatchUrl_post.remove(menuOperateCodeUrlRefPO.getUrl());
                        }
                        break;
                    case 2:
                        if (!ObjectUtils.isEmpty(regMatchUrl_put) && menuOperateCodeUrlRefPO.getRegMatch()){
                            regMatchUrl_put.remove(menuOperateCodeUrlRefPO.getUrl());
                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_put)){
                            completeMatchUrl_put.remove(menuOperateCodeUrlRefPO.getUrl());
                        }
                        break;
                    case 3:
                        if (!ObjectUtils.isEmpty(regMatchUrl_delete) && menuOperateCodeUrlRefPO.getRegMatch()){
                            regMatchUrl_delete.remove(menuOperateCodeUrlRefPO.getUrl());
                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_delete)){
                            completeMatchUrl_delete.remove(menuOperateCodeUrlRefPO.getUrl());
                        }
                        break;
                }
            });

            //保存回去
            ops.put("regMatchUrl_DELETE", appName,regMatchUrl_delete);
            ops.put("regMatchUrl_GET", appName,regMatchUrl_get);
            ops.put("regMatchUrl_POST", appName,regMatchUrl_post);
            ops.put("regMatchUrl_PUT", appName,regMatchUrl_put);
            ops.put("completeMatchUrl_DELETE", appName,completeMatchUrl_delete);
            ops.put("completeMatchUrl_GET", appName,completeMatchUrl_get);
            ops.put("completeMatchUrl_POST", appName,completeMatchUrl_post);
            ops.put("completeMatchUrl_PUT", appName,completeMatchUrl_put);
        });
    }

    /**
     * @description: redis新增操作URL
     * @param: menuOperateCodeUrlRefPOS
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void addRedisUrl(List<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOS) {
        List<String> apps = menuOperateCodeUrlRefPOS.stream().map(MenuOperateCodeUrlRefPO::getApp).distinct().collect(Collectors.toList());
        userUrlRefService.refreshRedis(apps);
//        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
//        Map<String, List<MenuOperateCodeUrlRefPO>> collect = menuOperateCodeUrlRefPOS.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getApp));
//        collect.forEach((appName,list) -> {
//            ArrayList<String> regMatchUrl_delete = (ArrayList<String>)ops.get("regMatchUrl_DELETE", appName);
//            ArrayList<String> regMatchUrl_get = (ArrayList<String>) ops.get("regMatchUrl_GET", appName);
//            ArrayList<String> regMatchUrl_put = (ArrayList<String>) ops.get("regMatchUrl_PUT", appName);
//            ArrayList<String> regMatchUrl_post = (ArrayList<String>) ops.get("regMatchUrl_POST", appName);
//            HashSet<String> completeMatchUrl_delete = (HashSet<String>) ops.get("completeMatchUrl_DELETE", appName);
//            HashSet<String> completeMatchUrl_get = (HashSet<String>) ops.get("completeMatchUrl_GET", appName);
//            HashSet<String> completeMatchUrl_put = (HashSet<String>) ops.get("completeMatchUrl_PUT", appName);
//            HashSet<String> completeMatchUrl_post = (HashSet<String>) ops.get("completeMatchUrl_POST", appName);
//            //删除对应数据
//            list.forEach(menuOperateCodeUrlRefPO -> {
//                switch (menuOperateCodeUrlRefPO.getMethodType()){
//                    case 0:
//                        if (!ObjectUtils.isEmpty(regMatchUrl_get) && menuOperateCodeUrlRefPO.getRegMatch()){
//                            regMatchUrl_get.add(menuOperateCodeUrlRefPO.getUrl());
//                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_get)){
//                            completeMatchUrl_get.add(menuOperateCodeUrlRefPO.getUrl());
//                        }
//                        break;
//                    case 1:
//                        if (!ObjectUtils.isEmpty(regMatchUrl_post) && menuOperateCodeUrlRefPO.getRegMatch()){
//                            regMatchUrl_post.add(menuOperateCodeUrlRefPO.getUrl());
//                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_post)){
//                            completeMatchUrl_post.add(menuOperateCodeUrlRefPO.getUrl());
//                        }
//                        break;
//                    case 2:
//                        if (!ObjectUtils.isEmpty(regMatchUrl_put) && menuOperateCodeUrlRefPO.getRegMatch()){
//                            regMatchUrl_put.add(menuOperateCodeUrlRefPO.getUrl());
//                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_put)){
//                            completeMatchUrl_put.add(menuOperateCodeUrlRefPO.getUrl());
//                        }
//                        break;
//                    case 3:
//                        if (!ObjectUtils.isEmpty(regMatchUrl_delete) && menuOperateCodeUrlRefPO.getRegMatch()){
//                            regMatchUrl_delete.add(menuOperateCodeUrlRefPO.getUrl());
//                        }else if (!ObjectUtils.isEmpty(completeMatchUrl_delete)){
//                            completeMatchUrl_delete.add(menuOperateCodeUrlRefPO.getUrl());
//                        }
//                        break;
//                }
//            });
//
//            //保存回去
//            ops.put("regMatchUrl_DELETE", appName,regMatchUrl_delete);
//            ops.put("regMatchUrl_GET", appName,regMatchUrl_get);
//            ops.put("regMatchUrl_POST", appName,regMatchUrl_post);
//            ops.put("regMatchUrl_PUT", appName,regMatchUrl_put);
//            ops.put("completeMatchUrl_DELETE", appName,completeMatchUrl_delete);
//            ops.put("completeMatchUrl_GET", appName,completeMatchUrl_get);
//            ops.put("completeMatchUrl_POST", appName,completeMatchUrl_post);
//            ops.put("completeMatchUrl_PUT", appName,completeMatchUrl_put);
//        });
    }
}
