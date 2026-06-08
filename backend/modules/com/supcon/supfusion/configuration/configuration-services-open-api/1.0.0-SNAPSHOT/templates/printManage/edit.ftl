<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta http-equiv="X-UA-Compatible" content="IE=8"/>
<style type="text/css">
.t{background:blue;cursor:pointer;}
.t tr{background:#FFF;}
.selected{background:#f2f200;}
.layoutselect{display:none;}
.cui-search-click {right:2px!important;top:3px;}
</style>
<#macro menuTree children parent defaultvalue>
	<#assign def=defaultvalue>
	<#if children?? && children?size gt 0>
		<#list children as m>
			<#assign n=(m.layRec?split("-"))?size - 1>
			<option value="${(m.id)!}" <#if m.id==def>selected="true"</#if>><#if n gt 0><#list 1..n as n>&nbsp;&nbsp;&nbsp;&nbsp;</#list></#if>${getText("${(m.name)!}")}</option>
			<@menuTree children=m.children parent=m defaultvalue=def/>
		</#list>
	</#if>
</#macro>
<@errorbar id="ec_print_template_formDialogErrorBar" />
<form  id="ec_print_template_form" action="save" namespace="ec/print/template" validate="false" callback="ec.print.template.callBack">
	<input type="hidden"  name="printTemplate.isPublish"  value="" />
	<input type="hidden"  name="printTemplate.ecEnv" value="" />
	<input type="hidden"  name="printTemplate.version" value="" />
	<input type="hidden"  name="printTemplate.entity.code" value="" />
	<input type="hidden"  name="printTemplate.code" value="" />
	<input type="hidden"  name="entity.workflowEnabled" value="" />
	<input type="hidden"  name="printTemplate.processKey" value="" />
	<input type="hidden"  name="printTemplate.processVersion" value="" />
	<input type="hidden"  name="printTemplate.templateEnabled" value="" />
	<input type="hidden"  name="printTemplate.extraParamScript" value="" />
	<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%">
		<tr>
			<td class="la"></td>
			<td class="co" colspan="3"></td>
		</tr>
		<tr>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.print.template.modelCode')}</td>
			<td class="co" style="width:30%">
			<#if printTemplate?? && (printTemplate.templateCode)?? && (printTemplate.templateCode)!=''>
				<input type="text" value="${entity.prefix}" name="printTemplate.templateCode" readonly="true" cssClass="cui-edit-field" style="PADDING-LEFT: 0px; WIDTH: 99%"/>
			<#else>
				<input type="text" value="" name="printTemplate.templateCode" cssClass="cui-edit-field" style="PADDING-LEFT: 0px; WIDTH: 99%"/>
			</#if>
			</td>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.print.template.templateName')} </td>
			<td class="co" style="width:30%">
				<#if printTemplate??>
					<@international name="printTemplate.templateName" isNew=true isOldEdit=true moduleCode=(entity.module.artifact)! key=(printTemplate.templateName)! cssClass="cui-edit-field"></@international>
				<#else>
					<@international name="printTemplate.templateName" isNew=true isOldEdit=true moduleCode=(entity.module.artifact)! key="" cssClass="cui-edit-field"></@international>
				</#if>
			</td>
		</tr>
		<tr id="printTemplate_batch_control_print">
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getText('ec.print.template.relatedView')}</td>
			<td class="edit-table-content" colspan="3">
				<DIV class=fix-input>
					<DIV id="printTemplate_viewCode_div" class="fix-search-click"> 
						<INPUT style="PADDING-LEFT: 0px; WIDTH: 100%; MARGIN-RIGHT: -5px" id="printTemplateViewSelected_formsysShow" value="${viewNamesStr!''}"
							class="cui-noborder-input" readOnly type="text" name="" property_type="SYSTEMCODE" istreesystem="1"> 
						<INPUT style="DISPLAY:NONE" id="printTemplateViewListSelect_clearbtn" class="cui-edit-clear" type="button"> 
						<INPUT id="systemValue_search_click_print_template_view_select_eidt_form" style="top:3px;right:2px !important;"
							class="cui-search-click systemValue_search_click" value="" type="button"></INPUT> 
					</DIV>
					<INPUT id="printTemplateViewSelected_formsysId"  name="printTemplate.viewCode" value="${(printTemplate.viewCode)!''}"
						title="printTemplateViewListSelect" value="" type="hidden" validateType="SystemCode">
				</DIV>
				<SCRIPT type=text/javascript>
					var tempViewName = "";
					var showOverLayerDIv_project_print_template_viewsListSelect_form;
						$( '#printTemplateViewListSelect_clearbtn' ).bind( 'click', function(){
							$('#printTemplateViewSelected_formsysShow').val( '' );
							$('#printTemplateViewSelected_formsysId').val( '' );
							$('#printTemplateModelSelected').val('');
							$('#printTemplateModelSelected_formsysId').val( '' );
							$(this).hide();
						});
						
						$('#printTemplate_viewCode_div').hover(function(){
							if($('#printTemplateViewSelected_formsysShow').val()){
								$('#printTemplateViewListSelect_clearbtn').show();
							}
						},function(){
							$('#printTemplateViewListSelect_clearbtn').hide();
						})
						
					$('#systemValue_search_click_print_template_view_select_eidt_form')
						.unbind('click.showOverlay').bind('click.showOverlay', function(e){
						ev = e || window.event;
						if (ev) { // 停止事件冒泡
							ev.stopPropagation();
						} else {
							window.event.cancelBubble = true;
						}
						showOverLayer_print_template_views_select_eidt_form(
							this,"/msService/ec/printManage/getModelVeiws?entity.code=${entity.code}&isProj=true")
					})
				
	
					 $('body').click(
							function(){
								if(showOverLayerDIv_project_print_template_viewsListSelect_form 
									&& showOverLayerDIv_project_print_template_viewsListSelect_form.isShow) {
									showOverLayerDIv_project_print_template_viewsListSelect_form.hide();
								}
								
							}
						);
						
				function showOverLayer_print_template_views_select_eidt_form(obj,url){
					if(showOverLayerDIv_project_print_template_modelsListSelect_form){
						showOverLayerDIv_project_print_template_modelsListSelect_form.hide();
					}
				
					if( showOverLayerDIv_project_print_template_viewsListSelect_form ){
						//showOverLayerDIv_project_print_template_viewsListSelect_form.setPosition();
						showOverLayerDIv_project_print_template_viewsListSelect_form.show();
					}
					showOverLayerDIv_project_print_template_viewsListSelect_form = new CUI.Overlay({
						align:obj,
				     	el:'printTemplate_viewListSelect_form',
				     	title:'${getText("ec.printTemplate.viewList")}',
				     	width:240,
				     	height:282,
				     	zIndex:9999,
				     	shadow:false,
						buttons:[
								{	name:'${getText("js.ec.script.manager.ok")}', // 确定
									id:"getSelectedViewsInfoButtonId",
									handler:function(){getSelectedViewsInfoFun()}
								},
								{	name:'${getText("ec.common.cancel")}', // 取消
									id:"systemValue_search_click_print_template_view_select_eidt_form_overlay_btn_cancel",
									handler:function(){closeSelectedViewsInfoButtonId()}
								}]
				     	
					});
					
					
					var sysId=CUI("#printTemplateViewSelected_formsysId").val();
					var valueId="#"+sysId;
					var sysCode=CUI(valueId).val();
					showOverLayerDIv_project_print_template_viewsListSelect_form.render();
					//showOverLayerDIv_project_print_template_viewsListSelect_form.show();
					
					$("#overlay-idprintTemplate_viewListSelect_form").click(
						function(e){
							
							ev = e || window.event;
							if (ev) { // 停止事件冒泡
								ev.stopPropagation();
			        		} else {
			        			window.event.cancelBubble = true;
			        		}
						}
					)
					url+="&fileName=knowledgeBorrowborrowType";
					url+="&formId=project_print_template_viewsListSelect_form";
					url+="&systemCodeCode="+sysCode;
					url+="&entityCode=borrowType";
					url+="&time="+new Date();
//					$("#overlay-idprintTemplate_viewListSelect_form,#overlay-shim-ieprintTemplate_viewListSelect_form").css("left", $("#printTemplateViewSelected_formsysShow").offset().left);
//					$("#overlay-idprintTemplate_viewListSelect_form,#overlay-shim-ieprintTemplate_viewListSelect_form").css("top", $("#printTemplateViewSelected_formsysShow").offset().top + 20);
					showOverLayerDIv_project_print_template_viewsListSelect_form.show();
					$("#printTemplate_viewListSelect_form").html(
						'<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("ec.printTemplate.loading")}</label></td></tr></table>'); // 正在加载...
					/*
					CUI('#printTemplate_viewListSelect_form').load(url,function(){
						var existsValue = $('input:hidden', $(obj).parent()).val();
						var arr = existsValue.split(',');
						for(var x = 0; x < arr.length; x++) {
							if(arr[x] && arr[x].length > 0) {
								$('#printTemplate_viewListSelect_form input[name="' + arr[x] + '"]').prop('checked', true);
							}
						}
					});
					*/
					CUI.ajax({
						url: url,
						type: 'post',
						async: false,
						success: function(html)
						{
							$('#printTemplate_viewListSelect_form').html(html);
							var viewList = $("#printTemplateViewSelected_formsysId").val();
							$.each($(".choosedView"),function(index,item){
								if(viewList.indexOf($(item).val()) != -1){
									$(item).attr("checked","checked");
									
								}
							})
						}
					});
				}
				
				closeSelectedViewsInfoButtonId=function(){
					showOverLayerDIv_project_print_template_viewsListSelect_form.hide();
					$("#overlay-shim-ieprintTemplate_viewListSelect_form").hide();
				}
				
				getSelectedViewsInfoFun=function(){
						var chooseViewValue = "";
						var chooseViewName = "";
						$.each($("input:checkbox[class=choosedView]:checked"),function(index, obj){
							chooseViewValue+=obj.value + ',';
							chooseViewName+=$(obj).next("span").text()  + ',';
						})
						chooseViewValue = chooseViewValue.substring(0, chooseViewValue.length - 1);
						chooseViewName = chooseViewName.substring(0, chooseViewName.length - 1);
						
						// 如果视图名称变化清空管理模型
						if(chooseViewValue != $("#printTemplateViewSelected_formsysId").val())
						{
							$("#printTemplateModelSelected").val("");
							$("#printTemplateModelSelected_formsysId").val("");
						}
						
						//设置视图名称
						$("#printTemplateViewSelected_formsysShow").val(chooseViewName);
						//设置视图value
						$("#printTemplateViewSelected_formsysId").val(chooseViewValue);
						showOverLayerDIv_project_print_template_viewsListSelect_form.hide();
						// 获取关联视图下的工作流信息
						getWorkFlowInfoByViewCode();
						$("#overlay-shim-ieprintTemplate_viewListSelect_form").hide();
						
				}
				
				// 获取关联视图下的工作流信息
				function getWorkFlowInfoByViewCode(){
					if("true" == $("#ec_print_template_form_entity_workflowEnabled").val())
					{
						var choosedViewCode = $("input:checkbox[class=choosedView]:checked");
						if(undefined != choosedViewCode && choosedViewCode.length > 0){
						   var viewCode = "";
						   $.each(choosedViewCode, function(index, obj){
						   		viewCode+="'" + obj.value + "',";
						   });
							viewCode = viewCode.substring(0,viewCode.length - 1);
							var printTemplateCode = '${entity.code}' + '_' + $("#ec_print_template_form_printTemplate_templateCode").val();
							CUI.ajax({
								url: "/msService/ec/printManage/getWorkFlowInfoByViewCode?entity.code=${entity.code}&printTemplate.viewCode=" + viewCode + "&printTemplate.code=" + printTemplateCode,
								type: 'post',
								async: true,
								success: function(result){
									if(undefined != result && null != result && result != ""){
										$("#wf_select").html("");
										// 请选择
										var pleaseChoice = '<option>${getText("ec.MenuInfo.Choice")}</option>'
										$("#wf_select").html(pleaseChoice + result);
									}else{
										// ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.print.template.RelworkFlow.errorInfo')}");
										// 请选择
										$("#wf_select").html("<option>${getText("ec.MenuInfo.Choice")}</option>");
										$("#ec_print_template_form_printTemplate_processKey").val("");
										$("#ec_print_template_form_printTemplate_processVersion").val("");
									}
							    }
							});
						}
					}
				}
				
			</SCRIPT>
			
			</td>
		</tr>
		<tr id="printTemplate_datagrid_type">
			<td class="la" name="printTemplate_addModel_td" style="width:20%;color:rgb(179, 3, 3);">
				${getHtmlText('ec.print.template.relatedModule')}</td>
			<td class="edit-table-content" name="printTemplate_addModel_td" colspan="3">
				<DIV class=fix-input>
					<DIV class="fix-search-click" id="printTemplate_modelCode_div"> 
						<INPUT style="PADDING-LEFT: 0px; WIDTH: 100%; MARGIN-RIGHT: -5px" id="printTemplateModelSelected" value="${modelNamesStr!''}"
							class="cui-noborder-input" readOnly type="text" name="printTemplate.value" property_type="SYSTEMCODE" istreesystem="1"> 
						
						<INPUT style="DISPLAY:NONE" id="printTemplateModelListSelect_clearbtn" class="cui-edit-clear" type="button" > 
						
						<INPUT id="systemValue_search_click_print_template_Model_select_eidt_form" style="top:3px;right:2px !important;"
							class="cui-search-click systemValue_search_click" value=&nbsp; type="button"></INPUT> 
					</DIV>
					<INPUT id="printTemplateModelSelected_formsysId" name="printTemplate.modelCode" value="${(printTemplate.modelCode)!''}" type="hidden" validateType="SystemCode">
					
				</DIV>
				<SCRIPT type=text/javascript>
					var showOverLayerDIv_project_print_template_modelsListSelect_form;
						$( '#printTemplateModelListSelect_clearbtn' ).bind( 'click', function(){
							$('#printTemplateModelSelected').val('');
							$('#printTemplateModelSelected_formsysId').val( '' );
							$(this).hide();
						});
						
						$( '#printTemplate_modelCode_div' ).hover(function(){
							if( $('#printTemplateModelSelected').val() ){
								$( '#printTemplateModelListSelect_clearbtn' ).show();
							}
						},function(){
							$( '#printTemplateModelListSelect_clearbtn' ).hide();
						})
						
					$('#systemValue_search_click_print_template_Model_select_eidt_form')
						.unbind('click.showOverlay').bind('click.showOverlay', function(e){
						ev = e || window.event;
						if (ev) { // 停止事件冒泡
							ev.stopPropagation();
						} else {
							window.event.cancelBubble = true;
						}
						
						showOverLayer_print_template_models_select_eidt_form(this, "/msService/ec/printManage/modelMenuTree?view.code=" + $("#printTemplateViewSelected_formsysId").val()
							+ "&isProj=false&entity.code=${entity.code}")
					})
				
	
					 $('body').click(
							function(){
								if(showOverLayerDIv_project_print_template_modelsListSelect_form 
									&& showOverLayerDIv_project_print_template_modelsListSelect_form.isShow) {
									showOverLayerDIv_project_print_template_modelsListSelect_form.hide();
								}
								
							}
						);
						
					function showOverLayer_print_template_models_select_eidt_form(obj, url){
						
						if(showOverLayerDIv_project_print_template_viewsListSelect_form)
						{
							showOverLayerDIv_project_print_template_viewsListSelect_form.hide();
						}
						
						if("" == $("#printTemplateViewSelected_formsysShow").val())
						{
							ec_print_template_formDialogErrorBarWidget.show('${getText("ec.printTemplate.chooseModel")}'); // 请先选择关联视图.
							return;
						}
						
						
						if(showOverLayerDIv_project_print_template_modelsListSelect_form){
							//showOverLayerDIv_project_print_template_modelsListSelect_form.setPosition();
							showOverLayerDIv_project_print_template_modelsListSelect_form.show();
						}

						// 如果视图名称不变，模型不用重新加载
						//if("" != tempViewName && tempViewName == $("#printTemplateViewSelected_formsysId").val()){
						//	return;
						//}
						
						showOverLayerDIv_project_print_template_modelsListSelect_form = new CUI.Overlay({
							align:obj,
					     	el:'printTemplate_modelListSelect_form',
					     	title:'${getText("ec.printTemplate.modelList")}', // 模型列表
					     	width:240,
					     	height:282,
					     	zIndex:9999,
					     	left:800,
					     	shadow:false,
							buttons:[
									{	name:'${getText("js.ec.script.manager.ok")}', // 确定
										id:"getSelectedModelsInfoButtonId",
										handler:function(){getSelectedModelInfoFun()}
									},
									{	name:'${getText("ec.common.cancel")}', // 取消
										id:"systemValue_search_click_print_template_model_select_eidt_form_overlay_btn_cancel",
										handler:function(){closeSelectedModelsInfoButtonId()}
									}]
						});
					
						var sysId=CUI("#printTemplateModelSelected_formsysId").val();
						var valueId="#"+sysId;
						var sysCode=CUI(valueId).val();
						showOverLayerDIv_project_print_template_modelsListSelect_form.render();
						
						$("#overlay-idprintTemplate_modelListSelect_form").click(
							function(e){
								ev = e || window.event;
								if (ev) { // 停止事件冒泡
									ev.stopPropagation();
				        		} else {
				        			window.event.cancelBubble = true;
				        		}
							}
						)
						url+="&fileName=knowledgeBorrowborrowType";
						url+="&formId=project_print_template_viewsListSelect_form";
						url+="&systemCodeCode="+sysCode;
						url+="&entityCode=borrowType";
						url+="&time="+new Date();
//						$("#overlay-shim-ieprintTemplate_modelListSelect_form,#overlay-idprintTemplate_modelListSelect_form").css("left", $("#printTemplateModelSelected").offset().left);
//						$("#overlay-shim-ieprintTemplate_modelListSelect_form,#overlay-idprintTemplate_modelListSelect_form").css("top", $("#printTemplateModelSelected").offset().top + 20);
						showOverLayerDIv_project_print_template_modelsListSelect_form.show();
						
						$("#printTemplate_modelListSelect_form").html(
							'<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("ec.printTemplate.loading")}</label></td></tr></table>' //正在加载...
						);
						
						/*
						CUI('#printTemplate_modelListSelect_form').load(url,function(){
							var existsValue = $('input:hidden', $(obj).parent()).val();
							var arr = existsValue.split(',');
							for(var x = 0; x < arr.length; x++) {
								if(arr[x] && arr[x].length > 0) {
									$('#printTemplate_modelListSelect_form input[name="' + arr[x] + '"]').prop('checked', true);
								}
							}
						});
						*/
						
						CUI.ajax({
							url: url,
							type: 'post',
							async: true,
							success: function(html)
							{
								$('#printTemplate_modelListSelect_form').html(html);
								if($('#printTemplate_modelListSelect_form')[0].innerText==""){
									ec_print_template_formDialogErrorBarWidget.show("${getText("ec.print.template.refView.notsamemodel")}");
									closeSelectedModelsInfoButtonId();
									return;
								}
								var checkedModelCodeStr = $("#printTemplateModelSelected_formsysId").val();

								$.each($(".choosedBox"),function(index, item){
									var commaStr = ",";
									if(checkedModelCodeStr.split(",").length == 1 || checkedModelCodeStr.split(",")[checkedModelCodeStr.split(",").length - 1] == $(item).val())
									{
										commaStr = "";
									}
								
									if(checkedModelCodeStr.indexOf($(item).val() + commaStr) != -1)
									{
										$(item).attr("checked","checked");
									}
								});
								
								
								var modelCnt = $(".main_properties_container li").size();
								if(modelCnt == 0){
									$("#printTemplateModelSelected").val("");
									$("#printTemplateModelSelected_formsysId").val("");
								}
								selectDescendCheckedModel();
							}
						});
						
						
						
						
					}
					
					function selectDescendCheckedModel() {
					    var str = $('#printTemplateModelSelected_formsysId').val();
					    var arr = str.split(',');
					    $.each(arr, function(i, v) {
					        var vArr = v.split('||');
					        var i = 0, propertyCode = '', $li, $img, hasclick, len = vArr.length;
					        while (i < len) {
					            propertyCode += propertyCode ? ('||' + vArr[i]) : vArr[i];
					            $li = $('.main_properties_container').find('li[propertycode="' + propertyCode + '"]');
					            $img = $li.find('img');
					            i++;            
					            if (i < len && $img.attr('hasclick') != 'true') {                
					                $img.trigger('click');
					            }
					            if (i == len) {
					                $input = $li.find('input');
					                $input.prop('checked', true);
					            }
					        }
					    })
					}

					
					closeSelectedModelsInfoButtonId=function(){
						showOverLayerDIv_project_print_template_modelsListSelect_form.hide();
						$("#overlay-shim-ieprintTemplate_modelListSelect_form").remove();
					}
					
					getSelectedModelInfoFun = function(){
						var chooseModelValue = "";
						var chooseModelName = "";
						$.each($("input:checkbox[class=choosedBox]:checked"),function(index, obj){
							chooseModelValue+=obj.value + ',';
							chooseModelName+=$(obj).next("span").text()  + ',';
						})
						chooseModelValue = chooseModelValue.substring(0, chooseModelValue.length - 1);
						chooseModelName = chooseModelName.substring(0, chooseModelName.length - 1);
						//设置模型名称
						$("#printTemplateModelSelected").val(chooseModelName);
						//设置模型value
						$("#printTemplateModelSelected_formsysId").val(chooseModelValue);
						showOverLayerDIv_project_print_template_modelsListSelect_form.hide();
						tempViewName = $("#printTemplateViewSelected_formsysId").val();
						$("#overlay-shim-ieprintTemplate_modelListSelect_form").remove();
					}
				</SCRIPT>
			
			</td>
		</tr>
		<tr id="printTemplate_wf_ywtzm">
		<td class="la">业务特征码</td>
		<td class="co" style="width:30%" colspan="3">
		<input type="text" <#if printTemplate?? && printTemplate.templateBusinesscode??>value="${printTemplate.templateBusinesscode}" </#if> name="printTemplate.templateBusinesscode" cssClass="cui-edit-field" style="PADDING-LEFT: 0px; WIDTH: 99.5%; MARGIN-RIGHT: -5px"/>
		</td>
		</tr>
		<tr id="printTemplate_wf_select" <#if entity?? && entity.workflowEnabled?? && entity.workflowEnabled?string("true","false") == 'true'><#else>style="display:none"</#if>>
			<#-- 关联视图工作流 -->
			<td class="la">${getText("ec.print.template.workFlowInfo")}</td>
			<td class="co" colspan="3">
				<select id="wf_select" onchange="workFlowInfo(this)" style="width:100%;height:24px;">
					<option>${getText("ec.MenuInfo.Choice")}</option>
					<#if listObj?? && (listObj?size>0)>
						<#list listObj as obj>
							<option processKey="${obj[1]}" processVersion="${obj[2]}" <#if printTemplate?? && printTemplate.processKey?? && printTemplate.processKey == obj[1]> selected="selected" </#if> >${obj[0]}</option>
						</#list>
					</#if>
				</select>
			</td>
		</tr>
		<#-- 
		<tr id="printTemplate_custom_button" <#if view?? && view.customFlag?? && view.customFlag?string("true","false") == 'true'><#else>style="display:none"</#if>>
			<td class="la"></td>
			<td class="co">
				<div id="printTemplate_defined_sqlscript" canclick="true" class="edit-btn btn-act" onclick="showDialog()">
					<a class="cui-btn-l">&nbsp;</a>
					<a class="cui-btn-c">
						<span i18n="ec.view.customerCondition">${getHtmlText('ec.view.customerCondition')}</span></a>
					<a class="cui-btn-r">&nbsp;</a>
				</div>
			</td>
			<td class="la batchControlPrintViews"></td>
			<td class="co batchControlPrintViews"></td
		</tr>
		<tr id="printTemplate_custom_templateScript" <#if view?? && view.customFlag?? && view.customFlag?string("true","false") == 'true'><#else>style="display:none"</#if>>
			<td class="la">${getHtmlText('ec.print.template.templateScript')}</td>
			<td class="co" colspan="3">
				<@s.textarea name="printTemplate.templateScript" id="dataClassificArea" cssClass="cui-edit-textarea"  cssStyle="width:97.5%;height:60px;margin-bottom:4px;"/>
			</td>
		</tr>
		-->
		<tr>
			<td class="la">${getText('ec.print.template.extraParam')}</td>
			<td class="co">
				<input type="checkbox" name="printTemplate.extraParam" onclick="ec.print.template.changeExtraParam()"/>
			</td>
		</tr>
		<tr id="extraParamCountTr" <#if printTemplate?? && printTemplate.extraParam?? && printTemplate.extraParam?string("true","false") == 'true'><#else>style="display:none"</#if>>
			<td class="la">${getText('ec.property.char')}${getText('ec.print.template.extraParamCount')}</td>
			<td class="co">
				<input type="text" name="printTemplate.extraParamCount" cssClass="cui-edit-field" cssStyle="width:25%;text-align:right;padding-right:2px"/>
				<span style="float:right;padding-right:5px;padding-top:4px">${getText('ec.property.picture')}${getText('ec.print.template.extraParamCount')}</span>
			</td>
			<td class="co">
				<input type="text" name="printTemplate.extraPicParamCount" cssClass="cui-edit-field" cssStyle="width:40%;text-align:right;padding-right:2px"/>
			</td>
			<td class="co">
				<a href="/msService/ec/printManage/editExtraParamScript" target="editExtraParamScriptWindow" style="color:blue">${getText('ec.print.template.editExtraParamScript')}</a>
			</td>
		</tr>
		<tr>
			<td class="la">${getHtmlText('ec.print.template.templateRemark')}</td>
			<td class="co" colspan="3">
				<input type="textarea"  name="printTemplate.templateRemark"  cssClass="cui-edit-textarea"  cssStyle="width:97.5%;height:60px;margin-top:4px;"/>
			</td>
		</tr>
		
	</table>
</@s.form>
<script type="text/javascript">
	// 注册命名空间
	// BAP-XA-DBZY zhanghd start
	CUI.ns("ec.print.template");
	
	/* 模板编码
	 * @method ec.print.template.codeValidate
	 * @public
	 */
	ec.print.template.codeValidate=function(){
	 	var validate=/^[a-z]{1}[a-zA-Z0-9_]{1,19}$/;
	 	var obj = $("#ec_print_template_form input[name='printTemplate.templateCode']").val();
		if (validate.test($("#ec_print_template_form input[name='printTemplate.templateCode']").val())){
			return true;
		}else{
		    ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.print.template.modelCode.formatmessage')}");
			return false
		}
	}
	/* 模板名称必须输入
	 * @method ec.print.template.templateName.requiredVerification
	 * @public
	 */
	ec.print.template.requiredVerification=function(){
		if($("#ec_print_template_form input[name='printTemplate.templateName']").val()=="" || 
			$("#ec_print_template_form input[name='international_printTemplatetemplateName_showName']").val()==""){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.print.template.name.Required')}");
			return false;
		}else{
			return true;
		}
	}
	/* 关联视图必须选择
	 * @method ec.print.template.refViewRequiredVerification
	 * @public
	 */
	ec.print.template.refViewRequiredVerification=function(){
		if($('#printTemplateViewSelected_formsysShow').val() == ""){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.printTemplate.viewCode.required')}");
			return false;
		}else{
			return true;
		}
	}
	
	/* 关联模型必须选择
	 * @method ec.print.template.refModelRequiredVerification
	 * @public
	 */
	ec.print.template.refModelRequiredVerification=function(){
		if($("#printTemplateModelSelected").val()==""){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.printTemplate.modelCode.required')}");
			return false;
		}else if($("#printTemplateModelSelected_formsysId").val().length>4000){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.print.template.relatedModule')}${getHtmlText('import.error.format.error.dataTooLong')}");
			return false;
		}else if($("input[name='printTemplate.extraParamCount']").val()!=""&&!/^[1-9]\d*$/.test($("input[name='printTemplate.extraParamCount']").val())){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.common.validate.int.plus.error',getText('ec.property.char')+getText('ec.print.template.extraParamCount'))}");
			return false;
		}else if($("input[name='printTemplate.extraParamCount']").val()!=""&&$("input[name='printTemplate.extraParamCount']").val()>50){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.common.validate.range.error2',getText('ec.property.char')+getText('ec.print.template.extraParamCount'),50)}");
			return false;
		}else if($("input[name='printTemplate.extraPicParamCount']").val()!=""&&!/^[1-9]\d*$/.test($("input[name='printTemplate.extraPicParamCount']").val())){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.common.validate.int.plus.error',getText('ec.property.picture')+getText('ec.print.template.extraParamCount'))}");
			return false;
		}else if($("input[name='printTemplate.extraPicParamCount']").val()!=""&&$("input[name='printTemplate.extraPicParamCount']").val()>50){
			ec_print_template_formDialogErrorBarWidget.show("${getHtmlText('ec.common.validate.range.error2',getText('ec.property.picture')+getText('ec.print.template.extraParamCount'),50)}");
			return false;
		}else{
			return true;
		}
	}
	
	ec.print.template.changeExtraParam=function(){
		if($("input[name='printTemplate.extraParam']").prop("checked")){
			$("#extraParamCountTr").show();
		}else{
			$("#extraParamCountTr").hide();
		}
	}
	
	function getHiddenExtraParamScript(){
		return $("input[name='printTemplate.extraParamScript']").val();
	}
	
	function setHiddenExtraParamScript(script){
		$("input[name='printTemplate.extraParamScript']").val(script);
	}
	
	function queryPrintTemplateCodeUnique(obj)
	{
		var flag = false;
		CUI.ajax({
			url: "/msService/ec/printManage/queryPrintTemplateCodeUnique?printTemplate.entity.code=${entity.code}&printTemplate.templateCode=" + $(obj).val(),
			type: 'post',
			async: false,
			success: function(subInfos){
				if(undefined != subInfos && null != subInfos && subInfos != "" && subInfos.flag == "false"){
					ec_print_template_formDialogErrorBarWidget.show('${getText("ec.printTemplate.templateCode.repeat")}');
					//$(obj).val("");
					flag = false;
				}else{
					flag = true;
				}
			}
		});
		return flag;
	}
	
	function showDialog(){
		if($('#printTemplate_defined_sqlscript').attr('canClick')=="true"){
			dg.advQuery.showAdv();
		}
	}
	
	
	// 设置工作流参数 processKey,processVersion
	function workFlowInfo(obj)
	{
		if("true" == $("#ec_print_template_form_entity_workflowEnabled").val()){
			$("#ec_print_template_form_printTemplate_processKey").val($(obj).find("option:selected").attr("processKey"));
			$("#ec_print_template_form_printTemplate_processVersion").val($(obj).find("option:selected").attr("processVersion"));
		}
	}

	(function(){
		// 设置工作流参数 processKey,processVersion
		workFlowInfo($("#wf_select"));
		$('#dataClassificArea').attr("readonly", "readonly");
	})();
</script>
<@customerCondition viewCode="X6Basic_1.0_teamManage_list" showArea="dataClassificArea" ccNameSpace="dg.advQuery"/>