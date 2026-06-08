package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

import java.util.List;

/**
 * 人工活动解析.
 * 
 * @author songjiawei
 * 
 */
public class TaskBinding extends AbstractNoticeBinding {

	public static final String TAG = "task";

	public TaskBinding() {
		super(TAG);
	}

	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
		return null;
	}
}
