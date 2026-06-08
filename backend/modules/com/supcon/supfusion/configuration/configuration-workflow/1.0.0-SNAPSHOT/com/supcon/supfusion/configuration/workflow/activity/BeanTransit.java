package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.pvm.internal.wire.descriptor.ArgDescriptor;

import java.io.Serializable;
import java.util.List;

public class BeanTransit implements Serializable {

	private static final long serialVersionUID = 6195085551334351332L;
	// ~ Instance fields =======================================================
	protected String variableName;
	protected List<ArgDescriptor> argDescriptors;
	protected String methodName;
	protected String beanName;
	protected String beanClass;

	// ~ Constructor ===========================================================
	// ~ Methods ===============================================================

	public String getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(String beanClass) {
		this.beanClass = beanClass;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
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

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
}
