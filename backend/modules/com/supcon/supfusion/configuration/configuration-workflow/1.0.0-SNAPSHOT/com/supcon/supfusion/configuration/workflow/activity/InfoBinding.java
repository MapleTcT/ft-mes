package com.supcon.supfusion.configuration.workflow.activity;

import org.jbpm.jpdl.internal.activity.JpdlBinding;
import org.jbpm.jpdl.internal.xml.JpdlParser;
import org.jbpm.pvm.internal.util.XmlUtil;
import org.jbpm.pvm.internal.xml.Parse;
import org.w3c.dom.Element;

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
public class InfoBinding extends JpdlBinding {
	// ~ Instance fields =======================================================
	public final static String TAG = "info";

	// ~ Constructor ===========================================================
	public InfoBinding() {
		super(TAG);
	}

	// ~ Methods ===============================================================
	
	@Override
	public Object parseJpdl(Element element, Parse parse, JpdlParser parser) {
		return null;
	}

}
