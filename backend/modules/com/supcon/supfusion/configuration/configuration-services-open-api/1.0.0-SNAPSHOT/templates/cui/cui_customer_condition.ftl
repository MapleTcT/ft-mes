<#-- Head -->
<#macro customerCondition dataGridCode="",viewCode="",showArea="conditionArea", checkBoxId="isTransCondition", ccNameSpace="dg.advQuery", modelCode="", modelName="">
<style type="text/css">
.edv-a{border:1px #5C8CB5 solid;width:500px;_width:485px;height:45px;margin:0 auto;padding:3px;_padding-bottom:0px;}
</style>
<div id="advQueryDiv" style="display:none">
	<@errorbar id="adv_query_edit_bar"></@errorbar>
	<input type="hidden" id="customerDataGridCode" value="${dataGridCode}" />
	<input type="hidden" id="customerViewCode" value="${viewCode}" />
	<input type="hidden" id="dataClassificCode" <#if dataClassific??>value="${dataClassific.code!}"<#else>value=""</#if> />
	<input type="hidden" id="customerJsonString" value="" />
	<div id="toolbar"> 
		<a href="javascript:${ccNameSpace}.query._mergeGroup('${getText('foundation.advquery.and')}');"><span style="background: url('/bap/static/ec/images/and.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupand')}</span></a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._mergeGroup('${getText('foundation.advquery.or')}');"><span style="background: url('/bap/static/ec/images/or.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupor')}</span></a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._changeGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.changeor')}</a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._changeGroup('${getText('foundation.advquery.and')}');">${getHtmlText('foundation.advquery.changeand')}</a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._releaseGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.releasegroup')}</a>&nbsp;&nbsp;
	</div>
	<div id="outerContainer">
		<div id="advQueryContainer">
	
		</div>
	</div>
</div>


<script type="text/javascript">
CUI.ns("${ccNameSpace}");
${ccNameSpace}.showAdv = function(){
	if(adv_query_edit_barWidget){
		adv_query_edit_barWidget.close();
	}
	var dialogWidth = 550;
	var dialogHeight = 480;
	if(window.navigator.userAgent.indexOf('MSIE 8.0') > -1){
		dialogWidth = 548;
		dialogHeight = 470;
	} else if(window.navigator.userAgent.indexOf('MSIE 7.0') > -1){
		dialogWidth = 548;
		dialogHeight = 470;
	} else if(window.navigator.userAgent.indexOf('MSIE 6.0') > -1){
		dialogWidth = 535;
		dialogHeight = 460;
	}
	new CUI.Dialog({
		title : "${getHtmlText("ec.view.customerCondition")}",
		width : dialogWidth,
		height : dialogHeight,
		modal:true,
		elementId : 'advQueryDiv',
		onload: '${ccNameSpace}.initAdvQuery',
		buttons:[
		{	name:"${getHtmlText("foundation.common.save")}",
			handler:function(){
				var ret = ${ccNameSpace}.saveCustomerCon();
				if (ret !== false) {
					this.close();
				}
			},
		}, {	name:"${getHtmlText("foundation.common.closed")}",
			handler:function(){this.close()}
		}]
	}).show();
}

${ccNameSpace}.saveCustomerCon = function(){
	var	json = ${ccNameSpace}.query._getCond();
	if (json === 'error') return false;
	<#if dataGridCode?? && (dataGridCode?length gt 0)>
	CUI('body').data('currentCondundefined', '');
	</#if>
	<#if viewCode?? && (viewCode?length gt 0)>
	CUI('body').data('currentCond${viewCode}', '');
	</#if>
	var datas = {"dgQueryCond" : json, <#if dataGridCode?? && (dataGridCode?length gt 0)>"datagrid.code": "${dataGridCode}"</#if><#if viewCode?? && (viewCode?length gt 0)>"view.code": "${viewCode}"</#if>};
	var ret = true;
	CUI.ajax({
		url: "/msService/ec/customerCon/transtoSql"+(window._proj_config_flag?'?isProj=true':''),
		type: 'post',
		async: false,
		data: datas,
		success: function(msg) {
				if(msg.success==false || msg.data==null){
					${ccNameSpace}.showMsg('');
				}else{
					${ccNameSpace}.showMsg(msg.data);
				}
		},
		error: function(XMLHttpRequest, textStatus, errorThrown) {
			try {
				var msg = CUI.parseJSON(XMLHttpRequest.responseText);
				adv_query_edit_barWidget.show(msg.exceptionMsg);
				ret = false;
			} catch(e){}
		}
	});
	if (ret){
		$('#customerJsonString').val(json);
	}
	return ret;
}

${ccNameSpace}.submitCustomerCon = function(){
	var json = $('#customerJsonString').val();
	var datas;
	if($('#' + '${checkBoxId}').prop('checked')){
		datas = {"sqlCond" : $('#' + '${showArea}').val(), <#if dataGridCode?? && (dataGridCode?length gt 0)>"datagrid.code": "${dataGridCode}"</#if><#if viewCode?? && (viewCode?length gt 0)>"view.code": "${viewCode}"</#if>};
	}else{
		datas = {"dgQueryCond" : json, <#if dataGridCode?? && (dataGridCode?length gt 0)>"datagrid.code": "${dataGridCode}"</#if><#if viewCode?? && (viewCode?length gt 0)>"view.code": "${viewCode}"</#if>};
	}
	<#if dataClassific??>
		datas['classific.code'] = "${dataClassific.code}";
	<#else>
		if(typeof($('#dataClassificName').val()) != 'undefined'){
			datas['classific.code'] = $('#dataClassificCode').val();
		}
	</#if>
	CUI.ajax({
		url: "/msService/ec/customerCon/saveCusCon"+(window._proj_config_flag?'?isProj=true':''),
		type: 'post',
		async: false,
		data: datas,
		success: function(msg) {
		}
	});
	$('#customerJsonString').val('');
	$('#dataClassificCode').val('');
}

${ccNameSpace}.showMsg = function(msg){
		$('#' + '${showArea}').val(msg);
}

${ccNameSpace}.initAdvQuery = function(_advDialog){
	$('#advQueryContainer').empty();
	//$('#savedCondList').empty();
	if(_advDialog.isShow == 0) {
		setTimeout(function(){${ccNameSpace}.initAdvQuery(_advDialog);}, 300);
		return;
	}
	// 根据查询实体初始化高级查询容器
	if(CUI('#BBIT_DP_CONTAINER')!=null&&CUI('#BBIT_DP_CONTAINER').length>0) {
		CUI('#BBIT_DP_CONTAINER').remove();
	}
	var json = $('#customerJsonString').val();
	<#if dataGridCode?? && (dataGridCode?length gt 0)>
	CUI('body').data('currentCondundefined', CUI.parseJSON(json));
	</#if>
	<#if viewCode?? && (viewCode?length gt 0)>
	CUI('body').data('currentCond${viewCode}', CUI.parseJSON(json));
	</#if>
	${ccNameSpace}.query = new CUI.AdvQuery({
		elementId: 'advQueryContainer',
		namespace: '${ccNameSpace}',
		env: (window._proj_config_flag?'proj':'ec')
		<#if dataGridCode?? && (dataGridCode?length gt 0)>
		,"dataGridCode": "${dataGridCode}"
		</#if>
		<#if viewCode?? && (viewCode?length gt 0)>
		,"viewCode": "${viewCode}"
		</#if>
		<#if modelCode?? && (modelCode?length gt 0)>
		,modelCode:"${modelCode}"
		</#if>	
		<#if modelName?? && (modelName?length gt 0)>
		,modelName:"${modelName}".substr(0,1).toLowerCase()+"${modelName}".substr(1)
		<#else>
		,modelName:""
		</#if>	
	});
	var value = $('#dataClassificCode').val();
	if(json=='' && typeof($('#dataClassificName').val()) == 'undefined'){
		CUI.ajax({
			url: "/msService/ec/customerCon/getCustomerCondition"+(window._proj_config_flag?'?isProj=true':''),
			type: 'post',
			async: false,
			data: {<#if dataGridCode?? && (dataGridCode?length gt 0)>"dataGridCode": "${dataGridCode}"</#if><#if viewCode?? && (viewCode?length gt 0)>"viewCode": "${viewCode}"</#if>},
			success: function(msg) {
				if(msg.success==false || msg.data==null){
					return;
				}else{
					${ccNameSpace}.query._resume(null, eval('(' + msg.data + ')'));
				}
			}
		});
	}else if(typeof(value) != 'undefined' && value != ""){
		if(json != ''){
			${ccNameSpace}.query._resume(null, eval('(' + json + ')'));
		}else{
			CUI.ajax({
				url: "/msService/ec/customerCon/getCustomerCondition"+(window._proj_config_flag?'?isProj=true':''),
				type: 'post',
				async: false,
				data: {"dataClassificCode" : value},
				success: function(msg) {
					if(msg.success==false || msg.data==null){
						return;
					}else{
						${ccNameSpace}.query._resume(null, eval('(' + msg.data + ')'));
					}
				}
			});
		}
	}
	
	${ccNameSpace}.query.staff = {};
	${ccNameSpace}.query.staff.id='${getCurrent('staffId')}';
	${ccNameSpace}.query.staff.name='${getCurrent('staffName')}';
	${ccNameSpace}.query.position = {};
	${ccNameSpace}.query.position.id='${getCurrent('positionId')}';
	${ccNameSpace}.query.position.name='${getCurrent('positionName')}';
	${ccNameSpace}.query.department = {};
	${ccNameSpace}.query.department.id='${getCurrent('departmentId')}';
	${ccNameSpace}.query.department.name='${getCurrent('departmentName')}';
}

${ccNameSpace}._selectEvent = function(obj,objName,_selectType,url,title,refparam){
	var arr = objName.split('.');
	${ccNameSpace}.currObj = obj;
	${ccNameSpace}.sUrl = url;
	if(arr[0]) {
		${ccNameSpace}._prefix = arr[0];
	}
	${ccNameSpace}._dialog = foundation.common.select({
		pageType : _selectType,
		closePage : true,
		callBackFuncName : '${ccNameSpace}.getcallBackInfo',
		url : url,
		title : title
	});
}

${ccNameSpace}.getcallBackInfo = function(obj){
	obj[0] = foundation.common.getObject(${ccNameSpace}.currObj,obj[0], ${ccNameSpace}._prefix, ${ccNameSpace}.sUrl);
	<#--如果有自定义回调函数，执行以上内容后，再执行自定义的函数-->
	CUI.commonFills(${ccNameSpace}.currObj,${ccNameSpace}._prefix,obj[0]);
	if(${ccNameSpace}._dialog) {
		${ccNameSpace}._dialog.close();
	}
}
</script>
</#macro>