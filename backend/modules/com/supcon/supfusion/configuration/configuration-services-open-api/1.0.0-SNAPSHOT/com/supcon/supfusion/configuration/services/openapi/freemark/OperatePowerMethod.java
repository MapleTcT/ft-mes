package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.base.entities.*;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.base.enums.MenuOperateType;
import com.supcon.supfusion.configuration.services.security.OrchidAuthenticationToken;
import com.supcon.supfusion.configuration.services.utils.OrchidUtils;
import com.supcon.supfusion.base.services.*;
import com.supcon.supfusion.configuration.services.service.EntityService;
import com.supcon.supfusion.framework.cloud.common.context.UserContext;
import flexjson.JSONSerializer;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.template.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class OperatePowerMethod implements TemplateMethodModelEx {

	@Autowired
	private EntityService entityService;
	@Autowired
	private MenuOperateService menuOperateService;
	@Autowired
	private MenuInfoService menuInfoService;
	@Autowired
	private DataPermissionService dataPermissionService;
	private UserPermissionService userPermissionService = null;
//	@Autowired
	private ButtonOperaterPowerService buttonOperaterPowerService;

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		Set<Map<String, String>> btns = new LinkedHashSet<Map<String, String>>();
		String buttonCode="";
		String buttonType="";
		String mobileParam="";
		String operateType = "";
		long start = System.currentTimeMillis();
		String subButtons = null;
		if (arguments.size() == 7||arguments.size() == 8) {
			if (arguments.get(0) == null) {
				return null;
			}
			buttonCode=arguments.get(5) == null ?"":arguments.get(5).toString();
			buttonType=arguments.get(6) == null ?"":arguments.get(6).toString();
			if(arguments.size() == 8){
				mobileParam=arguments.get(7) == null?"":arguments.get(7).toString();
			}
			String operateStr = arguments.get(0).toString();
			String menuCode = "";
			if (arguments.size() > 1 && null != arguments.get(1)) {
				menuCode = arguments.get(1).toString();

			}
			Map<String, String> map = new LinkedHashMap<String, String>();
			Map<String, Map<String, String>> infoMap = new LinkedHashMap<String, Map<String, String>>();
			List<Map<String, String>> operateList = new LinkedList<Map<String, String>>();
			String[] operateArr = operateStr.split(";");
			List<String> codes = new LinkedList<String>();
			if (!"".equals(operateStr)) {
				for (String operateInfo : operateArr) {
					String[] operate = operateInfo.split("\\|\\|");
					Map<String, String> map1 = new HashMap<String, String>();
					for (String info : operate) {
						String key = null;
						String value = null;

						if (info.startsWith("subButtons")) {
							key = "subButtons";
							value = info.substring(info.indexOf(":") + 1);
							subButtons = info;
						} else {
							String[] str = info.split(":");
							key = str[0].trim();
							value = str[1].trim();
						}
						map1.put(key, value);
						if (key.equals("code")) {
							codes.add(value);
						}
						operateList.add(map1);
					}
					if (map1.get("onclick") != null) {
						String c = map1.get("code").toString();
						String o = map1.get("onclick").toString();
						map.put(c, o);
					}
					Map<String, String> simpleInfoMap = new HashMap<String, String>();
					String useInMore = (map1.get("useInMore") != null) ? map1.get("useInMore").toString() : "false";
					String separateNum = (map1.get("separateNum") != null) ? map1.get("separateNum").toString() : "0";
					simpleInfoMap.put("useInMore", useInMore);
					simpleInfoMap.put("separateNum", separateNum);
					infoMap.put(map1.get("code").toString(), simpleInfoMap);
				}
			}
			if (arguments.size() > 3) {
				if (arguments.get(3) != null) {
					operateType = arguments.get(3).toString();
				}

			}
			if (!operateType.equals("noPower")) {// 判断是否需要进行权限判断
				Collection<MenuOperate> operates = new LinkedList<MenuOperate>();
				Long companyId = UserContext.getUserContext().getCompanyId();

				String entityCode = "";
				if (arguments.size() > 2 && null != arguments.get(2) && !"".equals(arguments.get(2).toString())) {
					entityCode = arguments.get(2).toString();
				}
				if (null != operateList && operateList.size() < 1) {
					if (menuInfoService != null) {
						if (!"".equals(menuCode)) {
							MenuInfo mi = menuInfoService.get(menuCode);
							if(mi != null){
								List<MenuOperate> tempOperates = menuInfoService.getOperateList(mi);
								if (!tempOperates.isEmpty()) {
									operates.addAll(tempOperates);
								}
							}
						} else {
							List<MenuInfo> menuList = menuInfoService.getEntityMenus(entityCode);
							for (MenuInfo me : menuList) {
								List<MenuOperate> tempOperates = menuInfoService.getOperateList(me);
								if (!tempOperates.isEmpty()) {
									operates.addAll(tempOperates);
								}
							}
						}

					}

					for (MenuOperate mo : operates) {
						String url = mo.getUrl();
						if (url == null || mo.getIsHidden()) {
							continue;
						}
						codes.add(mo.getCode());
						if (null != url && null != mo.getDeploymentId() && MenuOperateType.ACTIVEOPERATE.equals(mo.getMenuOperateType())) {

							if (url.indexOf("?") != -1) {
								url += "&deploymentId=" + mo.getDeploymentId();
							} else {
								url += "?deploymentId=" + mo.getDeploymentId();
							}
							url += "&entityCode=" + entityCode;
						}

						// 添加权限控制
						url += (url.indexOf("?") == -1 ? "?" : "&") + "__pc__="
								+ new String(OrchidUtils.encode((mo.getCode() + "|" + (mo.getFlowKey() == null ? "" : mo.getFlowKey())).getBytes()));
						String onlickStr = "operateBarOnclickFoundation('" + url + "')";
						map.put(mo.getCode(), onlickStr);
					}

				} else {
					List<MenuOperate> l = menuOperateService.getByCodes(codes, companyId);
					operates.addAll(l);
				}
				Long userId = UserContext.getUserContext().getUserId();
				if (!codes.isEmpty()) {
					if (menuOperateService != null) {
						if (null != operateType && "FLOW".equals(operateType.toUpperCase())) {// 流程的启动权限，通过DataPermission查询
							String entityType = entityService.getEntity(entityCode).getEntityType().toString();
							if ("SYSTEM/S2".equals(entityType)) {
								MenuInfo menuInfo = menuInfoService.get(menuCode);
								List<Map<String, Object>> s2List = menuOperateService.getS2BillInfo(userId, menuInfo.getId());
								if (s2List != null && s2List.size() > 0) {
									for (Map<String, Object> s2Map : s2List) {
										Map<String, String> btnMap = new HashMap<String, String>();
										btnMap.put("CODE", (String) s2Map.get("POWERCODE"));
										String url = "/app/Genius?_ets_=Base/WorkFlow/TemplateContainer.ets&WorkFlowTableCode=" + s2Map.get("FLOWTABLECODE")
												+ "&PowerCode=" + (String) s2Map.get("POWERCODE") + "&TransitionID="
												+ ((Number) s2Map.get("TRANSITIONID")).longValue() + "&ActiveID="
												+ ((Number) s2Map.get("ACTIVEID")).longValue()
												+ "&MasterOperateGroup=no&IsWorkFlowOperateGroup=1&MenuUser_OperateState=0&MenuUser_PositionFlag=0&ActiveUID="
												+ (String) s2Map.get("POWERCODE");
										btnMap.put("ONCLICK", "operateBarOnclickFoundation('" + url + "')");
										btnMap.put("ICONCLS", "cui-btn-add");
										btnMap.put("NAME", (String) s2Map.get("NAME"));
										Map<String, String> simpleInfoMap = infoMap.get(s2Map.get("POWERCODE"));
										if (null != simpleInfoMap && !simpleInfoMap.isEmpty()) {
											btnMap.put("USEINMORE", (simpleInfoMap.get("useInMore") != null) ? simpleInfoMap.get("useInMore") : "false");
											btnMap.put("SEPARATENUM", (simpleInfoMap.get("separateNum") != null) ? simpleInfoMap.get("separateNum") : "false");
										}
										btns.add(btnMap);
									}
								}

							} else {
								Set<Map<String, Object>> startList = dataPermissionService.getFlowStart(entityCode, userId);
								int o = 0;
								for (MenuOperate mo : operates) {
									for (Map<String, Object> dp : startList) {
										String f = dp.get("FLOWKEY").toString();
										// String flowVersion=dp.get("FLOWVERSION").toString();
										String a = dp.get("ACTIVITYCODE").toString();
										if (f.equals(mo.getFlowKey()) && a.equals(mo.getCode())) {
											o++;
										}
									}
								}
								for (MenuOperate mo : operates) {
									if(mo.getIsHidden()){
										continue;
									}
									for (Map<String, Object> dp : startList) {
										String flowKey = dp.get("FLOWKEY").toString();
										// String flowVersion=dp.get("FLOWVERSION").toString();
										String activeCode = dp.get("ACTIVITYCODE").toString();
										if (flowKey.equals(mo.getFlowKey()) && activeCode.equals(mo.getCode())) {
											Map<String, String> m = new HashMap<String, String>();
											m.put("CODE", mo.getCode());
											m.put("ONCLICK", map.get(mo.getCode()));
											m.put("ICONCLS", "cui-btn-add");
											m.put("PROCESSKEY", flowKey);
											String internationName = InternationalResource.get("ec.flow.add");

											if (o <= 1) {
												m.put("NAME", internationName);
											} else {
												String flowName = InternationalResource.get((dp.get("NAME") == null ? "" : dp.get("NAME")) + "");
												m.put("NAME", internationName + flowName);
											}
											Map<String, String> simpleInfoMap = infoMap.get(mo.getCode());
											if (null != simpleInfoMap && !simpleInfoMap.isEmpty()) {
												m.put("USEINMORE", (simpleInfoMap.get("useInMore") != null) ? simpleInfoMap.get("useInMore") : "false");
												m.put("SEPARATENUM", (simpleInfoMap.get("separateNum") != null) ? simpleInfoMap.get("separateNum") : "false");
											}
											btns.add(m);
										}
									}
								}
							}

						} else {// 查找普通的操作权限，通过userpermission查找
//							if (null != operates && !operates.isEmpty()) {
//								Collection<GrantedAuthority> permissions = authentication.getUserPermissions();
//								if (permissions != null && !permissions.isEmpty()) {
//									// AntPathMatcher matcher = new AntPathMatcher();
//									// Iterator<GrantedAuthority> authorityIterator = permissions.iterator();
//									Map<String, String> m;
//									Set<MenuOperate> ms = new HashSet<MenuOperate>();
//									for (MenuOperate mo : operates) {
//										if(mo.getIsHidden()){
//											continue;
//										}
//										UserPermission up = userPermissionService.findPermissionByOperateCodeAndUserId(mo, userId);
//										if (null != up)
//											ms.add(mo);
//									}
//
//									for (String c : codes)
//										for (MenuOperate mo : ms) {
//											if (c.equals(mo.getCode())) {
//												m = new HashMap<String, String>();
//												m.put("CODE", mo.getCode());
//												m.put("ONCLICK", map.get(mo.getCode()));
//												if("cui-btn-none".equals(mo.getIconCls())){
//													m.put("ICONCLS", "cui-btn-add");
//												}else{
//													m.put("ICONCLS", mo.getIconCls());
//												}
//
//												m.put("NAME", InternationalResource.get(mo.getName()));
//												Map<String, String> simpleInfoMap = infoMap.get(mo.getCode());
//												if (null != simpleInfoMap && !simpleInfoMap.isEmpty()) {
//													m.put("USEINMORE", (simpleInfoMap.get("useInMore") != null) ? simpleInfoMap.get("useInMore") : "false");
//													m.put("SEPARATENUM", (simpleInfoMap.get("separateNum") != null) ? simpleInfoMap.get("separateNum")
//															: "false");
//												}
//												btns.add(m);
//											}
//										}
//								}
//
//							}
						}
					}
				}
			} else {
				for (Map<String, String> opp : operateList) {
					Map<String, String> mm = new HashMap<String, String>();
					mm.put("CODE", (opp.get("code") != null) ? opp.get("code").toString() : "");
					mm.put("ONCLICK", (opp.get("onclick") != null) ? opp.get("onclick").toString() : "");
					
					if(opp.get("iconcls") != null && "none".equals(opp.get("iconcls"))){
						mm.put("ICONCLS", "cui-btn-add");
					}else{
						mm.put("ICONCLS", (opp.get("iconcls") != null) ? "cui-btn-" + opp.get("iconcls") : "");
					}
					
					mm.put("NAME", (opp.get("name") != null) ? opp.get("name").toString() : "");
					mm.put("USEINMORE", (opp.get("useInMore") != null) ? opp.get("useInMore").toString() : "false");
					mm.put("SEPARATENUM", (opp.get("separateNum") != null) ? opp.get("separateNum").toString() : "0");
					if (opp.get("separate") != null) {
						mm.put("SEPARATE", "true");
					}
					btns.add(mm);
				}

			}
		} else if (arguments.size() == 9) {
			HttpRequestParametersHashModel request = (HttpRequestParametersHashModel) arguments.get(6);
			TemplateCollectionModel keys = request.keys();
			Map<String, Object> params = new HashMap<String, Object>();
			TemplateModelIterator iterator = keys.iterator();
			while (iterator.hasNext()) {
				TemplateModel key = iterator.next();
				params.put(key.toString(), request.get(key.toString()).toString());
			}
			params.put("params", arguments);
			btns = buttonOperaterPowerService.getButtonOperaterPower(params);
		}
		String html = "";
		String resultType = arguments.get(4).toString();
		if (!"JSON".equalsIgnoreCase(resultType)) {
			for (Map<String, String> bt : btns) {
				String opCode = (bt.get("CODE") != null) ? bt.get("CODE").toString() : "";
				String opIconcls = (bt.get("ICONCLS") != null) ? bt.get("ICONCLS").toString() : "";
				String name = (bt.get("NAME") != null) ? bt.get("NAME").toString() : "";
				String onclick = (bt.get("ONCLICK") != null) ? bt.get("ONCLICK").toString() : "";
				if(!"".equals(buttonCode) && "CUSTOM".equals(buttonType) && !"".equals(onclick)) {
					int index=onclick.lastIndexOf("(");
					onclick="signatureUtil.sign('"+buttonCode+"','"+onclick.substring(0,index)+"')";
				}
				String processKey = (bt.get("PROCESSKEY") != null) ? bt.get("PROCESSKEY").toString() : "";
				html += "<a href=\"#\" id=\"" + opCode + "\" class=\"cui-btn mr10 " + opIconcls + "\" processKey=\"" + processKey + "\" onclick=\"" + onclick
						+ "\">" + name + "</a>";
			}
		} else {
			html += "buttons = [";
			for (Iterator<Map<String, String>> it = btns.iterator(); it.hasNext();) {
				Map<String, String> bt = it.next();
				html += "{text:\"" + ((bt.get("NAME") != null) ? bt.get("NAME").toString() : "") + "\",";
				html += "iconClass:\"" + ((bt.get("ICONCLS") != null) ? bt.get("ICONCLS").toString() : "") + "\",";
				html += "code:\"" + ((bt.get("CODE") != null) ? bt.get("CODE").toString() : "") + "\",";
				if (subButtons != null) {
					html += subButtons + ",";
				}
				html += "useInMore:\"" + ((bt.get("USEINMORE") != null) ? bt.get("USEINMORE").toString() : "false") + "\",";
				html += "separateNum:\"" + ((bt.get("SEPARATENUM") != null) ? bt.get("SEPARATENUM").toString() : "0") + "\",";
				html += "separate:\"" + ((bt.get("SEPARATE") != null) ? bt.get("SEPARATE").toString() : "") + "\",";
				html += "attr:{processkey:\"" + ((bt.get("PROCESSKEY") != null) ? bt.get("PROCESSKEY").toString() : "") + "\"},";
				String onclick=bt.get("ONCLICK") != null ? bt.get("ONCLICK").toString() : "";
				if(!"".equals(buttonCode) && "CUSTOM".equals(buttonType) && !"".equals(onclick)) {
					int index=onclick.lastIndexOf("(");
					onclick="signatureUtil.sign('"+buttonCode+"','"+onclick.substring(0,index)+"')";
					html += "handler:function(event){" + onclick + ";}}";
				}
				else {
					html += "handler:function(event){" + onclick + ";}}";
				}
				if (it.hasNext()) {
					html += ",";
				}
			}
			html += "];\r\n";
			html += "buttonsAll = addToButtons(buttonsAll,buttons);";

		}

		log.debug("============== OperatePowerMethod Cost {}ms.================", System.currentTimeMillis() - start);
		if("isMobile".equals(mobileParam)){
			JSONSerializer serializer = new JSONSerializer();
			String json="";
			if(null != operateType && !"FLOW".equals(operateType.toUpperCase())){
				if(!btns.isEmpty()){
					json = serializer.serialize(btns.iterator().next());
				}
			}else{
				json = serializer.serialize(btns);
			}
			return new SimpleScalar(json);
		}
		return new SimpleScalar(html);
		// LOGGER.info(html);
		// return html;
	}

}