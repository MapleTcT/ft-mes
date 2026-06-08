package com.supcon.supfusion.rbac.service.config;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.rbac.api.dto.MenuInfoJsonDTO;
import com.supcon.supfusion.rbac.common.Contants.Constants;
import com.supcon.supfusion.rbac.common.utils.JSONHelper;
import com.supcon.supfusion.rbac.dao.po.InitVersionInfoPO;
import com.supcon.supfusion.rbac.dao.po.MenuInfoPO;
import com.supcon.supfusion.rbac.dao.po.MenuOperatePO;
import com.supcon.supfusion.rbac.dao.po.RolePermissionPO;
import com.supcon.supfusion.rbac.service.IInitVersionInfoService;
import com.supcon.supfusion.rbac.service.IMenuInfoService;
import com.supcon.supfusion.rbac.service.IMenuOperateCodeUrlRefService;
import com.supcon.supfusion.rbac.service.IMenuOperateService;
import com.supcon.supfusion.rbac.service.IRolePermissionService;
import com.supcon.supfusion.rbac.service.IUserUrlRefService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @description: 启动初始化数据
 * @author: 袁阳
 * @date: 2020/7/13
 */
@Component
@Slf4j
@Setter
@Getter
@Order(value = 1)
public class LaunchInitialize  {
    private static final long serialVersionUID = -5209456650726974315L;

