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

import com.supcon.supfusion.configuration.workflow.script.ScriptCondition;
import org.jbpm.jpdl.internal.activity.ForkActivity;
import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.model.ActivityImpl;
import org.jbpm.pvm.internal.model.TransitionImpl;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.wire.usercode.UserCodeCondition;
import org.jbpm.pvm.internal.wire.usercode.UserCodeReference;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

import java.util.List;

/**
 * @author Tom Baeyens
 */
public class ForkBinding extends JpdlBinding {
	public static final String TAG = "fork";

	public ForkBinding() {
		super(TAG);
	}

	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {

		List<Element> transitionElements = XmlUtil.elements(element, "transition");
		ActivityImpl activity = parse.contextStackFind(ActivityImpl.class);
		List<TransitionImpl> transitions = (List<TransitionImpl>) activity.getOutgoingTransitions();
		for (TransitionImpl t : transitions) {
			for (Element te : transitionElements) {
				if (t.getName().equals(te.getAttribute("name"))) {
					Element conditionElement = XmlUtil.element(te, "condition");
					if (conditionElement != null) {
						if (conditionElement.hasAttribute("expr")) {
							if (conditionElement.getAttribute("expr").trim().length() > 0) {
								ScriptCondition sc = new ScriptCondition();
								sc.setExpression(conditionElement.getAttribute("expr"));
								t.setCondition(sc);
							}
						} else {
							Element conditionHandlerElement = XmlUtil.element(conditionElement, "handler");
							if (conditionHandlerElement != null) {
								UserCodeCondition userCodeCondition = new UserCodeCondition();
								UserCodeReference conditionReference = parser.parseUserCodeReference(conditionHandlerElement, parse);
								userCodeCondition.setConditionReference(conditionReference);
								t.setCondition(userCodeCondition);
							}
						}
					}
				}
			}
		}
		return new ForkActivity();
	}

}
