package com.supcon.supfusion.configuration.workflow.entities;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public interface IModelStatus {
	Integer getStatus();
	void setStatus(Integer status);
}

abstract class AbstractModelStatus implements IModelStatus {
	static class Adapter extends XmlAdapter<AbstractModelStatus, IModelStatus>{
		public IModelStatus unmarshal(AbstractModelStatus v){return v;}
		public AbstractModelStatus marshal(IModelStatus v){return (AbstractModelStatus)v;}
	}
}

class ModelStatusImpl extends AbstractModelStatus {
	@XmlAttribute String name;
	@Override
	public Integer getStatus() {
		return null;
	}
	@Override
	public void setStatus(Integer status) {
	}
}

class AnotherModelStatusImpl extends AbstractModelStatus {
	@XmlAttribute int id;
	@Override
	public Integer getStatus() {
		return null;
	}
	@Override
	public void setStatus(Integer status) {
	}
}