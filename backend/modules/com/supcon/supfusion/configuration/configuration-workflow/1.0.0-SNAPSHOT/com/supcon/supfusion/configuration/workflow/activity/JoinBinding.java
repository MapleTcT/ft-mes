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

import org.hibernate.LockMode;
import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

/**
 * @author Tom Baeyens
 */
public class JoinBinding extends JpdlBinding {
	public static final String TAG = "join";
	private static final String MULTIPLICITY = "multiplicity";

	private static final String LOCKMODE = "lockmode";

	public JoinBinding() {
		super("join");
	}

	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
		JoinActivity joinActivity = new JoinActivity();

		if (element.hasAttribute(MULTIPLICITY)) {
			String multiplicityText = element.getAttribute(MULTIPLICITY);
			// Expression expression = Expression.create(multiplicityText, Expression.LANGUAGE_UEL_VALUE);
			// joinActivity.setMultiplicity(expression);
			if (null != multiplicityText && multiplicityText.trim().length() > 0)
				joinActivity.setMultiplicity(Integer.parseInt(multiplicityText));
		}

//		if (element.hasAttribute(LOCKMODE)) {
//			String lockModeText = element.getAttribute(LOCKMODE);
//			LockMode lockMode = LockMode.parse(lockModeText.toUpperCase());
//			if (lockMode == null) {
//				parse.addProblem(lockModeText + " is not a valid lock mode", element);
//			} else {
//				joinActivity.setLockMode(lockMode);
//			}
//		}

		return joinActivity;
	}

}
