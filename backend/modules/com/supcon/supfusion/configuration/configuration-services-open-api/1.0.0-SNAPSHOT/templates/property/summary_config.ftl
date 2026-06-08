<#-- Head -->
<style type="text/css">
.edv-a{border:1px #5C8CB5 solid;width:500px;_width:485px;height:45px;margin:0 auto;padding:3px;_padding-bottom:0px;}
#summaryConfigContainer {margin: 3px 5px 5px 5px;}
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
<div id="summaryConfigDiv">
	<@errorbar id="code_query_edit_bar"></@errorbar>
	<div id="toolbar"> 
		<a href="#" onclick="javascript:ec.summary.config.addItem('property');">${getHtmlText('ec.ec.summary.config.add.entity.property')}</a>&nbsp;&nbsp;
		<a href="#" onclick="javascript:ec.summary.config.addItem('inherent');">${getHtmlText('ec.ec.summary.config.add.entity.attribute')}</a>&nbsp;&nbsp;
		<a href="#" onclick="javascript:ec.summary.config.addItem('custom');">${getHtmlText('ec.ec.summary.config.add.custom')}</a>
	</div>
	<div id="outerContainer">
		<table id="summaryConfigContainer">
			<tr style="display:none;">
				<td width="15%">
					<select onchange="ec.summary.config.typeHasChange(this);" class="edit-select-code" style="width:95%" name="type">
						<option value="property">${getText('ec.ec.summary.config.type.entity.property')}</option>
						<option value="inherent">${getText('ec.ec.summary.config.type.entity.attribute')}</option>
						<option value="custom">${getText('ec.ec.summary.config.type.custom')}</option>
					</select>
				</td>
				<td width="55%">
					<div name="property" class="fix-search-click" style="width:95%">
						<input size="15" name="value" class="cui-edit-field">
						<input type="button" onclick="ec.summary.config.selectProperty(event, this)"  value="" class="cui-search-click">
					</div>
					<div name="inherent" style="width:95%">
						<select class="edit-select-code" style="width:100%" name="value">
							<option value="${model.entity.module.artifact}">${getText('ec.ec.summary.config.current.module.code')}${model.entity.module.artifact}</option>
							<#if model.entity.workflowEnabled>
							<option value="${model.entity.prefix}">${getText('ec.ec.summary.config.current.table.prefix')}${model.entity.prefix}</option>
							</#if>
							<option value="${model.entity.entityName}">${getText('ec.ec.summary.config.current.entity.name')}${model.entity.entityName}</option>
							<option value="${model.modelName}">${getText('ec.ec.summary.config.current.model.name')}${model.modelName}</option>
						</select>
					</div>
					<div name="custom" style="width:95%">
						<input size="15" name="value" class="cui-edit-field">
					</div>
				</td>
				<td width="15%">
					<select class="edit-select-code" style="width:100%" name="thecase">
						<option value="original">${getText('ec.ec.summary.config.case.original')}</option>
						<option value="uppercase">${getText('ec.ec.summary.config.case.uppercase')}</option>
						<option value="lowercase">${getText('ec.ec.summary.config.case.lowercase')}</option>
					</select>
				</td>
				<td width="10%">
					<input size="15" name="separator" value="_" style="width:95%;" class="cui-edit-field">
				</td>
				<td width="5%">
					<span class="deleteBtn" onclick="javascript:ec.summary.config.delItem(this);">&nbsp;</span>
				</td>
			</tr>
		</table>
	</div>
</div>
<script type="text/javascript">
CUI.ns("ec.summary.config", "ec.code.config");
ec.summary.config.addItem = function(type){
	var newTr = $('#summaryConfigContainer tr:first').clone();
	$('#summaryConfigContainer').append(newTr);
	$('div[name!="' + type + '"]', newTr).remove();
	$('select[name="type"]', newTr).val(type);
	newTr.show();
	$('input[name="separator"]').show();
	$('input[name="separator"]:last').hide();
	return newTr;
}
ec.summary.config.delItem = function(obj){
	$(obj).parents('tr:first').remove();
	$('input[name="separator"]').show();
	$('input[name="separator"]:last').hide();
}
ec.summary.config.typeHasChange = function(obj){
	var type = $(obj).val();
	var newTr = $('#summaryConfigContainer tr:first').clone();
	var oldTr = $(obj).parents('tr:first');
	newTr.insertAfter(oldTr);
	oldTr.remove();

	$('div[name!="' + type + '"]', newTr).remove();
	$('select[name="type"]', newTr).val(type);
	newTr.show();
	return newTr;
}
ec.summary.config.selectProperty = function(e, obj){
	YUE.stopPropagation(e);
	ec.summary.config.current = $(obj).prev('input[name="value"]');
	ec.summary.config.showOverLayer(obj, '/msService/ec/property/select_property?model.code=${model.code}');
}
ec.summary.config.validateConfig = function(){
	if($('tr:visible', '#summaryConfigContainer').length <= 0) {
		code_query_edit_barWidget.show('${getText("ec.ec.summary.config.validate.must.one")}');
		return false;
	}
	var validateFlag = true;
	$('tr:visible', '#summaryConfigContainer').each(function(){
		var tmpTr = $(this);
		if($('select[name="type"]', tmpTr).val() == 'property' || $('select[name="type"]', tmpTr).val() == 'auto') {
			if($('[name="value"]', tmpTr).val() == null || $('[name="value"]', tmpTr).val().length == 0) {
				code_query_edit_barWidget.show('${getText("ec.ec.summary.config.validate.fail")}');
				validateFlag = false;
				return false;
			}
		}
	});
	return validateFlag;
}
ec.summary.config.saveConfig = function(){
	if(!ec.summary.config.validateConfig()) {
		return false;
	}
	var config = '{"config":[';
	$('tr:visible', '#summaryConfigContainer').each(function(){
		var tmpTr = $(this);
		var type = $('select[name="type"]', tmpTr).val();
		var thecase = $('select[name="thecase"]', tmpTr).val();
		var value = $('[name="value"]', tmpTr);
		var separator = $('input[name="separator"]', tmpTr).val();
		config += '{"type":"' + type + '",';
		config += '"thecase":"' + thecase + '",';
		config += '"value":"' + (type == 'property' ? value.attr('propertiesName') : value.val()) + '",';
		if(type == 'auto') {
			config += '"countType":"' + $('select[name="countType"]', tmpTr).val() + '",';
		}
		config += '"separator":"' + separator + '"},';
	});
	if(config.endsWith('},')) {
		config = config.substring(0, config.length - 1);
	}
	config += ']}';
	$('#ec_property_edit_form_property_attributes').val(config);
	return true;
}
ec.summary.config.init = function(){
	var config = $('#ec_property_edit_form_property_attributes').val();
	//try{
		var cjson = $.parseJSON(config);
		if(cjson && cjson.rollbackable && cjson.rollbackable !== 'false') {
			$('#rollbackable').prop('checked', true);
		}
		if(cjson && cjson.config && cjson.config.length > 0) {
			$.each(cjson.config, function(){
				var addTr = ec.summary.config.addItem(this.type);
				$('select[name="thecase"]', addTr).val(this.thecase);
				$('input[name="separator"]', addTr).val(this.separator);
				var valueInput = $('[name="value"]', addTr);
				if(this.type == 'property') {
					valueInput.attr('propertiesName', this.value);
					ec.summary.config.fillPropertiesNames(valueInput, this.value);
				} else {
					valueInput.val(this.value);
				}
				if(this.type == 'auto' && this.countType && this.countType.length > 0) {
					$('select[name="countType"]', addTr).val(this.countType);
				}
			});
		}
	//} catch(e) {}
	return true;
}
ec.code.config.propertyDblClickFunc = ec.summary.config.propertyDblClickFunc = function(obj){
	if(propertyClickFunc(obj)) {
		ec.summary.config.current.attr('propertiesName', $(obj).attr('name'));
		ec.summary.config.fillPropertiesNames(ec.summary.config.current, $(obj).attr('name'));
	}
}
ec.summary.config.fillPropertiesNames = function(obj, names){
	var ret = getDisplayNamesByNamesAndModelCode(names, '${model.code}');
	obj.val('');
	for(var v in ret.data) {
		if(obj.val().length > 0) {
			obj.val(obj.val() + '.');
		}
		obj.val(obj.val() + ret.data[v]);
	}
}
</script>

<script type="text/javascript">
	$(function(){
		ec.summary.config.init();
	});
	$('body').unbind('click.overlayer').bind('click.overlayer', function(){
		if(ec.summary.config.showOverLayerDiv && ec.summary.config.showOverLayerDiv.isShow) {
			ec.summary.config.showOverLayerDiv.close();
		}
	});
			
	ec.summary.config.showOverLayer = function(obj,url){
		CUI('#customContent').html("");
		ec.summary.config.showOverLayerDiv = new CUI.Overlay({
			align:obj,
	     	el:'customContent',
	     	title:'${getText('ec.property.choicefield')}',
	     	width:180,
	     	height:282,
	     	zIndex:9999,
	     	shadow:false,
			buttons:[
					{	name:"${getHtmlText('foundation.workbench.mainPage.sure')}",
						handler:function(){
							if($('li[isSelected="1"]').length == 0) {
								alert('${getText("ec.ec.summary.config.selectNullData")}');
							} else {
								ec.summary.config.propertyDblClickFunc($('li[isSelected="1"]')[0]);
								$('body').trigger('click.overlayer');
							}
						}
					},
					{	name:"${getHtmlText('calendar.common.cancal')}",
						handler:function(){$('body').trigger('click.overlayer');}
					}]
	     	
		});
		
		
		ec.summary.config.showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url+="&time="+new Date();
		ec.summary.config.showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
</script>