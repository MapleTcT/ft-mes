<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>$(getText('ec.view.editview'))</title>
	<@head/>
	<link rel="stylesheet" type="text/css" href="/bap/static/res/main.css" />
	<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/style.css" />
	<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/view.css" />
	<link rel="stylesheet" type="text/css" href="/bap/static/jquery/assets/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
	<script type="text/javascript" src="/bap/static/core/jquery.js"></script>
	<script type="text/javascript" src="/bap/static/jquery/jquery.tools.js"></script>
	<script type="text/javascript" src="/bap/static/jquery/jquery.ui.js"></script>
	<script type="text/javascript" src="/bap/static/res/main.js"></script>
	<script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_common.js"></script>
	<script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_edit.js"></script>
	<@loadpanel />
	<style type="text/css">
		.grid-s4m0 .main-wrap {width: 100%;float:left;height:100%;overflow:hidden;}
		.grid-s4m0 .col-sub { margin-left:2px;margin-top:8px;width: 140px; height:98%;float:left;background-color:white;border:1px solid #3C7FB1}
		.grid-s4m0 .col-operate{ border:1px solid #a2a2a2;background-color: #d4d4d4;width: 35px; height:98%;float:right;margin-top:8px;overflow:hidden;}
		#form_select_elements{height:98%}
		#form_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#form_select_elements h2.current {cursor:default;}
		#form_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#form_select_elements .accordion_pane li {font-size:12px;color:#999;cursor:pointer;line-height:18px;z-index:100;}	
		#main_form_tab_operate li{display:block;}
		.etv-navset .etv-nav li.selected em.etv-nav-em{background-color:##E3F1F9!important}
		#main_form_tab .etv-content{background-color:#E3F1F9!important}
		#main_design_container{padding:2px 2px 0 0;}
		.form_title{height:30px;width:99%;background-color:#d4d4d4;margin:6px 0px 0px 5px}
		.line-icon{width:31px;margin:2px 3px 2px 2px; border-color:#d4d4d4;overflow:hidden;}
		#form_area{position: relative;width:100%;height:95%;}
		#main_form_tab_operate{z-index: 1000;}
		#main_form_tab_operate li{width:30px;height:30px;margin:6px;cursor:pointer;display:block;overflow:hidden;}
		#main_form_tab_operate .p-set-icon{margin:10px 10px 0px 10px;background:url('/bap/static/css/sprite_20120525.png') 0 -1631px no-repeat;}
		#main_form_tab_operate .s-add-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1511px no-repeat;}
		#main_form_tab_operate .s-del-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1751px no-repeat;}
		#main_form_tab_operate .s-set-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1841px no-repeat;}
		#main_form_tab_operate .l-add-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1541px no-repeat;}
		#main_form_tab_operate .l-del-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1781px no-repeat;}
		#main_form_tab_operate .c-pro-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1691px no-repeat;}
		#main_form_tab_operate .c-del-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1721px no-repeat;}
		#main_form_tab_operate .c-split-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1661px no-repeat;}
		#main_form_tab_operate .t-add-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1571px no-repeat;}
		#main_form_tab_operate .t-mod-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1601px no-repeat;}
		#main_form_tab_operate .t-del-icon{background:url('/bap/static/css/sprite_20120525.png') 0 -1811px no-repeat;}
		
		#main_form_tab .etv-content{min-height:500px;}
		
		.form_design_ul_li{border:#CCC 1px dashed;float:left; height:30px;margin:1px;margin-right:10px;position: absolute;}
		.form_design_ul_li dt{float:left;width:100px;text-align: right;padding-right:5px;padding-top:2px;line-height:25px;}
		.form_design_ul_li dd{float:left;text-align: right;padding-top:5px;width:100%;margin-top:3px}
		.form_design_ul_li dd textarea,.form_design_ul_li dd input,.form_design_ul_li dd select{width:100%;height:20px;}
		.form_design_ul_li dd textarea{border:green 1px solid;margin:0;}
		.form_design_ul_li dd input.cui-search-click{width:24px!important;}
		.form_design_ul_li dd input.cui-calpick{width:22px!important;}
		.form_design_ul_li dd input[type='checkbox']{width:20px;height:20px;}
		.form_design_ul_li dd input[type='radio']{width:20px;height:20px;}
		
		.form_design_ul_li div{display:none;position: absolute;left:5px;top:2px;cursor:move;width:20px;height:20px;background: url("/bap/static/ec/images/arrow_switch.png") center center no-repeat;}
		.div-selected{}
		.ui-selectee{}
		.ui-selecting { background: #FECA40; }
		.ui-selected { background: #F39814;}
		.se-selected { background: #F39814;}
		.BlockInfoDiv {margin:2px;padding:4px;border-top:1px dashed #cccccc;}
		.editeventli {width:175px;text-align: right;}
		.datagird_th_td0 {width:5%;height:22px;overflow:hidden;text-align:center;padding:0 3px;background:url("/bap/static/css/sprite_20120525.png") repeat-x scroll 0 -1340px transparent;border-right:1px solid #FFFFFF}
		.datagird_th_td1 {width:95%;height:22px;overflow:hidden;text-align:center;padding:0 3px;background:url("/bap/static/css/sprite_20120525.png") repeat-x scroll 0 -1340px transparent;border-right:1px solid #FFFFFF}
		.datagird_tb_td0{width:5%;height:22px;text-align:center;overflow:hidden;background-color:#E7E7E7;border-top:1px solid #BABABA;border-left:1px solid #BABABA;border-bottom:1px solid #BABABA}
		.datagird_tb_td1{width:95%;height:22px;overflow:hidden;background-color:white;border:1px solid #BABABA;}
	</style>
	<script type="text/javascript">
		<#-- 全局变量 -->
		var main_tab,ec;
		(function($){
			$(function(){
				<#-- 初始化手风琴下拉 -->
				$('#form_select_elements').tabs("#form_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});
				<#-- 初始化主设计区域页签 -->
				main_tab = new CUI.TabView("main_form_tab",{tabposition:'top'});
				<#-- 初始化主操作方法 -->
				ec = new CUI.EntityConfEdit(
					${(view.code)!},
					<#if (ev.config)?? && ev.config != ''>
						<@s.property value="ev.config" escape=false/>
					<#else>
						<#noparse>{pageConfig:{colNum:6}}</#noparse>
					</#if>,
					main_tab,
					'${(ev.code)!}',
					'${(ev.version)!}'
					);
			});
		})(jQuery);
	</script>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2">
	<@errorbar id="workbenchErrorBar" offsetY=83 />
		<div class="layout grid-s4m0"> 	
		<div class="col-sub">
			<#-- 左侧 -->
			<div id="form_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:80%">
					<ul class="main_properties_container">
						<#list view.assModel.properties as p>
							<#if (p.name) != "extraCol" && (p.name) != "tableInfoId">
								<li source='main' name='${view.assModel.modelName}.${p.name}' columnType='${(p.type)!}' columnLong='${(p.maxLength)!}' nullable='${(p.nullable)?string('true','false')}' fillContent='${(p.fillcontent)!}'>
								<#if map?? && map.get((p.code))>
									<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='ec.showAssProperty("${(p.code)!}","${(view.assModel.code)!}",this,"${(view.assModel.modelName)!}.${(p.name)!}","${(p.name)!}")'></img>
								</#if>
								${getHtmlText('${(p.displayName)}')}
								</li>
							</#if>
						</#list>
					
					<#--
						<#if (Request.view)??>
							<#assign view = (Request.view)>
							<#if (view.assModel.properties)??>
							<#assign properties = (view.assModel.properties)>
							<#if properties??>
								<#if (Request.originalAssociatedInfos)??>
								<#assign map = Request.originalAssociatedInfos>
								<#list properties as p>
									<#if (p.name) != "extraCol" && (p.name) != "tableInfoId">
										<li source='main' name='${(view.mainModelName) + "." + (p.name)}' columnType='${(p.type)}' columnLong='${(p.maxLength)}' nullable='${(p.nullable)}' fillContent='${(p.fillcontent)}'>
										<#if map?? && map.get((p.code))>
											<img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='ec.showAssProperty("${(p.code)}","${view.mainModel.code}",this,"${(view.mainModelName)+"."+(p.name)}","${(p.name)}")'></img>
										</#if>
										${(p.displayName)}
										</li>
									</#if>	
								</#list>
								</#if>
							</#if>
							</#if>
						</#if>
					-->
					</ul>
				</div>
				<h2>${getHtmlText('ec.view.formelement')}</h2>
				<div class="accordion_pane"><ul id="form_elements_container">
				<#if fieldTypes??>
				<#list fieldTypes?keys as key>
					<li source="el" fieldType="${key}">${fieldTypes[value]}</li>
				</#list>
				</#if>
				</ul></div>
				<h2>${getHtmlText('ec.view.one2oneattr')}</h2>
				<div source="one" class="accordion_pane"><ul class="onetoone_properties_container"></ul></div>
				<h2>${getHtmlText('ec.view.one2manyattr')}</h2>
				<div source="many" class="accordion_pane"><ul class="onetomany_properties_container"></ul></div>
			</div>
		</div>	
		<div class="col-main"> 			
			<div class="main-wrap" id="main_design_container">
				<div>
					<div class="form_title"></div>
				</div>
				<div id="form_area">
					<div id="main_form_tab" class="etv-navset">
						<ul class="etv-nav">
							<#if ev?? && !(ev.config)?has_content>
							<li class="selected">${getHtmlText('ec.view.commoninfo')}</li>
							<#else>
								<#if (ev.configMap)??>
								<#list (ev.configMap)['tabs'] as tab1>
									<li>${(tab1.name)!}</li>
								</#list>
								</#if>
							</#if>							
							<#if (view.project.workflowEnabled)??><li fixed="true">${getHtmlText('ec.view.procinfo')}</li></#if>
						</ul>
						<div style="background-color:#e3f1f9;" class="etv-content" >
							<#if ev?? && !(ev.config)?has_content>
								<div><ul class="form_design_ul se-selected" onclick="ec.selectsection(this)" colNum="6"></ul></div>
							<#else>
							<#if (ev.configMap)??>
								<#list (ev.configMap)['tabs'] as tab>
								<div class="div-selected">
									<#list tab['sections'] as secatt>
										<ul class="form_design_ul"  onclick="ec.selectsection(this)" colwidth="${(secatt.pageConfig.colwidth)!}" colNum="${(secatt.pageConfig.colNum)!6}" name="${(secatt.name)!}" isborder="${(secatt.isborder)!1}">
											<#if (secatt.content)['form']??>
											<#list (secatt.content)['form'] as element>
											<#if (element.element)??>
											<li class="form_design_ul_li"  onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" complex="${(element.element.complex)?string("true","false")}" nullable="${(element.element.nullable)?string('true','false')}" isgroup="${(element.element.isgroup)?string('true','false')}" columnLong="${(element.element.columnLong)!}" isreadonly="${(element.element.readonly)?string('true','false')}" checkname="${(element.element.checkname)!}" fieldType="${(element.element.fieldType)!}"  fillType="${(element.element.fillType)!}" fillContent="${(element.element.fillContent)!}" sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${(element.callbackbody)!}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${(element.funcbody)!}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${(element.cssstyle)!''}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" 
											fill='{<#if (element.element.fill)??><#list (element.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>${fe}:<#if (element.element.fillType) == 4 && fe == 'fillContent'>{<#list (element.element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>${ne}:${(element.element.fill.fillContent)[ne]}</#list>}<#else><#if fe != 'fillType'>"</#if>${(element.element.fill)[fe]}<#if fe != 'fillType'>"</#if></#if></#list></#if>}' 
											<#if (element.element.fieldType) == 'DATAGRID'>tagModelCode="${(element.element.tagModelCode)!}" DataGridCode="${(element.element.DataGridCode)!}" tagtablename="${(element.element.tagtablename)!}" hideKey="${(element.element.hideKey)!}"</#if>>
												<#if (element.element.fieldType) == 'LABEL'>
													<dd>${(element.name)!}</dd>
												<#else>
													<dd>
														<#if  (element.element.fieldType)??>
														<#if (element.element.fieldType) == 'TEXTAREA'>
														<textarea style="color:gray">${(element.element.checkname)!''}</textarea>
														<#elseif (element.element.fieldType) == 'DATE' || (element.element.fieldType) == 'DATETIME'>
														<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/><input type='button' class='cui-calpick' />
														<#elseif (element.element.fieldType) == 'TEXTFIELD'>
														<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/>
														<#elseif (element.element.fieldType) == 'SELECTCOMP'>
														<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.fieldType) == 'SELECT'>
														<select style="color:gray"><option>${(element.element.checkname)!''}</option></select>
														<#elseif (element.element.fieldType) == 'RADIO'>
															<input type="radio"/><font color="gray">${(element.element.checkname)!''}</font>
														<#elseif (element.element.fieldType) == 'CHECKBOX'>
															<input type="checkbox"/><font color="gray">${(element.element.checkname)!''}</font>
														<#elseif (element.element.fieldType) == 'DATAGRID'>
															<font style="margin-right: 10000px;font-weight:bolder">${getHtmlText('ec.view.quitrecord')}</font><table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td0">${getHtmlText('ec.view.num')}</td><td class="datagird_th_td1">${getHtmlText('ec.view.modelfield')}</td></tr></thead><tbody><tr><td class="datagird_tb_td0">1</td><td class="datagird_tb_td1"></td></tr></tbody></table>
														</#if>
														</#if>
													</dd>
												</#if>
												<div></div>
											</li>
											<#else>
												<li class="form_design_ul_li"><dd></dd><div></div></li>
											</#if>
											</#list>
											</#if>
										</ul>
									
									</#list>	
								</div>
							</#list>
							</#if>
							</#if>
							<#if (view.project.workflowEnabled)??><div></div></#if>
						</div>						
					</div>
				</div>
			</div>
		</div>		
		<div class="col-operate">
			<div>
				<ul id="main_form_tab_operate" >
					<li class="p-set-icon" onclick="ec.cellPageProperty()" title="${getHtmlText('ec.view.pageProperty')}"></li>
					<li><hr class="line-icon"></hr></li>
					<li class="s-add-icon" onclick="ec.addSection()" title="${getHtmlText('ec.view.addSection')}"></li>
					<li class="s-del-icon" onclick="ec.delSection()" title="${getHtmlText('ec.view.delSection')}" ></li>
					<li class="s-set-icon" onclick="ec.sectionProperty()" title="${getHtmlText('ec.view.sectionattr')}"></li>
					<li><hr class="line-icon"></hr></li>
					<li class="l-add-icon" onclick="ec.addCell(1)" title="${getHtmlText('ec.view.addline')}"></li>
					<li class="l-del-icon" onclick="ec.delRow()" title="${getHtmlText('ec.view.deleteline')}"></li>
					<li><hr class="line-icon"></hr></li>
					<li class="c-split-icon" onclick="ec.splitCell()" title="${getHtmlText('ec.view.splitcell')}"></li>
					<li class="c-del-icon" onclick="ec.delCell()" title="${getHtmlText('ec.view.delcell')}"></li>
					<li class="c-pro-icon" onclick="ec.cellProperty()" title="${getHtmlText('ec.view.cellproperty')}"></li>
					<li><hr class="line-icon"></hr></li>
					<li class="t-add-icon" onclick="ec.tabAdd()" title="${getHtmlText('ec.view.addtab')}"></li>
					<li class="t-mod-icon" onclick="ec.tabModify()" title=${getHtmlText('ec.view.edittab')}"></li>
					<li class="t-del-icon" onclick="ec.tabDelete(event)" title="${getHtmlText('ec.view.deltab')}"></li>
				</ul>
			</div>
		</div>
	</div>	
	<div id="design-button" style="height:30px;width:100%;overflow:hidden;clear:both;text-align:right;margin-top:20px">
		<button class="Dialog_button btn_pointer" onclick="ec.save()" type="button">${getHtmlText('common.button.save')}</button>&#160;&#160;&#160;&#160;&#160;
		<button class="Dialog_button btn_pointer" type="button">${getHtmlText('common.button.saveexit')}</button>&#160;&#160;&#160;&#160;&#160;
		<button class="Dialog_button btn_pointer" onclick="ec.publish()" type="button">${getHtmlText('ec.view.publishview')}</button>&#160;&#160;&#160;&#160;&#160;
		<button class="Dialog_button btn_pointer" onclick="ec.preview()" type="button">${getHtmlText('ec.view.preview')}</button>		
	</div>
	
	<div id="form_set_item_property_dlg" style="display:none">
		<@errorbar id="EditDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.name')}</td>
				<td class="co" width="30%"><input type="text" id="form_property_name" class="cui-edit-field" style="width:100%" /></td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co" width="30%"><input type="text" id="form_property_show_name" class="cui-edit-field" style="width:100%" /></td>
			</tr>
			<tr><td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.type')}</td>
				<td class="co" width="30%"><select id="form_property_field_type" class="cui-edit-field" style="width:100%">
					<#if fieldTypes??>
					<#list fieldTypes?keys as key>
					<option value="${key}">${fieldTypes[key]}</option>
					</#list>
					</#if>
				</select></td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="iscomplexLable">${getHtmlText('ec.view.iscomplexlable')}</label></td>
				<td class="co" width="30%">
					<input type="checkbox" id="form_property_is_complex" />
					<select id="selectcomp_type"  class="cui-edit-field" style="display:none;width:100%">
						<option value="staff">${getText('ec.view.staff')}</option>
						<option value="department">${getText('ec.common.deptName')}</option>
						<option value="position">${getText('ec.view.positon')}</option>
						<option value="other">${getText('ec.view.else')}</option>
					</select>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isreadOnlyLable">${getHtmlText('ec.view.isreadonly')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_is_readonly" /></td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isnullableLable">${getHtmlText('ec.view.isnullable')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_is_nullable" /></td>
			</tr>
			<tr id="form_tr_isgroup">
				<td class="la" width="20%" align="right" padding-right="7px"><label id="isGroupLable">${getHtmlText('ec.view.isgroup')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_is_group" /></td>
			</tr>
			<tr>
				<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.fillvalue')}</td>
				<td class="co" colspan="3"><input type="text" id="form_property_field_val" class="cui-edit-field" style="width:100%" /><input onclick="ec.selectFillVal()" type='button' class='cui-search-click' /></td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.customevent')}</td>
				<td class="co" colspan="3">
					<div style="float:left;width:45%;height:60px;margin-bottom:5px;border:1px solid #0059B3;overflow:auto;">
						<ul id="event_ul">
							<li class="editeventli"><a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline" onclick="ec.editEvent('onchange')">onchange</a>：<input type="checkbox" id="li_property_event_onchange" value=""/>&#160;&#160;&#160;</li><li class="editeventli"><a title="${getText('ec.view.editevent')}" style="text-decoration:underline" href="#" onclick="ec.editEvent('onpropertychange')">onpropertychange</a>：<input type="checkbox" id="li_property_event_onpropertychange" value=""/>&#160;&#160;&#160;</li>
							<li class="editeventli"><a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline" onclick="ec.editEvent('onclick')">onclick</a>：<input type="checkbox" id="li_property_event_onclick" value=""/>&#160;&#160;&#160;</li><li class="editeventli"><a title="${getText('ec.view.editevent')}" style="text-decoration:underline" href="#" onclick="ec.editEvent('ondbclick')">ondbclick</a>：<input type="checkbox" id="li_property_event_ondbclick" value=""/>&#160;&#160;&#160;</li>
							<li class="editeventli"><a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline" onclick="ec.editEvent('onblur')">onblur</a>：<input type="checkbox" id="li_property_event_onblur"  value=""/>&#160;&#160;&#160;</li>
						</ul>
						<li class="editeventli"><a title="${getText('ec.view.editevent')}" style="text-decoration:underline" href="#" onclick="ec.editEvent('callback')">callback</a>：<input type="checkbox" id="li_property_event_callback" value=""/>&#160;&#160;&#160;</li>
					</div>
					<!-- <textarea  id="form_property_field_event" class="cui-edit-textarea" style="width:100%;height:120px;"></textarea> -->
					<div  style="float:right;width:50%;height:60px;margin-right:5px;padding-right:10px">
						${getHtmlText('ec.view.customeventtype')}<br/>
						<input type="text" id="editEventtype" class="cui-edit-field" style="width:150px;margin-top:10px" value=""/>&#160;&#160;&#160;&#160;&#160;&#160;<img onclick="ec.addnewevent()" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</div>
					<br/>					
					<textarea  id="editEventarea" class="cui-edit-textarea" style="width:100%;height:75px;"></textarea>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.comstyle')}</td>
				<td class="co" colspan="3"><textarea id="form_property_field_style" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea></td>
			</tr>
		</table>
	</div>
	<div id="form_set_select_val" style="display:none;">
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.type')}</td>
				<td class="co" width="30%">
					<select id="form_set_select_val_type" onchange="ec.changeFillValType(this)" class="cui-edit-field" style="width:100%">
						<option value="1">${getText('ec.view.initvalue')}</option>
						<!--<option value="2">固有值</option>-->
						<option value="SYSTEMCODE">${getText('ec.view.systemcode')}</option>
							<option value="ENUMERATE">${getText('ec.property.enum')}</option>
					</select>
				</td>
				<td class="la" width="20%" align="right" padding-right="10px"></td>
				<td class="co" width="30%"></td>
			</tr>
			<tr>
				<td class="la" align="right" id="form_set_select_val_sou_name"></td>
				<td class="co" colspan="3" id="form_set_select_val_sou"></td>
			</tr>
		</table>
	</div>
	<div id="page_setting_dlg" style="display:none;">
 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
				<td class="co" width="30%">${getHtmlText('ec.view.everyline')} <input type="text" id="layout_col_num" class="cui-edit-field" style="width:50px" /> ${getHtmlText('ec.view.colum')}</td>
				<td class="la" width="20%" align="right" padding-right="10px"></td>
				<td class="co" width="30%"></td>
			</tr>
			
			
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.pageload')}<br/>(onLoad)&#160;&#160;&#160;</td>
				<td class="co" colspan="3"><textarea id="onloadevent" class="cui-edit-textarea" style="width:100%;height:100px;"></textarea></td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.saveevent')}<br/>(onSave)&#160;&#160;&#160;</td>
				<td class="co" colspan="3"><textarea id="onsaveevent" class="cui-edit-textarea" style="width:100%;height:100px;"></textarea></td>
			</tr>
		</table>
 	</div>
 	<div id="section_setting_dlg" style="display:none;">
 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
				<td class="co" width="90%">${getHtmlText('ec.view.everyline')} <input type="text" id="section_num" class="cui-edit-field" style="width:50px"/> ${getHtmlText('ec.view.colum')}</td>
			
			</tr>
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.sectionname')}</td>
				<td class="co" width="90%"><input type="text" id="section_name" class="cui-edit-field" style="width:90px" /></td>
			</tr>
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.isborder')}</td>
				<td class="co" width="90%"><input type="checkbox" id="section_border"/></td>
				
			</tr>
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.colwidth')}</td>
				<td class="co" colspan="3">
					<div id="editcolwidthdiv">
						1、<input type="text" id="colwidth1" class="cui-edit-field" style="width:20px" value="13"/>
						2、<input type="text" id="colwidth2" class="cui-edit-field" style="width:20px" value="20"/>
						3、<input type="text" id="colwidth3" class="cui-edit-field" style="width:20px" value="13"/>
						4、<input type="text" id="colwidth4" class="cui-edit-field" style="width:20px" value="20"/>
						5、<input type="text" id="colwidth5" class="cui-edit-field" style="width:20px" value="13"/>
						6、<input type="text" id="colwidth6" class="cui-edit-field" style="width:20px" value="20"/>
					</div>
				</td>
			</tr>
		</table>
 	</div>
 	<div id="datagrid_setting_dlg" style="display:none;">
 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="40%" align="right" padding-right="5px">${getHtmlText('ec.view.choicemodel')}</td>
				<td class="co" width="60%">				
					<#--<@s.select id="datagrid_model_type" list="${Request.tomanyModelInfos!}" listKey="id" listValue="name" cssClass="cui-edit-field" cssStyle="width:100%"></@s.select>-->
				</td>
			</tr>
		</table>
 	</div>
 	<div id="datagrid_property_dlg" style="display:none;">
 		<iframe id="datagird_property_frame" width="100%" height="100%" SCROLLING="no" MARGINHEIGHT="0" MARGINWIDTH="0" frameborder="0" border="0" framespacing="0"></iframe>
 	</div>
</body>
<script type="text/javascript">	
	//自动设置中间编辑区域的宽度
	function _init_size(){
		$('.col-main').width($(window).width()-202);
	}
	$(function(){
		_init_size();
	});
	$(window).resize(function(){
		_init_size();				
	});
	setTimeout(function(){
		$('.etv-nav-em').css({'background-color':'#E3F1F9'});
	},100);
</script>
</html>