package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

public abstract class AbstractNoticeBinding extends JpdlBinding {
	public AbstractNoticeBinding(String tagName) {
		super(tagName);
	}

	/**
	 * 
	 * @param element
	 * @param parse
	 * @param parser
	 * @return
	 */
	public NoticeTransit parseNotice(Element element, Parse parse, JpdlParser parser) {
		Element noticeElement = XmlUtil.element(element, "notice");
		if (null != element) {
			NoticeTransit noticeTransit = new NoticeTransit();

			Element emailElement = XmlUtil.element(noticeElement, "email");
			Element jabberElement = XmlUtil.element(noticeElement, "jabber");
			Element smsElement = XmlUtil.element(noticeElement, "sms");

			if (null != emailElement) {
				Element emailTitleElement = XmlUtil.element(emailElement, "title");
				Element emailContentElement = XmlUtil.element(emailElement, "content");
				if (null != emailTitleElement) {
					noticeTransit.setEmailTitle(emailTitleElement.getTextContent());
				}
				if (null != emailContentElement) {
					noticeTransit.setEmailContent(emailContentElement.getTextContent());
				}
			}
			if (null != jabberElement) {
				Element jabberContentElement = XmlUtil.element(jabberElement, "content");
				if (null != jabberContentElement) {
					noticeTransit.setJabberContent(jabberContentElement.getTextContent());
				}
			}
			if (null != smsElement) {
				Element smsContentElement = XmlUtil.element(smsElement, "content");
				if (null != smsContentElement) {
					noticeTransit.setSmsContent(smsContentElement.getTextContent());
				}
			}
			return noticeTransit;
		}
		return null;
	}

}

/*
 * <notice enable="true"> <email> <title></title> <content></content> </email>
 * <jabber> <content></content> </jabber> <sms> <content></content> </sms>
 * </notice>
 */