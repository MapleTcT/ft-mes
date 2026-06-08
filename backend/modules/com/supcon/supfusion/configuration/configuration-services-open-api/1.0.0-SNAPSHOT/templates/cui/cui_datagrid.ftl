<#-- ==================================== DataTable View ==================================== -->

<#macro datatable id,beforeInitApplyCookieData="",beforeUploadCookieString="",noPermissionKeys="",checkedRowsMap="{}", colAdminFlag=false ,noCookie=false,colMerge="",modelCode="",queryFunc="",beforePageNav="",treeView=false,postData="",onclick="",checkEditCondition="",formId="",firstLoad=true,exportExcel=false,dtPage="page",pageOrder="DESC",orderMode="frontstage",style="",dataUrl="",displayRowsCount=0,width=0,height=0,transMethod="POST",complex=true,editable=false,editableStrFlag="false",insertRowAble=false,superChecked=false,defaultNodeExpanded=true,superCheckedName="",superCheckedId="",hidekey="",dblclick="",renderOverEvent="",throwError="",onPropertyChange="",fbuttons="",buttons="",dataclassifics="",custombtns="",paginator=true,displayRowsCollapse=false,withoutConfigTable=false,multiselect=false,tfoot="",autoaddrow="",dgattribute="",dataTableType="",condition="",caption="",hidekeyPrefix="",noPadding=false,isEdit=false,tabViewIndex=0,maxListCount=0,pageInitMethod="",lockColumnCount="",cannotAddNewRow=false,lazyLoad=false,lazyUrl="",moreButtonResizeFlag=true,isExtra=false,isSuperTable=false,ptRealTimeLoad=1,isEnhancedExtenedViwe=false,helpInfo='',helpInfoConfig='',dataclassificCode='',dgTitle=''>
<#if colAdminFlag == false>
<#local colAdminFlag=checkUserPermisition('ec_ptManage_list_view')>
</#if>
<div id="${id}" class="cui-datagrid-js" style="margin:6px 10px 2px;clear:both;<#if style != "">${style}</#if>"></div>
<#assign checkedRowsMap = checkedRowsMap>
<script type="text/javascript">
	foundation.common.labelNo="${getText('foundation.party.num')}";
	var datatable_${id}, ${id}Widget;
	(function($){
		function addToButtons(arr1,arr2){
			if(arr2.length > 0) {
				for(var i=0;i<arr2.length;i++) {
					arr1[arr1.length] = arr2[i];
				}
			}
			return arr1;
		}
		var validatorBusinessPool = {};
		<#if colMerge != "">var aColMergeKeys = ${colMerge} || [];</#if>	
		var aRowMergeKeys = [];
		var checkedRowsMap = ${checkedRowsMap};
		var columnDefs = [];
		var buttonsAll = [];
		var buttons = [];
		var hidekeys = [];
		<#if hidekey != "">
			hidekeys = ${hidekey};
		</#if>
		<#assign enable_fields_permission_config = getEnableFieldsPermissionConfig(modelCode)>
		<#if enable_fields_permission_config = 1>
		<#if noPermissionKeys != "" && modelCode != "">
		<#assign noPermissionFieldsMap = getPermissionFieldsMap(modelCode,noPermissionKeys)>
		<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
			var noPermissionFields = {};
			<#list noPermissionFieldsMap?keys as mapKey>
				noPermissionFields["${mapKey}"] = "${noPermissionFieldsMap[mapKey]}"; 
			</#list>
		</#if>
		</#if>
		<#else>
		<#assign noPermissionFieldsMap = []>
		</#if>
		<#nested>
		if(buttons.length > 0) {
			
		}
		<#if buttons != "">
		var additionalButtons = ${buttons};
		buttonsAll = addToButtons(buttonsAll,additionalButtons);
		</#if>
		var buildDataTable = function(data){				
			<#-- 新版supdatagrid -->
			<#if isSuperTable>
			return supDataGrid.buildDataTable('${id}',
				{
					rowData: [data],
					columnDefs: columnDefs,
					<#if beforeInitApplyCookieData != "">beforeInitApplyCookieData: ${beforeInitApplyCookieData},</#if>
		  			<#if beforeUploadCookieString != "">beforeUploadCookieString: ${beforeUploadCookieString},</#if>
					rowMergeKeys: aRowMergeKeys,
					checkedRowsMap: checkedRowsMap,
					moreButtonResizeFlag : <#if moreButtonResizeFlag>true<#else>false</#if>,
					<#if colMerge != "">colMergeKeys: aColMergeKeys,</#if>
		  			<#if condition != "">ClassCondition: "${condition}",</#if>
		  			<#if renderOverEvent != "">renderOverEvent: "${renderOverEvent}",</#if>
		  			<#if throwError != "">throwError: "${throwError}",</#if>
		  			<#if onPropertyChange != "">onPropertyChange: "${onPropertyChange}",</#if>
		            <#if caption != "">caption: "${caption}",</#if>
		            <#if height gt 0>height: ${height},</#if>
		            <#if width gt 0>width:${width},</#if>
		            <#if displayRowsCount gt 0>displayRowsCount:${displayRowsCount},</#if>
		            <#if displayRowsCollapse>displayRowsCollapse:true,</#if>
		            <#if editable>editable:true,</#if>
		            <#if editableStrFlag=="true">editable:true,</#if>
		            <#if insertRowAble>insertRowAble:true,</#if>
					<#if superChecked>superChecked:true,</#if>
					<#if defaultNodeExpanded>defaultNodeExpanded:true,</#if>
					<#if superCheckedName != "">superCheckedName:"${superCheckedName}",</#if>
					<#if superCheckedId != "">superCheckedId:"${superCheckedId}",</#if>
		            <#if cannotAddNewRow>cannotAddNewRow:true,</#if>
		            <#if hidekey != "">hideKey:hidekeys,</#if>
		        	<#if lazyUrl != "">lazyUrl:"${lazyUrl}",</#if>
		            <#if lazyLoad >lazyLoad:true,</#if>
		            <#if dblclick != "">dblclick:"${dblclick}",</#if>
		            <#if onclick != "">onclick:"${onclick}",</#if>
		            <#if checkEditCondition != "">checkEditCondition:"${checkEditCondition}",</#if>
		            <#if fbuttons != "">fbuttons:${fbuttons},</#if>
		            <#if custombtns != "">custombtns:${custombtns},</#if>			            
		            <#if paginator>paginator:true,</#if>
		            <#if withoutConfigTable>withoutConfigTable:true,</#if>
		            <#if exportExcel>exportExcel:true,</#if>
		           	<#if colAdminFlag>colAdminFlag:true,</#if>
		            <#if multiselect>multiselect:true,</#if>
		            <#if tfoot!="">tfoot:${tfoot},</#if>
		            <#if !complex>complex:false,</#if>
		            <#if pageOrder!= "">pageOrder:"${pageOrder}",</#if>
		            <#if orderMode!= "">orderMode:"${orderMode}",</#if>
				    <#if autoaddrow != "">autoAddRow:${autoaddrow},</#if>
				    <#if dgattribute != "">dbAttribute:true,</#if>
				    <#if dtPage != "">dtPage:"${dtPage}",</#if>
				    <#if queryFunc != "">queryFunc:"${queryFunc}",</#if>
				    <#if beforePageNav != "">beforePageNav:"${beforePageNav}",</#if>
				    <#if formId != "">formId:"${formId}",</#if>
				    <#if dataUrl != "">dataUrl:"${dataUrl}",</#if>
				    <#if postData != "">postData:"${postData}",</#if>
		            <#if dataTableType != "">responseType:"${dataTableType}",</#if>
		            <#if hidekeyPrefix != "">hidekeyPrefix:"${hidekeyPrefix}",</#if>
		            <#if noPadding>noPadding:true,</#if>
		            <#if dataclassifics != "">dataclassifics:${dataclassifics},</#if>
					<#if maxListCount gt 0>maxListCount:${maxListCount},</#if>
					<#if noCookie>noCookie:true,</#if>
					<#if helpInfo != "">helpInfo:"${helpInfo?js_string}",</#if>
					<#if helpInfoConfig != "">helpInfoConfig:${helpInfoConfig},</#if>
					<#if dgTitle != "">dgTitle:"${dgTitle}",</#if>
					treeView:<#if treeView>true<#else>false</#if>,
		            buttons:buttonsAll,		           	
		           	<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
		            noPermissionFields:noPermissionFields,
		            </#if>
		            <#if isEnhancedExtenedViwe>isEnhancedExtenedViwe:true,</#if>
		            method:"${transMethod}",
		            <#if lockColumnCount != "">freezedColsNum: "${lockColumnCount}" </#if>
				}
			);
			<#else>
			return new CUI.DataTable("${id}",
					columnDefs, 
					[data], 
			  		{	
			  			<#if beforeInitApplyCookieData != "">beforeInitApplyCookieData: ${beforeInitApplyCookieData},</#if>
			  			<#if beforeUploadCookieString != "">beforeUploadCookieString: ${beforeUploadCookieString},</#if>
						rowMergeKeys: aRowMergeKeys,
						checkedRowsMap: checkedRowsMap,
						moreButtonResizeFlag : <#if moreButtonResizeFlag>true<#else>false</#if>,
						<#if colMerge != "">colMergeKeys: aColMergeKeys,</#if>
			  			<#if condition != "">ClassCondition: "${condition}",</#if>
			  			<#if renderOverEvent != "">renderOverEvent: "${renderOverEvent}",</#if>
			  			<#if throwError != "">throwError: "${throwError}",</#if>
			  			<#if onPropertyChange != "">onPropertyChange: "${onPropertyChange}",</#if>
			            <#if caption != "">caption: "${caption}",</#if>
			            <#if height gt 0>height: ${height},</#if>
			            <#if width gt 0>width:${width},</#if>
			            <#if displayRowsCount gt 0>displayRowsCount:${displayRowsCount},</#if>
			            <#if displayRowsCollapse>displayRowsCollapse:true,</#if>
			            <#if editable>editable:true,</#if>
			            <#if editableStrFlag=="true">editable:true,</#if>
			            <#if insertRowAble>insertRowAble:true,</#if>
						<#if superChecked>superChecked:true,</#if>
						<#if defaultNodeExpanded>defaultNodeExpanded:true,</#if>
						<#if superCheckedName != "">superCheckedName:"${superCheckedName}",</#if>
						<#if superCheckedId != "">superCheckedId:"${superCheckedId}",</#if>
			            <#if cannotAddNewRow>cannotAddNewRow:true,</#if>
			            <#if hidekey != "">hideKey:hidekeys,</#if>
			        	<#if lazyUrl != "">lazyUrl:"${lazyUrl}",</#if>
			            <#if lazyLoad >lazyLoad:true,</#if>
			            <#if dblclick != "">dblclick:"${dblclick}",</#if>
			            <#if onclick != "">onclick:"${onclick}",</#if>
			            <#if checkEditCondition != "">checkEditCondition:"${checkEditCondition}",</#if>
			            <#if fbuttons != "">fbuttons:${fbuttons},</#if>
			            <#if custombtns != "">custombtns:${custombtns},</#if>			            
			            <#if paginator>paginator:true,</#if>
			            <#if withoutConfigTable>withoutConfigTable:true,</#if>
			            <#if exportExcel>exportExcel:true,</#if>
			           	<#if colAdminFlag>colAdminFlag:true,</#if>
			            <#if multiselect>multiselect:true,</#if>
			            <#if tfoot!="">tfoot:${tfoot},</#if>
			            <#if !complex>complex:false,</#if>
			            <#if pageOrder!= "">pageOrder:"${pageOrder}",</#if>
			            <#if orderMode!= "">orderMode:"${orderMode}",</#if>
					    <#if autoaddrow != "">autoAddRow:${autoaddrow},</#if>
					    <#if dgattribute != "">dbAttribute:true,</#if>
					    <#if dtPage != "">dtPage:"${dtPage}",</#if>
					    <#if queryFunc != "">queryFunc:"${queryFunc}",</#if>
					    <#if beforePageNav != "">beforePageNav:"${beforePageNav}",</#if>
					    <#if formId != "">formId:"${formId}",</#if>
					    <#if dataUrl != "">dataUrl:"${dataUrl}",</#if>
					    <#if postData != "">postData:"${postData}",</#if>
			            <#if dataTableType != "">responseType:"${dataTableType}",</#if>
			            <#if hidekeyPrefix != "">hidekeyPrefix:"${hidekeyPrefix}",</#if>
			            <#if noPadding>noPadding:true,</#if>
			            <#if dataclassifics != "">dataclassifics:${dataclassifics},</#if>
						<#if maxListCount gt 0>maxListCount:${maxListCount},</#if>
						<#if noCookie>noCookie:true,</#if>
						<#if helpInfo != "">helpInfo:"${helpInfo?js_string}",</#if>
						<#if helpInfoConfig != "">helpInfoConfig:${helpInfoConfig},</#if>
						<#if dgTitle != "">dgTitle:"${dgTitle}",</#if>
						treeView:<#if treeView>true<#else>false</#if>,
			            buttons:buttonsAll,		           	
			           	<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
			            noPermissionFields:noPermissionFields,
			            </#if>
			            <#if isEnhancedExtenedViwe>isEnhancedExtenedViwe:true,</#if>
			            method:"${transMethod}"
			        },
			        "${lockColumnCount}");			
			</#if>
		};
		YAHOO.util.Event.onDOMReady(function() {
			<#assign timeouts = 0 />
			<#if (isEdit && tabViewIndex gt 0) || ptRealTimeLoad==0 >
				<#assign timeouts = 200 />
				$('#${id}').parents('div.edit-panes').first().parent().find('ul.edit-tabs li').eq(${tabViewIndex}).one("click", function(event){
			</#if>
			<#if firstLoad && dataUrl != "">
				setTimeout(function(){
					${id}Widget = datatable_${id} = buildDataTable({result:[],pageNo:1,pageSize:20,totalCount:0,totalPages:0});
					<#if dataclassificCode?? && dataclassificCode != ''>
					<#if ptRealTimeLoad?? && ptRealTimeLoad !=0>
					if($('[id = "${dataclassificCode?replace('dg[0-9]*_', '', 'r')}_queryForm_data_classify"]').size()>0){
						$('[id = "${dataclassificCode?replace('dg[0-9]*_', '', 'r')}_queryForm_data_classify"]').trigger('click');
						$('[id = "${dataclassificCode?replace('dg[0-9]*_', '', 'r')}_queryForm_data_classify"]').find('[id $= "${dataclassificCode?replace('dg[0-9]*_', '', 'r')}_queryForm_confirm"]').trigger('click');
					}else{
						${id}Widget.setRequestDataUrl("${dataUrl}","${postData}");
					}
					<#else>
					${id}Widget.setRequestDataUrl("${dataUrl}","${postData}");
					</#if>
					<#else>
					if($('#${formId}_data_classify').size()>0){
						$('#${formId}_data_classify').trigger('click');
						$('#${formId}_data_classify').find('#${formId}_confirm').trigger('click');
					}else{
						${id}Widget.setRequestDataUrl("${dataUrl}","${postData}");
					}
					</#if>
					<#if pageInitMethod?has_content>${pageInitMethod!}();</#if>
				},${timeouts});
			<#else>
				setTimeout(function(){${id}Widget = datatable_${id} = buildDataTable({result:[],pageNo:1,pageSize:20,totalCount:0,totalPages:0});<#if pageInitMethod?has_content>${pageInitMethod!}();</#if>},${timeouts});
			</#if>
			<#if (isEdit && tabViewIndex gt 0) || (isExtra && ptRealTimeLoad==0) >		
				});
			</#if>
		});	
	})(jQuery);
	</script>
</#macro>


<#-- =======DataGrid Plugin ======== -->
<#macro datagrid noCookie=false,beforeInitApplyCookieData="",beforeUploadCookieString="",moreButtonResizeFlag=true,dataGridTableConfig=true,noEditQuery=false, colAdminFlag=false, route="0",id="",dataUrl="",usedFor="edit",customSetHeight="",viewType="edit",noPermissionKeys="",checkedRowsMap="{}",colMerge="",modelCode="",queryFunc="",treeView=false,displayRowsCollapse=false,allReadOnly=false,withOrderNumber=false,cannotDeleteRow=false,postData="",dataGridName="",onclick="",checkEditCondition="",formId="",firstLoad=true,exportExcel=false,dtPage="page",pageOrder="DESC",orderMode="frontstage",style="",displayRowsCount=0,width=0,height=0,transMethod="POST",complex=true,editable=false,insertRowAble=false,superChecked=false,defaultNodeExpanded=true,superCheckedName="",superCheckedId="",withoutConfigTable=false,hidekey="",dblclick="",renderOverEvent="",throwError="",onPropertyChange="",fbuttons="",buttons="",dataclassifics="",custombtns="",paginator=true,multiselect=false,tfoot="",autoaddrow="",dgattribute="",dataTableType="",condition="",caption="",formId="",hidekeyPrefix="",noPadding=false,isEdit=false,tabViewIndex=0,pageInitMethod="",cannotAddNewRow=false,canImportExcel=false,assModelDisplayName="",dgTitle="",activexButton="",lockColumnCount="",exportExcel=false,autoAddNewRow=false,addNewRow=true,canInsertRow=false,defaultRowCount=0,defaultCellValues="",isNew=false,changeProps="",copyRow=false,copyColumn=false,isSuperTable=false,isExtra=false,ptRealTimeLoad=0,defaultTextareaRows=0,defaultRows=0,maxTextareaRows=0,maxTableHeight=0,widthType=0 >
<#if colAdminFlag == false>
<#local colAdminFlag=checkUserPermisition('ec_ptManage_list_view')>
</#if>
	<#if route == "1">
		<div id="${id!''}_wrapper" class="datagrid-wrapper" style="margin-top:6px;position:relative;clear:both;<#if style != "">${style}</#if>">
			<div class="paginatorbar">
				<div class="paginatorbar-lc"></div>
				<div class="paginatorbar-rc"></div>
				<#if dgTitle?? && dgTitle?length gt 0>
				<div class="paginatorbar-pageinfo-left">${dgTitle!}</div>
				<div class="paginatorbar-pageinfo-left"><span class="headsplit"></span></div>
				</#if>
				<#if allReadOnly == true && ((usedFor == 'list' && !treeView) || usedFor == 'edit')>
				<div class="paginatorbar-pageinfo">
					<span class="dg_firstPage firstPageDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
					<span class="dg_prevlink prevlinkDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
					<span class="dg_nextlink nextlinkDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
					<span class="dg_lastlink lastlinkDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
				</div>
				</#if>
				<div class="paginatorbar-buttonbar">
				 	<#if colAdminFlag?? && colAdminFlag>
						<span class="configtable" title="管理员列配置" onclick="alert(1);"></span>
					</#if>
					<#if exportExcel?? && exportExcel>
						<span class="excel" title="导出EXCEL" onclick="CUI.ptExportExcel('${formId}', '${id}', '${postData}')"></span>
						<#if allReadOnly == true && ((usedFor == 'list' && !treeView) || usedFor == 'edit')>
						<span class="headsplit"></span>
						</#if>
					</#if>
				</div>
				<#if editable>
				<div class="paginatorbar-operatebar">
					<#if addNewRow == true >
					<a href="#"><span class="buttonbar-button cui-btn-add" onclick="var _DT = ${id}Widget._DT; if ( _DT.displayExpand && ( _DT._oGrid.RowLength + 1 ) > _DT.opts.displayRowsCount ) { _DT._oGrid.height = ( _DT._oGrid.RowLength + 1 ) * 18 + 21 + _DT._paddingHeight; } ; setTimeout( function(){ ${id}Widget.addRow(); }, 50 );">${getHtmlText('foundation.staff.addRow')}</span></a>
					</#if>
					<#if cannotDeleteRow == false>
					<a href="#"><span class="buttonbar-button cui-btn-del" onclick="${id}Widget_delRow();">${getHtmlText('foundation.staff.deleteRow')}</span></a>
					</#if>
					<#if activexButton!= "">
						<#list activexButton?split(";") as button>
						<#if button?split(',')[1]!="">
							<#if button?split(',')[1]=="ADDROW">
								<a href="#"><span class="buttonbar-button cui-btn-add" onclick="var _DT = ${id}Widget._DT; if ( _DT.displayExpand && ( _DT._oGrid.RowLength + 1 ) > _DT.opts.displayRowsCount ) { _DT._oGrid.height = ( _DT._oGrid.RowLength + 1 ) * 18 + 21 + _DT._paddingHeight; } ; setTimeout( function(){ ${id}Widget.addRow(); }, 50 );">${getHtmlText('foundation.staff.addRow')}</span></a>
							<#elseif button?split(',')[1]=="DELETEROW">
								<a href="#"><span class="buttonbar-button cui-btn-del" onclick="${id}Widget_delRow();">${getHtmlText('foundation.staff.deleteRow')}</span></a>
							<#elseif button?split(',')[1]=="INSERTROW">
								<a href="#"><span class="buttonbar-button cui-btn-insert" onclick="var _DT = ${id}Widget._DT; if ( _DT.displayExpand && ( _DT._oGrid.RowLength + 1 ) > _DT.opts.displayRowsCount ) { _DT._oGrid.height = ( _DT._oGrid.RowLength + 1 ) * 18 + 21 + _DT._paddingHeight; } ; setTimeout( function(){ ${id}Widget.appendRow(); }, 50 );">${getHtmlText('ec.view.button.insertRow')}</span></a>
								<a href="#"><span class="buttonbar-button cui-btn-up" onclick="${id}Widget.moveRow('up');">${getHtmlText('ec.view.button.moveRowUp')}</span></a>
								<a href="#"><span class="buttonbar-button cui-btn-down" onclick="${id}Widget.moveRow('down');">${getHtmlText('ec.view.button.moveRowDown')}</span></a>
							<#else>
								<a href="#"><span class="buttonbar-button cui-btn-${button?split(',')[2]!}" onclick="${id}_${button?split(',')[1]}">${getHtmlText("${button?split(',')[0]}")}</span></a>
							</#if>
						</#if>
					
						</#list>
					</#if>
					<#if canInsertRow == true>
					<a href="#"><span class="buttonbar-button cui-btn-insert" onclick="var _DT = ${id}Widget._DT; if ( _DT.displayExpand && ( _DT._oGrid.RowLength + 1 ) > _DT.opts.displayRowsCount ) { _DT._oGrid.height = ( _DT._oGrid.RowLength + 1 ) * 18 + 21 + _DT._paddingHeight; } ; setTimeout( function(){ ${id}Widget.appendRow(); }, 50 );">${getHtmlText('ec.view.button.insertRow')}</span></a>
					<a href="#"><span class="buttonbar-button cui-btn-up" onclick="${id}Widget.moveRow('up');">${getHtmlText('ec.view.button.moveRowUp')}</span></a>
					<a href="#"><span class="buttonbar-button cui-btn-down" onclick="${id}Widget.moveRow('down');">${getHtmlText('ec.view.button.moveRowDown')}</span></a>
					
					</#if>
					<#if canImportExcel == true>
					<a href="#"><span class="buttonbar-button cui-btn-add" onclick="${id}_showImportDialog()">${getHtmlText('foundation.company.staff.import')}Excel</span></a>
					<a class="buttonbar-morebtn" id="${id}_showMenuA" style="cursor:pointer;"><span></span></a>
					</#if>
				</div>
				</#if>
				<#if displayRowsCount gt 0>
					<div class="paginatorbar-operatebar-right" style="float: right; height: 23px; line-height: 20px;margin-top: 4px;">
						<a style="display:inline-block; margin: 2px 1px 2px 10px;padding: 0 1px;text-decoration: none;" href="###" ><span onclick="${id}Widget._DT.toggleHeight();if( ${id}Widget._DT.displayExpand ){ $(this).removeClass( 'cui-btn-expand' ).addClass( 'cui-btn-collapse' ).html( '收起' ) } else { $(this).removeClass( 'cui-btn-collapse' ).addClass( 'cui-btn-expand' ).html( '展开' ) } " style="color: black;cursor: pointer; display: inline-block;float: left;height: 18px;line-height: 18px; padding-left: 18px;position: relative;" class="buttonbar-button <#if displayRowsCollapse>cui-btn-expand<#else>cui-btn-collapse</#if>"><#if displayRowsCollapse>展开<#else>收起</#if></span></a>
					</div>
				</#if>
				
				
				<div class="paginatorbar-operatebar datagrid-loading"  style="display:none;background-position-y:5px;">${getHtmlText('foundation.common.data.waiting')}</div>
			</div>
			<#if canImportExcel == true>
			<div id="${id}_excelMenu" class="menu-box" style="position:absolute;top:24px;left:0px;display:block;z-index:4;display:none;">
				<ul class="white-corner">
					<li onclick="${id}_downLoadFile()">${getHtmlText('foundation.datagrid.downloadTemplate')}</li>
				</ul>
			</div>
			<iframe id="${id}_menubox" style="position:absolute;top:24px;left:0px;display:block;scrolling:no;height:36px;width:158px;z-index:3;display:none;" frameborder="0"></iframe>
			</#if>
			<div id="${id}"></div>
			
			<#if allReadOnly == true && ((usedFor == 'list' && !treeView) || usedFor == 'edit') && false>
			<div class="paginatorbar no-bt">
				<div class="paginatorbar-lc"></div>
				<div class="paginatorbar-rc"></div>
				<div class="paginatorbar-pageinfo">
					<span class="floatspan" style="padding-right: 5px; ">${getHtmlText('foundation.common.total')}<span class="dg_totalCount">0</span>${getHtmlText('foundation.common.tiao')}</span>
					<span class="floatspan" style="padding-right: 15px; ">
						<select class="PageLink-PageSelect" style="width:40px;font-size: 7.8pt; font-family: verdana;"></select>
						<span>/<span class="dg_totalPages">0</span> ${getHtmlText('foundation.common.ye')}</span>
					</span>
					<span class="dg_firstPage firstPageDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
					<span class="dg_prevlink prevlinkDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
					<span class="dg_nextlink nextlinkDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
					<span class="dg_lastlink lastlinkDisabled floatspan">&nbsp;&nbsp;&nbsp;</span>
				</div>
			</div>
			</#if>
			
		</div>
		<script type="text/javascript">
			var datatable_${id}, ${id}Widget, ${id}Widget${randomNum!};
			YAHOO.util.Event.onDOMReady(function(){
				<#assign timeouts = 0 />
				<#assign editIndex = formId?last_index_of('_')>
				<#assign editKey = formId?substring(0,editIndex)?replace('_','.')>
			    <#if (!isExtra && isEdit && tabViewIndex gt 0 && ptRealTimeLoad == 0) || (isExtra && ptRealTimeLoad == 0)>
				    <#assign timeouts = 200 />
				    $('#${id}').parents('div.edit-panes').first().parent().find('ul.edit-tabs li').eq(${tabViewIndex}).one("click", function(event){
			    </#if>
			    setTimeout(function(){
			    	${id}Widget = ${id}Widget${randomNum!} = datatable_${id} = new CUI.DataGrid({
			      		route:1,
			      		lockColumnCount:"${lockColumnCount}",
						sContainerId:"${id}",
			      		oConfigs:{
							<#if editable>editable:true,</#if>
							<#if dataGridTableConfig != false>dataGridTableConfig: true,</#if>
							<#if insertRowAble>insertRowAble:true,</#if>
							<#if viewType != "">viewType:"${viewType}",</#if>
							<#if renderOverEvent != "">renderOverEvent: "${renderOverEvent}",</#if>
							<#if throwError != "">throwError: "${throwError}",</#if>
			         		<#if customSetHeight != "">customSetHeight:"${customSetHeight}",</#if>
			         		<#if queryFunc != "">queryFunc:"${queryFunc}",</#if>
			         		<#if assModelDisplayName != "">assModelDisplayName : "${assModelDisplayName?js_string}",</#if>
			         		<#if height gt 0>height: ${height},</#if>
							<#if width gt 0>width:${width},</#if>
							<#if displayRowsCount gt 0>displayRowsCount:${displayRowsCount},</#if>
							<#if displayRowsCollapse>displayRowsCollapse:true,</#if>
							<#if cannotAddNewRow>cannotAddNewRow:true,</#if>
							firstLoad:<#if firstLoad>true<#else>false</#if>,
			         		url : "${dataUrl}${postData}"
			         	}     
			     	}) 
			     	<#if (!isExtra && isEdit && tabViewIndex gt 0 && ptRealTimeLoad == 0) || (isExtra && ptRealTimeLoad == 0)>
			     	${editKey}.initSize(${tabViewIndex});
				    </#if> 
				    <#if pageInitMethod?has_content>${pageInitMethod!}(<#if isEdit && tabViewIndex gt 0 && ptRealTimeLoad=0>${tabViewIndex}</#if>);</#if>
			    },${timeouts});
			    <#if canImportExcel == true>
			    setTimeout(function(){
				    if($('#${id}_menubox')){
				    	var div_position = 0;
				    	$(".paginatorbar-pageinfo-left", '#${id!''}_wrapper').each(function(){
		            			div_position += parseInt($(this).css("width"),10);
		            	});
		            	$(".paginatorbar-operatebar", '#${id!''}_wrapper').each(function(){
	            			if($(this).is(":visible")){
	            				div_position += parseInt($(this).css("width"),10);
	            			}
		            	});
		            	div_position -= 140;
		            	$('#${id}_menubox').css("left", (div_position-10)+"px");
		            	$('#${id}_excelMenu').css("left", (div_position-10)+"px");
				    }
				},${timeouts});
				</#if>   
			    <#if  (!isExtra && isEdit && tabViewIndex gt 0 && ptRealTimeLoad == 0) || (isExtra && ptRealTimeLoad == 0)>
			     });
			    </#if>
			});
			function ${id}Widget_delRow(){
				var ids = ${id}Widget.deleteRows();
				var idsArr = ids.split(",");
				for(var i = 0 ; i < idsArr.length; i++){
					if(idsArr[i]){
						$('<input type="hidden" name="${dataGridName}DeletedIds[' + $('input[name^="${dataGridName}DeletedIds"]').length + ']' + '" value="' + idsArr[i] + '">').appendTo($('#${formId!}'));
						var totalCount = $(".dg_totalCount",$("#${id}").parent());
						totalCount.text((parseInt(totalCount.text()) - 1));
					}
				}
			}
			<#if canImportExcel == true>
			$('#${id}_showMenuA').click(function(e){
				if (e){ // 停止事件冒泡
					e.stopPropagation();
				}else{
					window.event.cancelBubble = true;
				}
				$('#${id}_menubox').show();
				$('#${id}_excelMenu').show();
			});
			
			$('body').click(function(){
				$("#${id}_menubox").hide();
				$('#${id}_excelMenu').hide();
			});
			</#if>
		</script>
		
	<#elseif route == "2">
		
		<div id="${id}" class="cui-easytable" style="clear:both;<#if style != "">${style}</#if>"></div>
		<#assign checkedRowsMap = checkedRowsMap>
		<script type="text/javascript">
			foundation.common.labelNo="${getText('foundation.party.num')}";
			var datatable_${id}, ${id}Widget, ${id}Widget${randomNum!};
			(function($){
				function addToButtons(arr1,arr2){
					if(arr2.length > 0) {
						for(var i=0;i<arr2.length;i++) {
							arr1[arr1.length] = arr2[i];
						}
					}
					return arr1;
				}
				var validatorBusinessPool = {};
				<#if colMerge != "">var aColMergeKeys = ${colMerge} || [];</#if>	
				var aRowMergeKeys = [];
				var checkedRowsMap = ${checkedRowsMap};
				var columnDefs = [];
				var buttonsAll = [];
				var buttons = [];
				var hidekeys = [];
				<#if hidekey != "">
					hidekeys = ${hidekey};
				</#if>
				<#assign enable_fields_permission_config = getEnableFieldsPermissionConfig(modelCode)>
				<#if enable_fields_permission_config = 1>
				<#if noPermissionKeys != "" && modelCode != "">
				<#assign noPermissionFieldsMap = getPermissionFieldsMap(modelCode,noPermissionKeys)>
				<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
					var noPermissionFields = {};
					<#list noPermissionFieldsMap?keys as mapKey>
						noPermissionFields["${mapKey}"] = "${noPermissionFieldsMap[mapKey]}"; 
					</#list>
				</#if>
				</#if>
				<#else>
					<#assign noPermissionFieldsMap = []>
				</#if>
				<#nested>
				if(buttons.length > 0) {
					
				}
				<#if buttons != "">
				var additionalButtons = ${buttons};
				buttonsAll = addToButtons(buttonsAll,additionalButtons);
				</#if>
				
				<#if copyRow >
					var copyRowBtn=[{text:"${getText('foundation.party.copyRow')}",handler:function(event){${id}Widget.copyAddNewRow();},iconClass:"cui-btn-copyrow",useInMore:"false"}]
					buttonsAll = addToButtons(buttonsAll, copyRowBtn);
				</#if>
				<#if copyColumn >
				var copyColumnBtn=[{text:"${getText('foundation.party.copyColumnFullCoverage')}", handler:function(){${id}Widget.copyAllColumn(false);},iconClass:"cui-btn-copycolumn",useInMore:"false",subButtons:[
						{text:"${getText('foundation.party.copyColumnSkipExistingValues')}", handler:function(){${id}Widget.copyAllColumn(true);},iconClass:"cui-btn-add",useInMore:"false"}
						]}]
				buttonsAll = addToButtons(buttonsAll, copyColumnBtn);
				</#if>
				
				var buildDataTable = function(data){
					return new CUI.Easytable( "${id}",
								columnDefs,
								[data],
								{	
									validatorConfig: validatorBusinessPool,
									rowMergeKeys: aRowMergeKeys,
									checkedRowsMap: checkedRowsMap,
									<#if colMerge != "">colMergeKeys: aColMergeKeys,</#if>
									<#if condition != "">ClassCondition: "${condition}",</#if>
									<#if renderOverEvent != "">renderOverEvent: "${renderOverEvent}",</#if>
									<#if throwError != "">throwError: "${throwError}",</#if>
									<#if onPropertyChange != "">onPropertyChange: "${onPropertyChange}",</#if>
									<#if caption != "">caption: "${caption}",</#if>
									<#if height gt 0>height: ${height},</#if>
									<#if width gt 0>width:${width},</#if>
									<#if displayRowsCount gt 0>displayRowsCount:${displayRowsCount},</#if>
									<#if defaultTextareaRows gt 0>defaultTextareaRows:${defaultTextareaRows},</#if>
									<#if defaultRows gt 0>defaultRows:${defaultRows},</#if>
									<#if maxTextareaRows gt 0>maxTextareaRows:${maxTextareaRows},</#if>
									<#if maxTableHeight gt 0>maxTableHeight:${maxTableHeight},</#if>
									<#if widthType gte 0>widthType:${widthType},</#if>
									<#if displayRowsCollapse>displayRowsCollapse:true,</#if>
									<#if editable>editable:true,</#if>
									<#if withOrderNumber>withOrderNumber:true,</#if>
									<#if insertRowAble>insertRowAble:true,</#if>
									<#if superChecked>superChecked:true,</#if>
									<#if defaultNodeExpanded>defaultNodeExpanded:true,</#if>
									<#if superCheckedName != "">superCheckedName:"${superCheckedName}",</#if>
									<#if superCheckedId != "">superCheckedId:"${superCheckedId}",</#if>
									<#if cannotAddNewRow>cannotAddNewRow:true,</#if>
									<#if withoutConfigTable>withoutConfigTable:true,</#if>
									hideKey:hidekeys,
									<#if dblclick != "">dblclick:"${dblclick}",</#if>
									<#if onclick != "">onclick:"${onclick}",</#if>
									<#if checkEditCondition != "">checkEditCondition:"${checkEditCondition}",</#if>
									<#if fbuttons != "">fbuttons:${fbuttons},</#if>
									<#if custombtns != "">custombtns:${custombtns},</#if>			            
									<#if paginator>paginator:true,</#if>
									<#if exportExcel>exportExcel:true,</#if>
									<#if multiselect>multiselect:true,</#if>
									<#if viewType != "">viewType:"${viewType}",</#if>
									<#if isNew>isNew:true,</#if>
									<#if defaultCellValues != "">defaultCellValues:${defaultCellValues},</#if>
									
									<#if tfoot!="">tfoot:${tfoot},</#if>
									<#if !complex>complex:false,</#if>
									<#if pageOrder!= "">pageOrder:"${pageOrder}",</#if>
									<#if orderMode!= "">orderMode:"${orderMode}",</#if>
									<#if autoaddrow != "">autoAddRow:${autoaddrow},</#if>
									<#if dgattribute != "">dbAttribute:true,</#if>
									<#if dtPage != "">dtPage:"${dtPage}",</#if>
									<#if queryFunc != "">queryFunc:"${queryFunc}",</#if>
									<#if formId != "">formId:"${formId}",</#if>
									<#if dataUrl != "">dataUrl:"${dataUrl}<#if dataUrl?index_of('?') gt 0>&<#else>?</#if>rt=json",</#if>
									<#if dataTableType != "">responseType:"${dataTableType}",</#if>
									<#if hidekeyPrefix != "">hidekeyPrefix:"${hidekeyPrefix}",</#if>
									<#if noPadding>noPadding:true,</#if>
									<#if dataclassifics != "">dataclassifics:${dataclassifics},</#if>
									<#if exportExcel>exportExcel:true,</#if>
									postData:"${postData}",
									firstLoad:<#if firstLoad>true<#else>false</#if>,
									buttons:buttonsAll,
									treeView:<#if treeView>true<#else>false</#if>,
									<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
						            noPermissionFields:noPermissionFields,
						            </#if>
			         				<#if assModelDisplayName != "">assModelDisplayName : "${assModelDisplayName?js_string}",</#if>
			         				<#if dgTitle != "">dgTitle : "${dgTitle}",</#if>
			         				<#if changeProps != "">changeProps : "${changeProps}",</#if>
									method:"${transMethod}"
									
								});
				};
				
				YAHOO.util.Event.onDOMReady(function() {
					<#assign timeouts = 0 />
					<#if  (!isExtra && isEdit && tabViewIndex gt 0 && ptRealTimeLoad == 0) || (isExtra && ptRealTimeLoad == 0)>
						<#assign timeouts = 200 />
						$('#${id}').parents('div.edit-panes').first().parent().find('ul.edit-tabs li').eq(${tabViewIndex}).one("click", function(event){
					</#if>
					<#if firstLoad && dataUrl != "">
						setTimeout(function(){
							${id}Widget = ${id}Widget${randomNum!} = datatable_${id} = buildDataTable({result:[],pageNo:1,pageSize:20,totalCount:0,totalPages:0});
							<#if !isExtra && pageInitMethod?has_content>${pageInitMethod!}(<#if isEdit && tabViewIndex gt 0>${tabViewIndex}</#if>);</#if>
						},${timeouts});
					<#else>
						setTimeout(function(){${id}Widget = ${id}Widget${randomNum!}= datatable_${id} = buildDataTable({result:[],pageNo:1,pageSize:20,totalCount:0,totalPages:0});<#if pageInitMethod?has_content>${pageInitMethod!}();</#if>},${timeouts});
					</#if>
					<#if (!isExtra && isEdit && tabViewIndex gt 0 && ptRealTimeLoad == 0) || (isExtra && ptRealTimeLoad == 0)>		
						});
					</#if>
				});	
			})(jQuery);
		</script>
		
		<input type="hidden" name="${id}_DELROWS"/>
		
		
		
		
	<#else>	
		<div id="${id}" class="cui-datagrid-js" style="margin-top:6px;<#if isExtra?? && isExtra>margin-left:13px;</#if>clear:both;<#if style != "">${style}</#if>"></div>
		<#assign checkedRowsMap = checkedRowsMap>
		<script type="text/javascript">
			foundation.common.labelNo="${getText('foundation.party.num')}";
			var datatable_${id}, ${id}Widget, ${id}Widget${randomNum!};
			(function($){
				function addToButtons(arr1,arr2){
					if(arr2.length > 0) {
						for(var i=0;i<arr2.length;i++) {
							arr1[arr1.length] = arr2[i];
						}
					}
					return arr1;
				}
				var validatorBusinessPool = {};
				<#if colMerge != "">var aColMergeKeys = ${colMerge} || [];</#if>	
				var aRowMergeKeys = [];
				var checkedRowsMap = ${checkedRowsMap};
				var columnDefs = [];
				var buttonsAll = [];
				var buttons = [];
				var hidekeys = [];
				<#if hidekey != "">
					hidekeys = ${hidekey};
				</#if>
				<#assign enable_fields_permission_config = getEnableFieldsPermissionConfig(modelCode)>
				<#if enable_fields_permission_config = 1>
				<#if noPermissionKeys != "" && modelCode != "">
				<#assign noPermissionFieldsMap = getPermissionFieldsMap(modelCode,noPermissionKeys)>
				<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
					var noPermissionFields = {};
					<#list noPermissionFieldsMap?keys as mapKey>
						noPermissionFields["${mapKey}"] = "${noPermissionFieldsMap[mapKey]}"; 
					</#list>
				</#if>
				</#if>
				<#else>
					<#assign noPermissionFieldsMap = []>
				</#if>
				<#nested>
				if(buttons.length > 0) {
					
				}
				<#if buttons != "">
				var additionalButtons = ${buttons};
				buttonsAll = addToButtons(buttonsAll,additionalButtons);
					<#if copyRow >
					var copyRowBtn=[{text:"${getText('foundation.party.copyRow')}",handler:function(event){${id}Widget.copyAddNewRow();},iconClass:"cui-btn-copyrow",useInMore:"false"}]
					buttonsAll = addToButtons(buttonsAll, copyRowBtn);
					</#if>
					<#if copyColumn >
					var copyColumnBtn=[{text:"${getText('foundation.party.copyColumnFullCoverage')}", handler:function(){${id}Widget.copyAllColumn(false);},iconClass:"cui-btn-copycolumn",useInMore:"false",subButtons:[
							{text:"${getText('foundation.party.copyColumnSkipExistingValues')}", handler:function(){${id}Widget.copyAllColumn(true);},iconClass:"cui-btn-add",useInMore:"false"}
							]}]
					buttonsAll = addToButtons(buttonsAll, copyColumnBtn);
					</#if>
				</#if>
				
				var buildDataTable = function(data){
					<#if isSuperTable>
					return supDataGrid.buildDataGrid('${id}',
						{
							columnDefs:columnDefs,
							rowData:[data],
							validatorConfig: validatorBusinessPool,
							<#if beforeInitApplyCookieData != "">beforeInitApplyCookieData: ${beforeInitApplyCookieData},</#if>
							<#if beforeUploadCookieString != "">beforeUploadCookieString: ${beforeUploadCookieString},</#if>
							moreButtonResizeFlag : <#if moreButtonResizeFlag>true<#else>false</#if>,
							rowMergeKeys: aRowMergeKeys,
							checkedRowsMap: checkedRowsMap,
							<#if dataGridTableConfig != false>dataGridTableConfig: true,</#if>
							<#if noEditQuery != false>noEditQuery: true,</#if>
							<#if colMerge != "">colMergeKeys: aColMergeKeys,</#if>
							<#if condition != "">ClassCondition: "${condition}",</#if>
							<#if renderOverEvent != "">renderOverEvent: "${renderOverEvent}",</#if>
							<#if throwError != "">throwError: "${throwError}",</#if>
							<#if onPropertyChange != "">onPropertyChange: "${onPropertyChange}",</#if>
							<#if caption != "">caption: "${caption}",</#if>
							<#if height gt 0>height: ${height},</#if>
							<#if width gt 0>width:${width},</#if>
							<#if displayRowsCount gt 0>displayRowsCount:${displayRowsCount},</#if>
							<#if displayRowsCollapse>displayRowsCollapse:true,</#if>
							<#if editable>editable:true,</#if>
							<#if insertRowAble>insertRowAble:true,</#if>
							<#if superChecked>superChecked:true,</#if>
							<#if defaultNodeExpanded>defaultNodeExpanded:true,</#if>
							<#if superCheckedName != "">superCheckedName:"${superCheckedName}",</#if>
							<#if superCheckedId != "">superCheckedId:"${superCheckedId}",</#if>
							<#if cannotAddNewRow>cannotAddNewRow:true,</#if>
							<#if withoutConfigTable>withoutConfigTable:true,</#if>
							hideKey:hidekeys,
							<#if dblclick != "">dblclick:"${dblclick}",</#if>
							<#if onclick != "">onclick:"${onclick}",</#if>
							<#if checkEditCondition != "">checkEditCondition:"${checkEditCondition}",</#if>
							<#if fbuttons != "">fbuttons:${fbuttons},</#if>
							<#if custombtns != "">custombtns:${custombtns},</#if>			            
							<#if paginator>paginator:true,</#if>
							<#if exportExcel>exportExcel:true,</#if>
							<#if multiselect>multiselect:true,</#if>
							<#if tfoot!="">tfoot:${tfoot},</#if>
							<#if !complex>complex:false,</#if>
							<#if pageOrder!= "">pageOrder:"${pageOrder}",</#if>
							<#if orderMode!= "">orderMode:"${orderMode}",</#if>
							<#if autoaddrow != "">autoAddRow:${autoaddrow},</#if>
							<#if dgattribute != "">dbAttribute:true,</#if>
							<#if dtPage != "">dtPage:"${dtPage}",</#if>
							<#if queryFunc != "">queryFunc:"${queryFunc}",</#if>
							<#if formId != "">formId:"${formId}",</#if>
							<#if dataUrl != "">dataUrl:"${dataUrl}<#if dataUrl?index_of('?') gt 0>&<#else>?</#if>rt=json",</#if>
							<#if dataTableType != "">responseType:"${dataTableType}",</#if>
							<#if hidekeyPrefix != "">hidekeyPrefix:"${hidekeyPrefix}",</#if>
							<#if noPadding>noPadding:true,</#if>
							<#if dataclassifics != "">dataclassifics:${dataclassifics},</#if>
							<#if exportExcel>exportExcel:true,</#if>
							<#if copyRow>copyRow:true,</#if>
							<#if copyColumn>copyColumn:true,</#if>
							postData:"${postData}",
							firstLoad:<#if firstLoad>true<#else>false</#if>,
							buttons:buttonsAll,
							treeView:<#if treeView>true<#else>false</#if>,							
							<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
				            noPermissionFields:noPermissionFields,
				            </#if>			            
	         				<#if assModelDisplayName != "">assModelDisplayName : "${assModelDisplayName?js_string}",</#if>
	         				<#if dgTitle != "">dgTitle : "${dgTitle}",</#if>
	         				<#if noCookie>noCookie:true,</#if>
							<#if colAdminFlag>colAdminFlag:true,</#if>			         				
							method:"${transMethod}"
						}
					);
					<#else>
					return new CUI.DataGrid({
						route:0,
						lockColumnCount:"${lockColumnCount}",
						validatorConfig: validatorBusinessPool,
						sContainerId:"${id}",
						aColumnDefs:columnDefs,
						aDataSource:[data], 
						oConfigs:{
									<#if beforeInitApplyCookieData != "">beforeInitApplyCookieData: ${beforeInitApplyCookieData},</#if>
									<#if beforeUploadCookieString != "">beforeUploadCookieString: ${beforeUploadCookieString},</#if>
									moreButtonResizeFlag : <#if moreButtonResizeFlag>true<#else>false</#if>,
									rowMergeKeys: aRowMergeKeys,
									checkedRowsMap: checkedRowsMap,
									<#if dataGridTableConfig != false>dataGridTableConfig: true,</#if>
									<#if noEditQuery != false>noEditQuery: true,</#if>
									<#if colMerge != "">colMergeKeys: aColMergeKeys,</#if>
									<#if condition != "">ClassCondition: "${condition}",</#if>
									<#if renderOverEvent != "">renderOverEvent: "${renderOverEvent}",</#if>
									<#if throwError != "">throwError: "${throwError}",</#if>
									<#if onPropertyChange != "">onPropertyChange: "${onPropertyChange}",</#if>
									<#if caption != "">caption: "${caption}",</#if>
									<#if height gt 0>height: ${height},</#if>
									<#if width gt 0>width:${width},</#if>
									<#if displayRowsCount gt 0>displayRowsCount:${displayRowsCount},</#if>
									<#if displayRowsCollapse>displayRowsCollapse:true,</#if>
									<#if editable>editable:true,</#if>
									<#if insertRowAble>insertRowAble:true,</#if>
									<#if superChecked>superChecked:true,</#if>
									<#if defaultNodeExpanded>defaultNodeExpanded:true,</#if>
									<#if superCheckedName != "">superCheckedName:"${superCheckedName}",</#if>
									<#if superCheckedId != "">superCheckedId:"${superCheckedId}",</#if>
									<#if cannotAddNewRow>cannotAddNewRow:true,</#if>
									<#if withoutConfigTable>withoutConfigTable:true,</#if>
									hideKey:hidekeys,
									<#if dblclick != "">dblclick:"${dblclick}",</#if>
									<#if onclick != "">onclick:"${onclick}",</#if>
									<#if checkEditCondition != "">checkEditCondition:"${checkEditCondition}",</#if>
									<#if fbuttons != "">fbuttons:${fbuttons},</#if>
									<#if custombtns != "">custombtns:${custombtns},</#if>			            
									<#if paginator>paginator:true,</#if>
									<#if exportExcel>exportExcel:true,</#if>
									<#if multiselect>multiselect:true,</#if>
									<#if tfoot!="">tfoot:${tfoot},</#if>
									<#if !complex>complex:false,</#if>
									<#if pageOrder!= "">pageOrder:"${pageOrder}",</#if>
									<#if orderMode!= "">orderMode:"${orderMode}",</#if>
									<#if autoaddrow != "">autoAddRow:${autoaddrow},</#if>
									<#if dgattribute != "">dbAttribute:true,</#if>
									<#if dtPage != "">dtPage:"${dtPage}",</#if>
									<#if queryFunc != "">queryFunc:"${queryFunc}",</#if>
									<#if formId != "">formId:"${formId}",</#if>
									<#if dataUrl != "">dataUrl:"${dataUrl}<#if dataUrl?index_of('?') gt 0>&<#else>?</#if>rt=json",</#if>
									<#if dataTableType != "">responseType:"${dataTableType}",</#if>
									<#if hidekeyPrefix != "">hidekeyPrefix:"${hidekeyPrefix}",</#if>
									<#if noPadding>noPadding:true,</#if>
									<#if dataclassifics != "">dataclassifics:${dataclassifics},</#if>
									<#if exportExcel>exportExcel:true,</#if>
									<#if copyRow>copyRow:true,</#if>
									<#if copyColumn>copyColumn:true,</#if>
									postData:"${postData}",
									firstLoad:<#if firstLoad>true<#else>false</#if>,
									buttons:buttonsAll,
									treeView:<#if treeView>true<#else>false</#if>,							
									<#if noPermissionFieldsMap?? && noPermissionFieldsMap?size gt 0>
						            noPermissionFields:noPermissionFields,
						            </#if>			            
			         				<#if assModelDisplayName != "">assModelDisplayName : "${assModelDisplayName?js_string}",</#if>
			         				<#if dgTitle != "">dgTitle : "${dgTitle}",</#if>
			         				<#if noCookie>noCookie:true,</#if>
									<#if colAdminFlag>colAdminFlag:true,</#if>			         				
									method:"${transMethod}"																	
								}
					});
					</#if>
				};
				
				YAHOO.util.Event.onDOMReady(function() {
					<#assign timeouts = 0 />
					<#if (!isExtra && isEdit && tabViewIndex gt 0 && ptRealTimeLoad == 0) || (isExtra && ptRealTimeLoad == 0)>
						<#assign timeouts = 200 />
						$('#${id}').parents('div.edit-panes').first().parent().find('ul.edit-tabs li').eq(${tabViewIndex}).one("click", function(event){
					</#if>
					<#if firstLoad && dataUrl != "">
						setTimeout(function(){
							${id}Widget = ${id}Widget${randomNum!} = datatable_${id} = buildDataTable({result:[],pageNo:1,pageSize:20,totalCount:0,totalPages:0});
							<#if pageInitMethod?has_content>${pageInitMethod!}(<#if isEdit && tabViewIndex gt 0 && ptRealTimeLoad=0>${tabViewIndex}</#if>);</#if>
						},${timeouts});
					<#else>
						setTimeout(function(){${id}Widget = ${id}Widget${randomNum!}= datatable_${id} = buildDataTable({result:[],pageNo:1,pageSize:20,totalCount:0,totalPages:0});<#if pageInitMethod?has_content>${pageInitMethod!}();</#if>},${timeouts});
					</#if>
					<#if (!isExtra && isEdit && tabViewIndex gt 0 && ptRealTimeLoad == 0) || (isExtra && ptRealTimeLoad == 0)>		
						});
					</#if>
				});	
			})(jQuery);
		</script>
		
		<input type="hidden" name="${id}_DELROWS"/>
	</#if>
</#macro>


<#macro datacolumn key,label,columnName="",hrefTo="",fileUpload=false,width=100,multable=false,defaultDisplay="",showFormatFunc="",crossCompany=false,displayFieldName="name",treeNode=false,multiselect=false,showFormat="",showType="",type="",rowMerge=false,order=true,testReg="",formulas="",viewUrl="",viewTitle="",viewCode="",mneenable=false,isCount=false,isTotal=false,popView=false,selectCompName="",callbackname="",hiddenCol=false,decimal="",defaultTitle="",editable=false,notnull=false,options="",defaultValue="",click="",click2="",onpropertychange="",onchange="",createOptions="",checkall="",checkallclick="",textalign="left",selectCol=false,hideInConfigtable=false,sortable=true,emptyIsEmpty=true,sum=false,stat=false,fieldType=0,viewType="",onclick="",beforecallback="",isObjCustomProp=false,objCustomPropNames="",cssstyle="",disableEscape=false,onBeforeClear="",onAfterClear="",onBeforeSet="",onAfterSet="">
	var node = {};
	node.key = "${key}";
	node.label = "${label}";
	node.selfType = "${type}";
	<#if rowMerge>aRowMergeKeys.push("${key}");</#if>
	<#if type != "">
		<#if type?upper_case == "INTEGER">
	        node.testReg=/^[-+]?\d+$/;
	        node.type ='integer';
			validatorBusinessPool['${key}'] = 'isInt';
        <#elseif type?upper_case == "DECIMAL">
	        node.testReg=/^\d+\.*\d*$/;
	        node.type = 'decimal';
			validatorBusinessPool['${key}'] = 'isFloat';
        <#elseif type?upper_case == "DATE">
        	<#if showFormat=='Y'>
				node.type="datetimeyear";
				validatorBusinessPool['${key}'] = 'isDateYear';
			<#elseif showFormat=='YM'>
				node.type="datetimemonth";
				validatorBusinessPool['${key}'] = 'isDateMonth';
			<#elseif showFormat=='YMD'>
				node.type="date";
				validatorBusinessPool['${key}'] = 'isDate';
			<#else>
				node.type="date";
				validatorBusinessPool['${key}'] = 'isDate';
			</#if>
		<#elseif type?upper_case == "DATETIME">
			<#if showFormat=='YMD_H'>
				node.type="dateTimeHour";
			<#elseif showFormat=='YMD_HM'>
				node.type="dateTimeMin";
			<#elseif showFormat=='YMD'>
				node.type="date";
			<#elseif showFormat=='Y'>
				node.type="datetimeyear";
			<#elseif showFormat=='YM'>
				node.type="datetimemonth";
			<#else>
				node.type="datetime";
			</#if>
        <#else>
        	node.type = "${type}";
        <#if testReg != "">node.testReg = '${testReg}';</#if>
        </#if>
    </#if>
	<#if defaultTitle != "">node.defaultTitle = '${defaultTitle}';</#if>
	node.edit = <#if editable>true<#else>false</#if>;
	<#if notnull>
		node.notnull = ${notnull?string};
		validatorBusinessPool['${key}'] = 'require';
	</#if>
	node.width= ${width};
	<#if options != "">
		node.options = ${options};
	</#if>
	<#if multiselect>
		hidekeys.push('${key}AddIds');
		hidekeys.push('${key}DeleteIds');
	</#if>
				<#if click != "">node.colclick=${click};</#if>
				<#if cssstyle != "">node.cssstyle='${cssstyle}';</#if>
				<#if onclick != "">node.onclick=${onclick};</#if>
				<#if click2 != "">node.colclick2=${click2};</#if>
				node.order=<#if order>true<#else>false</#if>;
				<#if checkall != "">node.checkall='${checkall}';</#if>
				<#if checkallclick != "">node.checkallclick=${checkallclick};</#if>
				<#if textalign != "">node.textalign='${textalign}';</#if>
				<#if formulas != "">node.formulas='${formulas}';</#if>
				<#if decimal != "">node.decimal='${decimal}';</#if>
				<#if emptyIsEmpty?? && emptyIsEmpty>node.emptyIsEmpty=true;</#if>
				<#if selectCompName != "">node.selectCompName='${selectCompName}';</#if>
				<#if callbackname != "">node.callbackname='${callbackname}';</#if>
				<#if selectCol>node.selectCol=true;</#if>
				<#if hideInConfigtable>node.hideInConfigtable=true;</#if>
				<#if sortable && type?upper_case != "BOOLEAN">node.sortable=true;</#if>
				<#if sum>node.sum=true;</#if>
				<#if stat>node.stat=true;</#if>
				<#if multiselect>node.multiselect=true;</#if>
				node.displayFieldName = '${displayFieldName}';
				<#if hiddenCol>node.hiddenCol=true;</#if>
				<#if mneenable>node.mneenable=true;</#if>
				<#if crossCompany>node.crossCompany=true;<#else>node.crossCompany=false;</#if>
				<#if isCount>node.isCount=true;</#if>
				<#if isTotal>node.isTotal=true;</#if>
				<#if popView>node.popView=true;</#if>
				<#if viewUrl != "">node.viewUrl='${viewUrl}';</#if>
				<#if viewTitle != "">node.viewTitle='${viewTitle}';</#if>
				<#if viewCode != "">node.viewCode='${viewCode}';</#if>
				<#if fieldType gt 0>node.fieldType = ${fieldType};</#if>
				<#if viewType != "">node.viewType='${viewType}';</#if>
				<#if defaultValue != "">node.defaultValue='${defaultValue}';</#if>
				<#if defaultDisplay != "">node.defaultDisplay='${defaultDisplay}';</#if>
				<#if showFormatFunc != "">node.showFormatFunc='${showFormatFunc}';</#if>
				<#if onpropertychange != "">node.onpropertychange="${onpropertychange}";</#if>
				<#if beforecallback != "">node.beforecallback="${beforecallback}";</#if>
				<#if onchange != "">node.onchange="${onchange}";</#if>
				<#if createOptions != "">node.createOptions="${createOptions}";</#if>
				<#if showFormat != "">node.showFormat='${showFormat!}';</#if>
				<#if onBeforeClear != "">node.onBeforeClear='${onBeforeClear!}';</#if>
				<#if onAfterClear != "">node.onAfterClear='${onAfterClear!}';</#if>
				<#if onBeforeSet != "">node.onBeforeSet='${onBeforeSet!}';</#if>
				<#if onAfterSet != "">node.onAfterSet='${onAfterSet!}';</#if>
				node.treeNode=<#if treeNode>true<#else>false</#if>;
				node.multable=<#if multable>true<#else>false</#if>;
				<#if hrefTo != "">node.hrefTo='${hrefTo!}';</#if>
				node.fileUpload=<#if fileUpload>true<#else>false</#if>;
				<#if fileUpload>
				hidekeys.push('${key}FileAddPaths');
  				hidekeys.push('${key}FileDeleteIds');
				</#if>
				<#if columnName != "">node.columnName="${columnName}";</#if>
				<#if isObjCustomProp>node.isObjCustomProp=true;</#if>
				<#if objCustomPropNames != "">node.objCustomPropNames="${objCustomPropNames}";</#if>
				<#if disableEscape>node.disableEscape=true;<#else>node.disableEscape=false;</#if>
				columnDefs[columnDefs.length]=node;
</#macro>
<#macro systemCodeColumn key,label,code,isMultable=false,isTree=false,isMultTree=false,isCustom=false,isOnlySelectLeaf=false,columnName="",width=100,type="",systemCodeType="",systemEntityCode="",systemCodeUrl="",onchange="",createOptions="",onpropertychange="",defaultValue="",testReg="",defaultTitle="",editable=false,notnull=false,options="",hiddenCol=false,click="",checkall="",checkallclick="",textalign="left",sortable=true,sum=false,stat=false,fieldType=0,beforecallback="",seniorSystemCode=false>
	var node = {};
	node.key = "${key}";
	node.label = "${label}";
	node.selfType = "systemcode";
	node.type = 'select';
	<#if isMultable><#--系统编码多选-->
		node.isMultable = true;
		node.systemCodeType = 'multiSelect';
		node.type = "multiselect";
	</#if>
	<#if isTree><#--系统编码树形-->
		node.isTree = true;
		node.systemCodeType = 'tree';
		<#if isMultTree>
			node.isMultTree = true;
			node.systemCodeType = 'multiTree';
			node.type = 'multitree';
		</#if>
		<#if isOnlySelectLeaf><#--树形单选是否只能选择叶子节点-->
			node.isOnlySelectLeaf = true;
		</#if>
	</#if>
	<#if isCustom><#--是否自定义字段-->
		node.isCustom = true;
	</#if>
    node.seniorSystemCode = ${seniorSystemCode?string};
	<#if defaultTitle != "">node.defaultTitle = '${defaultTitle}';</#if>
	node.edit = <#if editable>true<#else>false</#if>;
	<#if notnull>
		node.notnull = ${notnull?string};
		validatorBusinessPool['${key}'] = 'require';
	</#if>
	node.width= ${width};
	<#if options != "">
		node.options = ${options};
	</#if>
	<#if code?has_content>
		<#if seniorSystemCode>
			<#assign optionMap = getSystemCodeList(code, code)>
		<#else>
			<#assign optionMap = getSystemCodeList(code)>
		</#if>
		<#if optionMap??>
			var options${code} = {};
			<#list optionMap?keys as mapKey>
				options${code}["${mapKey}"] = "${optionMap[mapKey]}"; 
			</#list>
			node.options = options${code};
		</#if>
	</#if>
	<#if defaultValue?? && defaultValue!="">node.defaultValue='${(defaultValue!)?string}';</#if>
	<#if click != "">node.colclick=${click};</#if>
	<#if checkall != "">node.checkall='${checkall}';</#if>
	<#if checkallclick != "">node.checkallclick=${checkallclick};</#if>
	<#if textalign != "">node.textalign='${textalign}';</#if>
	<#if sortable>node.sortable=true;</#if>
	<#if sum>node.sum=true;</#if>
	<#if hiddenCol>node.hiddenCol=true;</#if>
	<#if stat>node.stat=true;</#if>
	<#if fieldType gt 0>node.fieldType = ${fieldType};</#if>
	<#if onpropertychange != "">node.onpropertychange="${onpropertychange}";</#if>
	<#if beforecallback != "">node.beforecallback="${beforecallback}";</#if>
	<#if onchange != "">node.onchange="${onchange}";</#if>
	<#if systemCodeType != "">node.systemCodeType="${systemCodeType}";</#if>
	<#if systemEntityCode != "">node.systemEntityCode="${systemEntityCode}";</#if>
	<#if systemCodeUrl != "">node.systemCodeUrl="${systemCodeUrl}";</#if>
	<#if createOptions != "">node.createOptions="${createOptions}";</#if>
	<#if columnName != "">node.columnName="${columnName}";</#if>
	columnDefs[columnDefs.length]=node;
</#macro>