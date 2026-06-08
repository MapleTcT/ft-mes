<style>
#fast_select_elements h2 {
margin: 1;
padding: 5px 0;
text-align: center;
font-size: 13px;
font-weight: normal;
border: 1px solid #0369A3;
cursor: pointer;
background-color: #7bb3ff;
}

#fast_select_elements .accordion_pane li {
font-size: 12px;
color: #000000;
cursor: pointer;
line-height: 18px;
z-index: 100;
margin-left: 4px;
margin-top:2px;
}

.sort-selected {
background: #A3E0FF;
}

.even-num {
background-color: #EDEDED;
}

</style>
<@errorbar id="ec_model_edit_formDialogErrorBar" />
<form id="ec_model_edit_form" name="ec_model_edit_form" action="/msService/ec/model/save" method="post" validate="true"  callback="ec.model.Callback" onreset="clearErrorMessages(this);clearErrorLabels(this);">
	<input type="hidden" name="model.version" <#if model?? && model.version??>value="${model.version}"</#if> id="ec_model_edit_form_model_version" />
    <input type="hidden" name="model.code" <#if model?? && model.code??>value="${model.code}"</#if> id="ec_model_edit_form_model_code"/>
    <input type="hidden" name="model.isConfigSpecial" <#if model?exists&&model.isConfigSpecial?exists&&model.isConfigSpecial== true>value="true"<#else>value="false"</#if> id="ec_model_edit_form_model_isConfigSpecial"/>
    <input type="hidden" name="model.entity.code"<#if model?? && model.entity.code??>value="${model.entity.code}"</#if> id="ec_model_edit_form_model_entity_code"/>
    <input type="hidden" name="model.moduleCode" <#if model?? && model.moduleCode??>value="${model.moduleCode}"</#if> id="ec_model_edit_form_model_moduleCode"/>
    <input type="hidden" name="model.orgTableName" <#if model?? && model.orgTableName??>value="${model.orgTableName}"</#if> id="ec_model_edit_form_model_orgTableName"/>
    <input type="hidden" name="model.isAndRelation" <#if model?exists&&model.isAndRelation?exists&&model.isAndRelation== true>value="true"<#else>value="false"</#if> id="ec_model_edit_form_model_isAndRelation"/>
    <input type="hidden" name="model.configBussinePermissionRight" <#if model?exists&&model.configBussinePermissionRight?exists&&model.configBussinePermissionRight== true>value="true"<#else>value="false"</#if> id="ec_model_edit_form_model_configBussinePermissionRight"/>
	<div id="listPropertyDlg" >
		<@errorbar id="ListPropertyDlgErrorBar"></@errorbar>
		<div id="dlg_div_public" class="dlg-etv-navset">
	 		<div class="etv-scrollbar" style="margin:0 5px;top:0;">
				<ul class="etv-nav" style="display: block;">
					<li class="selected" name="proertyTabs" id="base_attr_public">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
					<li name="proertyTabs" id="event_defined_public">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('foundation.userpermission.specialPermission')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
				</ul>
			</div>
		</div>
		<table id="tab_common" class="infoTable" cellpadding="0" cellspacing="0" border="0"  align="center" width="90%"  style="margin-top: 5px">
				<tr>
				    <td class="la supplant-required-field"  style="width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.model.modelName')} </td>
					<td class="co" colspan="3">
						<div class="fix-input">
							<#if model?? && (model.modelName)??>
							    <input type="text" name="model.modelName" readonly="true" maxlength="14" value="${model.modelName}" id="ec_model_edit_form_model_modelName" class="cui-noborder-input"/>
							<#else>
							    <input type="text" name="model.modelName" maxlength="14" value="" id="ec_model_edit_form_model_modelName" class="cui-noborder-input"/>
							</#if>
						</div>
					<span class="description">(${getHtmlText('ec.model.Modelname')})</span>
					</td>
				</tr>
				<tr>
				    <td class="la"  style="width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.model.tableName')} </td>
					<td class="co" colspan="3">
						<div class="fix-input">							
							<div class="fix-search-click" style="background-color:#DCDCDC">
							<input type="text" name="model.tableName" <#if model?? && (model.tableName)??>value="${model.tableName}"</#if> readonly="readonly" id="ec_model_edit_form_model_tableName" class="cui-noborder-input" style="background-color:#DCDCDC"/>
							<input type="button" onclick="modifyTableName();" title="点击可以修改表名" value="" class="cui-international-click" style='background:url("/bap/static/css/edit_20150318.png") -1px -1190px no-repeat transparent' >
							</div>
						</div>
						<span class="description">(${getHtmlText('ec.model.tableName')})</span>
					</td>
				</tr>
				<tr>
					<td class="la supplant-required-field"  style="width: 20%;padding-right: 10px;" align="right">${getHtmlText('ec.model.name')}</td>
					<td class="co" colspan="3">
						<@international name="model.name" key="${(model.name)!}" moduleCode=module.artifact isNew=true maxLength=80></@international>
						<#--
						<#if !model?exists>
							<@international name="model.name" key="" moduleCode="${(module.artifact)!}" cssClass="cui-edit-field"></@international>
						<#else>
							<@international name="model.name" key="${model.name!}" moduleCode="${(module.artifact)!}" cssClass="cui-edit-field"></@international>
						</#if>
						-->
						<span class="description">(${getHtmlText('ec.model.modelshow')})</span>
					</td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.model.dataType')}</td>
					<td class="co"><select name="model.dataType" <#if (model.code)??>disabled=true</#if>>
						<option value="1"
							<#if model?? && model.dataType = 1>
								selected
							</#if>
						>${getText('ec.model.dataType.1')}</option>
						<option value="2"
							<#if model?? && model.dataType = 2>
								selected
							</#if>
						>${getText('ec.model.dataType.2')}</option>
					</select></td>
					<td class="la">${getHtmlText('ec.model.isMain')}</td>
					<td class="co"><input type="checkbox" name="model.isMain"   onclick="ec.model.showSpecialPermissionTab(this)"  <#if (model?exists&&model.isMain== true) || (responseMap.firstIsMain)?? && (responseMap.firstIsMain)>checked="checked"</#if> value="true" <#if model??>disabled=true</#if> />
					<input type='hidden' name='model.isMain' value='<#if (model?exists&&model.isMain== true) || (responseMap.firstIsMain)?? && (responseMap.firstIsMain)>true<#else>false</#if>'></td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.model.isExtraCol')}</td>
					<td class="co">
						<input type="hidden" name="model.isExtraCol" value="<#if (model.isExtraCol)?? && model.isExtraCol>true<#else>false</#if>">
						<input type="checkbox" name="model_isExtraCol" onclick=$("input[name='model.isExtraCol']").val($(this).prop('checked')) <#if model?? && model.isExtraCol?? &&model.isExtraCol>checked="checked"</#if> cssClass="cui-edit-field" /></td>
					<td class="la">${getHtmlText('ec.model.isCache')}</td>
					<td class="co">
						<input type="hidden" name="model.isCache" value="<#if (model.isCache)?? && model.isCache>true<#else>false</#if>">
						<input type="checkbox" name="model_isCache" onclick=$("input[name='model.isCache']").val($(this).prop('checked')) <#if model?? && model.isCache?? &&model.isCache>checked="checked"</#if> cssClass="cui-edit-field" /></td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.model.enableSync')}</td>
					<td class="co">
					<input type="hidden" name="model.enableSync" value="<#if (model.enableSync)?? && model.enableSync>true<#else>false</#if>">
					<input type="checkbox" name="model_enableSync" onclick=$("input[name='model.enableSync']").val($(this).prop('checked')) <#if model?? && model.enableSync?? &&model.enableSync>checked="checked"</#if> cssClass="cui-edit-field" /></td>
					</td>
				</tr>
				<!-- 默认提供日志功能
				<tr>
					<td class="la">${getHtmlText('ec.model.enableOperationAudit')}</td>
					<input type="hidden" name="model.enableOperationAudit" value="<#if (model?exists && model.enableOperationAudit == true ) && (entity?exists && entity.enableAudit == true)>true<#else>false</#if>" />
					<td class="co"><input type="checkbox" onclick="ec.model.setOperationAudit()" id="enableOperationAudit" <#if (model?exists && model.enableOperationAudit == true ) && (entity?exists && entity.enableAudit == true)>checked="checked"</#if> value="true" <#if entity?exists&&entity.enableAudit == false>disabled=true</#if> /></td>
					<td class="la">${getHtmlText('ec.model.enableDataAudit')}</td>
					<input type="hidden" name="model.enableDataAudit" value="<#if (model?exists&&model.enableDataAudit == true) && (entity?exists&&entity.enableAudit == true)>true<#else>false</#if>" />
					<td class="co"><input type="checkbox" onclick="ec.model.setDataAudit()" id="enableDataAudit" <#if (model?exists&&model.enableDataAudit == true) && (entity?exists&&entity.enableAudit == true)>checked="checked"</#if> value="true" <#if entity?exists&&entity.enableAudit == false>disabled=true</#if> /></td>
				</tr> -->
				<tr>
					<td class="la">${getHtmlText('ec.model.description')}</td>
					<td class="co" colspan="3">
					<textarea name="model.description" cols="" rows="" id="modelDescripttion" class="cui-edit-textarea" style="width:95%;height:75px"> <#if model?? && model.description??>${model.description}</#if></textarea></td>
				</tr>
				<tr style="display:none">
				<td class="la">${getHtmlText('ec.model.isExtends')}</td>
				
					<td class="co" colspan="3"><input type="checkbox" name="model.isExtends" <#if model?exists&&model.isExtends?exists&&model.isExtends== true>checked="checked"</#if> value="true" onclick="ec.model.checkIsExtends(this)"/></td>
				</tr>
				
				<tr id="model_extendsModelName_code_tr" style="display:<#if model?exists&&model.isExtends?exists&&model.isExtends== true><#else>none</#if>;">
					<td class="la" width="20%" align="right">${getHtmlText('ec.model.choicemodel')}</td>
					<td class="co" width="80%" colspan="3">
					<select id="ec_select_module" onchange="ec.ass.selectChange('entity',this,'module.code')" style="width:30%" />
					&#160;<select id="ec_select_entity" onchange="ec.ass.selectChange('model',this,'entity.code')" style="width:30%"></select>
					&#160;<select id="ec_select_model" style="width:30%" name="model.extendsModelName.code"></select>
						
					</select>
					</td>
					
					</td>
				</tr>
		</table>
		<div  id="tab_event"    >
				<!--头部--->
				<div  id="innerContent">
				<@errorbar id="specialPermission_edit_formDialogErrorBar"></@errorbar>
				<div class="edit-modal-action-wrap">
					<span>
						<i class="upgrade"></i><input type="button"  value="升级"  onclick="changeRank(true)"/>
						<i class="downgrade"></i><input type="button"  value="降级"  onclick="changeRank(false)"/>
					</span>
					<span  style="padding-left:20px">
						<i class="moveup"></i><input type="button"  value="上移"  onclick="upRow('fast')"/>
						<i class="movedown"></i><input type="button"  value="下移"  onclick="downRow('fast')"/>
					</span>
				</div>
				<div >
					<!--左边--->
					<div style="margin-left:2px;margin-top:8px;width: 120px; height:257px;float:left;background-color:white;border:1px solid #e7e7e7;border-collapse:collapse;overflow:hidden">
							<input type="hidden" id="fastDelCells" />
							<div id="fast_select_elements">
								<h2 class="current"  style="color:#657f97;background-color: #f5f5f5;border:none;border-collapse:collapse;">${getHtmlText('ec.specialpermission.associateEntity')}</h2>
								<div class="accordion_pane" style="display:block;overflow:auto;height:205px">
									<ul class="main_properties_container">
										<!--系统编码-->
										<#if objectProperties?? && (objectProperties.properties)?? && (objectProperties.properties)?size &gt; 0>
											<#assign properties = (objectProperties.properties)>
											<#list properties as p>
												<#if p.type != "LONGTEXT" && p.type != "OFFICE" && p.type != "PROPERTYATTACHMENT">
												<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'id' && (p.name) != 'version' && p.name != 'layNo' && p.name != 'layRec' && (p.type) != "LONGTEXT">														
												<li source='main' modelDataType="<#if (p.model.dataType)?? && (p.model.dataType) == 2>tree<#else>simple</#if>" onclick='addObject(this)' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}'  isTree='${(p.isTreeSystemCode!false)?string('true','false')}'   modelCode="${(p.model.code)!}" namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}'    columnName='${(p.columnName)!}'   dataType='${(p.dataType)!}'     propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'   seniorSystemCode='${(p.seniorSystemCode!false)?string('true','false')}'     nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'>
													${getHtmlText('${(p.displayName)!}')}
												</li>
												</#if>	
												</#if>
											</#list>
										</#if>
										<!--关联模型-->
										<#if objectProperties?? && (objectProperties.associatedInfos)??>
											<#assign associatedInfos = (objectProperties.associatedInfos)>
											<#assign i = 1>
											<#list associatedInfos as ass>
												<#if ass.originalProperty?? && ass.originalProperty.name != 'status'>
												<#assign refViewCode=""  />
												<#assign refViewDisplayCode=""  />
												<#assign url=""  />
												<#list ass.refViewInfo as refView>
													<#assign refViewCode=refViewCode+","+refView.code />
													<#assign refViewDisplayCode=refViewDisplayCode+","+refView.displayCode />
													<#assign url=url+","+refView.url />
												</#list>
												<#if refViewCode??&&refViewCode?length gt 0>
													<#assign refViewCode=refViewCode?substring(1) />
													<#assign refViewDisplayCode=refViewDisplayCode?substring(1) />
													<#assign url=url?substring(1) />
												</#if>
												<li source='test' isTree='<#if (ass.targetProperty.model.dataType)?? && (ass.targetProperty.model.dataType) == 2>true<#else>false</#if>' partDepend='common' assTar='${ass.targetProperty.code}'    columnType='${(ass.originalProperty.type)!}'   namekey="${(ass.originalProperty.displayName)!}"   modelCode="${(ass.targetProperty.model.code)!}"     onclick='addObject(this)'  assPropertyName='${ass.targetProperty.name}'   refViewCode='${(refViewCode)!}'  refViewDisplayCode='${(refViewDisplayCode)!}'     url='${(url)!}'   propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}'  multable='${(ass.targetProperty.multable!false)?string('true','false')}'   seniorSystemCode='${(ass.targetProperty.seniorSystemCode!false)?string('true','false')}'  entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
													${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
												</li>	
												</#if>
												<#assign i = i+1>
											</#list>
										</#if>
									</ul>
								</div>
							</div>
					</div>
					<!-- 右侧 -->
					<div style="width:350px;background-color:white;float:left;height:96%;margin: 8px 5px 0 0;"> 	
						<div style="position:absolute;height:89%;width:75%;margin-left:1px">
							<div style="overflow-y:auto;clear:both;height:62%">
								<table cellpadding="0" id="fastColTable" style="font-size:12px;width:99%;border:1px solid #e7e7e7;border-collapse:collapse;" sectionCode="${sectionCode!}"  cellspacing="0" align="center" class="infoTable"  >
									<thead>
										<tr>
											<td  style="background-color:#f5f5f5;width:20%;padding-left:2px;color:#657f97">${getHtmlText('ec.specialpermission.relation')}</td>
											<td  style="background-color:#f5f5f5;width:10%;color:#657f97">${getHtmlText('ec.specialpermission.rank')}</td>
											<td  style="background-color:#f5f5f5;width:40%;color:#657f97">${getHtmlText('ec.specialpermission.condition')}</td>
											<td  style="background-color:#f5f5f5;width:20%;color:#657f97">${getHtmlText('ec.specialpermission.reference')}</td>
											<td  style="background-color:#f5f5f5;width:10%;color:#657f97">${getHtmlText('ec.specialpermission.action')}</td>
										</tr>
									</thead>
									<tbody id="fastColOrder">
												<#--历史数据--->
												<#if specialAuthorityList?? >
													<#list specialAuthorityList as element>
														<#assign cellCode = element.cellCode!ecCodeInit()>
														<#if element??>
															<#if (element.propertyCode)?? &&propertyMap?? && propertyMap['${propCode}']?? >
															
															<#else>																																																											 
																<tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}"  ondblclick="selectRow('fast',this);" onmousedown="selectRow('fast',this);"  rank="${(element.rank)!''}"    isTree='${(element.isTree!false)?string('true','false')}'    relation="${(element.relation)!''}"   refviewcode="${(element.refView.code)!''}"     columntype="${(element.type)!''}" name="${(element.name)!}" namekey="${(element.namekey)!}" key="${(element.key)!}"    propertyCode="${(element.property.code)!}" layRec="${(element.layRec)!}" nullable="${(element.nullable)!}"   modelcode="${(element.targetModelCode)!}"  specialPermissionCode="${(element.code)!}"   version="${(element.version)!0}"
																showFormat="${(element.showFormat)!}" assPropertyName="${element.assPropertyName!}" <#if (element.refCondition)?has_content>refCondition="${(element.refCondition)?html}"</#if> modelDataType="simple" containLower="false"  fill='{<#if (element.fill)?has_content><#list (element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.fill.fillType)?has_content && (element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.fill.fillOrder?has_content><#list element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.fill.fillContent)[ne])?html}"</#list><#else><#list (element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.fill.fillContent)[ne])?html}"</#list></#if>}<#else>"${((element.fill)[fe])?html}"</#if></#list></#if>}'>
															</#if>
																<td><select  id="relation"  onchange="updateTrInfo('relation',this)" style="margin-left:2px"><option value="OR" <#if element.relation??&&element.relation=="OR"  >selected</#if> >或者</option><option value="AND"  <#if element.relation??&&element.relation=="AND"  >selected</#if> >并且</option></select></td>
																<td><span id="headRank">${(element.rank)!}</span></td>
																<#if element.type=='OBJECT' >
																	<td align="left"  id="content"  >${getHtmlText(element.property.displayName)!}&nbsp;<#if element.targetModelName??>[${getHtmlText(element.targetModelName)!}]</#if></td>
																<#else>
																	<td align="left"  id="content"  >${getHtmlText(element.property.displayName)!}</td>
																</#if>
																<td>
																	<#if element.refView??>
																	<#assign refViewNode=element.refView />
																	<select  id="refView"  onchange="updateTrInfo('refviewcode',this)"  style="width:80px" >
																		<#list element.relateRefViews  as view>
																			<option  value="${(view.code)!}"  <#if refViewNode.code==view.code>selected</#if>  >${getHtmlText(view.displayName)!}</option>
																		</#list>
																	</select>
																	</#if>
																</td>
																<td style="text-align:left;" ><img title="${getText('foundation.view.del')}" style="cursor:pointer;margin-left:8px"  src="/bap/static/ec/delete.gif" onMouseOver="deleteBtnChange(this)" onMouseOut="deleteBtnChange(this)"  onClick="deleteFastQueryField($(this).parent())"></img></td>
																</tr>
														</#if>
													</#list>
												</#if>
										</tbody>
									</table>
							</div>
							<div	style="height:40px;width:99%;padding-left:2px;">
								  <textarea  id="previewResult"  style="border:1px solid #e7e7e7;width:99%;height:36px;"  placeholder="预览结果"  >${(specialPermissionPreview)!}</textarea>
							</div>
						</div>
					</div>
					<input type="hidden" name="xmlString" value="" id="ec_model_edit_form_xmlString"/>
				</div>
				</div>
		</div>
	</div>	
