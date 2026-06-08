package com.supcon.supfusion.configuration.services.openapi.freemark;

import com.supcon.supfusion.configuration.services.i18n.InternationalResource;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HtmlUrlMethod implements TemplateMethodModelEx {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object exec(List args) throws TemplateModelException {
		String result=(String) args.get(0);
		List newArr = Collections.EMPTY_LIST;
		String regex="\\{[0-9]\\}";
		
		
		int argSize=args.size()-1;
		if(argSize>0){
			newArr = new ArrayList(argSize);
			for (int i = 1; i < args.size(); i++){
				newArr.add((String)args.get(i));
			}
			if (null != result) {
				int regexMatchSize=0;

				Pattern pattern = Pattern.compile(regex);
				Matcher matcher=pattern.matcher(result);
				while(matcher.find()){
					regexMatchSize++;
				}
				if(regexMatchSize==argSize){
					for(int i=0;i<argSize;i++){
						result=result.replaceFirst(regex, (String)newArr.get(i));
					}
				}else{
					result=new SimpleScalar("<span i18n='Wrong HtmlUrl usege!'>"+ InternationalResource.get("foundation.htmlUrl.wrongUsage")+"</span>").toString();
				}
			}
		}
		
		return result;
	}

}
