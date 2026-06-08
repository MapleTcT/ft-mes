package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.pvm.internal.wire.descriptor.ListDescriptor;

import java.io.Serializable;

public class HqlTransit implements Serializable  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6314553631837612200L;
	// ~ Instance fields =======================================================
	protected String query;
	protected boolean unique;
	protected ListDescriptor parametersListDescriptor;
	protected String variableName;

	// ~ Constructor ===========================================================
	// ~ Methods ===============================================================

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public ListDescriptor getParametersListDescriptor() {
		return parametersListDescriptor;
	}

	public void setParametersListDescriptor(ListDescriptor parametersListDescriptor) {
		this.parametersListDescriptor = parametersListDescriptor;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
}
