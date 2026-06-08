package com.supcon.supfusion.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.rbac.api.enums.MethodType;
import com.supcon.supfusion.rbac.common.exception.MenuErrorEnum;
import com.supcon.supfusion.rbac.common.exception.MenuException;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import com.supcon.supfusion.rbac.dao.MenuoperateMapper;
import com.supcon.supfusion.rbac.dao.UserUrlRefMapper;
import com.supcon.supfusion.rbac.dao.enums.FlowPermissionType;
import com.supcon.supfusion.rbac.dao.field.*;
import com.supcon.supfusion.rbac.dao.po.*;
import com.supcon.supfusion.rbac.manager.IOrganizationAdapter;
import com.supcon.supfusion.rbac.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户与请求URL关联表 服务实现类
 * </p>
 */
@Slf4j
@Service
@Transactional
public class UserUrlRefServiceImpl extends ServiceImpl<UserUrlRefMapper, UserUrlRefPO> implements IUserUrlRefService {

    @Autowired
    UserUrlRefMapper userUrlRefMapper;
    @Autowired
    private IFlowPermissionService flowPermissionService;
    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;
    @Autowired
    private IUserPermissionService userPermissionService;
    @Autowired
    private IRoleUserService roleUserService;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IMenuInfoCompanyRefService menuInfoCompanyRefService;
    @Autowired
    private IOrganizationAdapter organizationAdapter;
    @Qualifier("rbacRedisTemplate")
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * @description: 刷新redis中权限数据
     * @param: cid
     * @return: void
     * @author: 袁阳
     * @date: 2020/8/31
     */
    @Override
    public void addUserUrlRefList(List<Long> cid) {
        //获取租户信息
        String tenantId = RpcContext.getContext().getTenantId();
        if (StringUtils.isEmpty(tenantId)) {
            throw new MenuException(MenuErrorEnum.CAN_NOT_FOUND_TENANT);
        }
        // 删除用户与请求URL关联表中当前userId所有的关联关系
        //modify by yy 2020/7/2 删除redis中的人员权限数据
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        QueryWrapper<UserUrlRefPO> wrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(cid)){
            wrapper.in("UP.CID", cid);
        }
        // 通过用户权限表和菜单操作与请求URL关联表查询userId和请求URL并保存到用户与请求URL关联表中
        List<UserUrlRefPO> userUrlRefPOList = userUrlRefMapper.getUserUrlRefList(wrapper);
        //根据公司ID分组
        Map<Long, List<UserUrlRefPO>> collect = userUrlRefPOList.stream().collect(Collectors.groupingBy(UserUrlRefPO::getCid));
        collect.forEach((key1,value1) -> {
            //根据userId进行group by
            Map<Long, List<UserUrlRefPO>> collect1 = value1.stream().collect(Collectors.groupingBy(UserUrlRefPO::getUserId));
            //redis存关联数据 userUrl -> Map<userId,List<String> urls>
            collect1.forEach((key,value) -> {
                List<UserUrlRefPO> pos = value.stream().filter(userUrlRefPO -> !ObjectUtils.isEmpty(userUrlRefPO.getUrl())).collect(Collectors.toList());
                Map<String,List<String>> getMap = new HashMap<>();
                Map<String,List<String>> postMap = new HashMap<>();
                Map<String,List<String>> putMap = new HashMap<>();
                Map<String,List<String>> delMap = new HashMap<>();
                pos.forEach(po -> {
                    //0 GET请求
                    if (po.getMethodType() == 0){
                        List<String> list = getMap.get(po.getApp());
                        if (ObjectUtils.isEmpty(list)){
                            list = new ArrayList<>();
                            getMap.put(po.getApp(),list);
                        }
                        list.add(po.getUrl());

                    }else if(po.getMethodType() == 1){
                        //1 POST请求
                        List<String> list = postMap.get(po.getApp());
                        if (ObjectUtils.isEmpty(list)){
                            list = new ArrayList<>();
                            postMap.put(po.getApp(),list);
                        }
                        list.add(po.getUrl());
                    }else if(po.getMethodType() == 2){
                        //0 PUT请求
                        List<String> list = putMap.get(po.getApp());
                        if (ObjectUtils.isEmpty(list)){
                            list = new ArrayList<>();
                            putMap.put(po.getApp(),list);
                        }
                        list.add(po.getUrl());
                    }else if(po.getMethodType() == 3){
                        //0 DELETE请求
                        List<String> list = delMap.get(po.getApp());
                        if (ObjectUtils.isEmpty(list)){
                            list = new ArrayList<>();
                            delMap.put(po.getApp(),list);
                        }
                        list.add(po.getUrl());
                    }
                });
                ops.put(tenantId +"_getUrl_"+key1,key.toString(),getMap);
                ops.put(tenantId +"_postUrl_"+key1,key.toString(),postMap);
                ops.put(tenantId +"_putUrl_"+key1,key.toString(),putMap);
                ops.put(tenantId +"_deleteUrl_"+key1,key.toString(),delMap);
            });
        });
    }

    @Override
    public void addUserUrlRefList(List<Long> cid, String tenantId) {
        RpcContext.getContext().setTenantId(tenantId);
        addUserUrlRefList(cid);
    }

    //单独线程，需要修改
    @Override
    public Map<String,Map<String,List<String>>> addUserUrlRefListForUserFlow(List<Long> userIds,Long company,String tenantId) {
        RpcContext.getContext().setTenantId(tenantId);
        if (ObjectUtils.isEmpty(userIds)){
            return new HashMap<>();
        }
        if (ObjectUtils.isEmpty(company)){
            company = UserContext.getUserContext().getCompanyId();
        }
        Long cid = company;
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        //工作流权限
        List<FlowPermissionPO> permissionPOS = new ArrayList<>();
        List<MenuOperateCodeUrlRefPO> operateCodeUrlRefPOS =new ArrayList<MenuOperateCodeUrlRefPO>();
        Map<Long,Set<String>> operate_codes_user = new HashMap<>();
        List<FlowPermissionPO> flow_permissions = flowPermissionService.list(new QueryWrapper<FlowPermissionPO>().eq(FlowPermissionField.flowPermissionType, FlowPermissionType.USER).in(FlowPermissionField.typeId, userIds));
        if (!ObjectUtils.isEmpty(flow_permissions)){
            permissionPOS.addAll(flow_permissions);
        }
        //角色工作流权限
        List<RoleUserPO> roleUserPOS = roleUserService.list(new QueryWrapper<RoleUserPO>().in(RoleUserField.userId, userIds));
        if (!ObjectUtils.isEmpty(roleUserPOS)){
            Map<Long, List<RoleUserPO>> role_users = roleUserPOS.stream().collect(Collectors.groupingBy(RoleUserPO::getRoleId));
            List<FlowPermissionPO> role_permissionPOS = flowPermissionService.list(new QueryWrapper<FlowPermissionPO>().eq(FlowPermissionField.flowPermissionType, FlowPermissionType.ROLE).in(FlowPermissionField.typeId, role_users.keySet()));
            //由于是根据用户刷，所以要把typeId换成对应用户的ID
            role_permissionPOS.forEach(flowPermissionPO -> {
                List<RoleUserPO> userPOS = role_users.get(flowPermissionPO.getTypeId());
                userPOS.forEach(roleUserPO -> {
                    FlowPermissionPO flowPermissionPO1 = new FlowPermissionPO();
                    BeanUtils.copyProperties(flowPermissionPO,flowPermissionPO1);
                    flowPermissionPO1.setTypeId(roleUserPO.getUserId());
                    permissionPOS.add(flowPermissionPO1);
                });
            });
        }
        //工作流权限操作收集
        Map<Long, List<FlowPermissionPO>> user_flow_permissions = permissionPOS.stream().collect(Collectors.groupingBy(FlowPermissionPO::getTypeId));
        user_flow_permissions.forEach((userId,permissions) ->{
            operate_codes_user.put(userId,permissions.stream().map(FlowPermissionPO::getActivityCode).collect(Collectors.toSet()));
        });
        Set<String> operate_codes_all = permissionPOS.stream().map(FlowPermissionPO::getActivityCode).collect(Collectors.toSet());
        //普通权限
        List<UserPermissionPO> userPermissions = userPermissionService.list(new QueryWrapper<UserPermissionPO>().in(UserPermissionField.userId, userIds).eq(UserPermissionField.cid, cid));
        if (!ObjectUtils.isEmpty(userPermissions)){
            //普通权限操作收集
            Map<Long, List<UserPermissionPO>> user_permissions = userPermissions.stream().collect(Collectors.groupingBy(UserPermissionPO::getUserId));
            user_permissions.forEach((userId,permissions) ->{
                Set<String> codes = operate_codes_user.get(userId);
                if (ObjectUtils.isEmpty(codes)){
                    codes = new HashSet<>();
                    operate_codes_user.put(userId,codes);
                }
                codes.addAll(permissions.stream().map(UserPermissionPO::getMenuOperateCode).collect(Collectors.toSet()));
            });
            operate_codes_all.addAll(userPermissions.stream().map(UserPermissionPO::getMenuOperateCode).collect(Collectors.toList()));
        }
        //没有操作，所以清空redis
        if (ObjectUtils.isEmpty(operate_codes_user)){
            for (Long userId : userIds) {
                ops.set(tenantId + "_getUrl_"+ cid + "_" + userId,new HashMap<>());
                ops.set(tenantId + "postUrl_"+cid + "_" + userId,new HashMap<>());
                ops.set(tenantId + "putUrl_"+cid + "_" + userId,new HashMap<>());
                ops.set(tenantId + "deleteUrl_"+cid + "_" + userId,new HashMap<>());
            }
            return new HashMap<>();
        }
        operate_codes_all = operate_codes_all.stream().filter(Objects::nonNull).collect(Collectors.toSet());      
        ArrayList<String> operate_all_list = new ArrayList<>(operate_codes_all);
        //每1000条参数执行一次
        int batch = operate_all_list.size() / 1000;
        if (batch == 0) {
        	QueryWrapper<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOQueryWrapper = new QueryWrapper<>();
            menuOperateCodeUrlRefPOQueryWrapper.in(MenuOperateCodeUrlRefField.menuoperateCode, operate_all_list);
            operateCodeUrlRefPOS.addAll(menuOperateCodeUrlRefService.list(menuOperateCodeUrlRefPOQueryWrapper));
        } else {
            for (int i = 0; i < batch; i++) {
            	QueryWrapper<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOQueryWrapper = new QueryWrapper<>();
                menuOperateCodeUrlRefPOQueryWrapper.in(MenuOperateCodeUrlRefField.menuoperateCode, operate_all_list.subList(i * 1000, i * 1000 + 1000));
                operateCodeUrlRefPOS.addAll(menuOperateCodeUrlRefService.list(menuOperateCodeUrlRefPOQueryWrapper));               
            }
            	QueryWrapper<MenuOperateCodeUrlRefPO> menuOperateCodeUrlRefPOQueryWrappers = new QueryWrapper<>();            	
            	operateCodeUrlRefPOS.addAll(menuOperateCodeUrlRefService.list(menuOperateCodeUrlRefPOQueryWrappers.in(MenuOperateCodeUrlRefField.menuoperateCode, operate_all_list.subList(batch * 1000, operate_all_list.size()))));                
        }
//        operateCodeUrlRefPOS = menuOperateCodeUrlRefService.list(menuOperateCodeUrlRefPOQueryWrapper);
        //构造map
        Map<String, List<MenuOperateCodeUrlRefPO>> operate_map = operateCodeUrlRefPOS.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getMenuoperateCode));
        //用于返回的Map
        Map<String,Map<String,List<String>>> result = new HashMap<>();
        //根据用户分类
        operate_codes_user.forEach((userId,operate_codes) ->{
            Map<String,List<String>> get_urs_map =new HashMap<>() ;
            Map<String,List<String>> post_urs_map = new HashMap<>();
            Map<String,List<String>> put_urs_map = new HashMap<>();
            Map<String,List<String>> delete_urs_map = new HashMap<>();
            operate_codes.forEach(code -> {
                List<MenuOperateCodeUrlRefPO> operate_urls = operate_map.get(code);
                if (ObjectUtils.isEmpty(operate_urls)){
                    return;
                }
                //根据请求方式分类
                Map<Integer, List<MenuOperateCodeUrlRefPO>> type_urls = operate_urls.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getMethodType));
                type_urls.forEach((type,urls)->{
                    //根据app分类
                    Map<String, List<MenuOperateCodeUrlRefPO>> app_urls = urls.stream().collect(Collectors.groupingBy(MenuOperateCodeUrlRefPO::getApp));
                    app_urls.forEach((app,urls_app) ->{
                        List<String> urls_string = urls_app.stream().map(MenuOperateCodeUrlRefPO::getUrl).collect(Collectors.toList());
                        if (ObjectUtils.isEmpty(urls_string)){
                            return;
                        }
                        if (type == 0){
                            List<String> strings = get_urs_map.get(app);
                            if (ObjectUtils.isEmpty(strings)){
                                strings = new ArrayList<>(urls_string);
                            }else{
                                strings.addAll(urls_string);
                            }
                            get_urs_map.put(app,strings);
                        }
                        if (type == 1){
                            List<String> strings = post_urs_map.get(app);
                            if (ObjectUtils.isEmpty(strings)){
                                strings = new ArrayList<>(urls_string);
                            }else{
                                strings.addAll(urls_string);
                            }
                            post_urs_map.put(app,strings);
                        }
                        if (type == 2){
                            List<String> strings = put_urs_map.get(app);
                            if (ObjectUtils.isEmpty(strings)){
                                strings = new ArrayList<>(urls_string);
                            }else{
                                strings.addAll(urls_string);
                            }
                            put_urs_map.put(app,strings);
                        }
                        if (type == 3){
                            List<String> strings = delete_urs_map.get(app);
                            if (ObjectUtils.isEmpty(strings)){
                                strings = new ArrayList<>(urls_string);
                            }else{
                                strings.addAll(urls_string);
                            }
                            delete_urs_map.put(app,strings);
                        }
                    });
                });
            });
            ops.set(tenantId + "_getUrl_"+ cid + "_" + userId,get_urs_map);
            ops.set(tenantId + "_postUrl_"+cid + "_" + userId,post_urs_map);
            ops.set(tenantId + "_putUrl_"+cid + "_" + userId,put_urs_map);
            ops.set(tenantId + "_deleteUrl_"+cid + "_" + userId,delete_urs_map);
            redisTemplate.expire(tenantId + "_getUrl_"+ cid + "_" + userId,30, TimeUnit.MINUTES);
            redisTemplate.expire(tenantId + "_postUrl_"+ cid + "_" + userId,30, TimeUnit.MINUTES);
            redisTemplate.expire(tenantId + "_putUrl_"+ cid + "_" + userId,30, TimeUnit.MINUTES);
            redisTemplate.expire(tenantId + "_deleteUrl_"+ cid + "_" + userId,30, TimeUnit.MINUTES);
            result.put(userId + MethodType.GET.toString(), get_urs_map);
            result.put(userId + MethodType.POST.toString(), post_urs_map);
            result.put(userId + MethodType.PUT.toString(), put_urs_map);
            result.put(userId + MethodType.DELETE.toString(), delete_urs_map);
        });
        return result;
    }

    @Override
    public void refreshRedis(List<String> apps) {
        List<MenuInfoPO> menuInfoPOS = menuInfoService.list(new QueryWrapper<MenuInfoPO>().in(MenuInfoField.app, apps));
        if (ObjectUtils.isEmpty(menuInfoPOS)) {
            return;
        }
        List<Long> menuInfoIds = menuInfoPOS.stream().map(MenuInfoPO::getId).distinct().collect(Collectors.toList());
        //获取这个app下菜单所关联的所有公司
        // sql 超长处理
        if (!CollectionUtils.isEmpty(menuInfoIds)) {
            QueryWrapper<MenuInfoCompanyRefPO> menuCompanyRefWrapper = new QueryWrapper<>();
            int batch = menuInfoIds.size() / 1000;
            if (0 == batch) {
                menuCompanyRefWrapper.in(MenuInfoCompanyRefField.menuinfoId, menuInfoIds);
            } else {
                for (int i = 0; i < batch; i++) {
                    menuCompanyRefWrapper.or().in(MenuInfoCompanyRefField.menuinfoId, menuInfoIds.subList(i * 1000, i * 1000 + 1000));
                }
                if (menuInfoIds.size() % 1000 != 0) {
                    menuCompanyRefWrapper.or().in(MenuInfoCompanyRefField.menuinfoId, menuInfoIds.subList(batch * 1000, menuInfoIds.size()));
                }
            }
            List<MenuInfoCompanyRefPO> menuInfo_company = menuInfoCompanyRefService.list(menuCompanyRefWrapper);
            List<Long> cids = menuInfo_company.stream().map(MenuInfoCompanyRefPO::getCompanyId).distinct().collect(Collectors.toList());
            if (!ObjectUtils.isEmpty(cids) && cids.contains(-1L)) {
                cids = organizationAdapter.queryAllCompanies();
            }

            // sql 超长处理
            List<Long> menuIds = menuInfoPOS.stream().map(MenuInfoPO::getId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(menuIds)) {
                QueryWrapper<MenuOperatePO> operatePOQueryWrapper = new QueryWrapper<>();
                int idsBatch = menuIds.size() / 1000;
                if (0 == idsBatch) {
                    operatePOQueryWrapper.in(MenuOperateField.menuinfoId, menuIds);
                } else {
                    for (int i = 0; i < idsBatch; i++) {
                        operatePOQueryWrapper.or().in(MenuOperateField.menuinfoId, menuIds.subList(i * 1000, i * 1000 + 1000));
                    }
                    if (menuIds.size() % 1000 != 0) {
                        operatePOQueryWrapper.or().in(MenuOperateField.menuinfoId, menuIds.subList(idsBatch * 1000, menuIds.size()));
                    }
                }
                //查询菜单下的所有操作
                List<MenuOperatePO> operatePOS = menuOperateService.list(operatePOQueryWrapper);
                List<String> operateCodes = operatePOS.stream().map(MenuOperatePO::getCode).collect(Collectors.toList());
                if (ObjectUtils.isEmpty(operatePOS)) {
                    return;
                }
                log.info("开始更新需要权限验证的URL");
                apps.forEach(app -> {
                    menuOperateCodeUrlRefService.updateUrl(app);
                });

                // sql 超长处理
                if (!CollectionUtils.isEmpty(operateCodes)) {
                    QueryWrapper<UserPermissionPO> userPermissionPOQueryWrapper = new QueryWrapper<>();
                    QueryWrapper<FlowPermissionPO> flowUserPermissionPOQueryWrapper = new QueryWrapper<>();
                    QueryWrapper<FlowPermissionPO> flowRolePermissionPOQueryWrapper = new QueryWrapper<>();
                    int operateBatch = operateCodes.size() / 1000;
                    if (0 == operateBatch) {
                        userPermissionPOQueryWrapper.in(UserPermissionField.menuOperateCode, operateCodes);
                        flowUserPermissionPOQueryWrapper.in(FlowPermissionField.activityCode, operateCodes);
                        flowRolePermissionPOQueryWrapper.in(FlowPermissionField.activityCode, operateCodes);
                    } else {
                        for (int i = 0; i < operateBatch; i++) {
                            userPermissionPOQueryWrapper.or().in(UserPermissionField.menuOperateCode, operateCodes.subList(i * 1000, i * 1000 + 1000));
                            flowUserPermissionPOQueryWrapper.or().in(FlowPermissionField.activityCode, operateCodes.subList(i * 1000, i * 1000 + 1000));
                            flowRolePermissionPOQueryWrapper.or().in(FlowPermissionField.activityCode, operateCodes.subList(i * 1000, i * 1000 + 1000));
                        }
                        if (operateCodes.size() % 1000 != 0) {
                            userPermissionPOQueryWrapper.or().in(UserPermissionField.menuOperateCode, operateCodes.subList(batch * 1000, operateCodes.size()));
                            flowUserPermissionPOQueryWrapper.or().in(FlowPermissionField.activityCode, operateCodes.subList(batch * 1000, operateCodes.size()));
                            flowRolePermissionPOQueryWrapper.or().in(FlowPermissionField.activityCode, operateCodes.subList(batch * 1000, operateCodes.size()));
                        }
                    }

                log.info("开始更新用户权限URL");
                List<Long> userIds = new ArrayList<>();
                //更新userpermission
                List<UserPermissionPO> userPermissionPOS = userPermissionService.list(userPermissionPOQueryWrapper);
                if (!ObjectUtils.isEmpty(userPermissionPOS)) {
                    Map<Long, List<UserPermissionPO>> cid_permissions = userPermissionPOS.stream().collect(Collectors.groupingBy(UserPermissionPO::getCid));
                    List<Long> userpermission_user_ids = userPermissionPOS.stream().map(UserPermissionPO::getUserId).collect(Collectors.toList());
                    userIds.addAll(userpermission_user_ids);
                }
                //查找flow_permission表中有该操作的用户
                List<FlowPermissionPO> flowPermissionPOS_user = flowPermissionService.list(flowUserPermissionPOQueryWrapper.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.USER));
                if (!ObjectUtils.isEmpty(flowPermissionPOS_user)) {
                    List<Long> flowpermission_user_ids = flowPermissionPOS_user.stream().map(FlowPermissionPO::getTypeId).collect(Collectors.toList());
                    userIds.addAll(flowpermission_user_ids);
                }
                //查找flow_permission表中有该操作的角色，然后查角色下的用户
                List<FlowPermissionPO> flowPermissionPOS_role = flowPermissionService.list(flowRolePermissionPOQueryWrapper.eq(FlowPermissionField.flowPermissionType, FlowPermissionType.ROLE));
                if (!ObjectUtils.isEmpty(flowPermissionPOS_role)) {
                    List<Long> flowpermission_role_ids = flowPermissionPOS_role.stream().map(FlowPermissionPO::getTypeId).collect(Collectors.toList());
                    List<RoleUserPO> roleUserPOS = roleUserService.list(new QueryWrapper<RoleUserPO>().in(RoleUserField.roleId, flowpermission_role_ids));
                    List<Long> roleuser_user_ids = roleUserPOS.stream().map(RoleUserPO::getUserId).collect(Collectors.toList());
                    if (!ObjectUtils.isEmpty(roleuser_user_ids)) {
                        userIds.addAll(roleuser_user_ids);
                    }
                }
                if (ObjectUtils.isEmpty(userIds)) {
                    return;
                }
                log.info("开始刷用户URL{}", userIds.stream().map(Object::toString).collect(Collectors.joining(",")));
                cids.forEach(cid -> this.addUserUrlRefListForUserFlow(userIds.stream().distinct().collect(Collectors.toList()), cid, RpcContext.getContext().getTenantId()));
            }
            }
        }
    }
}
