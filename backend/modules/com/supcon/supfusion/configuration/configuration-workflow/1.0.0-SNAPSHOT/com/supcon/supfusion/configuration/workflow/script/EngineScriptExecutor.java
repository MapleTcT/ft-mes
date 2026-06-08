package com.supcon.supfusion.configuration.workflow.script;

import java.util.Map;

public class EngineScriptExecutor {

	public static Object eval(String script, Map<String, Object> variables) {

		return ScriptExecutor.eval(script, variables);
	}

}