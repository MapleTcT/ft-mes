/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD.
 * <p>
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.openapi.controller;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.base.services.MenuInfoService;
import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.base.services.ProcessService;
import com.supcon.supfusion.base.services.StaffService;
import com.supcon.supfusion.base.utils.RuntimeFlagHolder;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.openapi.framework.ConfigurationBaseController;
import com.supcon.supfusion.configuration.services.openapi.utils.DtoUtils;
import com.supcon.supfusion.configuration.services.openapi.vo.DeploymentVO;
import com.supcon.supfusion.configuration.services.openapi.vo.EntityVO;
import com.supcon.supfusion.configuration.services.openapi.vo.ResponseMsg;
import com.supcon.supfusion.configuration.services.openapi.wrapper.DeploymentWrapper;
import com.supcon.supfusion.configuration.services.openapi.wrapper.EntityWrapper;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ModuleService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.services.utils.DateUtil;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 实体管理Action类<br>
 *
 * @author songjiawei
 * @version 1.0
 */
@Slf4j
@Controller
//@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "${spring.application.name}" + HttpConstants.URL_SPLITER + "v1/ec/scheduler")
public class EntityController extends ConfigurationBaseController implements InitializingBean, DisposableBean {

    private static final EntityWrapper entityWrapper = new EntityWrapper();
    private static final DeploymentWrapper deploymentWrapper = new DeploymentWrapper();

    @Resource
    private EntityService entityService;
    //	@Resource
    //	private ProcessService processService;
    //	@Resource
    //	private TaskService taskService;
    @Resource
    private ViewService viewService;
    @Resource
    private ModuleService moduleService;
    @Autowired
    private MenuInfoService menuInfoService;
    @Autowired
    private MenuOperateService menuOperateService;
    @Resource
    private ModelService modelService;

    @Value("${bap.company.single:false}")
    private Boolean isSingleMode = false;

    @Resource
    private ProcessService processService;
    @Resource
    private StaffService staffService;
    //	@Autowired
    //	private ConsulService consulService;

    //	private boolean bapS2integration;
    private boolean recallAble = true;
    private long recallRemainTime = 3600;

    //	boolean exportFlag;

    @ResponseBody
    @RequestMapping("/ec/entity/list")
    public Page<EntityVO> list(Integer pageNo, Integer pageSize, boolean exportFlag) {
        Page<Entity> entities = new Page<Entity>(pageNo, pageSize);
        Module module = new Module();
        List<Entity> listentities = null;
        List<ModuleRelation> relations = null;
        String names = null;
        if (getRequest().getParameter("module.code") != null && !"".equals(getRequest().getParameter("module.code"))) {
            module.setCode(getRequest().getParameter("module.code"));
        }
        if (null == module) {
            entityService.findEntities(entities);
        } else {
            if (exportFlag) {
                listentities = entityService.findEntities(module);
                for (Entity entity : listentities) {
                    List<Model> modellist = modelService.findModels(entity);
                    Set<Model> setmodels = new HashSet<Model>();
                    for (Model model : modellist) {
                        List<Property> propertylist = modelService.findProperties(model);
                        Set<Property> setproperties = new HashSet<Property>();
                        for (Property property : propertylist) {
                            setproperties.add(property);
                        }
                        model.setProperties(setproperties);
                        setmodels.add(model);
                    }

                    entity.setModels(setmodels);
                    entity.setName(InternationalResource.get(entity.getName()));
                }
                module = moduleService.getModule(module.getCode());
                relations = moduleService.getRelations(module);
                for (ModuleRelation relation : relations) {
                    if (null == names) {
                        names = InternationalResource.get(relation.getTarget().getName());
                    } else {
                        names += "," + InternationalResource.get(relation.getTarget().getName());

                    }

                }
            } else {
                entityService.findEntities(entities, module);
            }
            if (exportFlag) {
                // TODO huning EXCEL导出
                //				request.setAttribute("fileName",InternationalResource.get(module.getName())+".xls");
                //				return "excel";
            }
        }
        return entityWrapper.e2vPage(entities);
    }

