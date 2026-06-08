package com.supcon.supfusion.configuration.workflow.activity;

import com.supcon.supfusion.configuration.services.exceptions.EcException;
import com.supcon.supfusion.configuration.services.entity.View;
import com.supcon.supfusion.configuration.services.service.ViewService;
import com.supcon.supfusion.configuration.workflow.handlers.GetUserListHandler;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.env.EnvironmentImpl;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

import java.util.List;

/**
 * 会签活动解析.
 * 
 * @author songjiawei
 * 
 */
public class CountersignBinding extends AbstractNoticeBinding {
	public static final String TAG = "countersign";

	public CountersignBinding() {
		super(TAG);
	}

	@Override
	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
		CountersignActivity activity = new CountersignActivity();


		// ////////自定义解析///////////////
		TaskCommonProperty tcp = new TaskCommonProperty();
		String taskName = XmlUtil.attribute(element, "name");
		tcp.setName(taskName);

		String customParam = XmlUtil.attribute(element, "customParam");
		if (null != customParam) {
			tcp.setCustomParam(customParam);
		}

		Element openActionElement = XmlUtil.element(element, "open-action");
		if (null != openActionElement) {
			EnvironmentImpl env = EnvironmentImpl.getCurrent();
			ViewService viewService = env.get(ViewService.class);
			if (null == viewService) {
				throw new EcException("can not find viewService.");
			}
			//String url = XmlUtil.attribute(openActionElement, "url");
			String viewCode=XmlUtil.attribute(openActionElement, "viewCode");
			if(viewCode!=null&&!viewCode.equals("")){
				View v=viewService.getView(viewCode);
				if(v==null){
					v=viewService.getView(viewCode);
				}
				String url="";
				if(v!=null){
					url=v.getUrl();
					if(customParam!=null&&!customParam.equals("")){
						if(url.indexOf("?")>-1){
							url+="&"+customParam;
						}else{
							url+="?"+customParam;
						}
					}
				}


				tcp.setOpenUrl(url);
			}

		}
		//是否可填写处理意见
		String dealSet = XmlUtil.attribute(element, "dealSet");
		if (null != dealSet&&dealSet.equals("0")) {
			tcp.setDealSet(0);
		}else if (null != dealSet&&dealSet.equals("1")){
			tcp.setDealSet(1);
		}else if (null != dealSet&&dealSet.equals("2")){
			tcp.setDealSet(2);
		}


		tcp.setDescription(element.getAttribute("internationalKey"));// 描述
		String bulkDealFlag = XmlUtil.attribute(element, "bulkDealFlag");
		if (null != bulkDealFlag&&bulkDealFlag.equals("true")) {
			tcp.setBulkDealFlag(Boolean.parseBoolean(bulkDealFlag));
		}
		String webSignetFalg = XmlUtil.attribute(element, "webSignetFalg");
		if (null != webSignetFalg&&webSignetFalg.equals("true")) {
			tcp.setWebSignetFalg(Boolean.parseBoolean(webSignetFalg));
		}
		String recallAble = XmlUtil.attribute(element, "recallAble");
		if (null != recallAble) {
			tcp.setRecallAble(Boolean.parseBoolean(recallAble));
		}

		String mobileApprove = XmlUtil.attribute(element, "mobileApprove"); //是否支持移动端审批
		if("true".equalsIgnoreCase(mobileApprove)){
			tcp.setMobileApprove(Boolean.TRUE);
		}

		// 解析GetUserHandler,自行解析，避免反射
		Element handlerE = XmlUtil.element(element, "assignment-handler");
		if (null != handlerE) {
			GetUserListHandler handler = new GetUserListHandler();
			List<Element> elements = XmlUtil.elements(handlerE, "field");
			if (!elements.isEmpty()) {
				for (Element e : elements) {
					if ("inputorFlag".equals(e.getAttribute("name"))) {
						handler.setInputorFlag(XmlUtil.element(e, "string").getAttribute("value"));
					}
					if ("leaderFlag".equals(e.getAttribute("name"))) {
						handler.setLeaderFlag(XmlUtil.element(e, "string").getAttribute("value"));
					}
					if ("bigLeaderFlag".equals(e.getAttribute("name"))) {
						handler.setBigLeaderFlag(XmlUtil.element(e, "string").getAttribute("value"));
					}
					if ("flowDealFlag".equals(e.getAttribute("name"))) {
						handler.setFlowDealFlag(XmlUtil.element(e, "string").getAttribute("value"));
					}
					if ("activityDealFlag".equals(e.getAttribute("name"))) {
						handler.setActivityDealFlag(XmlUtil.element(e, "string").getAttribute("value"));
					}
					if ("attentFlag".equals(e.getAttribute("name"))) {
						handler.setAttentFlag(XmlUtil.element(e, "string").getAttribute("value"));
					}

					if ("staffIds".equals(e.getAttribute("name"))) {
						handler.setStaffIds(XmlUtil.element(e, "string").getAttribute("value"));
					}
				}
			}
			tcp.setAssignmentHandler(handler);
		}

		Integer loop = XmlUtil.attributeInteger(element, "loop", parse);
		//FIXME
		activity.setLoop(null == loop ? 0 : loop.intValue());

		activity.setTcp(tcp);
		activity.setNoticeTransit(parseNotice(element, parse, parser));// 解析消息提醒
		return activity;
	}
}
