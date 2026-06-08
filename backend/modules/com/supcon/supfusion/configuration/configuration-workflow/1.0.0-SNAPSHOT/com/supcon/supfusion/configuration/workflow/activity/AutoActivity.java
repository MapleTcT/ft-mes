package com.supcon.supfusion.configuration.workflow.activity;

import com.supcon.supfusion.configuration.services.utils.DbUtils;
import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.base.entities.Script;
import com.supcon.supfusion.base.services.ScriptService;
import com.supcon.supfusion.configuration.workflow.service.WorkflowTaskService;
import com.supcon.supfusion.configuration.workflow.variables.Variables;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jbpm.api.JbpmException;
import org.jbpm.api.activity.ActivityExecution;
import org.jbpm.api.listener.EventListener;
import org.jbpm.api.listener.EventListenerExecution;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.jpdl.internal.activity.JpdlActivity;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.jbpm.pvm.internal.script.ScriptManager;
import org.jbpm.pvm.internal.wire.Descriptor;
import org.jbpm.pvm.internal.wire.WireContext;
import org.jbpm.pvm.internal.wire.WireDefinition;
import org.jbpm.pvm.internal.wire.descriptor.ObjectDescriptor;

import java.util.List;
import java.util.Map;

public class AutoActivity extends JpdlActivity implements EventListener {

	private static final long serialVersionUID = -678588379079230167L;
	private List<Object> targetTransits;

	@Override
	public void execute(ActivityExecution execution) throws Exception {
		perform(execution);
		ExecutionImpl exc=(ExecutionImpl) execution;

		// 经过自动活动后,待办不可撤回
		EnvironmentImpl env = EnvironmentImpl.getCurrent();
		WorkflowTaskService taskService = env.get(WorkflowTaskService.class);
		taskService.setRecallAbleFlag(false);


		String transitionName="";
		if(exc.getTransition()!=null){
			transitionName=exc.getTransition().getName();
		}else if(exc.getWorkFlowVar()!=null){
			transitionName=exc.getWorkFlowVar().getOutcome();
		}else{
			transitionName=exc.getActivity().getOutgoingTransitions().get(0).getName();
		}
		exc.historyAutomatic(transitionName);
		// history((ExecutionImpl) execution);
	}

	@Override
	public void notify(EventListenerExecution execution) throws Exception {
		perform(execution);
	}

