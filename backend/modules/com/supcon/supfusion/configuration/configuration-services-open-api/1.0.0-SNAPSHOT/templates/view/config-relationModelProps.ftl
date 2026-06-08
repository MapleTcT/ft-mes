<#-- Head -->
<style type="text/css">
.edv-a{border:1px #5C8CB5 solid;width:500px;_width:485px;height:45px;margin:0 auto;padding:3px;_padding-bottom:0px;}
#configContainer {margin: 3px 5px 5px 5px;}
#toolbar {background: url('/bap/static/css/edit.gif') 0 -939px repeat-x;height: 30px;line-height:23px;*line-height:30px;padding-left:15px;_padding-top:6px;}
#toolbar a {
	font:  13px normal;
	margin: 1px 1px 1px 10px;
	vertical-align: bottom;
	padding:2px 1px;
}
#toolbar a:hover{
	background-color: #FFF5C2;
	border: 1px solid #FFC100;
	margin: 1px 0 1px 9px;
	padding:1px 1px;
}
#toolbar img {
	margin-right: 2px;
	vertical-align: bottom;
}
a:link, a:visited {
	color: #000;
}
.deleteBtn {
	float:right;
	height: 24px;
	width: 13px;
	line-height: 24px;
	font-size: 12px;
	vertical-align:middle;
	cursor: pointer;
	margin: 0 0 0 3px;
	background-image: url("/bap/static/ec/images/delete.gif");
	background-repeat: no-repeat;
	background-position: left center;
}
.deleteBtnOn {
	float:right;
	height: 24px;
	width: 13px;
	line-height: 24px;
	font-size: 12px;
	vertical-align:middle;
	cursor: pointer;
	margin: 0 0 0 3px;
	background-image: url("/bap/static/ec/images/deleteon.gif");
	background-repeat: no-repeat;
	background-position: left center;
}
</style>
<div id="codeConfigDiv">
	<@errorbar id="config_relationModelPropsErrorBar"></@errorbar>
	<div id="toolbar">
		<a id="a_level_1" href="#" onclick="ec.config.relationModel.addItem1();">第一层关联模型</a>
		<a id="a_level_2" href="#" onclick="ec.config.relationModel.addItem2();">第二层关联模型</a>
	</div>
	<div id="outerContainer">
		<table id="configContainer">
			<tr id="assoModelPropsSampleTr">
				<td colspan="6" style="width:98%;">
					<textarea id="assoModelPropsSample" cols="100" rows="2" style="width:98%;color:blue;" class="cui-edit-textarea">预览：</textarea>
				</td>
			</tr>
			<tr style="display:none;" class="content_level_1">
				<td width="10%">
					<input size="5" stype="margin:0 3px;" name="separator1" value="" style="width:95%;" class="cui-edit-field" />
				</td>
				<td width="13%">
					<select id="method" style="width:95%;margin:0 3px;" onchange="ec.config.relationModel.changeMethod_level_1(this);">
						<option value="">-</option>
						<option value="COUNT">COUNT</option>
						<option value="SUM">SUM</option>
						<option value="MAX">MAX</option>
						<option value="MIN">MIN</option>
					</select>
				</td>
				<td width="20%">
					<select id ="relation_model_select" style="width:95%;margin:0 3px;" onchange="ec.config.relationModel.changeSelect_level_1();">
						<#if relationModels?? && relationModels?size gt 0>
							<#list relationModels as model>
						<option value="${model.code}">${getText("${model.name}")}</option>
							</#list>
						</#if>
					</select>
				</td>
				<td width="40%">
					<div name="property" class="fix-search-click" style="width:95%;margin:0 3px;">
						<input size="15" name="value" class="cui-edit-field" />
						<input type="button" onclick="ec.config.relationModel.selectProperty(event, this);" class="cui-search-click" />
					</div>
				</td>
				<td width="10%">
					<input size="5" name="separator2" value="," style="width:95%;" class="cui-edit-field" />
				</td>
				<td width="5%">
					<span class="deleteBtn" onclick="javascript:ec.config.relationModel.delItem(this);">&nbsp;</span>
				</td>
			</tr>
			
			<tr style="display:none;" class="content_level_2">
				<td width="10%">
					<input size="5" stype="margin:0 3px;" name="separator1" value="" style="width:95%;" class="cui-edit-field" />
				</td>
				<td width="13%">
					<select id="method" style="width:95%;margin:0 3px;">
						<option value="">-</option>
						<option value="COUNT">COUNT</option>
						<option value="SUM">SUM</option>
						<option value="MAX">MAX</option>
						<option value="MIN">MIN</option>
					</select>
				</td>
				<td width="20%">
					<select id ="relation_model_select" style="width:95%;margin:0 3px;" onchange="ec.config.relationModel.changeSelect_level_2();"></select>
				</td>
				<td width="40%">
					<div name="property" class="fix-search-click" style="width:95%;margin:0 3px;">
						<input size="15" name="value" class="cui-edit-field" />
						<input type="button" onclick="ec.config.relationModel.selectProperty(event, this);" class="cui-search-click" />
					</div>
				</td>
				<td width="10%">
					<input size="5" name="separator2" value="," style="width:95%;" class="cui-edit-field" />
				</td>
				<td width="5%">
					<span class="deleteBtn" onclick="javascript:ec.config.relationModel.delItem(this);">&nbsp;</span>
				</td>
			</tr>
		</table>
	</div>
