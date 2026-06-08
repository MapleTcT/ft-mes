<style>
	/*适用范围样式定义*/
	input:focus {
		outline: none;
	}

	.module-company-wrap {
		border: 1px solid #c6d4e1;
	}

	.module-company-selectBox {
		width: calc(100% - 20px);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		max-width: 400px;
	}

	.module-company-picker {
		position: absolute;
		width: 100%;
		bottom: 26px;
		left: 0;
		background: #fff;
		border: 1px solid #c6d4e1;
	}

	.module-company-picker .company-all {
		padding: 10px 10px 0 10px;
		white-space: nowrap;
	}

	.module-company-picker .company-all input[type='checkbox'] {
		margin-right: 5px;
	}

	.module-company-picker .ztree {
		padding: 10px;
		border-bottom: 1px solid #c6d4e1;
		max-height: 200px;
		overflow-y: auto;
	}

	.module-company-picker .custom-tree-box>.custom-tree-box {
		padding-left: 34px;
	}

	.module-company-picker .custom-tree-box .node-item {
		display: inline-block;
		margin-bottom: 5px;
		width: calc(100% - 20px);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.module-company-picker .custom-tree-box input[type='checkbox'] {
		margin-right: 5px;
	}

	.module-company-picker .custom-tree-box .node-arrow {
		width: 18px;
		height: 19px;
		display: inline-block;
		vertical-align: middle;
		border: 0;
		cursor: pointer;
		background-image: url(/bap/static/tree/assets/img/zTreeStandard25.png);
		background-position: -69px -72px;
		position: relative;
		left: -8px;
		top: -12px;
	}

	.module-company-picker .custom-tree-box .node-arrow.open {
		background-position: -87px -72px;
	}

	.module-company-picker .operate-btn {
		text-align: right;
		padding: 5px;
	}

	.module-company-picker .operate-btn .m-btn {
		display: inline-block;
		border: 1px solid #58c9cb;
		width: 50px;
		height: 26px;
		border-radius: 3px;
		border-top: 1px solid #c6d4e1;
		outline: none;
		cursor: pointer;
	}


	.module-company-picker .operate-btn .m-btn:hover {
		opacity: 0.7;
	}

	.module-company-picker .operate-btn .m-btn.confirm {
		background-color: #58c9cb;
		color: #fff;
	}

	.module-company-picker .operate-btn .m-btn.cancel {
		background-color: #fff;
		color: #58c9cb;
		margin-left: 10px;
	}

	.module-company-selectBox .company-item {
		margin-right: 3px;
	}

	.module-company-selectBox .company-item .btn-delete {
		display: inline-block;
		width: 16px;
		height: 18px;
		background: url(/bap/static/ec/delete.gif) no-repeat center;
		cursor: pointer;
		position: relative;
		top: 3px;
	}

	.module-company-selectBox .company-item .btn-delete:hover {
		background: url(/bap/static/ec/deleteon.gif) no-repeat center;
	}

</style>
<@errorbar id="ec_module_edit_formDialogErrorBar" />
<form id="ec_module_edit_form" name="ec_module_edit_form"
 action="/msService/ec/module/save?isMsService=Mis" method="post" callback="ec.module.addCallback" validate="true"  onreset="clearErrorMessages(this);clearErrorLabels(this);"
>
<#--  <#if module?? && (module.version)??>
	<input type="hidden" name="module.version"  id="ec_module_edit_form_module_version" value="${module.version}" />
<#else>
	<input type="hidden" name="module.version"  id="ec_module_edit_form_module_version" value="" />
</#if>
<#if module?? && (module.code)??>
	<input type="hidden" name="module.code"  id="ec_module_edit_form_module_code" value="${module.code}" />
<#else>
	<input type="hidden" name="module.code"  id="ec_module_edit_form_module_code" value="" />
</#if>
<#if module?? && (module.lastVersion)??>
	<input type="hidden" name="module.lastVersion"  id="ec_module_edit_form_module_lastVersion" value="${module.lastVersion}" />
<#else>
	<input type="hidden" name="module.lastVersion"  id="ec_module_edit_form_module_lastVersion" value="" />
</#if>  -->


<input type="hidden" name="module.version"  id="ec_module_edit_form_module_version" <#if module?? && module.version??>value="${module.version}"</#if> />
<input type="hidden" name="module.code"  id="ec_module_edit_form_module_code" <#if module?? && module.code??>value="${module.code}"</#if> />
<input type="hidden" name="module.lastVersion"  id="ec_module_edit_form_module_lastVersion" <#if module?? && module.lastVersion??>value="${module.lastVersion}"</#if>/>
    <table class="infoTable"  cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 20px">
        <tr>
            <td class="la" style="color:#B30303;width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.module.artifact')}</td>
            <td class="co" colspan="3">
                <#if module?? && (module.code)??>
                    <input type="text" name="module.artifact" readonly="true" maxlength="15" value="${module.artifact}" id="ec_module_edit_form_module_artifact" class="cui-edit-field" style="width:99%;"/>
                <#else>
                    <input type="text" name="module.artifact" maxlength="15" id="ec_module_edit_form_module_artifact" class="cui-edit-field" style="width:99%;"/>
                </#if>
                <div><span class="description">(${getHtmlText('ec.module.edit.description')})</span></div>
            </td>
        </tr>
        <tr>
            <td class="la" style="<#if !module??>color:#B30303;</#if>width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.module.acronym')}</td>
            <td class="co" colspan="3">
                <#if module?? && (module.acronym)??>
                    <input type="text" name="module.acronym" value="${module.acronym}" readonly="true" maxlength="5" value="" id="ec_module_edit_form_module_acronym" class="cui-edit-field" style="width:99%;"/>
                <#else>
                    <input type="text" name="module.acronym" maxlength="5" value="" id="ec_module_edit_form_module_acronym" class="cui-edit-field" style="width:99%;"/>
                </#if>
                <div><span class="description">(${getHtmlText('ec.module.acronym.description')})</span></div>
            </td>
        </tr>
        <tr>
            <td class="la" style="color:#B30303;width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.module.projectVersion')}</td>
            <td class="co" colspan="3">
                <input type="text" name="module.projectVersion" <#if module?? && (module.projectVersion)??>value="${module.projectVersion}"</#if> id="ec_module_edit_form_module_projectVersion" class="cui-edit-field" style="width:99%;"/>
                <div><span class="description">(${getHtmlText('ec.module.edit.descriptionVersion')})</span></div>
            </td>
        </tr>
        <tr>
            <td class="la" style="color:#B30303;width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.module.name')}</td>
            <td class="co" colspan="3">
                <@international name="module.name" key=(module.name)!'' moduleCode=(module.artifact)!'foundation' isNew=true maxLength=80></@international>
                <#--
                <#if !module?exists>
                    <@international name="module.name"  key="" cssClass="cui-edit-field"></@international>
                <#else>
                    <@international name="module.name" moduleCode="${module.artifact!}" key="${module.name!}" cssClass="cui-edit-field"></@international>
                </#if>
                -->
            </td>
        </tr>
        <tr>
            <td class="la" style="width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.module.category')}</td>
            <td class="co" colspan="3">
                <#if module?? && module.category??>
                <@international name="module.category" key=(module.category)! moduleCode=(module.artifact)!'appConfig' isNew=true maxLength=80></@international>
                <#else>
                <@international name="module.category" key="" moduleCode=(module.artifact)!'appConfig' isNew=true maxLength=80></@international>
                </#if>
            </td>
        </tr>

        <tr>
            <td class="la" style="width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.view.relatedModule')}
            <span id="moduleRelationHelpInfo" class="baphelp-icon helptip-mark"></span>
            </td>
            <td class="co" colspan="3" width="80%">
                <div class="fix-input">
                <div id="moduleSelectsMultiselectContainerDiv" class="multi-container" <#if (module.isProto)?? && module.isProto >style="height:26px;overflow:hidden;pointer-events:none;"<#else>style="height:26px;overflow:hidden;"</#if>>
                    <input type="hidden" name="moduleSelectsMultiIDs" id="moduleSelectsMultiIDs" value="${ids!}"/>
                    <input type="hidden" id="moduleSelectsDeleteIds" name="moduleSelectsDeleteIds" value="" originalvalue=""/>
                    <input type="hidden" id="moduleSelectsAddIds" name="moduleSelectsAddIds" value="" originalvalue=""/>
                    <span id="moduleSelectsMultiselectContainer">
                    <#if relations?? && relations?size gt 0>
                        <#list relations as relation>
                            <span style="padding-right:8px;">${getHtmlText('${relation.target.name}')}<img src="/bap/static/ec/delete.gif" style="cursor:pointer;vertical-align:middle;padding-bottom:3px;" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' onclick="moduleSelectsDeleteObj(this);" objid="${relation.target.code}"/></span>
                        </#list>
                    </#if>
                    </span>
                    <input type="button" style="position:absolute;" class="cui-search-click" onclick="moduleSelects_selectEvent('other','/msService/ec/module/ref?isMsService=${isMsService}&multiSelect=true<#if module?? && module.code??>&module.code=${(module.code)!}</#if>','${getText('ec.view.relatedModule')}','')"/>
                </div>
                </div>
            </td>
        </tr>

        <#if '${isMsService!""}' == 'Mis'>
        <tr>
            <td class="la" style="width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.view.referenceModule')}
            <span id="moduleReferenceHelpInfo" class="baphelp-icon helptip-mark"></span>
            </td>
            <td class="co" colspan="3" width="80%">
                <#if module?? && module.code??>
                    <#assign moduleCode="&module.code="+module.code>
                </#if>
                <@mneclient iframe=mneIframe!false mneenable=false reftitle="${getText('ec.view.referenceModule')}" conditionfunc=""  name="module.moduleReference" id="module_moduleReference" type="other" url="/msService/ec/module/moduleReference?isMsService=Mis${moduleCode!}" displayFieldName="name"   ids="${(module.moduleReferencemultiselectIDs)!}" names="${(module.moduleReferencemultiselectNames)!}"  onkeyupfuncname="" clicked=true multiple=true cssStyle="padding-left: 0px; width: 100%;" mnewidth=260 isCrossCompany=false isEdit=true  />
            </td>
        </tr>
        </#if>
		<#if !supos>
		<tr>
			<td class="la" style="color:#B30303;width: 20%;padding-right: 10px;" align="right">
				${getHtmlText('foundation.infoSet.companyCode')}
			</td>
			<td class="co" colspan="3" width="80%">
				<div id="moduleCompanyIdsDiv" class="multi-container module-company-wrap" style="height:26px">
					<input type="hidden" name="module.companyIds" id="moduleCompanyIds" <#if module??>value="${module.companyIds!}"<#else>value="-1"</#if>/>
					<div class="module-company-selectBox" id="moduleCompanyBox"></div>
					<input type="button" style="position:absolute;" class="cui-search-click"
						onclick="moduleCompanySelect(this,event)" />
				</div>
			</td>
		</tr>
		</#if>
        <tr <#if '${isMsService!""}' != 'Mis'> style='display:none;' </#if>>
            <td class="la" style="width: 20%;padding-right: 10px;white-space: normal;" align="right">${getHtmlText('ec.view.protoType')}</td>
            </td>
            <td class="co" colspan="3" width="80%" align="left">
                <input type="hidden" name="module.isProto" value="<#if (module.isProto)?? && module.isProto>true<#else>false</#if>">
                <input type="checkbox"  name="module_isProto" <#if module?? && (module.acronym)??>disabled="true" <#else> onclick="isProtoCheck(this)"</#if> <#if module?? && module.isProto?? && module.isProto>checked="checked"</#if> cssClass="cui-edit-field" />
            </td>
        </tr>
        <tr <#if '${isMsService!""}' == 'Mis'> style='display:none;' </#if>>
            <td class="la" style="width: 20%;padding-right: 10px;white-space: normal;" align="right">${getHtmlText('ec.view.generateType')}</td>
            </td>
            <td class="co" colspan="3" width="80%" align="left">
                <input type="hidden" name="module.isNewGenerate" value="<#if (module.isNewGenerate)?? && module.isNewGenerate>true<#else>false</#if>">
                <input type="checkbox" name="module_isNewGenerate" onclick="isNewGenerateCheck(this)" <#if module?? && module.isNewGenerate?? && module.isNewGenerate || '${isMsService!""}' == 'Mis'>checked="checked"</#if> cssClass="cui-edit-field" />
            </td>
        </tr>
        <tr>
            <td class="la" style="width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.module.description')}</td>
            <td class="co" colspan="3" width="80%"><textarea name="module.description" cols="" rows=""  id="ec_module_edit_form_module_description" class="cui-edit-textarea" style="width:98%;height:60px"><#if module?? && module.description??>${module.description} </#if></textarea></td>
        </tr>
    </table>
</form>


<script type="text/javascript">
(function(){  //onload事件，自动调用
	var form = CUI('#ec_module_edit_form');
})();

    function validateForm_ec_module_edit_form() {
        form = document.getElementById("ec_module_edit_form");
        // 内容前后去空格
    	$('select,input[type="text"],textarea', '#ec_module_edit_form').each(function(){
    		this.value = $.trim(this.value);
    	});
        clearErrorMessages(form);
        clearErrorLabels(form);

        var errors = false;
        var field = null;
        var continueValidation = true;
        var errorFields = new Array();
        var errorMessages = new Array();
        var validateFields = {};
		var oErrorWidget = null;
			oErrorWidget = ec_module_edit_formDialogErrorBarWidget;
		if(errors){
			addError(form,oErrorWidget,errorFields,errorMessages);
		}

        //return !errors;
        return false;
    }
</script>
<script type="text/javascript">

(function(){
	//注册命名空间
	CUI.ns("ec.module");
	 /* 编码规则验证
	 * @method ec_module_code_validate
	 * @public
	 */
	ec.module.artifactValidate=function(){
		if(!ec.module.acronymValidate()){
			return false;
		}
	 	var validate=/^[a-zA-Z]{1}[a-zA-Z0-9]*$/;
	 	var obj = $("#ec_module_edit_form input[name='module.artifact']").val();
	 	$("#ec_module_edit_form input[name='module.artifact']").val(CUI.trim(obj));
		if (validate.test($("#ec_module_edit_form input[name='module.artifact']").val())){

			return true;
		}else{
		    ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.formatmessage')}");
			return false
		}
	}
	 /* 编码规则验证
	 * @method ec_module_acronym_validate
	 * @public
	 */
	ec.module.acronymValidate=function(){
	 	var validate=/^[a-zA-Z]{1}[a-zA-Z0-9]*$/;
	 	var obj = $("#ec_module_edit_form input[name='module.acronym']").val();
	 	$("#ec_module_edit_form input[name='module.acronym']").val(CUI.trim(obj));
	 	obj = $("#ec_module_edit_form input[name='module.acronym']").val();
		if (<#if module??>"${module.acronym!}" == obj ||</#if> validate.test(obj)){

		}else{
		    ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.acronym.formatmessage')}");
			return false
		}
		if ($("#ec_module_edit_form input[name='module.projectVersion']").val() == "") {
            ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.projectVersion.required')}");
            return false
        } else {
            var versionValidate=/^((\d+\.*){1,3}|(\d+\.){3}\w{1,14})$/;
            if (!versionValidate.test($("#ec_module_edit_form input[name='module.projectVersion']").val())){
                ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.projectVersion.invalid')}");
                return false
            }
        }
        if ($("#ec_module_edit_form input[name='module.name']").val() == "") {
            ec_module_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.module.name.required')}");
            return false
        }
        return true;
	}
	$('#moduleRelationHelpInfo').helptip({  
	    content: "${getText('ec.module.moduleRelationHelpInfo')}"  
	});
	$('#moduleReferenceHelpInfo').helptip({  
	    content: "${getText('ec.module.moduleReferenceHelpInfo')}"  
	});
	CUI(function(){
		function submitBapForm(){//电子签名成功之后出现进度条并提交表单
			var ecFormFlag = false;
			var retrialFormFlag = false;
			if(ecFormFlag && ( $('#ec_module_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
				// dialog不出进度条
				ecFormFlag = false;
			}
			ecFormFlag = (ecFormFlag || retrialFormFlag);
			
			//前台验证通过之后出进度条
			CUI.Dialog.toggleAllButton('ec_module_edit_form',true,ecFormFlag, true);
			// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
			setTimeout(function(){
			
				// 延迟保存数据, 解决onchange事件无法触发问题
				var formId = 'ec_module_edit_form';
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
								$("#ec_module_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
									if($(this).parents('td[nullable=false]').length > 0) {
										showErrorField($(this));
									}
								});
							} else {
								var field = CUI("#ec_module_edit_form *[name='"+index+"']");
								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
									showErrorField(field.next());
								} else {
									showErrorField(field);
								}
							}
							CUI("#ec_module_edit_form *[name='"+index+"']").first().focus();
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
						oErrorWidget = ec_module_edit_formDialogErrorBarWidget;
						if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
							oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
						}	else {
							oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
						}					
						if(CUI.Dialog){
							CUI.Dialog.toggleAllButton('ec_module_edit_form', true);
						}
					},
					success : function(msg){
						window.onbeforeunload = null;
						if(window.containerLoadPanelWidget) {
							setTimeout(function(){closeLoadPanel();}, 500);
						}
						ec.module.addCallback(msg,postData);
					}
				});
			}
		}, 600);
			return false;
		}
		CUI('#ec_module_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
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
			//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_module_edit_form',true,ecFormFlag);
			//});
				//if(!validateForm_ec_module_edit_form())	return false;
			$('#ec_module_edit_form').trigger('beforeSubmit');
			if($('#ec_module_edit_form input[name="operateType"]').val() == "submit"){
				var deploymentId=$('#ec_module_edit_form input[name="deploymentId"]');
				var buttonCode=$('#ec_module_edit_form input[name="buttonCode"]');
				var namespace=$('#ec_module_edit_form input[name="namespace"]');
				if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
					var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
					if(signatureInfo[0] != '') {
						var cancelItem = $('input[name="workFlowVarStatus"]');
						if(cancelItem.val() != "cancel") {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_module_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_module_edit_form','ec.flow.submit',false)});
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_module_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_module_edit_form','ec.edit.remove',false)});
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
							parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_module_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_module_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_module_edit_form',buttonCode.val(),false)});},2000);
							}	
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_module_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_module_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_module_edit_form',buttonCode.val(),false)});},2000);
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
})();

var _dialogRelation;
function moduleSelects_selectEvent(type,url,title,refparam){
	_dialogRelation = foundation.common.select({
		pageType : type,
		closePage : true,
		callBackFuncName : '_callback_moduleSelects',
		url : url,
		title : title,
		params : refparam,
		closePage : false
	});
}

function isNewGenerateCheck(obj){
	var moduleCode = $("input[name='module.code']").val();
	if(moduleCode != ""){
		var ck = $(obj).prop('checked');
		var oldCk = $("input[name='module.isNewGenerate']").val();
		if(ck.toString() != oldCk){
			CUI.Dialog.confirm("修改生成方式将会改变代码目录结构，是否继续？",function(){
				$("input[name='module.isNewGenerate']").val(ck);
			},function(){
				$(obj).prop('checked',!ck);
			});
		}
	}
}

function isProtoCheck(obj){
	var ck = $(obj).prop('checked');
	var oldCk = $("input[name='module.isProto']").val();
	if(ck.toString() != oldCk){
		if(ck.toString()=="true"){
			$('#moduleSelectsMultiselectContainerDiv').css('pointer-events','none');
			$('#module_moduleReferenceMultiIDsContainerDiv').css('pointer-events','none');
			$('#mainCode').css("display","inline");
			$('#mainModule').css("display","inline");
		}else{
			$('#moduleSelectsMultiselectContainerDiv').css('pointer-events','auto');
			$('#module_moduleReferenceMultiIDsContainerDiv').css('pointer-events','auto');
			$('#mainCode').css("display","none");
			$('#mainModule').css("display","none");
		}
		$("input[name='module.isProto']").val(ck);
	}
}

function isMainCheck(obj){
	var ck = $(obj).prop('checked');
	var oldCk = $("input[name='module.mainModule']").val();
	if(ck.toString() != oldCk){
		$("input[name='module.mainModule']").val(ck);
	}
}

function _callback_moduleSelects(objs){
	if (objs == undefined || objs.length <= 0) {
			return false;
	}
	var alreadyIDs = '',IDs = '',AddIds = '',str = '';
	var flag = 0;
	if(CUI("#moduleSelectsMultiIDs").val() != ""){
		alreadyIDs = CUI("#moduleSelectsMultiIDs").val().split(",");
	}
	for(var i=0; i<alreadyIDs.length; i++){
		if(alreadyIDs[i] == objs[0].code){
			flag = 1;
			break;
		}
	}
	var allmnemultiselectids = CUI("#module_moduleReferenceMultiIDs").val() || '';
	for(var o=0 ; o < objs.length; o++) {
		if(allmnemultiselectids.indexOf(',' + objs[o].code + ',') != -1 || allmnemultiselectids.indexOf(objs[o].code + ',') == 0){
			ec_module_refBarWidget.show(objs[0].nameInternational + " " + "${getHtmlText('ec.module.alreadyReference')}");
			return;
		}
	}
	if(flag == 0) {
		var index = -1;
		var delStr = "";
		var delArr;
		if(CUI("#moduleSelectsDeleteIds").val() != "") {
			delArr = CUI("#moduleSelectsDeleteIds").val().split(',');
			for(var i = 0 ; i < delStr.length; i++) {
				if(objs[0].id == delStr[i]) {
					index = i;
					delStr.splice(i,1);
					break;
				}
			}
		}
		if(index != -1) {
			for(var j = 0 ; j < delStr.length; j++) {
				delStr += "," + delStr[j];
			}
			CUI("#moduleSelectsDeleteIds").val(delStr);
		}else {
			AddIds = (CUI("#moduleSelectsAddIds").val() == "") ? objs[0].code : CUI("#moduleSelectsAddIds").val() + "," +  objs[0].code;
			CUI("#moduleSelectsAddIds").val(AddIds);
		}
		IDs = (CUI("#moduleSelectsMultiIDs").val() == "") ? objs[0].code : CUI("#moduleSelectsMultiIDs").val() + "," +  objs[0].code;
		CUI("#moduleSelectsMultiIDs").val(IDs);
		str = CUI("#moduleSelectsMultiselectContainer").html() + "<span spanid="+objs[0].code+" style='padding-right:5px;float:left;display:inline-block;white-space:nowrap;'>"+objs[0].nameInternational+"<img src='/bap/static/ec/delete.gif' style='cursor:pointer;vertical-align:middle;padding-bottom:3px;' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' onclick='moduleSelectsDeleteObj(this);' objid='"+objs[0].code+"'/></span>";
		CUI("#moduleSelectsMultiselectContainer").html(str);
		$('body').trigger('resize');
		$('body').trigger('dialog.resize');
		ec_module_refBarWidget.show("${getHtmlText("ec.module.selectsuccess")}","s");
		setTimeout(function(){
			_dialogRelation.close();
		},1000);
	}else{
		ec_module_refBarWidget.show("${getHtmlText("ec.module.duplicatemodule")}");
	}
}

function moduleSelectsDeleteObj(imgObj) {
	var oldIDArrays = CUI("#moduleSelectsMultiIDs").val().split(",");		
	var newIDs = "",DeleteIds = "";
	var index = -1;
	var addStr = "";
	var addArr;
	if(CUI("#moduleSelectsAddIds").val() != "") {
		addArr = CUI("#moduleSelectsAddIds").val().split(',');
		for(var i = 0 ; i < addArr.length; i++) {
			if(imgObj.getAttribute("objid") == addArr[i]) {
				index = i;
				addArr.splice(i,1);
				break;
			}
		}
	}
	if(index != -1) {
		for(var j = 0 ; j < addArr.length; j++) {
			addStr += "," + addArr[j];
		}
		CUI("#moduleSelectsAddIds").val(addStr);
	} else {
		DeleteIds = (CUI("#moduleSelectsDeleteIds").val() == "") ? imgObj.getAttribute("objid") : CUI("#moduleSelectsDeleteIds").val() + "," +  imgObj.getAttribute("objid");
		CUI("#moduleSelectsDeleteIds").val(DeleteIds);
	}
	for(var i=0; i<oldIDArrays.length; i++){
		if(oldIDArrays[i] == imgObj.getAttribute("objid")) continue;
		newIDs = (newIDs == "") ? oldIDArrays[i] : newIDs + "," + oldIDArrays[i];
	}
	CUI("#moduleSelectsMultiIDs").val(newIDs);
	CUI(imgObj).parent().remove();
	if(CUI('span[spanid="'+imgObj.getAttribute("objid")+'"]',$('#moduleSelectsMultiselectContainer'))) {
		CUI('span[spanid="'+imgObj.getAttribute("objid")+'"]',$('#moduleSelectsMultiselectContainer')).remove();
	}
}

foundation.common._ec_module_ref__callbackFunction = function(){
	if(datatable_moduleRef._DT.selectedRows.length == 0){
			ec_module_refBarWidget.show("${getHtmlText("请选择一条记录")}");
	}
	_callback_moduleSelects(datatable_moduleRef._DT.selectedRows);
}

foundation.common._ec_module_moduleReference__callbackFunction = function(){
	if(datatable_moduleReference._DT.selectedRows.length == 0){
			ec_moduleReference_refBarWidget.show("${getHtmlText("请选择一条记录")}");
	}
	_callback_module_moduleReference(datatable_moduleReference._DT.selectedRows);
}
function _callback_module_moduleReference(objs) {
	if (objs == null || objs == undefined || objs.length <= 0) {
		return false;
	}
	for(var o=0 ; o < objs.length; o++) {
		var id = objs[o].code;
		// 当前所有id
		var allmnemultiselectids = CUI("#module_moduleReferenceMultiIDs").val() || '';
		// 判断是否已经存在
		if(allmnemultiselectids.indexOf(',' + id + ',') != -1 || allmnemultiselectids.indexOf(id + ',') == 0){
			ec_moduleReference_refBarWidget.show("${getHtmlText("ec.module.duplicatemodule")}");
			return;
		}
		var moduleRelationIds = CUI("#moduleSelectsMultiIDs").val() || '';
		// 判断是否已经存在
		if(moduleRelationIds.indexOf(',' + id) != -1 || moduleRelationIds.indexOf(',' + id) == 0){
			ec_moduleReference_refBarWidget.show(objs[o].nameInternational + " " + "${getHtmlText('ec.module.alreadyRelation')}");
			return;
		}
		var addInupt = CUI("#module_moduleReferenceAddIds");
		var addIDs = addInupt.val();
		var delInupt = CUI("#module_moduleReferenceDeleteIds");	
		var delIDs = delInupt.val();
		// 原有数据被删除
		if(delIDs.indexOf(',' + id + ',') != -1 || delIDs.indexOf(id + ',') == 0){
			var re = new RegExp('(.*,|^^)(' + id + ',)(.*)'); 
			delIDs = delIDs.replace(re,'$1$3');
			delInupt.val(delIDs);
		}else{
			// 增加
			addIDs += (id + ',');
			addInupt.val(addIDs);
		}
		// 更新当前所有id
		allmnemultiselectids += (id + ',');
		CUI("#module_moduleReferenceMultiIDs").val(allmnemultiselectids);
		var newSpan = $("<span class='mne-select-span'>"+objs[o].nameInternational+"<img src='/bap/static/ec/delete.gif' class='multi-mne-img' onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)' mneobjid='"+id+"'/></span>");
		CUI("#module_moduleReferenceMultiIDsContainer").append(newSpan);
		$('img[mneobjid="'+id+'"]',$('#module_moduleReferenceMultiIDsContainer')).click(function(e){
			stopBubble(e);
        	deletemodule_moduleReferenceMulti(this);
		});
		module_moduleReferenceCalWidth();
		if( $('#module_moduleReferenceMultiIDsContainerDiv').height() > 28 && ( $.browser.msie7 || $.browser.msie6 ) ){
			$("#module_moduleReferenceMultiIDsContainer").css( 'top', 0 );
		}
		
	}
	setTimeout(function(){
		module_moduleReference_dialog.close();
	},1000);
}
$(document).ready(function () {
		var checkedNodes = ec_module_Tree.getSelectedNodes();
		if (checkedNodes && checkedNodes[0] && !checkedNodes[0].id && !checkedNodes[0].code) {//category
			var artifactObj = $("#ec_module_edit_form_module_artifact")
			if (artifactObj && "" == artifactObj.val()) {//add
				var categoryVal = checkedNodes[0].category;
				if (categoryVal) {
					$("#ec_module_edit_form_module_category").val(categoryVal);
				}
			}
		}
		// 非supos融合环境
		if ($('#moduleCompanyIds').length > 0) {
			// 适用范围初始值绑定
			getCompanyData(function () {
				var companyIds = $('#moduleCompanyIds').val();
				renderCompanyItem(companyIds);
				ec.module.editDlg.resetSize(null, 560);
			});
		}
	});

	function renderCompanyItem(ids) {
		var selected = ids ? ids.split(',') : [];
		var citemStr = '';
		for (var i = 0; i < selected.length; i++) {
			var id = selected[i];
			citemStr += '<span class="company-item">' + getCompanyNameById(id) + '<i class="btn-delete" data-val=' + id + ' onclick="deletCompanyItem(this)"></i></span>';
		}
		$('#moduleCompanyBox').html(citemStr);
	}

	function deletCompanyItem(item) {
		var id = $(item).attr('data-val');
		var before = $('#moduleCompanyIds').val().split(',');
		var index = before.indexOf(id);
		before.splice(index, 1);
		var curIds = before.join(',');
		$('#moduleCompanyIds').val(curIds);
		renderCompanyItem(curIds);
	}

	// 获取显示文本
	function getCompanyNameById(id) {
		if (id == '-1') return '${getText("foundation.protlet.allcompany")}'; // 所有公司
		var cData = getCompanyData();
		var name = '';
		for (var i = 0; i < cData.length; i++) {
			var item = cData[i];
			if (String(item.id) === String(id)) {
				name = item.shortName;
				break;
			}
		}
		return name;
	}

	// 适用范围公司选择
	function moduleCompanySelect(obj, event) {
		event.stopPropagation();
		var box = $("#moduleCompanyIdsDiv");
		const picker = box.find('.module-company-picker');
		var intlVal = { allCompany: '${getText("foundation.protlet.allcompany")}', okTxt: '${getText("Button.text.ok")}', cancelTxt: '${getText("Button.text.cancel")}' };
		if (!picker || picker.length == 0) {
			var pickerDom = '<div class="module-company-picker">';
			pickerDom += '<div class="company-all"><input type="checkbox" name="allComponey" onchange="selectAllCompany(this)"><span>' + intlVal.allCompany + '</span></div>';
			pickerDom += '<ul class="ztree" id="companyZTree"></ul>';
			pickerDom += '<div class="operate-btn" ><input type="button" value="' + intlVal.okTxt + '" class="m-btn confirm" onclick="selectCompany()"/><input type="button" value="' + intlVal.cancelTxt + '" class="m-btn cancel" onclick="cancelTreeSelect()"/></div>';
			pickerDom += '</div>';
			box.append(pickerDom);
			initCompnayTree();
		} else {
			picker.show();
			var ids = $('#moduleCompanyIds').val();
			var box = $("#moduleCompanyIdsDiv");
			box.find('input[name="allComponey"]').attr('checked', ids == '-1');
			$.fn.customTree.setCheckedNodes(ids);
		}
	}
	// 点击确定
	function selectCompany() {
		var box = $("#moduleCompanyIdsDiv");
		var isAll = box.find('input[name="allComponey"]').attr('checked');
		var ids = $.fn.customTree.getCheckedNodes();
		var cur = isAll ? '-1' : ids;
		$('#moduleCompanyIds').val(cur);
		renderCompanyItem(cur);
		cancelTreeSelect();
	}
	// 点击取消
	function cancelTreeSelect() {
		var box = $("#moduleCompanyIdsDiv");
		box.find('.module-company-picker').hide();
	}

	// 所有公司选择
	function selectAllCompany(e) {
		var checked = e.checked;
		if (checked) {
			$.fn.customTree.cancelCheckedNodes();
		}
	}

	// 树选择改变
	function handleTreeCheck() {
		var box = $("#moduleCompanyIdsDiv");
		box.find('input[name="allComponey"]').attr('checked', false);
	}

	// 初始化公司树
	function initCompnayTree() {
		var companyData = getCompanyData();
		var zNodes = generateTree(companyData);
		var ids = $('#moduleCompanyIds').val();
		var box = $("#moduleCompanyIdsDiv");
		box.find('input[name="allComponey"]').attr('checked', ids == '-1');
		$.fn.customTree.init('companyZTree', { data: zNodes, value: ids, onCheck: handleTreeCheck });
		$('body').on('click', cancelTreeSelect);
		box.find('.module-company-picker').on('click', function (e) {
			e.stopPropagation();
		})
	}
	// 公司数据
	var companyData;
	function getCompanyData(callback) {
    	var result = companyData;
    	if (result) {
    		if (callback && typeof callback == 'function') callback(result);
    		return result;
    	}
    	$.ajax({
    		url: '/inter-api/organization/v1/companies/sub/ref', type: 'get', async: true, beforeSend: function (request) {
    			var ticket = localStorage.getItem('ticket');
    			var language = localStorage.getItem('language');
    			request.setRequestHeader("Authorization", "Bearer " + ticket);
    			request.setRequestHeader("Accept-Language", language);
    		}, success: function (data) {
    			result = data.list;
    			companyData = result;
    			if (callback && typeof callback == 'function') callback(result);
    		}
    	});
    }
	// 公司数据分组
	function generateTree(data) {
		var tree = [];
		$.each(data, function (i, x) {
			x.children = [];
			x.name = x.shortName;
			x.open = true;
			if (!x.parentId || x.parentId == null) {
				tree.push(x);
			} else {
				var obj;
				for (var j = 0; j < data.length; j++) {
					var item = data[j];
					if (item.id == x.parentId) {
						obj = item;
						break;
					}
				}
				if (!obj.children)
					obj.children = [];
				obj.children.push(x);
			}
		});
		return tree;
	}



	// --------------自定义树形组件----------
	function customTree() { };
	// 初始化
	customTree.prototype.init = function (id, setting) {
		var box = $("#" + id);
		var config = Object.assign({}, setting);
		this.box = box;
		this.config = config;
		var nodeStr = this.renderNode(config.data);
		box.append(nodeStr);
		box.on('click', '.node-arrow', function (e) {
			var arrow = $(e.target);
			arrow.toggleClass('open');
			var isOpen = arrow.hasClass('open');
			arrow.siblings('.custom-tree-box').css('display', isOpen ? 'block' : 'none');
		});
		box.find('input[name="ck-node"]').on('change', function (e) {
			if (config.onCheck) config.onCheck(e);
		});
		this.setCheckedNodes(config.value);
	}
    // 渲染节点
	customTree.prototype.renderNode = function (data) {
		var nodeStr = '';
		for (var i = 0; i < data.length; i++) {
			var item = data[i];
			var itemStr = '<span class="node-item"><input type="checkbox" name="ck-node"  value="' + item.id + '"/>' + item.name + '</span>';
			if (item.children && item.children.length > 0) {
				nodeStr += '<div class="custom-tree-box"><i class="node-arrow open"></i>' + itemStr + this.renderNode(item.children) + '</div>';
			} else {
				nodeStr += '<div class="custom-tree-box">' + itemStr + '</div>';
			}
		}
		return nodeStr;
	}

	// 获取选中节点
	customTree.prototype.getCheckedNodes = function () {
		var cks = this.box.find('input[name="ck-node"]');
		var ids = [];
		for (var i = 0; i < cks.length; i++) {
			var item = cks[i];
			var isChecked = $(item).attr('checked');
			if (isChecked) {
				var value = $(item).val();
				ids.push(value);
			}
		}
		return ids.join(',');
	}

	// 设置选中节点
	customTree.prototype.setCheckedNodes = function (ids) {
		var checked = ids && ids.split(',') || [];
		var cks = this.box.find('input[name="ck-node"]');
		for (var i = 0; i < cks.length; i++) {
			var item = cks[i];
			var value = $(item).val();
			var isChecked = checked.indexOf(value) > -1;
			$(item).attr('checked', isChecked);
		}
	}

	// 取消选中节点
	customTree.prototype.cancelCheckedNodes = function () {
		this.setCheckedNodes('');
	}

	// 是否全选
	customTree.prototype.isAllChecked = function () {
		var cks = this.box.find('input[name="ck-node"]');
		var flag = true;
		for (var i = 0; i < cks.length; i++) {
			var item = cks[i];
			var value = $(item).val();
			var isChecked = checked.indexOf(value) > -1;
			if (!isChecked) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	$.fn.customTree = new customTree();
</script>