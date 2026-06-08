<#-- 模型树 -->
<div id="form_select_elements">
	<div class="accordion_pane" style="display:block;overflow:auto;height:100%">
		<ul class="ass_models_container">
			<li modelname="${model.modelName}" modelcode="${model.code}" onclick="propertyClickFunc(this);" ondblclick="propertyDblClickFunc(this);">
				<#if hasAssModel?? && hasAssModel>
					<img align="absmiddle" src="/bap/static/treeview/assets/ectree_colse.gif" flag="false" hasclick="false" onclick='showAssModel(event, "${model.code}", this);'></img>
				</#if>
				${getHtmlText("${(model.name)!}")}
			</li>
		</ul>
	</div>
</div>
<script type="text/javascript" charset="UTF-8" language="javascript">
	/**
	 * 显示关联模型
	 */
	function showAssModel(event, modelCode, obj) {
		YUE.stopPropagation(event);
		if ( String(CUI(obj).attr('hasclick')) != 'true' ) {
			try{
	    		var oResponse;
	    		CUI.ajax({
	    			async   : false,
	    			url     : "/msService/ec/view/associatedModels",
	    			type    : 'POST',
	    			data    : "model.code=" + modelCode,
	    			success : function(jsonResponse) {
	    				oResponse = jsonResponse;
	    				if(window.closeLoadPanel){closeLoadPanel();}
	    			}
	    		});
	    		createAssModelNode(obj, oResponse);
	    		$(obj).attr("hasclick", true);
	    		obj.src = "/bap/static/treeview/assets/ectree_expand.gif";
			} catch (e) {
				//console.log(e);
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
	 * 创建关联模型
	 * @returns
	 */
	function createAssModelNode(obj, oResponse){
		var objUl = $('<ul style="margin-left:25px;width:100%"></ul>');
		$(obj.parentNode).after(objUl);
		$.each(oResponse, function(i, item){
			var relation = item.associatedProperty.model.tableName + ',' + item.associatedProperty.columnName + ',' + item.model.tableName + ',' + item.columnName;
			var objLi = $('<li relation="' + relation + '" propertyname="' + item.name + '" propertycode="' + item.code + '" modelname="' + item.associatedProperty.model.modelName + '" modelcode="' + item.associatedProperty.model.code + '" onclick="propertyClickFunc(this);" ondblclick="propertyDblClickFunc(this);"></li>');
			if ( hasAssoModel(item) ) {
				objLi.append('<img align="absmiddle" src="/bap/static/treeview/assets/ectree_colse.gif" flag=false hasclick=false onclick="showAssModel(event, \'' + item.associatedProperty.model.code + '\', this);"></img>');
			}
			objLi.append(item.displayNameInternational + '[' + item.associatedProperty.model.nameInternational + ']');
			objUl.append(objLi);
		});
	}
	
	function hasAssoModel(obj){
		return true;
		var flag = false;
		$.each(obj.associatedProperty.model.properties, function(i, item){
			if (item.associatedProperty && (item.isInherent == null || item.isInherent == false) 
				&& (item.associatedType == 1 || item.associatedType == 2) && item.associatedProperty.model.entity.module.code != 'sysbase_1.0') {
				flag = true;
				return false;
			}
		});
		return flag;
	}
	
	function propertyClickFunc(obj){
		if (obj) {
			$('#form_select_elements li').css('background','none');
			$('#form_select_elements li').attr('isselected','0');
			$(obj).css('background','#FFF5C2');
			$(obj).attr('isselected','1');
			return true;
		}
	}
	
	function propertyDblClickFunc(obj){
		if ( propertyClickFunc(obj) ) {
			var modelNameHtml = $.trim($(obj).text());
			if (modelNameHtml.indexOf('[') > -1 && modelNameHtml.indexOf(']') > -1) {
				modelNameHtml = modelNameHtml.substring(modelNameHtml.indexOf('[') + 1, modelNameHtml.indexOf(']'));
			}
			$('#config_custom_props_select_model').val(modelNameHtml);
			var layrec = '';
			var uls = $(obj).parent().parents('ul');
			var relations = '';
			if ( uls.length > 0 ) {
				for (var i = uls.length - 1; i >= 0; i--) {
					if (i == uls.length - 1) {
						var modelName = $(uls[i]).children('li').attr('modelname');
						modelName = modelName.charAt(0).toLowerCase() + modelName.substr(1);
						layrec += modelName;
					} else {
						layrec += '.' + $(uls[i]).children('li').attr('propertyname');
						relations += '-' + $(uls[i]).children('li').attr('relation');
					}
				}
			} else {
				var modelName = $(obj).attr('modelname');
				modelName = modelName.charAt(0).toLowerCase() + modelName.substr(1);
				layrec += modelName;
			}
			if ($(obj).attr('propertyname')) {
				layrec += '.' + $(obj).attr('propertyname');
				relations += '-' + $(obj).attr('relation');
			}
			if (relations) {
				layrec = layrec + '||' + relations.substr(1);
			}
			$('#config_custom_props_select_model').removeAttr("modelcode").attr("modelcode", $(obj).attr('modelcode'));
			$('#config_custom_props_select_model').removeAttr("layrec").attr("layrec", layrec);
		}
	}	
	
</script>