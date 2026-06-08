/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.activity.SubProcessInParameterImpl;
import org.jbpm.jpdl.internal.activity.SubProcessOutParameterImpl;
import org.jbpm.jpdl.internal.activity.SubProcessParameterImpl;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.el.Expression;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.wire.Descriptor;
import org.jbpm.pvm.internal.wire.WireContext;
import org.jbpm.pvm.internal.wire.xml.WireParser;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tom Baeyens
 */
public class SubProcessBinding extends JpdlBinding {
	public static final String TAG = "sub-process";

	public SubProcessBinding() {
		super(TAG);
	}

	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
		return null;
	}

	void parseParameter(Element element, SubProcessParameterImpl parameter) {
		String name = XmlUtil.attribute(element, "subvar");
		parameter.setSubVariableName(name);

		String expressionText = XmlUtil.attribute(element, "expr");
		String language = XmlUtil.attribute(element, "expr-lang");
		if (expressionText != null) {
			Expression expression = Expression.create(expressionText, language);
			parameter.setExpression(expression);
		}

		String variable = XmlUtil.attribute(element, "var");
		if (variable != null) {
			parameter.setVariableName(variable);
		}
	}

	public static Map<String, String> parseSwimlaneMappings(Element element, Parse parse) {
		Map<String, String> swimlaneMappings = new HashMap<String, String>();

		for (Element inElement : XmlUtil.elements(element, "swimlane-mapping")) {
			String swimlane = XmlUtil.attribute(inElement, "swimlane", parse);
			String subSwimlane = XmlUtil.attribute(inElement, "sub-swimlane", parse);

			swimlaneMappings.put(swimlane, subSwimlane);
		}

		return swimlaneMappings;
	}
}
