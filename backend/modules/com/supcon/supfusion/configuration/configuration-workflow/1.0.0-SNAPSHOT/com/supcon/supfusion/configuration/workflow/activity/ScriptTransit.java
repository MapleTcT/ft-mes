package com.supcon.supfusion.configuration.workflow.activity;

import java.io.Serializable;

public class ScriptTransit implements Serializable {

	private static final long serialVersionUID = 5475584613671814134L;
	// ~ Instance fields =======================================================
	protected String script;
	protected String lang;
	protected String variableName;

	// ~ Constructor ===========================================================

	// ~ Methods ===============================================================
	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
}
