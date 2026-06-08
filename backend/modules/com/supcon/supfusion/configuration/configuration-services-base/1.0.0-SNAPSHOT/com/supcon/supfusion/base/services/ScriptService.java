package com.supcon.supfusion.base.services;

import com.supcon.supfusion.base.entities.Script;
import com.supcon.supfusion.framework.scaffold.hibernate.page.Page;

import java.io.IOException;
import java.util.List;

/**
 * A interface of Orchid Script Engine.
 * 
 * @author songjiawei
 * 
 */
public interface ScriptService {

	Script get(long id) throws IOException;
	Script get(String entityCode, String code) throws IOException;
	Page<Script> find(String entityCode, Page<Script> page);
	void save(Script s);

	boolean hasScriptExist(String entityCode, String scriptCode);

	List<String> hasScriptUsed(Long scriptId);

	boolean deleteWithCallback(long id);
}