<#assign attachment = (view.hasAttachment!false)?string('true','false')>
<#assign hasAttachment = "false">
<#if (ev.configMap)?? && (ev.configMap.layout)??>
<#list ev.configMap.layout.sections as section>
	<#if section.regionType?? && section.regionType=='LISTPT'>
		<#if section.cells?? && (section.cells)?size gt 0>
			<#list section.cells as cell>
				<#if cell.key?? && cell.key == "bapAttachmentInfo" && cell.showType! == "ATTACHMENT">
					<#assign hasAttachment = "true">
					<#break>
				</#if>
			</#list>
		</#if>
	</#if>
</#list>
</#if>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${getText(view.assModel.entity.name!)}-${getText(view.displayName)}-${getText('ec.project.view_setting')}-<#if view.type == 'LIST'>${getText('ec.view.list')}<#else>${getText('ec.view.ref')}</#if></title>
	<@ec_listTop/>
	<@loadpanel />
	<style type="text/css">
		.layout{height:88%;}
		.grid-s4m0 .main-wrap {width: 100%;float:left;height:100%;overflow:hidden;background-color:#93BFD8;border:1px dashed #E6E6E6;}
		.grid-s4m0 .col-sub { margin-left:2px;margin-top:8px;width: 140px; height:98%;float:left;background-color:white;border:1px solid #3C7FB1}
		.grid-s4m0 .col-top{width: 100%;height:30px;background-color:#D4D8DB;border-bottom:1px solid #ADACAB;}
		#form_select_elements{height:98%}
		#form_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#form_select_elements h2.current {cursor:default;}
		#form_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#form_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#form_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}	
		#form_select_elements .accordion_pane li.uselist{color:#40E0D0;}		
		#fast_select_elements{height:98%}
		#fast_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#fast_select_elements h2.current {cursor:default;}
		#fast_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#fast_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#fast_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}		
		#adv_select_elements{height:98%}
		#adv_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
		#adv_select_elements h2.current {cursor:default;}
		#adv_select_elements .accordion_pane {	border:1px solid #fff;	border-width:0 2px;	display:none;padding:0px;font-size:12px;}
		#adv_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;}	
		#adv_select_elements .accordion_pane li.dragout{color:#BBBBBB;cursor:default;}		
		#main_form_tab_operate li{display:block;}
		#main_form_tab .etv-content{background-color:#F3F3F3!important}
		#main_form_tab .etv-navset{border:1px solid #2B82B2;}
		#main_design_container{padding:2px 2px 0 0;}
		.form_title{height:30px;width:99%;background-color:#d4d4d4;margin:6px 0px 0px 5px}
		.line-icon{width:110px;margin:2px 3px 2px 2px; border-color:#d4d4d4;overflow:hidden;}
		#form_area{position: relative;height:98%;}
		#main_form_tab_operate{z-index: 1000;}
		#main_form_tab .etv-content{min-height:500px;}
		#listSection{height:46px;background:#bababa;overflow-x:scroll;overflow-y:hidden;}	
		.list_design_ul_li{border:#CCC 1px dashed;height:25px;width:180px;border-right:1px solid #FFF;margin-top:1px;margin-bottom:1px;}
		.list_design_ul_li dd{text-align: center;padding-top:2px;width:100%;margin-top:3px;}
		.form_design_ul_li{border:#CCC 1px dashed;float:left; height:25px;margin:1px;margin-right:10px;position: absolute;}
		.button_design_ul_li{border:#CCC 1px dashed;float:left; height:25px;margin:1px;margin-right:10px;}
		.form_design_ul_li dt{float:left;width:100px;text-align: right;padding-right:5px;padding-top:2px;line-height:25px;}
		.form_design_ul_li dd{float:left;text-align: right;padding-top:5px;width:100%;margin-top:3px}
		.form_design_ul_li dd textarea,.form_design_ul_li dd input,.form_design_ul_li dd select{width:100%;height:20px;}
		.form_design_ul_li dd textarea{border:green 1px solid;margin:0;}
		.form_design_ul_li dd input.cui-search-click{width:24px!important;}
		.form_design_ul_li dd input.cui-calpick{width:22px!important;}
		.form_design_ul_li dd input[type='checkbox']{width:20px;height:20px;}
		.form_design_ul_li dd input[type='radio']{width:20px;height:20px;}
		.cui-btn-leftmove {
			background: url('/bap/static/css/sprite_20120525.png') 0px -2050px no-repeat;
		}
		.cui-btn-rightmove {
			background: url('/bap/static/css/sprite_20120525.png') -61px -2050px no-repeat;
		}
		.form_design_ul_li div{display:none;position: absolute;left:5px;top:2px;cursor:move;width:20px;height:20px;background: url("/bap/static/ec/images/arrow_switch.png") center center no-repeat;}
		.div-selected{}
		.ui-selectee{}
		.ui-selecting { background: #FECA40; }
		.ui-selected{background:#F39814;}
		.BlockInfoDiv {margin:2px;padding:4px;border-top:1px dashed #cccccc;}
		.editeventli {width:175px;text-align: right;}
		a.normala{text-decoration: underline;padding-left:2px;cursor:pointer;color:blue; display:inline-block;height:15px;overflow:hidden;}
		.operateDiv{height:22px;background:#bababa;border:#CCC 1px dashed;}
		.unsort-selected{ #FFFFFF;}
		.sort-selected{background:#A3E0FF;}
		.ec-list-btndiv{padding-top:25px;padding-left:30px;}
		.even-num {background-color:#EDEDED;}
		.data-classify {margin-top:0px;}
		.dc-menu-box{top:39px;left:135px;_left:185px;}
		.dc-menu-iframe {top:39px;left:135px;_left:185px;}
		#div_SELECT .dropselectbox {width:134px;border:1px solid #C0D8F0}
		.ec-list-topbtn{cursor:hand;}
		.ec-list-nextbtn{cursor:hand;}
		.ec-list-prevbtn{cursor:hand;}
		.ec-list-lastbtn{cursor:hand;}
		a.help-link {
			display: inline-block;
			width: 15px;
			height: 15px;
			background: url(/bap/static/css/edit_20150318.png) 0px -3553px no-repeat;
		}
	</style>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2" layoutCode="${(ev.configMap.layout.layoutCode)!ecCodeInit('layout')}" page_onload="${((ev.configMap.layout.pageConfig.onload)!)?html}" page_helpInfo="${((ev.configMap.layout.pageConfig.helpInfo)!)?html}">
	<@errorbar id="workbenchErrorBar" offsetY=83 />
	<div class="layout grid-s4m0"> 	
		<div class="col-top">
			<div>
				<h3 style="color:#3A70AA;padding-top:7px;padding-left:15px;float:left">${(view.assModel.entity.entityName)!}-${getText(view.displayName)}-${getHtmlText('ec.project.view_setting')}-<#if view.type == 'LIST'>${getHtmlText('ec.view.list')}<#else>${getHtmlText('ec.view.ref')}</#if></h3>
				<#if TempProjView?? && TempProjView.projFlag?? && TempProjView.projFlag>
				<h3 style="color:red;padding-top:7px;padding-left:5px;float:left">
					(${getText("ec.view.inherited")}-<#if TempProjView.projEnabled?? && TempProjView.projEnabled>${getText("ec.view.inherited.projenabled")}<#if !TempProjView.publishTime??>${getText("ec.engine.publishstste.not")}<#elseif (TempProjView.publishTime?long - TempProjView.modifyTime?long)?abs lt 1000>${getText("ec.engine.publishstste.published")}<#else>${getText("ec.engine.publishstste.waittopublish")}</#if><#else>${getText("ec.view.inherited.projenabled.not")}</#if>)
				</h3>
				</#if>
				<span style="color: #FFFFFF;font-size: 15px;font-weight: bold;margin: 8px 15px;;float:right">
            		<a href="/help/" target="_blank" title="帮助文档" class="help-link"></a>
            	</span>
			</div>
		</div>
		<div class="col-sub">
			<!-- 左侧 -->
			<div id="form_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:97%">
					<ul class="main_properties_container">
						<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
							<#assign properties = (subs.properties)>
							<#list properties as p>
								<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'version'>
								<li class="uselist" source='main' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'     seniorSystemCode='${(p.seniorSystemCode!false)?string('true','false')}' isCustom='${(p.isCustom!false)?string('true','false')}'>
									${getHtmlText('${(p.displayName)!}')}
								</li>
								</#if>	
							</#list>
						</#if>
						<#if subs?? && (subs.associatedInfos)??>
							<#assign associatedInfos = (subs.associatedInfos)>
							<#assign i = 1>
							<#list associatedInfos as ass>
								<li source='test' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}' isCustom='${(ass.originalProperty.isCustom!false)?string('true','false')}'>
									<img sourceType='list' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(ass.targetProperty.model.code)!}")'></img>
									${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
								</li>	
								<#assign i = i+1>
							</#list>
						</#if>
					</ul>
				</div>
			</div>
		</div>	
		<div class="col-main"> 	
			<input type="hidden" id="viewCode" value="${(view.code)!}" />
			<input type="hidden" id="viewType" value="${(view.type)!}" />
			<input type="hidden" id="modelType" value="${(view.assModel.dataType)!}" />
			<input type="hidden" id="delCellIds" name="delCellIds" />
			<input type="hidden" id="btDelCellIds" name="btDelCellIds" />
			<input type="hidden" id="delEventIds" name="delEventIds" />
			<input type="hidden" id="delValidateIds" name="delValidateIds" />
			<div class="main-wrap" id="main_design_container">
				<div id="buttonOperateDiv" style="margin:10px 10px 15px 10px;padding-bottom:5px;background:#F3F3F3;border:1px solid #0099CC;">
					<div class="operateDiv">
						<label for="button"><font style="font-weight:bolder">${getHtmlText('ec.view.configoperator')}</font></label>
						<div style="padding-left:20px;display:inline-block">
						<#if (view.type)?? && view.type!='REFERENCE'>
						<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addOperateButton()">${getText('ec.view.add')}</a>
						</#if>
						<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="listec.modifyOperateButton()">${getText('common.button.edit')}</a>
						<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.deleteOperateButton()">${getText('ec.view.delete')}</a>
						<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addSeparate()">${getText('ec.view.separate')}</a>
						<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="listec.leftmove('buttonOperateDiv')">${getText('ec.view.toleft')}</a>
						<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="listec.rightmove('buttonOperateDiv')">${getText('ec.view.toright')}</a>
						<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.operatorBarProperty()">${getText('ev.view.buttonbar.setting')}</a>
						<#if (view.type)?? && view.type=='REFERENCE'>
						<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.selectListForButton()">${getText('ec.view.refList')}</a>
						</#if>
						</div>
					</div>
					<div style="border:#CCC 1px solid;height:45px;margin:5px 5px 0 5px;">
						<div style="padding-top:10px;padding-left:10px;">
							<#if (ev.configMap)??  && (ev.configMap.layout)??>
							<#assign configMap = (ev.configMap.layout)>
							<#if (ev.configMap.layout.sections)??>
							<#list ev.configMap.layout.sections as section>
								<ul id="operateButton_ul" sectionCode="${(section.sectionCode)!ecCodeInit('section')}">
								<#if (section.regionType)?? && (section.regionType)=='BUTTON'>
									<#assign serviceName = section.serviceName!''>
									<#if section.cells??>
										<#list (section.cells) as operateButton>
											<li cellCode="${operateButton.cellCode!ecCodeInit()}" id="${(operateButton.id)!}" namekey="${(operateButton.namekey)!}" showname="${((operateButton.showname)!)?html}" buttonstyle="${((operateButton.buttonstyle)!)?html}"
											<#if (operateButton.useInMore)??>
												useInMore="${(operateButton.useInMore)?string('true','false')}" 
											<#else>
												useInMore="false" 
											</#if>
											<#if (operateButton.operatetype)??&&((operateButton.operatetype)!="ADD"&&(operateButton.operatetype)!="MODIFY")>
												<#if (operateButton.funcname)??>
													funcname="${((operateButton.funcname)!)?html}" 
												</#if>
												<#if (operateButton.funcbody)??>
													funcbody="${((operateButton.funcbody)!)?html}"
												</#if>
												<#if (operateButton.iscustomfunc)??>
												iscustomfunc="${(operateButton.iscustomfunc)?string('true','false')}"
												</#if>
											</#if>
											<#if (operateButton.operatetype)??>
												operatetype="${((operateButton.operatetype)!)?html}"
											</#if>
											<#if (operateButton.viewselect)??>
												viewselect="${((operateButton.viewselect)!)?html}"
											</#if>
											<#if (operateButton.opentype)??>
												opentype="${((operateButton.opentype)!)?html}"
											</#if>
											<#if (operateButton.iscallback)??>
												iscallback="${(operateButton.iscallback)?string('true','false')}"
											</#if>
											<#if (operateButton.ispermission)??>
												ispermission="${(operateButton.ispermission)?string('true','false')}"
											</#if>
											<#if (operateButton.operateurl)??>
												operateurl="${((operateButton.operateurl)!)?html}"
											</#if>
											<#if (operateButton.isconfirm)??>
												isconfirm="${(operateButton.isconfirm)?string('true','false')}"
											</#if>
											<#if (operateButton.isHide)??>
												isHide="${(operateButton.isHide)?string('true','false')}"
											</#if>
											<#if (operateButton.confirmcontent)??>
												confirmcontent="${((operateButton.confirmcontent)!)?html}"
											</#if>
											<#if (operateButton.selectType)??>
												selectType="${((operateButton.selectType)!false)?string('true','false')}"
											</#if>
											<#if (operateButton.scriptCode)??>
												scriptCode="${((operateButton.scriptCode)!)?html}"
											</#if>
											<#if (operateButton.isPublished)??>
												isPublished="${(operateButton.isPublished)?string('true','false')}"
											</#if>
											<#if (operateButton.isSignatureConfig)??>
												isSignatureConfig="${(operateButton.isSignatureConfig)?string('true','false')}"
											</#if>
											<#if (operateButton.permissionFromName)??>
												permissionFromName="${((operateButton.permissionFromName)!)?html}" 
											</#if>
											<#if (operateButton.permissionFromCode)??>
												permissionFromCode="${((operateButton.permissionFromCode)!)?html}" 
											</#if>
											 class="button_design_ul_li" onmousedown="listec.selectListLi(this)" <#if !(operateButton.operatetype)?? || ((operateButton.operatetype)?? && (operateButton.operatetype) != 'SEPARATE')>ondblclick="listec.modifyOperateButton()"></#if><dd><#if (operateButton.operatetype)?? && (operateButton.operatetype) == 'SEPARATE'><input type="button" style="width:30px" value="|"><#else><input type="button" class="Dialog_button btn_pointer" value="${getText('${(operateButton.namekey)!(operateButton.showname)!}')}"></#if></dd></li>
										</#list>
									</#if>
									<#break>
								</#if>
								</ul>
							</#list>
							</#if>
							<#else>
								<ul id="operateButton_ul" sectionCode="${ecCodeInit('section')}">
								</ul>
							</#if>	
						
						</div>	
					</div>
				</div>
				<hr style="margin-left:8px;margin-right:8px">
				<div id="form_area" style="padding-left:3px;">
					<div id="main_form_tab" class="etv-navset">
						<ul class="etv-nav">
							<li class="selected">${getHtmlText('ec.view.selectd')}</li>
						</ul>
						<div style="background-color:#e3f1f9;" class="etv-content" >
							<input type="hidden" id="mainEntityName" value="${(subs.mainEntityName)!}"/>
							<div class="div-selected">
								<div style="height:80px" class="ec-data-classify">
									<div style="float:left;border-right:1px solid #d5d5d5;width:30%;height:100%;padding-left:10px;">
									<div style="float:left;height:100%;margin-top:20px;"><button style="width:100%;padding-right:3px" <#if isReadOnlyMode>onclick="listec.dataClassificReadOnly(this)"<#else>onclick="listec.dataClassific(this)"</#if> title="${getText('ec.view.list.dcsetclick')}">${getHtmlText('ec.view.list.dcset')}</button></div>
										<div id="dataClassificShowDiv" style="float:left;padding-left:20px;margin-top:20px;">
										<#if dgList?? && dgList?size gt 0>
										<#assign dgStr = "">
										<#list dgList as dg>
											<#if dg_index != 0><#assign dgStr = dgStr + ","></#if>
											<#assign dgStr = dgStr + "{\"dgname\":\""+getText(dg.displayName)+"\",\"dgcode\":\""+dg.code+"\",\"dgtype\":"+dg.isMultiple?string("true","false")+",\"dgvalue\":[">
											<#if dg.dataClassifics??>
											<#list dg.dataClassifics as dc>
											<#if dc_index != 0><#assign dgStr = dgStr + ","></#if>
											<#assign dgStr = dgStr + "{\"code\":\""+dc.code+"\",\"dcvalue\":\""+getText(dc.displayName)+"\"}">
											</#list>
											</#if>
											<#assign dgStr = dgStr + "]}">
										</#list>
										<@dataclassific id="dataClassific" confirmClick="dataClassificTest()" formId="ec_testSearch_testSearch_testSearch_list_queryForm" dataTableId="ec_testSearch_testSearch_testSearch_list_query" dgList="[${dgStr!}]" />
										<#else>
										<@dataclassific id="dataClassific" confirmClick="dataClassificTest()" formId="ec_testSearch_testSearch_testSearch_list_queryForm" dataTableId="ec_testSearch_testSearch_testSearch_list_query" dgList="[]" />
										</#if>
										</div>
									</div>
									<div style="float:left;height:100%;padding-left:10px;">
										<div style="float:left;height:100%;margin-top:15px;">
											<div>
												<button style="width:100%;padding-right:3px" onclick="listec.fastQuerySetting(this,'${view.assModel.code}')" title="${getText('ec.view.list.fastsetclick')}">${getHtmlText('ec.view.list.fastset')}</button>
											</div>
											<div style="margin-top:5px;">
												<button id="advbutton" style="width:100%;padding-right:3px" onclick="listec.advQuerySetting(this,'${view.assModel.code}')" title="${getText('ec.view.list.advsetclick')}">${getHtmlText('ec.view.list.advset')}</button>
											</div>
										</div>
										
										<div id="fastQueryShowDiv" style="float:left;margin-top:15px;">
										<table cellpadding="0" cellspacing="0" border="0" align="center" width="100%" style="margin:5px 0px 5px 10px;">
											<tr>
												<td align="right" style="padding-right:10px;width:100px;">
													<#if (ev.configMap)??  && (ev.configMap.layout)??>
													<#assign configMap = (ev.configMap.layout)>
													<#if (ev.configMap.layout.sections)??>
													<#list ev.configMap.layout.sections as secatt>
														<#if (secatt.regionType)?? && (secatt.regionType)=='FASTQUERY'>
														<select name="fast_query_select" id="fast_query_select" class="list-select" onchange="changeInput(this)">
															<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
																<#list (secatt.cells) as element>
																<#if (element.element.propertyCode)??>
																	<#assign propCodes = (element.element.propertyCode)?split('||')>
																	<#assign propCode = propCodes[propCodes?size - 1]>
																<#else>
																	<#assign propCode = 'null'>
																</#if>
																<#if (element.element.propertyCode)?? &&propertyMap?? && propertyMap['${propCode}']?? >
																	<#assign property = propertyMap['${propCode}'] >
																 <option value="${(element.element.key)!element.element.name}" showFormat="<#if element.element.showFormatHasChanged!false>${(element.element.showFormat)!}<#else>${(property.format!)}</#if>">${getText('${(element.element.namekey)!element.element.name}')}</option>
																</#if>
																</#list>
															<#break>
														</select>
														</#if>
													</#list>
													</#if>
													</#if>
												</td>
												<td>
													<div class="fix-search-click cui-querycondition" style="height:18px" id="fastQueryCondition">
														<div class="fix-input" id="div_TEXT" style="display:none">
															<input type="text" class="cui-noborder-input" value="">
														</div>
														<div class="fix-input" id="div_SELECTCOMP" style="display:none">
														<div class="fix-search-click">
															<input type="text" class="cui-noborder-input" autocomplete="off">
															<input type="button" class="cui-search-click" value="">
														</div></div>
														<div id="div_SELECT" style="display:none">
															<select class="edit-select" style="width:100px;height:18px;"> 
																<option value=""></option>
															</select>
														</div>
														<div id="div_DATE" style="display:none">
														<div style="float:left;width:47%">
														<div class="fix-input"><div class="fix-search-click clearfix">
														<input type="text" class="cui-noborder-input"><input type="button" class="cui-calpick">
														</div></div>
														</div>
														<div style="float:left"> 至</div>
														<div style="float:left;width:47%">
														<div class="fix-input"><div class="fix-search-click clearfix">
														<input type="text" class="cui-noborder-input"><input type="button" class="cui-calpick">
														</div></div>
													 	</div>
												  		</div>
													</div>
												</td>
												<td style="padding-left:10px">
													<#if (ev.configMap)??  && (ev.configMap.layout)??>
													<#assign configMap = (ev.configMap.layout)>
													<#if ev.configMap.layout.sections??>
													<#list ev.configMap.layout.sections as secatt>
													<#if (secatt.regionType)?? && (secatt.regionType)=='FASTQUERY'>
													<@querybutton formId="ec_DataDeal_common_commonTest_list_queryForm" type="adv" onclick="" onadvancedclick="" /> 
					 								<@querybutton formId="ec_DataDeal_common_commonTest_list_queryForm" type="clear"  />
					 								</#if>
					 								</#list>
					 								</#if>
					 								</#if>
												</td>
											</tr>
										</table>
										</div>
									</div>
									
								</div>
								<hr>
								<div class="operateDiv">
									<label for="button"><font style="font-weight:bolder">${getHtmlText('ec.view.cfgfield')}</font></label>
									<div style="padding-left:20px;display:inline-block">
										<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addCustomListField()">${getHtmlText('ec.view.addCol')}</a>
										<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.delListField()">${getHtmlText('ec.view.delcol')}</a>
										<a class="cui-btn mr10 cui-btn-columnattr" href="#" onclick="listec.fieldProperty()">${getHtmlText('ec.view.colattribute')}</a>
										<a class="cui-btn mr10 cui-btn-tableattr" href="#" onclick="listec.listProperty()">${getHtmlText('ec.view.listproperty')}</a>
										<#--<a class="cui-btn mr10 cui-tableattr" href="#" onclick="listec.dataClassific()">${getHtmlText('ec.view.dataclass')}</a>-->
										<a class="cui-btn mr10 cui-btn-sort" href="#" onclick="listec.sortOrderBy()">${getHtmlText('ec.view.sortOrderBy')}</a>
										<a class="cui-btn mr10 cui-btn-pageattr" href="#" onclick="listec.cellPageProperty()">${getHtmlText('ec.view.pageProperty')}</a>
										<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="listec.leftmove('listSection')">${getHtmlText('ec.view.toleft')}</a>
										<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="listec.rightmove('listSection')">${getHtmlText('ec.view.toright')}</a>
									</div>
								</div>
								<table cellspacing="0" cellpadding="0" class="ec_table_bottom">									
									<tr>
										<td class="bottom_head" colspan="6">
							        	</td>
							        </tr>
									<tr>
										<td class="ec_head">${getHtmlText('ec.view.num')}</td>
										<td>
											<div id="listSection" class="listSection">
											<table id="listTable" class="listTable">
											<#if !(ev.configMap)??>
												<tr class="list_design_ul" operService="${serviceName!}" sectionCode="${ecCodeInit('section')}" id="list_design_section" isCheckBox="false" isFirstLoad="false" isExportExcel="true" isSuperTable="false" selectFirstRow="false" isTransCondition="false" conditionContent="" renderOver="" ptPageInit="" isdbcustom="false">
												<#if attachment?? && attachment == 'true'>
												<td class="list_design_ul_li" cellCode="${ecCodeInit()}" key='bapAttachmentInfo' name="bapAttachmentInfo" namekey="foundation.upload.attachment"  showType='ATTACHMENT' fill='' isOrderBy='false' sortType='' seqNumber='' propertyCode='' checkname='' formulas='' hide=false proWidth=250 textalign='center' ondblclick="listec.fieldProperty()" onmousedown="listec.selectListLi(this)"><dd>${getText('foundation.upload.attachment')}</dd></td>
												</#if>
												<tr>
											<#else>
												<#assign configMap = (ev.configMap.layout)>	
												<#list configMap.sections as section>
												<#if section.regionType?? && section.regionType == "LISTPT">
												<#if section.cells?? && (section.cells)?size <= 0>
													<tr class="list_design_ul" sectionCode="${ecCodeInit('section')}" operService="${serviceName!}" id="list_design_section" isTreeView="false" nodeExpanded="false" treePaging="false" isCheckBox="false" isFirstLoad="false" isExportExcel="true" isSuperTable="false" selectFirstRow="false" isTransCondition="false" conditionContent="" renderOver="" ptPageInit="" isdbcustom="false">
													<#if attachment?? && attachment == 'true'>
													<td class="list_design_ul_li" cellCode="${ecCodeInit()}" key='bapAttachmentInfo' name="bapAttachmentInfo" namekey="foundation.upload.attachment" showType='ATTACHMENT' fill='' isOrderBy='false' sortType='' seqNumber='' propertyCode='' checkname='' formulas='' hide=false proWidth=250 textalign='center' ondblclick="listec.fieldProperty()" onmousedown="listec.selectListLi(this)"><dd>${getText('foundation.upload.attachment')}</dd></td>
													</#if>
													<tr>
												<#else>
													<#assign sectionCode = section.sectionCode!ecCodeInit('section')>
													<#if (section.listProperty)??>
													<#assign listProperty = section.listProperty>
													<tr class="list_design_ul" sectionCode="${sectionCode!}" operService="${serviceName!}" id="list_design_section" isTreeView="${(listProperty.isTreeView!false)?string("true","false")}" nodeExpanded="${(listProperty.nodeExpanded!false)?string("true","false")}" treePaging="${(listProperty.treePaging!false)?string("true","false")}" isCheckBox="${listProperty.isCheckBox?string("true","false")}" isSuperTable="${(listProperty.isSuperTable!false)?string("true","false")}" isExportExcel="<#if listProperty.isExportExcel??>${listProperty.isExportExcel?string("true","false")}<#else>true</#if>" selectFirstRow="${(listProperty.selectFirstRow!false)?string("true","false")}" isFirstLoad="${listProperty.isFirstLoad?string("true","false")}" isTransCondition="<#if listProperty.isTransCondition??>${listProperty.isTransCondition?string("true","false")}<#else>false</#if>" conditionContent="${(listProperty.conditionContent!'')?html}" renderOver="${(listProperty.renderOver!'')?html}" ptPageInit="${(listProperty.ptPageInit!'')?html}" isdbcustom="<#if listProperty.isdbcustom??>${listProperty.isdbcustom?string("true","false")}<#else>false</#if>" dbcustomtextarea="${(listProperty.dbcustomtextarea!'')?html}">
													<#else>
													<tr class="list_design_ul" sectionCode="${sectionCode!}" operService="${serviceName!}" id="list_design_section" isTreeView="false" nodeExpanded="false" treePaging="false" isCheckBox="false" isFirstLoad="false" isExportExcel="true" selectFirstRow="false" isTransCondition="false" conditionContent="" renderOver="" ptPageInit="" isdbcustom="false">
													</#if>
														<#list section.cells as column>
															<#assign cellCode = column.cellCode!ecCodeInit()>
															<#if !(column.none)??>
																<#if (column.propertyCode)??>
																	<#assign propCodes = (column.propertyCode)?split('||')>
																	<#assign propCode = propCodes[propCodes?size - 1]>
																<#else>
																	<#assign propCode = 'null'>
																</#if>
																<#if (column.propertyCode)??&& propertyMap?? && propertyMap['${(propCode)!}']??>
																<#assign prop = propertyMap['${(propCode)!}'] >
																	<td class="list_design_ul_li" cellCode="${cellCode!}" name='${(column.key)!}' displayDefaultType="${(column.displayDefaultType)!}" columnType="${prop.type!}" openPending=<#if !column.openPending?? && column.key?? && column.key == 'tableNo'>true<#else>${(column.openPending!false)?string("true","false")}</#if> isCount=${(column.isCount!false)?string("true","false")} isTotal=${(column.isTotal!false)?string("true","false")} namekey="${(column.namekey)!}" key='${(column.key)!}' showFormatFunc="${(column.showFormatFunc?html)!}" <#if column.multable??>multable=${(prop.multable!false)?string("true","false")}<#else>multable=false</#if> <#if column.isOrderBy??>isOrderBy=${(column.isOrderBy)?string("true","false")}<#else>isOrderBy=false</#if> sortType='${(column.sortType)!}' seqNumber='${(column.seqNumber)!}' <#if column.isLink??>isLink=${(column.isLink)?string("true","false")}</#if> assModels='${(column.assModels)!}' linkView='${(column.linkView)!}' assModelCode='${(column.assModelCode)!}' modelCode='${(column.modelCode)!}' mnecode='${(prop.isUsedMneCode!false)?string('true','false')}' showType='${(prop.fieldType)!}' layRec='${(column.layRec)!}' formulas="${((column.formulas)!'')?html}" textalign="${(column.textalign)!'center'}" decimalNum='${(prop.decimalNum)!}'
																	fill='${(prop.fillcontent)!}' 
																	precisionHasChanged="${(column.precisionHasChanged!false)?string('true','false')}" precision="<#if (column.precisionHasChanged!false)?string('true','false')=='true'>${column.precision!}<#else>${prop.decimalNum!}</#if>" propPrecision="${prop.decimalNum!}" 
																	showFormatHasChanged="${(column.showFormatHasChanged!false)?string('true','false')}" showFormat="<#if (column.showFormatHasChanged!false)?string('true','false')=='true'>${column.showFormat!}<#else>${prop.format!}</#if>" propShowFormat="${prop.format!}"
																 	isTreeNode="${(column.isTreeNode!false)?string("true","false")}" 
																 	assPropertyName="${(column.assPropertyName)!}"
															 		funcname="${(column.funcname!'')?html}"
																	funcbody="${(column.funcbody!'')?html}"
																	cssstyle="${(column.cssstyle )!}"
																	seniorSystemcode="${(column.seniorSystemcode!false)?string("true","false")}"
																	isCustom="${(column.isCustom!false)?string("true","false")}" 
																 	<#if (column.ass)?? && column.ass?has_content><#list column.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(column.ass)[asstar]!}'</#list></#if>
																 	propertyCode='${(column.propertyCode)!}' 
																 	hide=${(column.isHidden!false)?string("true","false")} 
																 	style="width: ${(column.width)!100}px"
																 	proWidth=${(column.width)!100} 
																 	ondblclick="listec.fieldProperty()" 
																 	onmousedown="listec.selectListLi(this)">
																 	<dd>${getText('${(column.namekey)!(column.label)!}')}</dd>
																 	</td>
																 <#else>
																 <td class="list_design_ul_li" cellCode="${cellCode!}" name='${(column.key)!}' displayDefaultType="${(column.displayDefaultType)!}" openPending=<#if !column.openPending?? && column.key?? && column.key == 'tableNo'>true<#else>${(column.openPending!false)?string("true","false")}</#if> isCount=${(column.isCount!false)?string("true","false")} isTotal=${(column.isTotal!false)?string("true","false")} namekey="${(column.namekey)!}" key='${(column.key)!}' <#if column.multable??>multable=${(column.multable)?string("true","false")}<#else>multable=false</#if> <#if column.isOrderBy??>isOrderBy=${(column.isOrderBy)?string("true","false")}<#else>isOrderBy=false</#if> sortType='${(column.sortType)!}' seqNumber='${(column.seqNumber)!}' <#if column.isLink??>isLink=${(column.isLink)?string("true","false")}</#if> assModels='${(column.assModels)!}' linkView='${(column.linkView)!}' assModelCode='${(column.assModelCode)!}' modelCode='${(column.modelCode)!}' mnecode='${(column.mnecode!false)?string('true','false')}' showType='${(column.showType)!}' layRec='${(column.layRec)!}' formulas="${((column.formulas)!'')?html}" textalign="${(column.textalign)!'center'}" decimalNum='${(column.decimalNum)!}'
																	fill='{<#if (column.fill)?has_content><#list (column.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (column.showType) == 'ENUMERATE' && fe == 'fillContent'>{<#list (column.fill)[fe]?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((column.fill)[fe][ne])?html}"</#list>}<#else>"${((column.fill)[fe])?html}"</#if></#list></#if>}'
																 	<#if (column.assoFlag)!false>
																 	assoFlag="true"
																 	assoConfig='${(column.assoConfig)!}'
																 	</#if>
																 	<#if (column.customSection)!false>
																 	customsection="true"
																 	customModelCode="${(column.customModelCode)!}"
																 	propertyLayRec="${(column.propertyLayRec)!}"
																 	modelNameInternational="${(column.modelNameInternational)!}"
																 	</#if>
																 	code="${(column.code)!''}"
																 	precisionHasChanged="${(column.precisionHasChanged!false)?string('true','false')}"
																 	columnType="${(column.columnType)!}"
																 	showFormat="${(column.showFormat)!}"
																 	isTreeNode="${(column.isTreeNode!false)?string("true","false")}"
																 	isCustom="${(column.isCustom!false)?string("true","false")}" 
																 	assPropertyName="${(column.assPropertyName)!}"
																 	seniorSystemcode="${(column.seniorSystemcode!false)?string("true","false")}"
																 	funcname ="${(column.funcname!'')?html}"
																	funcbody="${(column.funcbody!'')?html}"
																	cssstyle="${(column.cssstyle )!}"
																 	<#if (column.ass)?? && column.ass?has_content><#list column.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(column.ass)[asstar]!}'</#list></#if>
																 	propertyCode='${(column.propertyCode)!}' 
																 	hide=${(column.isHidden!false)?string("true","false")}
																	style="width: ${(column.width)!100}px"						 	 
																 	proWidth=${(column.width)!100} 
																 	ondblclick="listec.fieldProperty()" 
																 	onmousedown="listec.selectListLi(this)">
																 	<dd><#if (column.customSection)!false>自定义字段区域&nbsp;[${(column.modelNameInternational)!}]<#else>${getText('${(column.namekey)!(column.label)!}')}</#if></dd>
																 	</td>
																</#if>
																
															</#if>	 
														</#list>
														<#--
														<#if hasAttachment?? && hasAttachment != 'true' && attachment?? && attachment == 'true'>
														<td class="list_design_ul_li" cellCode="${ecCodeInit()}" key='bapAttachmentInfo' name="bapAttachmentInfo" namekey="foundation.upload.attachment" showType='ATTACHMENT' fill='' isOrderBy='false' sortType='' seqNumber='' propertyCode='' checkname='' formulas='' hide=false proWidth=250 textalign='center' ondblclick="listec.fieldProperty()" onmousedown="listec.selectListLi(this)"><dd>${getText('foundation.upload.attachment')}</dd></td>
														</#if>
														-->
													</tr>
												</#if>
												</#if>
												</#list>
											</#if>
											</table>
											</div>
										</td>
									</tr>
									<tr><td class="ec_left">01</td>
									<td class="ec_right">
									</td>
									</tr>
									<tr><td class="ec_left">02</td>
									<td class="ec_right">
									</td>
									</tr>
								</table>
							</div>
						</div>						
					</div>
				</div>
			</div>
		</div>		
	</div>	
	<#if !isReadOnlyMode>
	<div id="design-button" style="height:30px;width:100%;float:right;text-align:right;margin-top:20px">
		<button class="Dialog_button btn_pointer btn-primary" onclick="listec.save()" type="button" style="margin-right:20px">${getHtmlText('common.button.save')}</button>
		<#--
		-->
		<button class="Dialog_button btn_pointer" type="button" onclick="listec.publish()" style="margin-right:20px">${getHtmlText('ec.view.publish')}</button>
	</div>
	</#if>
	<div id="form_set_item_property_dlg" style="display:none">
		<@errorbar id="EditDialogErrorBar" />
		<input type="hidden" id="form_prop_default_val" />
		<input type="hidden" id="form_property_default_val" />
		<input type="hidden" id="form_value_changed_val"  />
		<input type="hidden" id="form_showtype_changed_val"  />
		<input type="hidden" id="form_showformat_changed_val"  />
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
					<li name="proertyTabs" id="css_defined">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.style')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
				</ul>
			</div>
		</div>
		<table id="tab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<input type="hidden" id="form_property_field_val" />
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.entity.name')}</td>
				<td class="co" width="30%"><input type="text" id="form_property_name" class="cui-edit-field" style="width:100%" /></td>
				<td class="la showname" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co showname" width="30%">
				<#-- <input type="text" id="form_property_show_name" class="cui-edit-field" style="width:100%" /> -->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="formProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr class="btn-set" style="display:none">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.showvalue')}</label></td>
				<td class="co" width="30%">
					<select id="btn_lr"  class="cui-edit-field" style="width:96%">
						<option value="right">${getText('ec.view.right')}</option>
						<option value="left">${getText('ec.view.left')}</option>
					</select>
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
					<select id="form_property_show_format" class="cui-edit-field" style="width:100%"></select>
					<input type="text" id="form_property_show_format_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
				</td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="iscomplexLable">${getHtmlText('ec.view.iscomplexlable')}</label></td>
				<td class="co" width="30%">
					<select id="selectcomp_type"  class="cui-edit-field" style="display:none;width:100%">
					</select>
				</td>
			</tr>
			<tr class="iscrosscompany-tr" style="display:none;">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.iscrosscompany')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_iscrosscompany" /></td>
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
							<select id="ck_boolean" class="cui-edit-field" onChange="_changeDefaultValue(this)"  style="width:41%" >
								<option value=""></option>
								<option value="true" >${getText("common.radio.true")}</option>
								<option value="false" >${getText("common.radio.false")}</option>
							</select>
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
						</script>
					</td>
				</tr>
			<tr class="not-btn-set">
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isreadOnlyLable">${getHtmlText('ec.view.isreadonly')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_is_readonly" /></td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="operator" style="display:none;">${getHtmlText('ec.flowActive.doperate')}</label></td>
				<td class="co" width="30%">
				<select id="form_property_exp_type" class="cui-edit-field" style="display:none;width:96%">
				</select></td>
			</tr>
			<tr class="case-tr">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.caseSensitive')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_caseSensitive" /></td>
				<#--<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.crosscol')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_crosscol" /></td>--> 
			</tr>
			<tr id="ref_condition">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.refCondition')}</label>
				<span id="refConditionHelpinfo" class="baphelp-icon"></span>
<div id="refConditionHelpinforef" style="display:none">
<p class="baphelp-info">当单元属性为对象类型，可传自定义条件给参照页面。 </p>
<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>return "year=2008 &amp;entity=crossCom " ;</span></code></pre>
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
			<tr class="isrefselect-tr not-btn-set">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.isrefselect')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_isrefselect" /></td>
			</tr>
			<tr class="containLower-tr not-btn-set">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.containLower')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="containLower" /></td>
			</tr>
		</table>
		<div id="div-event" style="width:100%;align:left;height:418px;overflow-y:auto;display:none">
			<table id="tab-event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.eventType')}</label></td>
					<td class="co" width="30%">
						<select id="events-select" class="cui-edit-field" style="width:100%">
						</select>
					</td>
					<td class="la" width="20%" style="text-align:left;padding-left:10px">
						<img onclick="listec.addnewevent($('#events-select'),$('#tab-event1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
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
		<table id="tab-style" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px;display:none">
			<tr style="height:0px">
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
			</tr>
			<tr class="not-btn-set">
				<td class="la" style="width:20%" align="right" padding-right="10px">${getHtmlText('ec.view.customstyle')}
				<span id="customStyleHelpinfo" class="baphelp-icon"></span>&#160;
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
				<td class="co" style="width:80%"><textarea id="form_property_field_style" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea></td>
			</tr>
		</table>
	</div>
 	<div id="section_setting_dlg" style="display:none;">
 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.layout')}</td>
				<td class="co" width="90%">${getHtmlText('ec.view.everyline')} <input type="text" id="section_num" class="cui-edit-field" style="width:50px;margin-right:5px;"/> ${getHtmlText('ec.view.colum')}</td>
			</tr>
			<tr>
				<td class="la" width="10%" align="right" padding-right="10px">${getHtmlText('ec.view.colwidth')}</td>
				<td class="co" colspan="3">
					<div id="editcolwidthdiv">
						1、<input type="text" id="colwidth1" class="cui-edit-field" style="width:20px;margin-right:5px;" value="10"/>
						2、<input type="text" id="colwidth2" class="cui-edit-field" style="width:20px;margin-right:5px;" value="18"/>
						3、<input type="text" id="colwidth3" class="cui-edit-field" style="width:20px;margin-right:5px;" value="17"/>
						4、<input type="text" id="colwidth4" class="cui-edit-field" style="width:20px;margin-right:5px;" value="18"/>
						5、<input type="text" id="colwidth5" class="cui-edit-field" style="width:20px;margin-right:5px;" value="18"/>
						6、<input type="text" id="colwidth6" class="cui-edit-field" style="width:20px;margin-right:5px;" value="19"/>
					</div>
				</td>
			</tr>
		</table>
 	</div>
 	<div id="orderby_sort_dlg" style="display:none;">
		<div style="position:absolute;left:0px;top:0px;overflow-y:auto;height:280px;width:75%;display:inline-block;">
			<table cellpadding="0" id="listColOrderTable" style="padding-left:5px;font-size:12px;padding-top:15px;" cellspacing="0" border="0" align="center" width="93%" class="infoTable">
				<tbody id="listColOrder">
				</tbody>
			</table>
		</div>
		<div id="listContent" style="background-color:#f8f6f7;position:absolute;right:0px;top:0px;width:25%;border: 1px solid #efefef; height: 280px;">
			<div class="ec-list-btndiv"><div class="ec-list-topbtn" onclick="listec.firstRow('list')" id="listFirstMove" ></div></div>
			<div class="ec-list-btndiv"><div class="ec-list-prevbtn" onclick="listec.upRow('list')" id="listUpMove" ></div></div>
			<div class="ec-list-btndiv"><div class="ec-list-nextbtn" onclick="listec.downRow('list')" id="listDownMove"></div></div>
			<div class="ec-list-btndiv"><div class="ec-list-lastbtn" onclick="listec.lastRow('list')" id="listLastMove"></div></div>
		</div>
 	</div>
 	<div id="listFieldDlg" style="display:none;">
 		<@errorbar id="ListFieldDlgErrorBar"></@errorbar>
 		<input type="hidden" id="listFieldDefault" />
 		<input type="hidden" id="precision_changed_val" />
 		<div id="dlg-div-field" class="dlg-etv-navset">
		<div class="etv-scrollbar" style="margin:0 5px;">
			<ul class="etv-nav" style="display: block;">
				<li class="selected" name="proertyTabs" id="field_base_attr">
					<span class="etv-nav-span">
						<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
					</span>
					<span class="etv-nav-span-r"></span>
				</li>
				<li name="proertyTabs" id="field_event_defined">
					<span class="etv-nav-span">
						<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
					</span>
					<span class="etv-nav-span-r"></span>
				</li>
				<li name="proertyTabs" id="field_css_defined">
					<span class="etv-nav-span">
						<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.style')}</em>
					</span>
					<span class="etv-nav-span-r"></span>
				</li>
			</ul>
		</div>

	</div>
	<table id="field-tab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr class="config_relation_model_props config_custom_props">
				<td class="la config_relation_model_props config_custom_props" width="20%">字段类型</td>
				<td class="co config_relation_model_props config_custom_props" width="30%">
					<select id="changePropType_select" onchange="changePropType(this);">
						<option value="0">普通字段</option>
						<option value="1">关联模型字段</option>
						<option value="2">自定义字段</option>
					</select>
					<span id="config_relationModelProps" style="margin:0 10px;"><a href="#" onclick="openConfigRelationModelPropsDlg();" style="color:blue;">配置</a></span>
				</td>
				<td class="la config_custom_props" width="20%"></td>
				<td class="co config_custom_props" width="30%"></td>
			</tr>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;align:right;color:red">${getHtmlText('ec.view.code')}</td>
				<td class="co" width="30%"><input id="property_key" type="text" class="cui-edit-field" /></td>
				<td class="la" width="20%" style="padding-right:10px;align:right" id="decimalTd">${getHtmlText('ec.property.decimalNum')}</td>
				<td class="co" width="30%" id="decimalTextTd"><input id="property_decimal" type="text" class="cui-edit-field" /></td>
			</tr>
			<tr class="config_relation_model_props">
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.showname')}</td>
				<td class="co config_relation_model_props" width="30%">
				<#-- <input id="property_show_name" type="text" class="cui-edit-field" /> --> 
				<#--international_propertyshowName    international_propertyshowName_showName -->
					<@international name="property.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.cell.showFormat')}</td>
				<td class="co" width="30%">
					<select id="property_show_format" class="cui-edit-field" style="width:100%"></select>
					<input type="text" id="property_show_format_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
				</td>
			</tr>
			<tr class="config_relation_model_props">
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.width')}</td>
				<td class="co config_relation_model_props" width="30%"><input id="property_width" type="text" class="cui-edit-field" /></td>
				<td class="la count-total" width="20%" style="padding-right:10px;align:right"></td>
				<td class="co count-total" width="30%">
				<div id="listCount_div" style="width:100%">
					<input type="checkbox" name="isCount" id="isCount">${getHtmlText('ec.property.isCount')}&nbsp;</input>
					<input type="checkbox" name="isTotal" id="isTotal"/>${getHtmlText('ec.property.isTotal')}&nbsp;</input>
				</div>
				</td>
			</tr>
			<tr id="listFieldDefaultTR" class="config_relation_model_props">
				<td class="la config_relation_model_props" width="20%" align="right" padding-right="10px">
					<div id="listField_defaultValue_div" style="width:100%">
						${getHtmlText('ec.property.nulldefault.diaplay')}
					</div>
				</td>
				<td class="co config_relation_model_props" width="80%" colspan="3">
					<input type="text" name="displayDefaultValue" id="displayDefaultValue" class="cui-edit-field" onChange="_changeDisplayValue(this)" />
					<script type="text/javascript">
						function _changeDisplayValue(obj){
							CUI('#listFieldDefault').val(CUI(obj).val());
						}
					</script>
				</td>
			</tr>
			<tr class="config_relation_model_props">
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.ishidden')}</td>
				<td class="co config_relation_model_props" width="30%"><input type="checkbox" id="isHide" /></td>
				<td class="la config_relation_model_props" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.showvalue')}</td>
				<td class="co config_relation_model_props" width="30%">
					<select id="textalign"  class="cui-edit-field" style="width:100%">
						<option value="center">${getText('ec.view.center')}</option>
						<option value="left">${getText('ec.view.left')}</option>
						<option value="right">${getText('ec.view.right')}</option>
					</select>
				</td>
			</tr>
			<tr class="isorderby">
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.isOrderBy')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isOrderBy" /><input type="hidden" id="seqNumber" /></td>
				<td class="la orderby" width="20%" style="padding-right:10px;align:right;display:none;">${getHtmlText('ec.view.sortType')}</td>
				<td class="co orderby" width="30%" style="display:none;">
					<select id="sortType"  class="cui-edit-field" style="width:100%">
						<option value="asc">${getText('ec.view.asc')}</option>
						<option value="desc">${getText('ec.view.desc')}</option>
					</select>
				</td>
			</tr>
			<tr id="formulasTr" style="display:none;">
				<td class="la" width="30%" style="padding-right:10px;">${getHtmlText('ec.view.expression')}</td>
				<td class="co" width="70%" colspan="3"><input id="formulasText" type="text" class="cui-edit-field" /></td>
			</tr>
			<#if !view.entity.isBase && view.type != "REFERENCE" && view.assModel.isMain?? && view.assModel.isMain>
			<tr class="openPending">
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.openPending')}</td>
				<td class="co" width="30%"><input type="checkbox" id="openPending" /></td>
				</td>
			</tr>
			</#if>
			<tr id="islinkViewTr">
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.isLink')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isLink" /></td>
				
			</tr>
			<tr id="linkViewTr">
				<td class="la" width="20%" style="padding-right:10px;align:right" id="assModelTd">${getHtmlText('ec.view.assModel')}</td>
				<td class="co" width="30%">
					<select id="assModels"  class="cui-edit-field" style="width:100%">
						<option value=""></option>
					</select>
				</td>
				<td class="la" width="20%" style="padding-right:10px;align:right" id="linkViewTd">${getHtmlText('ec.view.linkView')}</td>
				<td class="co" width="30%">
					<select id="linkView"  class="cui-edit-field" style="width:100%">
						<option value=""></option>
					</select>
				</td>
			</tr>
			<tr class="treeNode">
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isTreeNode">${getHtmlText('ec.view.isTreeNode')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="property_is_treeNode" /></td>
			</tr>
			<tr id="formatTr">
				<td class="la" width="20%" align="right" padding-right="10px">
					<div style="width:100%">
						${getHtmlText('ec.view.cell.showFunction')}
					<span id="showFunctionHelpinfo" class="baphelp-icon"></span>
<div id="showFunctionHelpinforef" style="display:none">
	<p class="baphelp-info">在列表字段配置界面中的显示方法区域添加方法,一般用于添加超链接</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function createStaff_name_showFormatFunc(value,nRow){
	 return "&lt;div style='background:#3366CC;'&gt;"+value+"&lt;/div&gt;";
};
</code></pre>
</div>
<p class="baphelp-hint">注意：方法名为:列名_showFormatFunc,列名中的.替换为_。</p>
</div>
					<script type="text/javascript">
							$('#showFunctionHelpinfo').helptip({refElm: "#showFunctionHelpinforef", html: true , isCustom :false, width: 400 , title :"说明"});
						</script>
						</div>
				</td>
				<td class="co" width="80%" colspan="3">
					<textarea id="showFormatFunc" class="cui-edit-textarea" style="height:80px;"></textarea>
				</td>
			</tr>
			<tr id="tr_config_custom_props_select_model" class="config_custom_props">
				<td class="la config_custom_props" width="20%">所属模型</td>
				<td class="co config_custom_props" width="30%">
					<div name="property" class="fix-search-click" style="width:95%;">
						<input id="config_custom_props_select_model" size="15" class="cui-edit-field" readonly="readonly" />
						<input type="button" onclick="ec.config.customProp.selectModel(event, this);" class="cui-search-click" />
					</div>
				</td>
				<td class="la config_custom_props" width="20%"></td>
				<td class="co config_custom_props" width="30%"></td>
			</tr>
		</table>
		<div id="field-div-event" style="width:100%;align:left;height:300px;overflow-y:auto;display:none">
		<table id="field-tab-event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.eventType')}</label></td>
				<td class="co" width="30%">
					<select id="field-events-select" class="cui-edit-field" style="width:100%">
					<option value="onclick">onclick</option>
					</select>
				</td>
				<td class="la" width="20%" style="text-align:left;padding-left:10px">
					<img onclick="listec.addnewevent($('#field-events-select'),$('#field-tab-event1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
				</td>
				<td class="la" width="30%"></td>
			</tr>
			<tr>
				<td colspan="4">
					<table id="field-tab-event1" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;">
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
	<table id="field-tab-style" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px;display:none">
		<tr style="height:0px">
			<td style="width:20%;height:0px;text-align:right" class="la"></td>
			<td class="co" style="width:30%;height:0px"></td>
			<td style="width:20%;height:0px;text-align:right" class="la"></td>
			<td class="co" style="width:30%;height:0px"></td>
		</tr>
		<tr>
			<td class="la" style="width:22%" align="right" padding-right="10px">${getHtmlText('ec.view.tdcustomstyle')}
			<span id="tdstyleHelpinfo" class="baphelp-icon"></span>
<div id="tdstyleHelpinforef" style="display:none">
	<p class="baphelp-info">自定义TD的样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式。</p>
</div>
					<script type="text/javascript">
							$('#tdstyleHelpinfo').helptip({refElm: "#tdstyleHelpinforef", html: true , isCustom :false, width: 260 , title :"说明"});
						</script></td>
			<td class="co" style="width:80%"><textarea id="form_property_field_style_td" class="cui-edit-textarea" style="width:100%;height:100px;margin-top:5px"></textarea></td>
		</tr>
	</table>
	</div>
	<div id="listPropertyDlg" style="display:none;">
		<@errorbar id="ListPropertyDlgErrorBar"></@errorbar>
		<div id="dlg_div_public" class="dlg-etv-navset">
	 		<div class="etv-scrollbar" style="margin:0 5px;">
				<ul class="etv-nav" style="display: block;">
					<li class="selected" name="proertyTabs" id="base_attr_public">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
					<li name="proertyTabs" id="event_defined_public">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
				</ul>
			</div>
		</div>
		<table id="tab_common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isCheckBox" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isCheckbox')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isFirstLoad" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.firstsearch')}</td>
			</tr>
			<tr>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isExportExcel" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.exportexcel')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="selectFirstRow" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.selectFirstRow')}</td>
			</tr>
			<tr style="display:none">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isSuperTable" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.superTable')}</td>
			</tr>
			<#if view.assModel.dataType?? && view.assModel.dataType == 2>
			<tr class="tree-choise">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isTreeView" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isTreeView')}</td>
				<td class="la nodeExpanded" width="10%" style="padding-right:10px;"><input type="checkbox" id="nodeExpanded" /></td>
				<td class="co nodeExpanded" width="40%">${getHtmlText('ec.view.nodeExpanded')}</td>			
			</tr>
			<tr class = "tree-page">
				<td class="la treePaging" width="10%" style="padding-right:10px;"><input type="checkbox" id="treePaging" /></td>
				<td class="co treePaging" width="40%">${getHtmlText('是否分页')}</td>
			</tr>
			</#if>
			<tr>
				<td class="la" width="10%" style="padding-right:10px;">
					<input type="checkbox" id="isTransCondition" />
				</td>
				<td class="co" width="40%">
					${getHtmlText('ec.view.handwritingCondition')}
					<span id="handwritingHelpinfo" class="baphelp-icon"></span>	
<div id="handwritingHelpinforef" style="display:none">
	<p class="baphelp-info">可动态传递参数拼接到条件中，并根据变量进行逻辑判断</p>
	<p class="baphelp-example">范例</p>
<#noparse>
<div class="baphelp-code">
<pre><code><span>if(parameters.test[0]=="1"){</span>
	return "(\"p\".ID =\${deptId,Long} OR \"p\".LAY_REC LIKE \${layrec,String})";
}else{
	return "(\"p\".ID =\${deptId,String} OR \"p\".LAY_REC LIKE \${layrec,String})";
}</code></pre>
</div>
<p class="baphelp-hint">注意：test 为前台传的参数，post 或get 均可，数组可以用array或者list类型，参数默认用逗号分隔，或者额外传split参数作为分隔符，例如 pid in (${pids,array})</p>
</#noparse>
</div>

					<script type="text/javascript">
							$('#handwritingHelpinfo').helptip({refElm: "#handwritingHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
				</script>
				</td>
				<td class="co" colspan="2">
					<div id="66387035_button_2" canClick="true" class="edit-btn btn-act" onclick="showTableDialog()"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c">${getHtmlText('ec.view.customerCondition')}</a><a class="cui-btn-r">&nbsp;</a></div>
				</td>
			</tr>
			<tr>
				<td class="co" colspan="4" style="padding-left:30px">
					<textarea readonly="true" id="conditionArea" class="cui-edit-textarea" style="width:90%;height:180px;"></textarea>
				</td>
			</tr>
		</table>
		<table id="tab_event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px;display:none;">
			<tr style="height:0px">
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
			</tr>
			<tr>
				<td class="la" style="width:25%" align="right" padding-right="10px">renderOver:
				<span id="renderOverHelpinfo" class="baphelp-icon"></span>
<div id="renderOverHelpinforef" style="display:none">
	<p class="baphelp-info">列表数据请求执行接口</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>alert ( " 示例 " );
</code></pre>
</div>
<p class="baphelp-hint">注意：每次请求数据的时候都会调用该接口</p>
</div>

					<script type="text/javascript">
							$('#renderOverHelpinfo').helptip({refElm: "#renderOverHelpinforef", html: true , isCustom :false, width: 280 , title :"说明"});
				</script>
				</td>
				<td class="co" style="width:80%"><textarea id="dataGrid_public_event" class="cui-edit-textarea" style="width:100%;height:60px;margin-top:5px"></textarea></td>
			</tr>
			<tr>
				<td class="la" style="width:20%" align="right" padding-right="10px">ptInit（${getText('ec.view.list')}&#160;&#160;<br/>${getText('ec.view.init')}）:
				<span id="ptInitHelpinfo" class="baphelp-icon"></span>
<div id="ptInitHelpinforef" style="display:none">
	<p class="baphelp-info">列表初始化函数</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>alert ( " 示例 " );
</code></pre>
</div>
<p class="baphelp-hint">注意：只在第一次初始化时执行</p>
</div>

					<script type="text/javascript">
							$('#ptInitHelpinfo').helptip({refElm: "#ptInitHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
				</td>
				<td class="co" style="width:80%"><textarea id="dataGrid_init_event" class="cui-edit-textarea" style="width:100%;height:60px;margin-top:5px"></textarea></td>
			</tr>
			<tr >
				<td class="la" width="30%"><label for="isdbcustom">${getHtmlText('ec.view.dbcustom')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="isdbcustom"  onclick="changedbcustom(this);"/></td>
			</tr>
			<tr id="db_customt_extarea" style="display:none;" >
			<td class="la" style="width:20%" align="right" padding-right="10px">dblclick:
			<span id="dbclickHelpinfo" class="baphelp-icon"></span>
<div id="dbclickHelpinforef" style="display:none">
	<p class="baphelp-info">列表行双击事件，配置后行默认的双击事件以该配置为准</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function demo(event,row){
	console.info(event);
	console.info(row);
}
</code></pre>
</div>
<p class="baphelp-hint">注意：event为事件对象,row为双击行的对象</p>
</div>			
					<script type="text/javascript">
						$('#dbclickHelpinfo').helptip({refElm: "#dbclickHelpinforef", html: true , isCustom :false, width: 340 , title :"说明"});
					</script>
			</td>
			<td class="co" width="80%"  >
				<textarea id="dbcustomtextarea" class="cui-edit-textarea" style="width:100%;height:60px;margin-top:5px"></textarea>
			</td>
			</tr>
		</table>
	</div>
	<div id="button_property_dlg" style="display:none">
		<@errorbar id="ButtonDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la supplant-required-field" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.identiy')}</td>
				<td class="co" width="30%"><input type="text" id="button_property_id" class="cui-edit-field" style="width:100%;box-sizing:border-box;" /></td>
				<td class="la supplant-required-field" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co" width="30%">
				<#--<input type="text" id="button_property_show_name" class="cui-edit-field" style="width:100%" />-->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="buttonProperty.showName" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.ishidden')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isOperateHide" /></td>
				<td id="permissionFrom" style="display:none;" class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.permissionFrom')}</td>
				<td id="permissionFrom_" class="co" width="30%" style="display:none;" ><input type="text" id="permissionFromName" readonly=true class="cui-edit-field" style="box-sizing:border-box;"/>
				<input type="hidden" id="permissionFromCode" readonly=true/>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.operatetype')}</td>
				<td class="co" width="30%">
					<select id="operatetype"  class="cui-edit-field" style="width:100%">
						<option value="CUSTOM">${getText('ec.view.customoperate')}</option>
						<#if !(view.assModel.type)??  || view.assModel.type!=3>
						<option value="ADD">${getText('ec.view.addoperator')}</option>
						<option value="MODIFY">${getText('ec.view.editoperator')}</option>
						<option value="DELETE">${getText('ec.view.deloperator')}</option>
						<#if view.assModel.isMain!false>
						<option value="IMPORT">${getText('ec.view.importoperator')}</option>
						</#if>
						<#if view?exists&&view.entity?exists&&view.entity.isBase>
						<option value="RESTORE">${getText('ec.view.resoperator')}</option>
						</#if>
						</#if>
					</select>
				
				</td>
				<td class="la" width="20%" align="right" padding-right="10px" id="viewSelectLabel"></td>
				<td class="co" width="30%" style="display:none;" id="viewSelectTd">
					<select id="viewselect"  class="cui-edit-field" style="width:100%">
						<option value="">${getText('ec.view.none')}</option>
						<#list viewList as viewlist>
							<option value="${viewlist.code}">${viewlist.name}</option>
						</#list>
					</select>
				</td>
				<td class="la" width="20%" align="right" padding-right="10px" style="display:none;">不启用权限</td>
				<td class="co" width="30%" style="display:none;" id="noPermissionTd"><input type="checkbox" id="noPermission" /></td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">Confirm</td>
				<td class="co" width="30%"><input type="checkbox" id="isconfirm" /></td>
				<td class="la" width="20%" align="right" padding-right="10px" id="confirmname">${getHtmlText('ec.view.hint')}</td>
				<td class="co confirmcontent" width="30%">
					<#--<input type="text" id="confirmcontent" class="cui-edit-field" style="width:100%" />-->
					<@international name="confirmcontent" isNew=true isOldEdit=true moduleCode="${view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.styletype')}</td>
				<td class="co" width="30%">
					<select id="buttonstyle"  class="cui-edit-field" style="width:100%">
						<option value="add">${getText('ec.view.addstyle')}</option>
						<option value="modify">${getText('ec.view.editstyle')}</option>
						<option value="del">${getText('ec.view.delstyle')}</option>
						<option value="import">${getText('ec.view.importstyle')}</option>
						<option value="recover">${getText('ec.view.restorestyle')}</option>
					</select>
				
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.useInMore')}</td>
				<td class="co" width="30%"><input type="checkbox" id="useInMore" /></td>
			</tr>
			<tr id="callbackTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.refresh')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="iscallback" />			
				</td>
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.ispower')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="ispower" />			
				</td>
			</tr>
			<tr id="permissionTr">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.ispower')}</td>
				<td class="co" width="30%"><input type="checkbox" id="ispermission" /></td>
			</tr>
			<tr id="urlTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.powerurl')}</td>
				<td class="co" colspan="3">
					<input type="text" id="operateurl" class="cui-edit-field" style="width:100%" />				
				</td>
			</tr>
			<tr id="selectStypeTr" style="display:none">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.selectScript')}</td>
				<td class="co" width="30%">
					<input type="radio" name="stype" id="selectType" />		
				</td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.customerScript')}</td>
				<td class="co" width="30%">
					<input type="radio" name="stype" id="writeType" />		
				</td>
			</tr>
			<tr id="eventTr" style="display:none;">
				<td class="la supplant-required-field" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.customevent')}</td>
				<td class="co" width="30%">
					<div style="float:left;width:100%;height:20px;margin-bottom:5px;border:1px solid #0059B3;overflow:auto;">
						<ul id="button_event_ul">
							<li class="editeventli" style="padding-top:2px">
							<a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline;padding-left:20px;" onclick="listec.editButtonEvent('onclick')">onclick</a>：<input type="checkbox" id="button_property_event_onclick" value="" style="padding-right:10px"/>
							<span id="functionHelpinfo" class="baphelp-icon"></span>
<div id="functionHelpinforef" style="display:none">
	<p class="baphelp-info">自定义按钮的单击函数</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function customClick(event){
	console.info(event);
}</code></pre>
</div>
<p class="baphelp-hint">注意：event为事件对象，可不传</p>
</div>
					<script type="text/javascript">
							$('#functionHelpinfo').helptip({refElm: "#functionHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
							</li>
						</ul>
					</div>
					<br/>					
				</td>
			</tr>
			<tr id="iscustomfuncTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.iscallback')}
				</td>
				<td class="co" width="30%">
					<input type="checkbox" id="iscustomfunc" />	
					<span id="callBackHelpinfo" class="baphelp-icon"></span>
					<script type="text/javascript">
					$(function(){
							var sel = $('#operatetype');
								var cbElm = $('#callBackHelpinfo');
								var cbIns;
								var cbM = {
									ADD: '<p class="baphelp-info">新增的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在保存后执行</p>',
									MODIFY: '<p class="baphelp-info">修改的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在保存后执行</p>',
									DELETE: '<p class="baphelp-info">删除的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在删除后执行</p>',
									RESTORE: '<p class="baphelp-info">还原的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function demo(){<br/>&#160;&#160;&#160;alert(\"示例\");<br/>}</code></pre></div><p class="baphelp-hint">注意：回调函数会在保存后执行</p>'
									
								};		
								cbElm.on('click', function(){
									cbIns.setContent( cbM[sel.val()] );
								});				
								cbIns = cbElm.helptip({
									content: cbM[sel.val()], 
									html: true , 
									isCustom :false, 
									width: 200 , 
									title :"说明"
								}).data('cui.tooltip');
							});	
						</script>		
				</td>
			</tr>
			<tr id="customfuncTr">
				<td class="co" colspan="4">
					<textarea  id="customfuncarea" class="cui-edit-textarea" style="width:100%;height:55px;margin-left:10px;"></textarea>
				</td>	
			</tr>
			<tr id="selectGScript" style="display:none">
				<td class="la" id="view_scriptCodeLabel" >${getHtmlText('js.ec.script.manager.code')}</td>
				<td class="co" id="view_scriptCodeTd" >
				<@selector name="view.scriptCode" id="scriptCode" cssClass="cui-noborder-input" cssStyle="width:100%" value="${(view.scriptCode)!}"
					openType="page" closePage="true" pageType="script" onclick="selectScriptCode"/>
				</td>
			</tr>
			<tr id="textareaTr" style="display:none;">
				<td class="co" colspan="4">
					<textarea  id="buttonEventarea" class="cui-edit-textarea" style="width:100%;height:55px;margin-left:10px;"></textarea>
				</td>	
			</tr>
		</table>
	</div>
	<div id="page_setting_dlg" style="display:none;">
 		<table class="infoTable" cellpadding="0" cellspacing="10" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.pageload')}<br/>(onLoad)
				<span id="onLoadHelpinfo" class="baphelp-icon"></span>
<div id="onLoadHelpinforef" style="display:none">
	<p class="baphelp-info">页面加载的时候执行</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>alert("示例");
</code></pre>
</div>
<p class="baphelp-hint">注意：该时刻，datagrid或datatable可能还未加载完，不可在对这二个对象进行操作</p>
</div>
					<script type="text/javascript">
							$('#onLoadHelpinfo').helptip({refElm: "#onLoadHelpinforef", html: true , isCustom :false, width: 480 , title :"说明"});
						</script>
				</td>
				<td class="co" colspan="3"><textarea id="onloadevent" class="cui-edit-textarea" style="width:100%;height:200px;"></textarea></td>
			</tr>
			<!--<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.helpInfo')}<br/>(helpInfo)&#160;&#160;&#160;</td>
				<td class="co" colspan="3"><textarea id="helpInfo" class="cui-edit-textarea" style="width:100%;height:200px;"></textarea></td>
			</tr>-->
		</table>
 	</div>
 	<div id="fast_query_setting_dlg" style="display:none;">
		<div style="margin-left:2px;margin-top:8px;width: 160px; height:96%;float:left;background-color:white;border:1px solid #3C7FB1">
			<input type="hidden" id="fastDelCells" />
			<!-- 左侧 -->
			<div id="fast_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:89%">
					<ul class="main_properties_container">
						<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
							<#assign properties = (subs.properties)>
							<#list properties as p>
								<#if p.type != "LONGTEXT" && p.type != "OFFICE" && p.type != "PROPERTYATTACHMENT" && p.type != "COLOR">
								<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'version' && p.name != 'layNo' && p.name != 'layRec' && (p.type) != "LONGTEXT">
								<li source='main' modelDataType="<#if (p.model.dataType)?? && (p.model.dataType) == 2>tree<#else>simple</#if>" onclick='listec.addFastQueryField(this)' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'>
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
								<li source='test' parentModelDataType='<#if (ass.targetProperty.model.dataType)?? && (ass.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' assPropertyColumnName='${ass.targetProperty.columnName}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
									<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(ass.targetProperty.model.code)!}","fast")'></img>
									${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
								</li>	
								</#if>
								<#assign i = i+1>
							</#list>
						</#if>
					</ul>
				</div>
				<h2>${getHtmlText('ec.view.one2manyattr')}</h2>
				<div class="accordion_pane" style="overflow:auto;height:87%"><ul class="onetomany_properties_container">
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
								<li source='test' parentModelDataType='<#if (onetoManyAss.targetProperty.model.dataType)?? && (onetoManyAss.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='one2many' assTar='${onetoManyAss.targetProperty.code}' propertyCode="${onetoManyAss.targetProperty.code!}" assOrg="${onetoManyAss.originalProperty.code}" dbname='${(onetoManyAss.targetProperty.model.modelName)!}' name='${(one2ManyTargetPropertyName)!}' entityCode='${(onetoManyAss.targetProperty.model.code)!}' relativeName='${(one2ManyOriginalTableName)!},${(onetoManyAss.targetProperty.columnName)!},${(onetoManyAss.originalProperty.model.tableName)!},${(onetoManyAss.originalProperty.columnName)}'>
									<img sourceType='fast' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(onetoManyAss.targetProperty.model.code)!}","fast")'></img>
									${getHtmlText('${(onetoManyAss.targetProperty.displayName)!}')}[${getHtmlText('${(onetoManyAss.targetProperty.model.name)!}')}]
								</li>	
								<#assign j = j+1>
						</#list>
					 </#if>
				</ul></div>
			</div>
		</div>	
		<div id="fastQueryContentList" style="width:490px;background-color:white;border:2px solid #D4D4D4;float:left;height:96%;margin: 8px 5px 0 7px;"> 	
			<div style="position:absolute;height:89%;width:56%;">
				<#if (ev.configMap)??  && (ev.configMap.layout)??>
					<#assign configMap = (ev.configMap.layout)>
					<#if ev.configMap.layout.sections??>
					<#list ev.configMap.layout.sections as secatt>
					<#if (secatt.regionType)?? && (secatt.regionType)=='FASTQUERY'>
					<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
					</#if>
					</#list>
					</#if>
				</#if>
				<#if (ev.configMap)??  && (ev.configMap.layout)??>
				<#assign configMap = (ev.configMap.layout)>
				<#if ev.configMap.layout.sections??>
				<#list ev.configMap.layout.sections as secatt>
				<#if (secatt.regionType)?? && (secatt.regionType)=='FASTQUERY'>
				<#if (secatt.fastProperty)??>
				<#assign fastProperty = secatt.fastProperty>
				</#if>
				<#break>
				</#if>
				</#list>
				</#if>
				</#if>
				<div style="float:right;padding-top:10px;">
					<input type="checkbox" name="isExpandAll" id="isExpandAll" onclick="listec.expandAllMethod(this);" <#if fastProperty?? && fastProperty.isExpandAll?? && fastProperty.isExpandAll>checked="checked"</#if>>${getHtmlText('ec.view.isExpandAll')}</input>
					<select id="expandType" name="expandType" class="cui-edit-field" style="width:100px;<#if (fastProperty?? && fastProperty.isExpandAll?? && !fastProperty.isExpandAll) || !fastProperty??>display:none;</#if>">
						<option value="single">${getText("ec.view.isSingle")}</option>
						<option value="all">${getText("ec.view.isAll")}</option>
					</select>
					<a style="border:1px solid #fff;padding-right:15px;" class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.fastFieldProperty()">${getHtmlText('ec.view.fastFieldProperty')}</a>
				</div>
				<div style="margin-left:10px;margin-top:40px;overflow-y:auto;clear:both;height:82%">
				<table cellpadding="0" id="fastColTable" style="font-size:12px;width:96%;" sectionCode="${sectionCode!}" isExpandAll="${((fastProperty.isExpandAll)!true)?string('true','false')}" expandType="${(fastProperty.expandType)!'all'}" cellspacing="0" align="center" class="infoTable">
					<tbody id="fastColOrder">
					<#if (ev.configMap)??  && (ev.configMap.layout)??>
					<#assign configMap = (ev.configMap.layout)>
					<#if ev.configMap.layout.sections??>
					<#list ev.configMap.layout.sections as secatt>
						<#if (secatt.regionType)?? && (secatt.regionType)=='FASTQUERY'>
							<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
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
										<#if (element.element.propertyCode)?? &&propertyMap?? && propertyMap['${propCode}']?? >
											<#assign property = propertyMap['${propCode}'] >
											<#assign showType = element.element.showType!>
											<#if !element.element.showTypeHasChanged!false >
											<#assign showType = property.fieldType! >
											</#if>
											<#assign showFormat = element.element.showFormat!>
											<#if !element.element.showFormatHasChanged!false>
											<#assign showFormat = property.format! >
											</#if>
											<#if property.type?? && property.type=='BOOLEAN'>
												<#assign showType='SELECT'>
												<#assign showFormat = 'SELECT'>
											</#if>
											<tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}" key="${(element.element.name)!}" partDepend="${(element.element.partDepend)!'common'}" selfType="${(property.type)!}" ondblclick="listec.selectRow('fast',this);listec.fastFieldProperty();" onmousedown="listec.selectRow('fast',this);" defaultValue="<#if element.element.defaultValueHasChanged!false>${(element.element.defaultValue!)?string}<#else>${(property.defaultValue!)?string}</#if>" propDefaultValue="${(property.defaultValue!)?string}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" partDepend="${(element.element.partDepend)!'common'}" columnType="${(property.type)!''}" name="${(element.element.name)!}" namekey="${(element.element.namekey)!}" key="${(element.element.key)!}" mnecode='${(property.isUsedMneCode!false)?string('true','false')}' iscrosscompany='${(element.element.iscrosscompany!false)?string('true','false')}' isrefselect='${(element.element.isrefselect!false)?string('true','false')}' propertyCode="${(element.element.propertyCode)!}" layRec="${(element.element.layRec)!}" nullable="<#if (element.element.nullable)?? && (property.nullable)?? && (element.element.nullable)?string("true","false") == 'true' && (property.nullable)?string("true","false") == 'true'>true<#else>false</#if>" isreadonly="${(element.element.readonly)?string("true","false")}" exp="${(element.element.exp)!}" entityCode="${(property.model.entity.code)!}" layRec="${(element.element.layRec)!}" assTar="${(element.element.assTar)!}" assOrg="${(element.element.assOrg)!}" multable="${((property.multable)!false)?string('true','false')}" columnLong="${(property.maxLength)!}" readonly="${(element.element.readonly)?string("true","false")}" checkname="${ckname!}"  sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${((element.callbackbody)!)?html}" callbackname="${((element.callbackname)!)?html}" funcname="${((element.funcname)!)?html}" funcbody="${((element.funcbody)!)?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!"")?html}" referenceview="${(element.element.referenceview)!}" modelcode="${(property.model.code)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}"  
											propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(element.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}" 
											precision="${element.element.precision!}" propPrecision="${property.decimalNum!}" precisionHasChanged="${(element.element.precisionHasChanged!false)?string('true','false')}"  
											assPropertyName="${element.assPropertyName!}"
											assPropertyColumnName="${element.assPropertyColumnName!}" 
											showType="${showType!}" <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> modelDataType="<#if property.model.dataType == 2>tree<#else>simple</#if>" containLower="${(element.element.containLower!false)?string('true','false')}" showFormat="${showFormat!}" fill='${(property.fillcontent)!}' caseSensitive="${(element.element.caseSensitive!false)?string('true','false')}" crosscol="${(element.element.crosscol!false)?string('true','false')}">
										<#else>
											<tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}" partDepend="${(element.element.partDepend)!'common'}" selfType="${(element.element.selfType)!(element.element.columnType)!''}" key="${(element.element.name)!}" ondblclick="listec.selectRow('fast',this);listec.fastFieldProperty();" onmousedown="listec.selectRow('fast',this);" defaultValue="${(element.element.defaultValue!)?string}" propDefaultValue="${(element.element.defaultValue!)?string}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" partDepend="${(element.element.partDepend)!'common'}" columnType="${(property.type)!''}" name="${(element.element.name)!}" namekey="${(element.element.namekey)!}" key="${(element.element.key)!}" mnecode='${(element.element.mnecode!false)?string('true','false')}' iscrosscompany='${(element.element.iscrosscompany!false)?string('true','false')}' isrefselect='${(element.element.isrefselect!false)?string('true','false')}' propertyCode="${(element.element.propertyCode)!}" layRec="${(element.element.layRec)!}" nullable="${(element.element.nullable)?string("true","false")}" isreadonly="${(element.element.readonly)?string("true","false")}" exp="${(element.element.exp)!}" entityCode="${(element.element.entityCode)!}" layRec="${(element.element.layRec)!}" assTar="${(element.element.assTar)!}" assOrg="${(element.element.assOrg)!}" multable="${((element.element.multable)!false)?string('true','false')}" columnLong="${(element.element.columnLong)!}" readonly="${(element.element.readonly)?string("true","false")}" checkname="${ckname!}" showType="${(element.element.showType)!}"   sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${((element.callbackbody)!)?html}" callbackname="${((element.callbackname)!)?html}" funcname="${((element.funcname)!)?html}" funcbody="${((element.funcbody)!)?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!"")?html}" referenceview="${(element.element.referenceview)!}" modelcode="${(element.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" 
											showFormat="${(element.element.showFormat)!}" assPropertyName="${element.assPropertyName!}" assPropertyColumnName="${element.assPropertyColumnName!}" <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> modelDataType="simple" containLower="false" caseSensitive="${(element.element.caseSensitive!false)?string('true','false')}" crosscol="${(element.element.crosscol!false)?string('true','false')}" fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.element.fill.fillType)?has_content && (element.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.element.fill.fillOrder?has_content><#list element.element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.element.fill.fillContent)[ne])?html}"</#list><#else><#list (element.element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.element.fill.fillContent)[ne])?html}"</#list></#if>}<#else>"${((element.element.fill)[fe])?html}"</#if></#list></#if>}'>
										</#if>
											<td align="center" style="width:90%;">${getHtmlText('${(element.element.namekey)!element.element.name}')}</td>
											<td onclick="listec.deleteFastQueryField(this)"><img title="${getText('ec.view.cell.fastdel')}" style="cursor:pointer;" src="/bap/static/ec/delete.gif" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)'></img></td>
											</tr>
								</#if>
								</#list>
							<#break>
						</#if>
					</#list>
					</#if>
					</#if>
					</tbody>
				</table>
				</div>
				<div style="margin-left:10px;padding-top:20px;clear:both;">
				<label style="float:right;" id="fastquery-tips">${getHtmlText('ec.view.fastField.tips')}</label>
				</div>
			</div>
			<div id="fastContent" style="background-color:#f8f6f7;position:absolute;right:15px;width:15%;height:89%">
				<div style="padding-top:80px!important;" class="ec-list-btndiv"><div class="ec-list-topbtn" onclick="listec.firstRow('fast')" id="fastFirstMove" ></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-prevbtn" onclick="listec.upRow('fast')" id="fastUpMove" ></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-nextbtn" onclick="listec.downRow('fast')" id="fastDownMove"></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-lastbtn" onclick="listec.lastRow('fast')" id="fastLastMove"></div></div>
			</div>
		</div>
 	</div>
	
	<!-- 高级查询配置页面mkp -->
	<div id="adv_query_setting_dlg" style="display:none;">
		<div style="margin-left:2px;margin-top:8px;width: 160px; height:96%;float:left;background-color:white;border:1px solid #3C7FB1">
			<input type="hidden" id="avdDelCells" />
			<!-- 左侧 -->
			<div id="adv_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:89%">
					<ul class="main_properties_container">
						<#if subs?? && (subs.properties)?? && (subs.properties)?size &gt; 0>
							<#assign properties = (subs.properties)>
							<#list properties as p>
								<#if p.type != "LONGTEXT" && p.type != "OFFICE" && p.type != "PROPERTYATTACHMENT" && p.type != "COLOR">
								<#if (p.name) != 'tableInfoId' && (p.name) != 'status' && (p.name) != 'version' && p.name != 'layNo' && p.name != 'layRec' && (p.type) != "LONGTEXT">
								<li source='main' modelDataType="<#if (p.model.dataType)?? && (p.model.dataType) == 2>tree<#else>simple</#if>" onclick='listec.addAdvQueryField(this)' partDepend='common' propDefaultValue='${(p.defaultValue)!}' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' entityCode='${(p.model.code)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!true)?string('true','false')}' <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'>
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
								<li source='test' parentModelDataType='<#if (ass.targetProperty.model.dataType)?? && (ass.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='common' assTar='${ass.targetProperty.code}' assPropertyName='${ass.targetProperty.name}' propertyCode="${ass.originalProperty.code!}" assOrg="${ass.originalProperty.code}" dbname='${(ass.targetProperty.model.modelName)!}' name='${(ass.originalProperty.name)!}' entityCode='${(ass.targetProperty.model.code)!}' relativeName='${(ass.targetProperty.model.tableName)!},${(ass.targetProperty.columnName)},${(ass.originalProperty.model.tableName)!},${(ass.originalProperty.columnName)!}'>
									<img sourceType='adv' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(ass.targetProperty.model.code)!}","adv")'></img>
									${getHtmlText('${(ass.originalProperty.displayName)!}')} [${getHtmlText('${(ass.targetProperty.model.name)!}')}]
								</li>	
								</#if>
								<#assign i = i+1>
							</#list>
						</#if>
					</ul>
				</div>
				<h2>${getHtmlText('ec.view.one2manyattr')}</h2>
				<div class="accordion_pane" style="overflow:auto;height:87%"><ul class="onetomany_properties_container">
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
								<li source='test' parentModelDataType='<#if (onetoManyAss.targetProperty.model.dataType)?? && (onetoManyAss.targetProperty.model.dataType) == 2>tree<#else>simple</#if>' partDepend='one2many' assTar='${onetoManyAss.targetProperty.code}' propertyCode="${onetoManyAss.targetProperty.code!}" assOrg="${onetoManyAss.originalProperty.code}" dbname='${(onetoManyAss.targetProperty.model.modelName)!}' name='${(one2ManyTargetPropertyName)!}' entityCode='${(onetoManyAss.targetProperty.model.code)!}' relativeName='${(one2ManyOriginalTableName)!},${(onetoManyAss.targetProperty.columnName)!},${(onetoManyAss.originalProperty.model.tableName)!},${(onetoManyAss.originalProperty.columnName)}'>
									<img sourceType='adv' align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(onetoManyAss.targetProperty.model.code)!}","adv")'></img>
									${getHtmlText('${(onetoManyAss.targetProperty.displayName)!}')}[${getHtmlText('${(onetoManyAss.targetProperty.model.name)!}')}]
								</li>	
								<#assign j = j+1>
						</#list>
					 </#if>
				</ul></div>
			</div>
		</div>	
		<div id="advQueryContentList" style="width:490px;background-color:white;border:2px solid #D4D4D4;float:left;height:96%;margin: 8px 5px 0 7px;"> 	
			<div style="position:absolute;height:89%;width:56%;">
				<div style="float:right;padding-top:10px;">
					<a style="border:1px solid #fff;padding-right:15px;" class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.advFieldProperty()">${getHtmlText('ec.view.fastFieldProperty')}</a>
				</div>
				<div style="margin-left:10px;margin-top:40px;overflow-y:auto;clear:both;height:82%">
				<#if (ev.configMap)??  && (ev.configMap.layout)??>
					<#assign configMap = (ev.configMap.layout)>
					<#if ev.configMap.layout.sections??>
					<#list ev.configMap.layout.sections as secatt>
					<#if (secatt.regionType)?? && (secatt.regionType)=='ADVQUERY'>
					<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
					</#if>
					</#list>
					</#if>
				</#if>
				<#if (ev.configMap)??  && (ev.configMap.layout)??>
				<#assign configMap = (ev.configMap.layout)>
				<#if ev.configMap.layout.sections??>
				<#list ev.configMap.layout.sections as secatt>
				<#if (secatt.regionType)?? && (secatt.regionType)=='ADVQUERY'>
				<#if (secatt.advProperty)??>
				<#assign advProperty = secatt.advProperty>
				</#if>
				<#break>
				</#if>
				</#list>
				</#if>
				</#if>
				<table cellpadding="0" id="advColTable" style="font-size:12px;width:96%;" sectionCode="${sectionCode!}" isExpandAll="${((advProperty.isExpandAll)!true)?string('true','false')}" expandType="${(advProperty.expandType)!'all'}" cellspacing="0" align="center" class="infoTable">
					<tbody id="advColOrder">
					<#if (ev.configMap)??  && (ev.configMap.layout)??>
					<#assign configMap = (ev.configMap.layout)>
					<#if ev.configMap.layout.sections??>
					<#list ev.configMap.layout.sections as secatt>
						<#if (secatt.regionType)?? && (secatt.regionType)=='ADVQUERY'>
							<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
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
										<#if (element.element.propertyCode)?? &&propertyMap?? && propertyMap['${propCode}']?? >
											<#assign property = propertyMap['${propCode}'] >
											<#assign showType = element.element.showType!>
											<#if !element.element.showTypeHasChanged!false >
											<#assign showType = property.fieldType! >
											</#if>
											<#assign showFormat = element.element.showFormat!>
											<#if !element.element.showFormatHasChanged!false>
											<#assign showFormat = property.format! >
											</#if>
											<#if property.type?? && property.type=='BOOLEAN'>
												<#assign showType='SELECT'>
												<#assign showFormat = 'SELECT'>
											</#if>
											<tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}" key="${(element.element.name)!}" partDepend="${(element.element.partDepend)!'common'}" selfType="${(property.type)!}" ondblclick="listec.selectRow('adv',this);listec.advFieldProperty();" onmousedown="listec.selectRow('adv',this);" defaultValue="<#if element.element.defaultValueHasChanged!false>${(element.element.defaultValue!)?string}<#else>${(property.defaultValue!)?string}</#if>" propDefaultValue="${(property.defaultValue!)?string}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" partDepend="${(element.element.partDepend)!'common'}" columnType="${(property.type)!''}" name="${(element.element.name)!}" namekey="${(element.element.namekey)!}" key="${(element.element.key)!}" mnecode='${(property.isUsedMneCode!false)?string('true','false')}' iscrosscompany='${(element.element.iscrosscompany!false)?string('true','false')}' isrefselect='${(element.element.isrefselect!false)?string('true','false')}' propertyCode="${(element.element.propertyCode)!}" layRec="${(element.element.layRec)!}" nullable="<#if (element.element.nullable)?? && (property.nullable)?? && (element.element.nullable)?string("true","false") == 'true' && (property.nullable)?string("true","false") == 'true'>true<#else>false</#if>" isreadonly="${(element.element.readonly)?string("true","false")}" exp="${(element.element.exp)!}" entityCode="${(property.model.entity.code)!}" layRec="${(element.element.layRec)!}" assTar="${(element.element.assTar)!}" assOrg="${(element.element.assOrg)!}" multable="${((property.multable)!false)?string('true','false')}" columnLong="${(property.maxLength)!}" readonly="${(element.element.readonly)?string("true","false")}" checkname="${ckname!}"  sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${((element.callbackbody)!)?html}" callbackname="${((element.callbackname)!)?html}" funcname="${((element.funcname)!)?html}" funcbody="${((element.funcbody)!)?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!"")?html}" referenceview="${(element.element.referenceview)!}" modelcodes="${(element.element.modelcodes)!}" modelcode="${(property.model.code)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}"  
											propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(element.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}" 
											precision="${element.element.precision!}" propPrecision="${property.decimalNum!}" precisionHasChanged="${(element.element.precisionHasChanged!false)?string('true','false')}"  
											assPropertyName="${element.assPropertyName!}"
											showType="${showType!}" <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> modelDataType="<#if property.model.dataType == 2>tree<#else>simple</#if>"  containLower="${(element.element.containLower!false)?string('true','false')}" showFormat="${showFormat!}" fill='${(property.fillcontent)!}' caseSensitive="${(element.element.caseSensitive!false)?string('true','false')}" crosscol="${(element.element.crosscol!false)?string('true','false')}">
										<#else>
											<tr <#if element_index % 2 == 0>class="even-num" numType="even"<#else>class="odd-num" numType="odd"</#if> cellCode="${cellCode!}" partDepend="${(element.element.partDepend)!'common'}" selfType="${(element.element.selfType)!(element.element.columnType)!''}" key="${(element.element.name)!}" ondblclick="listec.selectRow('adv',this);listec.advFieldProperty();" onmousedown="listec.selectRow('adv',this);" defaultValue="${(element.element.defaultValue!)?string}" propDefaultValue="${(element.element.defaultValue!)?string}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" partDepend="${(element.element.partDepend)!'common'}" columnType="${(property.type)!''}" name="${(element.element.name)!}" namekey="${(element.element.namekey)!}" key="${(element.element.key)!}" mnecode='${(element.element.mnecode!false)?string('true','false')}' iscrosscompany='${(element.element.iscrosscompany!false)?string('true','false')}' isrefselect='${(element.element.isrefselect!false)?string('true','false')}' propertyCode="${(element.element.propertyCode)!}" layRec="${(element.element.layRec)!}" nullable="${(element.element.nullable)?string("true","false")}" isreadonly="${(element.element.readonly)?string("true","false")}" exp="${(element.element.exp)!}" entityCode="${(element.element.entityCode)!}" layRec="${(element.element.layRec)!}" assTar="${(element.element.assTar)!}" assOrg="${(element.element.assOrg)!}" multable="${((element.element.multable)!false)?string('true','false')}" columnLong="${(element.element.columnLong)!}" readonly="${(element.element.readonly)?string("true","false")}" checkname="${ckname!}" showType="${(element.element.showType)!}"   sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${((element.callbackbody)!)?html}" callbackname="${((element.callbackname)!)?html}" funcname="${((element.funcname)!)?html}" funcbody="${((element.funcbody)!)?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!"")?html}" referenceview="${(element.element.referenceview)!}" modelcodes="${(element.element.modelcodes)!}" modelcode="${(element.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" 
											showFormat="${(element.element.showFormat)!}" assPropertyName="${element.assPropertyName!}" <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> modelDataType="simple" containLower="false"  caseSensitive="${(element.element.caseSensitive!false)?string('true','false')}" crosscol="${(element.element.crosscol!false)?string('true','false')}" fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.element.fill.fillType)?has_content && (element.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.element.fill.fillOrder?has_content><#list element.element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.element.fill.fillContent)[ne])?html}"</#list><#else><#list (element.element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.element.fill.fillContent)[ne])?html}"</#list></#if>}<#else>"${((element.element.fill)[fe])?html}"</#if></#list></#if>}'>
										</#if>
											<td align="center" style="width:90%;">${getHtmlText('${(element.element.namekey)!element.element.name}')}</td>
											<td onclick="listec.deleteAdvQueryField(this)"><img title="${getText('ec.view.cell.fastdel')}" style="cursor:pointer;" src="/bap/static/ec/delete.gif" onMouseOver='deleteBtnChange(this)' onMouseOut='deleteBtnChange(this)'></img></td>
											</tr>
								</#if>
								</#list>
								</#if>
							<#break>
						</#if>
					</#list>
					</#if>
					</#if>
					</tbody>
				</table>
				</div>
				<div style="margin-left:10px;padding-top:20px;clear:both;">
				
				</div>
			</div>
			<div id="advContent" style="background-color:#f8f6f7;position:absolute;right:15px;width:15%;height:89%">
				<div style="padding-top:80px!important;" class="ec-list-btndiv"><div class="ec-list-topbtn" onclick="listec.firstRow('adv')" id="advFirstMove" ></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-prevbtn" onclick="listec.upRow('adv')" id="advUpMove" ></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-nextbtn" onclick="listec.downRow('adv')" id="advDownMove"></div></div>
				<div class="ec-list-btndiv"><div class="ec-list-lastbtn" onclick="listec.lastRow('adv')" id="advLastMove"></div></div>
			</div>
		</div>
 	</div>
	
 	<div id="operatorBar_setting_dlg" style="display:none;">
 		<@errorbar id="operatorBarDialogErrorBar"></@errorbar>
 		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="10%" style="padding-right:10px;">${getHtmlText('ec.view.classname')}</td>
				<td class="co" width="40%"><input type="text" id="serviceName" title="${getText('ec.view.whatclassnamefor')}"/></td>
			</tr>
		</table>
 	</div>
 	<#if (view.type)?? && view.type=='REFERENCE'>
 	<div id="selectListView" style="display:none;">
 	<table class="selectListTable" cellpadding="0" cellspacing="0" border="0" width="80%" align="center" style="margin-top: 28px">
		<tr>
	 		<td class="la"  name="view_assview_td">${getHtmlText('ec.view.listview')}</td>
			<td class="co" width="70%"  name="view_assview_td">
				<select name="view.assView.code"  style="width:100%">
					<#list assViews as av>
						<option value="${av.code}" 
						<#if  view ?? && view.assView ??&& view.assView.code = av.code>
							selected
						</#if>
						>
						${getText('${av.name}')}
						</option>
					</#list>
				</select>
			</td>
		</tr>
	</table>
 	</div>
 	</#if>
	<script type="text/javascript">
		var main_tab,listec;//全局变量
		var _proj_config_flag = ${isProj?string};
		var _viewAssModelCode = "${view.assModel.code}";
		$(function(){
			$('#form_select_elements').tabs("#form_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			$('#fast_select_elements').tabs("#fast_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			$('#adv_select_elements').tabs("#adv_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			main_tab = new CUI.TabView("main_form_tab",{tabposition:'top', removable: true});//初始化主设计区域页签
			listec = new CUI.EntityConfList(
					'${(view.code)!}',
					{pageConfig:{colNum:6}},
					main_tab,
					'${(ev.code)!}',
					'${(ev.version)!0}',
					<#if ev.view?? && ev.view.fastQueryJson?? && ev.view.fastQueryJson?has_content>
					<#list ev.view.fastQueryJson as fastQueryJson>
						'${(fastQueryJson.code)!}',
						'${(fastQueryJson.version)!0}',
						<#break>
					</#list>
				<#else>
				'',
				0,
				</#if>
				<#if ev.view?? && ev.view.advQueryJson?? && ev.view.advQueryJson?has_content>
					<#list ev.view.advQueryJson as advQueryJson>
						'${(advQueryJson.code)!}',
						'${(advQueryJson.version)!0}',
						<#break>
					</#list>
				<#else>
				'',
				0,
				</#if>
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
	</script>	
</body>
<script type="text/javascript">	
	//自动设置中间编辑区域的宽度
	function _init_size(){
		$('.col-main').width($(window).width()-230);
		$('#form_area').width($(window).width()-232);
		$('#listSection').width($(window).width()-279);
		listec.resizeListTable(); 
		$('#main_form_tab').width($(window).width()-232);
		$('.etv-navset').height($(window).height()-205);
		$('.col-sub').height($(window).height()-117);
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
		$('#assModels').change(function(){
			listec.modelViewChange(this);
		});
		$('body').keydown(function(e){
			if($('.ewc-dialog-blove','body').size() < 1) {
				if(e.keyCode == 46) {
					$('td,li','#main_design_container').each(
						function(index) {
						if($(this).hasClass('ui-selected')) {
							if($(this).parents().hasClass('form_design_ul')) {
								listec.delCell();
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
								listec.leftmove('buttonOperateDiv');
							}
							if($(this).parents().hasClass('list_design_ul')) {
								listec.leftmove('listSection');
							}
						}
					});
				}
				if(e.keyCode == 39) {
					$('td,li','#main_design_container').each(
						function(index) {
						if($(this).hasClass('ui-selected')) {
							if($(this).parents().hasClass('operateButton_ul')) {
								listec.rightmove('buttonOperateDiv');
							}
							if($(this).parents().hasClass('list_design_ul')) {
								listec.rightmove('listSection');
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
		    	liArr.removeClass('selected');
				$(this).addClass('selected');
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
		var liArr0 = $("#dlg-div-field").find('li');
		liArr0.each(function(index){
		    var objMe = $(this);
			var objId = $(objMe).attr('id');
		    $(this).unbind('click').bind('click',function(){
		    	liArr0.removeClass('selected')
				$(this).addClass('selected');
		        if(objId === 'field_base_attr'){
		            $('#field-tab-common').css('display','block');
		            $('#field-div-event').css('display','none');
		            $('#field-tab-style').css('display','none');
		        }else if(objId === 'field_event_defined'){
		            $('#field-tab-common').css('display','none');
		            $('#field-div-event').css('display','block');
		            $('#field-tab-style').css('display','none');
		        }else if(objId === 'field_css_defined'){
		            $('#field-tab-common').css('display','none');
		            $('#field-div-event').css('display','none');
		            $('#field-tab-style').css('display','block');          
		        }
		    });
		});
		var liArr1 = $("#dlg_div_public").find('li');
		liArr1.each(function(index){
		    var objMe = $(this);
			var objId = $(objMe).attr('id');
		    $(this).unbind('click').bind('click',function(){
		    	liArr1.removeClass('selected');
				$(this).addClass('selected');
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
	$(window).resize(function(){
		_init_size();				
	});
	setTimeout(function(){
		$('.etv-content').width($(window).width()-234);
		listec._init();
		changeInput();
	},200);
	function selectScriptCode(){
		var windowStyle = "width=800,height=450,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		<#if view?exists&&view.entity?exists>
			var param="entityCode=${view.entity.code}";
		<#elseif entity?? && entity.code??>
			var param="entityCode=${entity.code}";
		</#if>
		
		foundation.common.callOpenWeb("script",windowStyle,null,true,null,"setScript",param);
		
	}

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
	
	function openConfigRelationModelPropsDlg(){
		var configRelationModelPropsDlg = new CUI.Dialog({
			title    :  '配置关联模型字段',
			width    :  550,
			height   :  422,
			url      :  "/msService/ec/view/configRelationModelPropsFrame?modelCode=${view.assModel.code}",
			modal    :  true,
			dragable :  true,
			buttons  :  [{ name    : "${getHtmlText('common.button.save')}",
						   handler : function(){
						   		if ( ec.config.relationModel.saveConfig() ) {
						   			this.close();
						   		}
						   }
						},
						{ name    : "${getHtmlText('common.button.cancel')}",
						  handler : function(){
						  	this.close();
						  }
						}]
		});
		configRelationModelPropsDlg.show();
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
	
	function changedbcustom(obj){
		if($(obj).prop('checked')){
			$('#db_customt_extarea').show();
		}else{
			$('#db_customt_extarea').hide();
		}
	}
	CUI.ns("ec.config.customProp");
	/**
	* 选择展示哪个模型的自定义字段
	*/
	ec.config.customProp.selectModel = function(e, obj) {
		YUE.stopPropagation(e);
		ec.config.customProp.showOverLayer(obj, "/msService/ec/view/config-select-model?model.code=${(view.assModel.code)!}");
	}
	
	ec.config.customProp.showOverLayer = function(obj,url){
		CUI('#customContent').html("");
		ec.config.customProp.showOverLayerDiv = new CUI.Overlay({
			align   : obj,
	     	el      : 'customContent',
	     	title   : '选择模型',
	     	width   : 180,
	     	height  : 282,
	     	zIndex  : 9999,
	     	shadow  : false,
			buttons : [
						{	name:"${getHtmlText('foundation.workbench.mainPage.sure')}",
							handler:function(){
								if ( $('li[isselected="1"]').length == 0 ) {
									$('body').trigger('click.overlayer2');
									CUI.Dialog.alert('${getText("请先选择模型！")}');
								} else {
									propertyDblClickFunc($('li[isselected="1"]')[0]);
									$('body').trigger('click.overlayer2');
								}
							}
						},
						{	name:"${getHtmlText('calendar.common.cancal')}",
							handler:function(){$('body').trigger('click.overlayer2');}
						}
					  ]
	     	
		});
		ec.config.customProp.showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url += "&time=" + new Date();
		ec.config.customProp.showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
	
	$(function(){
		$('body').unbind('click.overlayer2').bind('click.overlayer2', function(){
			if(ec.config.customProp.showOverLayerDiv && ec.config.customProp.showOverLayerDiv.isShow) {
				ec.config.customProp.showOverLayerDiv.close();
				$('#overlay-shim-ie').remove();
			}
		});
	});
</script>
<@customerCondition viewCode="${view.code}"/>
</html>
