<style type="text/css">
.echarts_table {
	margin-top: 10px;
	width: 98%;
}
.event_table {
	margin-top: 10px;
	width: 90%;
}
.tabs li {
    display: inline-block;
    padding: 5px 0 0 5px;
    position: relative;
}
.tabs li a {
	display: block;
    padding: 10px 10px;
    background-color: #f5f5f5;
    color: #333;
    text-decoration: none;
}
.tabs li.active a {
    background-color: #1c84c6;
    border-color: #1c84c6;
    color: #fff;
}
.tab_content {
	display: none;
}
.tabs-close {
	width: 15px;
	height: 15px;
	position: absolute;
	z-index: 5;
	top: 5px;
	right: 1px;
	background: url("/bap/static/tabview/assets/list_all.gif") no-repeat 1px -1117px;
	cursor: pointer;
}
.display_none {
	display: none;
}
.event-textarea {
	height: 90px;
}
</style>
<@errorbar id="ec_echarts_edit_formDialogErrorBar" />
<form id="ec_echarts_edit_form" onSubmit="javascript:return ec.echarts.beforesubmit();" name="ec_echarts_edit_form"  method="post" action="/msService/ec/echarts/save"  callback="ec.echartsConfigCallback">
	<input type="hidden" name="isProj" id="ec_echarts_edit_form_isProj" value="${isProj?string('true','false')}"/>
	<input type="hidden" name="echarts.projFlag" value="${isProj?string('true','false')}"/>
	<input type="hidden" name="echarts.code" id="ec_echarts_edit_form_echarts_code" <#if echarts?? && echarts.code??>value="${echarts.code}"</#if>/>
	<input type="hidden" name="echarts.version" id="ec_echarts_edit_form_echarts_version"<#if echarts?? && echarts.version??>value="${echarts.version}" <#else>value="0"</#if> />
	<input type="hidden" name="emodelsJson" id="ec_echarts_edit_form_emodelsJson" <#if emodelsJson??>value="${emodelsJson}"</#if> />
	<input type="hidden" name="eventJson" id="ec_echarts_edit_form_eventJson" <#if eventJson?? >value="${eventJson}"</#if>/>
	<div id="ec_echarts_dialog" >
		<@errorbar id="ec_echarts_dialogDialogErrorBar" />
		<div id="ec_echarts_tab" class="dlg-etv-navset">
			<div class="etv-scrollbar" style="margin:5px 5px;">
				<ul class="etv-nav">
					<li class="selected" id="echarts_tab_attribute" onclick="ec.echarts.tab(0)">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getHtmlText('ec.view.echarts.attribute')}
								<@helpTip id="attributeTip" width=565 helpInfo="图表基本属性" helpExample="配置说明" helpCode="标题：图表标题，显示左上角<br/>加载数据：勾选后会请求配置数据源数据，如果自定义请求则建议不勾选<br/>图表切换：勾选后显示切换为折线图、切换为柱状图和还原三个按钮<br/>显示图例：勾选后显示图例；图例组件展现了不同系列的数据，可以通过点击图例控制哪些系列不显示<br/>图例位置：图例显示位置<br/>X轴单位：坐标轴名称，显示X轴右侧" />
							</em>
						</span>
					</li>
					<li id="echarts_tab_emodel" onclick="ec.echarts.tab(1)">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getHtmlText('ec.view.echarts.emodel')}
								<@helpTip id="emodelTip" width=450 helpInfo="数据模型：图表数据来源<br/>图表类型：可配置折线或柱状<br/>分类：选择模型字段，数据以分类进行分类，分类数据为X轴数据<br/>系列：选择模型字段，在分类的基础上再次筛选<br/>值字段：通过分类和系列进行筛选及计算该字段的值总和<br/>Y轴上限：坐标轴刻度最大值<br/>Y轴下限：坐标轴刻度最小值<br/>Y轴单位：坐标轴名称，显示Y轴最顶端<br/>Y轴位置：Y轴可选居左或居右<br/>自定义条件：可动态传递参数拼接到条件中，并根据变量进行逻辑判断<br/>首数据源Y轴：除首数据源外其他数据源显示，勾选表示使用第一个数据源的Y轴" helpExample="手写自定义条件范例" helpCode="if(test==1){<br/>&#160;&#160;&#160;return '(P.ID =&#92;&#36;{deptId,Long} OR P.LAY_REC LIKE &#92;'&#92;&#36;{layrec,String}&#92;')';<br/>}else{<br/>&#160;&#160;&#160;return '(P.ID = &#92;&#36;{deptId,String} OR P.LAY_REC LIKE &#92;'&#92;&#36;{layrec,String}&#92;')';<br/>}" helpHint="数据源配置注意：配置多数据源，系列名后跟数据源信息，如系列(1)，表示数据源1的系列<br/>手写自定义条件注意：test 为前台传的参数，post 或get 均可"/>
							</em>
						</span>
					</li>
					<li id="echarts_tab_event" onclick="ec.echarts.tab(2)">
						<span class="etv-nav-span">
							<em class="etv-nav-em" style="width: 90px;">${getHtmlText('ec.view.echarts.event')}
								<@helpTip id="eventTip" width=396 helpInfo="图表事件，请查看各个事件对应的帮助提示"helpCode="如需外部操作图表，请使用myCharts.${echarts.layoutName}对象，如：<br/>myCharts.${echarts.layoutName}.requestData();//请求图表数据<br/>myCharts.${echarts.layoutName}.requestData(url, data);//自定义url请求图表数据" helpHint="每个图表对象名称不同，根据布局命名"/>
							</em>
						</span>
					</li>
				</ul>
			</div>
		</div>
		<!-- 属性 -->
		<div id="echarts_attribute" style="display: block">
			<table class="infoTable echarts_table">
				<tr>
					<td class="la">${getHtmlText('ec.view.echarts.title')}</td>
					<td class="co">
                    <#if echarts?? && echarts.title??>
                        <@international name="echarts.title" key=(echarts.title)!'' moduleCode=view.entity.module.artifact isNew=true maxLength=80 ></@international>
                    <#else >
                        <@international name="echarts.title" key="" moduleCode=view.entity.module.artifact isNew=true maxLength=80></@international>
                    </#if>
					</td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.view.echarts.firstload')}</td>
					<td class="co">
						<input type="checkbox" name="echarts.isFirstLoad" <#if echarts?? && echarts.isFirstLoad??&& echarts.isFirstLoad>checked=true</#if>/>
					</td>
					<td class="la">${getHtmlText('ec.view.echarts.showmagictype')}</td>
					<td class="co">
						<input type="checkbox" name="echarts.isShowMagicType" <#if echarts?? && echarts.isShowMagicType??&& echarts.isShowMagicType>checked=true</#if>/>
					</td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.view.echarts.showlegend')}</td>
					<td class="co">
						<input type="checkbox" name="echarts.isShowLegend" <#if echarts?? && echarts.isShowLegend??&& echarts.isShowLegend>checked=true</#if> onclick="ec.echarts.isshowlegend()" />
					</td>
				</tr>
				<tr id="legendposition_tr" <#if (echarts.isShowLegend)?? && (echarts.isShowLegend)?string("true", "false")=="false">class="display_none"</#if>>
					<td class="la">${getHtmlText('ec.view.echarts.legendposition')}</td>
					<td class="co">
						<select id='legendPosition' name='echarts.legendPosition' class='edit-select'>
							<option value='top' <#if echarts?? && (echarts.legendPosition)?? && (echarts.legendPosition)=='top'>selected</#if>>${getHtmlText('ec.view.echarts.position.top')}</option>
							<option value='bottom' <#if echarts?? && (echarts.legendPosition)?? && (echarts.legendPosition)=='bottom'>selected</#if>>${getHtmlText('ec.view.echarts.position.middle')}</option>
							<option value='left' <#if echarts?? && (echarts.legendPosition)?? && (echarts.legendPosition)=='left'>selected</#if>>${getHtmlText('ec.view.echarts.position.left')}</option>
							<option value='right' <#if echarts?? && (echarts.legendPosition)?? && (echarts.legendPosition)=='right'>selected</#if>>${getHtmlText('ec.view.echarts.position.right')}</option>
							<option value='righttop' <#if echarts?? && (echarts.legendPosition)?? && (echarts.legendPosition)=='righttop'>selected</#if>>${getHtmlText('ec.view.echarts.position.righttop')}</option>
						</select>
					</td>
				</tr>
				<tr>
					<td class="la">${getHtmlText('ec.view.echarts.xAxisName')}</td>
					<td class="co">
						<input type="text" id="echarts_xAxisName" name="echarts.modelList[0].xaxis.name" maxLength="10" <#if echarts?? && echarts.modelList[0]?? && echarts.modelList[0].xaxis??>value ="${echarts.modelList[0].xaxis.name!}"</#if>class="cui-edit-field"/>
					</td>
				</tr>
			</table>
		</div>
		<!-- 数据源 -->
		<div id="echarts_emodel" style="overflow:scroll; height:369px; display: none">
			<div id="emodel_tab" class="tabs">
				<ul id="emodel_tab_ul">
					<#if echarts?? && echarts.modelList??>
					<#list echarts.modelList as emodel>
					<#if emodel??>
					<li id="emodel_tab${emodel_index}" <#if emodel_index==0>class="active"</#if>>
						<a href="javascript:;" onclick="ec.echarts.emodeltab(${emodel_index})">${getHtmlText('ec.view.echarts.emodel')}${emodel_index+1}</a>
						<span class="tabs-close" onclick="ec.echarts.delemodel(${emodel_index})" title="${getText('ec.view.close')}"></span>
					</li>
					</#if>
					</#list>
					</#if>
					<li id="add_emodel_li">
						<a href="javascript:;" onclick="ec.echarts.addemodel()" >
							<img src="/bap/static/ec/images/addEcharts.gif"/>
						</a>
					</li>
				</ul>
			</div>
			<#if echarts?? && echarts.modelList?? && (echarts.modelList)?size &gt; 0>
			<#list echarts.modelList as emodel>
			<#if emodel??>
			<div class="tab_content" id="emodel_tabcontent${emodel_index}" <#if emodel_index==0>style="display: block"</#if>>
				<table id="echarts_model_table${emodel_index}" class="infoTable echarts_table">
					<tbody>
						<tr>
							<td class="la">${getHtmlText('ec.view.echarts.model')}</td>
							<td class="co">
								<select id='echarts_emodel_modelCode${emodel_index}' onchange='ec.echarts.selectModelChange(${emodel_index})' value='' class='edit-select'>
									<option value=''></option>
									<#if models??>
									<#list models as model>
									<#if model?? && model.code?? && model.name??>
									<option value='${model.code}' modelName='${model.modelName}' <#if emodel.modelCode?? && emodel.modelCode==model.code>selected</#if>>${getText('${model.name}')}</option>
									</#if>
									</#list>
									</#if>
								</select>
							</td>
						</tr>
						<tr>
							<td class="la">${getHtmlText('ec.view.echarts.type')}</td>
							<td class="co">
								<select id='echarts_emodel_type${emodel_index}' value='' class='edit-select'>
									<option value='line' <#if (emodel.type)?? && (emodel.type)=='line'>selected</#if>>${getHtmlText('ec.view.echarts.line')}</option>
									<option value='bar' <#if (emodel.type)?? && (emodel.type)=='bar'>selected</#if>>${getHtmlText('ec.view.echarts.bar')}</option>
								</select>
							</td>
							<td class="la">${getHtmlText('ec.view.echarts.classification')}</td>
							<td class="co">
								<select id='echarts_emodel_xAxisfield${emodel_index}' value='' class='edit-select'>
									<option value=''></option>
								<#if propertiesMap?? && emodel.modelCode?? && propertiesMap[emodel.modelCode]??>
								<#assign properties = propertiesMap[emodel.modelCode]>
								<#if properties??>
								<#list properties as property>
								<#if property?? && property.code?? && property.name??>
									<option value='${property.columnName}' <#if (emodel.classificationColumn)?? && (emodel.classificationColumn=='${property.columnName}')>selected</#if> fieldtype='${property.type}'>${getText('${property.displayName}')}</option>
								</#if>
								</#list>
								</#if>
								</#if>
								</select>
							</td>
						</tr>
						<tr>
							<td class="la">${getHtmlText('ec.view.echarts.series')}</td>
							<td class="co">
								<select id='echarts_emodel_seriesfield${emodel_index}' value='' class='edit-select'>
									<option value=''></option>
								<#if propertiesMap?? && emodel.modelCode?? && propertiesMap[emodel.modelCode]??>
								<#assign properties = propertiesMap[emodel.modelCode]>
								<#list properties as property>
								<#if property?? && property.code?? && property.name??>
								<option value='${property.columnName}' <#if (emodel.seriesColumn)?? && (emodel.seriesColumn=='${property.columnName}')>selected</#if> fieldtype='${property.type}'>${getText('${property.displayName}')}</option>
								</#if>
								</#list>
								</#if>
								</select>
							</td>
							<td class="la">${getHtmlText('ec.view.echarts.valuefield')}</td>
							<td class="co">
								<select id='echarts_emodel_valuefield${emodel_index}' value='' class='edit-select'>
									<option value=''></option>
								<#if propertiesMap?? && emodel.modelCode?? && propertiesMap[emodel.modelCode]??>
								<#assign properties = propertiesMap[emodel.modelCode]>
								<#list properties as property>
								<#if property?? && property.code?? && property.name??>
								<option value='${property.columnName}' <#if (emodel.valueColumn)?? && (emodel.valueColumn=='${property.columnName}')>selected</#if> fieldtype='${property.type}'>${getText('${property.displayName}')}</option>
								</#if>
								</#list>
								</#if>
								</select>
							</td>
						</tr>
						<tr id="echarts_emodel_isEqualyAxisTr${emodel_index}" <#if emodel_index==0>class="display_none"</#if>>
							<td class="la">${getHtmlText('ec.view.echarts.yAxis.useFirstEmodel')}</td>
							<td class="co">
								<input type="checkbox" id="echarts_emodel_isEqualyAxis${emodel_index}" <#if emodel_index==0 || (emodel.yaxis)??><#else>checked="checked"</#if> onclick="ec.echarts.isEqualyAxis(${emodel_index})"/>
							</td>
						</tr>
						<tr id="echarts_emodel_yAxis1${emodel_index}" <#if emodel_index!=0><#if (emodel.yaxis)??><#else>class="display_none"</#if></#if>>
							<td class="la">${getHtmlText('ec.view.echarts.yAxisMax')}</td>
							<td class="co">
								<input id="echarts_emodel_yAxis_yAxisMax${emodel_index}" value="<#if (emodel.yaxis)?? && (emodel.yaxis.max)??>${emodel.yaxis.max!}</#if>" class='cui-edit-field' style="height: 99%;width: 97%"/>
							</td>
							<td class="la">${getHtmlText('ec.view.echarts.yAxisMin')}</td>
							<td class="co">
								<input id="echarts_emodel_yAxis_yAxisMin${emodel_index}" value="<#if (emodel.yaxis)?? && (emodel.yaxis.min)??>${emodel.yaxis.min!}</#if>" class='cui-edit-field' style="height: 99%;width: 97%"/>
							</td>
						</tr>
						<tr id="echarts_emodel_yAxis2${emodel_index}" <#if emodel_index!=0><#if (emodel.yaxis)??><#else>class="display_none"</#if></#if>>
							<td class="la">${getHtmlText('ec.view.echarts.yAxisName')}</td>
							<td class="co">
								<input id="echarts_emodel_yAxis_yAxisName${emodel_index}" value="<#if (emodel.yaxis)?? && (emodel.yaxis.name)??>${emodel.yaxis.name!}</#if>" class='cui-edit-field' style='height: 99%;width: 97%'/>
							</td>
							<td class="la">${getHtmlText('ec.view.echarts.yAxisPosition')}</td>
							<td class="co">
								<select id="echarts_emodel_yAxis_yAxisPosition${emodel_index}" name="emodel.yaxis.yAxisPosition" value='' class='edit-select'>
									<option value='left' <#if emodel.yaxis?? && (emodel.yaxis.position)?? && (emodel.yaxis.position)=='left'>selected</#if>>${getHtmlText('ec.view.echarts.position.left')}</option>
									<option value='right' <#if emodel.yaxis?? && (emodel.yaxis.position)?? && (emodel.yaxis.position)=='right'>selected</#if>>${getHtmlText('ec.view.echarts.position.right')}</option>
								</select>
							</td>
						</tr>
						<tr>
							<td class="la">
								<input type="checkbox" id="echarts_emodel_iscustomconditions${emodel_index}" onclick=ec.echarts.iscustomconditions(${emodel_index}) <#if (emodel.isCustomConditions)?? && (emodel.isCustomConditions)== true>checked="checked"</#if>>
							</td>
							<td class="co">${getHtmlText('ec.view.echarts.iscustomconditions')}</td>
							<td colspan="2">
								<div id="echarts_emodel_customconditions_button${emodel_index}" 
									<#if (emodel.isCustomConditions)?? && (emodel.isCustomConditions)== false>canclick="true" class="edit-btn btn-act"<#else>canclick="false" class="edit-btn-disabled"</#if> 
									onclick="ec.echarts.showCustomConditionsDialog(${emodel_index}, 'echarts_emodel_customConditions${emodel_index}')">
									<a class="cui-btn-l">&nbsp;</a>
									<a class="cui-btn-c">
										<span i18n='ec.view.customerCondition'>${getHtmlText('ec.view.echarts.customconditions')}</span>
									</a>
									<a class="cui-btn-r">&nbsp;</a>
								</div>
							</td>
						</tr>
						<tr>
							<td colspan="4" style="padding-left:30px">
								<textarea <#if (emodel.isCustomConditions)?? && (emodel.isCustomConditions)== false>readonly="true"</#if> id="echarts_emodel_customConditions${emodel_index}" class="cui-edit-textarea" style="height:100px;">${emodel.customConditions!}</textarea>
								<textarea id="echarts_emodel_customConditionsConfjson${emodel_index}" style="display:none;" class="confjson">${emodel.customConditionsConfjson!}</textarea>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
			</#if>
			</#list>
			</#if>
		</div>
		<!-- 事件 -->
		<div id="echarts_event" style="overflow:scroll; height:369px; display:none" >
			<table class="infoTable event_table">
				<tr>
					<td class="la" style="width:10%"><label>${getHtmlText('ec.view.cell.eventType')}</label>
						
					</td>
					<td class="co">
						<select id="field-events-select" class="edit-select">
							<option value="onload">onload</option>
							<option value="onclick">onclick</option>
							<option value="dbclick">dbclick</option>
							<option value="beforeRender">beforerender</option>
							<option value="afterRender">afterrender</option>
						</select>
					</td>
					<td class="la" style="padding-left: 5px; text-align: left">
						<img onclick="ec.echarts.addEvents($('#field-events-select').val())" title="${getText('ec.view.addevent')}" src="/bap/static/foundation/images/icon-add.gif" /><br/>
					</td>
					<td class="la"></td>
				</tr>
			</table>
			<table id="event_table" class="infoTable event_table">
				<tr id="tr_onload" <#if !(eventsMap?? && eventsMap["onload"]??)>class="display_none"</#if>>
					<td class="la" style="width:20%">onload
						<@helpTip id="onloadTip" helpInfo="图表渲染完成后执行" helpCode="console.log('图表数据加载完成');<br/>// 增加数据视图<br/>myCharts.${echarts.layoutName}.setOption({<br/>&#160;&#160;&#160;toolbox: {<br/>&#160;&#160;&#160;&#160;&#160;&#160;feature: {<br/>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;dataView: {show: true, readOnly: false}<br/>&#160;&#160;&#160;&#160;&#160;&#160;}<br/>&#160;&#160;&#160;}<br/>})" helpHint="注意：只在图表渲染完成后执行一次" />
					</td>
					<td>
						<textarea id="echarts_event_onload" class="cui-edit-textarea event-textarea" ><#if eventsMap?? && eventsMap["onload"]??>${eventsMap["onload"]}</#if></textarea>
					</td>
					<td style="padding-left: 10px;"><img onclick="ec.echarts.delEvents('onload')" src="/bap/static/foundation/images/icon-del.gif"/></td>
				</tr>
				<tr id="tr_onclick" <#if !(eventsMap?? && eventsMap["onclick"]??)>class="display_none"</#if>>
					<td class="la" style="width:20%">onclick
						<@helpTip id="clickTip" helpInfo="鼠标点击事件，包含参数 params，这是一个包含点击图形的数据信息的对象，如下格式：<br/>
						{<br/>
						&#160;&#160;&#160;seriesType: string, // 系列类型。值可能为：'line'、'bar'、'pie' 等。<br/>
						&#160;&#160;&#160;seriesIndex: number, // 系列在传入的 option.series 中的 index。<br/>
						&#160;&#160;&#160;seriesName: string, // 系列名称。<br/>
						&#160;&#160;&#160;name: string, // 数据名，类目名<br/>
						&#160;&#160;&#160;dataIndex: number, // 数据在传入的 data 数组中的 index<br/>
						&#160;&#160;&#160;data: Object, // 传入的原始数据项<br/>
						&#160;&#160;&#160;value: number|Array // 传入的数据值<br/>
						&#160;&#160;&#160;color: string 数据图形的颜色。<br/>
						}" helpCode="console.log(params);" helpHint="由于echarts限制，假设同时配置单击、双击事件，双击操作会触发两次单击和一次双击" width=500 />
					</td>
					<td>
						<textarea id="echarts_event_onclick" class="cui-edit-textarea event-textarea" ><#if eventsMap?? && eventsMap["onclick"]??>${eventsMap["onclick"]}</#if></textarea>
					</td>
					<td style="padding-left: 10px;"><img onclick="ec.echarts.delEvents('onclick')" src="/bap/static/foundation/images/icon-del.gif"/></td>
				</tr>
				<tr id="tr_dbclick" <#if !(eventsMap?? && eventsMap["dbclick"]??)>class="display_none"</#if>>
					<td class="la" style="width:20%">dbclick
						<@helpTip id="dbclickTip" helpInfo="鼠标双击事件，包含参数 params 与 onclick 事件的params一致" helpCode="console.log(params);" helpHint="由于echarts限制，假设同时配置单击、双击事件，双击操作会触发两次单击和一次双击" /></td>
					<td>
						<textarea id="echarts_event_dbclick" class="cui-edit-textarea event-textarea" ><#if eventsMap?? && eventsMap["dbclick"]??>${eventsMap["dbclick"]}</#if></textarea>
					</td>
					<td style="padding-left: 10px;"><img onclick="ec.echarts.delEvents('dbclick')" src="/bap/static/foundation/images/icon-del.gif"/></td>
				</tr>
				<tr id="tr_beforeRender" <#if !(eventsMap?? && eventsMap["beforeRender"]??)>class="display_none"</#if>>
					<td class="la" style="width:20%">beforerender
						<@helpTip id="beforeRenderkTip" helpInfo="执行请求图表数据方法echartsData()前执行，可修改参数url、data，url为图表数据请求路径，data为参数" helpCode="console.log('url' + url);<br/>console.log('data' + data);<br/>url='http://www.baidu.com';//修改url<br/>data.test=1;//增加参数test" />
					</td>
					<td>
						<textarea id="echarts_event_beforeRender" class="cui-edit-textarea event-textarea" ><#if eventsMap?? && eventsMap["beforeRender"]??>${eventsMap["beforeRender"]}</#if></textarea>
					</td>
					<td style="padding-left: 10px;"><img onclick="ec.echarts.delEvents('beforeRender')" src="/bap/static/foundation/images/icon-del.gif"/></td>
				</tr>
				<tr id="tr_afterRender" <#if !(eventsMap?? && eventsMap["afterRender"]??)>class="display_none"</#if>>
					<td class="la" style="width:20%">afterrender
						<@helpTip id="afterRenderTip" helpInfo="执行请求图表数据请求方法后执行，包含参数echarts数据对象，包含echartsCode、图例数据、系列数据、X轴数据" helpCode="var xAxisData=echarts.xAxisData; // 获取X轴数据<br/>// 修改图表标题<br/>myCharts.${echarts.layoutName}.setOption({<br/>&#160;&#160;&#160;title: {<br/>&#160;&#160;&#160;&#160;&#160;&#160;text: '自定义标题'<br/>&#160;&#160;&#160;}<br/>})" />
					</td>
					<td>
						<textarea id="echarts_event_afterRender" class="cui-edit-textarea event-textarea"><#if eventsMap?? && eventsMap["afterRender"]??>${eventsMap["afterRender"]}</#if></textarea>
					</td>
					<td style="padding-left: 10px;"><img onclick="ec.echarts.delEvents('afterRender')" src="/bap/static/foundation/images/icon-del.gif"/></td>
				</tr>
			</table>
		</div>
	</div>
