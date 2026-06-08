package com.supcon.supfusion.configuration.services.service.impl;

import com.supcon.supfusion.configuration.services.utils.DbUtils;
import com.supcon.supfusion.base.entities.Department;
import com.supcon.supfusion.base.services.BaseServiceImpl;
import com.supcon.supfusion.configuration.services.entity.Model;
import com.supcon.supfusion.configuration.services.entity.Property;
import com.supcon.supfusion.configuration.services.entity.*;
import com.supcon.supfusion.configuration.services.enums.AdvDateType;
import com.supcon.supfusion.configuration.services.enums.DbColumnType;
import com.supcon.supfusion.configuration.services.utils.ConditionUtil;
import com.supcon.supfusion.configuration.services.utils.DateUtil;
import com.supcon.supfusion.base.entities.Position;
import com.supcon.supfusion.base.entities.Staff;
import com.supcon.supfusion.base.services.DepartmentService;
import com.supcon.supfusion.base.services.PositionService;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.utils.Constants;
import com.supcon.supfusion.configuration.services.utils.ValidateUtils;
import com.supcon.supfusion.configuration.services.dao.AdvQueryConditionImpl;
import com.supcon.supfusion.configuration.services.dao.AdvQueryConditionItemDaoImpl;
import com.supcon.supfusion.configuration.services.dao.DefaultAdvCondDaoImpl;
import com.supcon.supfusion.configuration.services.service.ConditionService;
import com.supcon.supfusion.configuration.services.service.ModelService;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import flexjson.JSONException;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;

/**
 * 条件实体转化为SQL类
 * 
 * @author 谭正阳
 * 
 */
@ServiceApiService
@Transactional
public class ConditionServiceImpl extends BaseServiceImpl implements ConditionService {

	private static final String TO_REPLACE = "$__replaceME__$";

	@Autowired
	private AdvQueryConditionImpl conditionDao;
	@Autowired
	private DefaultAdvCondDaoImpl defAdvCondDao;

	@Autowired
	private AdvQueryConditionItemDaoImpl conditionItemDao;

	@Autowired
	private DepartmentService departmentService;
	
	@Autowired
	private PositionService positionService;

	@Autowired
	private ModelService modelService;

	@Override
	public AdvQueryCondition toSql(String advQueryCond, Boolean... existsParam) {
		if (advQueryCond == null || advQueryCond.isEmpty()) {
			return null;
		}
		String json = advQueryCond;
		AdvQueryCondition cond = null;
		try {
			cond = ConditionUtil.generateAdvQueryConditionFromJson(json);
			toSql(cond, existsParam);
		} catch (JSONException e) {
			throw new EcException(EcException.Code.ADVQUERYCONDITION_TYPE_ERROR);
		}
		// conditionDao.test(cond.getSql(), values);
		return cond;
	}

