package com.supcon.supfusion.configuration.workflow.service;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Map;


/**
 * 提供业务数据作为流程变量使用.每个模块生成时会对应生成相应实现,能够在需要使用时获取到业务的变量.
 * 
 * @author songjiawei
 * 
 */
public interface VariablesProvider {
	/**
	 * 提供流程业务变量.
	 * 
	 * @param id
	 * @return
	 */
	Map<String, Object> provide(Long id);
}

abstract class AbstractVariablesProvider implements VariablesProvider {
	static class Adapter extends XmlAdapter<AbstractVariablesProvider, VariablesProvider>{
		public VariablesProvider unmarshal(AbstractVariablesProvider v){return v;}
		public AbstractVariablesProvider marshal(VariablesProvider v){return (AbstractVariablesProvider)v;}
	}
}

class VariablesProviderImpl extends AbstractVariablesProvider {
	@XmlAttribute String name;
	@Override
	public Map<String, Object> provide(Long id) {
		return null;
	}
}

class AnotherVariablesProviderImpl extends AbstractVariablesProvider {
	@XmlAttribute int id;
	@Override
	public Map<String, Object> provide(Long id) {
		return null;
	}
}