<script type="text/javascript" src="${base}/static/core/jquery.js"></script>
<script type="text/javascript" src="${base}/static/cui/validation.js"></script>

<#if parameters.product>
<script type="text/javascript" src="${base}/static/res/main.js"></script>
<link rel="stylesheet" type="text/css" href="${base}/static/res/main.css" />
<#else>

<script type="text/javascript" src="${base}/static/core/cui-core.js"></script>
<script type="text/javascript" src="${base}/static/utils.js"></script>


<#if parameters.cssreset!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/reset/reset-min.css">
</#if>
<#if parameters.cssfonts!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/fonts/fonts-min.css">
</#if>
<#if parameters.cssgrids!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/grids/grids-min.css">
</#if>
<#if parameters.cssbase!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/base/base-min.css">
</#if>
	<script type="text/javascript" src="${base}/static/a/yahoo-dom-event.js"></script>
	<script type="text/javascript" src="${base}/static/a/utilities.js"></script>
	<script type="text/javascript" src="${base}/static/js/comfunc.js"></script>
	
	<#if parameters.frameset>
	<link rel="stylesheet" type="text/css" href="${base}/static/frameset/assets/layout.css">
	<script type="text/javascript" src="${base}/static/frameset/layout.js"></script>
	</#if>
	<#if parameters.panel>
	<link rel="stylesheet" type="text/css" href="${base}/static/panel/assets/panel.css">
	<script type="text/javascript" src="${base}/static/panel/panel.js"></script>
	</#if>
	<#if parameters.dialog>
	<link rel="stylesheet" type="text/css" href="${base}/static/dialog/assets/dialog.css">
	<script type="text/javascript" src="${base}/static/dialog/dialog.js"></script>
	</#if>
	<#if parameters.loading>
	<link rel="stylesheet" type="text/css" href="${base}/static/loading/assets/loading.css">
	<script type="text/javascript" src="${base}/static/loading/loading.js"></script>
	</#if>	
	<#if parameters.errorbar>
	<link rel="stylesheet" type="text/css" href="${base}/static/errorbar/assets/errorbar.css">
	<script type="text/javascript" src="${base}/static/errorbar/errorbar.js"></script>
	</#if>	
	<#if parameters.listmenu>
	<link rel="stylesheet" type="text/css" href="${base}/static/listmenu/assets/listmenu.css">
	<script type="text/javascript" src="${base}/static/listmenu/listmenu.js"></script>
	</#if>	
	<#if parameters.overlay>
	<link rel="stylesheet" type="text/css" href="${base}/static/overlay/assets/overlay.css">
	<script type="text/javascript" src="${base}/static/overlay/overlay.js"></script>
	</#if>

<#if parameters.tabview != 'false'>
	<#if parameters.tabview == 'true'>
	<link rel="stylesheet" type="text/css" href="${base}/static/tabview/assets/tabview1.css">
	<#else>
	<link rel="stylesheet" type="text/css" href="${base}/static/tabview/assets/${parameters.tabview}.css">
	</#if>
	<script type="text/javascript" src="${base}/static/tabview/tabview.js"></script>
</#if>

<#if parameters.treeview!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/treeview/assets/treeview.css">
	<link rel="stylesheet" type="text/css" href="${base}/static/treeview/assets/zTreeStyle.css">
	<#--<link rel="stylesheet" type="text/css" href="${base}/static/treeview/assets/zTreeIcons.css">-->
	<script type="text/javascript" src="${base}/static/treeview/treeview.js"></script>
	<script type="text/javascript" src="${base}/static/treeview/ztree.js"></script>
</#if>
   
<#if parameters.datepicker!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/datepicker/assets/main.css">
	<link rel="stylesheet" type="text/css" href="${base}/static/datepicker/assets/dp.css">
	<link rel="stylesheet" type="text/css" href="${base}/static/datepicker/assets/dropdown.css">
	<script type="text/javascript" src="${base}/static/datepicker/Common.js"></script>
	<script type="text/javascript" src="${base}/static/datepicker/jquery.datepicker.js"></script>
	<script type="text/javascript" src="${base}/static/datepicker/jquery.dropdown.js"></script>
</#if>

<#if parameters.datatable!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/datatable/assets/datatable.css">
	<script type="text/javascript" src="${base}/static/datatable/datasource.js"></script>
	<script type="text/javascript" src="${base}/static/datatable/datatable.js"></script>
	<script type="text/javascript" src="${base}/static/json/json.js"></script>
</#if>

<#if parameters.mneclient!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/mnemonic/assets/mnemonic.css">
	<script type="text/javascript" src="${base}/static/mnemonic/mnemonic.js"></script>
</#if>

<#if parameters.select!false>
	<link rel="stylesheet" type="text/css" href="${base}/static/select/assets/select.css">
	<script type="text/javascript" src="${base}/static/select/select.js"></script>
</#if>

<#if parameters.popupmenu>
	<link rel="stylesheet" type="text/css" href="${base}/static/menu/assets/menu.css">
	<script type="text/javascript" src="${base}/static/menu/menu.js"></script>
</#if>

<link rel="stylesheet" type="text/css" href="${base}/static/basictable/assets/basictable.css">
<script type="text/javascript" src="${base}/static/basictable/basictable.js"></script>

<script type="text/javascript" src="${base}/static/popWin/popWin.js"></script>
<link rel="stylesheet" type="text/css" href="${base}/static/popWin/assets/popWin.css">
</#if>