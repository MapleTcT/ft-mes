<#-- Head -->
<#macro newAdvQueryJs queryUrl,viewCode,idprefix,queryFunc="",ns="",layoutName="",modelCode="">
<style type="text/css">
.edv-a{border:1px #5C8CB5 solid;width:500px;_width:485px;height:45px;margin:0 auto;padding:3px;_padding-bottom:0px;}
</style>
<div id="advQueryDiv" style="display:none">
	
	<div id="toolbar"> 
		<a href="javascript:ec.advQuery.query${layoutName!}._mergeGroup('${getText('foundation.advquery.and')}');"><span style="background: url('/bap/static/ec/images/and.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupand')}</span></a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query${layoutName!}._mergeGroup('${getText('foundation.advquery.or')}');"><span style="background: url('/bap/static/ec/images/or.gif') no-repeat;padding-left:18px;display:inline-block;height:15px;">${getHtmlText('foundation.advquery.groupor')}</span></a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query${layoutName!}._changeGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.changeor')}</a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query${layoutName!}._changeGroup('${getText('foundation.advquery.and')}');">${getHtmlText('foundation.advquery.changeand')}</a>&nbsp;&nbsp;
		<a href="javascript:ec.advQuery.query${layoutName!}._releaseGroup('${getText('foundation.advquery.or')}');">${getHtmlText('foundation.advquery.releasegroup')}</a>&nbsp;&nbsp;
	</div>
	
	
	
	<div id="outerContainer">
		<div id="advQueryContainer">
	
		</div>
	</div>
	<div>
		
		<fieldset class="edv-a">
			<legend style="color:#4B92BD;">${getText('foundation.advquery.condition')}</legend>
			<div id="savedCondList"></div>
		</fieldset>
		<div id="${layoutName!}cookieInfoDiv" style="display:none"></div>
	</div>
</div>
<script type="text/javascript">
CUI.ns("ec.advQuery${layoutName!}");
CUI.ns("ec.advQuery");
ec.advQuery${layoutName!}.currViewCode = '${(view.code)!}';
ec.advQuery${layoutName!}.currObj = null;
ec.advQuery${layoutName!}._renderOverFlag=false;
ec.advQuery${layoutName!}.getAdvQueryCond = function(){
	if(ec.advQuery.query${layoutName!}.removedFlag) {
		return '';
	} else {
		var cond = ec.advQuery.query${layoutName!}._getCond();
		if(cond!='error'){
			return cond;
		}
	}
}
ec.advQuery${layoutName!}.saveAdvQueryCond = function(){
	return ec.advQuery.query${layoutName!}._saveCond();
}
ec.advQuery${layoutName!}.newAdvQueryCond = function(){
	ec.advQuery.query${layoutName!}._new();
}
ec.advQuery${layoutName!}.removeAdvQueryCond = function(){
	ec.advQuery.query${layoutName!}.removedFlag = true;
}
ec.advQuery${layoutName!}.saveAsAdvQueryCond = function(){
	return ec.advQuery.query${layoutName!}._saveAsCond();
}
ec.advQuery${layoutName!}.showScheme = function(){
	return ec.advQuery.query${layoutName!}._showScheme();
}
ec.advQuery${layoutName!}._prefix = '';
ec.advQuery${layoutName!}._dialog;
ec.advQuery${layoutName!}.currObj;
ec.advQuery${layoutName!}.sUrl;
ec.advQuery${layoutName!}._selectEvent = function(obj,objName,_selectType,url,title,refparam){
	var arr = objName.split('.');
	ec.advQuery${layoutName!}.currObj = obj;
	ec.advQuery${layoutName!}.sUrl = url;
	if(arr[0]) {
		ec.advQuery${layoutName!}._prefix = arr[0];
	}
	var radionStr=(new Date()).getTime();
	ec.advQuery${layoutName!}._dialog = foundation.common.select({
		pageType : _selectType,
		closePage : true,
		multiSelect:false,
		callBackFuncName : 'ec.advQuery${layoutName!}.getcallBackInfo',
		url : url,
		iframe:"advQueryFrame"+radionStr,
		title : title
	});
}
<#--选择函数的回调函数 ,该回调函数只是将选择的对象的ID和name赋值回来，如果需要执行其他内容，可以通过手动配置callback的方式执行-->
ec.advQuery${layoutName!}.getcallBackInfo = function(obj){
	var radioarr=$('input:radio[name=selectRow]');
	var selectedRow=null;
	for(var i=0;i<radioarr.length;i++){
		if(radioarr[i].checked == true){
			selectedRow=CUI(radioarr.eq(i)).parent().parent();
			break;
		}
	}
	
	obj[0] = foundation.common.getObject(selectedRow,obj[0], ec.advQuery${layoutName!}._prefix, ec.advQuery${layoutName!}.sUrl);
	<#--如果有自定义回调函数，执行以上内容后，再执行自定义的函数-->
	CUI.commonFills(selectedRow,ec.advQuery${layoutName!}._prefix,obj[0]);
	if(ec.advQuery${layoutName!}._dialog) {
		ec.advQuery${layoutName!}._dialog.close();
	}
}
ec.advQuery${layoutName!}.initAdvQuery = function(_advDialog) {
	// 针对页签模式进行优化, 只初始化一次,避免内存泄露
	if ( location.search.indexOf( 'openType=page' ) != -1 &&  ec.advQuery${layoutName!}._renderOverFlag_ == true ) {
		$( '#advQueryLoading' ).hide();
		return;
	}
	$('#advQueryContainer').empty();
	$('#savedCondList').empty();
	if(_advDialog.isShow == 0) {
		setTimeout(function(){ec.advQuery${layoutName!}.initAdvQuery(_advDialog);}, 300);
		return;
	}
	// 根据查询实体初始化高级查询容器
	if(CUI('#BBIT_DP_CONTAINER')!=null&&CUI('#BBIT_DP_CONTAINER').length>0) {
		CUI('#BBIT_DP_CONTAINER').remove();
	}
	ec.advQuery.query${layoutName!} = new CUI.AdvQuery({
		elementId: 'advQueryContainer',
		env: 'runtime',
		viewCode: '${viewCode!}',
		formId: '${idprefix}_queryForm'
	});
	
	ec.advQuery.query${layoutName!}.staff = {};
	ec.advQuery.query${layoutName!}.staff.id='${getCurrent('staffId')}';
	ec.advQuery.query${layoutName!}.staff.name='${getCurrent('staffName')}';
	ec.advQuery.query${layoutName!}.position = {};
	ec.advQuery.query${layoutName!}.position.id='${getCurrent('positionId')}';
	ec.advQuery.query${layoutName!}.position.name='${getCurrent('positionName')}';
	ec.advQuery.query${layoutName!}.department = {};
	ec.advQuery.query${layoutName!}.department.id='${getCurrent('departmentId')}';
	ec.advQuery.query${layoutName!}.department.name='${getCurrent('departmentName')}';
	// 初始化完成标识
	ec.advQuery${layoutName!}._renderOverFlag_ = true;
}
ec.advQuery${layoutName!}.newInitAdvQuery = function(_advDialog) {
	// 针对页签模式进行优化, 只初始化一次,避免内存泄露
	if ( location.search.indexOf( 'openType=page' ) != -1 &&  ec.advQuery${layoutName!}._renderOverFlag_ == true ) {
		$( '#advQueryLoading' ).hide();
		return;
	}
	$('#advQueryContainer').empty();
	$('#savedCondList').empty();
	if(_advDialog.isShow == 0) {
		setTimeout(function(){ec.advQuery${layoutName!}.initAdvQuery(_advDialog);}, 300);
		return;
	}
	// 根据查询实体初始化高级查询容器
	if(CUI('#BBIT_DP_CONTAINER')!=null&&CUI('#BBIT_DP_CONTAINER').length>0) {
		CUI('#BBIT_DP_CONTAINER').remove();
	}
	ec.advQuery.query${layoutName!} = new CUI.AdvQuery({
		elementId: 'advQueryContainer',
		env: 'runtime',
		viewCode: '${viewCode!}',
		formId: '${idprefix}_queryForm'
	});
	
	ec.advQuery.query${layoutName!}.staff = {};
	ec.advQuery.query${layoutName!}.staff.id='${getCurrent('staffId')}';
	ec.advQuery.query${layoutName!}.staff.name='${getCurrent('staffName')}';
	ec.advQuery.query${layoutName!}.position = {};
	ec.advQuery.query${layoutName!}.position.id='${getCurrent('positionId')}';
	ec.advQuery.query${layoutName!}.position.name='${getCurrent('positionName')}';
	ec.advQuery.query${layoutName!}.department = {};
	ec.advQuery.query${layoutName!}.department.id='${getCurrent('departmentId')}';
	ec.advQuery.query${layoutName!}.department.name='${getCurrent('departmentName')}';
	// 初始化完成标识
	ec.advQuery${layoutName!}._renderOverFlag_ = true;
}
</script>
<script type="text/javascript">
	var values={'staff':{'id':'${getCurrent('staffId')}','name':'${getCurrent('staffName')}'},'position':{'id':'${getCurrent('positionId')}','name':'${getCurrent('positionName')}'},'department':{'id':'${getCurrent('departmentId')}','name':'${getCurrent('departmentName')}'}};
		$(function(){
			if(ec.advQuery${layoutName!}.query!=undefined){
				var advContainer = $("[id='"+ec.advQuery.query${layoutName!}.olddiv+"']");
				advContainer.length>1 && advContainer.eq(0).remove();
				ec.advQuery.query${layoutName!}=undefined;
			}
			ec.view.newAdvQuery${layoutName!}._initNewAdvQuery(values);
		});
		
	function advQuery(views) {
		if(adv_query_edit_${layoutName!}barWidget){
			adv_query_edit_${layoutName!}barWidget.close();
		}
		var dialogWidth = 760;
		var dialogHeight = 480;
		//if(window.navigator.userAgent.indexOf('MSIE 8.0') > -1){
		//	dialogWidth = 758;
		//	dialogHeight = 510;
	//	} else if(window.navigator.userAgent.indexOf('MSIE 7.0') > -1){
	//		dialogWidth = 758;
	//		dialogHeight = 510;
	//	} else if(window.navigator.userAgent.indexOf('MSIE 6.0') > -1){
	//		dialogWidth = 745;
	//		dialogHeight = 500;
	//	}
		new CUI.Dialog({
			title : "${getHtmlText("ec.view.advQuery")}",
			width : dialogWidth,
			height : dialogHeight,
			modal:true,
			elementId : views,
			onload:'_showSavebtn',
			buttons:[
			 {	name:"${getHtmlText("ec.common.query")}",
				handler:function(){
					<#if ns != "">
					if(typeof ${ns}.cancelSelectedNode == 'function') {
						${ns}.cancelSelectedNode();
					}
					</#if>
					var flag = ${layoutName!}advQueryAfter();
					<#if !layoutName??>
					<#if queryFunc != "">
					 datatable_${idprefix}_query.setAttributeConfig('queryFunc', {
		                writeOnce: true,
		                value: "${queryFunc}('adv')"
        			}); 
            		</#if>
            		</#if>
            		if(flag!=false){
						this.close();
					}
				}
			}, {
				name:"${getHtmlText("ec.common.clear")}",
				handler:function(){clearAdvQuery();}
			},{	name:"${getHtmlText("foundation.common.closed")}",
				handler:function(){this.close()}
			}]
		}).show();		
	}
	
	function clearAdvQuery(){
		var advQueryObj = CUI('.advConContent .quick_query_node');
		advQueryObj.each(function(){
			CUI('.cui-noborder-input',CUI(this)).val('');
			CUI('.fix-search-click',CUI(this)).find('input').eq(0).val('');
			CUI('.advQuery_content:first',CUI(this)).find('input').eq(1).val('');
			CUI('select',CUI(this)).val('');
			CUI('.dropselectbox h4',CUI(this)).text('');
			CUI('.dropselectbox h4',CUI(this)).attr('title','');
			CUI('.mne-tip.search-ft-color',CUI(this)).css("display","block");
			// 同时清除多选系统编码下拉框	
			var input = CUI('.fix-search-click',CUI(this)).find('input').eq(0);
			if (input.attr('validatetype') === 'SystemCode' && input.attr('name')){			
				$( '#adv_'+ input.attr('name') +'_clearbtn' ).click();
			}
		});
		var subs = $("tr.newadvFilter");
		subs.each(function(i){	
			CUI('.cui-noborder-input',CUI(this).children()[5]).val('');
		});
	}
	
	function removeAdvQuery(){
		try{ec.advQuery${layoutName!}.removeAdvQueryCond();}catch(e){}
	}
	function ${layoutName!}advQueryAfter(pageNo,pageSize){
		var advQueryCond = ec.advQuery.query${layoutName!}._getCond();
		//console.log("----------------");
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
	function ${layoutName!}saveAdvQueryCon(){
		if(!ec.advQuery${layoutName!}._renderOverFlag){
			ec.view.newAdvQuery${layoutName!}._customQuery();
		}
		ec.advQuery${layoutName!}.saveAdvQueryCond();
	}
	function ${layoutName!}newAdvQueryCon(){
		ec.advQuery${layoutName!}.newAdvQueryCond();
	}
	function ${layoutName!}saveAsAdvQueryCon(){
		if(!ec.advQuery${layoutName!}._renderOverFlag){
			ec.view.newAdvQuery${layoutName!}._customQuery();
		}
		ec.advQuery${layoutName!}.saveAsAdvQueryCond();
	}
	function ${layoutName!}showScheme(){
		if(!ec.advQuery${layoutName!}._renderOverFlag){
			ec.view.newAdvQuery${layoutName!}._customQuery();
		}
		ec.advQuery${layoutName!}.showScheme();
	}
	function _showSavebtn(views){
		var dialogId = "dialog_"+$.trim(views.id);
		$('<div id="custom_btn" style="font-size:18px;display:none;float:left;margin-top:10px;margin-left:15px;"><a href="#" class="cui-label  cui-label-save" onclick="saveAdvQueryCon()">'+"${getHtmlText('foundation.common.save')}"+'</a><a href="#" class="cui-label cui-label-save" onclick="saveAsAdvQueryCon()">'+"${getHtmlText('foundation.common.saveAs')}"+'</a><a href="#" class="cui-label cui-label-query" onclick="showScheme()">'+"${getHtmlText('foundation.advQuery.plan')}"+'</a></div>').insertBefore("div[id='"+dialogId+"'] span.ewc-dialog-button-right");
		$("[id='" + dialogId + "'] #custom_btn").css('display','block');
	}
	
</script>
</#macro>

<#macro newExtraAdvQueryJs  viewCode = "", url = "", layoutName = "", ns = "",queryUrl = "", queryFunc = "", idprefix = ""  >
	<script type="text/javascript">
		function ${layoutName}advQuery(views,obj) {
			var dialogWidth = 760;
			var dialogHeight = 480;
			new CUI.Dialog({
				title : "${getHtmlText("ec.view.advQuery")}",
				width : dialogWidth,
				height : dialogHeight,
				modal:true,
				elementId : views,
				onload:'${layoutName}_showSavebtn',
				buttons:[
				 {	name:"${getHtmlText("ec.common.query")}",
					handler:function(){
						<#if ns?? && ns != "">
						if(typeof ${ns}.cancelSelectedNode == 'function') {
							${ns}.cancelSelectedNode();
						}
						</#if>
						var flag = ${layoutName!}advQueryAfter();
						<#if !layoutName??>
						<#if queryFunc?? && queryFunc != "">
						 datatable_${idprefix}_query.setAttributeConfig('queryFunc', {
							writeOnce: true,
							value: "${queryFunc}('adv')"
						}); 
						</#if>
						</#if>
						if(flag!=false){
							this.close();
						}
					}
				}, {
					name:"${getHtmlText("ec.common.clear")}",
					handler:function(){${layoutName}clearAdvQuery();}
				},{	name:"${getHtmlText("foundation.common.closed")}",
					handler:function(){this.close()}
				}]
			}).show();
		}
		
		function ${layoutName}clearAdvQuery(){
			var advQueryObj = CUI('.advConContent .quick_query_node');
			advQueryObj.each(function(){
				CUI('.cui-noborder-input',CUI(this)).val('');
				CUI('.fix-search-click',CUI(this)).find('input').eq(0).val('');
				CUI('.advQuery_content:first',CUI(this)).find('input').eq(1).val('');
				CUI('select',CUI(this)).val('');
				CUI('.dropselectbox h4',CUI(this)).text('');
				CUI('.dropselectbox h4',CUI(this)).attr('title','');
				CUI('.mne-tip.search-ft-color',CUI(this)).css("display","block");
			});
			var subs = $("tr.newadvFilter");
			subs.each(function(i){	
				CUI('.cui-noborder-input',CUI(this).children()[5]).val('');
			});
		}
		
		function ${layoutName!}advQueryAfter(pageNo,pageSize){
			var advQueryCond = ec.advQuery.query${layoutName!}._getCond();
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
		
		function ${layoutName}_showSavebtn(views){
			var dialogId = "dialog_"+$.trim(views.id);
			$('<div id="custom_btn" style="font-size:18px;display:none;float:left;margin-top:10px;margin-left:15px;"><a href="#" class="cui-label  cui-label-save" onclick="${layoutName}saveAdvQueryCon()">'+"${getHtmlText('foundation.common.save')}"+'</a><a href="#" class="cui-label cui-label-save" onclick="${layoutName}saveAsAdvQueryCon()">'+"${getHtmlText('foundation.common.saveAs')}"+'</a><a href="#" class="cui-label cui-label-query" onclick="${layoutName}showScheme()">'+"${getHtmlText('foundation.advQuery.plan')}"+'</a></div>').insertBefore("div[id='"+dialogId+"'] span.ewc-dialog-button-right");
			$("[id='" + dialogId + "'] #custom_btn").css('display','block');
		}
	 </script>
</#macro>