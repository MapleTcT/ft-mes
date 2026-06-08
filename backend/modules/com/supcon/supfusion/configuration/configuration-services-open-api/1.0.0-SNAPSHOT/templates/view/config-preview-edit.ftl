	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<title>${getText('${(view.assModel.name)!}')}-${(view.displayName)!}-${getText('ec.project.view_setting')}-<#if view.type == 'VIEW'>${getText('ec.view.view')}<#else>${getText('ec.view.edit')} </#if></title>
		<@head/>
		<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/style.css" />
		<link rel="stylesheet" type="text/css" href="/bap/static/ec/css/view.css" />
		<script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_common.js"></script>
		<script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_edit.js"></script>
		<@loadpanel />
		<style>
		#enumerate_content_table td {
			vertical-align: middle;
			text-align: center;
			height : 20px;
		}
		#enumerate_content_table thead td {
			background: url('/bap/static/css/sprite_20120525.png') 0 -1340px repeat-x;
		}
		</style>
		<style type="text/css">
			.grid-s4m0 .main-wrap {width: 100%;float:left;height:100%;overflow:hidden;background-color:#93BFD8;border:1px dashed #E6E6E6;}
			.grid-s4m0 .col-sub { margin-left:2px;margin-top:8px;width: 140px; float:left;background-color:white;border:1px solid #3C7FB1}
			.grid-s4m0 .col-operate{ margin-right:3px;border:1px solid #a2a2a2;background-color: #d4d4d4;width: 80px;float:right;margin-top:8px;overflow:hidden;}
			.grid-s4m0 .col-top{width: 100%;height:30px;background-color:#D4D8DB;border-bottom:1px solid #ADACAB;}
			#form_select_elements{height:98%}
			#form_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
			#form_select_elements h2.current {cursor:default;}
			#form_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
			#form_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
			#form_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}	
			#main_form_tab_operate li{display:block;}
			#main_form_tab .etv-content{background-color:#F3F3F3!important}
			#main_form_tab .etv-navset{border:1px solid #2B82B2;}
			#main_design_container{padding:2px 2px 0 0;}
			.form_title{height:30px;width:99%;background-color:#d4d4d4;margin:6px 0px 0px 5px}
			.line-icon{width:70px;margin:7px 3px 2px 2px; border-color:#d4d4d4;overflow:hidden;}
			#form_area{position: relative;width:100%;height:95%;}
			#main_form_tab_operate{z-index: 1000;}
			#main_form_tab_operate li{margin-left:3px;width:80px;height:25px;cursor:pointer;display:block;overflow:hidden;}
			#editcolwidthdiv input {margin-right:5px;}
			
			
			#main_form_tab .etv-content{min-height:500px;}
			
			.form_design_ul_li{border:#CCC 1px dashed;float:left; height:30px;margin:1px;margin-right:10px;position: absolute;}
			.form_design_ul_li dt{float:left;width:100px;text-align: right;padding-right:5px;padding-top:2px;line-height:25px;}
			.form_design_ul_li dd{float:left;text-align: right;padding-top:5px;width:100%;margin-top:3px}
			.form_design_ul_li dd textarea,.form_design_ul_li dd input,.form_design_ul_li dd select{width:99%;height:20px;}
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
			<#assign defaultColNum = 6>
			<#if view.openType == 'dialog'>
				<#assign defaultColNum = 4>
			</#if>
			<#-- 全局变量 -->
			var main_tab,ec;
			var _directAssosJson = ${(multSelectInfo.directAssosJson)!'{}'};
			var _indirectAssosJson = ${(multSelectInfo.indirectAssosJson)!'{}'};
			(function($){
				$(function(){
					<#-- 初始化手风琴下拉 -->
					$('#form_select_elements').tabs("#form_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});
					<#-- 初始化主设计区域页签 -->
					main_tab = new CUI.TabView("main_form_tab",{tabposition:'top', removable: true});
					<#-- 初始化主操作方法 -->
					ec = new CUI.EntityConfEdit(
						'${(view.code)!}',
						<#if (ev.config)?? && ev.config != ''>
							<@s.property value="ev.config" escape=false/>
						<#else>
							{pageConfig:{colNum:${defaultColNum}}}
						</#if>,
						main_tab,
						'${(ev.code)!}',
						'${(ev.version)!}',
						'${(isProj)?string!}',
						{'singlSelect' : {'SELECT':'${getText('ec.view.select')}', 'RADIO':'${getText('ec.view.radio')}'},
						 'multiSelect' : {'CHECKBOX':'${getText('ec.view.checkbox')}', 'MULTSELECT':'${getText('ec.view.mulselect')}'},
						 'objectSelect' : {'TEXTFIELD':'${getText('ec.view.textfield')}','TEXTAREA':'${getText('ec.view.textarea')}', 'DATE':'${getText('ec.view.date')}', 'DATETIME':'${getText('ec.view.datetime')}', 'SELECTCOMP':'${getText('ec.view.selectcomp')}'},
						 'normalSelect' : {<#if fieldTypes??><#list fieldTypes?keys as key>'${key}':'${fieldTypes[key]}'<#if key_has_next>,</#if></#list></#if>}
						},
						_directAssosJson,
						_indirectAssosJson
					);
					if($('.etv-content li').length == 0) {
						ec.addCell(1);
					}
				});
			})(jQuery);
		</script>
	</head>
	<body class="ec-config-page" style="background-color:#f2f2f2">
		<@errorbar id="workbenchErrorBar" offsetY=83 />
			<div class="layout grid-s4m0"> 	
			<div class="col-top">
				<div><h3 style="color:#3A70AA;padding-top:7px;padding-left:15px;">${(view.assModel.entity.entityName)!}-<#if view.type == 'VIEW'>${getHtmlText('ec.view.view')}<#else>${getHtmlText('ec.view.edit')}</#if>${getHtmlText('ec.project.view_setting')}</h3></div>
			</div>
			<div class="col-sub">
				<#-- 左侧 -->
				<div id="form_select_elements">
					<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
					<div class="accordion_pane" style="display:block;overflow:auto;height:91%">
						<ul class="main_properties_container">
							<#list view.assModel.properties as p>
								<#if (p.name) != "extraCol" && (p.name) != "tableInfoId">
									<li source='main' name='${view.assModel.modelName?uncap_first}.${p.name}' namekey="${(p.displayName)!}" columnType='${(p.type)!}' columnLong='${(p.maxLength)!}' decimalNum='${(p.decimalNum)!}' nullable='${(p.nullable)?string('true','false')}' multable='${(p.multable!false)?string('true','false')}' fillContent='${(p.fillcontent)!}'
									<#if (view.assModel.associatedInfos)??>
										<#assign assFlag = false>
										<#list (view.assModel.associatedInfos) as associatedInfo>
											<#if (associatedInfo.type)?has_content && ((associatedInfo.type) == 1 || (associatedInfo.type) == 2) && (associatedInfo.originalProperty.code)?? && (associatedInfo.originalProperty.code) == (p.code)>
											<#assign assFlag = true>
											modelcode = '${(associatedInfo.targetProperty.model.code)!}'><img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='ec.showAssProperty("${(p.code)!}","${(associatedInfo.targetProperty.model.code)!}",this,"${(view.assModel.modelName?uncap_first)!}.${(p.name)!}","${(p.name)!}")'></img>
											</#if>
										</#list>
										<#if assFlag?? && !assFlag>
										>
										</#if>
									<#else>
									>
									</#if>
									${getHtmlText('${(p.displayName)}')}
									</li>
								</#if>
							</#list>
						</ul>
					</div>
					<h2>${getHtmlText('ec.view.formelement')}</h2>
					<div class="accordion_pane"><ul id="form_elements_container">
					<#if fieldTypes??>
					<#list fieldTypes?keys as key>
						<#if key == 'TEXTFIELD' || key == 'TEXTAREA' || key == 'DATE' || key == 'DATETIME' || key == 'LABEL' || key == 'DATAGRID' || key == 'MULTSELECT' || key == 'RADIO' || key == 'CHECKBOX' || key == 'SELECT'>
						<li source="el" fieldType="${key}">${fieldTypes[key]}</li>
						<#-- 
						<#if view.assModel.isExtraCol?default(true)>
						<li source="el" fieldType="${key}">${fieldTypes[key]}</li>
						<#else>
						<#if key == 'DATAGRID' || key == 'MULTSELECT'>
						<li source="el" fieldType="${key}">${fieldTypes[key]}</li>
						</#if>
						</#if>
						-->
						</#if>
					</#list>
					</#if>
					</ul></div>
					<#-- 
					<h2>${getText('ec.view.one2oneattr')}</h2>
					<div source="one" class="accordion_pane"><ul class="onetoone_properties_container"></ul></div>
					<h2>${getText('ec.view.one2manyattr')}</h2>
					<div source="many" class="accordion_pane"><ul class="onetomany_properties_container"></ul></div>
					 -->
				</div>
			</div>	
			<div class="col-main"> 	
				<#if view.openType == 'dialog'>	
				<div  id="_dialogFrame" class="ewc-dialog-blove" style="visibility:visible;">
					<div class="hd" style="cursor: move;">
						<div class="hd-lc"></div>
						<div class="hd-cc"><h4>${getHtmlText('ec.project.view_setting')}</h4></div>
						<div class="dialog-handle"><a class="min" title="${getText('ec.view.mini')}" hidefocus="true" href="#"></a><a class="close" title="${getText('ec.view.accclose')}" href="#"></a></div>
						<div class="hd-rc">
						</div>
					</div>
					<div class="bd" id="bd_dialogFrame" style="width:${(view.width)!500}px;height:${(view.height)!260}px;">
						<div class="bd-lb"></div>
						<div class="bd-rb"></div>
						<div class="bd-cb" style="overyflow:hidden;">
							<div class="des"></div>
							<div class="content" style="overyflow:hidden;width:${((view.width)!500)-26}px;height:${(view.height)!260 - 4}px;">
							<div class="ewc-dialog-el">
				</#if>		
								<div class="main-wrap" id="main_design_container"<#if view.openType == 'dialog'> style="overflow:hidden;width:${((view.width)!500)-30}px;height:${(view.height)!260}px;"</#if>>
								<#-- <div class="main-wrap" id="main_design_container"> 
									<div>
										<div class="form_title"></div>
									</div>-->
									<div id="form_area">
										<div id="main_form_tab" class="etv-navset" style="overflow: hidden;">
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
											<div style="background-color:#e3f1f9;overflow-y: auto;" class="etv-content" >
												<#if ev?? && !(ev.config)?has_content>
													<div><ul class="form_design_ul se-selected" onclick="ec.selectsection(this)" colNum="${defaultColNum}"></ul></div>
												<#else>
												<#if (ev.configMap)??>
													<#list (ev.configMap)['tabs'] as tab>
													<div class="div-selected">
														<#list tab['sections'] as secatt>
															<#if (secatt.pageConfig.colNum)??>
																<#assign sectionColNum = secatt.pageConfig.colNum>
															<#else>
																<#assign sectionColNum = defaultColNum>
															</#if>
															<ul class="form_design_ul" onclick="ec.selectsection(this)" colwidth="${(secatt.pageConfig.colwidth)!}" colNum="${sectionColNum}" name="${(secatt.name)!}" isborder="${(secatt.isborder)!1}" cssstyle="${(secatt.cssstyle)!''}">
																<#if (secatt.content)['form']??>
																<#list (secatt.content)['form'] as element>
																<#if (element.element)??>
																<li class="form_design_ul_li" <#if (element.element.fieldType)?has_content>ondblclick="ec.cellProperty()" </#if> onclick="ec.chooseLi(this)" name="${(element.element.name)!}" complex="${(element.element.complex)?string("true","false")}" nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((element.element.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if> columnLong="${(element.element.columnLong)!}" decimalNum="${(element.element.decimalNum)!}"  <#if (iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if> <#if (isdelete)??>isdelete="${(element.element.isdelete)?string('true','false')}"</#if> isreadonly="${(element.element.readonly)?string('true','false')}" checkname="${(element.element.checkname)!}" fieldType="${(element.element.fieldType)!}"  fillType="${(element.element.fillType)!}" 
																fillContent="${(element.element.fillContent)!}" sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody='${(element.callbackbody)!''}' callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody='${(element.funcbody)!''}' selectCompType="${(element.element.selectCompType)!}" cssstyle="${(element.cssstyle)!''}" referenceview="${(element.element.referenceview)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}"
																<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> mneenable="${((element.element.mneenable)!false)?string('true','false')}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.element.fill.fillType)?has_content && (element.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.element.fill.fillOrder?has_content><#list element.element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${(element.element.fill.fillContent)[ne]}"</#list><#else><#list (element.element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${(element.element.fill.fillContent)[ne]}"</#list></#if>}<#else>"${(element.element.fill)[fe]}"</#if></#list></#if>}' 
																<#if (element.element.fieldType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"</#if>>
																	<#if (element.element.fieldType) == 'LABEL'>
																		<dd >${getText('${(element.element.namekey)!element.element.name}')}</dd>
																	<#else>
																		<dd>
																			<#if  (element.element.fieldType)??>
																			<#if (element.element.fieldType) == 'TEXTAREA' || (element.element.fieldType) == 'RICHTEXT'>
																			<textarea style="color:gray">${getText('${(element.element.checkname)!}')}</textarea>
																			<#elseif (element.element.fieldType) == 'DATE' || (element.element.fieldType) == 'DATETIME'>
																			<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${getText('${(element.element.checkname)!}')}"/><input type='button' class='cui-calpick' />
																			<#elseif (element.element.fieldType) == 'TEXTFIELD' ||  (element.element.fieldType) == 'PASSWORDFIELD'>
																			<input type="text" style="color:gray;width:100%;" value="${getText('${(element.element.checkname)!}')}"/>
																			<#elseif (element.element.fieldType) == 'SELECTCOMP'>
																			<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${getText('${(element.element.checkname)!}')}"/><input type='button' class='cui-search-click' />
																			<#elseif (element.element.fieldType) == 'MULTSELECT'>
																			<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${getText('${(element.element.checkname)!}')}"/><input type='button' class='cui-search-click' />
																			<#elseif (element.element.fieldType) == 'SELECT'>
																			<select style="color:gray;width:100%;"><option>${getText('${(element.element.checkname)!}')}</option></select>
																			<#elseif (element.element.fieldType) == 'RADIO'>
																				<input type="radio"/><font color="gray">${getHtmlText('${(element.element.checkname)!}')}</font>
																			<#elseif (element.element.fieldType) == 'CHECKBOX'>
																				<input type="checkbox"/><font color="gray">${getHtmlText('${(element.element.checkname)!}')}</font>
																			<#elseif (element.element.fieldType) == 'DATAGRID'>
																				<font style="font-weight:bolder;float: left;margin-left: 10px;">${(element.element.targetModelName)!} </font><table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td0">${getHtmlText('ec.view.num')}</td><td class="datagird_th_td1">${getHtmlText('ec.view.modelfield')}</td></tr></thead><tbody><tr><td class="datagird_tb_td0">1</td><td class="datagird_tb_td1"></td></tr></tbody></table>
																			</#if>
																			</#if>
																		</dd>
																	</#if>
																	<div></div>
																</li>
																<#else>
																	<li class="form_design_ul_li" onclick="ec.chooseLi(this)" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}"><dd></dd><div></div></li>
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
				<#if view.openType == 'dialog'>					
								</div>
							</div>
						</div>
					</div>
				
					<div class="ft">
						<div class="ft-lc"></div>
						<div class="ft-cc"></div>
						<div class="ft-rc"></div>
					</div>
				</div>
				</#if>
			</div>
			<div class="col-operate">
				<div>
					<ul id="main_form_tab_operate" >
						<li/>
						<li title="${getText('ec.view.pageProperty')}"><button class="Dialog_button btn_pointer" onclick="ec.cellPageProperty()" type="button">${getHtmlText('ec.view.pageProperty')}</button></li>
						<li><hr class="line-icon"></hr></li>
						<li title="${getText('ec.view.addSection')}"><button class="Dialog_button btn_pointer" onclick="ec.addSection()" type="button">${getHtmlText('ec.view.addSection')}</button></li>
						<li title="${getText('ec.view.delSection')}" ><button class="Dialog_button btn_pointer" onclick="ec.delSection()" type="button">${getHtmlText('ec.view.delSection')}</button></li>
						<li title="${getText('ec.view.sectionattr')}"><button class="Dialog_button btn_pointer" onclick="ec.sectionProperty()" type="button">${getHtmlText('ec.view.sectionattr')}</button></li>
						<li><hr class="line-icon"></hr></li>
						<li title="${getText('ec.view.addline')}"><button class="Dialog_button btn_pointer" onclick="ec.addCell(1)" type="button">${getHtmlText('ec.view.addline')}</button></li>
						<li  title="${getText('ec.view.deleteline')}"><button class="Dialog_button btn_pointer" onclick="ec.delRow()" type="button">${getHtmlText('ec.view.deleteline')}</button></li>
						<li><hr class="line-icon"></hr></li>
						<li title="${getText('ec.view.splitcell')}"><button class="Dialog_button btn_pointer" onclick="ec.splitCell2()" type="button">${getHtmlText('ec.view.splitcell')}</button></li>
						<li title="${getText('ec.view.delcell')}"><button class="Dialog_button btn_pointer" onclick="ec.delCell()" type="button">${getHtmlText('ec.view.delcell')}</button></li>
						<li title="${getText('ec.view.cellproperty')}"><button class="Dialog_button btn_pointer" onclick="ec.cellProperty()" type="button">${getHtmlText('ec.view.cellproperty')}</button></li>
						<li><hr class="line-icon"></hr></li>
						<li title="${getText('ec.view.addtab')}"><button class="Dialog_button btn_pointer" onclick="ec.addTab()" type="button">${getHtmlText('ec.view.addtab')}</button></li>
						<li title="${getText('ec.view.tabproperty')}"><button class="Dialog_button btn_pointer" onclick="ec.tabProperty()" type="button">${getHtmlText('ec.view.tabproperty')}</button></li>
					</ul>
				</div>
			</div>
			<div id="design-button" style="height:30px;width:100%;overflow:hidden;clear:both;text-align:right;padding-top:10px">
				<#-- <button class="Dialog_button btn_pointer" onclick="ec.save()" type="button">${getText('common.button.save')}</button>&#160;&#160;&#160;&#160;&#160;-->
				<#-- <button class="Dialog_button btn_pointer" onclick="ec.save(true)" type="button">${getText('common.button.saveexit')}</button>&#160;&#160;&#160;&#160;&#160;-->
				<#-- <button class="Dialog_button btn_pointer" onclick="ec.publish()" type="button">${getText('ec.view.publishview')}</button>&#160;&#160;&#160;&#160;&#160;-->
				<#-- <button class="Dialog_button btn_pointer" onclick="ec.preview()" type="button">${getText('ec.view.preview')}</button>		-->
			</div>
		</div>	
		
		
		<div id="form_set_item_property_dlg" style="display:none">
			<@errorbar id="EditDialogErrorBar"></@errorbar>
			<input type="hidden" id="form_property_field_val" />
			<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.name')}</td>
					<td class="co" width="30%"><input type="text" id="form_property_name" class="cui-edit-field" style="width:100%" /></td>
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
					<td class="co" width="30%">
					<#-- <input type="text" id="form_property_show_name" class="cui-edit-field" style="width:100%" /> -->
					<#-- international_propertyshowName    international_propertyshowName_showName -->
					<@international name="property.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
					</td>
				</tr>
				<tr id="selectDisplayTypeTR"><td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.type')}</td>
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
						</select>
					</td>
				</tr>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="form_property_alignLable">${getHtmlText('ec.view.align')}</label></td>
					<td class="co" width="30%">
						<select id="form_property_align">
							<option value="left">${getText('ec.view.left')}</option>
							<option value="center">${getText('ec.view.center')}</option>
							<option value="right">${getText('ec.view.right')}</option>
						</select>
					</td>
					<td id="selectcomp_checkbox_mneenable_label" class="la" width="30%" align="right">${getHtmlText('ec.view.mneenable')}</td>
					<td id="selectcomp_checkbox_mneenable_checkbox" class="co" width="60%">
						<input type="checkbox" id="selectcomp_checkbox_mneenable" value="true">
					</td>
				</tr>
				<#-- enumerate  -->
				<tr id="baseproperty_enum_enumerate">
					<td class="la" width="20%" align="right" style="vertical-align:top;">${getHtmlText('ec.property.enum')}&#160;&#160;&#160;</td>
					<td class="co" colspan="3">
						<div style="max-height:170px;overflow:auto;">
							<table width="90%" cellspacing="1" bgcolor="#CCCCCC" id="enumerate_content_table">
								<thead>
									<tr bgcolor="#FFFFFF">
										<td width="40%" class="dg-hd-td-label">${getHtmlText('ec.property.value')}</td>
										<td width="40%" class="dg-hd-td-label">${getHtmlText('ec.property.content')}</td>
										<td width="10%" class="dg-hd-td-label">${getHtmlText('ec.view.delete')}</td>
									</tr>
								</thead>
								<tr bgcolor="#FFFFFF">
									<td><input type="text" class="cui-edit-field" style="width:95%" /></td>
									<td><input type="text" class="cui-edit-field" style="width:95%" /></td>
									<td><img src="/bap/static/foundation/images/icon-del.gif" style="cursor: pointer;" /></td>
								</tr>
							</table>
						</div>
					</td>
				</tr>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isreadOnlyLable">${getHtmlText('ec.view.isreadonly')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_readonly" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isnullableLable">${getHtmlText('ec.view.isnullable')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_nullable" /></td>
				</tr>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isControlLable">${getHtmlText('ec.view.iscontrol')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_control" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isDeleteLable">${getHtmlText('ec.view.isdelete')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_delete" /></td>
				</tr>
				<tr id="form_tr_isgroup">
					<td class="la" width="20%" align="right" padding-right="7px"><label id="isGroupLable">${getHtmlText('ec.view.isgroup')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_group" /></td>
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
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.comstyle')}
					<span id="comstyleHelpinfo" class="baphelp-icon"></span>&#160;
<div id="comstyleHelpinforef" style="display:none">
	<p class="baphelp-info">内部控件的自定义样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式</p>
</div>
					<script type="text/javascript">
							$('#comstyleHelpinfo').helptip({refElm:"#comstyleHelpinforef", html: true , isCustom :false, width: 180 , title :"说明"});
						</script></td>
					<td class="co" colspan="3"><textarea id="form_property_field_style_com" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea></td>
				</tr>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.tdstyle')}
					<span id="tdstyleHelpinfo" class="baphelp-icon"></span>
<div id="tdstyleHelpinforef" style="display:none">
	<p class="baphelp-info">外部TD的自定义样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式</p>
</div>
					<script type="text/javascript">
							$('#tdstyleHelpinfo').helptip({refElm:"#tdstyleHelpinforef", html: true , isCustom :false, width: 180 , title :"说明"});
						</script></td>
					<td class="co" colspan="3"><textarea id="form_property_field_style_container" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea></td>
				</tr>
			</table>
		</div>
		<div id="form_set_select_val" style="display:none;">
			<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.type')}</td>
					<td class="co" width="30%">
						<select id="form_set_select_val_type" onchange="ec.changeFillValType(this)" class="cui-edit-field" style="width:100%">
							<!--<option value="DEFAULT">${getText('ec.view.initvalue')}</option>
							<option value="CONSTANT">固有值</option>-->
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
					<td class="co" width="30%">${getHtmlText('ec.view.everyline')}<input type="text" id="layout_col_num" class="cui-edit-field" style="width:50px" /> ${getHtmlText('ec.view.colum')}</td>
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
					<td class="la" width="15%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
					<td class="co" width="85%">${getHtmlText('ec.view.everyline')} <input type="text" id="section_num" class="cui-edit-field" style="width:50px"/> ${getHtmlText('ec.view.colum')}</td>
				</tr>
				<tr>
					<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.sectionname')}</td>
					<td class="co"><input type="text" id="section_name" class="cui-edit-field" style="width:90px" /></td>
				</tr>
				<tr>
					<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.isborder')}</td>
					<td class="co"><input type="checkbox" id="section_border"/></td>
					
				</tr>
				<tr>
					<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.colwidth')}</td>
					<td class="co">
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
				<tr>
					<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.customstyle')}</td>
					<td class="co"><textarea id="form_section_style" class="cui-edit-textarea" style="width:100%;height:80px;margin-top:5px"></textarea></td>
				</tr>
			</table>
	 	</div>
	 	<div id="tab_setting_dlg" style="display:none;">
	 		<@errorbar id="TabDialogErrorBar"></@errorbar>
	 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="15%" align="right" padding-right="10px" style="color: rgb(179, 3, 3);">${getHtmlText('ec.view.tabname')}</td>
					<td class="co">
					<#-- international_tab_name    international_tab_name_showName 
					<@international name="tab_name" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />-->
					<input type="text" id="tab_name" class="cui-edit-field" style="width:90px" /></td>
				</tr>
			</table>
	 	</div>
	 	<div id="datagrid_setting_dlg" style="display:none;">
	 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="40%" align="right" padding-right="5px">${getHtmlText('ec.view.choicemodel')}</td>
					<td class="co" width="60%">
						<select id="datagrid_model_type" class="cui-edit-field" style="width:100%">
						<#if assos?has_content>
							<#list assos as asso>
							<option value="${asso.originalProperty.code}">${getText('${asso.originalProperty.model.name}')}[${getText('${asso.originalProperty.displayName}')}]</option>
							</#list>
						</#if>
						</select>
					</td>
				</tr>
			</table>
	 	</div>
	 	<div id="multselect_setting_dlg" style="display:none;">
	 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="30%" align="right" padding-right="5px">${getHtmlText('ec.view.assmodel')}</td>
					<td class="co" width="60%">
						<select id="multselect_model_type" class="cui-edit-field" style="width:100%">
						<#if (multSelectInfo['directAssos'])??>
							<#list multSelectInfo['directAssos'] as asso>
							<option value="-${asso.originalProperty.code}-${asso.targetProperty.code}-">
								${getText('${asso.targetProperty.model.name}')}
							</option>
							</#list>
						</#if>
						</select>
					</td>
				</tr>
				<tr id="associated_1">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.attribute')}</td>
					<td class="co" width="60%">
					<select id="ec_select_indirect_model" style="width:100%;"></select>
					</td>
				</tr>
				<tr id="associated_2">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.choiceRef')}</td>
					<input type="hidden" id="ec_select_property" />
					<td class="co" width="60%">
					<select id="ec_reference_view" style="width:100%"></select>
					</td>
				</tr>
				<tr id="ec_tr_mneenable" style="display:none;">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.mneenable')}</td>
					<td class="co" width="60%">
					<input type="checkbox" id="ec_checkbox_mneenable" value="true">
					</td>
				</tr>
			</table>
	 	</div>
	 	<div id="datagrid_property_dlg" style="display:none;">
	 		<iframe id="datagird_property_frame" name="datagird_property_frame" width="100%" height="450px" style="overflow:hidden;" SCROLLING="no" MARGINHEIGHT="0" MARGINWIDTH="0" frameborder="0" border="0" framespacing="0"></iframe>
	 	</div>
	</body>
	<script type="text/javascript">	
		//自动设置中间编辑区域的宽度
		function _init_size(){
			$('.col-main').width($(window).width()-245);
			$('.col-main').height($(window).height()-85);
			$('.col-sub').height($(window).height()-83);
			$('.col-operate').height($(window).height()-83);
			<#if view.openType == 'dialog'>	
			$('.etv-navset').height(${(view.height)!260});
			$('.etv-navset').width(${(view.width)!500} - 42);
			$('.etv-content').width(${(view.width)!500} - 42);
			<#else>
			$('.etv-navset').height($(window).height()-100);
			$('.etv-navset').width($(window).width()-251);
			$('.etv-content').width($(window).width()-255);
			</#if>
			if($('#_dialogFrame').length>0){
				$('#bd_dialogFrame').css('max-height',$('.col-main').height()-50);
				$('#bd_dialogFrame').css('max-width',$('.col-main').width()-100);
				$('#_dialogFrame').css('margin-left',($('.col-main').width()-$('#bd_dialogFrame').width())/2);
			}
		}
		$(function(){
			_init_size();
			CUI('#multselect_model_type').change(function(){
				ec.multselect_model_type_changed(this);
			});
			CUI('#ec_select_indirect_model').change(function(){
				ec.loadModelProperties(this);
			});
			ec.multselect_model_type_changed(CUI('#multselect_model_type')[0]);
		});
		$(window).resize(function(){
			_init_size();				
		});
	</script>
	</html>