	public AdvQueryCondition toSql(AdvQueryCondition cond, Boolean... existsParam) {
		if (existsParam == null || existsParam.length == 0) {
			existsParam = new Boolean[] { Boolean.FALSE };
		}
		List<Object> values = new ArrayList<Object>();

		List<String> exps = new ArrayList<String>();
		StringBuilder retExp = new StringBuilder();
		List<AdvQueryConditionItem> conds = cond.getSubconds();
		if (conds != null && conds.size() > 0) {
			for (AdvQueryConditionItem item : conds) {
				if (cond.getModelAlias() != null && cond.getModelAlias().length() > 0) {
					item.setModelAlias(Constants.SYMBOL_DOUBLE_QUOTE + cond.getModelAlias() + Constants.SYMBOL_DOUBLE_QUOTE
							+ Constants.SYMBOL_HALF_POINT);
				}
				item.setCondition(cond);
				tranArg(item, exps, values, existsParam[0]);
			}
		}
		if (exps != null && exps.size() > 0) {
			for (String exp : exps) {
				if (retExp.length() > 0) {
					retExp.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SQL_KEYWORDS_AND).append(Constants.SYMBOL_HALF_BLANK);
				}
				retExp.append(exp);
			}
		}
		if ((cond.getSubconds() == null || cond.getSubconds().isEmpty()) && retExp.length() == 0) {
			return new AdvQueryCondition();
		} else {
			if (retExp.length() > 0) {
				cond.setSql(Constants.SYMBOL_LEFT_BRACKET + retExp.toString() + Constants.SYMBOL_RIGHT_BRACKET);
				cond.setValues(values);
			}
		}
		return cond;
	}
	public final void tranArg(AdvQueryConditionItem item, List<String> exps, List<Object> values, Boolean existsParam) {
		if (item == null) {
			return;
		}
		if (item.getValue().indexOf("SYMBOL_DOUBLE_QUOTE") != -1) {
			item.setValue(item.getValue().replaceAll("SYMBOL_DOUBLE_QUOTE", Constants.SYMBOL_DOUBLE_QUOTE));
		}

		if ("0".equals(item.getType())) { // 简单表字段条件
			String operator = item.getOperator();
			if (operator != null && (operator.toUpperCase().contains("INCLUDE") || operator.toUpperCase().contains("CURRENT"))) {
				specialArg(item, exps, values);
			} else {
				if (existsParam) {
					objectArg(item, exps, values);
				} else {
					switch (item.getDbColumnType()) {
						case DATE:
						case DATETIME:
							dateArg(item, exps, values);
							break;
						case TEXT:
						case LONGTEXT:
						case BAPCODE:
						case SUMMARY:
							stringArg(item, exps, values);
							break;
						case BINARY:
							byteArg(item, exps, values);
							break;
						case DECIMAL:
							floatArg(item, exps, values);
							break;
						case INTEGER:
						case LONG:
							intArg(item, exps, values);
							break;
						case ENUMERATE:
						case SYSTEMCODE:
							enumArg(item, exps, values);
							break;
						default:
							commonArg(item, exps, values);
					}
				}
			}
		} else if ("1".equals(item.getType())) { // 逻辑运算
			List<AdvQueryConditionItem> conds = item.getSubconds();
			if (conds != null && conds.size() > 0) {
				if (item.getModelAlias() != null && item.getModelAlias().length() > 0) {
					fillSubItemModelAlias(item);
				}

				List<String> expsTmp = new ArrayList<String>();
				for (AdvQueryConditionItem cond : conds) {
					cond.setParent(item);
					tranArg(cond, expsTmp, values, existsParam);
				}
				String joinResult = joinExps(expsTmp, item.getLogic());
				if (joinResult != null && !joinResult.isEmpty()) {
					exps.add(joinResult);
				}
			}
		} else if ("2".equals(item.getType())) { // 关连表
			String subSql = getSubSelect(item, values, existsParam);
			if (subSql != null && !subSql.isEmpty()) {
				exps.add(subSql);
			}
		} else if ("4".equals(item.getType())) { // 参照，通过ID查询
			if (item.getOperator().equals(Constants.NULL) || item.getOperator().equals(Constants.NOT_NULL)) {
				commonArg(item, exps, values);
			} else {
				String subSql = getReferenceSelect(item, values, item.getType(), existsParam);
				if (subSql != null && !subSql.isEmpty()) {
					exps.add(subSql);
				}
			}
		} else if ("5".equals(item.getType())) { // 参照，用户可以选择，也可以通过名称进行模糊查询，目前中针对部门与人员
			if (item.getOperator().equals(Constants.NULL) || item.getOperator().equals(Constants.NOT_NULL)) {
				commonArg(item, exps, values);
			} else {
				String subSql = getReferenceSelect(item, values, item.getType(), existsParam);
				if (subSql != null && !subSql.isEmpty()) {
					exps.add(subSql);
				}
			}
		}
	}
	public void specialArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if ((null != item && null != item.getOperator() && !item.getOperator().contains("current"))
				&& (item == null || item.getValue() == null || item.getValue().length() == 0)) {
			return;
		}
		if(item != null){
			String operator = item.getOperator();
			Object value = null;
			if (null != item.getValue() && item.getValue().length() > 0) {
				if ("ID".equals(item.getColumnName().toUpperCase())) {
					value = Long.valueOf(item.getValue());
				} else {
					value = "%" + item.getValue() + "%";
				}
			}
			String paramStr = null;
			if ((value != null && !"".equals(value)) || operator.contains("current")) {
				boolean flag = true;
				String PrimaryKey="ID";
				if(operator.startsWith("=includeCustSub#")){
					String tableName=operator.split("#")[1];
					String lay_rec = "LAY_REC";
					Model model = modelService.getModelWithProperties(tableName);
					Set<Property> properties = model.getProperties();
					for (Iterator<Property> iter = properties.iterator(); iter.hasNext();) {
						Property property = iter.next();
						if(Boolean.TRUE.equals(property.getIsPk())){
							PrimaryKey = property.getColumnName();
						}
						if("layRec".equals(property.getName())){
							lay_rec = property.getColumnName();
						}
					}
					String dbName = DbUtils.getDbName();
					if (dbName.startsWith("sqlserver")) {
						if ("ID".equals(item.getColumnName().toUpperCase())) {
							paramStr="(SELECT b."+PrimaryKey+" FROM "+tableName+" a,"+tableName+" b WHERE (b."+lay_rec+" LIKE (a."+lay_rec+" + '-%') or b."+PrimaryKey+"=a."+PrimaryKey+") and a."+PrimaryKey+" = ?)";
						}else{
							paramStr="(SELECT b."+PrimaryKey+" FROM "+tableName+" a,"+tableName+" b WHERE (b."+lay_rec+" LIKE (a."+lay_rec+" + '-%') or b."+PrimaryKey+"=a."+PrimaryKey+") and a."+item.getColumnName()+" LIKE ?)";
						}
					} else {
						if ("ID".equals(item.getColumnName().toUpperCase())) {
							paramStr="(SELECT b."+PrimaryKey+" FROM "+tableName+" a,"+tableName+" b WHERE (b."+lay_rec+" LIKE CONCAT(a."+lay_rec+",'-%') or b."+PrimaryKey+"=a."+PrimaryKey+") and a."+PrimaryKey+" = ?)";
						}else{
							paramStr="(SELECT b."+PrimaryKey+" FROM "+tableName+" a,"+tableName+" b WHERE (b."+lay_rec+" LIKE CONCAT(a."+lay_rec+",'-%') or b."+PrimaryKey+"=a."+PrimaryKey+") and a."+item.getColumnName()+" LIKE ?)";
						}
					}

					operator="=";
				}else{
					if ("=includeSubDept".equals(operator) || "<>includeSubDept".equals(operator)) {
						if ("ID".equals(item.getColumnName().toUpperCase())) {
							Department dept = departmentService.load(Long.valueOf(item.getValue()));
							paramStr = "(SELECT ID FROM BASE_DEPARTMENT P WHERE P.ID = ? OR P.LAY_REC LIKE '" + dept.getLayRec() + "-%')";
							value = Long.valueOf(item.getValue());
						} else {
							List<String> deptLayrec = departmentService.getDepartmentChildren(item.getValue(), getCurrentCompanyId());
							paramStr = "(SELECT ID FROM BASE_DEPARTMENT P WHERE P.NAME like ? ";
							for (String tmp : deptLayrec) {
								paramStr += " OR P.LAY_REC LIKE '" + tmp + "-%'";
							}
							paramStr += ")";
						}
						operator = operator.substring(0, operator.indexOf("includeSubDept"));
					} else if ("=includeSubPos".equals(operator) || "<>includeSubPos".equals(operator)) {
						Position pos = positionService.load(Long.valueOf(item.getValue()));
						paramStr = "(SELECT ID FROM BASE_POSITION P WHERE P.ID = ? OR P.LAY_REC LIKE '" + pos.getLayRec() + "-%')";
						operator = operator.substring(0, operator.indexOf("includeSubPos"));
					} else if ("=currentUser".equals(operator) || "<>currentUser".equals(operator)) {
						Staff staff = getCurrentStaff();
						flag = false;
						if (null != staff) {
							// paramStr = "SELECT ID FROM BASE_STAFF S WHERE S.ID=" + staff.getId();
							paramStr = String.valueOf(staff.getId());
							operator = operator.substring(0, operator.indexOf("current"));
						}
					} else if ("=currentPos".equals(operator) || "<>currentPos".equals(operator)) {
						Long mainPosId = getCurrentStaff().getMainPosition().getId();
						flag = false;
						if (null != mainPosId) {
							// paramStr = "SELECT ID FROM BASE_POSITION P WHERE P.ID=" + mainPosId;
							paramStr = String.valueOf(mainPosId);
							operator = operator.substring(0, operator.indexOf("current"));
						}
					} else if ("=currentDept".equals(operator) || "<>currentDept".equals(operator)) {
						Department department = (Department) getCurrentStaff().getMainPosition().getDepartment();
						flag = false;
						if (null != department) {
							// paramStr = "SELECT ID FROM BASE_DEPARTMENT P WHERE P.ID=" + department.getId();
							paramStr = String.valueOf(department.getId());
							operator = operator.substring(0, operator.indexOf("current"));
						}
					} else {
						operator = operator.substring(0, operator.indexOf("notInclude"));
						paramStr = item.getParamStr();
					}
				}
				if (Constants.EQ.equals(operator)) {
					operator = Constants.SQL_KEYWORDS_IN;
				} else {
					operator = Constants.SQL_KEYWORDS_NOT + Constants.SYMBOL_HALF_BLANK + Constants.SQL_KEYWORDS_IN;
				}
				if("ID".equals(item.getColumnName().toUpperCase())){
					retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + "ID").append(Constants.SYMBOL_HALF_BLANK)
							.append(operator);
				}else{
					retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + PrimaryKey).append(Constants.SYMBOL_HALF_BLANK)
							.append(operator);
				}

				retSql.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SYMBOL_LEFT_BRACKET).append(paramStr)
						.append(Constants.SYMBOL_RIGHT_BRACKET);
				if (flag && value != null) {
					values.add(value);
				}
				if (null != retSql && retSql.length() > 0) {
					exps.add(retSql.toString());
				}
			}}
	}

	public void objectArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if (item == null) {
			return;
		}

		retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
				.append(item.getOperator());
		if (Constants.NULL.equals(item.getOperator()) || Constants.NOT_NULL.equals(item.getOperator())) {
			// 若查询条件为判断字段是否为空，没有添加值
			exps.add(retSql.toString());
		} else if (Constants.ANY_TIME.equals(item.getOperator()) || Constants.YESTERDAY.equals(item.getOperator())
				|| Constants.TODAY.equals(item.getOperator()) || Constants.TOMORROW.equals(item.getOperator())
				|| Constants.NEXT_SEVENDAYS.equals(item.getOperator()) || Constants.LAST_SEVENDAYS.equals(item.getOperator())
				|| Constants.NEXT_WEEK.equals(item.getOperator()) || Constants.LAST_WEEK.equals(item.getOperator())
				|| Constants.THIS_WEEK.equals(item.getOperator()) || Constants.NEXT_MONTH.equals(item.getOperator())
				|| Constants.LAST_MONTH.equals(item.getOperator()) || Constants.THIS_MONTH.equals(item.getOperator())
				|| Constants.NEXT_YEAR.equals(item.getOperator()) || Constants.LAST_YEAR.equals(item.getOperator())
				|| Constants.THIS_YEAR.equals(item.getOperator()) || Constants.LAST_X_HOUR.equals(item.getOperator())
				|| Constants.NEXT_X_HOUR.equals(item.getOperator()) || Constants.LAST_X_DAY.equals(item.getOperator())
				|| Constants.NEXT_X_DAY.equals(item.getOperator()) || Constants.LAST_X_WEEK.equals(item.getOperator())
				|| Constants.NEXT_X_WEEK.equals(item.getOperator()) || Constants.LAST_X_MONTH.equals(item.getOperator())
				|| Constants.NEXT_X_MONTH.equals(item.getOperator()) || Constants.LAST_X_YEAR.equals(item.getOperator())
				|| Constants.NEXT_X_YEAR.equals(item.getOperator()) || Constants.BEFORE_NOW.equals(item.getOperator())
				|| Constants.AFTER_NOW.equals(item.getOperator())) {
			dateArg(item, exps, values);
		} else {
			retSql.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SYMBOL_DOUBLE_QUOTE).append(item.getValue())
					.append(Constants.SYMBOL_DOUBLE_QUOTE);
			if (item.getValue() != null && item.getValue().length() > 0) {
				exps.add(retSql.toString());
			}
		}
	}

	public void dateArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if (item == null) {
			return;
		}
		String operator = item.getOperator();
		if (null != operator) {
			if (operator.equals(Constants.ANY_TIME)) {
				return;
			} else {
				if (operator.equals(Constants.DATE_AFTER) || operator.equals(Constants.DATE_BEFORE)
						|| operator.equals(Constants.DATE_ON_OR_AFTER) || operator.equals(Constants.DATE_ON_OR_BEFORE)) {
					retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName())
							.append(Constants.SYMBOL_HALF_BLANK).append(operator).append(Constants.SYMBOL_QUESTION);
					if (item.getValue() != null && item.getValue().length() > 0) {
						values.addAll(DateUtil.getDateList(item.getValue(), operator));
					} else {
						// values.addAll(AdvDateType.THIS_TIME.getFormatDate(0));
						return;
					}
				} else if (operator.equals(Constants.NULL) || operator.equals(Constants.NOT_NULL)) {
					// 若查询条件为判断字段是否为空，没有添加值
					retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName())
							.append(Constants.SYMBOL_HALF_BLANK).append(operator).append(Constants.SYMBOL_HALF_BLANK);
					// exps.add(retSql.toString());
				} else if (operator.equals(Constants.X_MONTH_BEFORE)) {
					int initalSize = values.size();
					if (item.getValue() != null && !item.getValue().isEmpty()) {
						if (ValidateUtils.isInt(item.getValue())) {
							values.addAll(AdvDateType.X_MONTH_BEFORE.getFormatDate(Integer.parseInt(item.getValue())));
						}
					}
					if (initalSize < values.size()) {
						retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName())
								.append(Constants.SYMBOL_HALF_BLANK).append(Constants.NUM_LE).append(Constants.SYMBOL_QUESTION)
								.append(Constants.SYMBOL_HALF_BLANK);
					} else {
						retSql.append(" 1=1");
					}
				}else if(operator.equals(Constants.BEFORE_NOW)||operator.equals(Constants.AFTER_NOW)){
					int initalSize = values.size();
					String op="";
					if (operator.equals(Constants.BEFORE_NOW)) {
						values.addAll(AdvDateType.THIS_TIME.getFormatDate(0));
						op = Constants.NUM_LE;
					} else if (operator.equals(Constants.AFTER_NOW)) {
						values.addAll(AdvDateType.THIS_TIME.getFormatDate(0));
						op = Constants.NUM_GE;
					}
					if (initalSize < values.size()) {
						retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName())
								.append(Constants.SYMBOL_HALF_BLANK).append(op).append(Constants.SYMBOL_QUESTION)
								.append(Constants.SYMBOL_HALF_BLANK);
					} else {
						retSql.append(" 1=1");
					}
				} else {
					int initalSize = values.size();
					if (operator.equals(Constants.YESTERDAY)) {
						values.addAll(AdvDateType.YESTERDAY.getFormatDate(0));
					} else if (operator.equals(Constants.TODAY)) {
						values.addAll(AdvDateType.TODAY.getFormatDate(0));
					} else if (operator.equals(Constants.TOMORROW)) {
						values.addAll(AdvDateType.TOMORROW.getFormatDate(0));
					} else if (operator.equals(Constants.NEXT_SEVENDAYS)) {
						values.addAll(AdvDateType.NEXT_SEVEN_DAYS.getFormatDate(0));
					} else if (operator.equals(Constants.LAST_SEVENDAYS)) {
						values.addAll(AdvDateType.LAST_SEVEN_DAYS.getFormatDate(0));
					} else if (operator.equals(Constants.NEXT_WEEK)) {
						values.addAll(AdvDateType.NEXT_WEEK.getFormatDate(0));
					} else if (operator.equals(Constants.LAST_WEEK)) {
						values.addAll(AdvDateType.LAST_WEEK.getFormatDate(0));
					} else if (operator.equals(Constants.THIS_WEEK)) {
						values.addAll(AdvDateType.THIS_WEEK.getFormatDate(0));
					} else if (operator.equals(Constants.NEXT_MONTH)) {
						values.addAll(AdvDateType.NEXT_MONTH.getFormatDate(0));
					} else if (operator.equals(Constants.LAST_MONTH)) {
						values.addAll(AdvDateType.LAST_MONTH.getFormatDate(0));
					} else if (operator.equals(Constants.THIS_MONTH)) {
						values.addAll(AdvDateType.THIS_MONTH.getFormatDate(0));
					} else if (operator.equals(Constants.NEXT_YEAR)) {
						values.addAll(AdvDateType.NEXT_YEAR.getFormatDate(0));
					} else if (operator.equals(Constants.LAST_YEAR)) {
						values.addAll(AdvDateType.LAST_YEAR.getFormatDate(0));
					} else if (operator.equals(Constants.THIS_YEAR)) {
						values.addAll(AdvDateType.THIS_YEAR.getFormatDate(0));
					} else if (item.getValue() != null && !item.getValue().isEmpty()) {
						if (operator.equals(Constants.LAST_X_HOUR)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.LAST_X_HOURS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.NEXT_X_HOUR)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.NEXT_X_HOURS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.LAST_X_DAY)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.LAST_X_DAYS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.NEXT_X_DAY)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.NEXT_X_DAYS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.LAST_X_WEEK)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.LAST_X_WEEKS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.NEXT_X_WEEK)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.NEXT_X_WEEKS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.LAST_X_MONTH)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.LAST_X_MONTHS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.NEXT_X_MONTH)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.NEXT_X_MONTHS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.LAST_X_YEAR)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.LAST_X_YEARS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.NEXT_X_YEAR)) {
							if (ValidateUtils.isInt(item.getValue())) {
								values.addAll(AdvDateType.NEXT_X_YEARS.getFormatDate(Integer.parseInt(item.getValue())));
							}
						} else if (operator.equals(Constants.DATE_ON) && item.getValue() != null && item.getValue().length() > 0) {
							values.addAll(DateUtil.getDateList(item.getValue(), operator));
						}
					}
					if (initalSize < values.size()) {
						retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName())
								.append(Constants.SYMBOL_HALF_BLANK).append(Constants.NUM_GE).append(Constants.SYMBOL_QUESTION)
								.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SQL_KEYWORDS_AND).append(Constants.SYMBOL_HALF_BLANK)
								.append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
								.append(Constants.NUM_LE).append(Constants.SYMBOL_QUESTION).append(Constants.SYMBOL_HALF_BLANK);
					} else {
						retSql.append(" 1=1");
					}
				}
			}
		} else {
			return;
		}
		exps.add(retSql.toString());
	}

	public void intArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if (item == null) {
			return;
		}

		retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
				.append(item.getOperator());
		if (Constants.NULL.equals(item.getOperator()) || Constants.NOT_NULL.equals(item.getOperator())) {
			// 若查询条件为判断字段是否为空，没有添加值
			exps.add(retSql.toString());
		} else {
			retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getParamStr());
			if (item.getValue() != null && item.getValue().length() > 0) {
				try {
					values.add(Long.parseLong(item.getValue()));
					exps.add(retSql.toString());
				} catch (Exception e) {
					// 发生异常，子句略掉
				}
			}
		}
	}

	public void stringArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if (item == null) {
			return;
		}

		String value = item.getValue();
		String operator = item.getOperator();
		String columnName = item.getModelAlias() + item.getColumnName();
		if (Constants.NULL.equals(operator) || Constants.NOT_NULL.equals(operator)) {
			retSql.append(Constants.SYMBOL_HALF_BLANK).append(columnName).append(Constants.SYMBOL_HALF_BLANK).append(operator);
			// 若查询条件为判断字段是否为空，没有添加值
			exps.add(retSql.toString());
		} else {
			if (value != null && !value.isEmpty()) {
				if (operator.trim().endsWith("caseSensitive")) {
					operator = operator.replace("caseSensitive", "");
				} else {
					columnName = "UPPER (" + columnName + ")";
					value = value.toUpperCase();
				}
				retSql.append(Constants.SYMBOL_HALF_BLANK).append(columnName).append(Constants.SYMBOL_HALF_BLANK).append(operator);

				retSql.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SYMBOL_QUESTION);
				if (Constants.STR_CONTAINS.equalsIgnoreCase(operator) || Constants.STR_DOES_NOT_CONTAIN.equalsIgnoreCase(operator)) {
					retSql.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SQL_KEYWORDS_ESCAPE).append(Constants.SYMBOL_HALF_BLANK)
							.append("'&'");
					value = value.replaceAll("&", "&&");
					value = value.replaceAll("%", "&%");
					value = value.replaceAll("_", "&_");
				}

				if (item.getParamStr().length() > 0) {
					values.add(item.getParamStr().replace(Constants.SYMBOL_QUESTION, value));
				} else {
					values.add(item.getValue());
				}
				exps.add(retSql.toString());
			}
		}
	}

	public void byteArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if (item == null) {
			return;
		}

		retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
				.append(item.getOperator());
		if (Constants.NULL.equals(item.getOperator()) || Constants.NOT_NULL.equals(item.getOperator())) {
			// 若查询条件为判断字段是否为空，没有添加值
			exps.add(retSql.toString());
		} else {
			retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getParamStr());
			if (item.getValue() != null && item.getValue().length() > 0) {
				try {
					values.add(Byte.parseByte(item.getValue()));
					exps.add(retSql.toString());
				} catch (Exception e) {
					// 发生异常，子句略掉
				}
			}
		}
	}

	public void floatArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if (item == null) {
			return;
		}

		retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
				.append(item.getOperator());
		if (Constants.NULL.equals(item.getOperator()) || Constants.NOT_NULL.equals(item.getOperator())) {
			// 若查询条件为判断字段是否为空，没有添加值
			exps.add(retSql.toString());
		} else {
			retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getParamStr());
			if (item.getValue() != null && item.getValue().length() > 0) {
				try {
					values.add(Double.parseDouble(item.getValue()));
					exps.add(retSql.toString());
				} catch (Exception e) {
					// 发生异常，子句略掉
				}
			}
		}
	}

	public void enumArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		StringBuilder retSql2 = new StringBuilder();
		if (item == null) {
			return;
		}
		String operator = item.getOperator();
		if (Constants.EQ.equals(item.getOperator()) || Constants.NE.equals(item.getOperator())) {
			if (Constants.EQ.equals(item.getOperator())) {
				operator = Constants.STR_CONTAINS;
			} else {
				operator = Constants.STR_DOES_NOT_CONTAIN;
			}
		}
		retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
				.append(operator);
		retSql2.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
				.append(item.getOperator());
		if (Constants.NULL.equals(item.getOperator()) || Constants.NOT_NULL.equals(item.getOperator())) {
			// 若查询条件为判断字段是否为空，没有添加值
			exps.add(retSql.toString());
		} else {
			if (item.getValue() != null && item.getValue().length() > 0) {
				if (item.getDbColumnType() == DbColumnType.SYSTEMCODE && item.getValue().startsWith(Constants.SYMBOL_COMMA)
						&& item.getValue().endsWith(Constants.SYMBOL_COMMA)) {
					retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getParamStr());
					values.add("%" + item.getValue() + "%");
					exps.add(retSql.toString());
				} else if(Constants.STR_CONTAINS.equalsIgnoreCase(operator) || Constants.STR_DOES_NOT_CONTAIN.equalsIgnoreCase(operator)) {
					retSql.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SYMBOL_QUESTION)
							.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SQL_KEYWORDS_ESCAPE).append(Constants.SYMBOL_HALF_BLANK).append("'&'");
					values.add("%" + item.getValue() + "%");
					exps.add(retSql.toString());
				}else {
					retSql2.append(Constants.SYMBOL_HALF_BLANK).append(item.getParamStr());
					values.add(item.getValue());
					exps.add(retSql2.toString());
				}
			}
		}
	}

	public void commonArg(AdvQueryConditionItem item, List<String> exps, List<Object> values) {
		StringBuilder retSql = new StringBuilder();
		if (item == null) {
			return;
		}

		retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getModelAlias() + item.getColumnName()).append(Constants.SYMBOL_HALF_BLANK)
				.append(item.getOperator());
		if (Constants.NULL.equals(item.getOperator()) || Constants.NOT_NULL.equals(item.getOperator())) {
			// 若查询条件为判断字段是否为空，没有添加值
			exps.add(retSql.toString());
		} else {
			retSql.append(Constants.SYMBOL_HALF_BLANK).append(item.getParamStr());
			if (item.getValue() != null && item.getValue().length() > 0) {
				values.add(item.getValue());
				exps.add(retSql.toString());
			}
		}
	}

	/**
	 * 添加子句的ModelAlias
	 *
	 * @param item
	 */
	private void fillSubItemModelAlias(AdvQueryConditionItem item) {
		if (item.getModelAlias() != null && item.getModelAlias().length() > 0 && !item.getModelAlias().equalsIgnoreCase("NULL")) {
			if (item.getSubconds() != null && item.getSubconds().size() > 0) {
				for (AdvQueryConditionItem tmp : item.getSubconds()) {
					tmp.setModelAlias(item.getModelAlias());
				}
			}
		}
	}

	/**
	 * 连接多个条件表达式
	 *
	 * @param exps
	 * @param logic
	 * @return
	 */
	private String joinExps(List<String> exps, String logic) {
		StringBuilder retSql = new StringBuilder();
		if (exps != null && exps.size() > 0) {
			for (String exp : exps) {
				if (exp != null && exp.length() > 0) {
					if (retSql.length() > 0) {
						retSql.append(Constants.SYMBOL_HALF_BLANK);
						retSql.append(logic);
						retSql.append(Constants.SYMBOL_HALF_BLANK);
					}
					retSql.append(exp);
				}
			}
		}
		if(retSql.length() == 0) {
			return Constants.SYMBOL_EMPTY;
		}
		return "(" + retSql.toString() + ")";
	}

	/**
	 * 获取关连表子查询语句
	 *
	 * @param cond
	 * @param values
	 * @return
	 * @throws Exception
	 */
	private String getSubSelect(AdvQueryConditionItem cond, List<Object> values, Boolean existsParam) {
		String retSql = "";
		StringBuilder tmpSql = new StringBuilder();
		if (cond == null) {
			return retSql;
		}
		List<AdvQueryConditionItem> conds = cond.getSubconds();
		List<String> expsTmp = new ArrayList<String>();
		if (conds != null && conds.size() > 0) {
			// 结构：关联表表名,关连字段名,本表表名,本表对应字段名
			String joinInfo = cond.getJoinInfo();
			String[] arr = joinInfo.split(Constants.SYMBOL_COMMA);
			if (arr == null || arr.length != 4) {
				return retSql;
			}
			// 拼接in子查询语句
			if (cond.getSubconds() != null && cond.getSubconds().size() > 0) {
				tmpSql.append(Constants.SYMBOL_HALF_BLANK).append(cond.getModelAlias() + arr[3]).append(Constants.SYMBOL_HALF_BLANK)
						.append(Constants.SQL_KEYWORDS_IN).append(Constants.SYMBOL_LEFT_BRACKET).append(Constants.SQL_KEYWORDS_SELECT)
						.append(Constants.SYMBOL_HALF_BLANK).append(arr[1]).append(Constants.SYMBOL_HALF_BLANK)
						.append(Constants.SQL_KEYWORDS_FROM).append(Constants.SYMBOL_HALF_BLANK).append(arr[0])
						.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SQL_KEYWORDS_WHERE).append(Constants.SYMBOL_HALF_BLANK)
						.append(TO_REPLACE).append(Constants.SYMBOL_RIGHT_BRACKET);
			}

			// 转换关连表下的子查询条件
			for (AdvQueryConditionItem item : conds) {
				tranArg(item, expsTmp, values, existsParam);
				item.setParent(cond);
			}
			if (expsTmp == null || expsTmp.isEmpty()) {
				return "";
			}
			// 关联表不需要加有效条件
			//String[] addFalg =arr[0].split("_");
			//if(addFalg.length<=1||!(addFalg[addFalg.length-1].equals("DI")||addFalg[addFalg.length-1].equals("PA")||addFalg[addFalg.length-1].equals("SV")||addFalg[addFalg.length-1].equals("MC")) ){
			//	expsTmp.add("valid = 1");
			//}
			return tmpSql.toString().replace(TO_REPLACE, joinExps(expsTmp, Constants.SQL_KEYWORDS_AND));
		} else {
			// 如果关联表下没有子查询条件，do nothing
		}

		return retSql;
	}

	/**
	 * 获取关连表子查询语句
	 *
	 * @param cond
	 * @param values
	 * @return
	 * @throws Exception
	 */
	private String getReferenceSelect(AdvQueryConditionItem cond, List<Object> values, String type, Boolean existsParam) {
		String retSql = null;
		StringBuilder tmpSql = new StringBuilder();
		if (cond == null) {
			return retSql;
		}
		List<AdvQueryConditionItem> conds = cond.getSubconds();
		List<String> expsTmp = new ArrayList<String>();
		if (conds != null && conds.size() > 0) {
			// 结构：关联表表名,关连字段名,本表表名,本表对应字段名
			String joinInfo = cond.getJoinInfo();
			String[] arr = joinInfo.split(Constants.SYMBOL_COMMA);
			if (arr == null || arr.length != 4) {
				return retSql;
			}
			// 拼接in子查询语句
			if (cond.getSubconds() != null && cond.getSubconds().size() > 0) {
				tmpSql.append(Constants.SYMBOL_HALF_BLANK).append(cond.getModelAlias() + arr[3]).append(Constants.SYMBOL_HALF_BLANK)
						.append(Constants.SQL_KEYWORDS_IN).append(Constants.SYMBOL_LEFT_BRACKET).append(Constants.SQL_KEYWORDS_SELECT)
						.append(Constants.SYMBOL_HALF_BLANK).append(arr[1]).append(Constants.SYMBOL_HALF_BLANK)
						.append(Constants.SQL_KEYWORDS_FROM).append(Constants.SYMBOL_HALF_BLANK).append(arr[0])
						.append(Constants.SYMBOL_HALF_BLANK).append(Constants.SQL_KEYWORDS_WHERE).append(Constants.SYMBOL_HALF_BLANK)
						.append(TO_REPLACE).append(Constants.SYMBOL_RIGHT_BRACKET);
			}

			// 转换关连表下的子查询条件
			for (AdvQueryConditionItem item : conds) {
				item.setParent(cond);
				if ("4".equals(type)) { // 参照，通过ID查询
					if ("ID".equals(item.getColumnName())) {
						tranArg(item, expsTmp, values, existsParam);
					} else if (!"ID".equals(item.getColumnName())) {
						tranArg(item, expsTmp, values, existsParam);
					}
				} else if ("5".equals(type)) { // 参照，用户可以选择，也可以通过名称进行模糊查询，目前中针对部门与人员
					if ("ID".equals(item.getColumnName()) && item.getValue() != null && item.getValue().length() > 0) {
						tranArg(item, expsTmp, values, existsParam);
						break;
					} else if (!"ID".equals(item.getColumnName())) {
						tranArg(item, expsTmp, values, existsParam);
					}
				}
			}
			String joinResult = joinExps(expsTmp, Constants.SQL_KEYWORDS_AND);
			if (joinResult != null && !joinResult.isEmpty()) {
				retSql = tmpSql.toString().replace(TO_REPLACE, joinResult);
			}
		} else {
			// 如果关联表下没有子查询条件，do nothing
		}

		return retSql;
	}

	@Override
	public DefaultAdvCond getDefaultAdvCond(String viewCode) {
		return defAdvCondDao.findEntityByCriteria(Restrictions.eq("viewCode", viewCode));
	}

	@Override
	public void saveDefaultAdvCond(DefaultAdvCond defaultAdvCond) {
		defAdvCondDao.save(defaultAdvCond);
	}

	/**
	 * 获取视图对应的条件
	 *
	 * @param viewCode
	 * @return
	 */
	public List<AdvQueryCondition> getAdvQueryConditionByView(String viewCode){
		Assert.notNull(viewCode);
		return conditionDao.findByHql("from AdvQueryCondition adv where adv.view.code=? and adv.valid=true and (adv.owner=? or adv.adminFlag=true)", new Object[] {
				viewCode, getCurrentUser() });
	}

}