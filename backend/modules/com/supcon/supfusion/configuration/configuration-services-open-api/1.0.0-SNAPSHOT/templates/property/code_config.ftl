<#-- Head -->
<style type="text/css">
.edv-a{border:1px #5C8CB5 solid;width:500px;_width:485px;height:45px;margin:0 auto;padding:3px;_padding-bottom:0px;}
#codeConfigContainer {margin: 3px 5px 5px 5px;}
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
	<@errorbar id="code_query_edit_bar"></@errorbar>
	<div id="toolbar"> 
		<a href="#" onclick="javascript:ec.code.config.addItem('property');">${getHtmlText('ec.ec.code.config.add.entity.property')}</a>&nbsp;&nbsp;
		<a href="#" onclick="javascript:ec.code.config.addItem('inherent');">${getHtmlText('ec.ec.code.config.add.entity.attribute')}</a>&nbsp;&nbsp;
		<a href="#" onclick="javascript:ec.code.config.addItem('date');">${getHtmlText('ec.ec.code.config.add.date')}</a>&nbsp;&nbsp;
		<a href="#" onclick="javascript:ec.code.config.addItem('custom');">${getHtmlText('ec.ec.code.config.add.custom')}</a>&nbsp;&nbsp;
		<a href="#" onclick="javascript:ec.code.config.addItem('auto');">${getHtmlText('ec.ec.code.config.add.auto')}</a>&nbsp;&nbsp;
		<span id="rollbackableWrap">
			<input type="checkbox" id="rollbackable" name="rollbackable"><label style="cursor: pointer;" for="rollbackable">${getHtmlText('ec.ec.code.config.rollback')}</label>
		</span>
	</div>
	<div id="outerContainer" style="height:350px;overflow-y:scroll">
		<table id="codeConfigContainer">
			<tr style="display:none;">
				<td width="15%">
					<select onchange="ec.code.config.typeHasChange(this);" class="edit-select-code" style="width:95%" name="type">
						<option value="property">${getText('ec.ec.code.config.type.entity.property')}</option>
						<option value="inherent">${getText('ec.ec.code.config.type.entity.attribute')}</option>
						<option value="date">${getText('ec.ec.code.config.type.date')}</option>
						<option value="custom">${getText('ec.ec.code.config.type.custom')}</option>
						<option value="auto">${getText('ec.ec.code.config.type.auto')}</option>
					</select>
				</td>
				<td width="60%">
					<div name="property" class="fix-search-click" style="width:95%">
						<input size="15" name="value" class="cui-edit-field">
						<input type="button" onclick="ec.code.config.selectProperty(event, this)"  value="" class="cui-search-click">
					</div>
					<div name="inherent" style="width:95%">
						<select class="edit-select-code" style="width:100%" name="value">
							<option value="${model.entity.module.artifact}">${getText('ec.ec.code.config.current.module.code')}${model.entity.module.artifact}</option>
							<#if model.entity.workflowEnabled>
							<option value="${model.entity.prefix}">${getText('ec.ec.code.config.current.table.prefix')}${model.entity.prefix}</option>
							</#if>
							<option value="${model.entity.entityName}">${getText('ec.ec.code.config.current.entity.name')}${model.entity.entityName}</option>
							<option value="${model.modelName}">${getText('ec.ec.code.config.current.model.name')}${model.modelName}</option>
						</select>
					</div>
					<div name="date" class="fix-search-click" style="width:95%;text-align: right;">
						<select class="edit-select-code" style="width:40%" name="dateType">
							<option value="YearA">${getText('ec.ec.code.config.date.datetype.yeara')}</option>
							<option value="YearB">${getText('ec.ec.code.config.date.datetype.yearb')}</option>
							<option value="Month">${getText('ec.ec.code.config.date.datetype.month')}</option>
							<option value="Date">${getText('ec.ec.code.config.date.datetype.date')}</option>
							<option value="YearMonth">${getText('ec.ec.code.config.date.datetype.yearmonth')}</option>
                            <option value="YearMonthDate">${getText('ec.ec.code.config.date.datetype.yearmonthdate')}</option>
						</select>
						<input size="15" name="value" style="width:55%" class="cui-edit-field">
						<input type="button" onclick="ec.code.config.selectDateProperty(event, this)"  value="" class="cui-search-click">
					</div>
					<div name="custom" style="width:95%">
						<input size="15" name="value" class="cui-edit-field">
					</div>
					<div name="auto" class="fix-search-click" style="width:95%;">
						${getHtmlText('ec.ec.code.config.auto.digit')}<input size="15" style="width:10%" name="digit" class="cui-edit-field"> 
						<select class="edit-select-code" onchange="ec.code.config.autoTypeChange(this);" style="width:25%" name="autoType">
							<option value="Code">${getText('ec.ec.code.config.auto.autoType.Code')}</option>
							<option value="Date">${getText('ec.ec.code.config.auto.autoType.Date')}</option>
						</select>
						<select class="edit-select-code" style="width:20%" name="countType">
							<option value="Daily">${getText('ec.ec.code.config.auto.loop.cycle.daily')}</option>
							<option value="Monthly">${getText('ec.ec.code.config.auto.loop.cycle.monthly')}</option>
							<option value="Yearly">${getText('ec.ec.code.config.auto.loop.cycle.yearly')}</option>
						</select>
						<input size="15" name="value" style="width:30%" class="cui-edit-field">
						<input type="button" onclick="ec.code.config.selectDateProperty(event, this)"  value="" class="cui-search-click">
					</div>
				</td>
				<td width="15%">
					<select class="edit-select-code" style="width:100%" name="thecase">
						<option value="original">${getText('ec.ec.code.config.case.original')}</option>
						<option value="uppercase">${getText('ec.ec.code.config.case.uppercase')}</option>
						<option value="lowercase">${getText('ec.ec.code.config.case.lowercase')}</option>
					</select>
				</td>
				<td width="5%">
					<input size="15" name="separator" value="_" style="width:95%;" class="cui-edit-field">
				</td>
				<td width="5%">
					<span class="deleteBtn" onclick="javascript:ec.code.config.delItem(this);">&nbsp;</span>
				</td>
			</tr>
		</table>
	</div>
</div>
<script type="text/javascript">
CUI.ns("ec.code.config");
ec.code.config.addItem = function(type){
	var newTr = $('#codeConfigContainer tr:first').clone();
	$('#codeConfigContainer').append(newTr);
	$('div[name!="' + type + '"]', newTr).remove();
	$('select[name="type"]', newTr).val(type);
	newTr.show();
	$('input[name="separator"]').show();
	$('input[name="separator"]:last').hide();
	if(type=="auto"){
		ec.code.config.autoTypeChange($('select[name="autoType"]', newTr));
	}
	if(type=="date"||type=="auto"){
		$('select[name="thecase"]', newTr).attr("disabled",true); 
	}else{
		$('select[name="thecase"]', newTr).attr("disabled",false); 
	}
	return newTr;
}
ec.code.config.delItem = function(obj){
	$(obj).parents('tr:first').remove();
	$('input[name="separator"]').show();
	$('input[name="separator"]:last').hide();
}
ec.code.config.typeHasChange = function(obj){
	var type = $(obj).val();
	var newTr = $('#codeConfigContainer tr:first').clone();
	var oldTr = $(obj).parents('tr:first');
	newTr.insertAfter(oldTr);
	oldTr.remove();

	$('div[name!="' + type + '"]', newTr).remove();
	$('select[name="type"]', newTr).val(type);
	if(type=="auto"){
		ec.code.config.autoTypeChange($('select[name="autoType"]', newTr));
	}
	if(type=="date"||type=="auto"){
		$('select[name="thecase"]', newTr).attr("disabled",true); 
	}else{
		$('select[name="thecase"]', newTr).attr("disabled",false); 
	}
	newTr.show();
	return newTr;
}
ec.code.config.selectProperty = function(e, obj){
	YUE.stopPropagation(e);
	ec.code.config.current = $(obj).prev('input[name="value"]');
	ec.code.config.showOverLayer(obj, '/msService/ec/property/select_property?model.code=${model.code}');
}
ec.code.config.selectDateProperty = function(e, obj){
	YUE.stopPropagation(e);
	ec.code.config.current = $(obj).prev('input[name="value"]');
	ec.code.config.showOverLayer(obj, '/msService/ec/property/select_dateproperty?model.code=${model.code}');
}
ec.code.config.validateConfig = function(){
	if($('tr:visible', '#codeConfigContainer').length <= 0) {
		code_query_edit_barWidget.show('${getText("ec.ec.code.config.validate.must.one")}');
		return false;
	}
	if($('select[name="countType"]').length > 2) {
		code_query_edit_barWidget.show('${getText("ec.ec.code.config.validate.only.one")}');
		return false;
	}
	if($('select[name="countType"]').length == 2) {
		if($("select[name='autoType'").eq(1).val() == "Date"){
			var propertiesname = $('select[name="countType"]~input[name]:eq(1)').attr('propertiesname');
			if(propertiesname == null || propertiesname == undefined || propertiesname == "" || propertiesname == "undefined"){
				code_query_edit_barWidget.show('${getText("ec.ec.code.config.validate.autoincre.null")}');
				return false;
			}
		}
	}
	if($('select[name="dateType"]~input[name]:gt(0)').length > 0){
		var validateFlag = true;
		$('select[name="dateType"]~input[name]:gt(0)').each(function(){
			var propertiesname = $(this).attr('propertiesname');
			if(propertiesname == null || propertiesname == undefined || propertiesname == "" || propertiesname == "undefined" ){
				code_query_edit_barWidget.show('${getText("ec.ec.code.config.validate.timesection.null")}');
				validateFlag = false;
			}
		});
		if(!validateFlag){
			return false;
		}
	}
	var validateFlag = true;
	$('tr:visible', '#codeConfigContainer').each(function(){
		var tmpTr = $(this);
		if($('select[name="type"]', tmpTr).val() == 'property') {
			if($('[name="value"]', tmpTr).val() == null || $('[name="value"]', tmpTr).val().length == 0) {
				code_query_edit_barWidget.show('${getText("ec.ec.code.config.validate.fail")}');
				validateFlag = false;
				return false;
			}
		}
	});
	return validateFlag;
}
ec.code.config.saveConfig = function(){
	if(!ec.code.config.validateConfig()) {
		return false;
	}
	var config = '{"rollbackable":' + $('#rollbackable').prop('checked') + ',"config":[';
	var formatStr="";
	var trlen=$('tr:visible', '#codeConfigContainer').length;
	$('tr:visible', '#codeConfigContainer').each(function(index,element){
		var tmpTr = $(this);
		var type = $('select[name="type"]', tmpTr).val();
		var thecase = $('select[name="thecase"]', tmpTr).val();
		var value = $('[name="value"]', tmpTr);
		var separator = $('input[name="separator"]', tmpTr).val();
		config += '{"type":"' + type + '",';
		config += '"thecase":"' + thecase + '",';
		config += '"value":"' + ((type == 'property'|| type =='date'|| type =='auto') ? value.attr('propertiesName') : value.val()) + '",';
		if(type == 'auto') {
			var autoType=$('select[name="autoType"]', tmpTr).val();
			var digit=$('input[name="digit"]', tmpTr).val();
			config += '"digit":"' + digit + '",';
			config += '"autoType":"' + autoType + '",';
			if(autoType=="Code"){
				config += '"countType":"none",';
			}else{
				config += '"countType":"' + $('select[name="countType"]', tmpTr).val() + '",';
			}
			formatStr += "{0,number,"+String(Math.pow(10,digit)).substr(1)+"}";
		}else if(type == 'date') {
			config += '"dateType":"' + $('select[name="dateType"]', tmpTr).val() + '",';
			formatStr += "{"+(index+1)+"}";
		}else{
			formatStr += "{"+(index+1)+"}";
		}
		config += '"separator":"' + separator + '"},';
		if(index<trlen-1){
			formatStr += separator;
		}
	});
	if(config.endsWith('},')) {
		config = config.substring(0, config.length - 1);
	}
	config += '],"pattern":"'+formatStr+'"}';
	$('#ec_property_edit_form_property_attributes').val(config);
	return true;
}
ec.code.config.init = function(){
	var config = $('#ec_property_edit_form_property_attributes').val();
	//try{
		var cjson = $.parseJSON(config);
		if(cjson && cjson.rollbackable && cjson.rollbackable !== 'false') {
			$('#rollbackable').prop('checked', true);
		}
		if(cjson && cjson.config && cjson.config.length > 0) {
			$.each(cjson.config, function(){
				var addTr = ec.code.config.addItem(this.type);
				$('select[name="thecase"]', addTr).val(this.thecase);
				$('input[name="separator"]', addTr).val(this.separator);
				var valueInput = $('[name="value"]', addTr);
				if(this.type == 'property'||this.type == 'date'||this.type == 'auto') {
					valueInput.attr('propertiesName', this.value);
					ec.code.config.fillPropertiesNames(valueInput, this.value);
				} else {
					valueInput.val(this.value);
				}
				if(this.type == 'auto' && this.countType && this.countType.length > 0) {
					$('input[name="digit"]', addTr).val(this.digit);
					$('select[name="countType"]', addTr).val(this.countType);
					$('select[name="autoType"]', addTr).val(this.autoType);
					ec.code.config.autoTypeChange($('select[name="autoType"]', addTr));
				}else if(this.type == 'date') {
					$('select[name="dateType"]', addTr).val(this.dateType);
				}
			});
		}
	//} catch(e) {}
	return true;
}
ec.code.config.propertyDblClickFunc = function(obj){
	if(propertyClickFunc(obj)) {
		ec.code.config.current.attr('propertiesName', $(obj).attr('name'));
		ec.code.config.fillPropertiesNames(ec.code.config.current, $(obj).attr('name'));
	}
}
ec.code.config.fillPropertiesNames = function(obj, names){
	if(names=="_systemdate"){
		obj.val('${getText('ec.ec.code.config.date.systemdate')}');
		return;
	}
	var ret = getDisplayNamesByNamesAndModelCode(names, '${model.code}');
	obj.val('');
	for(var v in ret.data) {
		if(obj.val().length > 0) {
			obj.val(obj.val() + '.');
		}
		obj.val(obj.val() + ret.data[v]);
	}
}
ec.code.config.autoTypeChange = function(obj){
	var autoType=$(obj).val();
	if(autoType=="Code"){
		$(obj).next("select").val("").hide().next("input").val("").attr("propertiesName",'').hide().next("input").hide();
	}else if(autoType=="Date"){
		$(obj).next("select").show().next("input").show().next("input").show();
	}
}
</script>

<script type="text/javascript">
	$(function(){
		ec.code.config.init();
	});
	$('body').unbind('click.overlayer').bind('click.overlayer', function(){
		if(ec.code.config.showOverLayerDiv && ec.code.config.showOverLayerDiv.isShow) {
			ec.code.config.showOverLayerDiv.close();
		}
	});
			
	ec.code.config.showOverLayer = function(obj,url){
		CUI('#customContent').html("");
		ec.code.config.showOverLayerDiv = new CUI.Overlay({
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
								alert('${getText("ec.ec.code.config.selectNullData")}');
							} else {
								ec.code.config.propertyDblClickFunc($('li[isSelected="1"]')[0]);
								$('body').trigger('click.overlayer');
							}
						}
					},
					{	name:"${getHtmlText('calendar.common.cancal')}",
						handler:function(){$('body').trigger('click.overlayer');}
					}]
	     	
		});
		
		
		ec.code.config.showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url+="&time="+new Date();
		ec.code.config.showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
</script>