</form>
<@customerCondition viewCode="${view.code}" showArea="echarts_emodel_customConditions0" checkBoxId="echarts_emodel_iscustomconditions0"/>
<script type="text/javascript">
$(function(){
	//注册命名空间
	CUI.ns("ec.echarts");
	
	// 属性、数据源、事件页签切换
	ec.echarts.tab = function(v) {
		if (v == 0) {
			document.getElementById("echarts_tab_attribute").classList.add("selected");
			document.getElementById("echarts_tab_emodel").classList.remove("selected");
			document.getElementById("echarts_tab_event").classList.remove("selected");
			document.getElementById("echarts_attribute").style.display = "block";
			document.getElementById("echarts_emodel").style.display = "none";
			document.getElementById("echarts_event").style.display = "none";
		} else if (v == 1) {
			document.getElementById("echarts_tab_attribute").classList.remove("selected");
			document.getElementById("echarts_tab_emodel").classList.add("selected");
			document.getElementById("echarts_tab_event").classList.remove("selected");
			document.getElementById("echarts_attribute").style.display = "none";
			document.getElementById("echarts_emodel").style.display = "block";
			document.getElementById("echarts_event").style.display = "none";
		} else if (v == 2) {
			document.getElementById("echarts_tab_attribute").classList.remove("selected");
			document.getElementById("echarts_tab_emodel").classList.remove("selected");
			document.getElementById("echarts_tab_event").classList.add("selected");
			document.getElementById("echarts_attribute").style.display = "none";
			document.getElementById("echarts_emodel").style.display = "none";
			document.getElementById("echarts_event").style.display = "block";
		}
	}
	
	// 提交前valid
	ec.echarts.beforesubmit = function() {
		// 提交前组织数据源配置
		ec.echarts.setModellist();
		ec.echarts.setEventlist();
		return true;
	}
	
	// 图表校验 start
	ec.echarts.check = function() {
		clearErrorLabels();
		if(!ec.echarts.checkNotNull()){
			return false;
		}
		if(!ec.echarts.checkClassification()){
			return false;
		}
		if(!ec.echarts.checkSeriesfield()){
			return false;
		}
		if(!ec.echarts.checkFieldType()){
			return false;
		}
		if(!ec.echarts.checkYAxis()){
			return false;
		}
		return true;
	}
	
	//同一模型分类字段唯一; 不同模型不允许选择分类
	ec.echarts.checkClassification = function() {
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		var isCombination = false;
		var firstIndex = ec.echarts.getFirstEmodelIndex();
		var firstModel = $("#echarts_emodel_modelCode" + firstIndex).val();
		var firstClassification = $("#echarts_emodel_xAxisfield" + firstIndex).val();
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			var model = $("#echarts_emodel_modelCode" + index).val();
			if (model !== firstModel) {
				isCombination = true;
			}
		}
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			var classificationId = "echarts_emodel_xAxisfield" + index;
			var classification = $("#" + classificationId).val();
			if(isCombination){
				// 组合图表不允许填写分类信息
				if((firstClassification !== undefined && firstClassification !== null && firstClassification !== "") 
						|| (classification !== undefined && classification !== null && classification !== "")){
					ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.classification.nomodels')}", classificationId);
					return false;
				}
			}else{
				// 非组合图表分类和系列必须选一个
				if(($("#echarts_emodel_xAxisfield" + index).val() == null || $("#echarts_emodel_xAxisfield" + index).val() == "") 
					&& ($("#echarts_emodel_seriesfield" + index).val() == null || $("#echarts_emodel_seriesfield" + index).val() == "")){
					ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.classification.series.mustone')}", classificationId);
					showErrorField($("#echarts_emodel_seriesfield"+index).parent());
					return false;
				}
				// 分类最多只能选择1个字段
				if(firstClassification !== classification){
					ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.classification.onlyonefield')}", classificationId);
					return false;
				}
			}
		}
		return true;
	}
	
	// 同一模型系列字段唯一; 不同模型系列字段类型一致
	ec.echarts.checkSeriesfield = function() {
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		var firstIndex = ec.echarts.getFirstEmodelIndex();
		var firstModel = $("#echarts_emodel_modelCode" + firstIndex).val();
		var firstSeriesfield = $("#echarts_emodel_seriesfield" + firstIndex).val();
		var firstFieldType = $("#echarts_emodel_seriesfield" + firstIndex + " option:selected").attr('fieldtype'); 
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			var model = $("#echarts_emodel_modelCode" + index).val();
			var fieldId = "echarts_emodel_seriesfield" + index;
			var seriesfield = $("#" + fieldId).val();
			if (model !== firstModel) {
				var fieldType = $("#" + fieldId + " option:selected").attr('fieldtype'); 
				if (firstFieldType !== fieldType) {
					ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.series.modelsfieldtype')}", fieldId);
					return false;
				}
			} else if (firstSeriesfield !== seriesfield){
				ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.series.onemodelonlyonefield')}", fieldId);
				return false;
			}
		}
		return true;
	}
	
	// 分类和系列不能选择密码类型
	// 值字段只能为数字类型
	ec.echarts.checkFieldType = function() {
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			var valueType = $("#echarts_emodel_valuefield" + index + " option:selected").attr('fieldtype');
			var xAxisType = $("#echarts_emodel_xAxisfield" + index + " option:selected").attr('fieldtype');
			var seriesType = $("#echarts_emodel_seriesfield" + index + " option:selected").attr('fieldtype');
			if (valueType !== "INTEGER" && valueType !== "DECIMAL" && valueType !== "LONG" && valueType !== "MONEY") {
				ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.valuefield.onlynumber')}", "echarts_emodel_valuefield"+index);
				return false;
			}
			if (xAxisType == "PASSWORD") {
				ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.field.cannot.password')}", "echarts_emodel_xAxisfield" + index);
				return false;
			}
			if (seriesType == "PASSWORD") {
				ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.field.cannot.password')}", "echarts_emodel_seriesfield" + index);
				return false;
			}
		}
		return true;
	}
	
	ec.echarts.checkNotNull = function() {
		// 数据模型; 分类和系列至少选其一; 值字段;
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		if ((lis.length-1) == 0) {
			ec.echarts.error(1, null, "${getHtmlText('ec.view.echarts.model.notchoice')}", null);
			return false;
		}
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			if($("#echarts_emodel_modelCode" + index).val() == null || $("#echarts_emodel_modelCode" + index).val() == ""){
				ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.model.notchoice')}", "echarts_emodel_modelCode"+index);
				return false;
			}
			if($("#echarts_emodel_valuefield" + index).val() == null || $("#echarts_emodel_valuefield" + index).val() == ""){
				ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.valuefield.notchoice')}", "echarts_emodel_valuefield"+index);
				return false;
			}
		}
		return true;
	}
	
	ec.echarts.error = function(tabIndex,emodelIndex,msg,inputId){
		ec.echarts.tab(tabIndex);
		if(emodelIndex!==null && emodelIndex !==""){
			ec.echarts.emodeltab(emodelIndex);
		}
		if(inputId != null && inputId !== ""){
			showErrorField($("#"+inputId).parent());
		}
		ec_echarts_dialogDialogErrorBarWidget.show(msg, "f");
	}
	
	ec.echarts.checkYAxis = function(){
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			var yAxisMax = $("#echarts_emodel_yAxis_yAxisMax" + index).val();
			var yAxisMin = $("#echarts_emodel_yAxis_yAxisMin" + index).val();
			var yAxisName = $("#echarts_emodel_yAxis_yAxisName" + index).val();
			var yAxisPosition = $("#echarts_emodel_yAxis_yAxisPosition" + index).val();
			if(yAxisMin!="") {
				if(!ec.echarts.checkNumber(yAxisMin)) {
					ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.yaxisminmax.onlynumber')}", "echarts_emodel_yAxis_yAxisMin"+index);
					return false;
				}
			}
			if(yAxisMax!="") {
				if(!ec.echarts.checkNumber(yAxisMax)) {
					ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.yaxisminmax.onlynumber')}", "echarts_emodel_yAxis_yAxisMax"+index);
					return false;
				}
			}
			if(yAxisMin!="" && yAxisMax!="") {
				if(!ec.echarts.checkyAxisMaxMin(yAxisMin, yAxisMax)) {
					ec.echarts.error(1, index, "${getHtmlText('ec.view.echarts.yaxisminmax.maxbigger')}", "echarts_emodel_yAxis_yAxisMax"+index);
					showErrorField($("#echarts_emodel_yAxis_yAxisMin"+index).parent());
					return false;
				}
			}
		}
		return true;
	}
	
	ec.echarts.checkNumber = function(num) {
		var regPos = /^\d+(\.\d+)?$/; //非负浮点数
	    var regNeg = /^(-(([0-9]+\.[0-9]*[1-9][0-9]*)|([0-9]*[1-9][0-9]*\.[0-9]+)|([0-9]*[1-9][0-9]*)))$/; //负浮点数
	    if(regPos.test(num) || regNeg.test(num)) {
	    	return true;
	    }
	    return false;
	}
	
	ec.echarts.checkyAxisMaxMin = function(mix,max) {
		var flag = true;
        if (parseFloat(mix) > parseFloat(max)) {
			flag = false;
		}
		return flag;
	}
	// 图表校验 end
	
	// 数据源 start
	// 多数据源切换
	ec.echarts.emodeltab = function(v) {
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		for(var i = 0; i < lis.length; i++) {
			var li = lis[i];
			if(li == document.getElementById("emodel_tab" + v)) {
				li.classList.add("active");
			} else {
				li.classList.remove("active");
			}
		}
		var divs = document.getElementsByClassName("tab_content");
		for(var i = 0; i < divs.length; i++) {
			var divv = divs[i];
			if(divv == document.getElementById("emodel_tabcontent" + v)) {
				divv.style.display = "block";
			} else {
				divv.style.display = "none";
			}
		}
	}
	
	ec.echarts.addemodel = function() {
		var ul = document.getElementById("emodel_tab_ul");
		var index = parseInt(ec.echarts.getLastEmodelIndex()) + 1;
		$("#add_emodel_li").remove();
		$("#emodel_tab_ul").append("<li id='emodel_tab" + index + "'>"
			+ "<a href='javascript:;' onclick='ec.echarts.emodeltab(" + index + ")'>数据源" + (index+1) 
			+ "</a><span class='tabs-close' onclick='ec.echarts.delemodel(" + index
			+ ")' title='${getText('ec.view.close')}'></span></li>");
		$("#emodel_tab_ul").append("<li id='add_emodel_li'>"
			+ "<a href='javascript:;' onclick='ec.echarts.addemodel()' style='padding-top:10px'><img src='/bap/static/ec/images/addEcharts.gif'/></a>"
			+ "</li>");
		var textareaid = 'echarts_emodel_customConditions' + index;
		$("#echarts_emodel").append(
			"<div class='tab_content' id='emodel_tabcontent" + index + "' >"
			+ "<table id='echarts_model_table" + index + "' class='infoTable echarts_table'>"
			+ "<tbody>"
			+ "<tr>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.model')}</td>"
			+ "<td><select id='echarts_emodel_modelCode" + index + "' onchange='ec.echarts.selectModelChange(" + index + ")' value='' class='edit-select'></select></td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.type')}</td>"
			+ "<td class='co'><select id='echarts_emodel_type" + index + "' value='' class='edit-select'>"
			+ "<option value='line'>${getHtmlText('ec.view.echarts.line')}</option>"
			+ "<option value='bar'>${getHtmlText('ec.view.echarts.bar')}</option></select></td>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.classification')}</td>"
			+ "<td class='co'><select id='echarts_emodel_xAxisfield" + index + "' value='' class='edit-select'/></td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.series')}</td>"
			+ "<td class='co'><select id='echarts_emodel_seriesfield" + index + "' value='' class='edit-select'/></td>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.valuefield')}</td>"
			+ "<td class='co'><select id='echarts_emodel_valuefield" + index + "' value='' class='edit-select'/></td>"
			+ "</tr>"
			+ "<tr id='echarts_emodel_isEqualyAxisTr" + index + "'>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.yAxis.useFirstEmodel')}</td>"
			+ "<td class='co'><input type='checkbox' id='echarts_emodel_isEqualyAxis" + index + "' checked='checked' onclick='ec.echarts.isEqualyAxis(" + index + ")'/></td>"
			+ "</tr>"
			+ "<tr id='echarts_emodel_yAxis1" + index + "' class='display_none'>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.yAxisMax')}</td>"
			+ "<td class='co'><input id='echarts_emodel_yAxis_yAxisMax"+ index +"' class='cui-edit-field' style='height: 99%;width: 97%'></td>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.yAxisMin')}</td>"
			+ "<td class='co'><input id='echarts_emodel_yAxis_yAxisMin"+ index +"' class='cui-edit-field' style='height: 99%;width: 97%'></td>"
			+ "</tr>"
			+ "<tr id='echarts_emodel_yAxis2" + index + "' class='display_none'>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.yAxisName')}</td>"
			+ "<td class='co'><input id='echarts_emodel_yAxis_yAxisName"+ index +"' class='cui-edit-field' style='height: 99%;width: 97%'></td>"
			+ "<td class='la'>${getHtmlText('ec.view.echarts.yAxisPosition')}</td>"
			+ "<td class='co'><select id='echarts_emodel_yAxis_yAxisPosition" + index + "' value='' class='edit-select'>"
			+ "<option value='left'>${getHtmlText('ec.view.echarts.position.left')}</option>"
			+ "<option value='right'>${getHtmlText('ec.view.echarts.position.right')}</option></select></td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td class='la' style='width: 11%;'>${getHtmlText('ec.view.echarts.iscustomconditions')}</td>"
			+ "<td class='co'><input type='checkbox' id='echarts_emodel_iscustomconditions" + index + "' onclick='ec.echarts.iscustomconditions(" + index + ")'>"
			+ "<td colspan='2'><div id='echarts_emodel_customconditions_button" + index + "' canclick='true' class='edit-btn btn-act' "
			+ "onclick='ec.echarts.showCustomConditionsDialog(" + index + ",\"" + textareaid +"\")'>"
			+ "<a class='cui-btn-l'>&nbsp;</a><a class='cui-btn-c'>${getHtmlText('ec.view.customerCondition')}</a><a class='cui-btn-r'>&nbsp;</a></div></td>"
			+ "</tr>"
			+ "<tr>"
			+ "<td colspan='4' style='padding-left:30px'>"
			+ "<textarea readonly='true' id='echarts_emodel_customConditions" + index + "' class='cui-edit-textarea' style='width:98%;height:100px;' />"
			+ "<textarea id='echarts_emodel_customConditionsConfjson" + index + "' style='display:none;' class='confjson' /></td>"
			+ "</tr>"
			+ "</tbody></table></div>"
		);
		$('#emodel_tabcontent' + index).find(".edit-select").mSelect();
		ec.echarts.selectModelInit(index);
		ec.echarts.emodeltab(index);
	}
	
	ec.echarts.delemodel = function(v){
		var index = ec.echarts.getEmodelLength();
		if (index == 1) {
			ec.echarts.error(1, null, "${getHtmlText('ec.view.echarts.emodel.cannot.delete')}", null);
			return;
		}
		var activeli = $('.active').attr("id");
		
		$('#emodel_tab' + v).remove();
		$('#emodel_tabcontent' + v).remove();
		
		var index = ec.echarts.getFirstEmodelIndex();
		// 如果第一个数据源没有Y轴配置则打开Y轴配置
		if(!$('#echarts_emodel_isEqualyAxisTr' + index).hasClass("display_none")) {
			document.getElementById("echarts_emodel_isEqualyAxisTr" + index).classList.add("display_none");
			document.getElementById("echarts_emodel_yAxis1" + index).classList.remove("display_none");
			document.getElementById("echarts_emodel_yAxis2" + index).classList.remove("display_none");
		}
		if (activeli == ("emodel_tab" + v)) { // 如果删除当前选中，删除后选中第一个
			ec.echarts.emodeltab(index);
		}
	}
	
	ec.echarts.setModellist = function() {
		var modelList = [];
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		var echartsCode = $("input[name='echarts.code']").val();
		var projFlag = $("input[name='isProj']").val();
		var yAxisLeftSize = 0;
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			var yAxisPosition = $("#echarts_emodel_yAxis_yAxisPosition" + index).val();
			var isEqualyAxis = $("#echarts_emodel_isEqualyAxis" + index).prop('checked');
			if (!isEqualyAxis) {
				if (yAxisPosition == "left") {
					yAxisLeftSize = yAxisLeftSize + 1;
				}
			}
		}
		var yIndex = 0;
		var leftOffset = 0;
		var rightOffset = 0;
		for(var i=0; i<(lis.length-1); i++){
			var index = lis[i].id.slice(10);
			var modelCode = $("#echarts_emodel_modelCode" + index).val();
			var type = $("#echarts_emodel_type" + index).val();
			var xAxisField = $("#echarts_emodel_xAxisfield" + index).val();
			var seriesField = $("#echarts_emodel_seriesfield" + index).val();
			var valueField = $("#echarts_emodel_valuefield" + index).val();
			var xAxisName = $("#echarts_xAxisName").val();
			var yAxisMax = $("#echarts_emodel_yAxis_yAxisMax" + index).val();
			var yAxisMin = $("#echarts_emodel_yAxis_yAxisMin" + index).val();
			var yAxisName = $("#echarts_emodel_yAxis_yAxisName" + index).val();
			var yAxisPosition = $("#echarts_emodel_yAxis_yAxisPosition" + index).val();
			var isEqualyAxis = $("#echarts_emodel_isEqualyAxis" + index).prop('checked');
			var yAxisStr = "";
			if (!isEqualyAxis) {
				var offset = 0;
				if (yAxisPosition == "left") {
					offset = leftOffset;
					leftOffset = leftOffset + 40;
				} else {
					offset = rightOffset;
					rightOffset = rightOffset + 40;
				}
				yAxisStr = JSON.stringify([{"max": yAxisMax, "min": yAxisMin, "name": yAxisName, "position": yAxisPosition, "offset": offset, "index": yIndex}]);
				yIndex = yIndex + 1;
			}
			var isCustomConditions = $("#echarts_emodel_iscustomconditions" + index).prop('checked');
			var customConditions = $("#echarts_emodel_customConditions" + index).val();
			var customConditionsConfjson = $("#echarts_emodel_customConditionsConfjson" + index).val();
			modelList.push({
				"code": echartsCode + "@@emodel" + i,
				"echartsCode": echartsCode,
				"modelCode": modelCode,
				"type": type,
				"classificationColumn": xAxisField,
				"seriesColumn": seriesField,
				"valueColumn": valueField,
				"xaxisStr": JSON.stringify([{"name": xAxisName}]),
				"yaxisStr": yAxisStr,
				"isCustomConditions": isCustomConditions,
				"customConditions": customConditions,
				"customConditionsConfjson": customConditionsConfjson,
				"projFlag": projFlag
			});
		}
		var json = JSON.stringify(modelList);
		$("input[name='emodelsJson']").val(json);
	}
	
	ec.echarts.getLastEmodelIndex = function(){
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		var length = ec.echarts.getEmodelLength();
		if (length == 0) {
			return -1;
		}
		var id = lis[length-1].id;
		var index = id.slice(10);
		return index;
	}
	
	ec.echarts.getFirstEmodelIndex = function(){
		var lis = document.getElementById("emodel_tab_ul").getElementsByTagName("li");
		if ((lis.length-1) < 0) {
			return -1;
		}
		var id = lis[0].id;
		var index = id.slice(10);
		return index;
	}
	
	ec.echarts.isEqualyAxis = function(v){
		if (!$('#echarts_emodel_isEqualyAxis' + v).prop('checked')) {
			document.getElementById("echarts_emodel_yAxis1" + v).classList.remove("display_none");
			document.getElementById("echarts_emodel_yAxis2" + v).classList.remove("display_none");
		} else {
			document.getElementById("echarts_emodel_yAxis1" + v).classList.add("display_none");
			document.getElementById("echarts_emodel_yAxis2" + v).classList.add("display_none");
		}
	}
	
	ec.echarts.delAllOption = function(name, index){
		$("#echarts_emodel_" + name + index).delAllOption();
		$("#echarts_emodel_" + name + index).delOption("");
		$("#echarts_emodel_" + name + index).addOption("", "");
	}
	
	ec.echarts.selectModelInit = function(index){
		ec.echarts.delAllOption("modelCode", index);
		ec.echarts.delAllOption("xAxisfield", index);
		ec.echarts.delAllOption("seriesfield", index);
		ec.echarts.delAllOption("valuefield", index);
		$.ajax({
			async: false,
			type : "POST",
			url : "/msService/ec/model/list-select?entity.code=${view.entity.code}",
			success : function(msg){
				for(var i = 0 ; i < msg.length ; i ++){
					$("#echarts_emodel_modelCode" + index).addOption(msg[i].nameInternational, msg[i].code);
					$("#echarts_emodel_modelCode" + index + " option[value='" + msg[i].code + "']").attr("modelName", msg[i].modelName);
				}
			}
		});
	}
	
	ec.echarts.selectModelChange = function(index){
		ec.echarts.delAllOption("xAxisfield", index);
		ec.echarts.delAllOption("seriesfield", index);
		ec.echarts.delAllOption("valuefield", index);
		var modelCode = $("#echarts_emodel_modelCode" + index).val();
		if (modelCode != "") {
			$.ajax({
				async: false,
				type : "POST",
				url : "/msService/ec/property/list-all-select?model.code=" + modelCode,
				success : function(msg){
					for(var i = 0 ; i < msg.length ; i ++){
						$("#echarts_emodel_xAxisfield" + index).addOption(msg[i].displayNameInternational, msg[i].columnName);
						$("#echarts_emodel_seriesfield" + index).addOption(msg[i].displayNameInternational, msg[i].columnName);
						$("#echarts_emodel_seriesfield" + index + " option[value='" + msg[i].columnName + "']").attr("fieldtype", msg[i].type);
						$("#echarts_emodel_valuefield" + index).addOption(msg[i].displayNameInternational, msg[i].columnName);
						$("#echarts_emodel_valuefield" + index + " option[value='" + msg[i].columnName + "']").attr("fieldtype", msg[i].type);
					}
				}
			});
			$('#echarts_emodel_customConditions' + index).val('');
			$('#echarts_emodel_customConditionsConfjson' + index).val('');
		}
	};
	
	ec.echarts.getTableRows = function(tableId) {
		return document.getElementById(tableId).rows.length;
	}
	
	ec.echarts.getEmodelLength = function() {
		return $("#emodel_tab_ul li").length-1;
	}
	
	ec.echarts.iscustomconditions = function(index) {
		if($('#echarts_emodel_iscustomconditions' + index).prop('checked')) {
			$('#echarts_emodel_customConditions' + index).removeAttr("readonly");
			$('#echarts_emodel_customconditions_button' + index).attr('class', "edit-btn-disabled");
			$('#echarts_emodel_customconditions_button' + index).attr('canclick', 'false');
			if ($('#customerJsonString').val() == "") {
				dg.advQuery.beforeInitAdvQuery("echarts_emodel_customConditions"+index);
				dg.advQuery.initAdvQuery();
			}
		} else {
			CUI.Dialog.confirm("${getHtmlText('ec.view.leavehandwriting')}", function(){
				$('#echarts_emodel_customConditions' + index).val('');
				$('#echarts_emodel_customConditions' + index).attr('readonly', 'true');
				$('#echarts_emodel_customconditions_button' + index).attr('class', "edit-btn btn-act");
				$('#echarts_emodel_customconditions_button' + index).attr('canclick', 'true');
				var json = $('#customerJsonString').val();
				var datas = {"dgQueryCond" : json};
				CUI.ajax({
					url: "/msService/ec/customerCon/transtoSql",
					type: 'post',
					async: false,
					data: datas,
					success: function(msg) {
						if(msg.success==false || msg.data==null){
							dg.advQuery.showMsg('');
						}else{
							dg.advQuery.showMsg(msg.data);
							saveConfJsonVal(json);
						}
					}
				});
			}, function(){
				$('#echarts_emodel_iscustomconditions' + index).attr("checked", "true")
			});
		}
	}
	
	ec.echarts.showCustomConditionsDialog = function(index, textareaId) {
		var activeIndex = $('.active').attr("id").substring(10);
		var modelCode = $("#echarts_emodel_modelCode" + activeIndex).val();
		if(modelCode==undefined || modelCode==null || modelCode==""){
			ec.echarts.error(1, activeIndex, "${getHtmlText('ec.view.echarts.model.notchoice')}", "echarts_emodel_modelCode"+activeIndex);
			return;
		}
		if($('#echarts_emodel_customconditions_button' + index).attr('canclick')=="true"){
			dg.advQuery.beforeInitAdvQuery(textareaId);
			dg.advQuery.showAdv();
		}
	}
	
	ec.echarts.isshowlegend = function() {
		if($("input[name='echarts.isShowLegend']").prop('checked')) {
			document.getElementById("legendposition_tr").classList.remove("display_none");
		} else {
			document.getElementById("legendposition_tr").classList.add("display_none");
		}
	}
	
	// 自定义条件start
	dg.advQuery.showMsg = function (msg) {
		var json = dg.advQuery.query._getCond();
		// 保存配置json
		var el = getActiveTextareaEl();
		saveConfJsonVal(json);
		el.val(msg);
	}

	function saveConfJsonVal(val) {
		var tid = getActiveTextareaId();
		$('#' + tid).siblings('.confjson').val( val );
	}

	function getConfJsonVal(){
		var tid = getActiveTextareaId();
		return $('#' + tid).siblings('.confjson').val();
	}

	// 获取当前操作textarea
	function getActiveTextareaEl() {
		return $('#' + getActiveTextareaId() ).length ? $('#' + getActiveTextareaId() ) : $('#' + 'conditionArea');
	}

	function setActiveTextareaId(textareaId){
		dg.advQuery.textreaid = textareaId;
	}

	function getActiveTextareaId(){
		return dg.advQuery.textreaid;
	}

	dg.advQuery.beforeInitAdvQuery = function (textareaId) {		
		// 修改当前对应textreaid
		setActiveTextareaId(textareaId);
		// 根据当前textarea设置customerJsonString值
		// 区别获取json
		var json = getConfJsonVal();
		$('#customerJsonString').val(json);
	}
	
	dg.advQuery.initAdvQuery = function(_advDialog){
		var activeIndex = $('.active').attr("id").substring(10);
		var modelCode = $("#echarts_emodel_modelCode" + activeIndex).val();
		var modelName = $("#echarts_emodel_modelCode" + activeIndex).find("option:selected").attr('modelName');
		modelName = modelName.substring(0,1).toLowerCase()+modelName.substring(1);
		$('#advQueryContainer').empty();
		if(CUI('#BBIT_DP_CONTAINER')!=null&&CUI('#BBIT_DP_CONTAINER').length>0) {
			CUI('#BBIT_DP_CONTAINER').remove();
		}
		var json = getConfJsonVal();
		CUI('body').data('currentConddymk_1.0.0_dymkjcst_zqx_views', CUI.parseJSON(json));
		dg.advQuery.query = new CUI.AdvQuery({
			elementId: 'advQueryContainer',
			namespace: 'dg.advQuery',
			env: (window._proj_config_flag?'proj':'ec')
			,"viewCode": "${view.code}"
			,modelCode: modelCode
			,modelName: modelName
		});
		if(json!=''){
			dg.advQuery.query._resume(null, eval('(' + json + ')'));
		}
	}
	
    // 自定义条件end
	// 数据源事件 end
	
	ec.echarts.addEvents = function(eventName) {
		var x = document.getElementById("tr_" + eventName).classList.contains("display_none");
		if (!x) {
			CUI.Dialog.alert("${getText('ec.view.echarts.event.already.exist')}");
			return false;
		}
		var current = $("#tr_" + eventName);
	  	var last = $("#event_table").find("tr:last");
	  	var lastId = last.attr('id');
	  	if (lastId != ("tr_" + eventName)) {
	    	current.insertAfter(last);
	  	}
	  	document.getElementById("tr_" + eventName).classList.remove("display_none");
	}
	
	ec.echarts.delEvents = function(eventName) {
		if ($("#echarts_event_" + eventName).val().trim() == "") {
			$("#echarts_event_" + eventName).val("");
			document.getElementById("tr_" + eventName).classList.add("display_none");
		} else {
			CUI.Dialog.confirm("${getHtmlText('ec.view.echarts.events.del.confirm')}", function(){
				$("#echarts_event_" + eventName).val("");
				document.getElementById("tr_" + eventName).classList.add("display_none");
			}, function(){});
		}
	}
	
	ec.echarts.setEvent = function(echartsCode, eventList, eventName, projFlag) {
		var eventValue = $("#echarts_event_" + eventName).val();
		if (eventValue != null && eventValue != "") {
			eventList.push({
				"code": echartsCode + "@@" + eventName,
				"name": eventName,
				"moduleCode": "${view.entity.module.code}",
				"entityCode": "${view.entity.code}",
				"function": eventValue,
				"projFlag": projFlag
			});
		}
		return eventList;
	}
	
	ec.echarts.setEventlist = function() {
		var eventList = [];
		var echartsCode = $("input[name='echarts.code']").val();
		var projFlag = $("input[name='isProj']").val();
		eventList = ec.echarts.setEvent(echartsCode, eventList, "onload", projFlag);
		eventList = ec.echarts.setEvent(echartsCode, eventList, "onclick", projFlag);
		eventList = ec.echarts.setEvent(echartsCode, eventList, "dbclick", projFlag);
		eventList = ec.echarts.setEvent(echartsCode, eventList, "beforeRender", projFlag);
		eventList = ec.echarts.setEvent(echartsCode, eventList, "afterRender", projFlag);
		var json = JSON.stringify(eventList);
		$("input[name='eventJson']").val(json);
	}
	CUI(function(){
		function submitBapForm(){//电子签名成功之后出现进度条并提交表单
			var ecFormFlag = false;
			var retrialFormFlag = false;
			if(ecFormFlag && ( $('#ec_echarts_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
				// dialog不出进度条
				ecFormFlag = false;
			}
			ecFormFlag = (ecFormFlag || retrialFormFlag);
			
			//前台验证通过之后出进度条
			CUI.Dialog.toggleAllButton('ec_echarts_edit_form',true,ecFormFlag, true);
		// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
		setTimeout(function(){
			
				// 延迟保存数据, 解决onchange事件无法触发问题
				var formId = 'ec_echarts_edit_form';
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
								$("#ec_echarts_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
									if($(this).parents('td[nullable=false]').length > 0) {
										showErrorField($(this));
									}
								});
							} else {
								var field = CUI("#ec_echarts_edit_form *[name='"+index+"']");
								if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
									showErrorField(field.next());
								} else {
									showErrorField(field);
								}
							}
							CUI("#ec_echarts_edit_form *[name='"+index+"']").first().focus();
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
						oErrorWidget = ec_echarts_edit_formDialogErrorBarWidget;
						if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
							oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
						}	else {
							oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
						}					
						if(CUI.Dialog){
							CUI.Dialog.toggleAllButton('ec_echarts_edit_form', true);
						}
					},
					success : function(msg){
						window.onbeforeunload = null;
						if(window.containerLoadPanelWidget) {
							setTimeout(function(){closeLoadPanel();}, 500);
						}
						ec.echartsConfigCallback(msg,postData);
					}
				});
			}
		}, 600);
			return false;
		}





		CUI('#ec_echarts_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
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
			//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_echarts_edit_form',true,ecFormFlag);
			//});
			$('#ec_echarts_edit_form').trigger('beforeSubmit');
			if($('#ec_echarts_edit_form input[name="operateType"]').val() == "submit"){
				var deploymentId=$('#ec_echarts_edit_form input[name="deploymentId"]');
				var buttonCode=$('#ec_echarts_edit_form input[name="buttonCode"]');
				var namespace=$('#ec_echarts_edit_form input[name="namespace"]');
				if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
					var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
					if(signatureInfo[0] != '') {
						var cancelItem = $('input[name="workFlowVarStatus"]');
						if(cancelItem.val() != "cancel") {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_echarts_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_echarts_edit_form','ec.flow.submit',false)});
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_echarts_edit_form');
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_echarts_edit_form','ec.edit.remove',false)});
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
							parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_echarts_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_echarts_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_echarts_edit_form',buttonCode.val(),false)});},2000);
							}	
						}
						else {
							signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_echarts_edit_form",false,'');
							if(signatureInfo[0] == 'singleSign') {
								$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_echarts_edit_form',buttonCode.val(),false)});
							}
							else {
								setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_echarts_edit_form',buttonCode.val(),false)});},2000);
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
});
</script>
