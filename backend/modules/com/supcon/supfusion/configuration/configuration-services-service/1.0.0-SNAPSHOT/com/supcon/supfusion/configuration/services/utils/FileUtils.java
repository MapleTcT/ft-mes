/**
 * Copyright (C) 2011 ZHEJIANG SUPCON TECHNOLOGY CO.,LTD. 
 * All rights reserved.
 */
package com.supcon.supfusion.configuration.services.utils;

import com.supcon.supfusion.configuration.services.entity.Entity;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.base.entities.International;
import com.supcon.supfusion.base.entities.SystemCode;
import com.supcon.supfusion.base.entities.SystemEntity;
import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.entity.Module;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 
 * 操作BAP升级文件工具类
 * 
 * @author zhuyuyin
 * @version 1.0
 */
public class FileUtils {
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
	private static final String BAP_TEMP_PATH = PropertyHolder.get().getWorkspacePath() + "/updateTemp";
	private static final String TEMP_XML_NAME = "temp.xml";
	private static final String TEMP_SYSTEM_CODE_XML_NAME = "tempSystemCode.xml";
	private static final String OPERATE_TYPE_ATTRIBUTE = "operateType";
	private static final String BAP_CUSTOM_L10N_FILE_PATH = PropertyHolder.get().getCustomL10nPath();
	private static final String BAP_L10N_FILE_PATH = PropertyHolder.get().getL10nPath();
	private static final String L10N_REGEX = "^package\\w+\\.properties$";

