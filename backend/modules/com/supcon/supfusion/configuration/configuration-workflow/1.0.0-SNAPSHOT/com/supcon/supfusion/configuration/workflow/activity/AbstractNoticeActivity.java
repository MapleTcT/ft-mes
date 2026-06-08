package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.pvm.internal.model.ExecutionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AbstractNoticeActivity {

	private static final Logger logger = LoggerFactory.getLogger(AbstractNoticeActivity.class);
	public static final String EMAIL_FROM = "";
	public static final String JABBER_FROM = "";
	public static final String SMS_FROM = "";
	public static final String APP_FROM = "";

	protected NoticeTransit noticeTransit;

	public void setNoticeTransit(NoticeTransit noticeTransit) {
		this.noticeTransit = noticeTransit;
	}

	@SuppressWarnings("unchecked")
	public void sendNotice(ExecutionImpl execution, Set<Long> pendingUserIds) {
	}
	
}
