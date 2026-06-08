<#-- 属性树 -->
<div id="form_select_elements">
	<div class="accordion_pane" style="display:block;overflow:auto;height:100%">
		<ul class="main_properties_container">
			<#list model.properties as p>
				<#if p.type == 'OBJECT' && ((p.associatedProperty.model.code)!) == modelCode>
				<#else>
					<#if (p.name) != "extraCol" && (p.name) != "tableInfoId" && (p.name) != "id" && (p.name) != "version">
						<li source='<#if p.type == "OFFICE">office<#else>main</#if>' name='${model.modelName?uncap_first}.${p.name}' onclick='propertyClickFunc(this);' ondblclick='ec.config.relationModel.propertyDblClickFunc(this);' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}'  propnullable='${(p.nullable!false)?string('true','false')}' multable='${(p.multable!false)?string('true','false')}' fillContent='${(p.fillcontent)!}' propPrecision='${(p.decimalNum)!}'
						<#if (model.associatedInfos)?? && !(useMethod!false)>
							<#assign assFlag = false>
							<#list (model.associatedInfos) as associatedInfo>
								<#if (associatedInfo.type)?has_content && ((associatedInfo.type) == 1 || (associatedInfo.type) == 2) && (associatedInfo.originalProperty.code)?? && (associatedInfo.originalProperty.code) == (p.code)>
								<#assign assFlag = true>
								modelcode = '${(associatedInfo.targetProperty.model.code)!}' objectPropertyNullable='${(p.nullable!false)?string('true','false')}'><img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag=false hasclick=false onclick='showAssProperty("${(p.code)!}", "${(associatedInfo.targetProperty.model.code)!}", this, "${(model.modelName?uncap_first)!}.${(p.name)!}", "${(p.name)!}")'></img>
								</#if>
							</#list>
							<#if assFlag?? && !assFlag>
							>
							</#if>
						<#else>
						>
						</#if>
						${getHtmlText('${(p.displayName)}')}
						</li>
					</#if>
				</#if>
			</#list>
		</ul>
	</div>
</div>
<script type="text/javascript">
	/**
	 * 显示关联属性
	 * @returns
	 */
	function showAssProperty(propertyCode, modelCode, obj, modelName, propertyName) {
		if( String(CUI(obj).attr('hasclick')) != 'true'){
			// 第一次点击展开进入的时候，通过ajax查出属性的关联实体的属性
			try{
				var propArr = propertyCode.split("||");
	    		var oResponse;
	    		CUI.ajax({
	    			url: "/msService/ec/view/AssociatedProperty",
	    			type: 'post',
	    			async: false,
	    			data: "propertyCode=" + propArr[propArr.length - 1] + "&modelCode=" + modelCode,
	    			success: function(jsonResponse) {
	    				oResponse = jsonResponse;
	    				if(window.closeLoadPanel){closeLoadPanel();}
	    			}
	    		});
	    		CUI(obj).attr('hasclick', true);
	    		createAsspropertyNode(obj, oResponse, modelName, propertyName, CUI(obj).parent().attr('modelcode'), propertyCode);
	    		obj.src = "/bap/static/treeview/assets/ectree_expand.gif";
			}catch (e){
				return false;
			}
		} else {
			if ( String(CUI(obj).attr('flag')) == 'true' ) {
				obj.src = "/bap/static/treeview/assets/ectree_expand.gif";
				$(obj).parent().next().show();
	    		CUI(obj).attr('flag', false);
			} else {
				obj.src = "/bap/static/treeview/assets/ectree_colse.gif";
				$(obj).parent().next().hide();
	    		CUI(obj).attr('flag', true);
			}
		}
	}
	
	/**
	 * 创建关联属性
	 * @returns
	 */
	function createAsspropertyNode(obj, oResponse, modelName, propertyName, paramModelCode, parentCode){
		var objUl = $('<ul id="ass_' + propertyName + '" style="margin-left:25px;width:100%"></ul>');
		$(obj.parentNode).after(objUl);
		var sourcePropertyName = $(obj.parentNode).attr('name');// 源属性名称
		if($(obj.parentNode).attr("sourcePropertyName") && $(obj.parentNode).attr("sourcePropertyName") != ""){
			sourcePropertyName = $(obj.parentNode).attr("sourcePropertyName") + "-" + sourcePropertyName;// 源属性名称
		}
		var objectPropertyNullable = null;
		if($(obj.parentNode).attr("objectPropertyNullable") != undefined && $(obj.parentNode).attr("objectPropertyNullable") != ''){
			objectPropertyNullable = $(obj.parentNode).attr("objectPropertyNullable");
		}
		$.each(oResponse, function(i, item){
			if (item.type == 'OBJECT' && item.associatedProperty.model.code == "${modelCode}") {
				return true;
			}
			var dfvalue = "";
			if(item.defaultValue!=null){
				dfvalue = item.defaultValue;
			}
			if(item.name!="extraCol" && item.name!="tableInfoId"){
				if(item.type == 'OBJECT'){
					var objLi = $('<li source="main" sourcePropertyName="' + sourcePropertyName + '" onclick="propertyClickFunc(this);" ondblclick="ec.config.relationModel.propertyDblClickFunc(this);" propertyCode="' + parentCode + '||' + item.code + '" namekey="' + item.displayName + '" name="' + modelName + '.' + item.name + '" nullable="' + item.nullable + '" multable="' + item.multable + '" columnType="' + item.type + '" propPrecision="' + item.decimalNum + '" propShowFormat="' + item.format + '" propShowType="' + item.fieldType + '" modelcode="' + paramModelCode + '" fillContent="' + item.fillcontent + '"><img align="absmiddle" src="/bap/static/treeview/assets/ectree_colse.gif" flag=false hasclick=false></img>' + item.displayNameInternational + '</li>');
					var objImg = $("img", objLi);
					objImg.bind('click', function(){
						showAssProperty(parentCode + "||" + item.code, item.model.code, this, (modelName + "." + item.name), item.name);
					});
				} else {
					var objLi = $('<li source="main" sourcePropertyName="' + sourcePropertyName + '" onclick="propertyClickFunc(this);" ondblclick="ec.config.relationModel.propertyDblClickFunc(this);" propertyCode="' + parentCode + '||' + item.code + '" namekey="' + item.displayName + '" name="' + modelName + '.' + item.name + '" nullable="' + item.nullable + '" multable="' + item.multable + '" columnType="' + item.type + '" propPrecision="' + item.decimalNum + '" propShowFormat="' + item.format + '" propShowType="' + item.fieldType + '" modelcode="' + paramModelCode + '" fillContent="' + item.fillcontent + '">' + item.displayNameInternational + '</li>');
				}
				objUl.append(objLi);
			}
		});
	}
	
	function propertyClickFunc(obj){
		if (obj) {
			var asso = $('img', obj);
			if(asso && asso.length > 0) {
				return false;
			}
			$('#form_select_elements li').css('background','none');
			$('#form_select_elements li').attr('isSelected','0');
			$(obj).css('background','#FFF5C2');
			$(obj).attr('isSelected','1');
			return true;
		}
	}
</script>