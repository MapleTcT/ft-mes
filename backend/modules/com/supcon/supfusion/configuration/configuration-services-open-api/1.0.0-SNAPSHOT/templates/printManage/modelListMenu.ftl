<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="X-UA-Compatible" content="IE=8"/>
	
	<style type="text/css">
		.accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		.accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		.accordion_pane li.dragout{color:#BBBBBB;cursor:default;}	
		.accordion_pane li.uselist{color:#40E0D0;}		
	</style>
	
</head>
<body>
		<!-- 左侧 -->
		<div class="accordion_pane" style="display:block;overflow:auto;height:auto;width:auto;margin:5px;">
			<ul class="main_properties_container">
				<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
					<#assign properties = (subs.properties)>
					<#list properties as p>
						<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'id' && (p.name) != 'version'>
						<li class="uselist" source='main' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'     seniorSystemCode='${(p.seniorSystemCode!false)?string('true','false')}' isCustom='${(p.isCustom!false)?string('true','false')}'>
							<input type="checkbox" name='${(p.name)!}' value='${(p.code)!}' class="choosedBox">${getHtmlText('${(p.displayName)!}')}
						</li>
						</#if>	
					</#list>
				</#if>
				<#if subs?? && (subs.associatedInfos)??>
					<#assign associatedInfos = (subs.associatedInfos)>
					<#assign i = 1>
					<#list associatedInfos as ass>
						<li source='test' partDepend='common' assTar='${ass.targetProperty.code!}' assPropertyName='${ass.targetProperty.name!}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}' isCustom='${(ass.originalProperty.isCustom!false)?string('true','false')}'>
							<img sourceType='list' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='showAssPropertyMenu(this,"${(ass.targetProperty.model.code)!}","${(ass.originalProperty.name)!}")'></img>
							<input type="checkbox" name='${(ass.originalProperty.name)!}' value="${ass.originalProperty.code!}" class="choosedBox" onclick="checkboxNotnullProperty('${(ass.originalProperty.name)!}',this)"> ${getHtmlText('${(ass.originalProperty.displayName)!}')}     <#--[${getHtmlText('${(ass.targetProperty.model.name)!}')}]-->
						</li>	
						<#assign i = i+1>
					</#list>
				</#if>
			</ul>
		</div>
		
	<script type="text/javascript">
		/**
		 * 显示关联模块属性
		 * 
		 * @returns
		 */
		function showAssPropertyMenu(obj,modelCode,modelName) {	
			if(CUI(obj).attr('hasclick')!='true'&&CUI(obj).attr('hasclick')!=true){
				// 第一次点击展开进入的时候，通过ajax查出属性的关联实体的属性
				try{
		    		var oResponse;
		    		CUI.ajax({
		    			url: "/msService/ec/printManage/selectMenuSub?isProj=false",
		    			type: 'post',
		    			async: false,
		    			data: {
		    				"modelCode" : modelCode,
		    				"type" : $(obj).attr('sourceType')
		    			},
		    			success: function(jsonResponse) {
		    				oResponse = jsonResponse;
		    				if(oResponse == "undefined") {
		    					alert("${getText('js.ec.corresponding.attribute')}");
		    					return false;
		    				}
		    				if(window.closeLoadPanel){closeLoadPanel();}
		    			}
		    		});
		    		try
		    		{
		    			createAsspropertyNodeMenu(obj, oResponse, modelName);
		    		}
		    		catch(e)
		    		{
		    		}
		    		CUI(obj).attr('hasclick', true);
		    		CUI(obj).attr('flag', false);
		    		$(obj).next().prop('checked',true);
		    		obj.src="/bap/static/treeview/assets/ectree_expand.gif";
				}catch (e){
					return false;
				}
			}else{
				if(CUI(obj).attr('flag')=='true' || CUI(obj).attr('flag')==true){
					obj.src="/bap/static/treeview/assets/ectree_expand.gif";
					$(obj).parent().next().show();
		    		CUI(obj).attr('flag', false);
				}else{
					obj.src="/bap/static/treeview/assets/ectree_colse.gif";
					$(obj).parent().next().hide();
		    		CUI(obj).attr('flag', true);
				}
			}
		}
		
		
		
	/**
	 * 创建关联属性
	 * 
	 * @returns
	 */
	 function createAsspropertyNodeMenu(obj, oResponse, modelName){
		var objMe = this;
		var objUl = $("<ul id=\"ass\" style=\"margin-left:25px;\"></ul>");
		var sourcePropertyName =$(obj.parentNode).attr('name');// 源属性名称
		var parPartDepend = $(obj.parentNode).attr('partDepend');
		var parLevel = '';
		var assModelCode = '';
		if($(obj.parentNode).attr("sourcePropertyName") && $(obj.parentNode).attr("sourcePropertyName")!=""){
			sourcePropertyName =$(obj.parentNode).attr("sourcePropertyName")+"-"+sourcePropertyName;// 源属性名称
		}
		if($(obj.parentNode).attr("relativename") && $(obj.parentNode).attr("relativename")!=""){
			parLevel =$(obj.parentNode).attr("relativename");
		}
		if($(obj.parentNode).attr("assModelCode") && $(obj.parentNode).attr("assModelCode")!=""){
			assModelCode =$(obj.parentNode).attr("assModelCode");
		}
		var parModelDataType = $(obj.parentNode).attr("parentModelDataType");
		var properties = oResponse.properties;
		var associatedInfos = oResponse.associatedInfos;
		var oneToManyAssociatedInfos = oResponse.oneToManyAssociatedInfos;
		var reverseAssociatedInfos = oResponse.reverseAssociatedInfos;
		var checkedModelCodeStr = $("#printTemplateModelSelected_formsysId").val();
		
		if(properties != null && properties.length > 0) {
			for(var i in properties) {
				var deValue="";
				if(properties[i].defaultValue!=null){
					deValue=properties[i].defaultValue;
				}
				
				var commaStr = ",";
				if(checkedModelCodeStr.split(",").length == 1 || checkedModelCodeStr.split(",")[checkedModelCodeStr.split(",").length - 1] == $(obj).parent().attr('propertyCode')+'||'+properties[i].code)
				{
					commaStr = "";
				}
				
				var checkedStr = "";
				if(checkedModelCodeStr.indexOf($(obj).parent().attr('propertyCode')+'||'+properties[i].code + commaStr) != -1)
				{
					checkedStr = "checked='checked'"
				}
				
				if($(obj).attr('sourceType') == "list" && properties[i].isUsedForList) {
					if(properties[i].type == "ENUMERATE" || properties[i].type == 'SYSTEMCODE') {
						if(parPartDepend && parPartDepend == 'one2many') {
							var objLi = $("<li none='true' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' partDepend='one2many' source='main' namekey='" + properties[i].displayName + "' assModelCode='" + assModelCode + "' mnecode='" + properties[i].isUsedMneCode + "' propDefaultValue='" + deValue +"' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' assTar='" + $(obj.parentNode).attr('assTar') + "' assOrg='" + $(obj.parentNode).attr('assOrg') + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"'  nullable='true' list='true' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + " value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
						} else {
							var objLi = $("<li none='true' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' source='main' namekey='" + properties[i].displayName + "' assModelCode='" + assModelCode + "' mnecode='" + properties[i].isUsedMneCode+ "' propDefaultValue='" + deValue + "' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' assTar='" + $(obj.parentNode).attr('assTar') + "' assOrg='" + $(obj.parentNode).attr('assOrg') + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"' nullable='true' list='true' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + " value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
							objLi.addClass("uselist");
						}
					} else {
						if(parPartDepend && parPartDepend == 'one2many') {
							var objLi = $("<li source='main' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' propPrecision='"+properties[i].decimalNum+"' partDepend='one2many' namekey='" + properties[i].displayName + "'assModelCode='" + assModelCode + "' mnecode='" + properties[i].isUsedMneCode+ "' propDefaultValue='" + deValue + "' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' assTar='" + $(obj.parentNode).attr('assTar') + "' assOrg='" + $(obj.parentNode).attr('assOrg') + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"'  nullable='true' list='true' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + " value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
						} else {
							var objLi = $("<li source='main' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' propPrecision='"+properties[i].decimalNum+"' namekey='" + properties[i].displayName + "'assModelCode='" + assModelCode + "' mnecode='" + properties[i].isUsedMneCode+ "' propDefaultValue='" + deValue + "' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' assTar='" + $(obj.parentNode).attr('assTar') + "' assOrg='" + $(obj.parentNode).attr('assOrg') + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"'  nullable='true' list='true' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + " value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
							objLi.addClass("uselist");
						}	
					}
				}else {
					if(properties[i].type == "ENUMERATE" || properties[i].type == 'SYSTEMCODE') {
						if(parPartDepend && parPartDepend == 'one2many') {
							var objLi = $("<li none='true' modelDataType='" + parModelDataType + "' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' source='main' partDepend='one2many' namekey='" + properties[i].displayName + "' mnecode='" + properties[i].isUsedMneCode+ "' propDefaultValue='" + deValue + "' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"' nullable='true' list='false' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + "  value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
						} else {
							var objLi = $("<li none='true' modelDataType='" + parModelDataType + "' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' source='main' namekey='" + properties[i].displayName + "' mnecode='" + properties[i].isUsedMneCode+ "' propDefaultValue='" + deValue + "' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"' nullable='true' list='false' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + "  value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
						}	
					} else {
						if(parPartDepend && parPartDepend == 'one2many') {
							var objLi = $("<li source='main' modelDataType='" + parModelDataType + "' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' propPrecision='"+properties[i].decimalNum+"' partDepend='one2many' namekey='" + properties[i].displayName + "' mnecode='" + properties[i].isUsedMneCode+ "' propDefaultValue='" + deValue + "' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"'  nullable='true' list='false' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + "  value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
						} else {
							var objLi = $("<li source='main' modelDataType='" + parModelDataType + "' propShowType='"+properties[i].fieldType+"' propShowFormat='"+properties[i].format+"' propPrecision='"+properties[i].decimalNum+"' namekey='" + properties[i].displayName + "' mnecode='" + properties[i].isUsedMneCode+ "' propDefaultValue='" + deValue + "' multable='" + properties[i].multable + "' modelcode='" + properties[i].model.code + "' propertycode='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' layRec='" + parLevel + "-" + properties[i].name + "' entityCode='" + $(obj.parentNode).attr('entityCode') + "' name='" + $(obj.parentNode).attr('name') + "." + properties[i].name+"' sourcePropertyName='"+sourcePropertyName+"' columnType='"+ properties[i].type +"' nullable='true' list='false' fillContent='" + properties[i].fillcontent + "'>" + 
									"<input type='checkbox' name='' " + checkedStr + "  value='" + $(obj).parent().attr('propertyCode')+'||'+properties[i].code + "' class='choosedBox'>" + properties[i].displayNameInternational + "</li>");
						}	
					}
				}
				objUl.append(objLi);
			}
		}
		if(associatedInfos != null && associatedInfos.length > 0) {
			var num = 1;
			$.each(associatedInfos, function(ass, item){
				if(parPartDepend && parPartDepend == 'one2many') {
					var objLi = $("<li source='test' parentModelDataType='" + item.targetProperty.model.dataType + "' partDepend='one2many' assTar='"+ item.targetProperty.code + "' propertyCode='"+$(obj).parent().attr('propertyCode')+'||'+item.originalProperty.code+ "' assOrg='"+ item.originalProperty.code + "' dbname='" + item.targetProperty.model.modelName + "' sourcePropertyName='"+sourcePropertyName+"' name='" + $(obj.parentNode).attr('name') + "." + item.originalProperty.name + "' entityCode='" + item.targetProperty.model.code + "' relativeName='" + parLevel + "-" + item.targetProperty.model.tableName +","+ item.targetProperty.columnName+","+ item.originalProperty.model.tableName +","+ item.originalProperty.columnName +"' assModelCode='"+ item.originalProperty.model.code +"'></li>");
				} else {
					var objLi = $("<li source='test' parentModelDataType='" + item.targetProperty.model.dataType + "' assTar='"+ item.targetProperty.code + "' propertyCode='"+$(obj).parent().attr('propertyCode')+'||'+item.originalProperty.code+ "' assOrg='"+ item.originalProperty.code + "' dbname='" + item.targetProperty.model.modelName + "' sourcePropertyName='"+sourcePropertyName+"' name='" + $(obj.parentNode).attr('name') + "." + item.originalProperty.name + "' entityCode='" + item.targetProperty.model.code + "' relativeName='" + parLevel + "-" + item.targetProperty.model.tableName +","+ item.targetProperty.columnName+","+ item.originalProperty.model.tableName +","+ item.originalProperty.columnName +"' assModelCode='"+ item.originalProperty.model.code +"'></li>");
				}
				objUl.append(objLi);
				var objImg = $("<img sourceType='" + $(obj).attr('sourceType') + "' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false'></img>");
				objLi.append(objImg);
				// 复选框设置 非空对象默认勾选 只支持第二层模型
				var checked = "";
				var model = $('input[name='+modelName+']');
//				if (model[0].checked && !item.originalProperty.nullable && checkedModelCodeStr == "") {
//					checked = "checked='checked'";
//				}
				
				var commaStr = ",";
				if(checkedModelCodeStr.split(",").length == 1 || checkedModelCodeStr.split(",")[checkedModelCodeStr.split(",").length - 1] == $(obj).parent().attr('propertyCode')+'||'+item.originalProperty.code)
				{
					commaStr = "";
				}

				var checkedStr = "";
				if(checkedModelCodeStr.indexOf($(obj).parent().attr('propertyCode')+'||'+item.originalProperty.code  + commaStr) != -1)
				{
					checkedStr = "checked='checked'";
				}else if(checkedModelCodeStr==""&&item.originalProperty.nullable==false){
					checkedStr = "checked='checked'";
				}
				
				var checkbox = $("<input type='checkbox' " + checkedStr + "  name='' nullable='" + (!item.originalProperty.nullable) + "' value='" + $(obj).parent().attr('propertyCode')+'||'+item.originalProperty.code + "' " + checked + " class='choosedBox'>");
				objLi.append(checkbox);
				//objLi.append("<span>" + item.originalProperty.displayNameInternational + "[" + item.targetProperty.model.nameInternational + "]</span>");
				objLi.append("<span>" + item.originalProperty.displayNameInternational + "</span>");
				objImg.bind('click', function(){
					showAssPropertyMenu(this,item.targetProperty.model.code,'');
				});
				num++;
			});
		}
		$(obj.parentNode).after(objUl);
	}

	function checkboxNotnullProperty(parentName, obj){
		$.each($("li[name^='"+parentName+".']"),function(index, item){
			if(!$(obj).is(":checked")){
				$(item).find("input:checkbox").attr("checked",false);
			}
		})
	}
	</script>	
</body>
</html>
