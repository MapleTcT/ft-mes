package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.base.services.MenuOperateService;
import com.supcon.supfusion.base.utils.ProjectFlagHolder;
import com.supcon.supfusion.configuration.services.dao.ButtonDaoImpl;
import com.supcon.supfusion.configuration.services.entity.Button;
import com.supcon.supfusion.configuration.services.entity.DataGrid;
import com.supcon.supfusion.configuration.services.entity.Event;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.enums.OperateType;
import com.supcon.supfusion.configuration.services.enums.ViewType;
import com.supcon.supfusion.configuration.services.service.ButtonService;
import com.supcon.supfusion.configuration.services.service.EventService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.services.utils.SerializeUitls;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实体配置信息按钮操作实现
 * 
 * 
 * @author fangzhibin
 * @version $Id$
 */
@Slf4j
@ServiceApiService
public class ButtonServiceImpl implements ButtonService {


	private static final String[] BUTTON_CONSTANT = { "name", "operateType", "isConfirm", "isHide", "confirmContent", "buttonStyle", "isUseMore",
			"isPermission", "isCallback", "isCustomFunc", "operateUrl", "displayName", "regionType", "scriptCode","permissionCode","buttonAlign","isPublished","isSignatureConfig","releaseFelid" };

	private ButtonDaoImpl buttonDao;

	private EventService eventService;

	private ViewService viewService;

	@Autowired
	private MenuOperateService menuOperateService;