	@SuppressWarnings("unchecked")
	public static synchronized void updateXml(Property property, Boolean isNew) {
		try {
			Model model = property.getModel();
			Entity entity = model.getEntity();
			if (null != entity && null != entity.getModule()) {
				File tempFile = createNewTempFile(entity.getModule());
				SAXReader reader = new SAXReader();
				Document xmlDoc = reader.read(tempFile);
				Element root = xmlDoc.getRootElement();
				createModelElement(model, false, entity, root);
				List<Node> modelElements = root.selectNodes("//model[@id='" + model.getModelName() + "']");
				Element modelElement = (Element) modelElements.get(0);
				List<Node> elements = modelElement.selectNodes("//model[@id='" + model.getModelName() + "']//property[@id='" + property.getName() + "']");
				Element element = null;
				if (null == elements || elements.isEmpty()) {
					element = modelElement.addElement("property");
					element.addAttribute("id", property.getName());
					if (isNew) {
						element = element.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
					} else {
						element = element.addAttribute(OPERATE_TYPE_ATTRIBUTE, "ALTER");
					}
				} else {
					element = (Element) elements.get(0);
				}
				String operateType = element.attributeValue(OPERATE_TYPE_ATTRIBUTE);
				if (null == operateType || !operateType.toUpperCase().equals("CREATE")) {// 如是新建的字段则不需要管理属性orgColumnName
					Attribute orgAttribute = element.attribute("orgColumnName");
					if (null != orgAttribute) {
						if (orgAttribute.getValue().equals(property.getColumnName())) {// 如字段将名称改回之前的则删除orgColumnName属性
							element.remove(orgAttribute);
						}
					} else {
						String orgColumnName = property.getOrgColumnName();
						if (null != orgColumnName && !orgColumnName.toUpperCase().equals(property.getColumnName())) {// 字段名修改过
							if (null == element.attribute("orgColumnName") || element.attributeValue("orgColumnName").trim().length() == 0) {
								element.addAttribute("orgColumnName", orgColumnName.toUpperCase());
							}
						}
					}
				}
				if (null == element.attribute("columnName") || element.attributeValue("columnName").trim().length() == 0) {
					element = element.addAttribute("columnName", property.getColumnName());
				} else {
					Attribute attribute = element.attribute("columnName");
					attribute.setValue(property.getColumnName());
				}
				String desc = property.getDescription();
				if (null != desc && desc.trim().length() > 0) {
					desc = ", " + desc.trim();
				} else {
					desc = "";
				}
				String description = InternationalResource.get(property.getDisplayName()) + desc;
				if (null == element.attribute("description") || element.attributeValue("description").trim().length() == 0) {
					element = element.addAttribute("description", description);
				} else {
					Attribute attribute = element.attribute("description");
					attribute.setValue(description);
				}
				if (property.getIsPk()) {
					if (null == element.attribute("isPk") || element.attributeValue("isPk").trim().length() == 0) {
						element = element.addAttribute("isPk", "true");
					} else {
						Attribute attribute = element.attribute("isPk");
						attribute.setValue("true");
					}
				}
				DbColumnType type = property.getType();
				if (property.getIsCustom() != null && property.getIsCustom()) { // 对象类型自定义字段：Long，系统编码类型自定义字段：String
					if (type.equals(DbColumnType.OBJECT)) {
						type = DbColumnType.LONG;
					} else if (type.equals(DbColumnType.SYSTEMCODE)) {
						type = DbColumnType.TEXT;
					}
				} else {
					Property tempProperty = property;
					while (type.equals(DbColumnType.OBJECT) && null != tempProperty.getAssociatedProperty()) {
						type = tempProperty.getAssociatedProperty().getType();
						tempProperty = tempProperty.getAssociatedProperty();
					}
					if (type.equals(DbColumnType.OBJECT)) {
						type = DbColumnType.LONG;
					}
				}
				Integer maxLength = property.getMaxLength();
				if (property.getName().equalsIgnoreCase("layRec")) {
					type = DbColumnType.TEXT;
					maxLength = 4000;
				}
				if (null == element.attribute("type") || element.attributeValue("type").trim().length() == 0) {
					element = element.addAttribute("type", type.name());
				} else {
					Attribute attribute = element.attribute("type");
					attribute.setValue(type.name());
				}
				if (null != maxLength) {
					if (null == element.attribute("length") || element.attributeValue("length").trim().length() == 0) {
						element = element.addAttribute("length", maxLength.toString());
					} else {
						Attribute attribute = element.attribute("length");
						attribute.setValue(maxLength.toString());
					}
				}
				if (null != property.getDecimalNum()) {
					if (null == element.attribute("scale") || element.attributeValue("scale").trim().length() == 0) {
						element = element.addAttribute("scale", property.getDecimalNum().toString());
					} else {
						Attribute attribute = element.attribute("scale");
						attribute.setValue(property.getDecimalNum().toString());
					}
				}
				if (property.getIsIndex()) {
					String indexName = "IDX_";
					if (model.getModelName().length() > 14) {
						indexName += model.getModelName().toUpperCase().substring(0, 13);
					} else {
						indexName += model.getModelName().toUpperCase();
					}
					if (property.getName().length() > 9) {
						indexName += "_" + property.getName().toUpperCase().substring(0, 8);
					} else {
						indexName += "_" + property.getName().toUpperCase();
					}
					List<Node> indexElements = modelElement.selectNodes("//index[@name='" + indexName + "']");
					Element indexElement = null;
					if (null == indexElements || indexElements.isEmpty()) {
						indexElement = element.addElement("index");
						indexElement = indexElement.addAttribute("name", indexName);
						indexElement = indexElement.addAttribute("columns", property.getName());
						indexElement = indexElement.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
					}
				}
				OutputFormat format = OutputFormat.createPrettyPrint();
				format.setEncoding("UTF-8");
				XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
				writer.write(xmlDoc);
				writer.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * 非同步方法，供批量生成自定义字段调用，提高效率 add by wq, 2015-12-03
	 * @param propertyList 字段列表
	 */
	@SuppressWarnings("unchecked")
	public static void updateXml(List<Property> propertyList) {
		try {
			Model model = propertyList.get(0).getModel();
			File tempFile = createNewTempFile(model.getEntity().getModule());
			Document xmlDoc = new SAXReader().read(tempFile);
			Element root = xmlDoc.getRootElement();
			createModelElement(model, false, model.getEntity(), root);
			List<Node> modelElements = root.selectNodes("//model[@id='" + model.getModelName() + "']");
			Element modelElement = (Element) modelElements.get(0);
			for (Property p : propertyList) {
				Element element = modelElement.addElement("property");
				element.addAttribute("id", p.getName());
				element.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
				element.addAttribute("columnName", p.getColumnName());
				String desc = p.getDescription();
				if (null != desc && desc.trim().length() > 0) {
					desc = ", " + desc.trim();
				} else {
					desc = "";
				}
				String description = InternationalResource.get(p.getDisplayName()) + desc;
				element.addAttribute("description", description);
				if (p.getIsPk()) {
					element.addAttribute("isPk", "true");
				}
				DbColumnType type = p.getType();
				if (p.getIsCustom() != null && p.getIsCustom()) { // 对象类型自定义字段：Long，系统编码类型自定义字段：String
					if (type.equals(DbColumnType.OBJECT)) {
						type = DbColumnType.LONG;
					} else if (type.equals(DbColumnType.SYSTEMCODE)) {
						type = DbColumnType.TEXT;
					}
				} else {
					Property tempProperty = p;
					while (type.equals(DbColumnType.OBJECT) && null != tempProperty.getAssociatedProperty()) {
						type = tempProperty.getAssociatedProperty().getType();
						tempProperty = tempProperty.getAssociatedProperty();
					}
					if (type.equals(DbColumnType.OBJECT)) {
						type = DbColumnType.LONG;
					}
				}
				Integer maxLength = p.getMaxLength();
				if (p.getName().equalsIgnoreCase("layRec")) {
					type = DbColumnType.TEXT;
					maxLength = 4000;
				}
				element.addAttribute("type", type.name());
				if (null != maxLength) {
					element.addAttribute("length", maxLength.toString());
				}
				if (null != p.getDecimalNum()) {
					element = element.addAttribute("scale", p.getDecimalNum().toString());
				}
				if (p.getIsIndex()) {
					String indexName = "IDX_";
					if (model.getModelName().length() > 14) {
						indexName += model.getModelName().toUpperCase().substring(0, 13);
					} else {
						indexName += model.getModelName().toUpperCase();
					}
					if (p.getName().length() > 9) {
						indexName += "_" + p.getName().toUpperCase().substring(0, 8);
					} else {
						indexName += "_" + p.getName().toUpperCase();
					}
					Element indexElement = element.addElement("index");
					indexElement = indexElement.addAttribute("name", indexName);
					indexElement = indexElement.addAttribute("columns", p.getName());
					indexElement = indexElement.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
				}
			}
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
			writer.write(xmlDoc);
			writer.flush();
			writer.close();
		} catch (Throwable ex) {
			logger.error(ex.getMessage(), ex);
			throw new EcException(ex);
		}
	}
	
	/**
	 * 保存模型时修改临时升级文件
	 * 
	 * @param model
	 * @param isNew
	 *            是否新建
	 */
	public static synchronized void updateXml(Model model, Boolean isNew) {
		if (null != model) {
			try {
				Entity entity = model.getEntity();
				if (null != entity && null != entity.getModule()) {
					File tempFile = createNewTempFile(entity.getModule());
					SAXReader reader = new SAXReader();
					Document xmlDoc = reader.read(tempFile);
					Element root = xmlDoc.getRootElement();
					createModelElement(model, isNew, entity, root);
					OutputFormat format = OutputFormat.createPrettyPrint();
					format.setEncoding("UTF-8");
					XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
					writer.write(xmlDoc);
					writer.close();
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	@SuppressWarnings("unchecked")
	private static synchronized void createModelElement(Model model, Boolean isNew, Entity entity, Element root) {
		List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "']");
		Element element = null;
		if (null == elements || elements.isEmpty()) {//节点不存在
			element = root.addElement("model");
			element.addAttribute("id", model.getModelName());
			if (isNew) {
				element = element.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
				Attribute extendsAttribute = element.attribute("extends");
				if (null == extendsAttribute && isNew) {
					if (model.getIsMain()) {
						if (null == model.getDataType() || model.getDataType().intValue() == 1) {// 普通模型
							element = element.addAttribute("extends", "AbstractEcFullEntity");
						} else {// 树形
							element = element.addAttribute("extends", "AbstractEcTreeFullEntity");
						}
					} else {
						if (null == model.getDataType() || model.getDataType().intValue() == 1) {// 普通模型
							element = element.addAttribute("extends", "AbstractEcPartEntity");
						} else {// 树形
							element = element.addAttribute("extends", "AbstractEcTreeEntity");
						}
					}
				}
			} else {
				element = element.addAttribute(OPERATE_TYPE_ATTRIBUTE, "ALTER");
			}
		} else {
			element = (Element) elements.get(0);
		}
		String operateType = element.attributeValue(OPERATE_TYPE_ATTRIBUTE);
		if (null != operateType && operateType.equalsIgnoreCase("create")) {// 新建
			String orgTableName = model.getOrgTableName();
			String tableName = model.getTableName();
			if (!tableName.equalsIgnoreCase(orgTableName)) {// 表名被修改过则需要修改额外的表如ACL、PA、SV、DI、MC、GI等的表名
				if (null != entity.getEnableAclRestrict() && entity.getEnableAclRestrict()) {// ACL
					List<Node> extraElements = root.selectNodes("//model[@id='" + model.getModelName() + "Acl']");
					if (null != extraElements && !extraElements.isEmpty()) {
						Element extraElement = (Element) extraElements.get(0);
						Attribute attribute = extraElement.attribute("orgTableName");
						attribute.setValue(tableName + "_ACL");
					}
				}
				if (null != model.getEntity().getWorkflowEnabled() && model.getEntity().getWorkflowEnabled() && null != model.getEntity().getGroupEnabled()
						&& model.getEntity().getGroupEnabled()) {
					List<Node> extraElements = root.selectNodes("//model[@id='" + model.getModelName() + "GroupInfo']");
					if (null != extraElements && !extraElements.isEmpty()) {
						Element extraElement = (Element) extraElements.get(0);
						Attribute attribute = extraElement.attribute("orgTableName");
						attribute.setValue(tableName + "_GI");
					}
				}
				if (model.getIsMain()) {
					List<Node> extraElements = root.selectNodes("//model[@id='" + model.getModelName() + "DealInfo']");
					if (null != extraElements && !extraElements.isEmpty()) {
						Element extraElement = (Element) extraElements.get(0);
						Attribute attribute = extraElement.attribute("orgTableName");
						attribute.setValue(tableName + "_DI");
					}
					if (model.getEntity().getWorkflowEnabled() != null && model.getEntity().getWorkflowEnabled()) {
						List<Node> extraElements1 = root.selectNodes("//model[@id='" + model.getModelName() + "Supervision']");
						if (null != extraElements1 && !extraElements1.isEmpty()) {
							Element extraElement = (Element) extraElements1.get(0);
							Attribute attribute = extraElement.attribute("orgTableName");
							attribute.setValue(tableName + "_SV");
						}
						if (model.getEntity().getPayCloseAttention() != null && model.getEntity().getPayCloseAttention()) {
							List<Node> extraElements2 = root.selectNodes("//model[@id='" + model.getModelName() + "PayCloseAttention']");
							if (null != extraElements2 && !extraElements2.isEmpty()) {
								Element extraElement = (Element) extraElements2.get(0);
								Attribute attribute = extraElement.attribute("orgTableName");
								attribute.setValue(tableName + "_PA");
							}
						}
					}
				}
				if (null != model.getIsMneCode() && model.getIsMneCode()) {
					List<Node> extraElements = root.selectNodes("//model[@id='" + model.getModelName() + "MneCode']");
					if (null != extraElements && !extraElements.isEmpty()) {
						Element extraElement = (Element) extraElements.get(0);
						Attribute attribute = extraElement.attribute("orgTableName");
						attribute.setValue(tableName + "_MC");
					}
				}
			}
		} else {// 表已存在
			Attribute orgAttribute = element.attribute("orgTableName");
			if (null != orgAttribute) {// 修改过表名
				if (orgAttribute.getValue().equals(model.getTableName())) {// 原始表名与当前表名一致则删除orgTableName属性
					element.remove(orgAttribute);
					// 删除额外的表如ACL、PA、SV、DI、MC、GI等
					if (null != entity.getEnableAclRestrict() && entity.getEnableAclRestrict()) {// ACL
						List<Node> aclElements = root.selectNodes("//model[@id='" + model.getModelName() + "Acl']");
						Element aclElement = null;
						if (null != aclElements && !aclElements.isEmpty()) {
							aclElement = (Element) aclElements.get(0);
							String opValue = aclElement.attributeValue(OPERATE_TYPE_ATTRIBUTE);
							String orgTable = aclElement.attributeValue("orgTableName");
							if (null != opValue && opValue.equals("CREATE")) {
								if (null != orgTable && orgTable.length() > 0) {
									aclElement.remove(aclElement.attribute("orgTableName"));
								}
							} else {
								root.remove(aclElement);
							}
						}
					}
					if (null != model.getEntity().getWorkflowEnabled() && model.getEntity().getWorkflowEnabled() && null != model.getEntity().getGroupEnabled()
							&& model.getEntity().getGroupEnabled()) {
						List<Node> giElements = root.selectNodes("//model[@id='" + model.getModelName() + "GroupInfo']");
						Element giElement = null;
						if (null != giElements && !giElements.isEmpty()) {
							giElement = (Element) giElements.get(0);
							String opValue = giElement.attributeValue(OPERATE_TYPE_ATTRIBUTE);
							String orgTable = giElement.attributeValue("orgTableName");
							if (null != opValue && opValue.equals("CREATE")) {
								if (null != orgTable && orgTable.length() > 0) {
									giElement.remove(giElement.attribute("orgTableName"));
								}
							} else {
								root.remove(giElement);
							}
						}
					}
					if (model.getIsMain()) {
						List<Node> diElements = root.selectNodes("//model[@id='" + model.getModelName() + "DealInfo']");
						Element diElement = null;
						if (null != diElements && !diElements.isEmpty()) {
							diElement = (Element) diElements.get(0);
							String opValue = diElement.attributeValue(OPERATE_TYPE_ATTRIBUTE);
							String orgTable = diElement.attributeValue("orgTableName");
							if (null != opValue && opValue.equals("CREATE")) {
								if (null != orgTable && orgTable.length() > 0) {
									diElement.remove(diElement.attribute("orgTableName"));
								}
							} else {
								root.remove(diElement);
							}
						}
						if (model.getEntity().getWorkflowEnabled() != null && model.getEntity().getWorkflowEnabled()) {
							List<Node> svElements = root.selectNodes("//model[@id='" + model.getModelName() + "Supervision']");
							Element svElement = null;
							if (null != svElements && !svElements.isEmpty()) {
								svElement = (Element) svElements.get(0);
								String opValue = svElement.attributeValue(OPERATE_TYPE_ATTRIBUTE);
								String orgTable = svElement.attributeValue("orgTableName");
								if (null != opValue && opValue.equals("CREATE")) {
									if (null != orgTable && orgTable.length() > 0) {
										svElement.remove(svElement.attribute("orgTableName"));
									}
								} else {
									root.remove(svElement);
								}
							}
							if (model.getEntity().getPayCloseAttention() != null && model.getEntity().getPayCloseAttention()) {
								List<Node> paElements = root.selectNodes("//model[@id='" + model.getModelName() + "PayCloseAttention']");
								Element paElement = null;
								if (null != paElements && !paElements.isEmpty()) {
									paElement = (Element) paElements.get(0);
									String opValue = paElement.attributeValue(OPERATE_TYPE_ATTRIBUTE);
									String orgTable = paElement.attributeValue("orgTableName");
									if (null != opValue && opValue.equals("CREATE")) {
										if (null != orgTable && orgTable.length() > 0) {
											paElements.remove(paElement.attribute("orgTableName"));
										}
									} else {
										root.remove(paElement);
									}
								}
							}
						}
					}
					if (null != model.getIsMneCode() && model.getIsMneCode()) {
						List<Node> mneElements = root.selectNodes("//model[@id='" + model.getModelName() + "MneCode']");
						Element mneElement = null;
						if (null != mneElements && !mneElements.isEmpty()) {
							mneElement = (Element) mneElements.get(0);
							String opValue = mneElement.attributeValue(OPERATE_TYPE_ATTRIBUTE);
							String orgTable = mneElement.attributeValue("orgTableName");
							if (null != opValue && opValue.equals("CREATE")) {
								if (null != orgTable && orgTable.length() > 0) {
									mneElement.remove(mneElement.attribute("orgTableName"));
								}
							} else {
								root.remove(mneElement);
							}
						}
					}
				} else {
					// 修改额外的表如ACL、PA、SV、DI、MC、GI等
					if (null != entity.getEnableAclRestrict() && entity.getEnableAclRestrict()) {// ACL
						List<Node> aclElements = root.selectNodes("//model[@id='" + model.getModelName() + "Acl']");
						Element aclElement = null;
						if (null != aclElements && !aclElements.isEmpty()) {
							aclElement = (Element) aclElements.get(0);
						} else {
							aclElement = root.addElement("model");
							aclElement = aclElement.addAttribute("id", model.getModelName() + "Acl");
						}
						if (null != aclElement.attribute("tableName")) {
							Attribute attribute = aclElement.attribute("tableName");
							attribute.setValue(model.getTableName() + "_ACL");
						} else {
							aclElement = aclElement.addAttribute("tableName", model.getTableName() + "_ACL");
						}
					}
					if (null != model.getEntity().getWorkflowEnabled() && model.getEntity().getWorkflowEnabled() && null != model.getEntity().getGroupEnabled()
							&& model.getEntity().getGroupEnabled()) {
						List<Node> giElements = root.selectNodes("//model[@id='" + model.getModelName() + "GroupInfo']");
						Element giElement = null;
						if (null != giElements && !giElements.isEmpty()) {
							giElement = (Element) giElements.get(0);
						} else {
							giElement = root.addElement("model");
							giElement = giElement.addAttribute("id", model.getModelName() + "GroupInfo");
						}
						if (null != giElement.attribute("tableName")) {
							Attribute attribute = giElement.attribute("tableName");
							attribute.setValue(model.getTableName() + "_GI");
						} else {
							giElement = giElement.addAttribute("tableName", model.getTableName() + "_GI");
						}
					}
					if (model.getIsMain()) {
						List<Node> diElements = root.selectNodes("//model[@id='" + model.getModelName() + "DealInfo']");
						Element diElement = null;
						if (null != diElements && !diElements.isEmpty()) {
							diElement = (Element) diElements.get(0);
						} else {
							diElement = root.addElement("model");
							diElement = diElement.addAttribute("id", model.getModelName() + "DealInfo");
						}
						if (null != diElement.attribute("tableName")) {
							Attribute attribute = diElement.attribute("tableName");
							attribute.setValue(model.getTableName() + "_DI");
						} else {
							diElement = diElement.addAttribute("tableName", model.getTableName() + "_DI");
						}
						if (model.getEntity().getWorkflowEnabled() != null && model.getEntity().getWorkflowEnabled()) {
							List<Node> svElements = root.selectNodes("//model[@id='" + model.getModelName() + "Supervision']");
							Element svElement = null;
							if (null != svElements && !svElements.isEmpty()) {
								svElement = (Element) svElements.get(0);
							} else {
								svElement = root.addElement("model");
								svElement = svElement.addAttribute("id", model.getModelName() + "Supervision");
							}
							if (null != svElement.attribute("tableName")) {
								Attribute attribute = svElement.attribute("tableName");
								attribute.setValue(model.getTableName() + "_SV");
							} else {
								svElement = svElement.addAttribute("tableName", model.getTableName() + "_SV");
							}
							if (model.getEntity().getPayCloseAttention() != null && model.getEntity().getPayCloseAttention()) {
								List<Node> paElements = root.selectNodes("//model[@id='" + model.getModelName() + "PayCloseAttention']");
								Element paElement = null;
								if (null != paElements && !paElements.isEmpty()) {
									paElement = (Element) paElements.get(0);
								} else {
									paElement = root.addElement("model");
									paElement = paElement.addAttribute("id", model.getModelName() + "PayCloseAttention");
								}
								if (null != paElement.attribute("tableName")) {
									Attribute attribute = paElement.attribute("tableName");
									attribute.setValue(model.getTableName() + "_PA");
								} else {
									paElement = paElement.addAttribute("tableName", model.getTableName() + "_PA");
								}
							}
						}
					}
					if (null != model.getIsMneCode() && model.getIsMneCode()) {
						List<Node> mneElements = root.selectNodes("//model[@id='" + model.getModelName() + "MneCode']");
						Element mneElement = null;
						if (null != mneElements && !mneElements.isEmpty()) {
							mneElement = (Element) mneElements.get(0);
						} else {
							mneElement = root.addElement("model");
							mneElement = mneElement.addAttribute("id", model.getModelName() + "MneCode");
						}
						if (null != mneElement.attribute("tableName")) {
							Attribute attribute = mneElement.attribute("tableName");
							attribute.setValue(model.getTableName() + "_MC");
						} else {
							mneElement = mneElement.addAttribute("tableName", model.getTableName() + "_MC");
						}
					}

				}
			} else {// 之前未修改过表名
				String orgTableName = model.getOrgTableName();
				if (null != orgTableName && !orgTableName.equals(model.getTableName())) {// 本次操作对表名修改
					if (null == element.attribute("orgTableName") || element.attributeValue("orgTableName").trim().length() == 0) {
						element.addAttribute("orgTableName", orgTableName);
						// 修改额外的表如ACL、PA、SV、DI、MC、GI等
						if (null != entity.getEnableAclRestrict() && entity.getEnableAclRestrict()) {// ACL
							List<Node> aclElements = root.selectNodes("//model[@id='" + model.getModelName() + "Acl']");
							Element aclElement = null;
							if (null != aclElements && !aclElements.isEmpty()) {
								aclElement = (Element) aclElements.get(0);
							} else {
								aclElement = root.addElement("model");
								aclElement = aclElement.addAttribute("id", model.getModelName() + "Acl");
							}
							if (null != aclElement.attribute("tableName")) {
								Attribute attribute = aclElement.attribute("tableName");
								attribute.setValue(model.getTableName() + "_ACL");
							} else {
								aclElement = aclElement.addAttribute("tableName", model.getTableName() + "_ACL");
							}
							if (null != aclElement.attribute("orgTableName")) {
								Attribute attribute = aclElement.attribute("orgTableName");
								attribute.setValue(orgTableName + "_ACL");
							} else {
								aclElement = aclElement.addAttribute("orgTableName", orgTableName + "_ACL");
							}
						}
						if (null != model.getEntity().getWorkflowEnabled() && model.getEntity().getWorkflowEnabled()
								&& null != model.getEntity().getGroupEnabled() && model.getEntity().getGroupEnabled()) {
							List<Node> giElements = root.selectNodes("//model[@id='" + model.getModelName() + "GroupInfo']");
							Element giElement = null;
							if (null != giElements && !giElements.isEmpty()) {
								giElement = (Element) giElements.get(0);
							} else {
								giElement = root.addElement("model");
								giElement = giElement.addAttribute("id", model.getModelName() + "GroupInfo");
							}
							if (null != giElement.attribute("tableName")) {
								Attribute attribute = giElement.attribute("tableName");
								attribute.setValue(model.getTableName() + "_GI");
							} else {
								giElement = giElement.addAttribute("tableName", model.getTableName() + "_GI");
							}
							if (null != giElement.attribute("orgTableName")) {
								Attribute attribute = giElement.attribute("orgTableName");
								attribute.setValue(orgTableName + "_GI");
							} else {
								giElement = giElement.addAttribute("orgTableName", orgTableName + "_GI");
							}
						}
						if (model.getIsMain()) {
							List<Node> diElements = root.selectNodes("//model[@id='" + model.getModelName() + "DealInfo']");
							Element diElement = null;
							if (null != diElements && !diElements.isEmpty()) {
								diElement = (Element) diElements.get(0);
							} else {
								diElement = root.addElement("model");
								diElement = diElement.addAttribute("id", model.getModelName() + "DealInfo");
							}
							if (null != diElement.attribute("tableName")) {
								Attribute attribute = diElement.attribute("tableName");
								attribute.setValue(model.getTableName() + "_DI");
							} else {
								diElement = diElement.addAttribute("tableName", model.getTableName() + "_DI");
							}
							if (null != diElement.attribute("orgTableName")) {
								Attribute attribute = diElement.attribute("orgTableName");
								attribute.setValue(orgTableName + "_DI");
							} else {
								diElement = diElement.addAttribute("orgTableName", orgTableName + "_DI");
							}
							if (model.getEntity().getWorkflowEnabled() != null && model.getEntity().getWorkflowEnabled()) {
								List<Node> svElements = root.selectNodes("//model[@id='" + model.getModelName() + "Supervision']");
								Element svElement = null;
								if (null != svElements && !svElements.isEmpty()) {
									svElement = (Element) svElements.get(0);
								} else {
									svElement = root.addElement("model");
									svElement = svElement.addAttribute("id", model.getModelName() + "Supervision");
								}
								if (null != svElement.attribute("tableName")) {
									Attribute attribute = svElement.attribute("tableName");
									attribute.setValue(model.getTableName() + "_SV");
								} else {
									svElement = svElement.addAttribute("tableName", model.getTableName() + "_SV");
								}
								if (null != svElement.attribute("orgTableName")) {
									Attribute attribute = svElement.attribute("orgTableName");
									attribute.setValue(orgTableName + "_SV");
								} else {
									svElement = svElement.addAttribute("orgTableName", orgTableName + "_SV");
								}
								if (model.getEntity().getPayCloseAttention() != null && model.getEntity().getPayCloseAttention()) {
									List<Node> paElements = root.selectNodes("//model[@id='" + model.getModelName() + "PayCloseAttention']");
									Element paElement = null;
									if (null != paElements && !paElements.isEmpty()) {
										paElement = (Element) paElements.get(0);
									} else {
										paElement = root.addElement("model");
										paElement = paElement.addAttribute("id", model.getModelName() + "PayCloseAttention");
									}
									if (null != paElement.attribute("tableName")) {
										Attribute attribute = paElement.attribute("tableName");
										attribute.setValue(model.getTableName() + "_PA");
									} else {
										paElement = paElement.addAttribute("tableName", model.getTableName() + "_PA");
									}
									if (null != paElement.attribute("orgTableName")) {
										Attribute attribute = paElement.attribute("orgTableName");
										attribute.setValue(orgTableName + "_PA");
									} else {
										paElement = paElement.addAttribute("orgTableName", orgTableName + "_PA");
									}
								}
							}
						}
						if (null != model.getIsMneCode() && model.getIsMneCode()) {
							List<Node> mneElements = root.selectNodes("//model[@id='" + model.getModelName() + "MneCode']");
							Element mneElement = null;
							if (null != mneElements && !mneElements.isEmpty()) {
								mneElement = (Element) mneElements.get(0);
							} else {
								mneElement = root.addElement("model");
								mneElement = mneElement.addAttribute("id", model.getModelName() + "MneCode");
							}
							if (null != mneElement.attribute("tableName")) {
								Attribute attribute = mneElement.attribute("tableName");
								attribute.setValue(model.getTableName() + "_MC");
							} else {
								mneElement = mneElement.addAttribute("tableName", model.getTableName() + "_MC");
							}
							if (null != mneElement.attribute("orgTableName")) {
								Attribute attribute = mneElement.attribute("orgTableName");
								attribute.setValue(orgTableName + "_MC");
							} else {
								mneElement = mneElement.addAttribute("orgTableName", orgTableName + "_MC");
							}
						}
					}
				}
			}
		}

		if (null == element.attribute("tableName") || element.attributeValue("tableName").trim().length() == 0) {
			element = element.addAttribute("tableName", model.getTableName());
		} else {
			Attribute tableNameAttribute = element.attribute("tableName");
			tableNameAttribute.setValue(model.getTableName());
		}

		if (isNew) {
			if (null != entity.getEnableAclRestrict() && entity.getEnableAclRestrict()) {// ACL
				createAclTable(root, model);
			}
			if (null != model.getEntity().getGroupEnabled()&& model.getEntity().getGroupEnabled()) {
				createGroupTable(root, model);
			}
			if (model.getIsMain()) {
				createDealInfoTable(root, model);
				if (model.getEntity().getWorkflowEnabled() != null && model.getEntity().getWorkflowEnabled()) {
					createSupervisionTable(root, model);
					if (model.getEntity().getPayCloseAttention() != null && model.getEntity().getPayCloseAttention()) {
						createPayCloseAttentionTable(root, model);
					}
				}

			}
			if (null != model.getIsMneCode() && model.getIsMneCode()) {
				createMneCodeTable(root, model);
			}
		}
	}

	/**
	 * 操作ACL权限表
	 * 
	 * @param root
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public static synchronized void createAclTable(Element root, Model model) {
		List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "Acl']");
		Element element = null;
		if (null == elements || elements.isEmpty()) {
			element = root.addElement("model");
			element.addAttribute("id", model.getModelName() + "Acl");
			element.addAttribute("tableName", model.getTableName() + "_ACL");
			element.addAttribute("extends", "IdEntity");

			Element sid = element.addElement("property");
			sid.addAttribute("id", "sid");
			sid.addAttribute("columnName", "SID");
			sid.addAttribute("type", "LONG");
			sid.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element permission = element.addElement("property");
			permission.addAttribute("id", "permission");
			permission.addAttribute("columnName", "PERMISSION");
			permission.addAttribute("type", "TEXT");
			permission.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element sidType = element.addElement("property");
			sidType.addAttribute("id", "sidType");
			sidType.addAttribute("columnName", "SID_TYPE");
			sidType.addAttribute("type", "TEXT");
			sidType.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element objectId = element.addElement("property");
			objectId.addAttribute("id", "object");
			objectId.addAttribute("columnName", "OBJECT_ID");
			objectId.addAttribute("type", "LONG");
			objectId.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
		} else {
			element = (Element) elements.get(0);
			Attribute attribute = element.attribute("tableName");
			attribute.setValue(model.getTableName() + "_ACL");
		}
	}

	/**
	 * 操作GroupInfo表
	 * 
	 * @param root
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public static synchronized void createGroupTable(Element root, Model model) {
		List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "GroupInfo']");
		Element element = null;
		if (null == elements || elements.isEmpty()) {
			element = root.addElement("model");
			element.addAttribute("id", model.getModelName() + "GroupInfo");
			element.addAttribute("tableName", model.getTableName() + "_GI");
			element.addAttribute("extends", "AbstractAppGroupEntity");
		} else {
			element = (Element) elements.get(0);
			Attribute attribute = element.attribute("tableName");
			attribute.setValue(model.getTableName() + "_GI");
		}
	}

	/**
	 * 操作DealInfo表
	 * 
	 * @param root
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public static synchronized void createDealInfoTable(Element root, Model model) {
		List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "DealInfo']");
		Element element = null;
		if (null == elements || elements.isEmpty()) {
			element = root.addElement("model");
			element.addAttribute("id", model.getModelName() + "DealInfo");
			element.addAttribute("tableName", model.getTableName() + "_DI");
			element.addAttribute("extends", "AbstractDealInfoEntity");

			Element sort = element.addElement("property");
			sort.addAttribute("id", "sort");
			sort.addAttribute("columnName", "SORT");
			sort.addAttribute("type", "INTEGER");
			sort.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element mainObj = element.addElement("property");
			mainObj.addAttribute("id", "mainObj");
			mainObj.addAttribute("columnName", "MAIN_OBJ");
			mainObj.addAttribute("type", "LONG");
			mainObj.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element staff = element.addElement("property");
			staff.addAttribute("id", "staff");
			staff.addAttribute("columnName", "STAFF");
			staff.addAttribute("type", "LONG");
			staff.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element recalledFlag = element.addElement("property");
			recalledFlag.addAttribute("id", "recalledFlag");
			recalledFlag.addAttribute("columnName", "RECALLED_FLAG");
			recalledFlag.addAttribute("type", "BOOLEAN");
			recalledFlag.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element userAgent = element.addElement("property");
			userAgent.addAttribute("id", "userAgent");
			userAgent.addAttribute("columnName", "USER_AGENT");
			userAgent.addAttribute("type", "TEXT");
			userAgent.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element tableInfoId = element.addElement("property");
			tableInfoId.addAttribute("id", "tableInfoId");
			tableInfoId.addAttribute("columnName", "TABLE_INFO_ID");
			tableInfoId.addAttribute("type", "LONG");
			tableInfoId.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element index = element.addElement("index");
			if (model.getModelName().length() > 14) {
				index.addAttribute("name", "IDX_" + model.getModelName().toUpperCase().substring(0, 13) + "_DiTABLEID");
			} else {
				index.addAttribute("name", "IDX_" + model.getModelName().toUpperCase() + "_DiTABLEID");
			}
			index.addAttribute("columns", "tableInfoId");
			index.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
		} else {
			element = (Element) elements.get(0);
			Attribute attribute = element.attribute("tableName");
			attribute.setValue(model.getTableName() + "_DI");
		}
	}

	/**
	 * 操作督办表
	 * 
	 * @param root
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public static synchronized void createSupervisionTable(Element root, Model model) {
		List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "Supervision']");
		Element element = null;
		if (null == elements || elements.isEmpty()) {
			element = root.addElement("model");
			element.addAttribute("id", model.getModelName() + "Supervision");
			element.addAttribute("tableName", model.getTableName() + "_SV");
			element.addAttribute("extends", "AbstractSupervisionEntity");

			Element valid = element.addElement("property");
			valid.addAttribute("id", "valid");
			valid.addAttribute("columnName", "VALID");
			valid.addAttribute("type", "BOOLEAN");
			valid.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element mainObj = element.addElement("property");
			mainObj.addAttribute("id", "mainObj");
			mainObj.addAttribute("columnName", "MAIN_OBJ");
			mainObj.addAttribute("type", "LONG");
			mainObj.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element staff = element.addElement("property");
			staff.addAttribute("id", "staff");
			staff.addAttribute("columnName", "STAFF");
			staff.addAttribute("type", "LONG");
			staff.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element tableInfoId = element.addElement("property");
			tableInfoId.addAttribute("id", "tableInfoId");
			tableInfoId.addAttribute("columnName", "TABLE_INFO_ID");
			tableInfoId.addAttribute("type", "LONG");
			tableInfoId.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element index = element.addElement("index");
			if (model.getModelName().length() > 14) {
				index.addAttribute("name", "IDX_" + model.getModelName().toUpperCase().substring(0, 13) + "_SuTABLEID");
			} else {
				index.addAttribute("name", "IDX_" + model.getModelName().toUpperCase() + "_SuTABLEID");
			}
			index.addAttribute("columns", "tableInfoId");
			index.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
		} else {
			element = (Element) elements.get(0);
			Attribute attribute = element.attribute("tableName");
			attribute.setValue(model.getTableName() + "_SV");
		}
	}

	/**
	 * 操作关注表
	 * 
	 * @param root
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public static synchronized void createPayCloseAttentionTable(Element root, Model model) {
		List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "PayCloseAttention']");
		Element element = null;
		if (null == elements || elements.isEmpty()) {
			element = root.addElement("model");
			element.addAttribute("id", model.getModelName() + "PayCloseAttention");
			element.addAttribute("tableName", model.getTableName() + "_PA");
			element.addAttribute("extends", "AbstractPayCloseAttentionEntity");

			Element valid = element.addElement("property");
			valid.addAttribute("id", "valid");
			valid.addAttribute("columnName", "VALID");
			valid.addAttribute("type", "BOOLEAN");
			valid.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element mainObj = element.addElement("property");
			mainObj.addAttribute("id", "mainObj");
			mainObj.addAttribute("columnName", "MAIN_OBJ");
			mainObj.addAttribute("type", "LONG");
			mainObj.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element staff = element.addElement("property");
			staff.addAttribute("id", "staff");
			staff.addAttribute("columnName", "STAFF");
			staff.addAttribute("type", "LONG");
			staff.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element tableInfoId = element.addElement("property");
			tableInfoId.addAttribute("id", "tableInfoId");
			tableInfoId.addAttribute("columnName", "TABLE_INFO_ID");
			tableInfoId.addAttribute("type", "LONG");
			tableInfoId.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element index = element.addElement("index");
			if (model.getModelName().length() > 14) {
				index.addAttribute("name", "IDX_" + model.getModelName().toUpperCase().substring(0, 13) + "_PaTABLEID");
			} else {
				index.addAttribute("name", "IDX_" + model.getModelName().toUpperCase() + "_PaTABLEID");
			}
			index.addAttribute("columns", "tableInfoId");
			index.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");
		} else {
			element = (Element) elements.get(0);
			Attribute attribute = element.attribute("tableName");
			attribute.setValue(model.getTableName() + "_PA");
		}
	}

	@SuppressWarnings("unchecked")
	public static synchronized void updateMneCodeTable(Model model){
		try {
			File tempFile = createNewTempFile(model.getEntity().getModule());
			SAXReader reader = new SAXReader();
			Document xmlDoc = reader.read(tempFile);
			Element root = xmlDoc.getRootElement();
			if(model.getIsMneCode()){//启用助记码
				createMneCodeTable(root, model);
			} else {
				List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "MneCode']");
				if(null != elements && !elements.isEmpty()){
					root.remove(elements.get(0));
				}
			}
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
			writer.write(xmlDoc);
			writer.close();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	@SuppressWarnings("unchecked")
	public static synchronized void updateAclTable(Model model){
		try {
			File tempFile = createNewTempFile(model.getEntity().getModule());
			SAXReader reader = new SAXReader();
			Document xmlDoc = reader.read(tempFile);
			Element root = xmlDoc.getRootElement();
			if(model.getEntity().getEnableAclRestrict()){//启用Acl
				createAclTable(root, model);
			} else {
				List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "Acl']");
				if(null != elements && !elements.isEmpty()){
					root.remove(elements.get(0));
				}
			}
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
			writer.write(xmlDoc);
			writer.close();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	@SuppressWarnings("unchecked")
	public static synchronized void updatePayCloseAttentionTable(Model model){
		try {
			File tempFile = createNewTempFile(model.getEntity().getModule());
			SAXReader reader = new SAXReader();
			Document xmlDoc = reader.read(tempFile);
			Element root = xmlDoc.getRootElement();
			if(model.getEntity().getPayCloseAttention()){//启用关注
				createPayCloseAttentionTable(root, model);
			} else {
				List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "PayCloseAttention']");
				if(null != elements && !elements.isEmpty()){
					root.remove(elements.get(0));
				}
			}
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
			writer.write(xmlDoc);
			writer.close();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static synchronized void updateGroupTable(Model model){
		try {
			File tempFile = createNewTempFile(model.getEntity().getModule());
			SAXReader reader = new SAXReader();
			Document xmlDoc = reader.read(tempFile);
			Element root = xmlDoc.getRootElement();
			if(model.getEntity().getGroupEnabled()){//启用关注
				createGroupTable(root, model);
			} else {
				List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "GroupInfo']");
				if(null != elements && !elements.isEmpty()){
					root.remove(elements.get(0));
				}
			}
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
			writer.write(xmlDoc);
			writer.close();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
	}
	
	/**
	 * 操作助记码表
	 * 
	 * @param root
	 * @param model
	 */
	@SuppressWarnings("unchecked")
	public static synchronized void createMneCodeTable(Element root, Model model) {
		List<Node> elements = root.selectNodes("//model[@id='" + model.getModelName() + "MneCode']");
		Element element = null;
		if (null == elements || elements.isEmpty()) {
			element = root.addElement("model");
			element.addAttribute("id", model.getModelName() + "MneCode");
			element.addAttribute("tableName", model.getTableName() + "_MC");
			element.addAttribute("extends", "IdEntity");

			Element mneCode = element.addElement("property");
			mneCode.addAttribute("id", "mneCode");
			mneCode.addAttribute("columnName", "MNE_CODE");
			mneCode.addAttribute("type", "TEXT");
			mneCode.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

			Element field = element.addElement("property");
			field.addAttribute("id", model.getModelName());
			field.addAttribute("columnName", Inflector.getInstance().columnize(model.getModelName()));
			field.addAttribute("type", "LONG");
			field.addAttribute(OPERATE_TYPE_ATTRIBUTE, "CREATE");

		} else {
			element = (Element) elements.get(0);
			Attribute attribute = element.attribute("tableName");
			attribute.setValue(model.getTableName() + "_MC");
		}
	}

	/**
	 * 创建临时目录
	 * @param module
	 * @return
	 */
	public static synchronized File createNewTempDir(Module module) {
		File tempFileDir = null;
		try {
			String filePath = BAP_TEMP_PATH + "/" + module.getArtifact();
			File moduleTempDir = new File(filePath);
			if (!moduleTempDir.exists()) {
				moduleTempDir.mkdirs();
			}
			if (moduleTempDir.isDirectory()) {
				File[] files = moduleTempDir.listFiles();
				boolean needCreatDirectory = true;
				if (null != files && files.length > 0) {
					for (File file : files) {
						if (file.isFile()) {
							continue;
						}
						String fileName = file.getName();
						// 当前版本已生成过升级文件 当前版本为1.0.1，如存在以"-1.0.1"结尾或名称为1.0.1的目录则不生成新目录
						if (fileName.equals(module.getProjectVersion()) || fileName.endsWith("-" + module.getProjectVersion())) {
							filePath = file.getAbsolutePath();
							needCreatDirectory = false;
							break;
						} else if (fileName.startsWith(module.getProjectVersion() + "-")) {// 当前版本为1.0.0
																							// 如存在以"1.0.0-"开始的目录则表明版本降低，则保证存在1.0.0目录即可
							filePath = file.getAbsolutePath();
							needCreatDirectory = false;
							break;
						}
					}
				}
				if (needCreatDirectory) {
					if (module.getProjectVersion().equals(module.getLastVersion())||null==module.getLastVersion()) {
						filePath += "/" + module.getProjectVersion();
					} else {
						filePath += "/" + module.getLastVersion() + "-" + module.getProjectVersion();
					}
				}
			}
			tempFileDir = new File(filePath);
			if (!tempFileDir.exists()) {
				tempFileDir.mkdirs();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return tempFileDir;
	}
	
	/**
	 * 创建临时文件
	 * 
	 * @param module
	 */
	public static synchronized File createNewTempFile(Module module) {
		File tempFile = null;
		try {
			File tempFileDir = createNewTempDir(module);
			if (!tempFileDir.exists()) {
				tempFileDir.mkdirs();
			}
			tempFile = new File(tempFileDir.getAbsolutePath() + "/" + TEMP_XML_NAME);
			if (!tempFile.exists()) {
				Document document = DocumentHelper.createDocument();
				document.addElement("Models");
				OutputFormat format = OutputFormat.createPrettyPrint();
				format.setEncoding("UTF-8");
				XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
				writer.write(document);
				writer.close();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return tempFile;
	}

	/**
	 * 根据Module生成对应的初始化文件
	 * 
	 * @param module
	 * @param
	 */
	public static synchronized boolean createInitXmlFile(Module module, String resourcesPath) {
		boolean flag = true;
		try {
			String initFilePath = resourcesPath + "/init";
			File file = new File(initFilePath);
			if (!file.exists()) {
				file.mkdirs();
			}
			File tempFile = new File(initFilePath + "/temp.xml");
			Document xmlDoc = DocumentHelper.createDocument();
			Element root = xmlDoc.addElement("Models");
			Set<Entity> entities = module.getEntities();
			if (null != entities && !entities.isEmpty()) {
				for (Entity entity : entities) {
					Set<Model> models = entity.getModels();
					if (null != models && !models.isEmpty()) {
						for (Model model : models) {
							if (null != model.getType() && Model.TYPE_SQL == model.getType()) {
								continue;
							}
							Element modelElement = root.addElement("model");
							modelElement = modelElement.addAttribute("id", model.getModelName());
							modelElement = modelElement.addAttribute("tableName", model.getTableName());
							if (model.getIsMain()) {
								if (null == model.getDataType() || model.getDataType().intValue() == 1) {// 普通模型
									modelElement = modelElement.addAttribute("extends", "AbstractEcFullEntity");
								} else {// 树形
									modelElement = modelElement.addAttribute("extends", "AbstractEcTreeFullEntity");
								}
							} else {
								if (null == model.getDataType() || model.getDataType().intValue() == 1) {// 普通模型
									modelElement = modelElement.addAttribute("extends", "AbstractEcPartEntity");
								} else {// 树形
									modelElement = modelElement.addAttribute("extends", "AbstractEcTreeEntity");
								}
							}
							String modelDesc = model.getDescription();
							if (null != modelDesc && modelDesc.trim().length() > 0) {
								modelDesc = ", " + modelDesc.trim();
							} else {
								modelDesc = "";
							}
							modelElement = modelElement.addAttribute("description", InternationalResource.get(model.getName()) + modelDesc);
							if (null != model.getIsExtraCol() && model.getIsExtraCol()) {
								Element propertyElement = modelElement.addElement("property");
								propertyElement = propertyElement.addAttribute("id", "extraCol");
								propertyElement = propertyElement.addAttribute("columnName", "EXTRA_COL");
								propertyElement = propertyElement.addAttribute("type", "LONGTEXT");
								propertyElement = propertyElement.addAttribute("description", "大字段");
							}
							if (entity.getWorkflowEnabled()) {
								Element propertyElement = modelElement.addElement("property");
								propertyElement = propertyElement.addAttribute("id", "tableInfoId");
								propertyElement = propertyElement.addAttribute("columnName", "TABLE_INFO_ID");
								propertyElement = propertyElement.addAttribute("type", "LONG");
								propertyElement = propertyElement.addAttribute("description", "表单ID");
							}
							// 生成property
							Set<Property> properties = model.getProperties();
							if (null != properties && !properties.isEmpty()) {
								for (Property property : properties) {
									DbColumnType type = property.getType();
									Integer maxLength = property.getMaxLength();
									if (type.equals(DbColumnType.PROPERTYATTACHMENT)) {
										continue;
									}
									String name = property.getName();
									String columnName = property.getColumnName().toUpperCase();
									if (property.getIsInherent()) {// 固有基础字段
										String orgColumnName = Inflector.getInstance().columnize(name);
										if (type.equals(DbColumnType.OBJECT)) {
											orgColumnName += "_ID";
										}
										if( property.getColumnName().equalsIgnoreCase("TABLE_INFO_ID")){
											String indexName = "IDX_";
											if (model.getModelName().length() > 14) {
												indexName += model.getModelName().toUpperCase().substring(0, 13);
											} else {
												indexName += model.getModelName().toUpperCase();
											}
											if (property.getName().length() > 9) {
												indexName += "_" + property.getName().toUpperCase().substring(0, 8);
											} else {
												indexName += "_" + property.getName().toUpperCase();
											}
											Element indexElement = modelElement.addElement("index");
											indexElement = indexElement.addAttribute("name", indexName);
											indexElement = indexElement.addAttribute("columns", property.getName());
										}
										if (orgColumnName.toUpperCase().equals(columnName)) {
											continue;
										}
									}
									if (property.getIsCustom() != null && property.getIsCustom()) { // 对象类型自定义字段：Long，系统编码类型自定义字段：String
										if (type.equals(DbColumnType.OBJECT)) {
											type = DbColumnType.LONG;
										} else if (type.equals(DbColumnType.SYSTEMCODE)) {
											type = DbColumnType.TEXT;
										}
									} else {
										Property tempProperty = property;
										while (type.equals(DbColumnType.OBJECT) && null != tempProperty.getAssociatedProperty()) {
											type = tempProperty.getAssociatedProperty().getType();
											tempProperty = tempProperty.getAssociatedProperty();
										}
										if (type.equals(DbColumnType.OBJECT)) {
											type = DbColumnType.LONG;
										}
									}
									if (property.getName().equalsIgnoreCase("layRec")) {
										type = DbColumnType.TEXT;
										maxLength = 4000;
									}
									Element propertyElement = modelElement.addElement("property");
									propertyElement = propertyElement.addAttribute("id", name);
									propertyElement = propertyElement.addAttribute("columnName", columnName);
									propertyElement = propertyElement.addAttribute("type", type.name());
									if (property.getIsPk()) {
										propertyElement = propertyElement.addAttribute("isPK", "true");
									}
									if (null != maxLength) {
										propertyElement = propertyElement.addAttribute("length", maxLength.toString());
									}
									if (null != property.getDecimalNum()) {
										propertyElement = propertyElement.addAttribute("scale", property.getDecimalNum().toString());
									}
									String desc = property.getDescription();
									if (null != desc && desc.trim().length() > 0) {
										desc = ", " + desc.trim();
									} else {
										desc = "";
									}
									propertyElement = propertyElement.addAttribute("description", InternationalResource.get(property.getDisplayName()) + desc);
									
									if ((property.getIsIndex() && !property.getIsPk())) {
										
										String indexName = "IDX_";
										if (model.getModelName().length() > 14) {
											indexName += model.getModelName().toUpperCase().substring(0, 13);
										} else {
											indexName += model.getModelName().toUpperCase();
										}
										if (property.getName().length() > 9) {
											indexName += "_" + property.getName().toUpperCase().substring(0, 8);
										} else {
											indexName += "_" + property.getName().toUpperCase();
										}
										Element indexElement = modelElement.addElement("index");
										indexElement = indexElement.addAttribute("name", indexName);
										indexElement = indexElement.addAttribute("columns", property.getName());
									}
								}
							}
							// 生成额外表
							if (null != entity.getEnableAclRestrict() && entity.getEnableAclRestrict()) {// ACL
								createAclTable(root, model);
							}
							if (null != model.getEntity().getGroupEnabled() && model.getEntity().getGroupEnabled()) {
								createGroupTable(root, model);
							}
							if (model.getIsMain()) {
								createDealInfoTable(root, model);
								if (null != model.getEntity().getWorkflowEnabled() && model.getEntity().getWorkflowEnabled()) {
									createSupervisionTable(root, model);
									if (null != model.getEntity().getPayCloseAttention() && model.getEntity().getPayCloseAttention()) {
										createPayCloseAttentionTable(root, model);
									}
								}

							}
							if (null != model.getIsMneCode() && model.getIsMneCode()) {
								createMneCodeTable(root, model);
							}
						}
					}
				}
			}
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(new FileWriter(tempFile), format);
			writer.write(xmlDoc);
			writer.close();
			File initFile = new File(initFilePath + "/init.xml");
			if (initFile.exists()) {
				initFile.delete();
			}
			copy(tempFile, initFilePath + "/init.xml");
			tempFile.delete();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			flag = false;
		}
		return flag;
	}

	/**
	 * 根据临时文件更新update升级文件
	 * 
	 * @param module
	 * @param generatePath
	 * @return TODO
	 */
	public static synchronized boolean copy2UpdateFile(Module module, String generatePath) {
		boolean flag = true;
		String filePath = BAP_TEMP_PATH + File.separator + module.getArtifact();
		File tempFile = new File(filePath);
		if (!tempFile.exists()) {// 临时目录不存在则直接返回
			return flag;
		}
		String projectVersion = module.getProjectVersion();
		if (tempFile.isDirectory()) {
			try {
				File[] files = tempFile.listFiles();
				File srcFile = null;
				String srcFileName = null;
				if(null != files && files.length > 0){
					for (File file : files) {
						String fileName = file.getName();
						if (file.isDirectory() && null == srcFileName) {
							if (fileName.equals(projectVersion) || fileName.endsWith("-" + projectVersion)) {
								srcFileName = fileName;
							}
						}
					}
				}
				if (StringUtils.isEmpty(srcFileName)) {
					return true;
				}
				while (srcFileName.contains("-")) {
					srcFile = new File(filePath + File.separator + srcFileName);
					if (!srcFile.exists()) {
						break;
					}
					String filePerfixName = srcFileName.substring(0, srcFileName.indexOf("-"));
					File targetFile = null;
					for (File file : files) {
						if (file.isFile()) {
							continue;
						}
						if (file.getName().equals(srcFileName)) {
							continue;
						}
						if (file.getName().equals(filePerfixName) || file.getName().endsWith("-" + filePerfixName)) {
							targetFile = file;
							break;
						}
					}
					if (null == targetFile) {
						break;
					}
					File srcXmlFile = new File(srcFile.getAbsoluteFile() + File.separator + TEMP_XML_NAME);
					File targetXmlFile = new File(targetFile.getAbsoluteFile() + File.separator + TEMP_XML_NAME);
					mergeFile(srcXmlFile, targetXmlFile);
					File srcSystemCodeXmlFile = new File(srcFile.getAbsoluteFile() + File.separator + TEMP_SYSTEM_CODE_XML_NAME);
					File targetSystemCodeXmlFile = new File(targetFile.getAbsoluteFile() + File.separator + TEMP_SYSTEM_CODE_XML_NAME);
					mergeSystemCodeFile(srcSystemCodeXmlFile, targetSystemCodeXmlFile);
					srcFileName = targetFile.getName();
				}
				String updateBaseFilePath = null;
				if (!srcFileName.equalsIgnoreCase(projectVersion)) {
					String srcPath = srcFileName;
					if (!srcFileName.contains("-")) {
						srcPath = srcFileName + "-" + projectVersion;
					}
					updateBaseFilePath = generatePath + "update" + File.separator + srcPath;
				} else {
					updateBaseFilePath = generatePath + File.separator + "update";
					File baseUpdateFile = new File(updateBaseFilePath);
					if (null != baseUpdateFile && baseUpdateFile.exists() && baseUpdateFile.isDirectory()) {
						String updateFileName = null;
						File[] fs=baseUpdateFile.listFiles();
						if(fs !=null){
						for (File file : fs) {
							String fileName = file.getName();
							if (file.isDirectory()) {
								if (fileName.endsWith("-" + projectVersion)) {
									if (null == updateFileName) {
										updateFileName = fileName;
									} else {
										if (updateFileName.compareTo(fileName) < 0) {
											updateFileName = fileName;
										}
									}
								}
							}
						}
						}
						if (null != updateFileName) {
							updateBaseFilePath += File.separator + updateFileName;
						}
					} else {
						updateBaseFilePath = null;
					}
				}
				if (null != updateBaseFilePath) {
					File updateFile = new File(updateBaseFilePath + File.separator + "update.xml");
					srcFile = new File(filePath + File.separator + srcFileName + File.separator + TEMP_XML_NAME);
					mergeFile(srcFile, updateFile);
					File updateSystemCodeFile = new File(updateBaseFilePath + File.separator + "systemcode.xml");
					File srcSystemCodeFile = new File(filePath + File.separator + srcFileName + File.separator + TEMP_SYSTEM_CODE_XML_NAME);
					mergeSystemCodeFile(srcSystemCodeFile, updateSystemCodeFile);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				flag = false;
			}
		}
		return flag;
	}

	/**
	 * 删除临时文件
	 * 
	 * @param module
	 * @return
	 */
	public static boolean deleteTempFile(Module module) {
		boolean flag = true;
		try {
			String filePath = BAP_TEMP_PATH + File.separator + module.getArtifact();
			File file = new File(filePath);
			if (file.exists()) {
				deleteDirectory(file);
				flag = true;
			}
		} catch (Exception e) {
			flag = false;
			logger.error(e.getMessage(), e);
		}
		return flag;
	}

	/**
	 * 合并2个系统编码xml文件
	 * 
	 * @param srcFile
	 * @param targetFile
	 * @throws DocumentException
	 * @throws IOException
	 */
	public static void mergeSystemCodeFile(File srcFile, File targetFile) throws DocumentException, IOException {
		if (!srcFile.exists())
			return;
		if (!targetFile.exists()) {
			targetFile.getParentFile().mkdirs();
			copy(srcFile, targetFile.getAbsolutePath());
			return;
		} else {
			SAXReader reader = new SAXReader();
			Document srcXmlDoc = reader.read(srcFile);
			Document updateXmlDoc = reader.read(targetFile);
			Map<String, Object> modelAttributeMap = XMLUpgradeUtils.getSystemCodeAttributeMap(srcXmlDoc);
			if (null != modelAttributeMap && !modelAttributeMap.isEmpty()) {
				Element root = updateXmlDoc.getRootElement();
				for (Map.Entry<String, Object> entry : modelAttributeMap.entrySet()) {
					String systemEntityCode = entry.getKey();
					Map<String, Object> systemEntityMap = (Map<String, Object>) entry.getValue();
					List<Node> systemEntityElements = root.selectNodes("//systementity[@code='" + systemEntityCode + "']");
					Element systemEntityElement = null;
					if (null != systemEntityElements && !systemEntityElements.isEmpty()) {
						systemEntityElement = (Element) systemEntityElements.get(0);
					} else {
						systemEntityElement = root.addElement("systementity");
					}
					//遍历SystemEntity属性
					Map<String, String> attributeMap = (Map<String, String>) systemEntityMap.get("systemEntity");
					if (null != attributeMap && !attributeMap.isEmpty()) {
						for (Map.Entry<String, String> attributeEntry : attributeMap.entrySet()) {
							String key = attributeEntry.getKey();
							Attribute attribute = systemEntityElement.attribute(key);
							if (null == attribute) {
								systemEntityElement = systemEntityElement.addAttribute(key, attributeEntry.getValue());
							} else {
								attribute.setValue(attributeEntry.getValue());
							}
						}
					}
					// 遍历SystemCode
					Map<String, Object> systemCodeMap = (Map<String, Object>) systemEntityMap.get("systemCode");
					if (null != systemCodeMap && !systemCodeMap.isEmpty()) {
						for (Map.Entry<String, Object> systemCodeEntry : systemCodeMap.entrySet()) {
							String systemCodeId = systemCodeEntry.getKey();
							Map<String, String> systemCodeAttrMap = (Map<String, String>) systemCodeEntry.getValue();
							List<Node> systemCodeElements = systemEntityElement.selectNodes("//systementity[@code='" + systemEntityCode + "']//systemcode[@id='" + systemCodeId + "']");
							Element systemCodeElement = null;
							if (null != systemCodeElements && !systemCodeElements.isEmpty()) {
								systemCodeElement = (Element) systemCodeElements.get(0);
							} else {
								systemCodeElement = systemEntityElement.addElement("systemcode");
							}
							// 遍历SystemCode属性
							if (null != systemCodeAttrMap && !systemCodeAttrMap.isEmpty()) {
								for (Map.Entry<String, String> attributeEntry : systemCodeAttrMap.entrySet()) {
									String key = attributeEntry.getKey();
									Attribute attribute = systemEntityElement.attribute(key);
									if (null == attribute) {
										systemEntityElement = systemEntityElement.addAttribute(key, attributeEntry.getValue());
									} else {
										attribute.setValue(attributeEntry.getValue());
									}
								}
							}
						}
					}
				}
			}
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");
			XMLWriter writer = new XMLWriter(new FileWriter(targetFile), format);
			writer.write(updateXmlDoc);
			writer.close();
		}
	}
	
	/**
	 * 合并2个xml文件
	 * 
	 * @param srcFile
	 * @param targetFile
	 * @throws DocumentException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void mergeFile(File srcFile, File targetFile) throws DocumentException, IOException {
		if (!srcFile.exists())
			return;
		if (!targetFile.exists()) {
			targetFile.getParentFile().mkdirs();
			copy(srcFile, targetFile.getAbsolutePath());
			return;
		} else {
			SAXReader reader = new SAXReader();
			Document srcXmlDoc = reader.read(srcFile);
			Document updateXmlDoc = reader.read(targetFile);
			Map<String, Object> modelAttributeMap = XMLUpgradeUtils.getModelAttributeMap(srcXmlDoc, false, null);
			if (null != modelAttributeMap && !modelAttributeMap.isEmpty()) {
				Element root = updateXmlDoc.getRootElement();
				for (Map.Entry<String, Object> entry : modelAttributeMap.entrySet()) {
					String modelId = entry.getKey();
					Map<String, Object> modelMap = (Map<String, Object>) entry.getValue();
					List<Node> elements = root.selectNodes("//model[@id='" + modelId + "']");
					Element element = null;
					if (null != elements && !elements.isEmpty()) {
						element = (Element) elements.get(0);
					} else {
						element = root.addElement("model");
					}
					// 遍历属性
					Map<String, String> attributeMap = (Map<String, String>) modelMap.get(XMLUpgradeUtils.KEY_ATTRIBUTES);
					if (null != attributeMap && !attributeMap.isEmpty()) {
						String oldOperateType = element.attributeValue("operateType");
						if (null == oldOperateType || oldOperateType.trim().length() == 0) {
							oldOperateType = "ALTER";
						}
						for (Map.Entry<String, String> attributeEntry : attributeMap.entrySet()) {
							String key = attributeEntry.getKey();
							if (oldOperateType.equalsIgnoreCase("CREATE") && (key.equalsIgnoreCase("operateType") || key.equalsIgnoreCase("orgTableName"))) {
								continue;
							}
							Attribute attribute = element.attribute(key);
							if (null == attribute) {
								element = element.addAttribute(key, attributeEntry.getValue());
							} else {
								if (!key.equalsIgnoreCase("orgTableName")) {
									attribute.setValue(attributeEntry.getValue());
								}
							}
						}
					}
					// 遍历Property
					Map<String, Object> propertyMap = (Map<String, Object>) modelMap.get(XMLUpgradeUtils.KEY_PROPERTIES);
					if (null != propertyMap && !propertyMap.isEmpty()) {
						for (Map.Entry<String, Object> propertyEntry : propertyMap.entrySet()) {
							String propertyId = propertyEntry.getKey();
							Map<String, String> propertyAttrMap = (Map<String, String>) propertyEntry.getValue();
							List<Node> propertyElements = element.selectNodes("//model[@id='" + modelId + "']//property[@id='" + propertyId + "']");
							Element propertyElement = null;
							if (null != propertyElements && !propertyElements.isEmpty()) {
								propertyElement = (Element) propertyElements.get(0);
							} else {
								propertyElement = element.addElement("property");
							}
							// 遍历Property属性
							if (null != propertyAttrMap && !propertyAttrMap.isEmpty()) {
								String oldOperateType = propertyElement.attributeValue("operateType");
								if (null == oldOperateType || oldOperateType.trim().length() == 0) {
									oldOperateType = "ALTER";
								}
								for (Map.Entry<String, String> attributeEntry : propertyAttrMap.entrySet()) {
									String key = attributeEntry.getKey();
									if (oldOperateType.equalsIgnoreCase("CREATE")
											&& (key.equalsIgnoreCase("operateType") || key.equalsIgnoreCase("orgColumnName"))) {
										continue;
									}
									Attribute attribute = propertyElement.attribute(key);
									if (null == attribute) {
										propertyElement = propertyElement.addAttribute(key, attributeEntry.getValue());
									} else {
										if (!key.equalsIgnoreCase("orgColumnName")) {
											attribute.setValue(attributeEntry.getValue());
										}
									}
								}
							}
						}
					}

					// 遍历Index
					Map<String, Object> indexMap = (Map<String, Object>) modelMap.get(XMLUpgradeUtils.KEY_INDEXS);
					if (null != indexMap && !indexMap.isEmpty()) {
						for (Map.Entry<String, Object> indexEntry : propertyMap.entrySet()) {
							String indexName = indexEntry.getKey();
							Map<String, String> indexAttrMap = (Map<String, String>) indexEntry.getValue();
							List<Node> indexElements = element.selectNodes("//model[@id='" + modelId + "']//index[@name='" + indexName + "']");
							Element indexElement = null;
							if (null != indexElements && !indexElements.isEmpty()) {
								indexElement = (Element) indexElements.get(0);
							} else {
								indexElement = element.addElement("index");
							}
							// 遍历index属性
							if (null != indexAttrMap && !indexAttrMap.isEmpty()) {
								for (Map.Entry<String, String> attributeEntry : indexAttrMap.entrySet()) {
									Attribute attribute = indexElement.attribute(attributeEntry.getKey());
									if (null == attribute) {
										indexElement = indexElement.addAttribute(attributeEntry.getKey(), attributeEntry.getValue());
									} else {
										attribute.setValue(attributeEntry.getValue());
									}
								}
							}
						}
					}
				}
				OutputFormat format = OutputFormat.createPrettyPrint();
				format.setEncoding("UTF-8");
				XMLWriter writer = new XMLWriter(new FileWriter(targetFile), format);
				writer.write(updateXmlDoc);
				writer.close();
			}

		}
	}

	public static synchronized void removeInternationalFromFile(String key) {
		String modulePerfix = null;
		if (key.indexOf(".") > 0) {
			modulePerfix = key.substring(0, key.indexOf("."));
		}
		if (null!=modulePerfix&&modulePerfix.lastIndexOf("_") > 0) {
			modulePerfix = modulePerfix.substring(0, modulePerfix.indexOf("_"));
		}
		File tempFile = null;
		try {
			if (!OrchidVariable.getModelArtifacts().contains(modulePerfix)) {
				modulePerfix = "foundation";
			}
			List<File> fileList = new ArrayList<File>();
			String filePath = BAP_CUSTOM_L10N_FILE_PATH + "/" + modulePerfix;
			File moduleTempDir = new File(filePath);
			if (moduleTempDir.exists()) {
				File[] files = moduleTempDir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						String fileName = file.getName();
						if (file.isFile() && fileName.matches(L10N_REGEX)) {
							return true;
						}
						return false;
					}
				});
				if (null != files && files.length > 0) {
					for (File file : files) {
						fileList.add(file);
					}
				}
			}
			String devFilePath = BAP_L10N_FILE_PATH + "/" + modulePerfix;
			File moduleCustomDir = new File(devFilePath);
			if (moduleCustomDir.exists()) {
				File[] files = moduleCustomDir.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						String fileName = file.getName();
						if (file.isFile() && fileName.matches(L10N_REGEX)) {
							return true;
						}
						return false;
					}
				});
				if (null != files && files.length > 0) {
					for (File file : files) {
						fileList.add(file);
					}
				}
			}
			if (!fileList.isEmpty()) {
				for (File file : fileList) {
					Properties properties = new OrderProperties();
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
					properties.load(reader);
					OutputStream out = new FileOutputStream(file);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
					properties.remove(key);
					properties.store(writer, "update Properties at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
					out.close();
					reader.close();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static synchronized void updateAllInternationalFile(Map<String, List<International>> map, String modelArtifact){
		if(null != map && !map.isEmpty()){
			for(Map.Entry<String, List<International>> entry : map.entrySet()){
				String languageKey = entry.getKey();
				List<International> list = entry.getValue();
				if(null != list && !list.isEmpty()){
					Map<String, String> map2 = new LinkedHashMap<String, String>();
					for(International international : list){
						map2.put(international.getKey(), international.getValue());
					}
					try {
						OrchidVariable.addModelArtifact(modelArtifact);
						List<File> files = new ArrayList<File>();
						if(PropertyHolder.isProduct()){
							String filePath = BAP_CUSTOM_L10N_FILE_PATH + "/" + modelArtifact;
							File moduleTempDir = new File(filePath);
							if (!moduleTempDir.exists()) {
								moduleTempDir.mkdirs();
							}
							File tempFile = null;
							if (moduleTempDir.isDirectory()) {
								tempFile = new File(filePath + File.separator + "package_" + languageKey + ".properties");
								if (!tempFile.exists()) {
									tempFile.createNewFile();
								}
								files.add(tempFile);
							}
						} else {
							String devFilePath = BAP_L10N_FILE_PATH + "/" + modelArtifact;
							File moduleCustomDir = new File(devFilePath);
							if (!moduleCustomDir.exists()) {
								moduleCustomDir.mkdirs();
							}
							if (moduleCustomDir.isDirectory()) {
								File customFile = new File(devFilePath + File.separator + "package_" + languageKey + ".properties");
								if (!customFile.exists()) {
									customFile.createNewFile();
								}
								files.add(customFile);
							}
						}
						if (!files.isEmpty()) {
							for (File file : files) {
								Properties properties = new OrderProperties();
								BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
								properties.load(reader);
								OutputStream out = new FileOutputStream(file);
								BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
								properties.putAll(map2);
								properties.store(writer, "update Properties at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
								out.close();
								reader.close();
							}
						}
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		}
	}
	
	/**
	 * 合并国际化文件
	 * 
	 * @param srcFile
	 * @param targetFile
	 */
	public static void mergeInternationalFiles(File srcFile, File targetFile) {
		if (null != srcFile && srcFile.exists()) {
			try {
				if (!targetFile.exists()) {
					targetFile.getParentFile().mkdirs();
					copy(srcFile, targetFile.getAbsolutePath());
				} else {
					Properties properties = new OrderProperties();
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(srcFile), "UTF-8"));
					properties.load(reader);

					Properties targetProperties = new OrderProperties();
					BufferedReader targetReader = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), "UTF-8"));
					targetProperties.load(targetReader);
					OutputStream out = new FileOutputStream(targetFile);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
					targetProperties.putAll(properties);
					targetProperties.store(writer, "update Properties at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
					out.close();
					reader.close();
				}

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	public static boolean deleteDirectory(File file) {
		boolean flag = true;
		if (file.exists()){
			if(file.isDirectory()) {
				File[] files = file.listFiles();
				if (null != files && files.length > 0) {
					for (File f : files) {
						deleteDirectory(f);
					}
				}
			}
			file.delete();
			logger.info("=====删除文件路径:" + file.getAbsolutePath());
		}
		return flag;
	}

	/**
	 * 查找root目录下所有符合正则regex的文件
	 * 
	 * @param root
	 *            根目录
	 * @param files
	 *            不能为空，返回的文件列表放入该变量中
	 * @param regex
	 *            正则
	 */
	public static void listDirectory(File root, List<File> files, final String regex) {
		if (null == files) {
			return;
		}
		if (!root.exists()) {
			logger.info("文件名称不存在!" + root);
		} else {
			File[] files2 = root.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					String fileName = file.getName();
					if (file.isDirectory() || (file.isFile() && fileName.matches(regex))) {
						return true;
					}
					return false;
				}
			});
			if(files2 != null){
			for (File file : files2) {
				if (file.isDirectory()) {
					listDirectory(file, files, regex);
				} else {
					files.add(file);
				}
			}
			}
		}
	}

	/**
	 * 更新系统编码临时文件
	 * @param module
	 * @param object
	 */
	public static synchronized void updateSystemCodeXml(Module module, Object object) {
		if(object instanceof SystemEntity || object instanceof SystemCode){
			File tempDir = createNewTempDir(module);
			if(null != tempDir){
				try {
					File tempSystemCodeFile = new File(tempDir.getAbsolutePath() + "/" + TEMP_SYSTEM_CODE_XML_NAME);
					if(!tempSystemCodeFile.exists()){
						Document document = DocumentHelper.createDocument();
						document.addElement("bapSystemCode");
						OutputFormat format = OutputFormat.createPrettyPrint();
						format.setEncoding("UTF-8");
						XMLWriter writer = new XMLWriter(new FileWriter(tempSystemCodeFile), format);
						writer.write(document);
						writer.close();
					}
					SAXReader reader = new SAXReader();
					Document xmlDoc = reader.read(tempSystemCodeFile);
					Element root = xmlDoc.getRootElement();
					if(object instanceof SystemEntity){
						SystemEntity systemEntity = (SystemEntity) object;
						List<Node> elements = root.selectNodes("//systementity[@code='" + systemEntity.getCode() + "']");
						Element systementityElement = null;
						if(null == elements || elements.isEmpty()){
							systementityElement = root.addElement("systementity");
							systementityElement.addAttribute("code", translateToString(systemEntity.getCode()));
							systementityElement.addAttribute("moduleCode", translateToString(systemEntity.getModuleCode()));
							systementityElement.addAttribute("name", translateToString(systemEntity.getName()));
							systementityElement.addAttribute("sysDefault", translateToString(systemEntity.isSysDefault()));
							systementityElement.addAttribute("listType", translateToString(systemEntity.getListType()));
							systementityElement.addAttribute("multiFlag", translateToString(systemEntity.isMultiFlag()));
							systementityElement.addAttribute("memo", translateToString(systemEntity.getMemo()));
							systementityElement.addAttribute("valid", translateToString(systemEntity.isValid()));
							systementityElement.addAttribute("version", translateToString(systemEntity.getVersion()));
						} else {
							systementityElement = (Element) elements.get(0);
							String code = translateToString(systemEntity.getCode());
							if (null == systementityElement.attribute("code") || systementityElement.attributeValue("code").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("code", code);
							} else {
								Attribute attribute = systementityElement.attribute("code");
								attribute.setValue(code);
							}
							String moduleCode = translateToString(systemEntity.getModuleCode());
							if (null == systementityElement.attribute("moduleCode") || systementityElement.attributeValue("moduleCode").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("code", code);
							} else {
								Attribute attribute = systementityElement.attribute("moduleCode");
								attribute.setValue(moduleCode);
							}
							String name = translateToString(systemEntity.getName());
							if (null == systementityElement.attribute("name") || systementityElement.attributeValue("name").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("name", code);
							} else {
								Attribute attribute = systementityElement.attribute("name");
								attribute.setValue(name);
							}
							String sysDefault = translateToString(systemEntity.isSysDefault());
							if (null == systementityElement.attribute("sysDefault") || systementityElement.attributeValue("sysDefault").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("sysDefault", sysDefault);
							} else {
								Attribute attribute = systementityElement.attribute("sysDefault");
								attribute.setValue(sysDefault);
							}
							
							String listType = translateToString(systemEntity.getType());
							if (null == systementityElement.attribute("listType") || systementityElement.attributeValue("listType").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("listType", listType);
							} else {
								Attribute attribute = systementityElement.attribute("listType");
								attribute.setValue(listType);
							}
							
							String multiFlag = translateToString(systemEntity.isMultiFlag());
							if (null == systementityElement.attribute("multiFlag") || systementityElement.attributeValue("multiFlag").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("multiFlag", multiFlag);
							} else {
								Attribute attribute = systementityElement.attribute("multiFlag");
								attribute.setValue(multiFlag);
							}
							
							String memo = translateToString(systemEntity.getMemo());
							if (null == systementityElement.attribute("memo") || systementityElement.attributeValue("memo").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("memo", memo);
							} else {
								Attribute attribute = systementityElement.attribute("memo");
								attribute.setValue(memo);
							}
							
							String valid = translateToString(systemEntity.isValid());
							if (null == systementityElement.attribute("valid") || systementityElement.attributeValue("valid").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("valid", valid);
							} else {
								Attribute attribute = systementityElement.attribute("valid");
								attribute.setValue(valid);
							}
							
							String version = translateToString(systemEntity.getVersion());
							if (null == systementityElement.attribute("version") || systementityElement.attributeValue("version").trim().length() == 0) {
								systementityElement = systementityElement.addAttribute("version", version);
							} else {
								Attribute attribute = systementityElement.attribute("version");
								attribute.setValue(version);
							}
						}
					} else if(object instanceof SystemCode){
						SystemCode systemCode = (SystemCode) object;
						List<Node> systementityElements = root.selectNodes("//systementity[@code='" + systemCode.getEntityCode() + "']");
						Element systementityElement = null;
						if(null == systementityElements || systementityElements.isEmpty()){
							systementityElement = root.addElement("systementity");
							systementityElement.addAttribute("code", translateToString(systemCode.getEntityCode()));
							systementityElement.addAttribute("moduleCode", translateToString(module.getCode()));
							systementityElement.addAttribute("version", "0");
							systementityElement.addAttribute("valid", "true");
							systementityElement.addAttribute("sysDefault", "false");
						} else {
							systementityElement = (Element) systementityElements.get(0);
						}
						List<Node> elements = systementityElement.selectNodes("//systemcode[@id='" + systemCode.getUniqueCode() + "']");
						Element element = null;
						String id = translateToString(systemCode.getUniqueCode());
						String code = translateToString(systemCode.getCode());
						String value = translateToString(systemCode.getValue());
//						String attribute = translateToString(systemCode.isAttribute());
						String memo = translateToString(systemCode.getMemo());
						String sort = translateToString(systemCode.getSort());
						String valid = translateToString(systemCode.isValid());
						String version = translateToString(systemCode.getVersion());
						String parentId = translateToString(systemCode.getParentId());
						String leaf = translateToString(systemCode.getLeaf());
						String defaultFlag = translateToString(systemCode.getDefaultFlag());
						String layNo = "0";
						if (systemCode.getLayRec() != null && !systemCode.getLayRec().trim().equals("")) {
							layNo = translateToString(systemCode.getLayRec().split("-").length);
						} else {
							layNo = translateToString(0);
						}
						String layRec = translateToString(systemCode.getLayRec());
						String seqId = translateToString(systemCode.getSeqId());
//						String fullPathName = translateToString(systemCode.getFullPathName());
						if (null == elements || elements.isEmpty()) {
							element = systementityElement.addElement("systemcode");
							element.addAttribute("id", id);
							element.addAttribute("code", code);
							element.addAttribute("value", value);
//							element.addAttribute("attribute", attribute);
							element.addAttribute("memo", memo);
							element.addAttribute("sort", sort);
							element.addAttribute("valid", StringUtils.isEmpty(valid) ? String.valueOf(1) : valid);
							element.addAttribute("version", StringUtils.isEmpty(version) ? String.valueOf(1) : version);
							element.addAttribute("parentId", parentId);
							element.addAttribute("leaf", leaf);
							element.addAttribute("defaultFlag", defaultFlag);
							element.addAttribute("layNo", layNo);
							element.addAttribute("layRec", layRec);
							element.addAttribute("seqId", seqId);
//							element.addAttribute("fullPathName", fullPathName);
						} else {
							element = (Element) elements.get(0);
							if (null == element.attribute("id") || element.attributeValue("id").trim().length() == 0) {
								element = element.addAttribute("id", id);
							} else {
								Attribute attr = element.attribute("id");
								attr.setValue(id);
							}
							if (null == element.attribute("code") || element.attributeValue("code").trim().length() == 0) {
								element = element.addAttribute("code", code);
							} else {
								Attribute attr = element.attribute("code");
								attr.setValue(code);
							}
							if (null == element.attribute("value") || element.attributeValue("value").trim().length() == 0) {
								element = element.addAttribute("value", value);
							} else {
								Attribute attr = element.attribute("value");
								attr.setValue(value);
							}
//							if (null == element.attribute("attribute") || element.attributeValue("attribute").trim().length() == 0) {
//								element = element.addAttribute("attribute", attribute);
//							} else {
//								Attribute attr = element.attribute("attribute");
//								attr.setValue(attribute);
//							}
							if (null == element.attribute("memo") || element.attributeValue("memo").trim().length() == 0) {
								element = element.addAttribute("memo", memo);
							} else {
								Attribute attr = element.attribute("memo");
								attr.setValue(memo);
							}
							if (null == element.attribute("sort") || element.attributeValue("sort").trim().length() == 0) {
								element = element.addAttribute("sort", sort);
							} else {
								Attribute attr = element.attribute("sort");
								attr.setValue(sort);
							}
							if (null == element.attribute("valid") || element.attributeValue("valid").trim().length() == 0) {
								element = element.addAttribute("valid", valid);
							} else {
								Attribute attr = element.attribute("valid");
								attr.setValue(valid);
							}
							if (null == element.attribute("version") || element.attributeValue("version").trim().length() == 0) {
								element = element.addAttribute("version", version);
							} else {
								Attribute attr = element.attribute("version");
								attr.setValue(version);
							}
							if (null == element.attribute("parentId") || element.attributeValue("parentId").trim().length() == 0) {
								element = element.addAttribute("parentId", parentId);
							} else {
								Attribute attr = element.attribute("parentId");
								attr.setValue(parentId);
							}
							if (null == element.attribute("leaf") || element.attributeValue("leaf").trim().length() == 0) {
								element = element.addAttribute("leaf", leaf);
							} else {
								Attribute attr = element.attribute("leaf");
								attr.setValue(leaf);
							}
							if (null == element.attribute("defaultFlag") || element.attributeValue("defaultFlag").trim().length() == 0) {
								element = element.addAttribute("defaultFlag", defaultFlag);
							} else {
								Attribute attr = element.attribute("defaultFlag");
								attr.setValue(defaultFlag);
							}
							if (null == element.attribute("layNo") || element.attributeValue("layNo").trim().length() == 0) {
								element = element.addAttribute("layNo", layNo);
							} else {
								Attribute attr = element.attribute("layNo");
								attr.setValue(layNo);
							}
							if (null == element.attribute("layRec") || element.attributeValue("layRec").trim().length() == 0) {
								element = element.addAttribute("layRec", layRec);
							} else {
								Attribute attr = element.attribute("layRec");
								attr.setValue(layRec);
							}
							if (null == element.attribute("seqId") || element.attributeValue("seqId").trim().length() == 0) {
								element = element.addAttribute("seqId", seqId);
							} else {
								Attribute attr = element.attribute("seqId");
								attr.setValue(seqId);
							}
//							if (null == element.attribute("fullPathName") || element.attributeValue("fullPathName").trim().length() == 0) {
//								element = element.addAttribute("fullPathName", code);
//							} else {
//								Attribute attr = element.attribute("fullPathName");
//								attr.setValue(fullPathName);
//							}
						}
					}
					OutputFormat format = OutputFormat.createPrettyPrint();
					format.setEncoding("UTF-8");
					XMLWriter writer = new XMLWriter(new FileWriter(tempSystemCodeFile), format);
					writer.write(xmlDoc);
					writer.close();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	
	
	private static String translateToString(Object object) {
		return (object == null) ? "" : String.valueOf(object);
	}

	private static final int BUFFER_SIZE = 16 * 1024;

	public static void copy(File src, String fileName) {
		try {
			// 创建文件夹
			makeDirectory(fileName);

			// 创建文件
			File dst = new File(fileName);
			if (!isFileExist(fileName)) {
				dst.createNewFile();
			}

			InputStream in = null;
			OutputStream out = null;
			try {
				in = new BufferedInputStream(new FileInputStream(src), BUFFER_SIZE);
				out = new BufferedOutputStream(new FileOutputStream(dst), BUFFER_SIZE);
				byte[] buffer = new byte[BUFFER_SIZE];

				int len = 0;
				while ((len = in.read(buffer)) > 0) {
					out.write(buffer, 0, len);
				}
			} finally {
				if (null != in) {
					in.close();
				}
				if (null != out) {
					out.close();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isFileExist(String fileName) {
		return new File(fileName).isFile();
	}

	public static boolean makeDirectory(String fileName) {
		File file = new File(fileName);
		File parent = file.getParentFile();
		if (parent != null) {
			return parent.mkdirs();
		}
		return false;
	}

}
