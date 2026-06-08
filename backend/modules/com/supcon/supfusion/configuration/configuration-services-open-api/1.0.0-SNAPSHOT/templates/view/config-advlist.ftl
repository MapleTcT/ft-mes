<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${(subs.mainEntityName)!}-${getText(view.displayName)}-${getText('ec.project.view_setting')}-<#if view.type == 'LIST'>${getText('ec.view.list')}<#else>${getText('ec.view.ref')}</#if></title>
	<@ec_advconfig/>
	<@loadpanel />
	<style type="text/css">
	</style>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2" layoutCode="${(ev.configMap.layout.layoutCode)!ecCodeInit('layout')}" page_onload="${((ev.configMap.layout.pageConfig.onload)!)?html}" >
	<#assign rpFlag=checkUserPermisition('ec_adv_query_view')>
	<@errorbar id="workbenchErrorBar" offsetY=83 />
	<div style="height:270px;width:730px;line-height: 24px;">
		<div id="form_select_element" style="float:left;height:100%;width:18%;border: 1px solid #E1E1E1;margin-right:2px;">			
				<h2 style="text-align:center;font-size:11px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;" class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:82%">
				<ul class="main_properties_container" style="white-space:nowrap">
					<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
						<#assign properties = (subs.properties)>
						<#list properties as p>
							<#if p.type != "LONGTEXT" && p.type != "OFFICE" && p.type != "PROPERTYATTACHMENT">
							<#if (p.name) != 'tableInfoId'  && (p.name) != 'id' && (p.name) != 'version' && p.name != 'layNo' && p.name != 'layRec' && (p.type) != "LONGTEXT" &&!((p.name) == 'status'&&(p.model.entity.workflowEnabled!false)?string('true','false')=='false')>
							<li  ondblclick="ec.advQuery.query._createcond(this)" class="uselist ui-ordinary" source='main' modelDataType="<#if (p.model.dataType)?? && (p.model.dataType) == 2>tree<#else>simple</#if>" partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnName='${(p.columnName)!}'  name='${(p.name)!}' tableName='${(p.model.tableName)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'   nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'>
								${getHtmlText('${(p.displayName)!}')}
							</li>
							</#if>	
							</#if>
						</#list>
					</#if>
					<#if subs?? && (subs.associatedInfos)??>
						<#assign associatedInfos = (subs.associatedInfos)>
						<#assign i = 1>
						<#list associatedInfos as ass>
							<#if ass.originalProperty?? && ass.originalProperty.name != 'status'>
							<li  ondblclick="ec.advQuery.query._createcond(this)" class="uselist ui-object" source='test' parentModelDataType='<#if (ass.targetProperty.model.dataType)?? && (ass.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" columnType='${(ass.originalProperty.type)!}' dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' tableName='${(ass.targetProperty.model.tableName)!}' entityCode='${(ass.targetProperty.model.code)!}'  mnecode='${(ass.targetProperty.model.isMneCode!false)?string('true','false')}' columnName='${(ass.originalProperty.columnName)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
								<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(ass.targetProperty.model.code)!}","adv")'></img>
								${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
							</li>	
							</#if>
							<#assign i = i+1>
						</#list>
					</#if>
				</ul>
				</div>
				<h2 style="text-align:center;font-size:11px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;">${getHtmlText('ec.view.one2manyattr')}</h2>
				<div class="accordion_pane" style="overflow:auto;height:82%">
				<ul class="onetomany_properties_container" style="white-space:nowrap">
					<#if subs?? && (subs.oneToManyAssociatedInfos)?? && (subs.oneToManyAssociatedInfos)?size &gt; 0>
						<#assign oneToManyAssociatedInfos = (subs.oneToManyAssociatedInfos)>
						<#assign j = 1>
							<#list oneToManyAssociatedInfos as onetoManyAss>
								<#assign one2ManyOriginalTableName = (onetoManyAss.targetProperty.model.tableName)!>
								<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name)>
								<#if one2ManyOriginalTableName == 'WF_DEAL_INFO'>
									<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_DI')>
									<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Di')>
								<#elseif one2ManyOriginalTableName == 'WF_PAY_CLOSE_ATTENTION'>
									<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_PA')>
									<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Pa')>
								<#elseif one2ManyOriginalTableName == 'WF_SUPERVISION'>
									<#assign one2ManyOriginalTableName = (onetoManyAss.originalProperty.model.tableName + '_SV')>
									<#assign one2ManyTargetPropertyName = (onetoManyAss.targetProperty.name + onetoManyAss.targetProperty.model.modelName + 'Sv')>
								</#if>
								<li source='test' parentModelDataType='<#if (onetoManyAss.targetProperty.model.dataType)?? && (onetoManyAss.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='one2many' assTar='${onetoManyAss.targetProperty.code}' propertyCode="${onetoManyAss.targetProperty.code!}" assOrg="${onetoManyAss.originalProperty.code}" columnType='${(onetoManyAss.originalProperty.type)!}' dbname='${(onetoManyAss.targetProperty.model.modelName)!}' name='${(one2ManyTargetPropertyName)!}' entityCode='${(onetoManyAss.targetProperty.model.code)!}' mnecode='${(onetoManyAss.targetProperty.isMneCode!false)?string('true','false')}' columnName='${(onetoManyAss.originalProperty.columnName)!}' relativeName='${(one2ManyOriginalTableName)!},${(onetoManyAss.targetProperty.columnName)!},${(onetoManyAss.originalProperty.model.tableName)!},${(onetoManyAss.originalProperty.columnName)}'>
									<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(onetoManyAss.targetProperty.model.code)!}","adv")'></img>
									${getHtmlText('${(onetoManyAss.targetProperty.displayName)!}')}[${getHtmlText('${(onetoManyAss.targetProperty.model.name)!}')}]
								</li>	
								<#assign j = j+1>
						</#list>
					 </#if>
				</ul></div>
		</div>	
			<input type="hidden" id="viewCode" value="${(view.code)!}" />
			<input type="hidden" id="viewType" value="${(view.type)!}" />
			<input type="hidden" id="modelType" value="${(view.assModel.dataType)!}" />
			<input type="hidden" id="delCellIds" name="delCellIds" />
			<input type="hidden" id="btDelCellIds" name="btDelCellIds" />
			<input type="hidden" id="delEventIds" name="delEventIds" />
			<input type="hidden" id="delValidateIds" name="delValidateIds" />
			
			<div id="main_form_tab" style="float:left;height:100%;width:81%;border: 1px solid #C6D4E1;">
				
					<div class="customquery_column" >
						<table  id="newInfoSetQueryTable_thead">
							<thead>
								<tr>
									<td style="width:5%" align="center"></td>
									<td style="width:10%;" align="center"></td>
									<td style="width:7%" align="center">${getText('foundation.advQuery.grade')}</td>
									<td style="width:20%" align="center">${getText('foundation.advQuery.condition')}</td>
									<td style="width:20%" align="center">${getText('foundation.advQuery.comparisonSymbol')}</td>
									<td style="width:25%" align="center">${getText('foundation.advQuery.value')}</td>
									<td style="width:7%" align="center">${getText('foundation.advQuery.grade')}</td>
									<td style="width:4%" align="center"></td>
								</tr>
							</thead>
						</table>
						<div class="etv-content" >
						<div id="NewlistSection" style="height:222px;overflow:auto;">
						<ul class="etv-nav">
						<li class="selected"></li>
						</ul>
						<table id="newInfoSetQueryTable">
							<thead style="background:#DFE8EE;">
								<tr>
									<td style="width:5%" align="center"></td>
									<td style="width:10%;" align="center"></td>
									<td style="width:7%" align="center"></td>
									<td style="width:20%" align="center"></td>
									<td style="width:20%" align="center"></td>
									<td style="width:25%" align="center"></td>
									<td style="width:7%" align="center"></td>
									<td style="width:4%" align="center"></td>
								</tr>
							</thead>
							<tbody>
								
							</tbody>
						</table>
					</div>
				</div>
			</div>
			<div  height="30%" style="border: 1px solid #C6D4E1;cursor: pointer;" onclick="changeCustomQueryResult();">
				<span style="margin-left:5px;font-weight:bold;color:#5C6C77;;">${getText('foundation.advQuery.conditionalPreview')}</span>
				<span class="customQuery_preview_result" ></span>
				<div class="customQuery_cond">
					<span></span>
				</div>
			</div>
		</div>		
	</div>
	<script type="text/javascript">
		var main_tab,listec;//全局变量
		$(function(){
		$('#form_select_element').tabs("#form_select_element div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			main_tab = new CUI.TabView("main_form_tab",{tabposition:'top', removable: true});//初始化主设计区域页签
			listec = new CUI.EntityConfList(
				'${(view.code)!}',
				{pageConfig:{colNum:<@s.property value="${(ev.config.layout.pageConfig.colNum)!6}" escape=false/>}},
				main_tab,
				'${(ev.code)!}',
				'${(ev.version)!0}',
				'${(ev.view.advQueryJson.code)!}',
				'${(ev.view.advQueryJson.version)!0}',
				{'singlSelect' : {'SELECT':'${getText('ec.view.select')}'},
				 'multiSelect' : {'CHECKBOX':'${getText('ec.view.checkbox')}', 'MULTSELECT':'${getText('ec.view.multselect')}'},
				 'objectSelect' : {'TEXTFIELD':'${getText('ec.view.textfield')}', 'SELECTCOMP':'${getText('ec.view.selectcomp')}'},
				 'normalSelect' : {<#if fastfieldtypes??><#list fastfieldtypes?keys as key>'${key}':'${getText('${fastfieldtypes[key]!}')}',</#list></#if>},
				'viewColumnTypes' : {<#if viewColumnTypes??><#list viewColumnTypes?keys as key>'${key}':'${getText('${viewColumnTypes[key]!}')}'<#if key_has_next>,</#if></#list></#if>},
				'viewFormats' :  {<#if viewFormats??><#list viewFormats?keys as key>'${key}':'${getText('${viewFormats[key]!}')}'<#if key_has_next>,</#if></#list></#if>}
				},
				{
				'equalExp' : {'equal':'${getText('ec.view.equal')}', 'unequal':'${getText('ec.view.unequal')}'},
				'likeExp' : {'equal':'${getText('ec.view.like')}'},
				'textExp' : {'equal':'${getText('ec.view.equal')}', 'like':'${getText('ec.view.like')}', 'llike':'${getText('ec.view.llike')}','rlike':'${getText('ec.view.rlike')}'}
				}
			);
		});
		$(function(){
			if(navigator.userAgent.indexOf("Chrome")>-1){
				$('#form_select_element li').draggable({
				revert : "invalid",
				helper : "clone"
				});
			}else{
				$('#form_select_element li').draggable({
				revert : "false",
				helper : "clone"
				});
			}
		});
	</script>
	</body>
	<script type="text/javascript">	
		//自动设置中间编辑区域的宽度
		function _init_size(){
			$('.etv-content').removeAttr('style');
			$("#main_form_tab").css('width','81%');
		}
		
		function showTableDialog(){
			if($('#66387035_button_2').attr('canClick')=="true"){
				dg.advQuery.showAdv();
			}
		}
		$(function(){
			if($('option','#fast_query_select').size() > 0) {
				$(".list-select").mSelect({type:'list'});
			}
			_init_size();
			$('body').keydown(function(e){
				if($('.ewc-dialog-blove','body').size() < 1) {
					if(e.keyCode == 46) {
						$('td,li','#main_design_container').each(
							function(index) {
							if($(this).hasClass('ui-selected')) {
								if($(this).parents().hasClass('form_design_ul')) {
									//listec.delCell();
								} 
								if($(this).parents().hasClass('operateButton_ul')) {
									if (confirm("${getText('ec.view.deleteBtnSure')}")) {
										$(this).remove();
									}
								}
								if($(this).parents().hasClass('list_design_ul')) {
									if (confirm("${getText('ec.view.deleteListPro')}")) {
										$(this).remove();
									}
								}
							}
						});
					}
					if(e.keyCode == 37) {
						$('td,li','#main_design_container').each(
							function(index) {
							if($(this).hasClass('ui-selected')) {
								if($(this).parents().hasClass('operateButton_ul')) {
									//listec.leftmove('buttonOperateDiv');
								}
								if($(this).parents().hasClass('list_design_ul')) {
									//listec.leftmove('listSection');
								}
							}
						});
					}
					if(e.keyCode == 39) {
						$('td,li','#main_design_container').each(
							function(index) {
							if($(this).hasClass('ui-selected')) {
								if($(this).parents().hasClass('operateButton_ul')) {
								//	listec.rightmove('buttonOperateDiv');
								}
								if($(this).parents().hasClass('list_design_ul')) {
								//	listec.rightmove('listSection');
								}
							}
						});
					}
				} 
			});
			var liArr = $("#dlg-div").find('li');
			liArr.each(function(index){
			    var objMe = $(this);
				var objId = $(objMe).attr('id');
			    $(this).unbind('click').bind('click',function(){
					$(this).removeClass('selected').addClass('selected');
			        liArr.each(function(ind){
			            if($(this).attr('id')!=objId ){
			                $(this).removeClass('selected');
			            }
			        });
			        if(objId === 'base_attr'){
			            $('#tab-common').css('display','block');
			            $('#div-event').css('display','none');
			            $('#tab-style').css('display','none');
			        }else if(objId === 'event_defined'){
			            $('#tab-common').css('display','none');
			            $('#div-event').css('display','block');
			            $('#tab-style').css('display','none');
			        }else if(objId === 'css_defined'){
			            $('#tab-common').css('display','none');
			            $('#div-event').css('display','none');
			            $('#tab-style').css('display','block');          
			        }
			    });
			});
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
			        }else if(objId === 'event_defined_public'){
			            $('#tab_common').css('display','none');
			            $('#tab_event').css('display','block');
			        }
			    });
			});
		});		
		setTimeout(function(){
			//listec._init();
			changeInput();
			$("#scrollbar").hide();
		},200);
		$(window).resize(function(){
			setTimeout(function(){
				_init_size();
			},10);		
		});
		function setScript(objArr){
			var code =objArr[0].scriptCode;
			CUI('#scriptCode').val(code);
		}
		function dataClassificTest(){
			return;
			//alert("${getText('ec.view.list.dcset')}${getText('foundation.modelname.radion1320110242279')}");
		}
		function changeInput(obj){
			var value;
			if(obj) {
				value = obj.value;
			} else {
				value = $('option:first', '#fast_query_select').attr('value');
			}
			var showFormat;
			$('option','#fast_query_select').each(function(){
				if($(this).attr('value') == value) {
					showFormat = $(this).attr("showFormat");
					return;
				}
			});
			if(showFormat == "SELECTCOMP") {
				$('#div_SELECTCOMP').show();
				$('#div_TEXT').hide();
				$('#div_DATE').hide();
				$('#div_SELECT').hide();
			}else if(showFormat == "SELECT" || showFormat == "CHECKBOX" || showFormat == "RADIO" || showFormat == "SYSTEMCODE" || showFormat == "ENUMERATE") {
				$('#div_SELECT').show();
				$('#div_TEXT').hide();
				$('#div_SELECTCOMP').hide();
				$('#div_DATE').hide();
			} else if (showFormat == 'Y' || showFormat == 'YM' || showFormat == 'YMD' || showFormat == 'YMD_H' || showFormat == 'YMD_HM' || showFormat == 'YMD_HMS') {
				$('#div_DATE').show();
				$('#div_TEXT').hide();
				$('#div_SELECTCOMP').hide();
				$('#div_SELECT').hide();
			} else if(showFormat != null && showFormat != undefined){
				$('#div_TEXT').show();
				$('#div_SELECTCOMP').hide();
				$('#div_DATE').hide();
				$('#div_SELECT').hide();
			}
		}
		
		function changePropType(obj){
			var type = parseInt($(obj).val());
			$('td.ui-selected', '#list_design_section').removeAttr("assoFlag");
			$('td.ui-selected', '#list_design_section').removeAttr("assoConfig");
			$('#config_custom_props_select_model').val('');
			$('#config_custom_props_select_model').removeAttr('modelcode');
			$('#config_custom_props_select_model').removeAttr('layrec');
			if (type == 1) {
				$('tr:visible,td:visible', '#listFieldDlg').removeClass('listFieldDlgInit').addClass('listFieldDlgInit');
				$('tr,td', '#listFieldDlg').hide();
				$('tr.config_relation_model_props,td.config_relation_model_props', '#listFieldDlg').show();
				$(obj).next().show();
			} else if (type == 0) {
				$(obj).next().hide();
				$('tr.config_custom_props,td.config_custom_props', "#listFieldDlg").hide();
				$('tr.listFieldDlgInit,td.listFieldDlgInit', "#listFieldDlg").show();
				$('tr.listFieldDlgInit,td.listFieldDlgInit', "#listFieldDlg").removeClass("listFieldDlgInit");
				$('#tr_config_custom_props_select_model').hide();
			} else if (type == 2) {
				$('tr:visible,td:visible', '#listFieldDlg').removeClass('listFieldDlgInit').addClass('listFieldDlgInit');
				$('tr,td', '#listFieldDlg').hide();
				$('span[id="config_relationModelProps"]').hide();
				$('tr.config_custom_props,td.config_custom_props', "#listFieldDlg").show();
			}
		}
			$(function(){
				ec.advQuery.query.viewCode='${(view.code)!}';
				ec.advQuery.query.modelName='${(view.assModel.modelName)!}';
				ec.advQuery.query.currModelCode='${(view.assModel.code)!}';
				ec.advQuery.query._init_adv_droppable();
				$('#frontscroller').css('display','none');
				$('#backscroller').css('display','none');
			});
		CUI.ns("ec.config.newAdvQuery");
		ec.config.newAdvQuery._rpFlag="${rpFlag?string("true", "false")}";
		ec.config.newAdvQuery._createCookieInfoElement= function(){
			var str="${rpFlag?string("true", "false")}";
			var _cookieInnerHTML = '<table id="cookieInfoTable" cellpadding="0" cellspacing="0" border="0" align="center" style=";width:80%;">';
			if(str=="true"){
			_cookieInnerHTML += '<tr style="';
			}else{
			_cookieInnerHTML += '<tr style="display:none;';
			}
			_cookieInnerHTML +='height:20px;"><td class="edit-table-symbol" align="right" style="padding-top:10px;text-align: right;width:40px;padding-right:3px;" > <label style="width:100%;color:#B30303;" value="'+"${getHtmlText('foundation.advQuery.planType')}"+'">'+"${getHtmlText('foundation.advQuery.planType')}"+'</label> </td><td nullable=false class="edit-table-content" style="padding-top:10px;text-align: left;width:200px;"><div ><input name="setAdvQuery" style=";" value="0" type="radio" checked="true" >'+"${getHtmlText('foundation.advQuery.planPersonal')}"+'</input>';
			_cookieInnerHTML += '<input name="setAdvQuery" style=";" value="1" type="radio" >'+"${getHtmlText('foundation.advQuery.planPublic')}"+'</input></div></td></tr>';
			
			_cookieInnerHTML += '<tr style="height:20px;"><td class="edit-table-symbol" align="right" style="padding-top:10px;text-align: right;width:40px;padding-right:3px;" ><label style="width:100%;color:#B30303;" value="'+"${getHtmlText('foundation.otherRestrict.title')}"+'">'+"${getHtmlText('foundation.otherRestrict.title')}"+'</label></td><td nullable=false class="edit-table-content" style="padding-top:10px;text-align: left;width:200px;"><div class="fix-input"><input name="title" style=";" value="" type="text" class="cui-noborder-input " /></div></td></tr>';
			_cookieInnerHTML += '<tr style="height:84px;"><td class="edit-table-symbol" align="right" style="padding-top:10px;text-align: right;width:40px;vertical-align: top;padding-right:3px;" ><label style="width:100%;" value="'+"${getHtmlText('foundation.calendar.memo')}"+'">'+"${getHtmlText('foundation.calendar.memo')}"+'</label></td><td nullable=false class="edit-table-content" style="padding-top:10px;text-align: left;vertical-align: top;width:200px;"><div class="fix-input"><textarea name="memo" style="width:250px;height:80px;border:none;" rows="5"></textarea></div></td></tr>';
			_cookieInnerHTML += '</table>';
			YUD.get('cookieInfoDiv').innerHTML = _cookieInnerHTML;
		}
		
		ec.config.newAdvQuery.showScheme = function (){
			YUD.get('cookieInfoDiv').innerHTML =ec.advQuery.query._getSavedCondition();
		}
		ec.config.newAdvQuery.initdrag =function(){
			if(navigator.userAgent.indexOf("Chrome")>-1){
				$('#form_select_element li').draggable({
				revert : "invalid",
				helper : "clone"
				});
			}else{
				$('#form_select_element li').draggable({
				revert : "false",
				helper : "clone"
				});
			}
		};
		function  publicPlanShow(){
			if($("#public_plan_content").css('display') == 'block'){
				$('.plan-title.public').css('background','url("/bap/static/css/icon_adv.png") 0 -121px no-repeat');;
				$("#public_plan_content").css('display','none');
			}else{
				$('.plan-title.public').css('background','url("/bap/static/css/icon_adv.png") 0 -37px no-repeat');
				$("#public_plan_content").css('display','block');
			}
		}
		function  personalPlanShow(){
			if($("#personal_plan_content").css('display') == 'block'){
				$('.plan-title.personal').css('background','url("/bap/static/css/icon_adv.png") 0 -121px no-repeat');;
				$("#personal_plan_content").css('display','none');
			}else{
				$('.plan-title.personal').css('background','url("/bap/static/css/icon_adv.png") 0 -37px no-repeat');
				$("#personal_plan_content").css('display','block');
			}
		}
		function removePlan(obj){
			if(!confirm('${getText("common.button.suredelete")}')) return false;
			$(obj).parent().remove();
			ec.advQuery.query._deleteCond(obj.id);
			ec.advQuery.query._dialog.close();
		}
	</script>
</html>