	@Autowired
	private ModelService modelService;
	@Override
	@Transactional
	public void saveButton(Button button) {
		buttonDao.save(button);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Button getButton(String buttonCode) {
		return buttonDao.load(buttonCode);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Button getButton(Button button) {
		return buttonDao.load(button.getCode());
	}

	@Override
	@Transactional
	public void deleteButton(Button button) {
		buttonDao.deletePhysical(button);
		deleteButtonOperate(button);
	}

	@Override
	@Transactional
	public void deleteButton(String buttonCode) {
		Button button = getButton(buttonCode);
		if (null != button) {
			buttonDao.deletePhysical(button);
		}
	}

	@Autowired
	public void setButtonDao(ButtonDaoImpl buttonDao) {
		this.buttonDao = buttonDao;
	}

	@Override
	@Transactional
	public void deleteButtonByViewCode(String viewCode) {
		List<Button> buttons = getButtons(viewCode);
		if (null != buttons && !buttons.isEmpty()) {
			for (Button button : buttons) {
				Set<Event> events = button.getEvents();
				if (null != events && !events.isEmpty()) {
					for (Event event : events) {
						eventService.deleteEvent(event);
					}
				}
				deleteButton(button);
			}
		}
	}

	@Override
	@Transactional
	public void deleteButtonByDataGridCode(String dataGridCode) {
		List<Button> buttons = getButtonsByDataGridCode(dataGridCode);
		if (null != buttons && !buttons.isEmpty()) {
			for (Button button : buttons) {
				Set<Event> events = button.getEvents();
				if (null != events && !events.isEmpty()) {
					for (Event event : events) {
						eventService.deleteEvent(event);
					}
				}
				deleteButton(button);
			}
		}
	}

	@Override
	@Transactional
	public void deleteButtonByCellCodes(Object obj,String cellCodes) {
		if (cellCodes != null && cellCodes.length() > 0) {
			String[] cellCodeArr = cellCodes.split(",");
			for (String cellCode : cellCodeArr) {
				Button b = null;
				if(obj instanceof View){
					b=buttonDao.findEntityByCriteria(Restrictions.eq("view",obj),Restrictions.eq("cellCode", cellCode));
				}else if(obj instanceof DataGrid){
					b=buttonDao.findEntityByCriteria(Restrictions.eq("dataGrid",obj),Restrictions.eq("cellCode", cellCode));
				}else{
					b=buttonDao.findEntityByCriteria(Restrictions.eq("cellCode", cellCode));
				}
				if (null != b) {
					eventService.deleteEventByField(b.getCode());
					deleteButton(b);
					if (b.getIsPermission() != null && b.getIsPermission()) {
						String operateCode = null;
						if (null != b.getView() && (b.getView().getType() == ViewType.EDIT || b.getView().getType() == ViewType.VIEW)) {
							List<Button> buttonList = buttonDao.findByHql("from Button where permissionCode=?0 and view.entity.code=?1 and code!=?2",
									b.getPermissionCode(), b.getView().getEntity().getCode(), b.getCode());
							if (null == buttonList || buttonList.isEmpty()) {
								operateCode = b.getView().getEntity().getCode() + "_" + b.getPermissionCode();
							}
						} else {
							operateCode = b.getCode();
						}
						if(null != operateCode){
							menuOperateService.deleteMenuOperateByPhysical(operateCode);
						}
					}
				}
			}
			buttonDao.flush();
		}
	}

	@Transactional
	private void deleteButtonOperate(Button button) {
		String name = null, code = null, buttonCode = null;
		if (button.getView() != null) {
			code = button.getView().getCode();
			name = button.getView().getName();
		} else {
			code = button.getDataGrid().getCode();
			name = button.getDataGrid().getName();
		}
		String iconCls = (String) button.getButtonStyle();
		if(button.getButtonOperationCode()!=null){
			buttonCode=button.getButtonOperationCode();
		}else{
			if (null != iconCls) {
				buttonCode = name + "_" + button.getName() + "_" + iconCls + "_" + code;
			} else {
				buttonCode = name + "_" + button.getName() + "_" + code;
			}
			button.setButtonOperationCode(buttonCode);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Transactional
	public void saveButton(Object object, String fieldConfig, String btDelCellIds) {
 		if (null != btDelCellIds && btDelCellIds.length() > 0) {
			deleteButtonByCellCodes(object,btDelCellIds);
		}
		String code = "";
		View view = null;
		String moduleCode = null;
		String entityCode = null;
		if (object instanceof View) {
			code = ((View) object).getCode();
			view = (View) object;
			moduleCode = view.getModuleCode();
			entityCode = view.getEntity().getCode();
		} else if (object instanceof DataGrid) {
			DataGrid grid = (DataGrid) object;
			code = grid.getCode();
			moduleCode = grid.getModuleCode();
			entityCode = grid.getEntityCode();
		}
		if (fieldConfig != null && !fieldConfig.isEmpty()) {
			Map fieldsMap = (Map) SerializeUitls.deserialize(fieldConfig);
			if (fieldsMap != null && !fieldsMap.isEmpty()) {
				List<Map> buttons = (List<Map>) fieldsMap.get("buttons");
				if (buttons != null && !buttons.isEmpty()) {
					for (int i = 0; i < buttons.size(); i++) {
						Map<String, Object> map = buttons.get(i);

						Map newMap = new HashMap();
						newMap.put("button", map);
						String config = SerializeUitls.serializeAsXml(newMap);

						String buttonCode = code + "_" + map.get("regionType").toString() + "_" + map.get("name").toString();
						Button button = getButton(buttonCode);
						if (null == button) {
							button = new Button();
							button.setVersion(0);
						}

						button.setConfig(config);
						button.setModuleCode(moduleCode);
						button.setEntityCode(entityCode);
						if(Boolean.TRUE.equals(ProjectFlagHolder.getInstance().getProjFlag().get())){
							button.setProjFlag(true);
						}
						boolean hasImportButton = false;
						for (String fc : BUTTON_CONSTANT) {
							Class type;
							try {
								type = PropertyUtils.getPropertyType(button, fc);
								Method setMethod = button.getClass().getMethod(
										"set" + Character.toUpperCase(fc.charAt(0)) + fc.substring(1), type);
								if (map.get(fc) != null) {
									if (null != type
											&& ("OperateType".equals(type.getSimpleName()) || "RegionType".equals(type.getSimpleName()))) {
										String getmethodName = "get" + Character.toUpperCase(fc.charAt(0)) + fc.substring(1);
										Method getMethod = button.getClass().getMethod(getmethodName);
										Class clazz = getMethod.getReturnType();
										if ("OperateType".equals(type.getSimpleName())) {
											String opType = map.get(fc).toString();
											if ("add".equals(opType)) {
												setMethod.invoke(button, Enum.valueOf(clazz, "ADD"));
											} else if ("edit".equals(opType)) {
												setMethod.invoke(button, Enum.valueOf(clazz, "MODIFY"));
											} else if ("del".equals(opType)) {
												setMethod.invoke(button, Enum.valueOf(clazz, "DELETE"));
											} else if ("custom".equals(opType)) {
												setMethod.invoke(button, Enum.valueOf(clazz, "CUSTOM"));
											} else if ("move".equals(opType)) {
													setMethod.invoke(button, Enum.valueOf(clazz, "MOVE"));
											} else if ("import".equals(opType)) {
												setMethod.invoke(button, Enum.valueOf(clazz, "IMPORT"));
											} else {
												if("IMPORT".equals(opType)){
													hasImportButton = true;
												}
												setMethod.invoke(button, Enum.valueOf(clazz, map.get(fc).toString()));
											}
										} else {
											setMethod.invoke(button, Enum.valueOf(clazz, map.get(fc).toString()));
										}
									} else {
										if (null != type && "buttonStyle".equalsIgnoreCase(type.getSimpleName()) && map.get(fc) != null
												&& "edit".equals(map.get(fc).toString())) {
											setMethod.invoke(button, "modify");
										} else {
											setMethod.invoke(button, map.get(fc));
										}
									}
								} else if ("displayName".equals(fc) && map.get("namekey") != null) {
									button.setDisplayName(map.get("namekey").toString());
								} else {
									setMethod.invoke(button, map.get(fc));
								}
							} catch (Exception e) {
								log.error(e.getMessage());
							}
						}
						if(null != button.getOperateType() && button.getOperateType().equals(OperateType.RESTORE)){
							button.setIsSignatureConfig(false);//还原按钮不需要支持电子签名
						} else {
							button.setIsSignatureConfig(true);//所有按钮都需要支持电子签名
						}
						
						if (object instanceof View) {
							View objView = (View) object;
							button.setView(objView);
						} else if (object instanceof DataGrid) {
							button.setDataGrid((DataGrid) object);
						}
						button.setCellCode(map.get("cellCode").toString());
						button.setCode(buttonCode);
						if (null != map.get("releaseFelid") && map.get("releaseFelid").toString().length() > 0) {
							button.setReleaseFelid(map.get("releaseFelid").toString());
						}else{
							button.setReleaseFelid(null);
						}
						if (null != map.get("confirmContent") && map.get("confirmContent").toString().length() > 0) {
							button.setConfirmContent(map.get("confirmContent").toString());
						}else{
							button.setConfirmContent(null);
						}
						if (null != map.get("viewSelect") && map.get("viewSelect").toString().length() > 0) {
							View viewSelect = viewService.getView(map.get("viewSelect").toString());
							button.setViewSelect(viewSelect);
						}else {
							button.setViewSelect(null);
						}
						buttonDao.save(button);
						// FIXME DataGrid暂时不支持权限按钮
						if (view != null && button.getOperateType() != OperateType.SEPARATE) {
							viewService.saveButtonOperate(view, button);
						}

						if (null != map.get("events") && map.get("events") instanceof List) {
							List<Map<String, String>> eventList = (List<Map<String, String>>) map.get("events");
							if (null != eventList && !eventList.isEmpty()) {
								for (Map<String, String> event : eventList) {
									if(event == null || event.keySet() == null || event.keySet().size() == 0){
										continue;
									}
									String type = event.get("name").toString().split("=")[0];
									if(type.endsWith(")")){
										type="callback";
									}
									Event e = eventService.getEvent(buttonCode + "_" + type);
									if (null == e) {
										e = new Event();
										e.setVersion(0);
									}
									e.setCode(buttonCode + "_" + type);
									e.setName(event.get("name").toString());
									e.setFunction(event.get("function").toString());
									e.setFunction_es5(event.get("function_es5"));
									e.setButton(button);
									e.setModuleCode(moduleCode);
									e.setEntityCode(entityCode);
									eventService.saveEvent(e);
								}
							}
						}

					}
				}
			}
		}

	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Map<String, Button> getButtons(View view) {
		if (null != view) {
			Map<String, Button> buttonMap = new HashMap<String, Button>();
			List<Button> buttons = this.getButtons(view.getCode());
			for (Button b : buttons) {
				buttonMap.put(b.getCellCode(), b);
			}
			return buttonMap;
		} else {
			return null;
		}
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Button> getButtons(String viewCode) {
		List<Button> buttons = buttonDao.findByHql("from Button where view.code=?0", viewCode);
		if (buttons != null && !buttons.isEmpty()) {
			for (Button button : buttons) {
				if(null != button.getEvents() && !button.getEvents().isEmpty()){
					for(Event event : button.getEvents()){
						if (!Hibernate.isInitialized(event)){
							Hibernate.initialize(event);
						}
					}
				}
			}
		}
		return buttons;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Map<String, Button> getButtons(DataGrid dataGrid) {
		if (null != dataGrid) {
			Map<String, Button> buttonsMap = new HashMap<String, Button>();
			List<Button> buttons = this.getButtonsByDataGridCode(dataGrid.getCode());
			for (Button b : buttons) {
				buttonsMap.put(b.getCellCode(), b);
			}
			return buttonsMap;
		} else {
			return null;
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Button> getButtonsByViewSelect(String viewCode) {
		return buttonDao.findByHql("from Button where viewSelect.code = ?0", viewCode);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<Button> getButtonsByDataGridCode(String dataGridCode) {
		List<Button> buttons = buttonDao.findByHql("from Button where dataGrid.code=?0", dataGridCode);
		if (buttons != null && !buttons.isEmpty()) {
			for (Button button : buttons) {
				if (!Hibernate.isInitialized(button.getEvents())) {
                    Hibernate.initialize(button.getEvents());
                }
			}
		}
		return buttons;
	}

	/**
	 * 处理Button的操作类型
	 */
	@Transactional
	@Override
	public void addOperateType(String moduleCode) {
		List<Button> buttons = null;
		if (moduleCode != null && moduleCode.length() > 0) {
			buttons = buttonDao.findByHql("from Button b where b.valid=true and b.view.entity.module.code=?0", moduleCode);
		} else {
			buttons = buttonDao.findByCriteria(Restrictions.eq("valid", true), Restrictions.isNull("operateType"));
		}
		if (buttons != null && !buttons.isEmpty()) {
			for (Button button : buttons) {
				if(null != button.getEvents() && !button.getEvents().isEmpty()){
					for(Event event : button.getEvents()){
						if (!Hibernate.isInitialized(event)){
							Hibernate.initialize(event);
						}
					}
				}
				if (null == button.getOperateType()) {
					String buttonStyle = button.getButtonStyle();
					if (null != buttonStyle && buttonStyle.length() > 0) {
						if ("edit".equals(buttonStyle)) {
							button.setButtonStyle("modify");
						}
						if (button.getViewSelect() != null) {
							if ("add".equals(buttonStyle)) {
								button.setOperateType(OperateType.ADD);
							} else if ("edit".equals(buttonStyle)) {
								button.setOperateType(OperateType.MODIFY);
							}
						} else {
							if (button.getEvents() != null && !button.getEvents().isEmpty()) {
								button.setOperateType(OperateType.CUSTOM);
							} else {
								button.setOperateType(OperateType.DELETE);
							}
						}
						buttonDao.save(button);
					}
				}
			}
		}
	}

	/**
	 * 根据条件查询Button
	 * @param criterions
	 * @return
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Button> findButtons(Criterion... criterions) {
		return buttonDao.findByCriteria(criterions);
	}
	@Autowired
	public void setEventService(EventService eventService) {
		this.eventService = eventService;
	}

	@Autowired
	public void setViewService(ViewService viewService) {
		this.viewService = viewService;
	}
	
	@Override
	public void mergeButton(Button button){
		this.buttonDao.merge(button);
	}
}
