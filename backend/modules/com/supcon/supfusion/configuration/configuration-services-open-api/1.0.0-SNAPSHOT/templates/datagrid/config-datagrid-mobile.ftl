<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	<title>${getText(dataGrid.view.assModel.entity.name)}-${getText(dataGrid.view.displayName)}-${getText(dataGrid.dataGridName!)}</title>
	<@ec_datagridTop />

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
		#main_form_tab_operate li{display:block;}
		#main_form_tab .etv-content{background-color:#F3F3F3!important}
		#main_design_container{padding:2px 2px 0 0;}
		.form_title{height:30px;width:99%;background-color:#d4d4d4;margin:6px 0px 0px 5px}
		.line-icon{width:110px;margin:2px 3px 2px 2px; border-color:#d4d4d4;overflow:hidden;}
		#form_area{position: relative;height:95%;padding-top:10px;}
		#main_form_tab_operate{z-index: 1000;}
		#main_form_tab .etv-content{min-height:500px;}
		#listSection{height:46px;background:#bababa;overflow-x:scroll;overflow-y:hidden;}	
		.list_design_ul_li{border:#CCC 1px dashed;height:25px;width:97px;border-right:1px solid #FFF;margin-top:1px;margin-bottom:1px;}
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
		.se-selected { background: #F39814;}
		.BlockInfoDiv {margin:2px;padding:4px;border-top:1px dashed #cccccc;}
		.editeventli {width:175px;text-align: right;}
		a.normala{text-decoration: underline;padding-left:2px;cursor:pointer;color:blue; display:inline-block;height:15px;overflow:hidden;}
		.operateDiv{height:22px!important;background:#bababa;border:#CCC 1px dashed;}
	</style>
</head>
<body class="ec-config-page" style="background-color:#f2f2f2">
	<@errorbar id="workbenchErrorBar" offsetY=83 />
	<div class="layout grid-s4m0"> 	
		<div class="col-sub">
			<!-- 左侧 -->
			<div id="form_select_elements">
				<h2 class="current">${getHtmlText('ec.view.entityattribute')}</h2>
				<div class="accordion_pane" style="display:block;overflow:auto;height:94%">
					<#if model??>
						<#assign properties = (model.properties)![]>
						<#assign associatedInfos = (model.associatedInfos)![]>
						<#assign associatedInfosMap = {}>
						<ul class="main_properties_container">
							<#if (properties)?? && (properties)?size &gt; 0>
								<#list properties as p>
								<#if (!(p.isCustom!false)||(p.projCustomInUse)!false) && p.type != "PICTURE">
									<#if !assMap?? || (assMap?? && !assMap[p.code]??)>
									<li source='main' propertyCode='${(p.code)!}' namekey="${(p.displayName)!}" propDefaultValue='${(p.defaultValue!)?string}' name='${(p.name)!}' columnType='${(p.type)!}' propShowFormat='${(p.format)!}' propShowType='${(p.fieldType)!}' multable='${(p.multable!false)?string('true','false')}'  nullable='${(p.nullable!false)?string('true','false')}' propnullable='${(p.nullable)?string('true','false')}'  <#if p.isUsedForList?? && p.isUsedForList> list='true' <#else> list='false' </#if> fillContent='${(p.fillcontent)!}' layRec='${(p.name)!}' mnecode='${(p.isUsedMneCode!false)?string('true','false')}' propPrecision='${(p.decimalNum)!}'  isCustom='${((p.isCustom)!false)?string}'>
											${getHtmlText('${(p.displayName)!}')}
										</li>
									<#elseif assMap?? && assMap[p.code]??>	
										<#assign i = 1>
										<li source='test' nullable='${(p.nullable!false)?string}' propnullable='${(p.nullable)?string}' assTar='${p.associatedProperty.code}' propertyCode='${p.code}' assOrg="${p.code}" dbname='${(p.associatedProperty.model.modelName)!}' name='${(p.name)!}' modelcode='${(p.associatedProperty.model.code)!}' entitycode='${(p.associatedProperty.model.code)!}' relativeName='${(p.associatedProperty.model.tableName)!},${(p.associatedProperty.columnName)},${(p.model.tableName)!},${(p.columnName)!}' isCustom='${((p.isCustom)!false)?string}'>
											<img sourceType="fast" align='absmiddle' src='/bap/static/treeview/assets/ectree_colse.gif' flag='true' hasclick='false' onclick='listec.showAssProperty(this,"${(p.associatedProperty.model.code)!}")'></img>
											${getHtmlText('${(p.displayName)!}')} [${getHtmlText('${(p.associatedProperty.model.name)!}')}]
										</li>
									</#if>
									</#if>
								</#list>
							</#if>
						</ul>
					</#if>	
				</div>
				<h2>${getHtmlText('ec.view.formelement')}</h2>
				<div class="accordion_pane">
					<ul id="form_elements_container">
						<li source='el' showType="MULTSELECT" name="multselect">${getHtmlText('ec.view.property.multiselect')}</li>
					</ul>
				</div>
			</div>
		</div>	
		<div class="col-main"> 			
			<div class="main-wrap" id="main_design_container">
				<div id="buttonOperateDiv" style="margin:10px 10px 15px 10px;padding-bottom:5px;background:#F3F3F3 !important;border:1px solid #0099CC;">
					<div class="operateDiv">
						<label for="button"><font style="font-weight:bolder">${getHtmlText('ec.view.configoperator')}</font></label>
						<div style="padding-left:20px;display:inline-block">
						<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addOperateButton()">${getHtmlText('ec.view.add')}</a>
						<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="listec.modifyOperateButton()">${getHtmlText('common.button.edit')}</a>
						<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.deleteOperateButton()">${getHtmlText('common.button.delete')}</a>
						<a class="cui-btn mr10 cui-btn-leftmove" href="#" onclick="listec.leftmove('buttonOperateDiv')">${getHtmlText('ec.view.toleft')}</a>
						<a class="cui-btn mr10 cui-btn-rightmove" href="#" onclick="listec.rightmove('buttonOperateDiv')">${getHtmlText('ec.view.toright')}</a>
						</div>
					</div>
					<div style="border:#CCC 1px solid;height:45px;margin:5px 5px 0 5px;">
						<div style="padding-top:10px;padding-left:10px;">
							<#if (ev.configMap)??  && (ev.configMap.layout)??>
							<#assign configMap = (ev.configMap.layout)>
							<#if (configMap.sections)??>
							<#list (configMap.sections) as section>
								<ul id="operateButton_ul" sectionCode="${(section.sectionCode)!}">
								<#if (section.regionType)?? && (section.regionType)=='BUTTON'>
									<#if section.cells??>
										<#list (section.cells) as operateButton>
											<li cellCode="${operateButton.cellCode!ecCodeInit()}" id="${(operateButton.id)!}" namekey="${(operateButton.namekey)!}" showname="${((operateButton.showname)!)?html}" buttonstyle="${((operateButton.buttonstyle)!)?html}"
											<#if (operateButton.useInMore)??>
												useInMore="${(operateButton.useInMore)?string('true','false')}" 
											<#else>
												useInMore="false" 
											</#if>
											<#if (operateButton.funcname)??>
												funcname="${((operateButton.funcname)!)?html}" 
											</#if>
											<#if (operateButton.funcbody)??>
												funcbody="${((operateButton.funcbody)!)?html}"
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
											<#if (operateButton.iscustomfunc)??>
												iscustomfunc="${(operateButton.iscustomfunc)?string('true','false')}"
											</#if>
											<#if (operateButton.operateurl)??>
												operateurl="${((operateButton.operateurl)!)?html}"
											</#if>
											<#if (operateButton.isconfirm)??>
												isconfirm="${(operateButton.isconfirm)?string('true','false')}"
											</#if>
											<#if (operateButton.confirmcontent)??>
												confirmcontent="${((operateButton.confirmcontent)!)?html}"
											</#if>
											<#if (operateButton.isSignatureConfig)??>
												isSignatureConfig="${(operateButton.isSignatureConfig)?string('true','false')}"
											</#if> 
											<#if (operateButton.releaseFelid)??>
												releaseFelid="${((operateButton.releaseFelid)!)?html}" 
											</#if>
											test='1' 
											 class="button_design_ul_li" onmousedown="listec.selectListLi(this)" ondblclick="listec.modifyOperateButton()" <#if operateButton.operatetype?? && operateButton.operatetype =='IMPORT'> style="display:none"</#if>><dd><input type="button" class="Dialog_button btn_pointer" value="${getText('${((operateButton.namekey)!)}')?html}"></dd></li>
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
				<!--<hr style="margin-left:8px;margin-right:8px">
				<div class="operateDiv" style="margin:5px 8px 0 8px;">
					<label for="button" style="padding-left:15px;"><font style="font-weight:bolder">${getText('ec.view.cfgList')}</font></label>
					<div style="padding-left:20px;display:inline-block">
						<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addLine(1)">${getText('ec.view.addline')}</a>
						<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.delRow()">${getText('ec.view.deleteline')}</a>
						<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.delCell()">${getText('ec.model.propertyDel')}</a>
						<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.setPropertys()">${getText('ec.view.fieldattribute')}</a>
						<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.sectionProperty()">${getText('ec.view.layout')}</a>
						<a class="cui-btn mr10 cui-btn-move" href="#" onclick="listec.splitCell()">${getText('ec.view.splitcell')}</a>
					</div>
				</div>-->
				<div id="form_area" style="padding-left:3px;">
					<input type="hidden" id="viewCode" value="${(dataGrid.code)!}" />
					<input type="hidden" id="modelCode" value="${(model.code)!}" />
					<input type="hidden" id="modelType" value="${(model.dataType)!}" />
					<input type="hidden" id="viewType" value="DATAGRID" />
					<input type="hidden" id="delCellIds" name="delCellIds" />
					<input type="hidden" id="btDelCellIds" name="btDelCellIds" />
					<input type="hidden" id="delEventIds" name="delEventIds" />
					<input type="hidden" id="delValidateIds" name="delValidateIds" />
					<div id="main_form_tab" class="etv-navset">
						<ul class="etv-nav">
							<li class="selected">DataGrid</li>
						</ul>
						<div style="background-color:#e3f1f9;" class="etv-content" >
							<input type="hidden" id="mainEntityName" value="${(subs.mainEntityName)!}"/>
							<#if !(ev.configMap.layout)??>
								<div class="div-selected">
									<ul class="form_design_ul se-selected" colNum="8"></ul>
							<#else>
							<#if (ev.configMap)??  && (ev.configMap.layout)??>
							<#assign configMap = (ev.configMap.layout)>
							<#if (configMap.sections)??>
							<#list (configMap.sections) as section>
							<#if section?? && section.regionType=='FASTQUERY'>
							<#if !section.cells??>
								<div class="div-selected">
									<ul class="form_design_ul se-selected" colNum="8"></ul>
							<#else>
								<#--<div class="div-selected">
									<#list configMap.get('fastsections') as secatt>
										<ul class="form_design_ul se-selected" id="designarea" colwidth="${(secatt.pageConfig.colwidth)!}" colNum="${(secatt.pageConfig.colNum)!8}" name="${(secatt.name)!}" isborder="${(secatt.isborder)!1}">
											<#if (secatt.content)['form']??>
											<#list (secatt.content)['form'] as element>
											<#if (element.element)??>
											<li class="form_design_ul_li" ondblclick="listec.setPropertys()" onmousedown="listec.selectListLi(this)" name="${(element.element.name)!}" mnecode='${(element.element.mnecode!false)?string('true','false')}' propertyCode="${(element.element.propertyCode)!}" layRec="${(element.element.layRec)!}" nullable="${(element.element.nullable)?string("true","false")}" isreadonly="${(element.element.readonly)?string("true","false")}" exp="${(element.element.exp)!}" entityCode="${(element.element.entityCode)!}" layRec="${(element.element.layRec)!}" assTar="${(element.element.assTar)!}" assOrg="${(element.element.assOrg)!}" multable="${((element.element.multable)!false)?string('true','false')}" columnLong="${(element.element.columnLong)!}" readonly="${(element.element.readonly)?string("true","false")}" checkname="${(element.element.checkname)!}" fieldType="${(element.element.fieldType)!}"  fillType="${(element.element.fillType)!}" fillContent="${(element.element.fillContent)!}" sourcepropertyname="${(element.sourcepropertyname)!}" callbackbody="${(element.callbackbody)!}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${(element.funcbody)!}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${(element.cssstyle)!""}" referenceview="${(element.element.referenceview)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" 
											fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.element.fill.fillType)?has_content && (element.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.element.fill.fillOrder?has_content><#list element.element.fill.fillOrder?split(",") as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${(element.element.fill.fillContent)[ne]}"</#list><#else><#list (element.element.fill.fillContent)?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${(element.element.fill.fillContent)[ne]}"</#list></#if>}<#else>"${(element.element.fill)[fe]}"</#if></#list></#if>}'>
												<#if (element.element.fieldType) == 'LABEL'>
													<dd>${(element.name)!}</dd>
												<#elseif (element.element.fieldType) == 'BUTTON'>
													<dd><input type="button" style="width:70px!important" value="${(element.name)!}"/></dd>
												<#else>
													<dd>
														<#if (element.element.fieldType) == 'DATE' || (element.element.fieldType) == 'DATETIME'>
															<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/><input type='button' class='cui-calpick' />
														<#elseif (element.element.fieldType) == 'TEXTFIELD'>
															<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/>
														<#elseif (element.element.fieldType) == 'SELECTCOMP'>
															<input type="text" style="color:gray" value="${(element.element.checkname)!''}"/><input type='button' class='cui-search-click' />
														<#elseif (element.element.fieldType) == 'SELECT'>
															<select style="color:gray"><option>${(element.element.checkname)!''}</option></select>
														<#elseif (element.element.fieldType) == 'RADIO'>
															<input type="radio"/><font color="gray">${(element.element.checkname)!}</font>
														<#elseif (element.element.fieldType) == 'CHECKBOX'>
															<input type="checkbox" /><font color="gray">${(element.element.checkname)!''}</font>
														</#if>
													</dd>
												</#if>
												<div></div>
											</li>
											<#else>
												<li class="form_design_ul_li" ondblclick="listec.setPropertys()" onmousedown="listec.selectListLi(this)" colspan="1" rowspan="1"><dd></dd><div></div></li>
											</#if>
											</#list>
											</#if>
										</ul>
										</#list>
										-->
								</#if>
								</#if>
								</#list>
								</#if>
								</#if>
							</#if>
								<#--<div style="height:135px"></div>-->
								<hr>
								<div class="operateDiv">
									<label for="button"><font style="font-weight:bolder">${getHtmlText('ec.view.cfgfield')}</font></label>
									<div style="padding-left:20px;display:inline-block">
										<a class="cui-btn mr10 cui-btn-add" href="#" onclick="listec.addCustomListField()">${getHtmlText('ec.view.addCol')}</a>
										<a class="cui-btn mr10 cui-btn-del" href="#" onclick="listec.delListField()">${getHtmlText('ec.view.delcol')}</a>
										<a class="cui-btn mr10 cui-btn-edit" href="#" onclick="listec.setColumnPropertys()">${getHtmlText('ec.view.colattribute')}</a>
										<a class="cui-btn mr10 cui-btn-setting" href="#" onclick="listec.listProperty()">${getHtmlText('ec.view.listproperty')}</a>
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
											<#if !(ev.configMap.layout)??>
												<tr class="list_design_ul" id="list_design_section" isCheckBox="false" isFirstLoad="false" pendingDeal="false" isTransCondition="false" conditionContent=""  autoAddRow="false" renderOver="" ptPageInit="" <#if dataGrid.view?? && dataGrid.view.type=="EDIT"> isEditable="true"</#if>><tr>
											<#else>
											<#if (ev.configMap)??  && (ev.configMap.layout)??>
												<#assign configMap = (ev.configMap.layout)>
												<#if (configMap.sections)??>
												<#list (configMap.sections) as section>
												<#if section?? && section.regionType=='DATAGRID'>
												<#if section.cells?? && (section.cells)?size <= 0>
													<tr class="list_design_ul" id="list_design_section" isTreeView="false" isAddChild="false" isDelele="false" isAddPrev="false" isAddNext="false" isMoveUp="false" isMoveDown="false" isCheckBox="false" isLevelUp="false" isLevelDown="false" isFirstLoad="false" pendingDeal="false" isTransCondition="false" conditionContent=""  autoAddRow="false" renderOver="" ptPageInit=""><tr>
												<#else>
													<#assign sectionCode = section.sectionCode!ecCodeInit('section')>
													<#if (configMap.listProperty)??>
													<#assign listProperty = (configMap.listProperty)>
													<tr class="list_design_ul" id="list_design_section"  
													isTreeView="${(listProperty.isTreeView!false)?string("true","false")}" 
													isAddChild="${(listProperty.isAddChild!false)?string("true","false")}" 
													isDelete="${(listProperty.isDelete!false)?string("true","false")}" 
													isAddPrev="${(listProperty.isAddPrev!false)?string("true","false")}" 
													isAddNext="${(listProperty.isAddNext!false)?string("true","false")}" 
													isMoveUp="${(listProperty.isMoveUp!false)?string("true","false")}" 
													isMoveDown="${(listProperty.isMoveDown!false)?string("true","false")}" 
													isLevelUp="${(listProperty.isLevelUp!false)?string("true","false")}" 
													isLevelDown="${(listProperty.isLevelDown!false)?string("true","false")}"
													copyRow="${(listProperty.copyRow!false)?string("true","false")}"
													copyColumn="${(listProperty.copyColumn!false)?string("true","false")}"   
													isSuperTable="${(listProperty.isSuperTable!false)?string("true","false")}"
													isTransverseShow="${(listProperty.isTransverseShow!false)?string("true","false")}"
													cannotAddNewRow="<#if listProperty.cannotAddNewRow?? && listProperty.cannotAddNewRow>true<#else>false</#if>" 
													nodeExpanded="<#if listProperty.nodeExpanded?? && listProperty.nodeExpanded>true<#else>false</#if>" 
													renderOver="${(listProperty.renderOver!)?html}" 
													ptPageInit="${(listProperty.ptPageInit!)?html}"
													displayRowsCount="${(listProperty.displayRowsCount!)?string}"
													isEditable="<#if listProperty.isEditable?? && !listProperty.isEditable>false<#else>true</#if>" isCheckBox="${(listProperty.isCheckBox)?string("true","false")}" isFirstLoad="${(listProperty.isFirstLoad)?string("true","false")}" pendingDeal="${(listProperty.pendingDeal)?string("true","false")}" isTransCondition="<#if listProperty.isTransCondition??>${listProperty.isTransCondition?string("true","false")}<#else>false</#if>" conditionContent="${(listProperty.conditionContent)!''}" importExcel="${(listProperty.importExcel!false)?string("true","false")}" canNotDeleteRow="${(listProperty.canNotDeleteRow!false)?string("true","false")}" canInsertRow="${(listProperty.canInsertRow!false)?string("true","false")}" autoAddRow="${(listProperty.autoAddRow!false)?string("true","false")}" exportExcel="${(listProperty.exportExcel!false)?string("true","false")}"
													isEasytable="${(listProperty.isEasytable!false)?string("true","false")}">
													<#else>
													<tr class="list_design_ul" id="list_design_section" renderOver="" ptPageInit="" isTreeView="false" isAddChild="false" isDelele="false" isAddPrev="false" isAddNext="false" isMoveUp="false" isMoveDown="false" isCheckBox="false" isLevelUp="false" isLevelDown="false" isCheckBox="false" isFirstLoad="false" pendingDeal="false" isTransCondition="false" conditionContent="" cannotAddNewRow="<#if listProperty.cannotAddNewRow?? && listProperty.cannotAddNewRow>true<#else>false</#if>" nodeExpanded="<#if listProperty.nodeExpanded?? && listProperty.nodeExpanded>true<#else>false</#if>" autoAddRow="${(listProperty.autoAddRow!false)?string("true","false")}">
													</#if>
														<#list section.cells as column>
															<#assign cellCode = column.cellCode!ecCodeInit()>
															<#if !(column.none)??>
																<#if (column.propertyCode)??>
																	<#assign propCodes = (column.propertyCode)?split('||')>
																	<#assign propCode = propCodes[propCodes?size - 1]>
																	<#assign objectCode = propCodes[0]>
																<#else>
																	<#assign propCode = 'null'>
																</#if>
																<#if (column.propertyCode)?? && propertyMap['${(propCode)!}']??>
																<#assign prop = propertyMap['${(propCode)!}'] >
																<#if propertyMap[objectCode]??>
																	<#assign objectProperty = propertyMap[objectCode]>
																	<#if objectCode == propCode> <#-- 不是对象 -->
																		<#assign objectProperty = prop>
																	</#if>
																</#if>
																<td class="list_design_ul_li" 
																	cellCode='${cellCode!}' 
																	name='${(column.key)!}' 
																	namekey="${(column.namekey)!}" 
																	isCount=${(column.isCount!false)?string("true","false")} 
																	isTotal=${(column.isTotal!false)?string("true","false")}
																	key='${(column.key)!}' 
																	<#if (column.validate)??>validates = "<#list column.validate as validate><#if validate_index gt 0>~~~~</#if>${validate.type}<#if validate.param??><#list (validate.param)?keys as paramKey>~~${paramKey}~${validate.param[paramKey]?html}<#if paramKey=='errorMsg'>~~international_errorMsg_showName~${getText("${validate.param[paramKey]}")}</#if></#list></#if></#list>"</#if>
																	propDefaultValue="${(prop.defaultValue!)?string}" 
																	defaultValue="<#if column.defaultValueHasChanged!false>${(column.defaultValue!)?string}<#else>${(prop.defaultValue!)?string}</#if>"  
																	defaultValueHasChanged="${(column.defaultValueHasChanged!false)?string('true','false')}" 
																	<#if prop.multable??>multable=${(prop.multable)?string("true","false")}<#else>multable=false</#if>
																	<#if column.isLink??>isLink=${(column.isLink)?string("true","false")}</#if>
																	showFormatFunc="${(column.showFormatFunc?html)!}"  
																	assModels='${(column.assModels)!}' 
																	linkView='${(column.linkView)!}' 
																	assModelCode='${(column.assModelCode)!}' 
																	cssstyle='${(column.cssstyle)!}'
																	showType="<#if propCodes?size gt 1 || column.showTypeHasChanged!false>${(column.showType)!}<#else>${(prop.fieldType!)}</#if>" 
																	showFormat="<#if propCodes?size gt 1 || column.showFormatHasChanged!false>${(column.showFormat)!}<#else>${(prop.format!)}</#if>" 
																	propShowType="${(prop.fieldType!)}"  
																	propShowFormat="${(prop.format!)}"  
																	showTypeHasChanged="${(column.showTypeHasChanged!false)?string('true','false')}"  
																	showFormatHasChanged="${(column.showFormatHasChanged!false)?string('true','false')}" 
																	precision="<#if column.precisionHasChanged!false>${column.precision!}<#else>${(prop.decimalNum!)}</#if>"  
																	propPrecision="${prop.decimalNum!}" 
																	precisionHasChanged="${(column.precisionHasChanged!false)?string('true','false')}" 
																	columnType='${(prop.type)!}' 
																	layRec='${(column.layRec)!}' 
																	formulas="${(column.formulas)!''}" 
																	textalign="${(column.textalign)!'center'}" 
																	fill='${(prop.fillcontent)!}'
																	ishide='${(column.isHidden!false)?string('true','false')}'
																 	isreadonly='${(column.isreadonly!false)?string('true','false')}' <#if (column.ass)?? && column.ass?has_content><#list column.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(column.ass)[asstar]!}'</#list></#if>
																	nullable='<#if (column.nullable)?? && column.nullable>true<#else>false</#if>' 
																	<#if (prop.type)?? && (prop.type) == 'LONGTEXT'>
																	popView='${(column.popView!true)?string('true','false')}' 
																	<#else>
																	popView='${(column.popView!false)?string('true','false')}' 
																	</#if>
																	propnullable='<#if (objectProperty.nullable)?? && objectProperty.nullable>true<#else>false</#if>'  
																	mneenable="${((column.mneenable)!false)?string('true','false')}" 
																	isgroup='${(column.isgroup!false)?string('true','false')}' 
																	<#if (column.ass)?? && column.ass?has_content><#list column.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(column.ass)[asstar]!}'</#list></#if>
																 	propertyCode='${(column.propertyCode)!}'
																 	modelcode='${(column.modelcode)!}'
																	referenceview='${(column.referenceview)!}' 
																 	checkname='${(column.label)!}'
																 	hide=${(column.hide)?string("true","false")}
																 	isCustom='${(column.isCustom!false)?string('true','false')}'
																 	isTreeNode=${(column.isTreeNode!false)?string("true","false")}
																 	width=${(column.width)!100}
																 	refCondition="${((column.refCondition)!)?html}"
																	funcname="${((column.funcname)!)?html}"
																	funcbody="${((column.funcbody)!)?html}"
																	callbackbody="${((column.callbackbody)!)?html}" 
																	callbackname="${((column.callbackname)!)?html}" 
																	<#if column.showType?? && column.showType=='MULTSELECT'>
																	ondblclick="listec.cellMultSelectProperty()" 
																	<#else>
																 	ondblclick="listec.setPropertys()" 
																	</#if>
																 	onmousedown="listec.selectListLi(this)" 
																 	<#if column.directasso??>directasso=${column.directasso!}</#if>
																 	<#if column.indirectasso??>indirectasso=${column.indirectasso!}</#if>
																 	<#if column.displayfield??>displayfield=${column.displayfield!}</#if>
																 	autoAddRow="${(listProperty.autoAddRow!false)?string("true","false")}"
																 	cannotAddNewRow="<#if listProperty.cannotAddNewRow?? && listProperty.cannotAddNewRow>true<#else>false</#if>"
																 	nodeExpanded="<#if listProperty.nodeExpanded?? && listProperty.nodeExpanded>true<#else>false</#if>"
																 	proWidth=${(column.width)!100}
																 	style="width: ${(column.width)!100}px"
																 ><dd>${(column.label)!}</dd></td>
																<#else>
																	<td class="list_design_ul_li" 
																	cellCode='${cellCode!}'  
																	name='${(column.key)!}' 
																	namekey="${(column.namekey)!}" 
																	isCount=${(column.isCount!false)?string("true","false")} 
																	isTotal=${(column.isTotal!false)?string("true","false")}
																	key='${(column.key)!}' 
																	propDefaultValue="${(column.defaultValue!)?string}" 
																	defaultValue="${(column.defaultValue!)?string}"  
																	defaultValueHasChanged="${(column.defaultValueHasChanged!false)?string('true','false')}" 
																	<#if column.multable??>multable=${(column.multable)?string("true","false")}<#else>multable=false</#if>
																	<#if column.isLink??>isLink=${(column.isLink)?string("true","false")}</#if> 
																	showFormatFunc="${(column.showFormatFunc?html)!}" 
																	assModels='${(column.assModels)!}' 
																	linkView='${(column.linkView)!}' 
																	assModelCode='${(column.assModelCode)!}' 
																	mnecode='${(column.mnecode!false)?string('true','false')}' 
																	showType='${(column.showType)!}' 
																	showFormat='${(column.showFormat)!}' 
																	columnType='${(column.columnType)!}' 
																	layRec='${(column.layRec)!}' 
																	formulas="${(column.formulas)!''}" 
																	textalign="${(column.textalign)!'center'}" 
																	fill='{<#if (column.fill)?has_content><#list (column.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (column.format) == 'ENUMERATE' && fe == 'fillContent'>{<#list (column.fill)[fe]?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((column.fill)[fe][ne])?html}"</#list>}<#else>"${((column.fill)[fe])?html}"</#if></#list></#if>}'
																 	ishide='${(column.ishide!false)?string('true','false')}'
																 	isCustom='${(column.isCustom!false)?string('true','false')}'
																 	isreadonly='${(column.isreadonly!false)?string('true','false')}' <#if (column.ass)?? && column.ass?has_content><#list column.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(column.ass)[asstar]!}'</#list></#if>
																	nullable='<#if (column.nullable)?? && column.nullable>true<#else>false</#if>' 
																	<#if (column.columnType)?? && (column.columnType) == 'LONGTEXT'>
																	popView='${(column.popView!true)?string('true','false')}' 
																	<#else>
																	popView='${(column.popView!false)?string('true','false')}' 
																	</#if>
																	mneenable="${((column.mneenable)!false)?string('true','false')}" 
																	isgroup='${(column.isgroup!false)?string('true','false')}' 
																	<#if (column.ass)?? && column.ass?has_content><#list column.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(column.ass)[asstar]!}'</#list></#if>
																 	propertyCode='${(column.propertyCode)!}'
																 	modelcode='${(column.modelcode)!}'
																	referenceview='${(column.referenceview)!}' 
																 	checkname='${(column.label)!}'
																 	hide=${(column.hide)?string("true","false")}
																 	isTreeNode=${(column.isTreeNode!false)?string("true","false")}
																 	width=${(column.width)!100}
																 	refCondition="${((column.refCondition)!)?html}"
																	funcname="${((column.funcname)!)?html}"
																	funcbody="${((column.funcbody)!)?html}" 
																	callbackbody="${((column.callbackbody)!)?html}" 
																	callbackname="${((column.callbackname)!)?html}"
																 	<#if column.showType?? && column.showType=='MULTSELECT'>
																	ondblclick="listec.cellMultSelectProperty()" 
																	<#else>
																 	ondblclick="listec.setPropertys()" 
																	</#if>
																 	onmousedown="listec.selectListLi(this)" 
																 	<#if column.directasso??>directasso=${column.directasso!}</#if>
																 	<#if column.indirectasso??>indirectasso=${column.indirectasso!}</#if>
																 	<#if column.displayfield??>displayfield=${column.displayfield!}</#if>
																	<#if (column.customSection)!false>
																 	customSection="true"
																 	customModelCode="${(column.customModelCode)!}"
																 	propertyLayRec="${(column.propertyLayRec)!}"
																 	modelNameInternational="${(column.modelNameInternational)!}"
																 	<#assign propDisplayName = getPropDisplayName("${(column.propertyLayRec)!}", "${(column.customModelCode)!}")>
																 	</#if>
																 	code="${(column.code)!}"
																 	proWidth=${(column.width)!100}
																 	style="width: ${(column.width)!100}px"
																 ><dd><#if (column.customSection)!false>自定义字段区域&nbsp;[${propDisplayName!}]<#else>${(column.label)!}</#if></dd></td>
																</#if>
															</#if>
														</#list>
													</tr>
												</#if>
												</#if>
												</#list>
												</#if>
												</#if>
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
	<div id="design-button" style="height:30px;width:100%;float:right;text-align:right;margin-top:20px">
		<#if !isReadOnlyMode>
		<button class="Dialog_button btn_pointer" onclick="listec.property_save()" type="button" style="margin-right:20px">${getHtmlText('common.button.save')}</button>
		</#if>
		<button class="Dialog_button btn_pointer" type="button" onclick="window.close()" style="margin-right:20px">${getHtmlText('common.button.cancel')}</button>
	</div>

 	<div id="listFieldDlg" style="display:none;">
 		<@errorbar id="ListFieldDlgErrorBar"></@errorbar>
 		<input type="hidden" id="form_grid_default_val" />
 		<input type="hidden" id="form_grid_prop_default_val" />
 		<input type="hidden" id="form_grid_value_changed_val" />
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
		<table id="tab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr class="config_custom_props">
				<td class="la config_custom_props" width="20%">列类型</td>
				<td class="co config_custom_props" width="30%">
					<select id="select_change_prop_type" onchange="changePropType(this);">
						<option value="0">普通字段</option>
						<option value="1">自定义字段</option>
					</select>
				</td>
				<td class="la config_custom_props" width="20%"></td>
				<td class="co config_custom_props" width="30%"></td>
			</tr>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;align:right;color:red">${getHtmlText('ec.view.code')}</td>
				<td class="co" width="30%"><input id="property_key" type="text" class="cui-edit-field" /></td>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.property.displayName')}</td>
				<td class="co" width="30%">
				<#-- <input id="property_show_name" type="text" class="cui-edit-field" /> -->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international moduleCode="${dataGrid.targetModel.entity.module.artifact}" name="property.showName" isNew=true isOldEdit=true key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr id="selectDisplayTypeTR">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.cell.fieldType')}</td>
				<td class="co" width="30%">
					<select id="form_property_column_type" class="cui-edit-field" style="width:100%;display:none"></select>
					<input type="text" id="form_property_column_type_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%"/>
				</td>
				<td class="la" width="20%" name="td_showType" align="right"  padding-right="10px">${getHtmlText('ec.view.cell.showType')}</td>
				<td class="co" width="30%" name="td_showType">
					<select id="property_field_type" class="cui-edit-field" style="width:100%"></select>
					<input type="text" id="form_property_field_type_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
				</td>
			</tr>
			<tr id="tr_showFormat">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.cell.showFormat')}</td>
				<td class="co" width="30%">
					<select id="form_property_show_format" class="cui-edit-field" style="width:100%"></select>
					<input type="text" id="form_property_show_format_input" class="cui-edit-field cui-readonly-field" readonly="readonly" style="width:100%;display:none" />
				</td>
				<td class="la" width="20%" align="right"  padding-right="10px"><label id="property_iscomplexLable"></label></td>
				<td class="co" width="30%">					
					<select id="property_selectcomp_type"  class="cui-edit-field" style="display:none;width:100%">
					</select>
				</td>
			</tr>
			<tr id="gridDefaultTR" style="display:none">
					<td class="la" width="20%" align="right" padding-right="10px">
						<div id="form_grid_defaultValue_div" style="width:100%">
							${getHtmlText('ec.property.default.td')}
						</div>
					</td>
					<td class="co" width="80%" colspan="3">
						<div id="form_text_grid_defaultValue_div" style="display:none;width:100%">
							<input type="text" id="form_text_defaultValue" class="cui-edit-field" style="width:100%" onChange="_changeDefaultValue(this)" />
						</div>
						<div id="grid_dateRadio_div" style="display:none;width:100%">
							<input type="radio" name="dateRadio" value="today" onClick="_changeDefaultValue(this)">${getHtmlText('ec.property.default.today')}&nbsp;</input>
							<input type="radio" name="dateRadio" value="firstday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.firstdayOfMonth')}&nbsp;
							<input type="radio" name="dateRadio" value="lastday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.lastdayOfMonth')}&nbsp;
							<input type="radio" name="dateRadio" value="nextsevenday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.later')}&nbsp;
						</div>
						<div id="grid_dateTimeRadio_div" style="display:none;width:100%">
							<input type="radio" name="dateTimeRadio" value="currentTime" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currenttime')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="firstday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.firstdayOfMonth')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="lastday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.lastdayOfMonth')}&nbsp;
							<input type="radio" name="dateTimeRadio" value="nextsevenday" onClick="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.later')}&nbsp;
						</div>
						<div id="grid_ck_boolean_div" style="display:none;width:100%">
                            <select id="grid_ck_boolean" class="cui-edit-field" onChange="_changeDefaultValue(this)"  style="width:41%" >
                                <option value=""></option>
                                <option value="true" >${getText("common.radio.true")}</option>
                                <option value="false" >${getText("common.radio.false")}</option>
                            </select>
                        </div>
						<div id="grid_ck_currentUser_div" style="display:none;width:100%">
							<input type="checkbox" id="grid_ck_currentUser" value="currentUser" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentStaff')}
						</div>
						<div id="grid_ck_currentDepart_div" style="display:none;width:100%">
							<input type="checkbox" id="grid_ck_currentDepart" value="currentDepart" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentDepartment')}
						</div>
						<div id="grid_ck_currentPost_div" style="display:none;width:100%">
							<input type="checkbox" id="grid_ck_currentPost" value="currentPost" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentPosition')}
						</div>
						<div id="grid_ck_currentComp_div" style="display:none;width:100%">
							<input type="checkbox" id="grid_ck_currentComp" value="currentComp" onChange="_changeDefaultValue(this)"/>${getHtmlText('ec.property.default.currentCompany')}
						</div>
						<div id="grid_ck_syscode_div" style="display:none;width:100%">
							<select id="ck_syscode" name="grid_ck_syscode" class="cui-edit-field" style="width:100%" onChange="_changeDefaultValue(this)"></select>
						</div>
						<script type="text/javascript">
							var ckradio = null;
							function _changeDefaultValue(obj){
								if(obj.type=="text"){
									CUI('#form_grid_default_val').val(CUI(obj).val());
								}else if(obj.type=="radio"){
									if(obj==ckradio){
										obj.checked=false;
										ckradio = null;
										CUI('#form_grid_default_val').val('');
									}else{
										ckradio = obj;
										CUI('#form_grid_default_val').val(CUI(obj).val());
									}
								}else if(obj.type=="checkbox"){
									if(obj.id=="grid_ck_boolean"){
										if(obj.checked){
											CUI('#form_grid_default_val').val(CUI(obj).val());
										}else{
											CUI('#form_grid_default_val').val("false");
										}
									}else if(obj.id=="grid_ck_currentUser" || obj.id=="grid_ck_currentDepart" || obj.id=="grid_ck_currentPost" || obj.id=="grid_ck_currentComp"){
										if(obj.checked){
											CUI('#form_grid_default_val').val(CUI(obj).val());
										}else{
											CUI('#form_grid_default_val').val("");
										}
									}
								}else{
									CUI('#form_grid_default_val').val(CUI(obj).val());
								}
								if(CUI('#form_grid_default_val').val()!=CUI('#form_grid_prop_default_val').val()){
									CUI('#form_grid_value_changed_val').val(true);
								}
							}
						</script>
					</td>
				</tr>
			<#-- <tr>
				<td class="la" width="20%" style="padding-right:10px;align:right" id="decimalTd">${getText('ec.view.decimal')}</td>
				<td class="co" width="30%" id="decimalTextTd"><input id="property_decimal" type="text" class="cui-edit-field" /></td>
				<td class="la" width="20%" align="right"  padding-right="10px" style="visibility: hidden;">${getHtmlText('ec.view.width')}</td>
				<td class="co" width="30%" style="visibility: hidden;"><input id="property_width" type="text" class="cui-edit-field" /></td>
				<td id="selectcomp_checkbox_mneenable_label" class="la" width="30%" align="right">${getHtmlText('ec.view.mneenable')}</td>
				<td id="selectcomp_checkbox_mneenable_checkbox" class="co" width="60%">
					<input type="checkbox" id="selectcomp_checkbox_mneenable" value="true">
				</td>
			</tr>-->
			<tr id="form_tr_isgroup">
				<td class="la" width="20%" align="right" padding-right="7px"><label id="isGroupLable">${getHtmlText('ec.view.isgroup')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="form_property_is_group" /></td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right"  padding-right="10px"><label id="isreadOnlyLable">${getHtmlText('ec.view.isreadonly')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="property_is_readonly" /></td>
				<td class="la" width="20%" align="right" padding-right="10px"><label id="isnullableLable">${getHtmlText('ec.view.isnullable')}</label></td>
				<td class="co" width="30%"><input type="checkbox" id="property_is_nullable" /></td>
			</tr>
			<tr  id="textalignTr">
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.showvalue')}</td>
				<td class="co" width="30%">
					<select id="textalign"  class="cui-edit-field" style="width:100%">
						<option value="center">${getText('ec.view.center')}</option>
						<option value="left">${getText('ec.view.left')}</option>
						<option value="right">${getText('ec.view.right')}</option>
					</select>
				</td>
				<td class="la treeNode" width="20%" align="right" padding-right="10px"><label id="isTreeNode">${getHtmlText('ec.view.isTreeNode')}</label></td>
				<td class="co treeNode" width="30%"><input type="checkbox" id="property_is_treeNode" /></td>
			</tr>
			<tr>
				<td class="la" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.ishidden')}</td>
				<td class="co" width="30%"><input type="checkbox" id="property_is_hide" /></td>
				<#--<td class="la popView" width="20%" style="padding-right:10px;align:right">${getHtmlText('ec.view.popView')}</td>
				<td class="co popView" width="30%"><input type="checkbox" id="property_is_popView" /></td>
				-->
			</tr>
			<tr id="formulasTr" style="display:none;">
				<td class="la" width="30%" style="padding-right:10px;">${getHtmlText('ec.view.expression')}</td>
				<td class="co" width="70%" colspan="3"><input id="formulasText" type="text" class="cui-edit-field" /></td>
			</tr>
			<tr id="form_tr_precision">
				<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.property.decimalNum')}</label></td>
				<td class="co" width="30%"><input type="text" id="precision" class="cui-edit-field" /></td>
			</tr>
			<#--<tr class="tr count-total">
			<td class="la count-total" width="20%" style="padding-right:10px;align:right"></td>
			<td class="co count-total" width="30%">
			<div id="listCount_div" style="width:100%">
				<input type="checkbox" name="isCount" id="isCount">${getHtmlText('ec.property.isCount')}&nbsp;</input>
				<#if dataGrid.view.type?? && dataGrid.view.type == 'VIEW'>
				<input type="checkbox" name="isTotal" id="isTotal"/>${getText('ec.property.isTotal')}&nbsp;</input>
				</#if>
			</div>
			</td>
			</tr>-->
			<tr id="ref_condition" style="height:30px">
				<td class="la" width="20%" align="right" padding-right="7px"><label id="refConditionLabel">${getHtmlText('ec.view.refCondition')}</label>
				<s<span id="refConditionHelpinfo" class="baphelp-icon"></span>
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
					<textarea id="showFormatFunc" class="cui-edit-textarea" style="height:80px;margin-top: 10px;"></textarea>
				</td>
			</tr>
			<tr id="tr_config_custom_props_select_model" class="config_custom_props">
				<td class="la config_custom_props" width="20%">所属模型</td>
				<td class="co config_custom_props" width="30%">
					<div name="property" class="fix-search-click" style="width:95%;">
						<input id="config_custom_props_select_model" size="15" class="cui-edit-field" readonly="readonly" />
						<input type="button" onclick="ec.datagrid.customProp.selectModel(event, this);" class="cui-search-click" />
					</div>
				</td>
				<td class="la config_custom_props" width="20%"></td>
				<td class="co config_custom_props" width="30%"></td>
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
		<table id="tab-validate" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;display:none">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.validatorType')}</label></td>
					<td class="co" width="30%">
						<select id="validate-select" class="cui-edit-field" style="width:100%">
						</select>
					</td>
					<td class="la" width="20%" style="text-align:left;padding-left:10px">
						<img onclick="listec.addValidate($('#validate-select'),$('#tab-validate1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</td>
					<td class="la" width="30%"></td>
				</tr>
				<tr>
					<td colspan="4">
						<div id="div-validate" style="width:570px;align:left;height:390px;overflow-y:auto">
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
		<table id="tab-style" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px;display:none">
			<tr style="height:0px">
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
				<td style="width:20%;height:0px;text-align:right" class="la"></td>
				<td class="co" style="width:30%;height:0px"></td>
			</tr>
			<tr>
				<td class="la" style="width:20%" align="right" padding-right="10px">${getHtmlText('ec.view.customstyle')}
				<span id="customStyleHelpinfo" class="baphelp-icon"></span>
<div id="customStyleHelpinforef" style="display:none">
	<p class="baphelp-info">自定义样式</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>background-color:red;</span>
</code></pre>
</div>
<p class="baphelp-hint">注意：直接写css样式，不能设置宽度高度</p>
</div>
					<script type="text/javascript">
							$('#customStyleHelpinfo').helptip({refElm: "#customStyleHelpinforef", html: true , isCustom :false, width: 260 , title :"说明"});
						</script>
				</td>
				<td class="co" style="width:80%"><textarea id="property_field_style" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea></td>
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
				<td class="la" width="10%" style="padding-right:10px;">${getHtmlText('ec.action.datagrid.properties.title')}</td>
				<td class="co" width="40%">
					<@international name="dataGrid.dataGridName" moduleCode="${dataGrid.targetModel.entity.module.artifact}" isNew=true isOldEdit=true key="${dataGrid.dataGridName!''}" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
				<td class="la" width="10%" style="padding-right:10px;display:none;><input type="checkbox" id="exportExcel"/></td>
				<td class="co" width="40%" style="display:none;">${getHtmlText('ec.action.datagrid.properties.excel.export')}</td>
			</tr>
			<tr style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;display:none;;">${getHtmlText('ec.action.datagrid.properties.height')}</td>
				<td class="co" width="40%" style="display:none;">
					<input type="text" id="displayRowsCount" class="cui-edit-field" style="width:50px">${getHtmlText('ec.action.datagrid.properties.height.unit')}
				</td>
				<td class="la" width="10%" style="padding-right:10px;display:none;"><input type="checkbox" id="cannotAddNewRow" /></td>
				<td class="co" width="40%" style="display:none;">${getHtmlText('ec.view.noaddRow')}</td>
			</tr>
			
			<#if model.dataType == 2>
			<tr class="tree-choise" style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isTreeView" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isTreeView')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="nodeExpanded" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.nodeExpanded')}</td>
			</tr>
			<tr class="tree-pro" style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isAddChild" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isAddChild')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isDelete" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isDelete')}</td>
			</tr>
			<tr class="tree-pro" style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isAddPrev" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isAddPrev')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isAddNext" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isAddNext')}</td>
			</tr>
			<tr class="tree-pro" style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isMoveUp" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isMoveUp')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isMoveDown" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isMoveDown')}</td>
				
			</tr>
			<tr class="tree-pro" style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isLevelUp" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isLevelUp')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isLevelDown" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isLevelDown')}</td>
			</tr>
			</#if>
			<tr id="tree-none" style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isEx"<#if dataGrid.ex?? && dataGrid.ex> checked="checked"</#if> /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.issupport')}</td>
			</tr>
			<tr>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="isEditable" onchange="editableChange()"/></td>
				<td class="co" width="40%">${getHtmlText('ec.view.isedit')}</td>
				<td class="la" width="10%" style="padding-right:10px;display: none;"><input type="checkbox" id="isTransverseShow"/></td>
                <td class="co" width="40%" style="display: none;">${getHtmlText('ec.view.isTransverseShow')}</td>
				<td class="la" width="10%" style="padding-right:10px;display:none;"><input type="checkbox" id="isCheckBox" /></td>
				<td class="co" width="40%" style="display:none;">${getHtmlText('ec.view.isCheckbox')}</td>
			</tr>
			<tr style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="importExcel" /></td>
				<td class="co" width="40%">${getHtmlText('ec.view.importExcel')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="autoAddRow" /></td>
				<td class="co" width="40%">${getText('ec.view.autoAddRow')}</td>
			</tr>
			<#if model.dataType == 1>
			<tr style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="copyRow"></td>
				<td class="co" width="40%">${getText('ec.view.copyRow')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="copyColumn"></td>
				<td class="co" width="40%">${getText('ec.view.copyColumn')}</span></td>
			</tr>
			<#else>
			<tr style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="copyColumn"></td>
				<td class="co" width="40%">${getText('ec.view.copyColumn')}</span></td>
			</tr>
			</#if>
			<tr id="deleteAndInsertRow" style="display:none;">
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="canNotDeleteRow" /></td>
				<td class="co" width="40%">${getHtmlText('ec.action.datagrid.properties.delete.forbbiden')}</td>
				<td class="la" width="10%" style="padding-right:10px;"><input type="checkbox" id="canInsertRow" /></td>
				<td class="co" width="40%">${getHtmlText('ec.datagrid.properties.insert.permit')}</td>
			</tr>
			<tr>
				<td class="la" width="11%" style="padding-right:10px;">
					<input type="checkbox" id="isTransCondition" />
				</td>
				<td class="co" width="39%">
					${getHtmlText('ec.view.handwritingCondition')}
					<span id="handwritingHelpinfo" class="baphelp-icon"></span>	
<div id="handwritingHelpinforef" style="display:none">
	<p class="baphelp-info">可动态传递参数拼接到条件中，并根据变量进行逻辑判断</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>if(test==1){</span>
	return '(P.ID =\$\{{deptId,Long} OR P.LAY_REC LIKE \\'\$\{layrec,String}\\')';
}else{
	return '(P.ID = \$\{deptId,String} OR P.LAY_REC LIKE \\'\$\{layrec,String}\\')';
}</code></pre>
</div>
<p class="baphelp-hint">注意：test 为前台传的参数，post 或get 即可</p>
</div>

					<script type="text/javascript">
							$('#handwritingHelpinfo').helptip({refElm: "#handwritingHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
				</script>
				</td>
				<td class="co" colspan="2">
					<div id="66387035_button_2" canClick="true" class="edit-btn btn-act" onclick="showDialog()"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c">${getHtmlText('ec.view.customerCondition')}</a><a class="cui-btn-r">&nbsp;</a></div>
				</td>
			</tr>
			<!-- <tr>
		    <td class="la" width="10%" style="padding-right:10px;" >
		     <input type="checkbox" id="isEasytable" />
		    </td>
		    <td class="co" width="40%" >
		     ${getHtmlText('ec.view.isEasytable')}
		    </td>
		    </tr> -->
			<tr>
				
				<td class="co" colspan="4" style="padding-left:30px">
					<textarea readonly="true" id="conditionArea" class="cui-edit-textarea" style="width:90%;height:50px;"></textarea>
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
				<td class="la" style="width:20%" align="right" padding-right="10px">renderOver:
								<span id="renderOverHelpinfo" class="baphelp-icon"></span>
<div id="renderOverHelpinforef" style="display:none">
	<p class="baphelp-info">DATAGRID数据请求执行接口</p>
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
				<td class="co" style="width:80%"><textarea id="dataGrid_public_event" class="cui-edit-textarea" style="width:100%;height:80px;margin-top:5px"></textarea></td>
			</tr>
			<tr>
				<td class="la" style="width:20%" align="right" padding-right="10px">ptInit（DataGrid${getText('ec.view.init')}）:
				<span id="ptInitHelpinfo" class="baphelp-icon"></span>
<div id="ptInitHelpinforef" style="display:none">
	<p class="baphelp-info">DATAGRID的初始化函数</p>
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
		</table>
	</div>
	<!--多选控件 -->
	<div id="multselect_setting_dlg" style="display:none;">
		<@errorbar id="ListFieldDlgMulErrorBar"></@errorbar>
		<div id="muldlg-div" class="dlg-etv-navset">
	 		<div class="etv-scrollbar" style="margin:0 5px;">
				<ul class="etv-nav" style="display: block;">
					<li class="selected" name="proertyTabs" id="mulbase_attr">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.property')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
					<li name="proertyTabs" id="mulevent_defined">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getText('ec.view.cell.event')}</em>
						</span>
						<span class="etv-nav-span-r"></span>
					</li>
				</ul>
			</div>
		</div>
 		<table id="multab-common" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
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
			<tr id="ec_tr_nullable">
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.isnullable')}</td>
				<td class="co" width="60%">
				<input type="checkbox" id="ec_checkbox_nullable" value="true">
				</td>
			</tr>
			<tr>
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.isgroup')}</td>
				<td class="co" width="60%">
					<input type="checkbox" id="form_property_is_group_multi" />
				</td>
			</tr>
			<tr>
				<td class="la" width="30%" align="right">${getHtmlText('ec.view.isreadonly')}</td>
				<td class="co" width="60%">
					<input type="checkbox" id="ec_is_readonly" />
				</td>
			</tr>
			<tr id="ref_condition" style="height: 30px; display: table-row;">
				<td class="la" width="20%" align="right" padding-right="7px"><label id="refConditionLabel"><span i18n="ec.view.refCondition">参照参数</span></label>
				<span id="refConditionHelpinfo1" class="baphelp-icon"></span>
<div id="refConditionHelpinforef1" style="display:none">
<p class="baphelp-info">当单元属性为对象类型，可传自定义条件给参照页面。 </p>
<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code><span>return "year=2008 &amp;entity=crossCom " ;</span></code></pre>
</div>
<p class="baphelp-hint">注意：该功能需要参照列表的表格属性配置自定义条件，具体见其帮助说明。</p>
</div>
					<script type="text/javascript">
							$('#refConditionHelpinfo1').helptip({refElm: "#refConditionHelpinforef1", html: true , isCustom :false, width: 460 , title :"说明"});
						</script>
				</td>
				<td class="co" colspan="3">
					<textarea id="ref_condition_content" class="cui-edit-textarea" style="width:100%;height:50px;margin-top:5px"></textarea>
				</td>
			</tr>
		</table>
		<div id="muldiv-event" style="width:100%;align:left;height:300px;overflow-y:auto;display:none">
			<table id="multab-event" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
				<tr>
					<td class="la" width="20%" align="right" padding-right="10px"><label>${getHtmlText('ec.view.cell.eventType')}</label></td>
					<td class="co" width="30%">
						<select id="mulevents-select" class="cui-edit-field" style="width:100%">
							<option value="onBeforeClear">onBeforeClear</option>
							<option value="onAfterClear">onAfterClear</option>
							<option value="onBeforeSet">onBeforeSet</option>
							<option value="onAfterSet">onAfterSet</option>
						</select>
					</td>
					<td class="la" width="40%" style="text-align:left;padding-left:10px">
						<img onclick="listec.addnewevent($('#mulevents-select'),$('#multab-event1'))" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</td>
					<td class="la" width="10%"><span id="mulHelpinfo"></td>
				</tr>
				<tr>
					<td colspan="4">
						<table id="multab-event1" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="100%" align="left" style="margin-top: 10px;">
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
	 </div>
	<div id="button_property_dlg" style="display:none">
		<@errorbar id="ButtonDialogErrorBar"></@errorbar>
		<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.identiy')}</td>
				<td class="co" width="30%"><input type="text" id="button_property_id" class="cui-edit-field" style="width:100%" /></td>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.showname')}</td>
				<td class="co" width="30%">
				<#--<input type="text" id="button_property_show_name" class="cui-edit-field" style="width:100%" />-->
				<#--international_propertyshowName    international_propertyshowName_showName -->
				<@international name="buttonProperty.showName" isNew=true isOldEdit=true moduleCode="${model.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
			</tr>
			<tr style="">
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.operatetype')}</td>
				<td class="co" width="30%">
					<select id="operatetype"  class="cui-edit-field" style="width:100%">
						<#--<option value="CUSTOM">${getText('ec.view.customoperate')}</option>-->
						<option value="ADDROW">${getText('ec.view.isaddrow')}</option>
						<option value="DELETEROW">${getText('ec.view.isdeleterow')}</option>
						<#--<option value="REF">${getText('ec.view.isrefrow')}</option>               `
						<option value="INSERTROW">${getText('ec.view.isinsertrow')}</option>
						<option value="ADDCHILD">${getText('ec.view.isAddChild')}</option>
						<option value="DELETENODE">${getText('ec.view.isDelete')}</option>
						<option value="ADDPREV">${getText('ec.view.isAddPrev')}</option>
						<option value="ADDNEXT">${getText('ec.view.isAddNext')}</option>
						<option value="LEVELUP">${getText('ec.view.isLevelUp')}</option>
						<option value="LEVELDOWN">${getText('ec.view.isLevelDown')}</option>-->
					</select>
				</td>
					<td class="la" width="20%" align="right" padding-right="10px" id="releaseFelidLabel">${getText('ec.view.releaseFelid')}</td>
				<td class="co" width="30%"  id="">
					<select id="releaseFelid"  class="cui-edit-field" style="width:100%" onchange="getRefView();">
						<option value="" modelcode=''></option>
						<#if (properties)?? && (properties)?size &gt; 0>
						<#list properties as p>
						<#if p.type=='OBJECT'&& !(p.isMainAssociated) && !(p.isCustom)>
						<option value="${(p.code)!}" modelcode='${(p.associatedProperty.model.code)!}'>${getText((p.displayName)!)}</option>
						</#if>
						</#list>
						</#if>
						
					</select>
				</td>
			</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">confirm${getHtmlText('ec.view.config')}</td>
				<td class="co" width="30%"><input type="checkbox" id="isconfirm" /></td>
				<td class="la" width="20%" align="right" padding-right="10px" id="confirmname">${getHtmlText('ec.view.hint')}</td>
				<td class="co confirmcontent" width="30%">
					<@international name="confirmcontent" isNew=true isOldEdit=true moduleCode="${dataGrid.view.assModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-edit-field" />
				</td>
				</tr>
			<tr>
				<td class="la" width="20%" align="right" padding-right="10px">${getHtmlText('ec.view.styletype')}</td>
				<td class="co" width="30%">
					<select id="buttonstyle"  class="cui-edit-field" style="width:100%">
						<option value="add">${getText('ec.view.addstyle')}</option>
						<option value="edit">${getText('ec.view.editstyle')}</option>
						<option value="del">${getText('ec.view.delstyle')}</option>
						<option value="insert">${getText('ec.view.insertstyle')}</option>
						<option value="eighteen-dt-op-reference">${getText('ec.view.isrefrowstyle')}</option>
					</select>
				
				</td>
				<td class="la" width="20%" align="right" padding-right="10px" id="viewrefselectLab">${getHtmlText('ec.view.selectrefview')}</td>
				<td class="co" width="30%" "><select id="viewrefselect"  class="cui-edit-field" style="width:100%"></select></td>
			</tr>
			<tr id="iscustomfuncTr">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.iscallback')}</td>
				<td class="co" width="30%">
					<input type="checkbox" id="iscustomfunc" />	
					<span id="callBackHelpinfo" class="baphelp-icon"></span>
					<script type="text/javascript">
					$(function(){
							var sel = $('#operatetype');
								var cbElm = $('#callBackHelpinfo');
								var cbIns;
								var cbM = {
									ADDROW: '<p class="baphelp-info">增行的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function addrowCustomBack(event,obj){<br/>&#160;&#160;&#160;console.info(obj);<br/>&#160;&#160;&#160;console.info(event);<br/>}</code></pre></div><p class="baphelp-hint">注意：obj为行对象，event为事件对象</p>',
									DELETEROW: '<p class="baphelp-info">删行的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function deleteCustomBack(event,obj){<br/>&#160;&#160;&#160;console.info(obj);<br/>&#160;&#160;&#160;console.info(event);<br/>}</code></pre></div><p class="baphelp-hint">注意：obj为行对象，event为事件对象</p>',
									REF: '<p class="baphelp-info">参照时回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function refback(arr){<br/>&#160;&#160;&#160;console.info(arr);<br/>}</code></pre></div><p class="baphelp-hint">注意：arr为本次参照选择的对象数组</p>',
									INSERTROW: '<p class="baphelp-info">插行的回调函数</p><p class="baphelp-example">范例：</p><div class="baphelp-code"><pre><code>function insertCustomBack(event,obj){<br/>&#160;&#160;&#160;console.info(obj);<br/>&#160;&#160;&#160;console.info(event);<br/>}</code></pre></div><p class="baphelp-hint">注意：obj为行对象，event为事件对象</p>'
								};		
								cbElm.on('click', function(){
									cbIns.setContent( cbM[sel.val()] );
								});				
								cbIns = cbElm.helptip({
									content: cbM[sel.val()], 
									html: true , 
									isCustom :false, 
									width: 240 , 
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
			<tr id="eventTr">
				<td class="la" width="30%" align="center" padding-right="10px">${getHtmlText('ec.view.customevent')}
				
				</td>
				<td class="co" width="30%">
					<div style="float:left;width:100%;height:20px;margin-bottom:5px;border:1px solid #0059B3;overflow:auto;">
						<ul id="button_event_ul">
							<li class="editeventli" style="padding-top:2px">
							<a href="#" title="${getText('ec.view.editevent')}" style="text-decoration:underline;padding-left:20px;" onclick="listec.editButtonEvent('onclick')">onclick</a>：<input type="checkbox" id="button_property_event_onclick" value="" style="padding-right:10px"/>
							<span id="functionHelpinfo" class="baphelp-icon"></span>
<div id="functionHelpinforef" style="display:none">
	<p class="baphelp-info">自定义按钮的click函数</p>
	<p class="baphelp-example">范例</p>
<div class="baphelp-code">
<pre><code>function customClick(event){
	console.info(event);
}</code></pre>
</div>
<p class="baphelp-hint">注意：event为事件对象</p>
</div>

					<script type="text/javascript">
							$('#functionHelpinfo').helptip({refElm: "#functionHelpinforef", html: true , isCustom :false, width: 220 , title :"说明"});
				</script>
							</li>
						</ul>
					</div>
					<br/>					
				</td>
				<td class="la" width="20%" align="right" padding-right="10px" style="display: none;">${getHtmlText('ec.view.ispower')}</td>
				<td class="co" width="30%" style="display: none;"><input type="checkbox" id="ispermission" /></td>
			</tr>
			<!--
			<tr id="urlTr" style="display: none;">
			-->
			<tr style="display: none;">
				<td class="la" width="30%" align="right" padding-right="10px">${getHtmlText('ec.view.powerurl')}</td>
				<td class="co" colspan="3">
					<input type="text" id="operateurl" class="cui-edit-field" style="width:100%" />				
				</td>
			</tr>
			<tr id="textareaTr2" style="display: none;">
				<td class="co" colspan="4">
					<textarea  id="buttonEventarea" class="cui-edit-textarea" style="width:100%;height:55px;margin-left:10px;"></textarea>
				</td>	
			</tr>
		</table>
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
									<@international name="errorMsgRegex" isNew=true isOldEdit=true moduleCode="${dataGrid.targetModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-noborder-input" />
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
									<@international name="errorMsg" isNew=true isOldEdit=true moduleCode="${dataGrid.targetModel.entity.module.artifact}" key="" style="width:100%;margin-right: -5px;" cssClass="cui-noborder-input" />
								</div>
							</td>
						</tr>
					</table>
				</td>
			</tr>
			</table>
	</div>
	<script type="text/javascript">
		var main_tab,listec;//全局变量
		var _proj_config_flag = ${isProj?string};
		var _datagridTargetModelCode = "${dataGrid.targetModel.code}";
		$(function(){
			$('#form_select_elements').tabs("#form_select_elements div.accordion_pane", {tabs:'h2',effect:'slide',initialIndex:null});//初始化手风琴下拉
			main_tab = new CUI.TabView("main_form_tab",{tabposition:'top'});//初始化主设计区域页签
			var _directAssosJson = ${(multSelectInfo.directAssosJson)!'{}'};
			var _indirectAssosJson = ${(multSelectInfo.indirectAssosJson)!'{}'};
			listec = new CUI.EntityConfDataGrid(
					'${(view.code)!}',
					<#if ev?? && (ev.config)?? && (ev.config) != '' && (ev.config.layout.PageConfig)??>
						<@s.property value="(ev.config)" escape=false/>
					<#else>
						<#noparse>{pageConfig:{colNum:8}}</#noparse>
					</#if>,
					main_tab,
					'${(ev.code)!}',
					'${(ev.version)!0}',
					'${(ev.view.fastQueryJson.code)!}',
					'${(ev.view.fastQueryJson.version)!0}',
					{'singlSelect' : {'SELECT':'${getText('ec.view.select')}', 'RADIO':'${getText('ec.view.radio')}'},
					 'multiSelect' : {'CHECKBOX':'${getText('ec.view.checkbox')}', 'MULTSELECT':'${getText('ec.view.mulselect')}'},
					 'objectSelect' : {'TEXTFIELD':'${getText('ec.view.textfield')}','TEXTAREA':'${getText('ec.view.textarea')}','SELECT':'${getText('ec.view.select')}', 'DATE':'${getText('ec.view.date')}', 'DATETIME':'${getText('ec.view.datetime')}', 'SELECTCOMP':'${getText('ec.view.selectcomp')}'},
					 'normalSelect' : {<#if fieldTypes??><#list fieldTypes?keys as key>'${key}':'${getText('${fieldTypes[key]!}')}',</#list></#if>},
					'viewColumnTypes' : {<#if viewColumnTypes??><#list viewColumnTypes?keys as key>'${key}':'${getText('${viewColumnTypes[key]!}')}'<#if key_has_next>,</#if></#list></#if>},
					'viewFormats' :  {<#if viewFormats??><#list viewFormats?keys as key>'${key}':'${getText('${viewFormats[key]!}')}'<#if key_has_next>,</#if></#list></#if>}
					},
					{
					'equalExp' : {'equal':'${getText('ec.view.equal')}', 'unequal':'${getText('ec.view.unequal')}'},
					'textExp' : {'equal':'${getText('ec.view.equal')}', 'like':'${getText('ec.view.like')}', 'llike':'${getText('ec.view.llike')}','rlike':'${getText('ec.view.rlike')}'}
					},
					'${dataGrid.code}',
					_directAssosJson,
					_indirectAssosJson
					);
		});
		function getRefView(){
			var modelCode=CUI("#releaseFelid").find("option:selected").attr("modelCode");
			if(modelCode!=""){
				var refUrl="/msService/ec/view/referenceViews"+(window._proj_config_flag?'?isProj=true':'');
				if(this.dataGridCode.includes('__mobile__')){
					refUrl="/msService/ec/view/referenceMobileViews"+(window._proj_config_flag?'?isProj=true':'');
				}
				CUI.ajax({
					url: refUrl,
					type: 'post',
					async: false,
					data: {"modelCode" : modelCode},
					success: function(msg) {
						CUI('body').data(modelCode, msg);
					}
				});
				var oResponse = CUI('body').data(modelCode);
				var objSelect=CUI("#viewrefselect")[0];
				objSelect.options.length = 0;
				objSelect.options[objSelect.options.length] = new Option('${getText("ec.view.reference.null")}', '');
				CUI.each(oResponse, function(i, item){
					objSelect.options.add(new Option(item.displayNameInternational, item.code));
				});
			}
		}
		
		function editableChange(){
			if(!$("#isEditable").attr('checked')){
				$("#importExcel").attr('checked',false);
			}
		}
	</script>	
</body>
<script type="text/javascript">	
	//自动设置中间编辑区域的宽度
	function _init_size(){
		$('.col-main').width($(window).width()-230);
		$('#form_area').width($(window).width()-230);
		$('#listSection').width($(window).width()-277);
		listec.resizeListTable();
		$('#main_form_tab').width($(window).width());		
	}
	
	function showDialog(){
		if($('#66387035_button_2').attr('canClick')=="true"){
			dg.advQuery.showAdv();
		}
	}
	
	$(function(){
		// 设置自定义字段宽度
		$('#listTable td[customSection="true"]').each(function(){
			var text = $(this).text();
			var textWidth = CUI.getTextWidth(text);
			if (textWidth > 0){
				textWidth = 20 + textWidth;
				$(this).width(textWidth).attr('prowidth', textWidth);
			}
		})
		_init_size();
		<#if !dataGrid.config??>
		var modulecode="${dataGrid.targetModel.entity.module.artifact}";
		var buttons="del,add";
		listec.autoAddOperateButton(modulecode,buttons);
		$('#list_design_section').attr("canNotDeleteRow","true");
		$('#list_design_section').attr("cannotAddNewRow","true");
		<#else>
		if($('#list_design_section').attr("canInsertRow")=="true"){
			var modulecode="${dataGrid.targetModel.entity.module.artifact}";
			var buttons="insert";
			listec.autoAddOperateButton(modulecode,buttons);
			$('#list_design_section').attr("canInsertRow","false");
		}
		if($('#list_design_section').attr("canNotDeleteRow")=="false"){
			var modulecode="${dataGrid.targetModel.entity.module.artifact}";
			var buttons="del";
			listec.autoAddOperateButton(modulecode,buttons);
			$('#list_design_section').attr("canNotDeleteRow","true");
		}
		if($('#list_design_section').attr("cannotAddNewRow")=="false"){
			var modulecode="${dataGrid.targetModel.entity.module.artifact}";
			var buttons="add";
		}
		</#if>
		CUI('#multselect_model_type').change(function(){
			listec.multselect_model_type_changed(this);
		});
		CUI('#ec_select_indirect_model').change(function(){
			listec.loadModelProperties(this);
		});
		listec.multselect_model_type_changed(CUI('#multselect_model_type')[0]);
		
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
	$(function(){
		_init_size();
		var liArr = $("#muldlg-div").find('li');
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
		        if(objId === 'mulbase_attr'){
		            $('#multab-common').css('display','block');
		            $('#muldiv-event').css('display','none');
		        }else if(objId === 'mulevent_defined'){
		            $('#multab-common').css('display','none');
		            $('#muldiv-event').css('display','block');
		        }
		    });
		});
	});
	function changePropType(obj) {
		var type = parseInt($(obj).val());
		$('#config_custom_props_select_model').val('');
		$('#config_custom_props_select_model').removeAttr('modelcode');
		$('#config_custom_props_select_model').removeAttr('layrec');
		if (type == 1) {
			$('tr:visible,td:visible', '#tab-common').removeClass('list_field_init').addClass('list_field_init');
			$('tr:visible,td:visible', '#tab-common').hide();
			$('tr.config_custom_props,td.config_custom_props', '#tab-common').show();
			$('tr,td', '#div-event').hide();
			$('tr,td', '#tab-validate').hide();
			$('tr,td', '#tab-style').hide();
		} else {
			$('tr.config_custom_props,td.config_custom_props', '#tab-common').hide();
			$('tr.list_field_init,td.list_field_init', '#tab-common').show();
			$('tr.list_field_init,td.list_field_init', '#tab-common').removeClass('list_field_init');
			$('tr,td', '#div-event').show();
			$('tr,td', '#tab-validate').show();
			$('tr,td', '#tab-style').show();
		}
	}
	
	CUI.ns("ec.datagrid.customProp");
	/**
	* 选择展示哪个模型的自定义字段
	*/
	ec.datagrid.customProp.selectModel = function(e, obj) {
		YUE.stopPropagation(e);
		ec.datagrid.customProp.showOverLayer(obj, "/msService/ec/view/config-select-model?model.code=${(model.code)!}");
	}
	
	ec.datagrid.customProp.showOverLayer = function(obj,url){
		CUI('#customContent').html("");
		ec.datagrid.customProp.showOverLayerDiv = new CUI.Overlay({
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
									$('body').trigger('click.overlayer3');
									CUI.Dialog.alert('${getText("请先选择模型！")}');
								} else {
									propertyDblClickFunc($('li[isselected="1"]')[0]);
									$('body').trigger('click.overlayer3');
								}
							}
						},
						{	name:"${getHtmlText('calendar.common.cancal')}",
							handler:function(){$('body').trigger('click.overlayer3');}
						}
					  ]
	     	
		});
		ec.datagrid.customProp.showOverLayerDiv.render();
		$("#overlay-idcustomContent").click(
			function(e){
				YUE.stopPropagation(e);
			}
		)
		url += "&time=" + new Date();
		ec.datagrid.customProp.showOverLayerDiv.show();
		$("#customContent").html('<table style="width:100%;height:100%;text-align:center;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>');
		CUI('#customContent').load(url);
	}
	//是否自动扩展单元格的change事件
    function isEasyTableChange(obj){
        if($(obj).attr("checked")){
            $("#isEasyTableTr").show();
            //$("#isEasyTableTr").next().show();
            $("#defaultRows").parent().show();
            $("#defaultRows").parent().prev().show();
            $("#displayRowsCount").parent().hide();
            $("#displayRowsCount").parent().prev().hide();
            $(".tree-choise").hide();
            $("#isTreeView").prop("checked", false);
            $("#nodeExpanded").prop("checked", false);
            defaultRowsChange($("#defaultRows"));
        }else{
            $("#isEasyTableTr").hide();
            //$("#isEasyTableTr").next().hide();
            $("#defaultRows").parent().hide();
            $("#defaultRows").parent().prev().hide();
            $("#displayRowsCount").parent().show();
            $("#displayRowsCount").parent().prev().show();
            $(".tree-choise").show();
            $("#displayRowsCount").val("");
        }
    }
	$(function(){
		$('body').unbind('click.overlayer3').bind('click.overlayer3', function(){
			if(ec.datagrid.customProp.showOverLayerDiv && ec.datagrid.customProp.showOverLayerDiv.isShow) {
				ec.datagrid.customProp.showOverLayerDiv.close();
				$('#overlay-shim-ie').remove();
			}
		});
	});
	
	$(window).resize(function(){
		_init_size();				
	});
	setTimeout(function(){
		$('.etv-content').width($(window).width()-191);
	},200);

	${(functions!'')?replace('@@double_quote@@', '"', 'r')?replace('@@single_quote@@', "'", 'r')}
</script>
<@customerCondition dataGridCode="${dataGrid.code}"/>
</html>