	private void perform(OpenExecution execution) throws Exception {
		/* 此举是为了即使N个不同类型的要执行的目标也能按照既定的顺序执行. */
		EnvironmentImpl env = EnvironmentImpl.getCurrent();
		if (targetTransits != null) {

			for (Object t : targetTransits) {
				/* bean */
				if (t instanceof BeanTransit) {
					/* 单个bean执行开始 */
					BeanTransit bt = (BeanTransit) t;
					Object target = null;
					if (bt.getBeanClass() != null) {
						Class<?> clazz = Class.forName(bt.getBeanClass());
						if (clazz != null) {
							target = env.get(clazz);
						}
					} else if (bt.getBeanName() != null) {
						target = env.get(bt.getBeanName());
					} else {
						// FIXME 抛异常or不抛?
						throw new JbpmException("该bean不存在.");
					}
					if (target != null) {
						Class<?> clazz = target.getClass();
						WireContext wireContext = new WireContext(new WireDefinition());

						Object returnValue = ObjectDescriptor.invokeMethod(bt.getMethodName(), bt.getArgDescriptors(), wireContext, target, clazz);
						if (bt.getVariableName() != null) {
							execution.setVariable(bt.getVariableName(), returnValue);
						}
					}
					bt = null;
					/* 单个bean执行结束 */
				}
				/* java */
				else if (t instanceof JavaTransit) {
					/* 单个java执行开始 */
					JavaTransit jt = (JavaTransit) t;
					Object target = null;
					if (jt.getInvocationReference() != null) {
						target = jt.getInvocationReference().getObject(execution);
					} else {
						throw new JbpmException("没有指定要执行的目标类.");
					}
					Class<?> clazz;
					if (target != null) {
						clazz = target.getClass();
					} else {
						ObjectDescriptor objectDescriptor = (ObjectDescriptor) jt.getInvocationReference().getDescriptor();
						String className = objectDescriptor.getClassName();
						ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
						clazz = Class.forName(className, true, classLoader);
					}
					WireContext wireContext = new WireContext(new WireDefinition());
					Object returnValue = ObjectDescriptor.invokeMethod(jt.getMethodName(), jt.getArgDescriptors(), wireContext, target, clazz);
					if (jt.getVariableName() != null) {
						execution.setVariable(jt.getVariableName(), returnValue);
					}
					jt = null;
					/* 单个java执行结束 */
				}
				/* script */
				else if (t instanceof ScriptTransit) {
					
					/* 单个script执行开始 */
					// ScriptTransit st = (ScriptTransit) t;
					// ScriptManager scriptManager = environment.get(ScriptManager.class);
					// Object returnValue = scriptManager.evaluateScript(st.getScript(), st.getLang());
					// if (st.getVariableName() != null) {
					// execution.setVariable(st.getVariableName(), returnValue);
					// }
					// st = null;

					/* 单个script执行结束 */
					// ////////////////////////////////////////
					
					
					ScriptService scriptService = env.get(ScriptService.class);
					if (null == scriptService)
						throw new EcException("could not found ScriptService.");
					ScriptTransit st = (ScriptTransit) t;
					Script script = scriptService.get(execution.getEntityCode(), st.getScript());
					if (null == script || null == script.getCode()){
						throw new EcException(EcException.Code.NO_SCRIPT);
					}
					Map<String, Object> variables = Variables.executeAll(execution);
					variables.put("execution", (ExecutionImpl) execution);
					// variables.putAll(execution.getVariables());
					variables.put("db", DbUtils.getInstance());// add db support.
					// ScriptExecutor.eval(st.getScript(), variables);
					variables.put("env", env);

					st = null;
					// ////////////////////////////////////////

				}
				/* hql */
				else if (t instanceof HqlTransit) {
					/* 单个hql执行开始 */
					HqlTransit ht = (HqlTransit) t;
					Session session = env.get(Session.class);
					Query query = session.createQuery(ht.getQuery());
					if (ht.getParametersListDescriptor() != null) {
						for (Descriptor valueDescriptor : ht.getParametersListDescriptor().getValueDescriptors()) {
							String parameterName = valueDescriptor.getName();
							Object value = WireContext.create(valueDescriptor);
							applyParameter(query, parameterName, value);
						}
					}
					Object result = null;
					if (ht.isUnique()) {
						result = query.uniqueResult();
					} else {
						result = query.list();
					}
					if (ht.getVariableName() != null) {
						execution.setVariable(ht.getVariableName(), result);
					}
					ht = null;
					/* 单个hql执行结束 */
				}
				/* sql */
				else if (t instanceof SqlTransit) {
					/* 单个sql执行开始 */
					SqlTransit st = (SqlTransit) t;
					Session session = env.get(Session.class);
					ScriptManager scriptManager = env.get(ScriptManager.class);
					Query query = session.createSQLQuery((String) scriptManager.evaluateExpression(st.getQuery(), null));
					if (st.getParametersListDescriptor() != null) {
						for (Descriptor valueDescriptor : st.getParametersListDescriptor().getValueDescriptors()) {
							String parameterName = valueDescriptor.getName();
							Object value = WireContext.create(valueDescriptor);
							applyParameter(query, parameterName, value);
						}
					}
					Object result = null;
					String queryStr = st.getQuery().toLowerCase();
					if (queryStr.startsWith("update") || queryStr.startsWith("insert") || queryStr.startsWith("delete")) {
						query.executeUpdate();
					} else {
						if (st.isUnique()) {
							result = query.uniqueResult();
						} else {
							result = query.list();
						}
						if (st.getVariableName() != null) {
							execution.setVariable(st.getVariableName(), result);
						}
					}
					st = null;
					/* 单个sql执行结束 */
				} else {
					throw new JbpmException("不支持的自动活动类型");
				}
			}

		}

	}

	/**
	 * 附加变量
	 * 
	 * @param q
	 * @param parameterName
	 * @param value
	 */
	protected void applyParameter(Query q, String parameterName, Object value) {
		if (value instanceof String) {
			q.setString(parameterName, (String) value);
		} else if (value instanceof Long) {
			q.setLong(parameterName, (Long) value);
		} else {
			throw new JbpmException("unknown hql parameter type: " + value.getClass().getName());
		}
	}

	public void setTargetTransits(List<Object> targetTransits) {
		this.targetTransits = targetTransits;
	}

}
