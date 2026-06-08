	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
	<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<title>${getText(view.assModel.entity.name)}-${getText(view.displayName)}-${getText('ec.project.view_setting')}-<#if view.type == 'VIEW'>${getText('ec.view.view')}<#else>${getText('ec.view.edit')}</#if></title>
		<@ec_editMobileTop/>

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
			.grid-s4m0 .col-sub { margin-left:2px;margin-top:8px;width: 220px; float:left;background-color:white;border:1px solid #3C7FB1}
			.grid-s4m0 .col-operate{ margin-right:3px;border:1px solid #a2a2a2;background-color: #d4d4d4;width: 80px;float:right;margin-top:8px;overflow:hidden;}
			.grid-s4m0 .col-top{width: 100%;height:30px;background-color:#D4D8DB;border-bottom:1px solid #ADACAB;}
			#form_select_elements{height:98%}
			//#form_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
			#form_select_elements h2 {padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
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
			
			#main_form_tab .etv-content{min-height:470px;}
			
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
			.couple-selected { background: #FFD27F;}
			.couple-sort-checked { background-color: #92D8FB;}
			.se-selected { background: #F39814;}
			.BlockInfoDiv {margin:2px;padding:4px;border-top:1px dashed #cccccc;}
			.editeventli {width:175px;text-align: right;}
			.datagird_th_td0 {width:5%;height:22px;overflow:hidden;text-align:center;padding:0 3px;background:url("/bap/static/css/sprite_20120525.png") repeat-x scroll 0 -1340px transparent;border-right:1px solid #FFFFFF}
			.datagird_th_td1 {width:95%;height:22px;overflow:hidden;text-align:center;padding:0 3px;background:url("/bap/static/css/sprite_20120525.png") repeat-x scroll 0 -1340px transparent;border-right:1px solid #FFFFFF}
			.datagird_tb_td0{width:5%;height:22px;text-align:center;overflow:hidden;background-color:#E7E7E7;border-top:1px solid #BABABA;border-left:1px solid #BABABA;border-bottom:1px solid #BABABA}
			.datagird_tb_td1{width:95%;height:22px;overflow:hidden;background-color:white;border:1px solid #BABABA;}
			a.help-link {
				display: inline-block;
				width: 15px;
				height: 15px;
				background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
			}
		</style>
		<script type="text/javascript">
			<#assign defaultColNum = 2>
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
					ec = new CUI.EntityConfEditMobile(
						'${(view.code)!}',
						{pageConfig:{colNum:${defaultColNum}}},
						main_tab,
						'${(ev.code)!}',
						'${(ev.version)!0}',
 						'${(isProj)?string!}',
						{
							'singlSelect' : {'SELECT':'${getText('ec.view.select')}', 'RADIO':'${getText('ec.view.radio')}'},
						 	'multiSelect' : {'CHECKBOX':'${getText('ec.view.checkbox')}', 'MULTSELECT':'${getText('ec.view.mulselect')}'},
							'objectSelect': {'TEXTFIELD':'${getText('ec.view.textfield')}','TEXTAREA':'${getText('ec.view.textarea')}', 'DATE':'${getText('ec.view.date')}', 'DATETIME':'${getText('ec.view.datetime')}', 'SELECTCOMP':'${getText('ec.view.selectcomp')}'},
						 	'normalSelect': {<#if fieldTypes??><#list fieldTypes?keys as key>'${key}':'${getText('${fieldTypes[key]!}')}'<#if key_has_next>,</#if></#list></#if>},
							'viewColumnTypes' : {<#if viewColumnTypes??><#list viewColumnTypes?keys as key>'${key}':'${getText('${viewColumnTypes[key]!}')}'<#if key_has_next>,</#if></#list></#if>},
							'viewFormats' :  {<#if viewFormats??><#list viewFormats?keys as key>'${key}':'${getText('${viewFormats[key]!}')}'<#if key_has_next>,</#if></#list></#if>}
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
	<body class="ec-config-page" style="background-color:#f2f2f2" layoutCode="${(ev.configMap.layout.layoutCode)!ecCodeInit('layout')}" page_beforesave="${((ev.configMap.layout.pageConfig.beforeSave)!)?html}" page_onload="${((ev.configMap.layout.pageConfig.onload)!)?html}" page_onsave="${((ev.configMap.layout.pageConfig.onsave)!)?html}" page_aftersave="${((ev.configMap.layout.pageConfig.afterSave)!)?html}" page_beforesubmit="${((ev.configMap.layout.pageConfig.beforeSubmit)!)?html}" page_aftersubmit="${((ev.configMap.layout.pageConfig.afterSubmit)!)?html}">
		<@errorbar id="workbenchErrorBar" offsetY=83 />
			<div class="layout grid-s4m0"> 	
			<div class="col-top">
				<div>
					<h3 style="color:#3A70AA;padding-top:7px;padding-left:15px;float:left">
						${getText(view.assModel.entity.name)}-${getText(view.displayName)}-${getHtmlText('ec.project.view_setting')}-<#if view.type == 'VIEW'>${getHtmlText('ec.view.view')}<#else>${getHtmlText('ec.view.edit')}</#if>
					</h3>
					<span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin: 8px 15px;float:right">
            			<a href="/help/" target="_blank" title="帮助文档" class="help-link"></a>
            		</span>
				</div>
			</div>
			<div class='col-mobile-mian'>
			<div class="col-sub">
				<#-- 左侧 -->
				<div id="form_select_elements">
					<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
					<div class="accordion_pane" style="display:block;overflow:auto;height:91%">
						<ul class="main_properties_container">
							<#list view.assModel.properties as p>
								<#if (p.name) != "extraCol" && (p.name) != "tableInfoId" && (p.name) != "id" && (p.name) != "version" && ((p.isCustom)?? && !(p.isCustom))>
									<li source='<#if p.type == "OFFICE">office<#else>main</#if>' name='${view.assModel.modelName?uncap_first}.${p.name}' propDefaultValue='${(p.defaultValue)!}'  propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}'  propnullable='${(p.nullable!false)?string('true','false')}' multable='${(p.multable!false)?string('true','false')}' fillContent='${(p.fillcontent)!}' propPrecision='${(p.decimalNum)!}'
									<#if subs?? && (subs.associatedInfos)??>
										<#assign assFlag = false>
										<#assign associatedInfos = (subs.associatedInfos)>
                                        <#list associatedInfos as associatedInfo>
											<#if (associatedInfo.type)?has_content && ((associatedInfo.type) == 1 || (associatedInfo.type) == 2) && (associatedInfo.originalProperty.code)?? && (associatedInfo.originalProperty.code) == (p.code)>
											<#assign assFlag = true>
											modelcode = '${(associatedInfo.targetProperty.model.code)!}' topAssName = '${(associatedInfo.targetProperty.name)!}' objectPropertyNullable='${(p.nullable!false)?string('true','false')}'><img align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='ec.showAssProperty("${(p.code)!}","${(associatedInfo.targetProperty.model.code)!}",this,"${(view.assModel.modelName?uncap_first)!}.${(p.name)!}","${(p.name)!}")'></img>
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
						<#if key == 'TEXTFIELD' || key == 'DATE' || key == 'DATETIME' || key == 'LABEL' || key == 'DATAGRID' || key == 'MULTSELECT' || key == 'RADIO' || key == 'CHECKBOX' || key == 'SELECT' >
						<li source="el" showType="${key}">${getHtmlText('${fieldTypes[key]!}')}</li>
						<#-- 
						<#if view.assModel.isExtraCol?default(true)>
						<li source="el" fieldType="${key}">${fieldTypes[key]!}</li>
						<#else>
						<#if key == 'DATAGRID' || key == 'MULTSELECT'>
						<li source="el" fieldType="${key}">${fieldTypes[key]!}</li>
						</#if>
						</#if>
						-->
						</#if>
					</#list>
					<li source="custom" name="customsection"  showtype="customsection">${getHtmlText("foundation.common.currency.customfield")}</li>
					</#if>
					</ul></div>
					
				</div>
			</div>
			<div class="col-main">
				<#assign viewWidth = 460>
				<#assign viewHeight = 330>
				<div class="main-wrap" id="main_design_container">
			<#-- <div class="main-wrap" id="main_design_container"> 
				<div>
					<div class="form_title"></div>
				</div>-->
				<div id="form_area">
					<div id="main_form_tab" class="etv-navset" style="overflow: hidden;">
						<input type="hidden" id="viewCode" value="${(view.code)!}" />
						<input type="hidden" id="configType" value="edit" />
						<input type="hidden" id="viewType" value="${(view.type)!}" />
						<input type="hidden" id="delCellIds" name="delCellIds" />
						<input type="hidden" id="btDelCellIds" name="btDelCellIds" />
						<input type="hidden" id="delEventIds" name="delEventIds" />
						<input type="hidden" id="delValidateIds" name="delValidateIds" />
						<#--<div id="buttonOperateDiv" style="margin:10px 10px 15px 10px;padding-bottom:5px;background:#F3F3F3!important;border:1px solid #0099CC;">
							<div class="operateDiv">
								<label for="button"><font style="font-weight:bolder">${getHtmlText('ec.view.configoperator')}</font></label>
								<div style="padding-left:20px;display:inline-block">
								<a class="cui-btn mr10 cui-btn-add" href="#" onclick="ec.addOperateButton()">${getText('ec.view.add')}</a>
								<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="ec.modifyOperateButton()">${getText('common.button.edit')}</a>
								<a class="cui-btn mr10 cui-btn-del" href="#" onclick="ec.deleteOperateButton()">${getText('ec.view.delete')}</a>
								<a class="cui-btn mr10 cui-btn-add" href="#" onclick="ec.addSeparate()">${getText('ec.view.separate')}</a>
								<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="ec.leftmove('buttonOperateDiv')">${getText('ec.view.toleft')}</a>
								<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="ec.rightmove('buttonOperateDiv')">${getText('ec.view.toright')}</a>
								<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="ec.operatorBarProperty()">${getText('ev.view.buttonbar.setting')}</a>
								</div>
							</div>
							<div style="border:#CCC 1px solid;height:45px;margin:5px 5px 0 5px;">
								<div style="padding-top:10px;padding-left:10px;">
									<#if (ev.configMap)??  && (ev.configMap.layout)??>
									<#assign configMap = (ev.configMap.layout)>
									<#if (configMap.sections)??>
									<#list (configMap.sections) as section>
										<ul id="operateButton_ul" cssName="${section.cssName!}" sectionCode="${(section.sectionCode)!ecCodeInit('section')}">
										<#if (section.regionType)?? && (section.regionType)=='BUTTON'>
											<#assign serviceName = section.serviceName!''>
											<#if section.cells??>
												<#list (section.cells) as operateButton>
													<li cellCode="${operateButton.cellCode!ecCodeInit()}" 
														id="${(operateButton.id)!}" namekey="${(operateButton.namekey)!}" showname="${((operateButton.showname)!)?html}"
													<#if (operateButton.funcname)??>
														funcname="${((operateButton.funcname)!)?html}" 
													</#if>
													<#if (operateButton.funcbody)??>
														funcbody="${((operateButton.funcbody)!)?html}"
													</#if>
													<#if (operateButton.ispermission)??>
														ispermission="${(operateButton.ispermission)?string('true','false')}"
													</#if>
													<#if (operateButton.operateurl)??>
														operateurl="${((operateButton.operateurl)!)?html}"
													</#if>
													<#if (operateButton.isHide)??>
														isHide="${(operateButton.isHide)?string('true','false')}"
													</#if>
													<#if (operateButton.permissionCode)??>
														permissionCode="${operateButton.permissionCode!}"
													</#if>
													<#if (operateButton.operatetype)??>
														operatetype="${((operateButton.operatetype)!)?html}"
													</#if>
													<#if (operateButton.buttonAlign)??>
														buttonAlign="${((operateButton.buttonAlign)!)?html}"
													</#if>
													 class="button_design_ul_li" onmousedown="ec.selectListLi(this)" <#if !(operateButton.operatetype)?? || ((operateButton.operatetype)?? && (operateButton.operatetype) != 'SEPARATE')>ondblclick="ec.modifyOperateButton()"></#if><dd><#if (operateButton.operatetype)?? && (operateButton.operatetype) == 'SEPARATE'><input type="button" style="width:30px" value="|"><#else><input type="button" class="Dialog_button btn_pointer" value="${getText('${(operateButton.namekey)!(operateButton.showname)!}')}"></#if></dd></li>
												</#list>
											</#if>
											<#break>
										</#if>
										</ul>
									</#list>
									<#else>
									<ul id="operateButton_ul" sectionCode="${ecCodeInit('section')}">
									</ul>
									</#if>
									<#else>
										<ul id="operateButton_ul" sectionCode="${ecCodeInit('section')}">
										</ul>
									</#if>	
								
								</div>	
							</div>
						</div>-->
						<hr style="margin-left:8px;margin-right:8px;display:none">
						<ul class="etv-nav">
							<#if ev?? && !(ev.config)?has_content>
								<li class="selected" namekey="ec.view.commoninfo">${getHtmlText('ec.view.commoninfo')}</li>
							<#else>
								<#assign commoninfo = getText('ec.view.commoninfo')>
								<#if (ev.configMap)?? && (ev.configMap.layout)??>
								<#list (ev.configMap.layout)['tabs'] as tab1>
									<#if !tab1.namekey?? && (tab1.name)?? && commoninfo?? && commoninfo == tab1.name>
										<li class="selected" namekey="ec.view.commoninfo">${getHtmlText('ec.view.commoninfo')}</li>
									<#else>
										<li namekey="${(tab1.namekey!)}">${getHtmlText('${(tab1.namekey)!((tab1.name)!)}')}</li>
									</#if>
								</#list>
								</#if>
							</#if>
							<#if (view.project.workflowEnabled)??><li fixed="true">${getHtmlText('ec.view.procinfo')}</li></#if>
						</ul>
						<#if view.assModel.entity.module.type??&&view.assModel.entity.module.type=="Mis">
						<div class="etv-content-mobile-style-edit">
						<#else>
						<div class="etv-content-mobile-style">
						</#if>
							<div class="etv-mobile-bg">
								<div class="bg-top"><span class="circle-wrap"><i></i><i></i><i></i></span></div>
								<div class="bg-bottom"><i></i></div>
							</div>
						<div style="background-color:#e3f1f9;overflow-y: auto;" class="etv-content" >
							<#if ev?? && !(ev.config)?has_content>
								<div><ul class="form_design_ul se-selected" sectionCode="${ecCodeInit('section')}" onclick="ec.selectsection(this)" colNum="${defaultColNum}"></ul></div>
							<#else>
							<#if (ev.configMap)?? && (ev.configMap.layout)??>
								<#list (ev.configMap.layout)['tabs'] as tab>
								<div class="div-selected">
									<#list tab['sections'] as secatt>
										<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
										<#assign sectionColNum = defaultColNum>
										<ul class="form_design_ul" sectionCode="${sectionCode!}" onclick="ec.selectsection(this)" colwidth="${(secatt.pageConfig.colwidth)!}" colNum="${sectionColNum}" name="${(secatt.name)!}" isborder="${(secatt.isborder)!1}" cssstyle="${(secatt.cssstyle)!''}">
											<#if (secatt.cells)??>
											<#list (secatt.cells) as element>
											<#assign cellCode = element.cellCode!ecCodeInit()>
											<#if (element.element)??>
												<#if (element.element.namekey)??>
													<#assign ckname = getText(element.element.namekey)! >
												<#else>
													<#assign ckname = (element.element.checkname)! >
												</#if>
												<#if (element.element.propertyCode)??>
													<#assign propCodes = (element.element.propertyCode)?split('||')>
													<#assign propCode = propCodes[propCodes?size - 1]>
												<#else>
													<#assign propCode = 'null'>
												</#if>
												<#assign currentProperty = 'undefined' />
												<#if (element.element.name)?? && element.element.name?string?split('.')?size gt 1>
												<#list view.assModel.properties as p>
													<#if p.type == 'OBJECT' && (element.element.propertyCode!)?index_of(p.code + '||') == 0>
														<#assign currentProperty = p />
														<#break />
													</#if>
												</#list>
												</#if>
												<#if (element.element.showType) == 'LABEL'>
													<#if (propCode)?? && propertyMap?? && propertyMap[propCode]??>
														<#assign property = propertyMap[propCode] >
													<li class="form_design_ul_li" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" key="${(element.element.key)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" propertyCode="${(element.element.propertyCode)!}" complex="${(element.element.complex)?string("true","false")}" <#if currentProperty=='undefined'>propnullable="${(property.nullable!false)?string('true','false')}"</#if> nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((property.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if>  <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}" showFormat="${(element.element.showFormat)!}"
											propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(element.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}" sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" 
											<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' 
											<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}" <#assign targetPropertyCode = element.element.targetPropertyCode></#if>
											<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" <#--downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
											isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
											acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
											</#if>>
													<#else>
													<li class="form_design_ul_li" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" key="${(element.element.key)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" propertyCode="${(element.element.propertyCode)!}" complex="${(element.element.complex)?string("true","false")}" nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((element.element.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if>   <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}"  showFormat="${(element.element.showFormat)!}"
											 sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" 
											<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' 
											<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"<#assign targetPropertyCode = element.element.targetPropertyCode></#if>
											<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" <#--downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
											isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
											acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
											</#if>>
													</#if>
													<dd >${getText('${((element.element.namekey)!element.element.name)!}')}</dd>
												
												<#elseif (element.element.showType) == 'customsection'>
													<li class="form_design_ul_li" cellCode="${cellCode!}"  onmousedown="ec.chooseLi(this)" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> name="${(element.element.name)!}" key="${(element.element.key)!}"  showType="${(element.element.showType)!}"  showFormat="${(element.element.showFormat)!}" isCustom="${((element.element.isCustom)!false)?string('true','false')}"
											  multallowview="${((element.element.multallowview)!false)?string('true','false')}"  colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" modelCode="${(element.element.modelCode)!}"
											<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if>'>
													<dd >${getText('${((element.element.namekey)!element.element.name)!}')}</dd>
												
												<#else>
													<#if (propCode)?? && propCode!='' && propertyMap?? && propertyMap[propCode]??>
														<#assign property = propertyMap[propCode] >
													<li class="form_design_ul_li" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" defaultValue="<#if element.element.defaultValueHasChanged!false>${(element.element.defaultValue!)?string}<#else>${(property.defaultValue!)?string}</#if>" propDefaultValue="${(property.defaultValue)!}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" complex="${(element.element.complex)?string("true","false")}" nullable="<#if (element.element.nullable)?? && element.element.nullable>true<#else>false</#if>" <#if currentProperty=='undefined'>propnullable="${(property.nullable!true)?string('true','false')}" </#if> multable="${((property.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if> <#if (element.element.autoresize)??>autoresize="${(element.element.autoresize)?string('true','false')}"</#if>  <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol!false)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" 
													  showType="<#if propCodes?size gt 1 || element.element.showTypeHasChanged!false>${(element.element.showType)!}<#else>${(property.fieldType!)}</#if>" showFormat="<#if propCodes?size gt 1 || element.element.showFormatHasChanged!false>${(element.element.showFormat)!}<#else>${(property.format!)}</#if>"  
														<#if currentProperty?? && currentProperty?string != 'undefined'>
															objectPropertyNullable = "${currentProperty.nullable?string('true', 'false')}" 
														</#if>
											propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(element.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}"  maxLength = "${property.maxLength!}"
											precision="<#if element.element.precisionHasChanged!false>${element.element.precision!}<#else>${(property.decimalNum!)}</#if>" propPrecision="${property.decimalNum!}" precisionHasChanged="${(element.element.precisionHasChanged!false)?string('true','false')}" 
														<#if (element.validate)??>validates = "<#list element.validate as validate><#if validate_index gt 0>~~~~</#if>${validate.type}<#if validate.param??><#list (validate.param)?keys as paramKey>~~${paramKey}~${validate.param[paramKey]?html}<#if paramKey=='errorMsg'>~~international_errorMsg_showName~${getText("${validate.param[paramKey]}")}</#if></#list></#if></#list>"</#if>
											sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" propertyCode="${(element.element.propertyCode)!}" picWidth="${(element.element.picWidth)!}" picHeight="${(element.element.picHeight)!}"
											<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' <#if property.fillcontent??>fill='${property.fillcontent!''?html}'</#if> 
											<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}" <#assign targetPropertyCode = element.element.targetPropertyCode></#if>
											<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" <#--downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
											isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
											acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
											</#if>>
													<#else>
													<li class="form_design_ul_li" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" defaultValue="${(element.element.defaultValue!)?string}" propDefaultValue="${(element.element.defaultValue!)?string}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" complex="${(element.element.complex)?string("true","false")}" nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((element.element.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if> <#if (element.element.autoresize)??>autoresize="${(element.element.autoresize)?string('true','false')}"</#if> <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}"  
														<#if currentProperty?? && currentProperty?string != 'undefined'>
															objectPropertyNullable = "${currentProperty.nullable?string('true', 'false')}" 
														</#if>
														<#if (element.validate)??>validates = "<#list element.validate as validate><#if validate_index gt 0>~~~~</#if>${validate.type}<#if validate.param??><#list (validate.param)?keys as paramKey>~~${paramKey}~${validate.param[paramKey]?html}<#if paramKey=='errorMsg'>~~international_errorMsg_showName~${getText("${validate.param[paramKey]}")}</#if></#list></#if></#list>"</#if>
											sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" showFormat="${(element.element.showFormat)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" propertyCode="${(element.element.propertyCode)!}"
											<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index gt 0>,</#if>"${fe}":<#if (element.element.fill.fillType)?has_content && (element.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.element.fill.fillOrder?has_content><#list element.element.fill.fillOrder?split(",") as ne><#if ne_index gt 0>,</#if>"${(ne)!}":"${(((element.element.fill.fillContent)[ne])!)?html}"</#list><#else><#list (element.element.fill.fillContent)?keys as ne><#if ne_index gt 0>,</#if>"${ne!}":"${(((element.element.fill.fillContent)[ne])!)?html}"</#list></#if>}<#else>"${((element.element.fill)[fe])?html}"</#if></#list></#if>}' 
											<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}" <#assign targetPropertyCode = element.element.targetPropertyCode></#if>
											<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" <#--downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
											isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
											acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
											</#if>>
													</#if>
													<dd>
														<#if  (element.element.showType)??>
														<#if (element.element.showType) == 'TEXTAREA' || (element.element.showType) == 'RICHTEXT'>
														<textarea style="color:gray">${ckname!}</textarea>
														<#elseif (element.element.showType) == 'DATE' || (element.element.showType) == 'DATETIME'|| (element.element.showType) == 'TIME'>
														<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-calpick' />
														<#elseif (element.element.showType) == 'TEXTFIELD' || (element.element.showType) == 'BAPCODE' || (element.element.showType) == 'SUMMARY' ||  (element.element.showType) == 'PASSWORDFIELD'>
														<input type="text" style="color:gray;width:100%;" value="${ckname!}"/>
														<#elseif (element.element.showType) == 'MULTFILES'>
														<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.showType) == 'SELECTCOMP'>
														<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.showType) == 'MULTSELECT' || (element.element.showType) == 'SUPERVISION'>
														<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.showType) == 'PROPERTYATTACHMENT'>
														<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.showType) == 'PICTURE'>
														<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.showType) == 'SELECT'>
														<select style="color:gray;width:100%;"><option>${ckname!}</option></select>
														<#elseif (element.element.showType) == 'RADIO'>
															<input type="radio"/><font color="gray">${ckname!}</font>
														<#elseif (element.element.showType) == 'CHECKBOX'>
															<input type="checkbox"/><font color="gray">${ckname!}</font>
														<#elseif (element.element.showType) == 'DATAGRID'>
															<table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td0">${getHtmlText('ec.view.num')}</td><td class="datagird_th_td1"><#if targetPropertyCode??>
																					<#if assos?has_content>
																						<#list assos as asso>
																							<#if asso.originalProperty.code == targetPropertyCode>
																							DATAGRID:${getText('${asso.originalProperty.model.name}')}[${getText('${asso.originalProperty.displayName}')}]
																							</#if>
																						</#list>
																					</#if>
																				<#else>${getHtmlText('ec.view.modelfield')}</#if></td></tr></thead><tbody><tr><td class="datagird_tb_td0">1</td><td class="datagird_tb_td1"></td></tr></tbody></table>
																			
														<#elseif (element.element.showType) == 'OFFICE'>
															<table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td1">${getHtmlText("office文档控件")}</td></tr></thead><tbody><tr><td class="datagird_tb_td1"></td></tr></tbody></table>
														</#if>
														</#if>
													</dd>
												</#if>
												<div></div>
											</li>
											<#else>
												<li class="form_design_ul_li" cellCode="${cellCode!}" onmousedown="ec.chooseLi(this)" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}"><dd></dd><div></div></li>
											</#if>
											</#list>
											<#-- 移动编辑视图配置，如果原节中单元格数为单数，自动补上一个单元格 -->
											<#-- <#if (secatt.cells)?size % 2 == 1>
												<li class="form_design_ul_li" cellCode="${ecCodeInit()}" onmousedown="ec.chooseLi(this)" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}"><dd></dd><div></div></li>
											</#if>-->
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
			</div>
			<div class="col-operate">
				<div>
					<ul id="main_form_tab_operate" >
						<li/>
						<li title="${getText('ec.view.pageProperty')}"><button class="Dialog_button btn_pointer" onclick="ec.cellPageProperty()" type="button">${getHtmlText('ec.view.pageProperty')}</button></li>
						<#--<li><hr class="line-icon"></hr></li>
						<li title="${getText('ec.view.addSection')}"><button class="Dialog_button btn_pointer" onclick="ec.addSection()" type="button">${getHtmlText('ec.view.addSection')}</button></li>
						<li title="${getText('ec.view.delSection')}" ><button class="Dialog_button btn_pointer" onclick="ec.delSection()" type="button">${getHtmlText('ec.view.delSection')}</button></li>
						
						<li title="${getText('ec.view.sectionattr')}"><button class="Dialog_button btn_pointer" onclick="ec.sectionProperty()" type="button">${getHtmlText('ec.view.sectionattr')}</button></li>
						
						<li><hr class="line-icon"></hr></li>-->
						<li title="${getText('ec.view.addline')}"><button class="Dialog_button btn_pointer" onclick="ec.addCell(1)" type="button">${getHtmlText('ec.view.addline')}</button></li>
						<li  title="${getText('ec.view.deleteline')}"><button class="Dialog_button btn_pointer" onclick="ec.delRow()" type="button">${getHtmlText('ec.view.deleteline')}</button></li>
						<li><hr class="line-icon"></hr></li>
						<li title="${getText('ec.view.splitcell')}"><button class="Dialog_button btn_pointer" onclick="ec.splitCell2()" type="button">${getHtmlText('ec.view.splitcellEdit')}</button></li>
						<li title="${getText('ec.view.delcell')}"><button class="Dialog_button btn_pointer" onclick="ec.delCell()" type="button">${getHtmlText('ec.view.delcell')}</button></li>
						<li title="${getText('ec.view.cellproperty')}"><button class="Dialog_button btn_pointer" onclick="ec.cellProperty()" type="button">${getHtmlText('ec.view.cellproperty')}</button></li>
						<#if view.assModel.entity.module.type??&&view.assModel.entity.module.type=="Mis">
						<li><hr class="line-icon"></hr></li>
						<li title="${getText('ec.view.addtab')}"><button class="Dialog_button btn_pointer" onclick="ec.addTab()" type="button">${getHtmlText('ec.view.addtab')}</button></li>
						<li title="${getText('ec.view.tabproperty')}"><button class="Dialog_button btn_pointer" onclick="ec.tabProperty()" type="button">${getHtmlText('ec.view.tabproperty')}</button></li>
						<li title="${getText('ec.view.toleft')}"><button class="Dialog_button btn_pointer" onclick="main_tab.moveTo('left')" type="button">${getHtmlText('ec.view.toleft')}</button></li>
						<li title="${getText('ec.view.toright')}"><button class="Dialog_button btn_pointer" onclick="main_tab.moveTo('right')" type="button">${getHtmlText('ec.view.toright')}</button></li>
						</#if>
					</ul>
				</div>
			</div>
			</div>
			<div class="mobile-design-btn" id="design-button" style="height:30px;width:100%;overflow:hidden;clear:both;text-align:center;padding-top:10px">
				<button class="Dialog_button btn_pointer" onclick="ec.save()" type="button">${getHtmlText('common.button.save')}</button>&#160;&#160;&#160;&#160;&#160;
				<button class="Dialog_button btn_pointer" onclick="ec.save(true)" type="button">${getHtmlText('common.button.saveexit')}</button>&#160;&#160;&#160;&#160;&#160;
				<button class="Dialog_button btn_pointer" onclick="ec.publish()" type="button">${getHtmlText('ec.view.publishview')}</button>&#160;&#160;&#160;&#160;&#160;
				<#--<button class="Dialog_button btn_pointer" onclick="ec.preview()" type="button">${getText('ec.view.preview')}</button>-->
			</div>
		
		</div>
		
		<div id="form_set_item_property_dlg" style="display:none">
			<@errorbar id="EditDialogErrorBar"></@errorbar>
			<input type="hidden" id="form_property_field_val" />
			<input type="hidden" id="form_prop_default_val" />
			<input type="hidden" id="form_property_default_val" />
			<input type="hidden" id="form_value_changed_val"  />
			<input type="hidden" id="form_showtype_changed_val"  />
			<input type="hidden" id="form_showformat_changed_val"  />
			<input type="hidden" id="form_precision_changed_val"  />
			<div id="dlg-div" class="dlg-etv-navset">
				<div class="etv-scrollbar" style="margin:0 5px;">
					<ul class="etv-nav" style="display: block;">
						<li class="selected" name="proertyTabs" id="base_attr">
							<span class="etv-nav-span">
								<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
							</span>
							<span class="etv-nav-span-r"></span>
						</li>
						<li name="proertyTabs" id="event_defined">
							<span class="etv-nav-span">
								<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
							</span>
							<span class="etv-nav-span-r"></span>
						</li>
						<li name="proertyTabs" id="validate_defined" style="display: none;">
							<span class="etv-nav-span">
								<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.validation')}</em>
							</span>
							<span class="etv-nav-span-r"></span>
						</li>
						<li name="proertyTabs" id="css_defined">
							<span class="etv-nav-span">
								<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.style')}</em>
							</span>
							<span class="etv-nav-span-r"></span>
						</li>
					</ul>
				</div>
			</div>
			<table id="tab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px;display:block">
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
				<tr id="selectDisplayTypeTR">
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.cell.fieldType')}</td>
					<td class="co" width="30%">
						<select id="form_property_column_type" class="cui-edit-field" style="width:100%;display:none"></select>
						<input type="text" id="form_property_column_type_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%"/>
					</td>
					<td class="la" width="20%" name="td_showType" align="right" padding-right="10px">${getHtmlText('ec.view.cell.showType')}</td>
					<td class="co" width="30%" name="td_showType">
						<select id="form_property_field_type" class="cui-edit-field" style="width:100%"></select>
						<input type="text" id="form_property_field_type_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
					</td>
				</tr>
				<tr id="tr_showFormat">
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.cell.showFormat')}</td>
					<td class="co" width="30%">
						<select id="form_property_show_format" class="cui-edit-field" style="width:100%" onChange="_showFormatChangeEvent(this)"></select>
						<input type="text" id="form_property_show_format_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
					</td>
					<td class="la id='iscomplexlabletd' complex-td" width="20%" align="right" padding-right="10px" ><label id="iscomplexLable">${getHtmlText('ec.view.iscomplexlable')}</label></td>
					<td id='complexlabletd' class="co complex-td" width="30%">
						<input type="checkbox" id="form_property_is_complex" />
						<select id="selectcomp_type"  class="cui-edit-field" style="display:none;width:100%">
						</select>
					</td>
				</tr>
				<tr id="defaultTR" style="display:none">
					<td class="la" width="20%" align="right" padding-right="10px">
						<div id="form_defaultValue_div" style="width:100%">
							${getHtmlText('ec.property.default.td')}
						</div>
					</td>
					<td class="co" width="80%" colspan="3">
						<div id="form_text_defaultValue_div" style="display:none;width:100%">
							<input type="text" id="form_text_defaultValue" class="cui-edit-field" style="width:100%" onChange="_changeDefaultValue(this)" />
						</div>
						<div id="form_textarea_defaultValue_div" style="display:none;width:100%">
							<textarea id="form_textarea_defaultValue" class="cui-edit-textarea" rows="3" style="width:100%" onChange="_changeDefaultValue(this)" ></textarea>
						</div>
						<div id="dateRadio_div" style="display:none;width:100%">
							<input type="radio" name="dateRadio" value="today" onClick="_changeDefaultValue(this)">${getHtmlText('ec.property.default.today')}&nbsp;</input>
							<input type="radio" name="dateRadio" value="firstday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.firstdayOfMonth')}&nbsp;
							<input type="radio" name="dateRadio" value="lastday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.lastdayOfMonth')}&nbsp;
							<input type="radio" name="dateRadio" value="nextsevenday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.later')}&nbsp;
						</div>
						<div id="dateTimeRadio_div" style="display:none;width:100%">
							<input type="radio" name="dateTimeRadio" value="currentTime" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currenttime')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="firstday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.firstdayOfMonth')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="lastday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.lastdayOfMonth')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="nextsevenday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.later')}&nbsp;
						</div>
						<div id="ck_boolean_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_boolean" value="true" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.yes')}
						</div>
						<div id="ck_currentUser_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentUser" value="currentUser" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentStaff')}
						</div>
						<div id="ck_currentDepart_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentDepart" value="currentDepart" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentDepartment')}
						</div>
						<div id="ck_currentPost_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentPost" value="currentPost" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentPosition')}
						</div>
						<div id="ck_currentComp_div" style="display:none;width:100%">
							<input type="checkbox" id="ck_currentComp" value="currentComp" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentCompany')}
						</div>
						<div id="ck_syscode_div" style="display:none;width:100%">
							<select id="ck_syscode" name="ck_syscode" class="cui-edit-field" style="width:100%" onChange="_changeDefaultValue(this)"></select>
						</div>
						<script type="text/javascript">
							var ckradio = null;
							function _changeDefaultValue(obj){
								if(obj.type=="text"){
									CUI('#form_property_default_val').val(CUI(obj).val());
								}else if(obj.type=="radio"){
									if(obj==ckradio){
										obj.checked=false;
										ckradio = null;
										CUI('#form_property_default_val').val('');
									}else{
										ckradio = obj;
										CUI('#form_property_default_val').val(CUI(obj).val());
									}
								}else if(obj.type=="checkbox"){
									if(obj.id=="ck_boolean"){
										if(obj.checked){
											CUI('#form_property_default_val').val(CUI(obj).val());
										}else{
											CUI('#form_property_default_val').val("false");
										}
									}else if(obj.id=="ck_currentUser" || obj.id=="ck_currentDepart" || obj.id=="ck_currentPost" || obj.id=="ck_currentComp"){
										if(obj.checked){
											CUI('#form_property_default_val').val(CUI(obj).val());
										}else{
											CUI('#form_property_default_val').val("");
										}
									}
								}else{
									CUI('#form_property_default_val').val(CUI(obj).val());
								}
								if(CUI('#form_property_default_val').val()!=CUI('#form_prop_default_val').val()){
									CUI('#form_value_changed_val').val(true);
								}
							}
							function _showFormatChangeEvent(obj){
								var obj_val = CUI(obj).val();
								if($('#form_property_column_type').val()=='DATE' || $('#form_property_column_type').val()=='DATETIME'){
									if(obj_val=='YM' || obj_val=='Y'){
										$('#defaultTR').hide();
										$('#dateRadio_div').hide();
									}else{
										$('#defaultTR').hide();//移动端不支持默认值
										$('#dateRadio_div').hide();//移动端不支持默认值
									}
								}
							}
						</script>
					</td>
				</tr>
				<tr id="tr_common">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="form_property_alignLable">${getHtmlText('ec.view.align')}</label></td>
					<td class="co" width="30%">
						<select id="form_property_align">
							<option value="left">${getText('ec.view.left')}</option>
							<option value="center">${getText('ec.view.center')}</option>
							<option value="right">${getText('ec.view.right')}</option>
						</select>
					</td>
					
					
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isCreateNew">${getHtmlText('ec.view.isCreateNew')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_createNew" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isNoCopy">${getHtmlText('ec.view.isNoCopy')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_noCopy" /></td>
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="saveTemplate">${getHtmlText('ec.view.saveTemplate')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_saveTemplate" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="openEmptyDoc">${getHtmlText('ec.view.openEmptyDoc')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_openEmptyDoc" /></td>
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isOfficeSign">${getHtmlText('ec.view.isOfficeSign')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_officeSign" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isOfficeHandSign">${getHtmlText('ec.view.isOfficeHandSign')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_officeHandSign" /></td>
				</tr>
				<tr class="tr-office sign-url">
					<td class="la" width="20%" align="right" padding-right="10px">
						<label>${getHtmlText('ec.view.signUrl')}</label>
					</td>
					<td class="co" width="80%" colspan="3">
						<input type="text" id="signUrl" class="cui-edit-field" style="width:100%" />
					</td>
				</tr>
				<tr class="tr-office handSign-url">
					<td class="la" width="20%" align="right" padding-right="10px">
						<label>${getHtmlText('ec.view.handSignUrl')}</label>
					</td>
					<td class="co" width="80%" colspan="3">
						<input type="text" id="handSignUrl" class="cui-edit-field" style="width:100%" />
					</td>
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="officeOpenType">${getHtmlText('ec.view.officeOpenType')}</label></td>
					<td class="co" width="30%">
						<select id="form_property_is_officeOpenType"  class="cui-edit-field" style="width:100%">
							<option value="doc">${getText('ec.view.doc')}</option>
							<option value="pdf">${getText('ec.view.pdf')}</option>
						</select>
					</td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="officeSaveType">${getHtmlText('ec.view.officeSaveType')}</label></td>
					<td class="co" width="30%">
						<select id="form_property_is_officeSaveType"  class="cui-edit-field" style="width:100%">
							<option value="doc">${getText('ec.view.doc')}</option>
							<option value="pdf">${getText('ec.view.pdf')}</option>
						</select>
					</td>
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="isRevision">${getHtmlText('ec.view.isRevision')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_isRevision" /></td>
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="showRevision">${getHtmlText('ec.view.showRevision')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_showRevision" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="hideRevision">${getHtmlText('ec.view.hideRevision')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_hideRevision" /></td>
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="getRevisions">${getHtmlText('ec.view.getRevisions')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_getRevisions" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="acceptRevisions">${getHtmlText('ec.view.acceptRevisions')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_acceptRevisions" /></td>
				</tr>
				<tr class="convert">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="convertPdfOnLine">${getHtmlText('ec.view.convertPdfOnLine')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_convertPdfOnLine" /></td>
				</tr>
				<tr class="tr-office">
					<td class="la" width="20%" align="right" padding-right="10px"><label id="officePrint">${getHtmlText('ec.view.officePrint')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_officePrint" /></td>
					<td class="la" width="20%" align="right" padding-right="10px"><label id="officeNotNull">${getHtmlText('ec.view.officeNotNull')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_officeNotNull" /></td>
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
				<tr id="tr_readonly">
					<td class="la" width="20%" align="right" padding-right="10px"><label for="form_property_is_readonly" id="isreadOnlyLable">${getHtmlText('ec.view.isreadonly')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_readonly" /></td>
					<td class="la nullable-td" width="20%" align="right" padding-right="10px"><label for="form_property_is_nullable" id="isnullableLable">${getHtmlText('ec.view.isnullable')}</label></td>
					<td class="co nullable-td" width="30%"><input type="checkbox" id="form_property_is_nullable" /></td>
				</tr>
				<tr>
					<td class="la couple-td" width="20%" align="right" padding-right="10px"><label for="couple">${getHtmlText('ec.view.common.couple')}</label></td>
					<td class="co couple-td" width="30%"><input type="checkbox" id="couple" /></td>
					<#--<td class="la" width="20%" align="right" padding-right="10px" name="td_control"><label for="form_property_is_control" id="isControlLable">${getHtmlText('ec.view.iscontrol')}</label></td>
					<td class="co" width="30%" name="td_control"><input type="checkbox" id="form_property_is_control" /></td>-->
					<#--<td class="la" width="20%" align="right" padding-right="10px"><label id="isDeleteLable">${getText('ec.view.isdelete')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_delete" /></td>-->
				</tr>
				<tr id="tr_picSize" style="display:none;visibility: hidden;">
					<td class="la" width="20%">${getHtmlText('ec.property.width')}</td>
					<td class="co" width="30%"><input type="text" id="form_pic_width" class="cui-edit-field" style="width:100%" /></td>
					<td class="la" width="20%">${getHtmlText('ec.property.height')}</td>
					<td class="co" width="30%"><input type="text" id="form_pic_height" class="cui-edit-field" style="width:100%" /></td>
				</tr>
				<tr id="form_tr_isgroup">
					<td class="la" width="20%" align="right" padding-right="7px"><label for="form_property_is_group" id="isGroupLable">${getHtmlText('ec.view.isgroup')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="form_property_is_group" /></td>
				</tr>
				<tr id="form_tr_allow_view" style="display:none;">
					<td class="la" width="20%"><label for="allowView">${getHtmlText('ec.view.allowView')}</label></td>
					<td class="co" width="30%"><input type="checkbox" id="allowView" value="true" onclick="changeAllowView(this);"/></td>
					<td class="la" width="20%" id="form_tr_allow_view_td1" style="display:none;">${getHtmlText('ec.business.view.choose')}</td>
					<td class="co" width="30%" id="form_tr_allow_view_td2" style="display:none;">
						<select id="allowViewSelect" class="cui-edit-field" style="width:100%">
						</select>
					</td>
				</tr>
				<tr id="ref_condition" style="height:30px">
					<td class="la" width="20%" align="right" padding-right="7px"><label id="refConditionLabel">${getHtmlText('ec.view.refCondition')}</label>
					<span id="refConditionHelpinfo" class="baphelp-icon"></span>
<div id="refConditionHelpinforef" style="display:none">
<p class="baphelp-info">当单元属性为对象类型，可传自定义条件给参照页面。 </p>
<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>return "year=2008 &amp;entity=crossCom "</span></code></pre>
</div>
<p class="baphelp-hint">注意：该功能需要参照列表的表格属性配置自定义条件，具体见其帮助说明。</p>
</div>
					<script type="text/javascript">
							$('#refConditionHelpinfo').helptip({refElm: "#refConditionHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
						</script>
					</td>
					<td class="co" colspan="3">
						<textarea id="ref_condition_content" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea>
					</td>
				</tr>
				<tr id="form_tr_precision">
					<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.property.decimalNum')}</label></td>
					<td class="co" width="30%"><input type="text" id="precision" class="cui-edit-field" /></td>
				</tr>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">
						<div id="form_textarea_auto_resize_div_label" style="width:100%;visibility: hidden;">
							${getHtmlText('ec.property.auto.resize.td')}
						</div>
					</td>
					<td class="co" width="80%" colspan="3">
						<div id="form_textarea_auto_resize_div_checkbox" style="display:none;width:100%;visibility: hidden;">
							<input type="checkbox" id="form_property_auto_resize" />
						</div>
					</td>
				</tr>
			</table>
			<div id="div-event" style="width:100%;align:left;height:450px;overflow-y:auto;display:none">
				<table id="tab-event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
					<tr>
						<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.eventType')}</label></td>
						<td class="co" width="30%">
							<select id="events-select" class="cui-edit-field" style="width:100%">
							</select>
						</td>
						<td class="la" width="20%" style="text-align:left;padding-left:10px">
							<img onclick="ec.addnewevent($('#events-select'),$('#tab-event1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
						</td>
						<td class="la" width="30%"></td>
					</tr>
					<tr>
						<td colspan="4">
							<table id="tab-event1" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;">
								<tr style="height:0px">
									<td class="la" style="width:15%;height:0px;text-align:right"></td>
									<td style="width:35%;height:0px" class="co"></td>
									<td class="la" style="width:45%;height:0px;text-align:right"></td>
									<td style="width:5%;height:0px" class="co"></td>
								</tr>
							</table>
						</td>
					</tr>
				</table>
			</div>
			<table id="tab-validate" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;display:none">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.validatorType')}</label></td>
					<td class="co" width="30%">
						<select id="validate-select" class="cui-edit-field" style="width:100%">
						</select>
					</td>
					<td class="la" width="20%" style="text-align:left;padding-left:10px">
						<img onclick="ec.addValidate($('#validate-select'),$('#tab-validate1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</td>
					<td class="la" width="30%"></td>
				</tr>
				<tr>
					<td colspan="4">
						<div id="div-validate" style="width:100%;align:left;height:390px;overflow-y:auto">
							<table id="tab-validate1" cellpadding="0" cellspacing="0" border="0" width="100%" style="margin-top: 10px;">
								<tr style="height:0px">
									<td class="la" style="width:15%;height:0px;text-align:right"></td>
									<td style="width:35%;height:0px" class="co"></td>
									<td class="la" style="width:45%;height:0px;text-align:right"></td>
									<td style="width:5%;height:0px" class="co"></td>
								</tr>
							</table>
						</div>
					</td>
				</tr>
			</table>
			<table id="tab-style" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="display:none">
				<tr style="height:0px">
					<td style="width:20%;height:0px;text-align:right" class="la"></td>
					<td class="co" style="width:30%;height:0px"></td>
					<td style="width:20%;height:0px;text-align:right" class="la"></td>
					<td class="co" style="width:30%;height:0px"></td>
				</tr>
				<tr>
					<td class="la" style="width:20%" align="right" padding-right="10px">${getHtmlText('ec.view.tdcustomstyle')}
					<span id="comstyleHelpinfo" class="baphelp-icon"></span>&#160;
<div id="comstyleHelpinforef" style="display:none">
	<p class="baphelp-info">内部控件的自定义样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式。</p>
</div>
					<script type="text/javascript">
							$('#comstyleHelpinfo').helptip({refElm:"#comstyleHelpinforef", html: true , isCustom :false, width: 180 , title :"说明"});
						</script></td>
					<td class="co" style="width:80%"><textarea id="form_property_field_style_com" class="cui-edit-textarea" style="width:100%;height:100px;margin-top:5px"></textarea></td>
				</tr>
				
			</table>
		</div>
		<div id="form_set_select_val" style="display:none;">
			<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.type')}</td>
					<td class="co" width="30%">
						<select id="form_set_select_val_type" onchange="ec.changeFillValType(this)" class="cui-edit-field" style="width:100%">
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
			<div>
	 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr style="visibility: hidden;">
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
					<td class="co"  width="30%">${getHtmlText('ec.view.everyline')}<input type="text" id="layout_col_num" class="cui-edit-field" style="width:50px" /> ${getHtmlText('ec.view.colum')}</td>
					<td class="la" width="20%" align="right" padding-right="10px"></td>
					<td class="co" width="30%"></td>
				</tr>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.pageload')}<br/>(onLoad)<span id="onLoadHelpinfo" class="baphelp-icon"></span>
<div id="onLoadHelpinforef" style="display:none">
	<p class="baphelp-info">页面加载的时候执行</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>alert("示例");
</code></pre>
</div>
<p class="baphelp-hint">注意：该时刻，DataGrid或DataTable可能还未加载完，不可再对这两个对象进行操作</p>
</div>
					<script type="text/javascript">
							$('#onLoadHelpinfo').helptip({refElm: "#onLoadHelpinforef", html: true , isCustom :false, width: 480 , title :"说明"});
						</script>
					</td>
					<td class="co" colspan="3"><textarea id="onloadevent" class="cui-edit-textarea" style="width:100%;height:100px;"></textarea></td>
				</tr>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.saveevent')}&#160;&#160;&#160;<br/>(onSave)<span id="onSaveHelpinfo" class="baphelp-icon"></span>
<div id="onSaveHelpinforef" style="display:none">
	<p class="baphelp-info">页面保存或提交的时候执行</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>var type = $("#operateType").val();
if(type =='submit'){//提交
	alert("这是示例");
}else if(type =='save'){//保存
	alert("这是示例");
}
var type1=$("input[name='workFlowVarStatus']").val();
if(type1 =='reject'){//驳回
	alert("这是示例");
}else if(type1 =='cancel'){//作废
	alert("这是示例");
}
</code></pre>
</div>
<p class="baphelp-hint">注意：只有表单才有operateType属性</p>
</div>					
					<script type="text/javascript">
							$('#onSaveHelpinfo').helptip({refElm:"#onSaveHelpinforef", html: true , isCustom :false, width: 330 , title :"说明"});
						</script>
					</td>
					<td class="co" colspan="3"><textarea id="onsaveevent" class="cui-edit-textarea" style="width:100%;height:100px;margin-top:5px;"></textarea></td>
				</tr>
			</table>
			</div>
			<div style="float:left;width:100%;">
			<fieldset style="border:1px solid #A5C7EF;width:90%;margin-left:18px;">
			    <legend>Groovy ${getText('ec.view.groovyscript')}</legend>
			    <table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px">
			    <tr>
					<td class="la" width="20%" align="right" padding-right="10px">BeforeSave</td>
					<td class="co" width="30%">
						<@selector name="beforeSave" id="beforeSave" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
							openType="page" closePage="true" pageType="script" onclick="selectScriptCode('beforeSave')"/>
					</td>
					<td class="la" width="20%" align="right" padding-right="10px">AfterSave</td>
					<td class="co" width="30%">
						<@selector name="afterSave" id="afterSave" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
							openType="page" closePage="true" pageType="script" onclick="selectScriptCode('afterSave')"/>
					</td>
				</tr>
				<#if view.assModel.entity.workflowEnabled?default(false) && view.assModel.isMain?? && view.assModel.isMain>
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px">BeforeSubmit</td>
					<td class="co" width="30%">
						<@selector name="beforeSubmit" id="beforeSubmit" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
							openType="page" closePage="true" pageType="script" onclick="selectScriptCode('beforeSubmit')"/>
					</td>
					<td class="la" width="20%" align="right" padding-right="10px">AfterSubmit</td>
					<td class="co" width="30%">
						<@selector name="afterSubmit" id="afterSubmit" cssClass="cui-noborder-input" cssStyle="width:100%" value=""
							openType="page" closePage="true" pageType="script" onclick="selectScriptCode('afterSubmit')"/>
					</td>
				</tr>
				</#if>
				</table>
 		 	</fieldset>
 		 	</div>
	 	</div>
	 	<div id="section_setting_dlg" style="display:none;height:280;overflow-y:auto;">
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
					<td class="la" align="right" padding-right="10px">${getHtmlText('ec.view.customstyle')}<span id="customStyleHelpinfo" class="baphelp-icon"></span>&#160;
<div id="customStyleHelpinforef" style="display:none">
	<p class="baphelp-info">自定义样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式。</p>
</div>
					<script type="text/javascript">
							$('#customStyleHelpinfo').helptip({refElm: "#customStyleHelpinforef", html: true , isCustom :false, width: 180 , title :"说明"});
						</script></td>
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
					<#-- international_tabname    international_tabname_showName -->
					<@international name="tab.name" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
					<#-- <input type="text" id="tab_name" class="cui-edit-field" style="width:90px" /> --></td>
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
							DATAGRID:<option value="${asso.originalProperty.code}">${getText('${asso.originalProperty.model.name}')}[${getText('${asso.originalProperty.displayName}')}]</option>
							</#list>
						</#if>
						</select>
					</td>
				</tr>
			</table>
	 	</div>
	 	<div id="groupSelectPeople_dlg" style="display:none;OVERFLOW-Y:auto;">
	 		<input type="hidden" id="selectPeopleDel" />
	 		<table id="selectPeopleTable" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;margin-left: 10px;">
				<tr>
					<td class="la" width="20%">
						<input name="groupname" type="text" size="4" value="分组名称"/>
					</td>
					<td class="co" width="30%">
						<div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)">
							<a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a>
							<a class="cui-btn-r">&nbsp;</a>
						</div>
						<input name="staffName" type="text" size="5" readonly="true" />
						<input name="selectStaffId" type="hidden"/>
						<input name="uid" type="hidden"/>
					</td>
					<td class="la" width="20%">
						<input name="sort" type="text" size="1" value="1"/>
					</td>
					<td class="co" width="30%">
						<img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" />
						<img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" />
					</td>
				</tr>
				<#-- 
				<tr>
					<td colspan="4">
						<div id="div-validate" style="width:100%;align:left;height:390px;overflow-y:auto">
							<table id="tab-validate1" cellpadding="0" cellspacing="0" border="0" width="100%" style="margin-top: 10px;">
								<tr style="height:0px">
									<td class="la" style="width:15%;height:0px;text-align:right"></td>
									<td style="width:35%;height:0px" class="co"></td>
									<td class="la" style="width:45%;height:0px;text-align:right"></td>
									<td style="width:5%;height:0px" class="co"></td>
								</tr>
							</table>
						</div>
					</td>
				</tr>
				-->
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
				<tr id="staffSelectType">
					<td class="la" width="30%" align="right" style="vertical-align:middle;">选择类型</td>
					<td class="co" width="60%">
						<label for="company" class="">本公司</label><input class="" id="company" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="company" onclick="staffSelectTypeRadio(this);" checked="true"/>&nbsp;&nbsp;
						<label for="crossCompany" class="">跨公司</label><input class="" id="crossCompany" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="crossCompany" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
						<label for="department" class="">本部门</label><input class="" id="department" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="department" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
						<label for="departmentAndSub" class="">本部门及下级</label><input class="" id="departmentAndSub" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="departmentAndSub" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
						<label for="customer" class="">自定义</label><input class="" id="customer" name="staffSelectTypeRadio" style="vertical-align:middle;" type="radio" value="customer" onclick="staffSelectTypeRadio(this);"/>&nbsp;&nbsp;
					</td>
				</tr>
				<tr id="staffLimit" style="display:none;">
					<td class="la" width="30%" align="right">${getHtmlText('选择范围')}</td>
					<td class="co" width="60%">
						<div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="showSelectPeopleSettingDialog()">
							<a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="配置范围">配置范围</span></a>
							<a class="cui-btn-r">&nbsp;</a>
						</div>
						<#-- <@mneclient reftitle="${getText('foundation.ec.entity.view.reference')}"   name="staffLimit" id="staffLimit" type="Staff" url="/msService/ec/foundation/staff/common/staffListFrameset" displayFieldName="name"  ids="${(weituo.weituoGuanlianrenmultiselectIDs)!}" names="${(weituo.weituoGuanlianrenmultiselectNames)!}" onkeyupfuncname="getstaffLimitMultiInfo()"   clicked=true multiple=true cssStyle="padding-left: 0px; width: 100%;" mnewidth=260 isCrossCompany=false isEdit=true /> -->
					</td>
				</tr>
				<tr id="associated_2">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.choiceRef')}</td>
					<td class="co" width="60%">
					<select id="ec_reference_view" style="width:100%"></select>
					</td>
				</tr>
				<tr id="associated_2">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.showfield')}</td>
					<td class="co" width="60%">
					<div name="property" class="fix-search-click" style="width:95%">
					<input id="ec_select_property" type="hidden">
					<input size="15" id="ec_select_property_displayName" class="cui-edit-field">
					<input type="button" onclick="ec_view_select_property.selectProperty(event, this)"  value="" class="cui-search-click">
				</div>
					</td>
				</tr>
			
				<tr id="ec_tr_nullable">
					<td class="la" width="30%" align="right"><label for="ec_checkbox_nullable">${getHtmlText('ec.view.isnullable')}</label></td>
					<td class="co" width="60%">
					<input type="checkbox" id="ec_checkbox_nullable" value="true">
					</td>
				</tr>
				<#-- 
				<tr>
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.isgroup')}</td>
					<td class="co" width="60%">
						<input type="checkbox" id="form_property_is_group_multi" />
					</td>
				</tr>
				-->
				<tr>
					<td class="la" width="30%" align="right"><label for="ec_is_readonly">${getHtmlText('ec.view.isreadonly')}</label></td>
					<td class="co" width="60%">
						<input type="checkbox" id="ec_is_readonly" />
					</td>
				</tr>
				<tr >
					<td class="la" width="30%" align="right"><label for="multAllowView">${getHtmlText('ec.view.allowView')}</label></td>
					<td class="co" width="60%">
						<input type="checkbox" id="multAllowView" onclick="changeMultAllowView(this);"/>
					</td>
				</tr>
				<tr id="multAllowViewTR" style="display:none;">
					<td class="la" width="30%" align="right">${getHtmlText('ec.business.view.choose')}</td>
					<td class="co" width="60%">
						<select id="allowMultViewSelect"></select>
					</td>
				</tr>
			</table>
	 	</div>
	 	<div id="supervision_setting_dlg" style="display:none;">
	 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr id="associated_2">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.choiceRef')}</td>
					<td class="co" width="60%">
					<select id="supervision_ec_reference_view" style="width:100%"></select>
					</td>
				</tr>
				<tr id="supervision_ec_tr_mneenable">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.mneenable')}</td>
					<td class="co" width="60%">
					<input type="checkbox" id="supervision_ec_checkbox_mneenable" value="true" checked>
					</td>
				</tr>
				<#--
				<tr id="ec_tr_nullable">
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.isnullable')}</td>
					<td class="co" width="60%">
					<input type="checkbox" id="ec_checkbox_nullable" value="true">
					</td>
				</tr>
				-->
				<tr>
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.isgroup')}</td>
					<td class="co" width="60%">
						<input type="checkbox" id="supervision_form_property_is_group_multi" />
					</td>
				</tr>
				<tr>
					<td class="la" width="30%" align="right">${getHtmlText('ec.view.isreadonly')}</td>
					<td class="co" width="60%">
						<input type="checkbox" id="supervision_ec_is_readonly" />
					</td>
				</tr>
			</table>
	 	</div>
	 	<div id="datagrid_property_dlg" style="display:none;">
	 		<iframe id="datagird_property_frame" name="datagird_property_frame" width="100%" height="450px" style="overflow:hidden;" SCROLLING="no" MARGINHEIGHT="0" MARGINWIDTH="0" frameborder="0" border="0" framespacing="0"></iframe>
	 	</div>
	 	<table id="table_validate_example_row" style="display:none;">
			<tr id="tr_example">
				<td style="width:100%;text-align:right;" class="la">
					<table style="border: 1px solid #DDD;padding: 0 10px;margin: 10px 0 0 10px;width: 100%;">
						<tr>
							<td style="width:20%;text-align:right" class="la">
								<b class="displayLabel"</b>
							</td>
							<td class="co" style="text-align: left;width:30%;">
								<img class="deleteImg" style="cursor: pointer;" src="/bap/static/foundation/images/icon-del.gif">
							</td>
							<td style="width:15%;text-align:right" class="la"></td>
							<td class="co" style="width:35%;"></td>
						</tr>
						<tr class="rangeExampleRow">
							<td style="text-align:right" class="la">
							</td>
							<td class="co">
								<div class="fix-input"><div class="fix-search-click clearfix">
									<input class="cui-noborder-input" name="min" type="text">
								</div></div>
							</td>
							<td style="width:15%;text-align:right" class="la">
								
							</td>
							<td class="co" style="width:35%;">
								<div class="fix-input"><div class="fix-search-click clearfix">
									<input class="cui-noborder-input" name="max" type="text">
								</div></div>
							</td>
						</tr>

						<tr class="stringlengthExampleRow">
							<td style="text-align:right" class="la">
							</td>
							<td class="co">
								<div class="fix-input"><div class="fix-search-click clearfix">
									<input class="cui-noborder-input" name="minLength" type="text">
								</div></div>
							</td>
							<td style="width:15%;text-align:right" class="la">
								
							</td>
							<td class="co" style="width:35%;">
								<div class="fix-input"><div class="fix-search-click clearfix">
									<input class="cui-noborder-input" name="maxLength" type="text">
								</div></div>
							</td>
						</tr>

						<tr class="regexExampleRow">
							<td style="text-align:right" class="la">
							</td>
							<td class="co" colspan="3">
								<div class="fix-input">
									<input class="cui-noborder-input" name="regex" type="text">
								</div>
							</td>
						</tr>
						<tr class="regexExampleRow">
							<td style="width:20%;text-align:right" class="la">
							</td>
							<td class="co" colspan="3">
								<div class="fix-input">
									<#-- international_errorMsg    international_errorMsg_showName -->
									<@international name="errorMsg" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-noborder-input" />
								</div>
							</td>
						</tr>

						<tr class="compareExampleRow">
							<td style="text-align:right" class="la">
							</td>
							<td class="co">
								<select class="noborder" name="operator"  style="width:100%">
									<option value="==">${getText('ec.view.config.validate.equal')}</option>
									<option value="!=">${getText('ec.view.config.validate.notequal')}</option>
									<option value=">">${getText('ec.view.config.validate.great')}</option>
									<option value=">=">${getText('ec.view.config.validate.greatequal')}</option>
									<option value="<">${getText('ec.view.config.validate.little')}</option>
									<option value="<=">${getText('ec.view.config.validate.littlequal')}</option>
								</select>
							</td>
							<td style="width:15%;text-align:right" class="la">
								
							</td>
							<td class="co" style="width:35%;">
								<select class="noborder" name="another" style="width:100%">
								</select>
							</td>
						</tr>

						<tr class="customExampleRow">
							<td style="width:20%;text-align:right" class="la">
								
							</td>
							<td class="co" colspan="3">
								<div class="fix-input fix-ie7-textarea">
									<textarea style="height: 85px;width: 99.8%;" name="jsCode" class="cui-noborder-textarea" value=""></textarea>
								</div>
							</td>
						</tr>

						<tr class="customExampleRow">
							<td style="width:20%;text-align:right" class="la">
								
							</td>
							<td class="co" colspan="3">
								<div class="fix-input">
									<#-- international_errorMsg    international_errorMsg_showName -->
									<@international name="errorMsg" isNew=true isOldEdit=true isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-noborder-input" />
								</div>
							</td>
						</tr>
					</table>
				</td>
			</tr>
			</table>
			<div id="button_property_dlg" style="display:none">
				<@errorbar id="ButtonDialogErrorBar"></@errorbar>
				<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
					<tr>
						<td class="la" width="20%" style="color: rgb(179, 3, 3);" align="right" padding-right="10px">${getHtmlText('ec.view.identiy')}</td>
						<td class="co" width="30%"><input type="text" id="button_property_id" class="cui-edit-field" style="width:100%" /></td>
						<td class="la" width="20%" style="color: rgb(179, 3, 3);" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
						<td class="co" width="30%">
						<@international name="buttonProperty.showName" moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
						</td>
					</tr>
					<tr id="permissionTr">
						<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.ispower')}</td>
						<td class="co" width="30%"><input type="checkbox" id="isPermission" /></td>
						<td class="la" width="20%" style="color: rgb(179, 3, 3);" id="td_permission_label" align="right" padding-right="10px">${getHtmlText('ec.edit.permission.code')}</td>
						<td class="co" width="30%" id="td_permission_code">
							<input type="text" id="button_permission_code" class="cui-edit-field" style="width:100%" />
						</td>
					</tr>
					<tr id="urlTr" style="display:none;">
						<td class="la" width="30%" style="color: rgb(179, 3, 3);" align="right" padding-right="10px">${getHtmlText('ec.view.powerurl')}</td>
						<td class="co" colspan="3">
							<input type="text" id="operateUrl" class="cui-edit-field" style="width:100%" />				
						</td>
					</tr>
					<tr id="permissionMemo" style="display:none;">
						<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.edit.button.memo')}</td>
						<td class="co" colspan="3">
							<span>${getText('ec.edit.operateCode.rule')}</span>				
						</td>
					</tr>
					<tr>
						<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.ishidden')}</td>
						<td class="co" width="30%"><input type="checkbox" id="isOperateHide" /></td>
						<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.edit.button.align')}</td>
						<td class="co" width="30%">
						<select id="buttonAlign" style="width:100%;">
							<option value="left">${getText('ec.edit.button.align.left')}</option>
							<option value="right">${getText('ec.edit.button.align.right')}</option>
						</select>
						</td>
					</tr>
					<tr id="eventTr">
						<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.edit.button.onclick')}
						<span id="functionHelpinfo" class="baphelp-icon"></span>
<div id="functionHelpinforef" style="display:none">
	<p class="baphelp-info">单击事件，点击框内时触发</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function onclickPosition(){
	console.log("onclick");
}</code></pre>
</div>
<p class="baphelp-hint">注意：必须要有function</p>
</div>
					<script type="text/javascript">
							$('#functionHelpinfo').helptip({refElm: "#functionHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
						</td>
						<td class="co" colspan="3">
							<textarea id="customfuncarea" class="cui-edit-textarea" style="width:100%;height:170px;"></textarea>
						</td>
					</tr>
				</table>
			</div>
			<div id="operatorBar_setting_dlg" style="display:none;">
		 		<@errorbar id="operatorBarDialogErrorBar"></@errorbar>
		 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px">
					<tr>
						<td class="la" width="10%" style="padding-right:10px;">${getHtmlText('ec.view.classname')}</td>
						<td class="co" width="40%"><input type="text" id="cssName" title="${getText('ec.view.whatclassnamefor')}"/></td>
					</tr>
				</table>
	 		</div>
	</body>
	<script type="text/javascript">	
	
		//自动设置中间编辑区域的宽度
		function _init_size(){
			$('.col-main').width($('.col-mobile-mian').width()-450);
			$('.col-main').height($(window).height()-83);
			$('.col-sub').height($(window).height()-83);
			var paneH= $('#form_select_elements').height()- $('#form_select_elements h2').outerHeight(true)*$('#form_select_elements h2').length;
			$('#form_select_elements .accordion_pane').height(paneH);	
			//$('#layoutTree').height($(".col-sub .treeBox").height()-46);
			$('.col-operate').height($(window).height()-83);
			$('#form_area .etv-navset').height($(window).height()-83);
			$('#form_area .etv-navset').width($('.col-main').width());
			<#if view.assModel.entity.module.type??&&view.assModel.entity.module.type=="Mis">
			$('#form_area .etv-content').height($(window).height()-243);
			<#else>
			$('#form_area .etv-content').height($(window).height()-205);
			</#if>
			$('#form_area .etv-content').width($('.col-main').width()-10);
			$('#form_area .etv-content > .tab-pane').height($('#form_area .etv-content').height()-10);
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
			if($(".etv-content-mobile-style-edit .etv-scrollbar li a").length<=1){
				$(".etv-content-mobile-style-edit .etv-scrollbar li a").hide();
			}
			ec.multselect_model_type_changed(CUI('#multselect_model_type')[0]);
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
			            $('#tab-validate').css('display','none');
			            $('#tab-style').css('display','none');
			        }else if(objId === 'event_defined'){
			            $('#tab-common').css('display','none');
			            $('#div-event').css('display','block');
			            $('#tab-validate').css('display','none');
			            $('#tab-style').css('display','none');
			        }else if(objId === 'validate_defined'){
			            $('#tab-common').css('display','none');
			            $('#div-event').css('display','none');
			            $('#tab-validate').css('display','block');
			            $('#tab-style').css('display','none');
			        }else if(objId === 'css_defined'){
			            $('#tab-common').css('display','none');
			            $('#div-event').css('display','none');
			            $('#tab-validate').css('display','none');
			            $('#tab-style').css('display','block');          
			        }
			    });
			});
		});
		$(window).resize(function(){
			//等页面渲染完重新计算高度
			setTimeout(function(){
			_init_size();	
			});				
		});
		
		function selectScriptCode(code){
			var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
			<#if view?exists&&view.entity?exists>
				var param="entityCode=${view.entity.code}";
			<#elseif entity?? && entity.code??>
				var param="entityCode=${entity.code}";
			</#if>
			foundation.common.callOpenWeb("script",windowStyle,null,true,null,code,param);
			
		}
		
		function setScript(objArr){
			var code =objArr[0].scriptCode;
			this.val(code);
			//CUI('#scriptCode').val(code);
		}
	</script>
	<script type="text/javascript">
	var ec_view_select_property = {};
	$('body').unbind('click.overlayer').bind('click.overlayer', function(){
		if(ec_view_select_property.showOverLayerDiv && ec_view_select_property.showOverLayerDiv.isShow) {
			ec_view_select_property.showOverLayerDiv.close();
		}
	});
	ec_view_select_property.selectProperty = function(e, obj){
		YUE.stopPropagation(e);
		ec_view_select_property.current = $('#ec_select_property_displayName');
		ec_view_select_property.showOverLayer(obj, '/msService/ec/property/select_property?config=true&model.code=' + $('#ec_select_indirect_model').val().split('-')[1]);
	}
	ec_view_select_property.showOverLayer = function(obj,url){
		CUI('#customContent').html("");
		ec_view_select_property.showOverLayerDiv = new CUI.Overlay({
			align:obj,
	     	el:'customContent',
	     	title:'${getText('ec.property.choicefield')}',
	     	width:180,
	     	height:282,
	     	zIndex:9999,
	     	shadow:false,
			buttons:[
					{	name:"${getHtmlText('foundation.workbench.mainPage.sure')}",
						handler:function(){
							if($('li[isSelected="1"]').length == 0) {
								alert('${getText("ec.ec_view_select_property.selectNullData")}');
							} else {
								ec_view_select_property.propertyDblClickFunc($('li[isSelected="1"]')[0]);
								$('body').trigger('click.overlayer');
							}
						}
					},
					{	name:"${getHtmlText('calendar.common.cancal')}",
						handler:function(){$('body').trigger('click.overlayer');}
					}]
	     	
		});
		
		
		ec_view_select_property.showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url+="&time="+new Date();
		ec_view_select_property.showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
	ec_view_select_property.propertyDblClickFunc = function(obj){
		if(propertyClickFunc(obj)) {
			var name = $(obj).attr('name');
			name = name.substr(name.indexOf('.') + 1);
			$('#ec_select_property').val(name);
			ec_view_select_property.fillPropertiesNames(ec_view_select_property.current, $(obj).attr('name'));
		}
	}
	ec_view_select_property.fillPropertiesNames = function(obj, names){
		if(obj == null) {
			obj = $('#ec_select_property_displayName');
		}
		if(names == null) {
			names = 'xx.' + $('#ec_select_property').val();
		}
		if($('#ec_select_indirect_model') && $('#ec_select_indirect_model').val()) {
			var ret = getDisplayNamesByNamesAndModelCode(names, $('#ec_select_indirect_model').val().split('-')[1]);
			obj.val('');
			for(var v in ret.data) {
				if(obj.val().length > 0) {
					obj.val(obj.val() + '.');
				}
				obj.val(obj.val() + ret.data[v]);
			}
		}
	}
	
	function showSelectPeopleSettingDialog(){
		var selectSettingDialog = new CUI.Dialog({
	    	title:"选人范围配置",
	        elementId:"groupSelectPeople_dlg",
	        //formId:"advQueryForm",
	        modal:true,
	        type:3,
	        dragable:true,
	        buttons:[{name:"确定", handler:function(){this.close();}},
	                 {name:"取消", handler:function(){this.close();}}
	                ]
    	});
    	selectSettingDialog.show();
	}
	
	var nextLable;
	var nextInput;
	
	function selectPeopleReference(obj){
		nextLable = $(obj).next();
		nextInput = nextLable.next();
		//var windowStyle = "width=800,height=600,top=50,left=150,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		//foundation.common.callOpenWeb("staff",windowStyle,null,null,null,"selectPeopleReferenceCallBack","unassignStaffSupport=true");
		
		foundation.common.select({
				pageType : 'staff',
				closePage : true,
				callBackFuncName : 'selectPeopleReferenceCallBack',
				params : 'unassignStaffSupport=true&multiSelect=true'
			});
	}
	
	function selectPeopleReferenceCallBack(staff){
		for(var i = 0; i < staff.length; i++){
			$(nextLable).val(staff[i].name);
			$(nextInput).val(staff[i].id);
			
			if(i != staff.length-1){
				var copyElement = $('<tr><td class="la" width="20%"><input name="groupname" type="text" size="4" value="分组名称"/></td><td class="co" width="30%"><div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a><a class="cui-btn-r">&nbsp;</a></div><input name="staffName" type="text" size="5" readonly="true" /><input name="selectStaffId" type="hidden"/><input name="uid" type="hidden"/></td><td class="la" width="20%"><input name="sort" type="text" size="1" value="1"/></td><td class="co" width="30%"><img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" /><img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" /></td></tr>');
				$(nextLable).parent().parent().after(copyElement);
				nextLable = copyElement.find("input[name='staffName']");
				nextInput = copyElement.find("input[name='selectStaffId']");
			}
		}
	}
	
	function addSelectRow(obj){
		var copyElement = $('<tr><td class="la" width="20%"><input name="groupname" type="text" size="4" value="分组名称"/></td><td class="co" width="30%"><div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a><a class="cui-btn-r">&nbsp;</a></div><input name="staffName" type="text" size="5" readonly="true" /><input name="selectStaffId" type="hidden"/><input name="uid" type="hidden"/></td><td class="la" width="20%"><input name="sort" type="text" size="1" value="1"/></td><td class="co" width="30%"><img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" /><img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" /></td></tr>');
		$(obj).parent().parent().after(copyElement);
	}
	function delSelectRow(obj){
		if($('tr', $(obj).parents('table')).length <=1) {
			var copyElement = $('<tr><td class="la" width="20%"><input name="groupname" type="text" size="4" value="分组名称"/></td><td class="co" width="30%"><div id="selectSetting" canclick="true" class="edit-btn btn-act" onclick="selectPeopleReference(this)"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c"><span i18n="选择">选择</span></a><a class="cui-btn-r">&nbsp;</a></div><input name="staffName" type="text" size="5" readonly="true" /><input name="selectStaffId" type="hidden"/><input name="uid" type="hidden"/></td><td class="la" width="20%"><input name="sort" type="text" size="1" value="1"/></td><td class="co" width="30%"><img onclick="addSelectRow(this)" src="/bap/static/foundation/images/icon-add.gif" /><img onclick="delSelectRow(this)" src="/bap/static/foundation/images/icon-del.gif" /></td></tr>');
			$(obj).parent().parent().after(copyElement);
		}
		var id = $(obj).parents('tr').find('input[name="uid"]').val();
		if(id!="" && id!="-1"){
			$('#selectPeopleDel').val($('#selectPeopleDel').val() + "," + id);
		}
		$(obj).parents('tr').remove();
	}
	
	function staffSelectTypeRadio(obj){
		var value = $(obj).val();
		if(value=="customer"){
			$("#staffLimit").show();
		}else{
			$("#staffLimit").hide();
		}
	}
	
	function changeAllowView(obj){
		if($(obj).prop('checked')){
			$('#form_tr_allow_view_td1').show();
			$('#form_tr_allow_view_td2').show();
		}else{
			$('#form_tr_allow_view_td1').hide();
			$('#form_tr_allow_view_td2').hide();
		}
	}
	
	function changeMultAllowView(obj){
		if($(obj).prop('checked')){
			$('#multAllowViewTR').show();
		}else{
			$('#multAllowViewTR').hide();
		}
	}
</script>
	</html>