package com.supcon.supfusion.configuration.workflow.activity;


import com.supcon.supfusion.base.services.impl.ProcessServiceImpl;

public class NoticeTransit {

	public static String DEFAULT_EMAIL_TITLE;
	public static String DEFAULT_EMAIL_CONTENT;
	public static String DEFAULT_JABBER_CONTENT;
	public static String DEFAULT_SMS_CONTENT;
	public static String DEFAULT_APP_CONTENT;

	private String emailTitle;
	private String emailContent;
	private String jabberContent;
	private String smsContent;
	private String appContent;

	/* 以下属性用于模板key */
	public static String DEFAULT_EMAIL_TITLE_KEY;
	public static String DEFAULT_EMAIL_CONTENT_KEY;
	public static String DEFAULT_JABBER_KEY;
	public static String DEFAULT_SMS_KEY;
	public static String DEFAULT_APP_KEY;

	private String emailTitleKey;
	private String emailContentKey;
	private String jabberKey;
	private String smsKey;
	private String appKey;
	static {
		DEFAULT_EMAIL_TITLE = ProcessServiceImpl.DEFAULT_EMAIL_TITLE;
		DEFAULT_EMAIL_CONTENT = ProcessServiceImpl.DEFAULT_EMAIL_CONTENT;
		DEFAULT_JABBER_CONTENT = ProcessServiceImpl.DEFAULT_JABBER_CONTENT;
		DEFAULT_SMS_CONTENT = ProcessServiceImpl.DEFAULT_SMS_CONTENT;
		DEFAULT_APP_CONTENT = ProcessServiceImpl.DEFAULT_APP_CONTENT;
		DEFAULT_EMAIL_TITLE_KEY = "DEFAULT_EMAIL_TITLE";
		DEFAULT_EMAIL_CONTENT_KEY = "DEFAULT_EMAIL_CONTENT";
		DEFAULT_JABBER_KEY = "DEFAULT_JABBER";
		DEFAULT_SMS_KEY = "DEFAULT_SMS";
		DEFAULT_APP_KEY = "DEFAULT_APP";
	}

	public boolean use() {
		if (null != emailTitle && null != emailContent) {
			return true;
		}
		if (null != jabberContent)
			return true;
		if (null != smsContent)
			return true;
		if (null != appContent)
			return true;
		return false;
	}

	public String getEmailTitle() {
		this.emailTitle = ProcessServiceImpl.DEFAULT_EMAIL_TITLE;
		this.emailTitleKey = DEFAULT_EMAIL_TITLE_KEY;
		return emailTitle;
	}

	public void setEmailTitle(String emailTitle) {
		this.emailTitle = emailTitle;
	}

	public String getEmailContent() {
		this.emailContent = ProcessServiceImpl.DEFAULT_EMAIL_CONTENT;
		this.emailContentKey = DEFAULT_EMAIL_CONTENT_KEY;

		return emailContent;
	}

	public void setEmailContent(String emailContent) {
		this.emailContent = emailContent;
	}

	public String getJabberContent() {
		this.jabberContent = ProcessServiceImpl.DEFAULT_JABBER_CONTENT;
		this.jabberKey = DEFAULT_JABBER_KEY;
		return jabberContent;
	}

	public void setJabberContent(String jabberContent) {
		this.jabberContent = jabberContent;
	}

	public String getSmsContent() {
		this.smsContent = ProcessServiceImpl.DEFAULT_SMS_CONTENT;
		this.smsKey = DEFAULT_SMS_KEY;
		return smsContent;
	}

	public void setSmsContent(String smsContent) {
		this.smsContent = smsContent;
	}

	public String getEmailTitleKey() {
		if (null == emailTitle || emailTitle.trim().length() == 0) {
			this.emailTitleKey = DEFAULT_EMAIL_TITLE_KEY;
		}
		return emailTitleKey;
	}

	public void setEmailTitleKey(String emailTitleKey) {
		this.emailTitleKey = emailTitleKey;
	}

	public String getEmailContentKey() {
		if (null == emailContent || emailContent.trim().length() == 0) {
			this.emailContentKey = DEFAULT_EMAIL_CONTENT_KEY;
		}
		return emailContentKey;
	}

	public void setEmailContentKey(String emailContentKey) {
		this.emailContentKey = emailContentKey;
	}

	public String getJabberKey() {
		if (null == jabberContent || jabberContent.trim().length() == 0) {
			this.jabberKey = DEFAULT_JABBER_KEY;
		}
		return jabberKey;
	}

	public void setJabberKey(String jabberKey) {
		this.jabberKey = jabberKey;
	}

	public String getSmsKey() {
		if (null == smsContent || smsContent.trim().length() == 0) {
			this.smsKey = DEFAULT_SMS_KEY;
		}
		return smsKey;
	}

	public void setSmsKey(String smsKey) {
		this.smsKey = smsKey;
	}

	public String getAppContent() {
		this.appContent = ProcessServiceImpl.DEFAULT_APP_CONTENT;
		this.appKey = DEFAULT_APP_KEY;
		return appContent;
	}

	public void setAppContent(String appContent) {
		this.appContent = appContent;
	}

	public String getAppKey() {
		if (null == appContent || appContent.trim().length() == 0) {
			this.appKey = DEFAULT_APP_KEY;
		}
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

}
