<#-- Head   其他限制相关函数集合 -->
<#macro otherCondition dataGridCode="",viewCode="",env="ec", showArea="conditionArea", checkBoxId="isTransCondition", ccNameSpace="dg.advQuery",inputNameId="otherConfigName",inputTitleId="otherConfigTitle",inputMemoId="otherConfigMemo",inputCodeId="otherConfigMemo">
<style type="text/css">
.edv-a{border:1px #5C8CB5 solid;width:500px;_width:485px;height:45px;margin:0 auto;padding:3px;_padding-bottom:0px;}
</style>
<div id="otherQueryDiv" style="display:none">
	<@errorbar id="adv_query_edit_bar"></@errorbar>
	<input type="hidden" id="customerDataGridCode" value="${dataGridCode}" />
	<input type="hidden" id="customerViewCode" value="${viewCode}" />
	<input type="hidden" id="dataClassificCode" <#if dataClassific??>value="${dataClassific.code!}"<#else>value=""</#if> />
	<input type="hidden" id="otherJsonString" value="" />
	<input type="hidden"  id="otherConfigCode"  />
	<div id="toolbar"> 
		<a href="javascript:${ccNameSpace}.query._mergeGroup('${getText('foundation.advquery.and')}');"><span style="background: url('/bap/static/ec/images/and.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupand')}</span></a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._mergeGroup('${getText('foundation.advquery.or')}');"><span style="background: url('/bap/static/ec/images/or.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupor')}</span></a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._changeGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.changeor')}</a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._changeGroup('${getText('foundation.advquery.and')}');">${getHtmlText('foundation.advquery.changeand')}</a>&nbsp;&nbsp;
		<a href="javascript:${ccNameSpace}.query._releaseGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.releasegroup')}</a>&nbsp;&nbsp;
	</div>
	<div id="outerContainer">
		<div id="otherQueryContainer">
	
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
		elementId : 'otherQueryDiv',
		onload: '${ccNameSpace}.initAdvQuery',
		buttons:[
		{	name:"${getHtmlText("foundation.common.save")}",
			handler:function(){${ccNameSpace}.saveCustomerCon();this.close();},
		}, {	name:"${getHtmlText("foundation.common.closed")}",
			handler:function(){this.close()}
		}]
	}).show();
}

${ccNameSpace}.saveCustomerCon = function(){
	var	json = ${ccNameSpace}.query._getCond();
	<#if viewCode?? && (viewCode?length gt 0)>
	CUI('body').data('currentCond${viewCode}', '');
	</#if>
	var datas = {"dgQueryCond" : json, <#if viewCode?? && (viewCode?length gt 0)>"viewCode": "${viewCode}"</#if>};
	CUI.ajax({
		url: "/foundation/otherRestrict/transtoSql",
		type: 'post',
		async: false,
		data: datas,
		success: function(msg) {
				if(msg.success==false || msg.data==null){
					${ccNameSpace}.showMsg('');
				}else{
					${ccNameSpace}.showMsg(msg.data);
				}
		}
	});
	$('#otherJsonString').val(json);
	
}

${ccNameSpace}.submitCustomerCon = function(){
	var json = $('#otherJsonString').val();
	if(json=='') {
		return;
	}
	var datas;
	if($('#' + '${checkBoxId}').prop('checked')){
		datas = {"sqlCond" : $('#' + '${showArea}').val(), <#if dataGridCode?? && (dataGridCode?length gt 0)>"datagridCode": "${dataGridCode}"</#if><#if viewCode?? && (viewCode?length gt 0)>"viewCode": "${viewCode}"</#if>};
		datas['isHandWriting'] = 1;
	}else{
		datas = {"dgQueryCond" : json, <#if dataGridCode?? && (dataGridCode?length gt 0)>"datagridCode": "${dataGridCode}"</#if><#if viewCode?? && (viewCode?length gt 0)>"viewCode": "${viewCode}"</#if>};
		datas['isHandWriting'] = 0;
	}
	<#if dataClassific??>
		datas['classific.code'] = "${dataClassific.code}";
	<#else>
		if(typeof($('#dataClassificName').val()) != 'undefined'){
			datas['classific.code'] = $('#dataClassificCode').val();
		}
	</#if>
	datas['conditionName'] = $('#' + '${inputNameId}').val();
	datas['conditionTitle'] = $('#' + '${inputTitleId}').val();
	datas['conditionMemo'] = $('#' + '${inputMemoId}').val();
	datas['code'] = $('#' + '${inputCodeId}').val();
	CUI.ajax({
		url: "/foundation/otherRestrict/saveOtherRestrict",
		type: 'post',
		async: false,
		data: datas,
		success: function(msg) {
		}
	});
	$('#otherJsonString').val('');
	$('#dataClassificCode').val('');
	
}

${ccNameSpace}.showMsg = function(msg){
		$('#' + '${showArea}').val(msg);
}

${ccNameSpace}.initAdvQuery = function(_advDialog){
	$('#otherQueryContainer').empty();
	//$('#savedCondList').empty();
	if(_advDialog.isShow == 0) {
		setTimeout(function(){${ccNameSpace}.initAdvQuery(_advDialog);}, 300);
		return;
	}
	// 根据查询实体初始化高级查询容器
	if(CUI('#BBIT_DP_CONTAINER')!=null&&CUI('#BBIT_DP_CONTAINER').length>0) {
		CUI('#BBIT_DP_CONTAINER').remove();
	}
	var json = $('#otherJsonString').val();
	<#if dataGridCode?? && (dataGridCode?length gt 0)>
	CUI('body').data('currentCondundefined', CUI.parseJSON(json));
	</#if>
	<#if viewCode?? && (viewCode?length gt 0)>
	CUI('body').data('currentCond${viewCode}', CUI.parseJSON(json));
	</#if>

	${ccNameSpace}.query = new CUI.AdvQuery({
		elementId: 'otherQueryContainer',
		namespace: '${ccNameSpace}',
		env: "${env!}"
		<#if dataGridCode?? && (dataGridCode?length gt 0)>
		,"dataGridCode": "${dataGridCode}"
		</#if>
		<#if viewCode?? && (viewCode?length gt 0)>
		,"viewCode": "${viewCode}"
		</#if>
		
	});
	var value = $('#dataClassificCode').val();
	if(json=='' && typeof($('#dataClassificName').val()) == 'undefined'){
		CUI.ajax({
			url: "/foundation/otherRestrict/getOtherRestrict",
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
				url: "/foundation/otherRestrict/getOtherRestrict",
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