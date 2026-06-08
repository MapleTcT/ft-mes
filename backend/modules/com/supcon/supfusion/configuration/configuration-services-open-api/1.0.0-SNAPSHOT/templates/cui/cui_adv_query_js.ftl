<#-- Head -->
<#macro advQueryJs queryUrl,viewCode,idprefix,queryFunc="",ns="">
<style type="text/css">
.edv-a{border:1px #5C8CB5 solid;width:500px;_width:485px;height:45px;margin:0 auto;padding:3px;_padding-bottom:0px;}
</style>
<div id="advQueryDiv" style="display:none">
	<@errorbar id="adv_query_edit_bar"></@errorbar>
	<div id="toolbar"> 
		<a href="javascript:ec.advQuery.query._mergeGroup('${getText('foundation.advquery.and')}');"><span style="background: url('/bap/static/ec/images/and.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupand')}</span></a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query._mergeGroup('${getText('foundation.advquery.or')}');"><span style="background: url('/bap/static/ec/images/or.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupor')}</span></a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query._changeGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.changeor')}</a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query._changeGroup('${getText('foundation.advquery.and')}');">${getHtmlText('foundation.advquery.changeand')}</a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query._releaseGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.releasegroup')}</a>&nbsp;&nbsp;
	</div>
	
	<table id="advQueryLoading" style="width:100%;height:100%;text-align:center;"><tr><td><label class="advQuery-loading">${getText("foundation.common.data.waiting")}</label></td></tr></table>
	
	<div id="outerContainer">
		<div id="advQueryContainer">
	
		</div>
	</div>
	<div>
		
		<fieldset class="edv-a">
			<legend style="color:#4B92BD;">${getText('foundation.advquery.condition')}</legend>
			<div id="savedCondList"></div>
		</fieldset>
		<div id="cookieInfoDiv" style="display:none"></div>
	</div>
</div>
<script type="text/javascript">
CUI.ns("ec.advQuery");
ec.advQuery.currViewCode = '${(view.code)!}';
ec.advQuery.currObj = null;
ec.advQuery.getAdvQueryCond = function(){
	if(ec.advQuery.query.removedFlag) {
		return '';
	} else {
		var cond = ec.advQuery.query._getCond();
		if(cond!='error'){
			return cond;
		}
	}
}
ec.advQuery.saveAdvQueryCond = function(){
	return ec.advQuery.query._saveCond();
}
ec.advQuery.newAdvQueryCond = function(){
	ec.advQuery.query._new();
}
ec.advQuery.removeAdvQueryCond = function(){
	ec.advQuery.query.removedFlag = true;
}
ec.advQuery.saveAsAdvQueryCond = function(){
	return ec.advQuery.query._saveAsCond();
}
ec.advQuery._prefix = '';
ec.advQuery._dialog;
ec.advQuery.currObj;
ec.advQuery.sUrl;
ec.advQuery._selectEvent = function(obj,objName,_selectType,url,title,refparam){
	var arr = objName.split('.');
	ec.advQuery.currObj = obj;
	ec.advQuery.sUrl = url;
	if(arr[0]) {
		ec.advQuery._prefix = arr[0];
	}
	ec.advQuery._dialog = foundation.common.select({
		pageType : _selectType,
		closePage : true,
		callBackFuncName : 'ec.advQuery.getcallBackInfo',
		url : url,
		title : title
	});
}
<#--选择函数的回调函数 ,该回调函数只是将选择的对象的ID和name赋值回来，如果需要执行其他内容，可以通过手动配置callback的方式执行-->
ec.advQuery.getcallBackInfo = function(obj){
	obj[0] = foundation.common.getObject(ec.advQuery.currObj,obj[0], ec.advQuery._prefix, ec.advQuery.sUrl);
	<#--如果有自定义回调函数，执行以上内容后，再执行自定义的函数-->
	CUI.commonFills(ec.advQuery.currObj,ec.advQuery._prefix,obj[0]);
	if(ec.advQuery._dialog) {
		ec.advQuery._dialog.close();
	}
}
ec.advQuery.initAdvQuery = function(_advDialog) {
	// 针对页签模式进行优化, 只初始化一次,避免内存泄露
	if ( location.search.indexOf( 'openType=page' ) != -1 &&  ec.advQuery._renderOverFlag_ == true ) {
		$( '#advQueryLoading' ).hide();
		return;
	}
	$('#advQueryContainer').empty();
	$('#savedCondList').empty();
	if(_advDialog.isShow == 0) {
		setTimeout(function(){ec.advQuery.initAdvQuery(_advDialog);}, 300);
		return;
	}
	// 根据查询实体初始化高级查询容器
	if(CUI('#BBIT_DP_CONTAINER')!=null&&CUI('#BBIT_DP_CONTAINER').length>0) {
		CUI('#BBIT_DP_CONTAINER').remove();
	}
	ec.advQuery.query = new CUI.AdvQuery({
		elementId: 'advQueryContainer',
		env: 'runtime',
		viewCode: '${viewCode!}',
		formId: '${idprefix}_queryForm'
	});
	
	ec.advQuery.query.staff = {};
	ec.advQuery.query.staff.id='${getCurrent('staffId')}';
	ec.advQuery.query.staff.name='${getCurrent('staffName')}';
	ec.advQuery.query.position = {};
	ec.advQuery.query.position.id='${getCurrent('positionId')}';
	ec.advQuery.query.position.name='${getCurrent('positionName')}';
	ec.advQuery.query.department = {};
	ec.advQuery.query.department.id='${getCurrent('departmentId')}';
	ec.advQuery.query.department.name='${getCurrent('departmentName')}';
	// 初始化完成标识
	ec.advQuery._renderOverFlag_ = true;
}
</script>
<script type="text/javascript">
	function advQuery() {
		$('#advQueryLoading').show();
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
			title : "${getHtmlText("ec.view.advQuery")}",
			width : dialogWidth,
			height : dialogHeight,
			modal:true,
			elementId : 'advQueryDiv',
			onload: 'ec.advQuery.initAdvQuery',
			buttons:[
			{	name:"${getHtmlText("foundation.view.add")}",
				handler:function(){newAdvQueryCon();},
				align:"left"
			}, {	name:"${getHtmlText("foundation.common.save")}",
				handler:function(){saveAdvQueryCon();},
				align:"left"
			}, {	name:"${getHtmlText("foundation.common.saveAs")}",
				handler:function(){saveAsAdvQueryCon();},
				align:"left"
			}, {	name:"${getHtmlText("ec.common.query")}",
				handler:function(){
					<#if ns != "">
					if(typeof ${ns}.cancelSelectedNode == 'function') {
						${ns}.cancelSelectedNode();
					}
					</#if>
					var flag = advQueryAfter();
					<#if queryFunc != "">
					 datatable_${idprefix}_query.setAttributeConfig('queryFunc', {
		                writeOnce: true,
		                value: "${queryFunc}('adv')"
        			}); 
            		</#if>
            		if(flag!=false){
						this.close();
					}
				}
			}, {	name:"${getHtmlText("foundation.common.closed")}",
				handler:function(){this.close()}
			}]
		}).show();
	}
	function removeAdvQuery(){
		try{ec.advQuery.removeAdvQueryCond();}catch(e){}
	}
	function advQueryAfter(pageNo,pageSize){
		var advQueryCond = ec.advQuery.query._getCond();
		if(advQueryCond==='error'){
			return false;
		}
		var postUrl;
		var dataPost="";
		if($('#${idprefix}_queryForm #advQueryCond').length > 0) {
			$('#${idprefix}_queryForm #advQueryCond').val(advQueryCond);
			$('#${idprefix}_queryForm #advQueryCond').attr("reset","false");
			CUI.resetForm('${idprefix}_queryForm');
		}
		dataPost = "&advQueryCond=" + encodeURIComponent(advQueryCond);
		CUI('input,select', CUI('#${idprefix}_queryForm')).each(function(index){
			if(CUI(this).attr('name') && CUI(this).attr('typeUse') == 'forAll'){
				var fastCol = CUI(this).attr('name');
				var fastColValue = CUI('#${idprefix}_queryForm *[name="'+fastCol+'"]').val();
				if(fastColValue != null && fastColValue != "undefined" && fastColValue != "") {
					dataPost += "&" + fastCol + "=" + encodeURIComponent(fastColValue);
				}
			}
		});
		var pageSize=$('input[name="${idprefix}_query_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);
		if(null != pageNo && undefined != pageNo && "" != pageNo) {
			dataPost += "&page.pageNo="+encodeURIComponent(pageNo);	
		}		
		if($('#${idprefix}_query_selectClassify') && $('#${idprefix}_query_selectClassify').val() != '' && $('#${idprefix}_query_selectClassify').val() != undefined) {
	 		dataPost += "&classifyCodes=" + $('#${idprefix}_query_selectClassify').val();
	 	}
		var url = "${queryUrl}";
		${queryFunc}('adv');
		// ${idprefix}_queryWidget.setRequestDataUrl(url,dataPost);
		
	}
	function saveAdvQueryCon(){
		var advQueryCond = ec.advQuery.saveAdvQueryCond();
	}
	function newAdvQueryCon(){
		ec.advQuery.newAdvQueryCond();
	}
	function saveAsAdvQueryCon(){
		ec.advQuery.saveAsAdvQueryCond();
	}
</script>
</#macro>