    @Value("${supfusion.environment:normal}")
    private String environment;
    @Value("${rbac.initsupos:true}")
    private Boolean isSupos;
    @Autowired
    private IInitVersionInfoService iInitVersionInfoService;
    @Autowired
    private IMenuInfoService menuInfoService;
    @Autowired
    private IRolePermissionService rolePermissionService;
    @Autowired
    private IMenuOperateService menuOperateService;
    @Autowired
    private IUserUrlRefService userUrlRefService;
    @Autowired
    private IMenuOperateCodeUrlRefService menuOperateCodeUrlRefService;
    @Autowired
    @Qualifier("rbacRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    private static final Long ROLEID = 1L;

    private static final Long COMPANY_ROLE_ID = 2L;

    private static final Long NORMAL_ROLE_ID = 3L;

//    @Override
//    public void onApplicationEvent(ApplicationReadyEvent event) {
//        if (dataSourceConnectionProperties.getUseSystem()) {
//            // 如果使用系统库时，初始化数据
//            TenantInfo systemTenant = TenantUtils.getTenantInfo(SystemConstant.SYSTEM_TENANT_ID);
//            RpcContext.getContext().setTenantId(systemTenant.getId());
//            run(null);
//        }
//    }
    @Transactional(rollbackFor = Exception.class)
    public void run(String tenantId) {
        log.info("beginning to initialize menus from json file");

        JSONHelper jsonHelper = new JSONHelper();
        try {
            //先将这两个菜单的valid都置为1，再做统一处理
            menuInfoService.recoverMenuInfo("menuManageConfigure");
            menuInfoService.recoverMenuInfo("menumanage");

            String json = jsonHelper.ResolveJsonFileToString(isSupos ? "initdata_supos.json" : "initdata_bap.json");           
            initPermission(json);
            //判断启动参数,组态期菜单配置菜单是否显示
            MenuInfoPO menuManageConfigure = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", "menuManageConfigure"));
            MenuInfoPO menuManage = menuInfoService.getOne(new QueryWrapper<MenuInfoPO>().eq("CODE", "menumanage"));
            switch (environment){
                case Constants.ENVIRONMENT_ALL:
                    menuManageConfigure.setIsHide(true);
                    menuInfoService.update(menuManageConfigure,new QueryWrapper<MenuInfoPO>().eq("CODE", "menuManageConfigure"));
                    break;
                case Constants.ENVIRONMENT_CONFIGURE:
                    menuInfoService.removeById(menuManage.getId());
                    break;
                case Constants.ENVIRONMENT_NORMAL:
                    if(null != menuManageConfigure){
                        menuInfoService.removeById(menuManageConfigure.getId());
                    }
                    break;
                default: {

                }
            }

            log.info("ending to initialize menus from json file");
        } catch (Exception e) {
            log.error("fail to initialize menus from json file", e);
        }
    }

    @Transactional
    public void initPermission(String json) {
    	InitVersionInfoPO initVersionInfoPO = iInitVersionInfoService.getOne(new QueryWrapper<InitVersionInfoPO>().eq("ID", 1));
        log.info("获取数据库菜单初始化文件版本========================================{}",initVersionInfoPO.getInitVersion());
    	//判断版本是否高于当前初始化版本
        InitJSON initJSON = JSON.parseObject(json, InitJSON.class);
        if (ObjectUtils.isEmpty(initVersionInfoPO) || initVersionInfoPO.getInitVersion() < initJSON.getVersion()){
            if (ObjectUtils.isEmpty(initVersionInfoPO)){
                initVersionInfoPO = new InitVersionInfoPO();
                initVersionInfoPO.setId(1L);
            }
            initVersionInfoPO.setInitVersion(initJSON.getVersion());
            List<MenuInfoJsonDTO> menuInfoJsonDTOS = JSON.parseArray(initJSON.getInitBaseData(), MenuInfoJsonDTO.class);
            log.info("initVersionInfoPO ==============================================>{}",initVersionInfoPO);
            iInitVersionInfoService.saveOrUpdate(initVersionInfoPO);
            log.info("initmenuInfoJsonDTOS ===========================================>{}",menuInfoJsonDTOS);
            menuInfoService.saveBachUrl(menuInfoJsonDTOS,false);
            
            //管理员新增权限
            //查询没有权限的操作
            List<MenuOperatePO> menuOperateWithoutUserPermission = menuOperateService.getMenuOperateWithoutRolePermission(initJSON.getInitBasePermissionCodes(), ROLEID);
            if(null!=menuOperateWithoutUserPermission &&menuOperateWithoutUserPermission.size()>0) {
	            log.info("menuOperateWithoutUserPermission=========={}",JSON.toJSONString(menuOperateWithoutUserPermission));
	            List<RolePermissionPO> rolePermissionPOS = menuOperateWithoutUserPermission.stream().map(menuOperatePO -> {
	                RolePermissionPO rolePermissionPO = new RolePermissionPO();
	                rolePermissionPO.setCid(menuOperatePO.getCid());
	                rolePermissionPO.setNoRestrictFlag(true);
	                rolePermissionPO.setMenuOperateId(menuOperatePO.getId());
	                rolePermissionPO.setRoleId(ROLEID);
	                return rolePermissionPO;
	            }).collect(Collectors.toList());
	            //新增权限
	            log.info("rolePermissionService.saveOrUpdateBatch start================================>");
	            rolePermissionService.saveOrUpdateBatch(rolePermissionPOS);
	            rolePermissionService.freshSubOperate(ROLEID, null, null,null);
            }
            //公司管理员新增权限
            //查询没有权限的操作
            List<MenuOperatePO> menuOperateWithoutUserPermissionByAdmin = menuOperateService.getMenuOperateWithoutRolePermission(initJSON.getInitCompanyPermissionCodes(), COMPANY_ROLE_ID);
            if(null!=menuOperateWithoutUserPermissionByAdmin && menuOperateWithoutUserPermissionByAdmin.size()>0) {
            	log.info(JSON.toJSONString(menuOperateWithoutUserPermissionByAdmin));
	            List<RolePermissionPO> rolePermissionPOList = menuOperateWithoutUserPermissionByAdmin.stream().map(menuOperatePO -> {
	                RolePermissionPO rolePermissionPO = new RolePermissionPO();
	                rolePermissionPO.setCid(menuOperatePO.getCid());
	                rolePermissionPO.setNoRestrictFlag(true);
	                rolePermissionPO.setMenuOperateId(menuOperatePO.getId());
	                rolePermissionPO.setRoleId(COMPANY_ROLE_ID);
	                return rolePermissionPO;
	            }).collect(Collectors.toList());
	            //新增权限
	            rolePermissionService.saveOrUpdateBatch(rolePermissionPOList);
        	}
            //普通用户新增权限
            //查询没有权限的操作
            List<MenuOperatePO> menuOperateWithoutUserPermissionByNormal = menuOperateService.getMenuOperateWithoutRolePermission(initJSON.getInitNormalPermissionCodes(), NORMAL_ROLE_ID);
            if(null!=menuOperateWithoutUserPermissionByNormal &&menuOperateWithoutUserPermissionByNormal.size()>0) {
	            log.info(JSON.toJSONString(menuOperateWithoutUserPermissionByNormal));
	            List<RolePermissionPO> rolePermissionList = menuOperateWithoutUserPermissionByNormal.stream().map(menuOperatePO -> {
	                RolePermissionPO rolePermissionPO = new RolePermissionPO();
	                rolePermissionPO.setCid(menuOperatePO.getCid());
	                rolePermissionPO.setNoRestrictFlag(true);
	                rolePermissionPO.setMenuOperateId(menuOperatePO.getId());
	                rolePermissionPO.setRoleId(NORMAL_ROLE_ID);
	                return rolePermissionPO;
	            }).collect(Collectors.toList());
	            //新增权限
	            rolePermissionService.saveOrUpdateBatch(rolePermissionList);
            }
        }
        //菜单、操作、URL初始化后 将所有需要匹配的URL加入redis
        menuOperateCodeUrlRefService.updateUrl(null);
    }
    static class InitJSON{

        private Integer version;

        private String initBaseData;

        private List<String> initBasePermissionCodes;

        private List<String> initCompanyPermissionCodes;

        private List<String> initNormalPermissionCodes;

        public List<String> getInitBasePermissionCodes() {
            return initBasePermissionCodes;
        }

        public void setInitBasePermissionCodes(List<String> initBasePermissionCodes) {
            this.initBasePermissionCodes = initBasePermissionCodes;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public String getInitBaseData() {
            return initBaseData;
        }

        public void setInitBaseData(String initBaseData) {
            this.initBaseData = initBaseData;
        }

        public List<String> getInitCompanyPermissionCodes() {
            return initCompanyPermissionCodes;
        }

        public void setInitCompanyPermissionCodes(List<String> initCompanyPermissionCodes) {
            this.initCompanyPermissionCodes = initCompanyPermissionCodes;
        }

        public List<String> getInitNormalPermissionCodes() {
            return initNormalPermissionCodes;
        }

        public void setInitNormalPermissionCodes(List<String> initNormalPermissionCodes) {
            this.initNormalPermissionCodes = initNormalPermissionCodes;
        }
    }
}