    @ResponseBody
    @RequestMapping(value = "/ec/entity/list-select")
    public List<EntityVO> listselect(HttpServletRequest request) {
        String moduleCode = request.getParameter("module.code");
        Module module = moduleService.getModule(moduleCode);
        List<Entity> entities = entityService.findEntities(module);
        List<Entity> listentities = new ArrayList<Entity>();
        for (Entity entity : entities) {
            if (!("sysbase_1.0_group".equals(entity.getCode()) || "sysbase_1.0_status".equals(entity.getCode()) || "sysbase_1.0_inherent".equals(entity.getCode()))) {
                listentities.add(entity);
            }
        }
        return entityWrapper.e2vList(listentities);
    }

    /**
     * 实体编辑页面 判断是否code为空，不是为编辑页面需要get实体返回，否则为添加页面
     *
     * @return
     */
    @RequestMapping(value = "/ec/entity/edit")
    public String edit(ModelMap map, @Nullable @RequestParam("moduleCode") String moduleCode, @Nullable @RequestParam("isView") boolean isView, @Nullable @RequestParam("entity.code") String entityCode) throws Exception {
        Map<String, Object> responseMap = new HashMap<String, Object>();

        Entity entity = null;
        if (!StringUtils.isEmpty(entityCode)) {
            entity = entityService.getEntity(entityCode);
        }
        responseMap.put("isRead", false);
        Module module = null;
        if (null != entity && entity.getModule() != null) {
            module = moduleService.getModule(entity.getModule().getCode());
        } else {
            module = moduleService.getModule(moduleCode);
        }
        responseMap.put("isProject", false);
        map.addAttribute("responseMap", responseMap);
        map.addAttribute("module", module);
        map.addAttribute("entity", entity);
        map.addAttribute("isView", isView);
        return "entity/edit";
    }

    /**
     * 实体保存结果页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/entity/save")
    public String save(HttpServletRequest request) throws Exception {
        Entity entity = DtoUtils.getEntity(request);

        entityService.dealPayCloseAttention(entity);
        entityService.saveEntity(entity);
        if (!entity.getMobile() && !entity.getCode().isEmpty()) {//如果关闭了移动平台的支持，要更新流程数据
            entityService.updateEntityProcessMobile(entity.getCode());
        }
        ResponseMsg response = new ResponseMsg(true);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(response);
    }

    /**
     * 实体保存删除页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/entity/delete")
    public ResponseMsg delete(Entity entity, HttpServletRequest request) throws Exception {
        return deleteChoise(true, entity, request);
    }

    /**
     * 实体逻辑删除页面
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/ec/entity/ordinaryDelete")
    public ResponseMsg ordinaryDelete(Entity entity, HttpServletRequest request) throws Exception {
        return deleteChoise(false, entity, request);

    }

    private ResponseMsg deleteChoise(boolean flag, Entity entity, HttpServletRequest request) throws JsonProcessingException {
        String code = request.getParameter("entity.code");
        if (null != code) {
            entity.setCode(code);
        }

        ResponseMsg response = new ResponseMsg(true);
        if (null == entity) {
            response.setExceptionMsg("ec.entity.nofind");
        } else {
            if (flag) {
                //add by yubo20171221
                String responseMsg = entityService.deleteEntityPhysical(entity.getCode(), false);
                if (responseMsg != null && responseMsg.length() != 0 && (!("".equals(responseMsg)))) {
                    response.setSuccess(false);
                    response.setExceptionMsg(responseMsg);
                }
            } else {
                String responseMsg = entityService.deleteEntity(entity);
                if (responseMsg != null && responseMsg.length() != 0 && (!("".equals(responseMsg)))) {
                    response.setSuccess(false);
                    response.setExceptionMsg(responseMsg);
                }
            }
        }
        //		ObjectMapper mapper = new ObjectMapper();
        //		return mapper.writeValueAsString(response);
        return response;
    }

    /**
     * ??
     *
     * @return
     */
    @RequestMapping(value = "/ec/entity/config")
    public String config(ModelMap map, @RequestParam("entity.code") String entityCode) throws Exception {
            Entity entity = entityService.getEntity(entityCode);
            map.addAttribute("entity", entity);
        return "entity/config";
    }

    /**
     * @return
     */
    @RequestMapping(value = "/ec/entity/publish")
    public String publish() throws Exception {
        return "entity/publish";
    }