</div>
<script type="text/javascript">
	CUI.ns("ec.config.relationModel");
	
	ec.config.relationModel.changeMethod_level_1 = function(obj){
		if ($(obj).val() != '') {
			$('#a_level_2').hide();
			$('#configContainer tr.content_level_2:visible').remove();
			$('#configContainer tr.separator_begin_level_2').remove();
			$('#configContainer tr.separator_end_level_2').remove();
		} else {
			var flag = true;
			$.each($('#configContainer tr.content_level_1:visible select[id="method"]'), function(){
				if ($(this).val() != '') {
					flag = false;
					return false;
				}
			});
			if (flag && $('#a_level_2').css('display') == 'none') {
				$('#a_level_2').show();
			}
		}
	}
	
	ec.config.relationModel.changeSelect_level_1 = function(){
		if($('#configContainer tr.content_level_2:visible').length > 0){
			$('#configContainer tr.separator_begin_level_2').remove();
			$('#configContainer tr.content_level_2:visible').remove();
			$('#configContainer tr.separator_end_level_2').remove();
			
		}
		$('#configContainer tr.content_level_1:visible input[name="value"]').removeAttr("propertyCode");
		$('#configContainer tr.content_level_1:visible input[name="value"]').removeAttr("propertyName");
		$('#configContainer tr.content_level_1:visible input[name="value"]').removeAttr("propertytype");
		$('#configContainer tr.content_level_1:visible input[name="value"]').val('');
	}
	
	ec.config.relationModel.changeSelect_level_2 = function(){
		$('#configContainer tr.content_level_2:visible input[name="value"]').removeAttr("propertyCode");
		$('#configContainer tr.content_level_2:visible input[name="value"]').removeAttr("propertyName");
		$('#configContainer tr.content_level_2:visible input[name="value"]').removeAttr("propertytype");
		$('#configContainer tr.content_level_2:visible input[name="value"]').val('');
	}
	
	ec.config.relationModel.addItem1 = function(){
		if ($('tr.content_level_1:first #relation_model_select')[0].options.length < 1) {
			CUI.Dialog.alert("没有可选择的关联模型！");
		}
		var newTr = $('#configContainer tr.content_level_1:first').clone(true);
		if ($('#configContainer tr.separator_end_level_1').length > 0){
			$('select[id="relation_model_select"]', newTr).val($('select[id="relation_model_select"]', 'tr.content_level_1:visible:first').val());
			if ($('#configContainer tr.separator_begin_level_2').length > 0) {
				$('#configContainer tr.separator_begin_level_2').before(newTr);
			} else {
				$('#configContainer tr.separator_end_level_1').before(newTr);
			}
		} else {
			$('#configContainer').append('<tr class="separator_begin_level_1"><td width="10%"><input size="5" name="separatorBeginLevel1" style="width:95%;" class="cui-edit-field" onchange="ec.config.relationModel.doPreview();" /></td></tr>');		
			$('#configContainer').append(newTr);
			$('#configContainer').append('<tr class="separator_end_level_1"><td width="10%"><input size="5" name="separatorEndLevel1" style="width:95%;" class="cui-edit-field" onchange="ec.config.relationModel.doPreview();" /></td></tr>');
		}
		newTr.show();
		if ($('tr.content_level_1:visible').length > 1) {
			$('select[id="relation_model_select"]', 'tr.content_level_1:visible').prop("disabled", true);
		}
		return newTr;
	}
	
	ec.config.relationModel.addItem2 = function(){
		if ($('#configContainer tr.separator_begin_level_1').length < 1) {
			CUI.Dialog.alert("请先选择第一层关联模型！");
			return;
		}
		var flag = false;
		var modelCode = $("tr.content_level_1:visible:first #relation_model_select").val();
		$.ajax({
			async   : false,
			url     : '/msService/ec/view/getSecondLevelAssoModels?modelCode=' + modelCode,
			success : function(res){
				if(res.dealSuccessFlag){
					var models = $.parseJSON(res.models).models;
					if (models.length < 1) {
						flag = true;
						return;
					}
			    	$.each(models, function(i, item){
			    		$('tr.content_level_2:first #relation_model_select').empty().append('<option value="' + item.code + '">' + item.name + '</option>');
			    	});
			    }
		    }
		});
		if (flag) {
			CUI.Dialog.alert("没有可选择的第二层关联模型！");
			return;
		}
		var newTr = $('#configContainer tr.content_level_2:first').clone(true);
		if ($('#configContainer tr.separator_begin_level_2').length > 0) {
			$('select[id="relation_model_select"]', newTr).val($('select[id="relation_model_select"]', 'tr.content_level_2:visible:first').val());
			$('#configContainer tr.separator_end_level_2').before(newTr);
		} else {
			$('#configContainer tr.separator_end_level_1').before('<tr class="separator_begin_level_2"><td width="10%"><input size="5" name="separatorBeginLevel2" style="width:95%;" class="cui-edit-field" onchange="ec.config.relationModel.doPreview();" /></td></tr>');		
			$('#configContainer tr.separator_end_level_1').before(newTr);
			$('#configContainer tr.separator_end_level_1').before('<tr class="separator_end_level_2"><td width="10%"><input size="5" name="separatorEndLevel2" style="width:95%;" class="cui-edit-field" onchange="ec.config.relationModel.doPreview();" /></td></tr>');
		}
		newTr.show();
		if ($('tr.content_level_2:visible').length > 1) {
			$('select[id="relation_model_select"]', 'tr.content_level_2:visible').prop("disabled", true);
		}
		return newTr;
	}
	
	ec.config.relationModel.delItem = function(obj){
		$(obj).parents('tr:first').remove();
		if ($('tr.content_level_1:visible').length == 1) {
			$('tr.content_level_1:visible select[id="relation_model_select"]').prop("disabled", false);
		}
		if ($('tr.content_level_2:visible').length == 1) {
			$('tr.content_level_2:visible select[id="relation_model_select"]').prop("disabled", false);
		}
		if ($('#configContainer tr.content_level_1:visible').length < 1) {
			$('#configContainer tr.separator_begin_level_1').remove();
			$('#configContainer tr.separator_end_level_1').remove();
		}
		if ($('#configContainer tr.content_level_2:visible').length < 1) {
			$('#configContainer tr.separator_begin_level_2').remove();
			$('#configContainer tr.separator_end_level_2').remove();
		}
		ec.config.relationModel.doPreview();
	}
	
	ec.config.relationModel.showOverLayer = function(obj,url){
		CUI('#customContent').html("");
		ec.config.relationModel.showOverLayerDiv = new CUI.Overlay({
			align   : obj,
	     	el      : 'customContent',
	     	title   : '${getText('ec.property.choicefield')}',
	     	width   : 180,
	     	height  : 282,
	     	zIndex  : 9999,
	     	shadow  : false,
			buttons : [
						{	name:"${getHtmlText('foundation.workbench.mainPage.sure')}",
							handler:function(){
								if ( $('li[isSelected="1"]').length == 0 ) {
									CUI.Dialog.alert('${getText("ec.config.relationModel.selectNullData")}');
								} else {
									ec.config.relationModel.propertyDblClickFunc($('li[isSelected="1"]')[0]);
									$('body').trigger('click.overlayer');
								}
							}
						},
						{	name:"${getHtmlText('calendar.common.cancal')}",
							handler:function(){$('body').trigger('click.overlayer');}
						}
					  ]
	     	
		});
		ec.config.relationModel.showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url += "&time=" + new Date();
		ec.config.relationModel.showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
	
	ec.config.relationModel.selectProperty = function(e, obj){
		YUE.stopPropagation(e);
		ec.config.relationModel.current = $(obj).prev('input[name="value"]');
		var modelCode = $("#relation_model_select", $(obj).parents("tr:first")).val();
		if(null == modelCode || undefined == modelCode || "" == modelCode){
			return;
		}
		var method = $('#method', $(obj).parents("tr:first")).val();
		var useMethod = false;
		if (method != '') {
			useMethod = true;
		}
		if ($(obj).parents("tr:first").attr("class") == "content_level_1") {
			ec.config.relationModel.showOverLayer(obj, '/msService/ec/view/config-select-property?model.code=' + modelCode + "&modelCode=${modelCode!}&useMethod=" + useMethod);
		} else {
			ec.config.relationModel.showOverLayer(obj, '/msService/ec/view/config-select-property?model.code=' + modelCode + "&modelCode=" + String($('tr.content_level_1:visible:first select[id="relation_model_select"]').val()) + "&useMethod=" + useMethod);
		}
	}
	
	ec.config.relationModel.propertyDblClickFunc = function(obj){
		if ( propertyClickFunc(obj) ) {
			ec.config.relationModel.current.removeAttr("propertyName").attr('propertyName', $(obj).attr('name'));
			ec.config.relationModel.current.removeAttr("propertyCode").attr('propertyCode', $(obj).attr('propertyCode'));
			ec.config.relationModel.current.removeAttr("propertytype").attr('propertytype', $(obj).attr('columnType'));
			ec.config.relationModel.fillPropertiesNames(ec.config.relationModel.current, $(obj).attr('name'));
			ec.config.relationModel.doPreview();
		}
	}
	
	ec.config.relationModel.fillPropertiesNames = function(obj, names){
		var ret = getDisplayNamesByNamesAndModelCode(names, $('#relation_model_select', $(obj).parents("tr:first")).val());
		obj.val('');
		for(var v in ret.data) {
			if(obj.val().length > 0) {
				obj.val(obj.val() + '.');
			}
			obj.val(obj.val() + ret.data[v]);
		}
	}
	
	ec.config.relationModel.validateConfig = function(){
		if($('tr[id!="assoModelPropsSampleTr"]:visible', '#configContainer').length < 1) {
			config_relationModelPropsErrorBarWidget.show("请选择一个关联模型字段！");
			return false;
		}
		
		var flag1 = false, flag2 = false;
		$('#configContainer tr.content_level_1:visible select[id="method"]').each(function(){
			if ($(this).val() != '') {
				flag1 = true;
			}
			if ($(this).val() == '') {
				flag2 = true;
			}
		});
		if (flag1 && flag2) {
			config_relationModelPropsErrorBarWidget.show('第一层关联模型字段必需都设置聚合函数，或都不设置！');
			return false;
		}
		var flag3 = false, flag4 = false;
		$('#configContainer tr.content_level_2:visible select[id="method"]').each(function(){
			if ($(this).val() != '') {
				flag3 = true;
			}
			if ($(this).val() == '') {
				flag4 = true;
			}
		});
		if (flag3 && flag4) {
			config_relationModelPropsErrorBarWidget.show('第二层关联模型字段必需都设置聚合函数，或都不设置！');
			return false;
		}
		
		var validateFlag = true;
		$('tr[id!="assoModelPropsSampleTr"]:visible', '#configContainer').each(function(){
			var tmpTr = $(this);
			if($('input[name="value"]', tmpTr).length > 0 && $('input[name="value"]', tmpTr).val().length == 0) {
				config_relationModelPropsErrorBarWidget.show('没有选择关联模型字段！');
				$('input[name="value"]', tmpTr).focus();
				validateFlag = false;
				return false;
			}
			
			if ( tmpTr.attr("class") == 'content_level_1' || tmpTr.attr("class") == 'content_level_2' ) {
				var method = $('#method', tmpTr).val();
				var propertytype = $('input[name="value"]', tmpTr).attr("propertytype");
				if ( (method == 'SUM' || method == 'MIN' || method == 'MAX') && propertytype != 'INTEGER' && propertytype != 'DECIMAL' && propertytype != 'BINARY' && propertytype != 'LONG' && propertytype != 'MONEY' ) {
					config_relationModelPropsErrorBarWidget.show('聚合函数SUM、MAX、MIN不能作用于非数值类型字段！');
					$('input[name="value"]', tmpTr).focus();
					validateFlag = false;
					return false;	
				}
			}
		});
		return validateFlag;
	}
	
	ec.config.relationModel.saveConfig = function(){
		if(!ec.config.relationModel.validateConfig()) {
			return false;
		}
		var config = '';
		var separator_begin_level_1 =  $('input[name="separatorBeginLevel1"]').val();
		var separator_end_level_1 =  $('input[name="separatorEndLevel1"]').val();
		var separator_begin_level_2 = '', separator_end_level_2 = '';
		config += '{"separatorBeginLevel1":"' + separator_begin_level_1 + '","separatorEndLevel1":"' + separator_end_level_1 + '",';
		if ($('#configContainer tr.separator_begin_level_2').length > 0) {
			separator_begin_level_2 =  $('input[name="separatorBeginLevel2"]').val();
			separator_end_level_2 =  $('input[name="separatorEndLevel2"]').val();
			config += '"separatorBeginLevel2":"' + separator_begin_level_2 + '","separatorEndLevel2":"' + separator_end_level_2 + '",';
		}
		config += '"config":[';
		$( 'tr[id!="assoModelPropsSampleTr"]:visible', '#configContainer' ).each(function(i, item){
			var tmpTr = $(this);
			if(tmpTr.attr("class") == 'content_level_1' || tmpTr.attr("class") == 'content_level_2'){
				var separator1 = $('input[name="separator1"]', tmpTr).val();
				if ( tmpTr.attr("class") == 'content_level_1' ) {
					config += '{"level":1,';
				} else {
					config += '{"level":2,';
				}
				config += '"separator1":"' + separator1 + '",';
				
				var method = $('#method', tmpTr).val();
				config += '"method":"' + method + '",';
				
				var modelCode = $('#relation_model_select', tmpTr).val();
				config += '"modelCode":"' + modelCode + '",';			
				
				var value = $('input[name="value"]', tmpTr);
				config += '"propertyCode":"' + value.attr("propertyCode") + '",';
				config += '"propertyName":"' + value.attr("propertyName") + '",';
				config += '"propertyType":"' + value.attr("propertytype") + '",';
				
				var separator2 = $('input[name="separator2"]', tmpTr).val();
				config += '"separator2":"' + separator2 + '"},';
			}
		});
		if (config.endsWith('},')) {
			config = config.substring(0, config.length - 1);
		}
		config += ']}';
		$('td.ui-selected', '#list_design_section').removeAttr("assoFlag").attr("assoFlag", 'true');
		$('td.ui-selected', '#list_design_section').removeAttr("assoConfig").attr("assoConfig", config);
		$('tr,td', '#listFieldDlg').hide();
		$('tr.config_relation_model_props,td.config_relation_model_props', '#listFieldDlg').show();
		return true;
	}
	
	ec.config.relationModel.preview = function(){
		var sample_level_1 = '', sample_level_2 = '';
		var separator_begin_level_1 =  $('input[name="separatorBeginLevel1"]').val() == undefined ? '' : $('input[name="separatorBeginLevel1"]').val();
		var separator_end_level_1 =  $('input[name="separatorEndLevel1"]').val() == undefined ? '' : $('input[name="separatorEndLevel1"]').val();
		var separator_begin_level_2 = '', separator_end_level_2 = '';
		if ($('#configContainer tr.separator_begin_level_2').length > 0) {
			separator_begin_level_2 =  $('input[name="separatorBeginLevel2"]').val();
			separator_end_level_2 =  $('input[name="separatorEndLevel2"]').val();
		}
		var lastSeparator1 = '';
		var lastSeparator2 = '';
		var useMethod1 = false, useMethod2 = false;
		$( 'tr[id!="assoModelPropsSampleTr"]:visible', '#configContainer' ).each(function(i, item){
			var tmpTr = $(this);
			if(tmpTr.attr("class") == 'content_level_1' || tmpTr.attr("class") == 'content_level_2'){
				var separator1 = $('input[name="separator1"]', tmpTr).val();
				var method = $('#method', tmpTr).val();
				var modelName = $('#relation_model_select', tmpTr).find('option:selected').text();
				var propsName = $('input[name="value"]', tmpTr).val();
				var separator2 = $('input[name="separator2"]', tmpTr).val();
				
				if ( tmpTr.attr("class") == 'content_level_1' ) {
					if (method == '') {
						sample_level_1 += separator1 + modelName + '.' + propsName + separator2;
					} else {
						useMethod1 = true;
						sample_level_1 += separator1 + method + '(' + modelName + '.' + propsName + ')' + separator2;
					}
					lastSeparator1 = separator2;
				} else {
					if (method == '') {
						sample_level_2 += separator1 + modelName + '.' + propsName + separator2;
					} else {
						useMethod2 = true;
						sample_level_2 += separator1 + method + '(' + modelName + '.' + propsName + ')' + separator2;
					}
					lastSeparator2 = separator2;
				}
			}
		});
		if (useMethod1 || useMethod2) {
			if (useMethod1) {
				sample_level_1 = sample_level_1.substring(0, sample_level_1.length - (lastSeparator1 == '' ? 0 : lastSeparator1.length));
			} 
			if (useMethod2) {
				sample_level_2 = sample_level_2.substring(0, sample_level_2.length - (lastSeparator2 == '' ? 0 : lastSeparator2.length));
			}
		} else {
			sample_level_2 += sample_level_2.substring(0, sample_level_2.length - (lastSeparator2 == '' ? 0 : lastSeparator2.length));
		}
		var sample = separator_begin_level_1 + sample_level_1 + separator_begin_level_2 + sample_level_2 + separator_end_level_2 + separator_end_level_1;
		$('#assoModelPropsSample').val('预览：' + sample);
		return true;
	}	
	
	ec.config.relationModel.init = function(){
		var config = $('td.ui-selected', '#list_design_section').attr("assoConfig");
		var cjson = $.parseJSON(config);
		if(cjson && cjson.config && cjson.config.length > 0) {
			$.each(cjson.config, function(){
				var addTr;
				if (this.level == 1) {
					addTr = ec.config.relationModel.addItem1();
				} else {
					addTr = ec.config.relationModel.addItem2();
				}
				$('input[name="separator1"]', addTr).val(this.separator1);
				
				$('#method', addTr).val(this.method);
				
				$('#relation_model_select', addTr).val(this.modelCode);
				
				var valueInput = $('input[name="value"]', addTr);
				valueInput.removeAttr("propertyCode").attr('propertyCode', this.propertyCode);
				valueInput.removeAttr("propertyName").attr('propertyName', this.propertyName);
				valueInput.removeAttr("propertytype").attr('propertytype', this.propertyType);
				ec.config.relationModel.fillPropertiesNames(valueInput, this.propertyName);
				
				$('input[name="separator2"]', addTr).val(this.separator2);
			});
			if (cjson && cjson.separatorBeginLevel1) {
				$('input[name="separatorBeginLevel1"]').val(cjson.separatorBeginLevel1);
			}
			if (cjson && cjson.separatorEndLevel1) {
				$('input[name="separatorEndLevel1"]').val(cjson.separatorEndLevel1);
			}
			if (cjson && cjson.separatorBeginLevel2) {
				$('input[name="separatorBeginLevel2"]').val(cjson.separatorBeginLevel2);
			}
			if (cjson && cjson.separatorEndLevel2) {
				$('input[name="separatorEndLevel2"]').val(cjson.separatorEndLevel2);
			}
			ec.config.relationModel.doPreview();
		}
		return true;
	}
	
	ec.config.relationModel.doPreview = function(){
		$('#assoModelPropsSample').val('');
		ec.config.relationModel.preview();
	}
	
	$(function(){
		ec.config.relationModel.init();
		$('body').unbind('click.overlayer').bind('click.overlayer', function(){
			if(ec.config.relationModel.showOverLayerDiv && ec.config.relationModel.showOverLayerDiv.isShow) {
				ec.config.relationModel.showOverLayerDiv.close();
				$('#overlay-shim-ie').remove();
			}
		});
		$('input[name^="separator"],select[id="method"],select[id="relation_model_select"]').unbind('change.preview').bind('change.preview', function(){
			ec.config.relationModel.doPreview();
		});
	});
</script>