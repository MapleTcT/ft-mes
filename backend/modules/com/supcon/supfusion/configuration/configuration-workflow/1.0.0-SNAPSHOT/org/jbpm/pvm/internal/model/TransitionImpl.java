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
package org.jbpm.pvm.internal.model;

import org.jbpm.api.model.Event;
import org.jbpm.api.model.Transition;

import java.util.List;

/**
 * @author Tom Baeyens
 */
public class TransitionImpl extends ObservableElementImpl implements Transition {

	private static final long serialVersionUID = 1L;
	// 原活动
	protected ActivityImpl source;
	protected Integer sourceIndex;
	// 目标活动
	protected ActivityImpl destination;
	protected Condition condition;

	protected int selectPeople;// 1本公司选人，2跨公司选择，0不支持选择
	protected int notificationType;// 1通知线
	protected int cancel;// 作废线
	protected int requiredStaff;//指定人员是否必填
	protected int defaultSelectStaff;//是否默认上次选人
	protected int reject;//驳回线
	protected int routerSequence;//路由顺序
	protected int userRangeFalg;//路由选人范围userRange
	
	
	public int getDefaultSelectStaff() {
		return defaultSelectStaff;
	}
	public void setDefaultSelectStaff(int defaultSelectStaff) {
		this.defaultSelectStaff = defaultSelectStaff;
	}

	public int getUserRangeFalg() {
		return userRangeFalg;
	}

	public void setUserRangeFalg(int userRangeFalg) {
		this.userRangeFalg = userRangeFalg;
	}

	public int getReject() {
		return reject;
	}

	public void setReject(int reject) {
		this.reject = reject;
	}

	public int getRequiredStaff() {
		return requiredStaff;
	}

	public void setRequiredStaff(int requiredStaff) {
		this.requiredStaff = requiredStaff;
	}

	public int getCancel() {
		return cancel;
	}

	public void setCancel(int cancel) {
		this.cancel = cancel;
	}

	public int getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(int notificationType) {
		this.notificationType = notificationType;
	}

	public int getSelectPeople() {
		return selectPeople;
	}

	public void setSelectPeople(int selectPeople) {
		this.selectPeople = selectPeople;
	}

	/* Use one of the ActivityImpl.createOutgoingTransition methods instead. */
	public TransitionImpl() {
		super();
	}

	public void makeDefault() {
		source.setDefaultOutgoingTransition(this);
	}

	public String toString() {
		String destinationName = (destination != null ? destination.getName()
				: null);
		String sourceName = (source != null ? source.getName() : null);
		return (sourceName != null ? "(" + sourceName + ")--" : "--")
				+ (name != null ? name + "-->" : ">")
				+ (destinationName != null ? "(" + destinationName + ")" : "");
	}

	public EventImpl createEvent() {
		EventImpl event = new EventImpl();
		event.setName(Event.TAKE);
		addEvent(event);
		return event;
	}

	public EventImpl getEvent() {
		return getEvent(Event.TAKE);
	}

	// get parent
	// ///////////////////////////////////////////////////////////////

	/**
	 * the first common parent between the source and the destination activity.
	 * The source and destination itself are included in the search except if
	 * source and destination are equal. In that case (self-transition), then
	 * it's the parent of the activity.
	 */
	public ObservableElementImpl getParent() {
		// if one of the two ends is null
		if ((source == null) || (destination == null)) {
			// the process definition is returned
			return processDefinition;
		}
		// if this is a self transition
		if (source.equals(destination)) {
			// the parent of the activity is returned
			return source.getParent();
		}
		// if none of the above, we search for the first common element in the
		// parent chains
		List<ObservableElementImpl> sourceChain = source.getParentChain();
		List<ObservableElementImpl> destinationChain = destination
				.getParentChain();
		for (ObservableElementImpl sourceElement : sourceChain) {
			for (ObservableElementImpl destinationElement : destinationChain) {
				if (sourceElement.equals(destinationElement)) {
					return sourceElement;
				}
			}
		}
		return null;
	}

	// getters and setters
	// //////////////////////////////////////////////////////

	public void setName(String name) {
		this.name = name;
		if (source != null) {
			source.clearOutgoingTransitionsMap();
		}
	}

	public ActivityImpl getSource() {
		return source;
	}

	public void setSource(ActivityImpl source) {
		this.source = source;
	}

	public ActivityImpl getDestination() {
		return destination;
	}

	public void setDestination(ActivityImpl destination) {
		this.destination = destination;
	}

	public Integer getSourceIndex() {
		return sourceIndex;
	}

	public void setSourceIndex(Integer sourceIndex) {
		this.sourceIndex = sourceIndex;
	}

	public Condition getCondition() {
		return condition;
	}

	public void setCondition(Condition condition) {
		this.condition = condition;
	}

	public int getRouterSequence() {
		return routerSequence;
	}

	public void setRouterSequence(int routerSequence) {
		this.routerSequence = routerSequence;
	}
}
