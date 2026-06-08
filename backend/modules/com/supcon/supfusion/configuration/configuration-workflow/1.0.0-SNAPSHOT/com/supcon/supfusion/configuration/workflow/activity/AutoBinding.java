package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.wire.Descriptor;
import org.jbpm.pvm.internal.wire.descriptor.ArgDescriptor;
import org.jbpm.pvm.internal.wire.descriptor.ListDescriptor;
import org.jbpm.pvm.internal.wire.descriptor.ObjectDescriptor;
import org.jbpm.pvm.internal.wire.usercode.UserCodeReference;
import org.jbpm.pvm.internal.wire.xml.WireParser;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

import java.util.*;

/**
<auto>
  <auto-bean (beanName|class)="" method="" var=""><arg /><arg /></auto-bean>
  <auto-java class="" method="" var="" />
  <auto-script lang="">script content.</auto-script>
  <auto-hql query="" var="" unique="">
  	<parameters>
      <string name="activityName" value="aaa" />
      <int name="activityName" value="1" />
      <long  name="activityName" value="1" />
      <float  name="activityName" value="13.2" />
      <double name="activityName" value="1.2" />
      <true />
      <false />
      <object name="activityName" value="1" />
    </parameters>
  </auto-hql>
  <auto-sql query="" var="" unique="">
  	<parameters>
      <string name="activityName" value="aaa" />
      <int name="activityName" value="1" />
      <long  name="activityName" value="1" />
      <float  name="activityName" value="13.2" />
      <double name="activityName" value="1.2" />
      <true />
      <false />
      <object name="activityName" value="1" />
    </parameters>
  </auto-sql>
</auto>
 **/

/**
 * 
 * @author 宋佳维
 * 
 */
public class AutoBinding extends JpdlBinding {
	// ~ Instance fields =======================================================
	public final static String TAG = "auto";

	// ~ Constructor ===========================================================
	public AutoBinding() {
		super(TAG);
	}

	// ~ Methods ===============================================================
	private void parseJavaInvocation(JavaTransit javaTransit, Element element, Parse parse, JpdlParser parser) {
		UserCodeReference invocationReference = parser.parseUserCodeReference(element, parse);
		javaTransit.setInvocationReference(invocationReference);
		ObjectDescriptor objectDescriptor = (ObjectDescriptor) invocationReference.getDescriptor();
		javaTransit.setArgDescriptors(objectDescriptor.getArgDescriptors());
		objectDescriptor.setArgDescriptors(null);
		javaTransit.setMethodName(objectDescriptor.getMethodName());
		objectDescriptor.setMethodName(null);
	}

	private void parseBeanInvocation(BeanTransit beanTransit, Element element, Parse parse, JpdlParser parser) {

		String className = XmlUtil.attribute(element, "class");
		String beanName = XmlUtil.attribute(element, "beanName");
		if (className != null) {
			beanTransit.setBeanClass(className);
			if (beanName != null) {
				parse.addProblem("bean class和beanName不能同时设置.");
			}
		} else if (beanName != null) {
			beanTransit.setBeanName(beanName);
		} else {
			parse.addProblem("bean的class或者beanName必须存在一个.");
		}
		if (element.hasAttribute("method")) {
			beanTransit.setMethodName(element.getAttribute("method"));
			List<Element> argElements = XmlUtil.elements(element, "arg");
			if (!argElements.isEmpty()) {
				List<ArgDescriptor> argDescriptors = WireParser.getInstance().parseArgs(argElements, parse);
				beanTransit.setArgDescriptors(argDescriptors);
			}
		} else {
			parse.addProblem("bean的method必须设置.");
		}
	}

	@Override
	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
		AutoActivity autoActivity = new AutoActivity();

		/* 此举是为了即使N个不同类型的要执行的目标也能按照既定的顺序执行. */
		Set<String> allowedTagNames = new HashSet<String>(5);
		allowedTagNames.add("auto-bean");
		allowedTagNames.add("auto-java");
		allowedTagNames.add("auto-script");
		allowedTagNames.add("auto-hql");
		allowedTagNames.add("auto-sql");
		List<Element> targetElements = XmlUtil.elements(element, allowedTagNames);

