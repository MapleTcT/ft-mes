<style type="text/css">
#ec_classific_edit_form .infoTable tr{height:23px;}
#ec_classific_edit_form .infoTable tr td {padding-bottom:0px;height:23px;}
#ec_classific_edit_form .infoTable td.la {padding-right:2px;}
</style>
<@errorbar id="ec_classific_edit_formDialogErrorBar" />
<form id="ec_classific_edit_form" action="dataclassificsave" name="ec_classific_edit_form" namespace="/msService/ec/view" validate="true" callback="ec.dataclassific.Callback">
	<input type="hidden"  name="dataClassific.version" id="ec_classific_edit_form_dataClassific_version"  <#if dataClassific?? && dataClassific.version??>value="${dataClassific.version}" <#else>value="0"</#if> />
	<#--<@s.hidden  name="dataClassific.id" />-->
	<input type="hidden"  name="dataClassific.code" id="ec_classific_edit_form_dataClassific_code"  <#if dataClassific?? && dataClassific.code??>value="${dataClassific.code}"</#if>  />
	<input type="hidden"  name="dataClassific.isDefault" id="ec_classific_edit_form_dataClassific_isDefault"  <#if dataClassific?? && dataClassific.isDefault??>value="${dataClassific.isDefault?string}"</#if>/>
	<input type="hidden"  name="dataClassific.dataGroup.code" id="ec_classific_edit_form_dataClassific_dataGroup_code" <#if dataClassific?? && dataClassific.dataGroup?? && dataClassific.dataGroup.code?? >value="${dataClassific.dataGroup.code}"</#if>/>
	<#if targetModel??>
	<input type="hidden"  name="targetModel.code" value="${targetModel.code}"/>
	</#if>
	<input type="hidden" name ="isProj" id="isProj" value="${(isProj?c)!}" />
	<table class="infoTable" id="ec_classific_edit_table" align="center" cellpadding="0" cellspacing="0" border="0" style="margin-top:8px;width:90%;">
		<tr>
			<td class="la cui-lmust">${getHtmlText('ec.dataclassific.code')}</td>
			<td class="co">
				<#if dataClassific?? && dataClassific.name??>
				<input type="text"  id="dataClassificName" name="dataClassific.name" value="${dataClassific.name}" readonly="true" cssClass="cui-readonly-field"/>
				<#else>	
				<input type="text"  id="dataClassificName" name="dataClassific.name" cssClass="cui-edit-field"/>
				</#if>
			</td>
			<td style="width:4%;"></td>
			<td class="la cui-lmust">${getHtmlText('ec.dataclassific.name')}</td>
			<td class="co">
				<#if dataClassific??>
					<@international name="dataClassific.displayName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact!}" key="${dataClassific.displayName!}" cssClass="cui-edit-field"></@international>
				<#else>
					<@international name="dataClassific.displayName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact!}" key="" cssClass="cui-edit-field"></@international>
				</#if>
			</td>
		</tr>
		<tr>
			<td class="la">默认查询</td>
			<#if dataClassific??>
				<td class="co"><input id="dataClassificIsDefault" type="checkbox" onclick="changeDataClassificIsDefault(this);" <#if dataClassific.isDefault == true>checked </#if> />
			<#else>
				<td class="co"><input id="dataClassificIsDefault" type="checkbox" onclick="changeDataClassificIsDefault(this);"  />
			</#if>
			
		</tr>
		<tr>
			<td class="la" colspan="3"  style="height:40px;padding-top:8px;">
			<div id="66387034_button_1" canClick="true" class="edit-btn btn-act" onclick="showDialog()"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c">${getHtmlText('ec.view.customerCondition')}</a><a class="cui-btn-r">&nbsp;</a></div>
			</td>
			<td class="la"></td>
			<td class="co" style="text-align:right;height:40px;padding-top:11px;"><input id="dataClassificBox" type="checkbox" /><label class="cui-label-font">${getHtmlText('ec.dataClassific.condition.edit')}</label>
			<span id="conditionHelpinfo" class="baphelp-icon"></span>
			<div id="conditionHelpinforef" style="display:none">
	<p class="baphelp-info">可动态传递参数拼接到条件中，并根据变量进行逻辑判断</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<#noparse><pre><code><span>if(parameters.test[0]=="1"){</span>
	return "(\"p\".ID =\${deptId,Long} OR \"p\".LAY_REC LIKE \${layrec,String})";
}else{
	return "(\"p\".ID =\${deptId,String} OR \"p\".LAY_REC LIKE \${layrec,String})";
}</code></pre></#noparse>
</div>
<p class="baphelp-hint">注意：test 为前台传的参数，post 或get 即可</p>
</div>

					<script type="text/javascript">
							$('#conditionHelpinfo').helptip({refElm: "#conditionHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
				</script>
			</td>			
		</tr>
		<tr>
			<td class="co" colspan="5">
				<textarea name="dataClassific.condition" cols="" rows="" id="dataClassificArea" class="editTextarea" style="border:1px solid #A5C7EF;width:100%;height:190px" readonly="readonly"><#if dataClassific?? && dataClassific.condition??>${dataClassific.condition} </#if></textarea></td>
		</tr>
	</table>
</form>
<script type="text/javascript">
	function showDialog(){
		if($('#66387034_button_1').attr('canClick')=="true"){
			dataclassifc.condition.showAdv();
		}
	}

	(function(){
		$('#dataClassificArea').attr("readonly", "readonly");
		$('#dataClassificBox').bind('click',function(e){
		    var self = this;
			if(this.checked) {
				$('#dataClassificArea').removeAttr("readonly");
				$('#66387034_button_1').attr('class', "edit-btn-disabled");
				$('#66387034_button_1').attr('canClick', 'false');
			}else {
				if(e && e.preventDefault) {
				　　e.preventDefault();
				} else {
				　　window.event.returnValue = false;
				}
				var json = $('#customerJsonString').val();
				CUI.Dialog.confirm("${getHtmlText('ec.view.leavehandwriting')}", function(){	
					self.checked = false;
					$('#dataClassificArea').val('');
					$('#dataClassificArea').attr('readonly', 'true');
					$('#66387034_button_1').attr('class', "edit-btn btn-act");
					$('#66387034_button_1').attr('canClick', 'true');
					var datas = {"dgQueryCond" : json};
					CUI.ajax({
						url: "/msService/ec/customerCon/transtoSql",
						type: 'post',
						async: false,
						data: datas,
						success: function(msg) {
								if(msg.success==false || msg.data==null){
									dataclassifc.condition.showMsg('');
								}else{
									dataclassifc.condition.showMsg(msg.data);
								}
						}
					});
				});
			}
		});
	})();

	<#if dataClassific?? &&dataClassific.code??>
	(function(){
		var datas = {"dataClassificCode" : "${dataClassific.code}"};
		CUI.ajax({
			url: "/msService/ec/customerCon/getCustomerCondition",
			type: 'post',
			async: false,
			data: datas,
			success: function(msg) {
				if(msg.success==true){
					$('#customerJsonString').val(msg.data);
				}else{
					$('#customerJsonString').val('');
				}
			}
		});
		CUI.ajax({
			url: "/msService/ec/customerCon/getShowSql",
			type: 'post',
			async: false,
			data: datas,
			success: function(msg) {
				if(msg.success==true){
					$('#dataClassificBox').prop('checked', true);
					$('#dataClassificArea').removeAttr("readonly");
					$('#66387034_button_1').attr('class', "edit-btn-disabled");
					$('#66387034_button_1').attr('canClick', 'false');
				}
				if(msg.data==null){
					$('#dataClassificArea').val('');
				}else{
					$('#dataClassificArea').val(msg.data);
				}
			}
		});
	})();
	</#if>
	function changeDataClassificIsDefault(obj){
		if(obj.checked){
			var datas = {"dataClassific.code" : $('input[name = "dataClassific.code"]').val() ,"view.code" : "${view.code}"};
			CUI.ajax({
				url: "/msService/ec/view/wouldDefaultChecked",
				type: 'post',
				async: false,
				data: datas,
				success: function(msg) {
					if(msg.isExist==true){
						console.log(msg.dataClassific);
						CUI.Dialog.confirm(  
						    '分类组别为'+msg.dataClassific.dataGroup.displayNameInternational+'中的'+msg.dataClassific.name+'存在默认选项，选择后保存会覆盖视图中其他默认选项',  // 提示消息  
						    function(){$('input[name="dataClassific.isDefault"]').val(obj.checked);}, // 确定后事件  
						    function(){obj.checked = false;$('input[name="dataClassific.isDefault"]').val(obj.checked);}, // 取消后事件  
						);  
					}else{
						$('input[name="dataClassific.isDefault"]').val(obj.checked);
					}
				}
			});
		}else{
			$('input[name="dataClassific.isDefault"]').val(obj.checked);
		}
	}


</script>
<script>
	CUI(function(){
			function submitBapForm(){//电子签名成功之后出现进度条并提交表单
				var ecFormFlag = false;
				var retrialFormFlag = false;
				if(ecFormFlag && ( $('#ec_classific_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
					// dialog不出进度条
					ecFormFlag = false;
				}
				ecFormFlag = (ecFormFlag || retrialFormFlag);
				
				//前台验证通过之后出进度条
				CUI.Dialog.toggleAllButton('ec_classific_edit_form',true,ecFormFlag, true);
			// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
			setTimeout(function(){
				
					// 延迟保存数据, 解决onchange事件无法触发问题
					var formId = 'ec_classific_edit_form';
					var ecformflag = false;
					
					
					$('input[type="text"]','#'+formId).each(function(){
						var v=$.trim($(this).val());
						$(this).val(v);
					});
					var files = $('input[type="file"]', '#' + formId);
					if(ecformflag || (files!=null&&files.length>0)) {
						ajaxFileUpload(CUI('#'+formId).attr('action'),formId);
					} else {
					
					var postData = CUI('#'+formId).serialize();
					CUI.ajax({
						url : CUI('#'+formId).attr('action'),
						type : 'POST',
						dataType : 'json',
						data : postData,
						error : function(XMLHttpRequest, textStatus, errorThrown){
							//console.log("jqXHR=%o,textStatus=%o,errorThrown=%o", XMLHttpRequest, textStatus, errorThrown );
							if (XMLHttpRequest.status==401) {
								//showLoginDialog();
								return ;
							}
							var msg = CUI.parseJSON(XMLHttpRequest.responseText);
							var errorMsgs = "";
							CUI.each(msg.items,function(index,item){
								if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
									$("#ec_classific_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
										if($(this).parents('td[nullable=false]').length > 0) {
											showErrorField($(this));
										}
									});
								} else {
									var field = CUI("#ec_classific_edit_form *[name='"+index+"']");
									if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
										showErrorField(field.next());
									} else {
										showErrorField(field);
									}
								}
								CUI("#ec_classific_edit_form *[name='"+index+"']").first().focus();
								for(var i = 0 ; i < item.length ; i++){
									errorMsgs += item[i] + '<br/>';
								}
							});
							CUI.each(msg.actionErrors,function(index,item){
								errorMsgs += item + '<br/>';
							});
							if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
								errorMsgs += msg.exceptionMsg + '<br/>';
							}
							var oErrorWidget = null;
							oErrorWidget = ec_classific_edit_formDialogErrorBarWidget;
							if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
								oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
							}	else {
								oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
							}					
							if(CUI.Dialog){
								CUI.Dialog.toggleAllButton('ec_classific_edit_form', true);
							}
						},
						success : function(msg){
							window.onbeforeunload = null;
							if(window.containerLoadPanelWidget) {
								setTimeout(function(){closeLoadPanel();}, 500);
							}
							ec.dataclassific.Callback(msg,postData);
						}
					});
				}
			}, 600);
				return false;
			}

			CUI('#ec_classific_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
				//每次提交时先隐藏报错信息
				try{
				}catch(e){
				
				}
				// 清除错误标红
				try{clearErrorLabels();}catch(e){}
				var ecFormFlag = false;
				var retrialFormFlag = false;
				if(ecFormFlag && ( $(this).parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
					// dialog不出进度条
					ecFormFlag = false;
				}
				ecFormFlag = (ecFormFlag || retrialFormFlag);
				
				
				//禁用所有按钮
				//CUI("body").one("click", function(event){
				//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_classific_edit_form',true,ecFormFlag);
				//});
					//if(!validateForm_ec_classific_edit_form())	return false;
				$('#ec_classific_edit_form').trigger('beforeSubmit');
				if($('#ec_classific_edit_form input[name="operateType"]').val() == "submit"){
					var deploymentId=$('#ec_classific_edit_form input[name="deploymentId"]');
					var buttonCode=$('#ec_classific_edit_form input[name="buttonCode"]');
					var namespace=$('#ec_classific_edit_form input[name="namespace"]');
					if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
						var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
						if(signatureInfo[0] != '') {
							var cancelItem = $('input[name="workFlowVarStatus"]');
							if(cancelItem.val() != "cancel") {
								signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_classific_edit_form');
								$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_classific_edit_form','ec.flow.submit',false)});
							}
							else {
								signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_classific_edit_form');
								$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_classific_edit_form','ec.edit.remove',false)});
							}
							
						}
						else {
							submitBapForm();
						}
					}
					else if( buttonCode.length > 0 && buttonCode.val() != undefined && buttonCode.val() != ''){
						var signatureInfo=signatureUtil.getSignatureInfo(false,buttonCode.val());
						if(signatureInfo[0] != '') {
							if(namespace.length > 0 && namespace.val() != undefined && namespace.val() != '') {
								parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_classific_edit_form",false,'');
								if(signatureInfo[0] == 'singleSign') {
									parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_classific_edit_form',buttonCode.val(),false)});
								}
								else {
									setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_classific_edit_form',buttonCode.val(),false)});},2000);
								}	
							}
							else {
								signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_classific_edit_form",false,'');
								if(signatureInfo[0] == 'singleSign') {
									$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_classific_edit_form',buttonCode.val(),false)});
								}
								else {
									setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_classific_edit_form',buttonCode.val(),false)});},2000);
								}
							}
						}
						else {
							submitBapForm();	
						}
					}
					else {
						submitBapForm();
					}
				}
				else {
					submitBapForm();
				}
				return false;
			});
		});

</script>
<#if targetModel??>
<@customerCondition viewCode="${view.code}" showArea="dataClassificArea" checkBoxId="dataClassificBox" ccNameSpace="dataclassifc.condition" modelCode="${targetModel.code}" modelName="${targetModel.modelName}" />
<#else>
<@customerCondition viewCode="${view.code}" showArea="dataClassificArea" checkBoxId="dataClassificBox" ccNameSpace="dataclassifc.condition" />
</#if>
