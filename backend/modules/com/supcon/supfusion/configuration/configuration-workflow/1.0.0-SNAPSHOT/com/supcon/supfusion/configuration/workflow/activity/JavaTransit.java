package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.pvm.internal.wire.descriptor.ArgDescriptor;
import org.jbpm.pvm.internal.wire.usercode.UserCodeReference;

import java.io.Serializable;
import java.util.List;

public class JavaTransit implements Serializable {

	private static final long serialVersionUID = 2787641457980304548L;
	// ~ Instance fields =======================================================
	protected String variableName;
	protected UserCodeReference invocationReference;
	protected List<ArgDescriptor> argDescriptors;
	protected String methodName;

	// ~ Constructor ===========================================================
	// ~ Methods ===============================================================

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	public UserCodeReference getInvocationReference() {
		return invocationReference;
	}

	public void setInvocationReference(UserCodeReference invocationReference) {
		this.invocationReference = invocationReference;
	}

	public List<ArgDescriptor> getArgDescriptors() {
		return argDescriptors;
	}

	public void setArgDescriptors(List<ArgDescriptor> argDescriptors) {
		this.argDescriptors = argDescriptors;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
}