		if (!targetElements.isEmpty()) {// 不可能为null,所以无须判断
			List<Object> targetTransits = new LinkedList<Object>();
			JavaTransit javaTransit;
			BeanTransit beanTransit;
			ScriptTransit scriptTransit;
			HqlTransit hqlTransit;
			SqlTransit sqlTransit;
			for (Element e : targetElements) {
				/* bean */
				if (e.getTagName().equals("auto-bean")) {
					beanTransit = new BeanTransit();
					if (null != e.getAttribute("method")) {
						parseBeanInvocation(beanTransit, e, parse, parser);
					}
					String variableName = XmlUtil.attribute(e, "var");
					beanTransit.setVariableName(variableName);
					targetTransits.add(beanTransit);
					beanTransit = null;
				}
				/* java */
				if (e.getTagName().equals("auto-java")) {
					javaTransit = new JavaTransit();
					if (null != e.getAttribute("method")) {
						parseJavaInvocation(javaTransit, e, parse, parser);
					}
					String variableName = XmlUtil.attribute(e, "var");
					javaTransit.setVariableName(variableName);
					targetTransits.add(javaTransit);
					javaTransit = null;
				}
				/* script */
				if (e.getTagName().equals("auto-script")) {
					scriptTransit = new ScriptTransit();
					scriptTransit.setLang(XmlUtil.attribute(e, "lang"));
					scriptTransit.setVariableName(XmlUtil.attribute(e, "var"));
					scriptTransit.setScript(XmlUtil.attribute(e, "code"));
					targetTransits.add(scriptTransit);
//					String content = XmlUtil.getContentText(e);
//					if (content != null && !content.trim().equals("")) {
//						scriptTransit = new ScriptTransit();
//						scriptTransit.setLang(XmlUtil.attribute(e, "lang"));
//						scriptTransit.setVariableName(XmlUtil.attribute(e, "var"));
//						scriptTransit.setScript(content.trim());
//						targetTransits.add(scriptTransit);
//						//scriptTransit = null;
//					}
				}
				/* hql */
				if (e.getTagName().equals("auto-hql")) {
					String query = XmlUtil.attribute(e, "query");
					if (query != null) {
						hqlTransit = new HqlTransit();
						hqlTransit.setQuery(query);
						hqlTransit.setVariableName(XmlUtil.attribute(e, "var"));
						Boolean unique = XmlUtil.attributeBoolean(e, "unique", parse);
						hqlTransit.setUnique(unique != null ? unique.booleanValue() : false);// 默认不为unique
						List<Element> paramElements = XmlUtil.elements(e);
						if (!paramElements.isEmpty()) {
							List<Descriptor> parametersDescriptor = new ArrayList<Descriptor>();
							for (Element paramElement : paramElements) {
								WireParser wireParser = WireParser.getInstance();
								Descriptor paramDescriptor = (Descriptor) wireParser.parseElement(paramElement, parse, WireParser.CATEGORY_DESCRIPTOR);
								parametersDescriptor.add(paramDescriptor);
							}
							ListDescriptor parametersListDescriptor = new ListDescriptor();
							parametersListDescriptor.setValueDescriptors(parametersDescriptor);
							hqlTransit.setParametersListDescriptor(parametersListDescriptor);
						}
						targetTransits.add(hqlTransit);
						hqlTransit = null;
					}
				}
				/* sql */
				if (e.getTagName().equals("auto-sql")) {
					String query = XmlUtil.attribute(e, "query");
					if (query != null) {
						sqlTransit = new SqlTransit();
						sqlTransit.setQuery(query);
						sqlTransit.setVariableName(XmlUtil.attribute(e, "var"));
						Boolean unique = XmlUtil.attributeBoolean(e, "unique", parse);
						sqlTransit.setUnique(unique != null ? unique.booleanValue() : false);// 默认不为unique
						List<Element> paramElements = XmlUtil.elements(e);
						if (!paramElements.isEmpty()) {
							List<Descriptor> parametersDescriptor = new ArrayList<Descriptor>();
							for (Element paramElement : paramElements) {
								WireParser wireParser = WireParser.getInstance();
								Descriptor paramDescriptor = (Descriptor) wireParser.parseElement(paramElement, parse, WireParser.CATEGORY_DESCRIPTOR);
								parametersDescriptor.add(paramDescriptor);
							}
							ListDescriptor parametersListDescriptor = new ListDescriptor();
							parametersListDescriptor.setValueDescriptors(parametersDescriptor);
							sqlTransit.setParametersListDescriptor(parametersListDescriptor);
						}
						targetTransits.add(sqlTransit);
						sqlTransit = null;
					}
				}
			}
			autoActivity.setTargetTransits(targetTransits);
		}

		return autoActivity;
	}

}
