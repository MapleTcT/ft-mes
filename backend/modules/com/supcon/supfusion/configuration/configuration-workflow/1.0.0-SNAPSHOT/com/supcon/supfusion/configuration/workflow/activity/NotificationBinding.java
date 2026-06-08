package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;


public class NotificationBinding extends AbstractNoticeBinding {

	public static final String TAG = "notification";

	public NotificationBinding() {
		super(TAG);
	}

	@Override
	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
		return null;
	}

}
