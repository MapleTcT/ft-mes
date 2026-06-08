package com.supcon.supfusion.configuration.workflow.script;

import com.supcon.supfusion.configuration.workflow.variables.Variables;
import org.jbpm.api.JbpmException;
import org.jbpm.api.model.OpenExecution;
import org.jbpm.pvm.internal.model.Condition;

public class ScriptCondition implements Condition {
	private static final long serialVersionUID = -4499145835110264587L;
	protected String expression;

	@Override
	public boolean evaluate(OpenExecution execution) {
		if (null == expression || expression.trim().length() == 0)
			return false;
		Object result = EngineScriptExecutor.eval(expression, Variables.executeAll(execution));
		if (result instanceof Boolean) {
			return ((Boolean) result).booleanValue();
		}
		throw new JbpmException("expression condition '" + expression + "' did not return a boolean: " + result);
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
}