    @RequestMapping(value = "/ec/entity/publishMenuFrame")
    public String publishMenuFrame(ModelMap map, String entityCode) throws Exception {
        List<View> views = null;
        Module module = null;
        Set<MenuInfo> publishMenus = null;
        Entity entity = entityService.getEntity(entityCode);
        MenuInfo menusTree = null;
        if (null != entity) {
            //			if(entity.getIsBase()){	//如果是基础类型，菜单发布时，可以选取到无框架增强型视图
            views = viewService.findViews(entity, 1, 3, 0, ViewType.LIST, ViewType.EXTRA);    //经沟通，表单类型也，无框架增强型视图也可以发布菜单
            //			}else{
            //				views = viewService.findViews(entity, 1, 3, 0, ViewType.LIST);
            //			}
            // views = viewService.getViews(entity.getCode());
            module = entity.getModule();
            menusTree = menuInfoService.getMenusTree();
            List<MenuInfo> list = new ArrayList<MenuInfo>();
            if (null != module) {
                list = menuInfoService.getMenuInfoByModul(module.getCode());

            }
            List<String> codes = new ArrayList<String>();
            for (View view : views) {
                codes.add(view.getCode());
            }
            publishMenus = new HashSet<>();
            for (MenuInfo menu : list) {

                if (codes.contains(menu.getCode())) {
                    List<MenuOperate> operateList = menuInfoService.getOperateList(menu);
                    Set<MenuOperate> opList = new HashSet<MenuOperate>();
                    for (MenuOperate im : operateList) {
                        opList.add(im);
                    }
                    menu.setMenuOperates(opList);

                    publishMenus.add(menu);
                }
            }

        }
        map.addAttribute("entity", entity);
        map.addAttribute("views", views);
        map.addAttribute("module", module);
        map.addAttribute("menusTree", menusTree);
        map.addAttribute("publishMenus", publishMenus);
        map.addAttribute("entityCode", entityCode);
        return "entity/publishMenuFrame";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/entity/public-menu")
    public ResponseMsg publishMenu(String viewCode, Long parentMenuId, String menuName) throws Exception {
        View view = viewService.getView(viewCode);
        moduleService.publishMenu(view, parentMenuId, menuName);
        ResponseMsg response = new ResponseMsg(true);
        return response;
    }

    @RequestMapping(value = "/ec/entity/wf")
    public String workflow(ModelMap map, String entityCode) throws Exception {
        String entityMobile = null;
        String moduleCode = null;
        String artifact = null;
        List<MenuInfo> menuList = null;
        List<View> views = null;
        if (null != entityCode) {
            Entity entity = entityService.getEntity(entityCode);
            if (null != entity && null != entity.getMobile()) {
                entityMobile = entity.getMobile().toString();
            } else {
                entityMobile = Boolean.FALSE.toString();
            }
            List<MenuInfo> list = menuInfoService.getEntityMenus2(entityCode);
            moduleCode = entity.getModule().getCode();
            artifact = entity.getModule().getArtifact();
            menuList = new ArrayList<MenuInfo>();
            Set<String> urls = new HashSet<String>();
            if (null != entity) {
                urls = listExtraViewUrl(entity);
            }
            for (MenuInfo m : list) {
                MenuInfo me = (MenuInfo) m;
                if (null != me.getUrl() && !"".equals(me.getUrl()) && urls.contains(me.getUrl())) {
                    continue;
                }
                String internationOpName = InternationalResource.get(me.getName());
                if (null != internationOpName && !"".equals(internationOpName)) {
                    me.setName(internationOpName);
                }
                menuList.add(me);
            }
            RuntimeFlagHolder.getInstance().getRuntimeFlag().set(true);
            views = viewService.findViews(entity, ViewType.VIEW);
            RuntimeFlagHolder.getInstance().getRuntimeFlag().set(false);
            map.addAttribute("entity", entity);
        }
        map.addAttribute("entityCode", entityCode);
        map.addAttribute("entityMobile", entityMobile);
        map.addAttribute("moduleCode", moduleCode);
        map.addAttribute("artifact", artifact);
        map.addAttribute("menuList", menuList);
        map.addAttribute("views", views);
        map.addAttribute("recallAble", "true");
        map.addAttribute("recallRemainTime", recallRemainTime);
        return "entity/wf";
    }

    @ResponseBody
    @RequestMapping(value = "/ec/entity/wf-list")
    public Page<DeploymentVO> workflowData(HttpServletRequest request) throws Exception {
        String entityCode = request.getParameter("entity.code");
        Entity entity = entityService.getEntity(entityCode);

        String showHistoryVersionStr = request.getParameter("showHistoryVersion");
        Integer pageSize = 20;
        if (!StringUtils.isEmpty(request.getParameter("deployments.pageSize"))) {
            pageSize = Integer.parseInt(request.getParameter("deployments.pageSize"));
            if (pageSize > 500) {
                pageSize = 500;
            }
        }
        Integer pageNo = 1;
        if (!StringUtils.isEmpty(request.getParameter("deployments.pageNo"))) {
            pageNo = Integer.parseInt(request.getParameter("deployments.pageNo"));
        }

        Page<Deployment> page = new Page<>(pageNo, pageSize);
        if (!StringUtils.isEmpty(showHistoryVersionStr)) {
            boolean showHistoryVersion = Boolean.parseBoolean(showHistoryVersionStr);
            if (showHistoryVersion) {
                Page<Deployment> deployments = processService.findDeployments(page, entity.getCode());
                setStaffInfo(deployments);
                return deploymentWrapper.e2vPage(deployments);
            } else {
                Page<Deployment> deployments = processService.findCurrentDeployments(page, entity.getCode());
                setStaffInfo(deployments);
                return deploymentWrapper.e2vPage(deployments);
            }
        }

        return null;
    }

    private void setStaffInfo(Page<Deployment> deployments) {
        List<Deployment> list = deployments.getResult();
        for (Deployment d : list) {
            Long staffId = d.getCreateStaffId();
            Long mStaffId = d.getModifyStaffId();
            if (null != staffId) {
                StaffVO createStaffVO = new StaffVO();
                Staff createStaff = staffService.load(staffId);
                if (createStaff != null) {
                    createStaffVO.setCode(createStaff.getCode());
                    createStaffVO.setCreateTime(createStaff.getCreateTime());
                    createStaffVO.setId(createStaff.getId());
                    createStaffVO.setName(createStaff.getName());
                    createStaffVO.setVersion(createStaff.getVersion());
                    d.setCreateStaff(createStaffVO);
                }
            }
            if (null != mStaffId) {
                StaffVO modifyStaffVO = new StaffVO();
                Staff modifyStaff = staffService.load(mStaffId);
                if (modifyStaff != null) {
                    modifyStaffVO.setCode(modifyStaff.getCode());
                    modifyStaffVO.setModifyTime(modifyStaff.getModifyTime());
                    modifyStaffVO.setId(modifyStaff.getId());
                    modifyStaffVO.setName(modifyStaff.getName());
                    modifyStaffVO.setVersion(modifyStaff.getVersion());

                    StaffVO createStaffVO = new StaffVO();
                    createStaffVO.setModifyStaff(modifyStaffVO);
                    d.setModifyStaff(modifyStaffVO);
                }
            }

        }
    }

    /**
     * 查询所有的无框架增强型视图的url
     *
     * @param entity
     * @return
     */
    private Set<String> listExtraViewUrl(Entity entity) {
        Set<String> urls = new HashSet<String>();
        List<View> views = viewService.findViews(entity, ViewType.EXTRA);
        for (View view : views) {
            if (null != view.getUrl() && !"".equals(view.getUrl())) {
                urls.add(view.getUrl());
            }
        }
        urls.addAll(viewService.findEngineViewUrl(entity, ViewType.EXTRA));
        return urls;
    }

    /* @Action(value = "/ec/entity/transform-menucode") */
    public String transformMenuCodeAndOperateCode() {/*
		try {
			List<MenuInfo> allMenus = menuInfoService.getAllLeafMenuInfo();
			Collection<IMenuOperate> operates = null;
			View view = null;
			String code = null;
			String newMenuCode = null;
			String opCode = null;
			String viewCode = null;
			for (MenuInfo menu : allMenus) {
				code = menu.getCode();
				if (code != null && code.length() > 0 && code.indexOf("_") > 0) {
					try {
						viewCode = code.substring(code.lastIndexOf("_") + 1).toString();
						view = viewService.getView(viewCode);
						newMenuCode = view.getCode();
						menu.setCode(newMenuCode);
						operates = menu.getOperates();
						for (IMenuOperate op : operates) {
							MenuOperate op2 = (MenuOperate) op;
							opCode = op2.getCode();
							if (opCode != null && opCode.length() > 0 && opCode.indexOf("_") > 0) {
								if (opCode.equals(code + "_self")*//* && op.getPowerFlag() != null && op.getPowerFlag() *//*) {
									op2.setCode(newMenuCode + "_self");
									menuOperateService.save(op2);
								}
							}
						}
						menuInfoService.save(menu);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			response.setContentType("text/html");
			response.setCharacterEncoding("UTF-8");
			PrintWriter writer = response.getWriter();
			writer.write("sucess");
		} catch (IOException e) {
			throw new RuntimeException("error");
		}
		return null;*/
        return null;
    }

    @RequestMapping(value = "/ec/entity/getViewList")
    public ResponseEntity getViewList(String entityCode) {
        Entity entity = entityService.getEntity(entityCode);
        List<View> views = viewService.findViewList(entity);
        String jsonString = "";
        for (View view : views) {
            String displayName = InternationalResource.get(view.getDisplayName());
            jsonString += view.getCode() + "," + displayName + "," + view.getType() + "," + view.getUrl() + ";";
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(new ByteArrayInputStream(jsonString.getBytes()));
    }

    //	@ResponseBody
    //	@RequestMapping(value="/ec/entity/findCurrentDeployment")
    //	public String findCurrentDeployment(){
    //		//String entityCode = request.getParameter("entityCode");
    //		ResponseMsg response = new ResponseMsg(true);
    //		String processKey = getRequest().getParameter("processKey");
    //		if(processKey!=null && processKey.length()>0){
    //			Deployment deploy = taskService.getCurrentDeployment(processKey);
    //			//List<Deployment> deploys = processService.findCurrentDeployments(entityCode);
    //			if(deploy!=null){
    //				json.put("dealSuccessFlag", true);
    //				json.put("deployId", deploy.getId());
    //				String powerCode = processService.getStartPowerCode(deploy.getId());
    //				json.put("powerCode", powerCode);
    //			}else{
    //				json.put("dealSuccessFlag", false);
    //				json.put("message", "not unique");
    //			}
    //		}else{
    //			json.put("dealSuccessFlag", false);
    //			json.put("message", "entityCode error");
    //		}
    //		return json.toString();
    //	}

    private static final String HTML_HEAD = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />" + "<meta http-equiv=\"pragma\" content=\"no-cache\" /><meta http-equiv=\"cache-control\" content=\"no-cache\" />" + "<meta http-equiv=\"expires\" content=\"-1\" /><title>console</title><style>*{font-size:12px;}</style></head><body><div style=\"height:365px;overflow-y:auto;\">";

    @ResponseBody
    @RequestMapping(value = "/ec/entity/migrate")
    public String migrate(String entityCodes, Module module, Module targetModule) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

        getResponse().setContentType("text/html");
        //        getResponse().setCharacterEncoding("UTF-8");
        PrintWriter out = getResponse().getWriter();
        out.write(HTML_HEAD);
        List<Entity> entities = new ArrayList<Entity>();
        String[] entity = entityCodes.split(",");
        if (entity != null && entity.length > 0) {
            for (String code : entity) {
                Entity item = entityService.getEntity(code);
                if (item != null) {
                    if (module == null) {
                        module = item.getModule();
                    }
                    entities.add(item);
                }
            }
        }
        String deploySuccessMsg = "<script type=\"text/javascript\">try{parent.datatable_ec_module_entity_datatable.setRequestDataUrl('/ec/entity/list?module.code=" + module.getCode() + "');}catch(e){}</script>";
        if (targetModule != null && targetModule.getCode() != null) {
            targetModule = moduleService.getModule(targetModule.getCode(), true);
        }
        entityService.migrateEntity(out, entities, targetModule);
        out.write("[" + DateUtil.getTime(new Date(), dateFormat) + "]<span style=\"color:blue;font-weight:bold;\">" + " 实体复制成功" + "</span>");
        out.write(deploySuccessMsg);
        out.flush();
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //		bapS2integration=consulService.getValueAsBoolean(OrchidConstants.BAP_S2_INTEGRATION);
        //		recallAble=consulService.getValueAsBoolean(OrchidConstants.BAP_RECALL_ABLE_DEFAULT);
        //		recallRemainTime= Long.valueOf(consulService.getValueAsString(OrchidConstants.BAP_RECALL_REMAIN_TIME));
    }

    @Override
    public void destroy() throws Exception {
        // TODO Auto-generated method stub

    }

}