</form>
<script type="text/javascript">
(function(){
		//注册命名空间
		CUI.ns("ec.model", "ec.ass");
		 /* 编码规则验证
		 * @method ec.model.codeValidate
		 * @public
		 */
		ec.model.codeValidate=function(){
			var returnFlag=false;
		 	var validate=/^[A-Z]{1}[a-z]{1}[a-zA-Z0-9]+$/;
		 	var obj = $("#ec_model_edit_form input[name='model.modelName']").val();
		 	$("#ec_model_edit_form input[name='model.modelName']").val(obj.trim());
			if (validate.test($("#ec_model_edit_form input[name='model.modelName']").val())){
				returnFlag= true;
			}else{
				ec_model_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.model.formatmessage')}");
				return false;
			}
			
			var modelstr=$("#ec_model_edit_form input[name='model.modelName']").val();
			var tablename=$("#ec_model_edit_form input[name='model.tableName']").val().trim();
			if(tablename==''&&modelstr.length>1){
				var tableFlag=modelstr.substring(modelstr.length-2,modelstr.length);
				if(tableFlag=="Di"||tableFlag=="Pa"||tableFlag=="Sv"||tableFlag=="Mc"){
					ec_model_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.model.formatmodelname')}");
					return false;
				}
			}else if(tablename!=''&&tablename.length>2){
				var tableFlag=tablename.substring(tablename.length-3,tablename.length);
				if(tableFlag=="_DI"||tableFlag=="_PA"||tableFlag=="_SV"||tableFlag=="_MC"){
					ec_model_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.model.formattablename')}");
					return false;
				}
			}
			return returnFlag;
		}
		ec.model.checkIsExtends = function(obj) {
			if(obj.checked) {
				$('#model_extendsModelName_code_tr').show();
			} else {
				$('#model_extendsModelName_code_tr').hide();
			}
		if(obj.value!="" && (parseInt(obj.value) < parseInt($(obj).attr('bakvalue')))){
			ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.maxLengthIslarger')}");
			return false;
		}
	};
		ec.ass.selectChange = function(type,obj,param,callback){
			if(obj && $(obj).val()){
				var data = {};
				data[param] = $(obj).val();
				$.ajax({
					type : "POST",
					url : "/msService/ec/" + type + "/list-select",
					data : data,
					success : function(msg){
						var nextSelects = $(obj).nextAll("select");
						nextSelects.each(function(){this.options.length = 0;});
						var n = $(obj).next();
						var se = n[0];
						se.options.add(new Option('--',''));
						for(var i = 0 ; i < msg.length ; i ++){
							if(msg[i].nameInternational) {
								se.options.add(new Option(msg[i].nameInternational,msg[i].code));
							} else {
								se.options.add(new Option(msg[i].displayNameInternational,msg[i].code));
							}
						}
						if (callback) {
							CUI(obj).each(callback);
						}
					}
				});
			}
		};
		ec.model.showSpecialPermissionTab=function(obj)  {
			if($(obj).prop("checked"))  {
				$("#modelDescripttion").css("height","60px");
				$("#dlg_div_public").css("display","block");
				$("#base_attr_public").css("display","block");
				$("#event_defined_public").css("display","block");
			}else {
			   $("#modelDescripttion").css("height","90px");
			   $("#dlg_div_public").css("display","none");
			}
		};
		ec.model.load = function() {
			<#if model?exists && (model.isMain)== true>
			$("#modelDescripttion").css("height","60px");
			$("#dlg_div_public").css("display","block");
			$("#base_attr_public").css("display","block");
			$("#event_defined_public").css("display","block");
		
			<#else>
			$("#modelDescripttion").css("height","90px");
			$("#dlg_div_public").css("display","none");
			</#if>
			ec.model.showSpecialPermissionTab($("input[name='model.isMain']"));
		}
		
		ec.model.load();
		$('#tab_event').css('display','none');		
		
		//QC-455:实体配置-修改模型-业务数据权限，配置业务权限第一次保存成功后，第二次打开什么都不修改直接保存，配置变成空白 FIXED
		var  xmlStr="";
	    if(!($('tr','#fastColOrder').length!=null&&$('tr','#fastColOrder').length==0)) {
	        xmlStr=_buildXml();
	    }
	    //保存数据
	    $("input[name='xmlString']").attr('value', xmlStr);
		
		var liArr1 = $("#dlg_div_public").find('li');
		liArr1.each(function(index){
		    var objMe = $(this);
			var objId = $(objMe).attr('id');
		    $(this).unbind('click').bind('click',function(){
				$(this).removeClass('selected').addClass('selected');
		        liArr1.each(function(ind){
		            if($(this).attr('id')!=objId ){
		                $(this).removeClass('selected');
		            }
		        });
		        if(objId === 'base_attr_public'){
		            $('#tab_common').css('display','block');
		            $('#tab_event').css('display','none');
		            $('#innerContent').css('display','none');
		        }else if(objId === 'event_defined_public'){
		            $('#tab_common').css('display','none');
		            $('#tab_event').css('display','block');
		            if($("input[name='model.isMain']").prop("checked"))  {
		            	 $('#innerContent').css('display','block');
			            //style="padding:5px;border:1px #ccc   solid;" 
			            $('#tab_event').css('padding','5px');
			            //$('#tab_event').css('border','1px #ccc   solid');
		            }else {
		            	 $('#innerContent').css('display','none');
		            	 $('#tab_event').css('padding','0px');
		            	 $('#tab_event').css('border','0');
		            }
		        }
		    });
		});
		function upperFirstLetter(str){
			return str.replace(/\b\w+\b/g,function(word){
				return word.substring(0,1).toUpperCase()+word.substring(1);
			});
		}
		$("[name='model.modelName']").change(function(){
			var modelName = $("[name='model.modelName']").val().trim();
			var entityCode = "${entity.code}";
			if(null != modelName && "" != modelName){
				modelName = upperFirstLetter(modelName);
				$("[name='model.modelName']").val(modelName);
				$.ajax({
					type : "POST",
					url : "/msService/ec/model/formatTableName",
					data : {"model.modelName":$("[name='model.modelName']").val(),"entity.code":entityCode},
					success : function(msg){
						if(null != msg.tableName && "" != msg.tableName){
							$("[name='model.tableName']").val(msg.tableName);
							var moduleCode = "${module.code}";
							var moduleName = moduleCode.substring(0,moduleCode.lastIndexOf("_"));
							var entityCode = "${entity.code}";
							var entityName = entityCode.substring(entityCode.lastIndexOf("_")+1);
							var modelName = $("[name='model.modelName']").val();
							var internationKey = "key=" + moduleName + "." + entityName + "." + modelName;
							$("[name='model.name']").attr("key",internationKey);
							// fix119260: 修改key同时修改国际化value中的key
							var intlVal = $("[name='model.name']").val();
							if (intlVal){
								var splitVal = intlVal.split('$&#');
								if (splitVal[0] && splitVal[0] !== internationKey){
									splitVal[0] = internationKey;
									$("[name='model.name']").val(splitVal.join('$&#'));
								}								
							}
						}
					}
				});
			}
		});
			CUI(function(){
		function submitBapForm(){//电子签名成功之后出现进度条并提交表单
			var ecFormFlag = false;
			var retrialFormFlag = false;
			if(ecFormFlag && ( $('#ec_model_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
				// dialog不出进度条
				ecFormFlag = false;
			}
			ecFormFlag = (ecFormFlag || retrialFormFlag);
			
			//前台验证通过之后出进度条
			CUI.Dialog.toggleAllButton('ec_model_edit_form',true,ecFormFlag, true);
			// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
			setTimeout(function(){
			
				// 延迟保存数据, 解决onchange事件无法触发问题
				var formId = 'ec_model_edit_form';
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
								$("#ec_model_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
									if($(this).parents('td[nullable=false]').length > 0) {
										showErrorField($(this));
									}
								});
							} else {
								var field = CUI("#ec_model_edit_form *[name='"+index+"']");
								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
									showErrorField(field.next());
								} else {
									showErrorField(field);
								}
							}
							CUI("#ec_model_edit_form *[name='"+index+"']").first().focus();
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
						oErrorWidget = ec_model_edit_formDialogErrorBarWidget;
						if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
							oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
						}	else {
							oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
						}					
						if(CUI.Dialog){
							CUI.Dialog.toggleAllButton('ec_model_edit_form', true);
						}
					},
					success : function(msg){
						window.onbeforeunload = null;
						if(window.containerLoadPanelWidget) {
							setTimeout(function(){closeLoadPanel();}, 500);
						}
						ec.model.Callback(msg,postData);
					}
				});
			}
		}, 600);
			return false;
		}
		CUI('#ec_model_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
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
			//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_model_edit_form',true,ecFormFlag);
			//});
				//if(!validateForm_ec_model_edit_form())	return false;
			$('#ec_model_edit_form').trigger('beforeSubmit');
			if($('#ec_model_edit_form input[name="operateType"]').val() == "submit"){
				var deploymentId=$('#ec_model_edit_form input[name="deploymentId"]');
				var buttonCode=$('#ec_model_edit_form input[name="buttonCode"]');
				var namespace=$('#ec_model_edit_form input[name="namespace"]');
				if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
					var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
					if(signatureInfo[0] != '') {
						var cancelItem = $('input[name="workFlowVarStatus"]');
						if(cancelItem.val() != "cancel") {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_model_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_model_edit_form','ec.flow.submit',false)});
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_model_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_model_edit_form','ec.edit.remove',false)});
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
							parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_model_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_model_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_model_edit_form',buttonCode.val(),false)});},2000);
							}	
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_model_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_model_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_model_edit_form',buttonCode.val(),false)});},2000);
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


	/**
	 * 添加字段
	 */
	function addObject(obj) {
		var objMe = this;
		var li = $(obj);
		var propertyCode = li.attr('propertyCode');
		var flag = true;
		$('tr','#fastColOrder').each(function(index){
			if($(this).attr('propertyCode') == propertyCode) {
				flag = false;
				return false;
			}
		});
		var	cellCode = "cell_" + new Date().getTime() +'_'+ Math.round(Math.random(1)*10000);
		if(flag) {
			var target = $('<tr id="tr1" cellCode="' + cellCode+ '"><td><select  id="relation"  style="margin-left:2px" onchange="updateTrInfo(\'relation\',this)"><option value="OR">或者</option><option value="AND"  selected >并且</option></select></td><td><span id="headRank">1</span></td><td align="left"  ></td></tr>');
			var refSel="";
			if(li.attr('columnType')=='SYSTEMCODE')  {
				refSel = $('<td></td>');
			}else {
				refSel = $('<td><select  id="refView"   style="width:80px" onchange="updateTrInfo(\'refviewcode\',this)"></select></td>');
			}
			var viewCode;
			var viewTitle;
			var url;
			if(li.attr('columnType')=='OBJECT'&&li.attr('refviewcode')!='undefined'&&li.attr('refviewdisplaycode')!='undefined')  {
				if(li.attr('refviewcode').indexOf(",")!=-1&&li.attr('refviewdisplaycode').indexOf(",")!=-1)  {
					viewCode =  li.attr('refviewcode').split(",");
					viewTitle = li.attr('refviewdisplaycode').split(",");
					url = li.attr('url').split(",");
					for(var i=0;i<viewCode.length;i++)  {
						var  option = $('<option  value="'+viewCode[i]+'" url="'+url[i]+'">'+viewTitle[i]+'</option>');
						refSel.find("#refView").append(option);
					}
				}else {
					viewCode =  li.attr('refviewcode');
					viewTitle = li.attr('refviewdisplaycode');
					url = li.attr('url');
					var  option = $('<option  value="'+viewCode+'" url="'+url+'">'+viewTitle+'</option>');
					refSel.find("#refView").append(option);
				}
			}
			
			
			target.append(refSel);
			var objImg = $('<td style="text-align:center;"><img title="${getText('foundation.view.del')}"  style="cursor:pointer;marigin-left:8px" src="/bap/static/ec/delete.gif" onMouseOver="deleteBtnChange(this)" onMouseOut="deleteBtnChange(this)"></img></td>');
			objImg.unbind('click').bind('click',function(){
				objMe.deleteFastQueryField(this);
			});
			target.append(objImg);
			var columnType = li.attr('columnType');
			var showType = columnType;
			target.removeAttr('ondblclick');
			target.unbind('dblclick').dblclick(function(){
				objMe.selectRow('fast',this);
				objMe.fastFieldProperty();
			});
			$('td:eq(2)', target).text($.trim(li.text()));
			
			if(li.attr('propertyCode')!=undefined && li.attr('propertyCode').search('\\|')!=-1)  {
				var  propertyCodes =  new Array();
				var  finalString  = "";
				var  reverseString = "";
				var  reverse =  new Array();
				propertyCodes=li.attr('propertyCode').split('||');
				var parent = li;
				for(var i=1;i<propertyCodes.length;i++)  {
					parent = parent.parent();
					var  targetString =  $.trim(parent.prev().find('span:first').attr("i18n"));
					finalString+=targetString+",";
				}
				reverse=finalString.trim().split(",");
				for(var j=reverse.length-1;j>=0;j--)  {
					if(reverse[j]!="")  {
					reverseString+=reverse[j]+",";
					}
				}
				target.removeAttr('namekey').attr('namekey', reverseString+li.attr('namekey'));
				//$('td:first', target).text(targetString.substring(0,targetString.indexOf('[')-1)+"."+$.trim(li.text()));
			}else {
				target.removeAttr('namekey').attr('namekey', li.attr('namekey'));
			}
			
			target.removeAttr('key').attr('key', li.attr('name')).removeAttr('partDepend').attr('partDepend',li.attr('partDepend')).removeAttr('propertyCode').attr('propertyCode',li.attr('propertyCode')).removeAttr('layRec').attr('layRec', li.attr('layRec')).removeAttr('name').removeAttr('showType').removeAttr('checkname').removeAttr('columnType').removeAttr('nullable').attr('name', li.attr('name')).attr('checkname',$.trim(li.text())).attr('nullable',li.attr('nullable')).removeAttr('mnecode').attr('mnecode', li.attr('mnecode'))
			.attr('columnType', li.attr('columnType')).removeAttr('selfType').attr('selfType', li.attr('columnType')).removeAttr('funcname').removeAttr('funcbody').removeAttr('sourcepropertyname').removeAttr('multable').attr('multable', li.attr('multable')).removeAttr('seniorsystemcode').attr('seniorsystemcode', li.attr('seniorsystemcode')).removeAttr('modelcode').attr('modelcode', li.attr('modelcode')).removeAttr('propDefaultValue').attr('propDefaultValue',li.attr('propDefaultValue')).removeAttr('defaultValue').attr('defaultValue',li.attr('propDefaultValue')).removeAttr('defaultValueHasChanged').attr('defaultValueHasChanged',false)
			.removeAttr('showTypeHasChanged').attr('showTypeHasChanged',false).removeAttr('showFormatHasChanged').attr('showFormatHasChanged',false).attr('showFormat',li.attr('propShowFormat')).attr('showType',li.attr('propShowType')).removeAttr('columnName').attr('columnName', li.attr('columnname'));
			target.removeAttr('rank').attr('rank','1');
			target.removeAttr('relation').attr('relation','AND');
			target.removeAttr('modelCode').attr('modelCode',li.attr('modelCode'));
			target.removeAttr('refViewCode').attr('refViewCode',refSel.find("#refView").val());
			target.removeAttr('url').attr('url',refSel.find("#refView").find("option:selected").attr("url"));
			target.removeAttr('isTree').attr('isTree',li.attr('isTree'));
			
			// 增加原属性的input标示，以便确认是否已经添加过关联实体的属性
			if(li.attr('sourcePropertyName') && li.attr('sourcePropertyName')!=''){
				target.attr('sourcePropertyName', li.attr('sourcePropertyName'));
			}	
			target.bind('click',function(obj){
				objMe.selectRow('fast',this);
			});
			target.attr('newAdd',"add");
			$('#fastColOrder').append(target);
			//生成条件
			li.addClass("dragout");
			var select = $('.sort-selected','#fastColOrder');
			var pre_index=CUI(select).prev().index();
			var next_index=CUI(select).next().index();
			if(pre_index==-1){
				objMe.disableUPBtn('fast',1);
			}else{
				objMe.disableUPBtn('fast',0);
			}	
			if(next_index==-1){
				objMe.disableDownBtn('fast',1);
			}else{
				objMe.disableDownBtn('fast',0);
			}	
			objMe.addSortStyle('fast');
		}
	}
	
	
	function  updateTrInfo(key,obj)  {
		$(obj).parent().parent().attr(key,$(obj).val());
		addSortStyle('fast');
	}
	
	
	function deleteFastQueryField(obj) {
		if (confirm("${getText('ec.common.checkDelete')}")) {
			var objMe = this;
			var delCells = $("#fastDelCells").val();
			if(delCells!=''){
				$("#fastDelCells").val(delCells+","+$(obj).parent().attr("cellCode"));
			}else{
				$("#fastDelCells").val($(obj).parent().attr("cellCode"));
			}
			$(obj).parent().remove();
			$('li[propertyCode="'+ $(obj).parent().attr("propertyCode")+'"]','#fast_select_elements').removeClass("dragout");
			var selected = $('.sort-selected','#fastColOrder');
			var pre_index=CUI(selected).prev().index();
			var next_index=CUI(selected).next().index();
			if(pre_index==-1){
				objMe.disableUPBtn('fast',1);
			}else{
				objMe.disableUPBtn('fast',0);
			}	
			if(next_index==-1){
				objMe.disableDownBtn('fast',1);
			}else{
				objMe.disableDownBtn('fast',0);
			}
			objMe.addSortStyle('fast');
		}
	} 
	ec.model.setDataAudit = function(){
		if($('#enableDataAudit').prop('checked')){
			$('input[name="model.enableOperationAudit"]').val('true');
			$('input[name="model.enableDataAudit"]').val('true');
		}else{
			$('input[name="model.enableDataAudit"]').val('false');
		}
	}
	ec.model.setOperationAudit = function(){
		if($('#enableOperationAudit').prop('checked')){
			$('input[name="model.enableOperationAudit"]').val('true');
		}else{
			$('input[name="model.enableOperationAudit"]').val('false');
			$('input[name="model.enableDataAudit"]').val('false');
		}
	}	
	
	function addSortStyle(type) {
		if(type == 'fast') {
			$('.even-num','#fastColOrder').removeClass('even-num').removeAttr('numType');
			$('.odd-num','#fastColOrder').removeClass('odd-num').removeAttr('numType');
			$('tr:even','#fastColOrder').addClass('even-num').attr('numType',"even");
			$('tr:odd','#fastColOrder').addClass('odd-num').attr('numType',"odd");
			if($('tr.sort-selected','#fastColOrder').attr('numType') == "even") {
				$('tr.sort-selected','#fastColOrder').removeClass("even-num");
			}
		}
		$("#previewResult").val("");
		var temp="";
		$("#fastColOrder").find("tr").each(
			  function(index){
			  		var leftRank = parseInt($(this).find("td").eq(1).find("#headRank").text())-1;
			  		var rightRank = parseInt($(this).find("td").eq(1).find("#headRank").text())-1;
			  		var leftSysmbol ="";
			  		var rightSysmbol ="";
			  		var previousRank = 0;
			  		var relation = $(this).find("td").eq(0).find("#relation").val();
			  		var nodeText=$(this).find("td").eq(2).text();
			  	
			  		if(index==0)  {
					    previousRank = leftRank;
				    }else {
					    previousRank = parseInt($(this).prev().find("td").eq(1).find("#headRank").text())-1;
				    }
				    if(previousRank==leftRank)  {
				    	//前后2个等级一致
				    	if(index==0)  {
				    		while(leftRank>0)  {
				    			leftSysmbol+="(";
					  			leftRank--;
				    	    }
				    	}else {
					    	while(leftRank>0)  {
					    			//去除右括号
					    			temp=temp.substring(0,temp.length-1);
						  			leftRank--;
					    	}
					    	leftSysmbol="";
				    	}
				    }else {
				    	while(leftRank>0)  {
				    			leftSysmbol+="(";
					  			leftRank--;
				    	}
				    }
				    if(temp=="")  {
				    		temp=temp+" "+leftSysmbol+" "+nodeText+" ";
				    }else {
				    		temp=temp+" "+relation+" "+leftSysmbol+" "+nodeText+" ";
				    }
				  	while(rightRank>0) {
						  rightSysmbol+=")";
						  rightRank--;
					}
					temp=temp+rightSysmbol;
			  }
		);
		$("#previewResult").val($("#previewResult").val()+temp);
		var previouseTemp="";
		var  findFirstTag=false;
		$("#fastColOrder").find("tr").each(
			  function(){
			  	//先找到最外层的关系,该关系由排在最前面的等级为1的条件决定
			  	if($(this).find("td").eq(1).find("#headRank").text()=="1")  {
			  		$("#previewResult").val($("#previewResult").val().replace(/^AND|^OR/gi,""));
			  		if($(this).find("td").eq(0).find("#relation").val()=="AND")  {
				  		previouseTemp=" AND ("+previouseTemp;
				  		$("input[name='model.isAndRelation']").attr('value', 'true');
				  	}else if($(this).find("td").eq(0).find("#relation").val()=="OR")  {
				  		previouseTemp=" OR ("+previouseTemp;
				  		$("input[name='model.isAndRelation']").attr('value', 'false');
				  	}
				  	$("#previewResult").val(previouseTemp+$("#previewResult").val());
				  	$("#previewResult").val($("#previewResult").val()+"  )");
				  	findFirstTag=true;
				  	return false;
			  	}
			  }
			);
		if(!findFirstTag)  {
			 $("[name='model.configBussinePermissionRight']").val("false");
		}else {
			 $("[name='model.configBussinePermissionRight']").val("true");
		}
		var  xmlStr="";
		if(!($('tr','#fastColOrder').length!=null&&$('tr','#fastColOrder').length==0)) {
		    xmlStr=_buildXml();
		}
		//保存数据
		$("input[name='xmlString']").attr('value', xmlStr);
	}
	
	/**
	 * 查询SQL组织所需XML
	 */
   function  _buildXml() {
		var objMe = this;
		var xmlStr = '<?xml version="1.0" encoding="UTF-8"?>';
			xmlStr += '<objects>';
			$('tr','#fastColOrder').each(function(){
				if ($.trim($('td', this).html()) != '') {
					// 组织li中的元素
					var tempNum = 1;
					for(var i = 0 ; i < tempNum; i++) {
						xmlStr += '<item>';
						var name = $(this).attr('name');
						xmlStr += '<name><![CDATA[' + name +']]></name>';
						if ($(this).attr('propertyCode')) {
							xmlStr += '<propertyCode><![CDATA[' + $(this).attr("propertyCode") + ']]></propertyCode>';
						}
						if($(this).attr('modelCode')){
							xmlStr += '<modelCode><![CDATA[' + $(this).attr("modelCode") + ']]></modelCode>';
						}
						if($(this).attr('rank')){
							xmlStr += '<rank><![CDATA[' + $(this).attr("rank") + ']]></rank>';
						}
						if($(this).attr('relation')){
							xmlStr += '<relation><![CDATA[' + $(this).attr("relation") + ']]></relation>';
						}
						if($(this).attr('specialPermissionCode')){
							xmlStr += '<specialPermissionCode><![CDATA[' + $(this).attr("specialPermissionCode") + ']]></specialPermissionCode>';
						}
						if($(this).attr('version')){
							xmlStr += '<version><![CDATA[' + $(this).attr("version") + ']]></version>';
						}
						if($(this).attr('columnType')){
							xmlStr += '<columnType><![CDATA[' + $(this).attr("columnType") + ']]></columnType>';
						}
						if($(this).attr('refViewCode')){
							xmlStr += '<refViewCode><![CDATA[' + $(this).attr("refViewCode") + ']]></refViewCode>';
						}
						if($(this).attr('isTree')){
							xmlStr += '<isTree><![CDATA[' + $(this).attr("isTree") + ']]></isTree>';
						}
						xmlStr += '</item>';
					}
				} 
			});
			xmlStr += '</objects>';// section的
			return xmlStr;
	}	
	
	function disableUPBtn(id,type){// 1禁用，其他开启
		var fullId = id + "Content";
		if(type=='1'){
			CUI("#upMove",'#' + fullId).removeClass("ec-list-prevbtn").addClass("ec-list-dis-prevbtn");
			CUI("#firstMove",'#' + fullId).removeClass("ec-list-topbtn").addClass("ec-list-dis-topbtn");
		}else{
			CUI("#upMove",'#' + fullId).removeClass("ec-list-dis-prevbtn").addClass("ec-list-prevbtn");
			CUI("#firstMove",'#' + fullId).removeClass("ec-list-dis-topbtn").addClass("ec-list-topbtn");	
		}
	}

	function disableDownBtn(id,type){// 1禁用，其他开启
		var fullId = id + "Content";
		if(type=='1'){
			CUI("#downMove",'#' + fullId).removeClass("ec-list-nextbtn").addClass("ec-list-dis-nextbtn");
			CUI("#lastMove",'#' + fullId).removeClass("ec-list-lastbtn").addClass("ec-list-dis-lastbtn");
		}else{
			CUI("#downMove",'#' + fullId).removeClass("ec-list-dis-nextbtn").addClass("ec-list-nextbtn");
			CUI("#lastMove",'#' + fullId).removeClass("ec-list-dis-lastbtn").addClass("ec-list-lastbtn");	
		}
	}
	
	
	
	function  changeRank(isAdd)  {
		 var objMe = this;
		 var selectedTr = $("#fastColOrder").find(".sort-selected");
		 if(selectedTr.length==0)   {
		 		specialPermission_edit_formDialogErrorBarWidget.show("${getHtmlText('请选择相关行!')}");
				return false;
		 }
		 if(isAdd)  {
		 		var newRank=parseInt(selectedTr.attr("rank"))+1;
		 		selectedTr.removeAttr('rank').attr('rank',newRank);
		 		selectedTr.find("#headRank").text(newRank);
		 }else {
		 		var newRank=parseInt(selectedTr.attr("rank"))-1;
		 		if(newRank==0) {
		 			specialPermission_edit_formDialogErrorBarWidget.show("${getHtmlText('已到最低等级,无法降级!')}");
					return false;
		 		}
		 		selectedTr.removeAttr('rank').attr('rank',newRank);
		 		selectedTr.find("#headRank").text(newRank);
		 }
		 objMe.addSortStyle('fast');
	}
	
	function selectRow(type,obj){
		var objMe = this;
		CUI('.sort-selected').each(function(){
			CUI(this).removeClass("sort-selected");
			if(CUI(this).attr('numType') == "even") {
				CUI(this).addClass("even-num");
			}
		});
		if(CUI(obj).attr('numType') == "even") {
			CUI(obj).removeClass("even-num");
		}
		CUI(obj).addClass("sort-selected");
		var pre_index=CUI(obj).prev().index();
		var next_index=CUI(obj).next().index();
		if(pre_index==-1){
			objMe.disableUPBtn(type,1);
		}else{
			objMe.disableUPBtn(type,0);
		}	
		if(next_index==-1){
			objMe.disableDownBtn(type,1);
		}else{
			objMe.disableDownBtn(type,0);
		}	
		
	}
	
	
	// 上一行
	function upRow(type){
		var objMe = this;
		var selectedRow= ('list' == type) ? $('tr.sort-selected',$('#listColOrder')) : $('tr.sort-selected',$('#fastColOrder'));
		if(selectedRow.size()<1){
			specialPermission_edit_formDialogErrorBarWidget.show("请选择相关行");
			return false;
		}
		var prevRow = selectedRow.prev('tr');
		var index=prevRow.index();
		prevRow.before(selectedRow);
		if(index==0){
			objMe.disableUPBtn(type,1);
			objMe.disableDownBtn(type,0);
		}else{
			objMe.disableDownBtn(type,0);
		}
		objMe.addSortStyle(type);
	}

	// 下一行
	function downRow(type){
		var objMe = this;
		var selectedRow= ('list' == type) ? $('tr.sort-selected',$('#listColOrder')) : $('tr.sort-selected',$('#fastColOrder'));
		if(selectedRow.size()<1){
			specialPermission_edit_formDialogErrorBarWidget.show("请选择相关行");
			return false;
		}
		var nextRow = selectedRow.next('tr');
		var index=nextRow.next().index();
		nextRow.after(selectedRow);
		if(index==-1){
			objMe.disableUPBtn(type,0);
			objMe.disableDownBtn(type,1);
		}else{
			objMe.disableUPBtn(type,0);
		}
		objMe.addSortStyle(type);
	}
	
	function modifyTableName(){
		$("[name='model.tableName']").removeAttr("readonly");
		$("[name='model.tableName']").parents(".fix-search-click").css({"background-color":"#FFFFFF"});
		$("[name='model.tableName']").css({"background-color":"#FFFFFF"});
	}

</script>