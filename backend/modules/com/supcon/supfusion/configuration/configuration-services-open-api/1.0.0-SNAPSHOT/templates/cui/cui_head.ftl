<#macro logincss>
	<link rel="stylesheet" type="text/css" href="/bap/static/res/login.css" />
	<link rel='stylesheet' type='text/css' href='/bap/static/css/defaultSkin.css}' />
	<#if getConfigProperty("platform/bap/basic/bap.theme")?? && getConfigProperty("platform/bap/basic/bap.theme")!="default">
	<link rel='stylesheet' type='text/css' href='/bap/static/css/${getConfigProperty("platform/bap/basic/bap.theme")}Skin.css}' />
	</#if>
</#macro>
<#macro loginjs>
	<script type="text/javascript" src="/bap/static/core/config.js"></script>
	<script type="text/javascript" src="/bap/static/core/jquery.js"></script>
	<script type="text/javascript" src="/bap/static/core/${getCurrent('lang')}/cui-core.js"></script>
	<script type="text/javascript" src="/bap/static/select/${getCurrent('lang')}/select.js"></script>
</#macro>
<#macro maincss>
    <link rel="stylesheet" type="text/css" href="/bap/static/res/main.css" />
    <link rel='stylesheet' type='text/css' href='/bap/static/css/defaultSkin.css' />
    <#if getConfigProperty("platform/bap/basic/bap.theme")?? && getConfigProperty("platform/bap/basic/bap.theme")!="default">
    <link rel='stylesheet' type='text/css' href='/bap/static/css/${getConfigProperty("platform/bap/basic/bap.theme")}Skin.css' />
    </#if>
    <#include 'cui_head_skin.ftl'>
    <#include 'cui_head_font.ftl'>
    <#include 'cui_head_international.ftl'>
</#macro>

<#macro mainframeshowskin customThemeCode="">
	<#if customThemeCode??>
        <link rel="stylesheet" type="text/css" href="/bap/static/bap-themes/${customThemeCode}/style.css">
	</#if>	
</#macro>

<#macro mainframev3skin customThemeCode="">
	<#if customThemeCode??>
		<link rel="stylesheet" type="text/css" href="/bap/static/bap-themes/${customThemeCode}/v3_main.css">
		<link rel="stylesheet" type="text/css" href="/bap/static/bap-themes/${customThemeCode}/menu.css">
	</#if>	
</#macro>
	
	
<#macro mainjs>
    <script type="text/javascript" src="/bap/static/res/${getCurrent('lang')}/core.js"></script>
    <script type="text/javascript" src="/bap/static/res/${getCurrent('lang')}/main.js"></script>
</#macro>

<#macro editcss>
    <link rel="stylesheet" type="text/css" href="/bap/static/res/main.css" />
    <link rel='stylesheet' type='text/css' href='/bap/static/css/defaultSkin.css}' />
    <#if getConfigProperty("platform/bap/basic/bap.theme")?? && getConfigProperty("platform/bap/basic/bap.theme")!="default">
    <link rel='stylesheet' type='text/css' href='/bap/static/css/${getConfigProperty("platform/bap/basic/bap.theme")}Skin.css' />
    </#if>
    <#include 'cui_head_skin.ftl'>
    <#include 'cui_head_font.ftl'>
    <#include 'cui_head_international.ftl'>
</#macro>

<#macro extracss>
    <link rel="stylesheet" type="text/css" href="/bap/static/css/extra.css" />
</#macro>

<#macro ecStyleCss>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/style.css" />
</#macro>
<#macro editjstop>
    <script type="text/javascript" src="/bap/static/res/${getCurrent('lang')}/editcore.js"></script>
</#macro>
<#macro editjs>
    <script type="text/javascript" src="/bap/static/res/${getCurrent('lang')}/edit.js"></script>
</#macro>
<#macro printjs>
    <script type="text/javascript" src="/bap/static/pagePrint/${getCurrent('lang')}/ec_print.js"></script>
</#macro>

<#macro ec_commonTop>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/style.css" />
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/view.css" />
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_common.js"></script>
    <script src="/bap/static/babel/babel-standalone.js?v=6.26.0"></script>
</#macro>
<#macro ec_editTop>
	<@head/>
	<@ec_commonTop/>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_edit.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_extra.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
</#macro>
<#macro ec_digestTop>
	<@ec_commonTop/>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_digest.js"></script>
</#macro>
<#macro ec_editMobileTop>
	<@head/>
	<@ec_commonTop/>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_edit_mobile.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
</#macro>
<#macro ec_listTop>
	<@mainjs/><@maincss/>
	<@ec_commonTop/>
    <link href="/bap/static/foundation/css/advquery.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="/bap/static/foundation/js/${getCurrent('lang')}/advquery.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_list.js"></script>
	<@adpSkin />
</#macro>
<#macro ec_extraTop>
	<@head/>
	<@ec_commonTop/>
    <link href="/bap/static/foundation/css/advquery.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="/bap/static/foundation/js/${getCurrent('lang')}/advquery.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_edit.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_extra.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
	<@adpSkin />
</#macro>
<#macro ec_extraMobileTop>
	<@head/>
	<@ec_commonTop/>
    <link href="/bap/static/foundation/css/advquery.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="/bap/static/foundation/js/${getCurrent('lang')}/advquery.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_edit.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_extraMobile.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
</#macro>
<#macro ec_advconfig>
    <script type="text/javascript" src="/bap/static/runtime/js/${getCurrent('lang')}/ec_common.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
    <script type="text/javascript" src="/bap/static/runtime/js/${getCurrent('lang')}/runtime_list.js"></script>
</#macro>
<#macro ec_treeTop>
	<@mainjs/><@maincss/>
	<@ec_commonTop/>
    <link href="/bap/static/foundation/css/advquery.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="/bap/static/foundation/js/${getCurrent('lang')}/advquery.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_list.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_datagrid.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_tree.js"></script>
	<@adpSkin />
</#macro>

<#macro ec_datagridTop>
	<@head />
	<@ec_commonTop/>
    <link href="/bap/static/foundation/css/advquery.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="/bap/static/foundation/js/${getCurrent('lang')}/advquery.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/jquery/assets/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_list.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_datagrid.js"></script>
	<@adpSkin />
</#macro>
<#macro ec_easytableTop>
	<@head />
	<@ec_commonTop/>
    <link href="/bap/static/foundation/css/advquery.css" rel="stylesheet" type="text/css" />
    <script type="text/javascript" src="/bap/static/foundation/js/${getCurrent('lang')}/advquery.js"></script>
    <link rel="stylesheet" type="text/css" href="/bap/static/jquery/assets/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/bap/static/ec/css/purchase.css">
    <link rel="stylesheet" type="text/css" href="/bap/static/treeview/assets/treeview.css">
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_list.js"></script>
    <script type="text/javascript" src="/bap/static/ec/js/${getCurrent('lang')}/ec_easytable.js"></script>
</#macro>

<#macro ec_flow_edit>
	<@head />
    <script type="text/javascript" charset="utf-8" src="/bap/static/js/FusionCharts.js"></script>
    <script type="text/javascript" charset="utf-8" src="/bap/static/js/common.js"></script>

</#macro>

<#macro ec_flow_edit_swf_url>/bap/static/ec/flowEditSwf/bpm_console.swf}</#macro>
<#macro ec_flow_view_swf_url>/bap/static/ec/flowEditSwf/flowView.swf}</#macro>

<#macro mobilejs>

	    <#if Session['touchScreen']?? && Session['touchScreen']??>
			<script type="text/javascript" src="/bap/static/res/${getCurrent('lang')}/touchScreen.js"></script>
	    <#else>
			<script type="text/javascript" src="/bap/static/res/${getCurrent('lang')}/mobile.js"></script>
			<#-- <script type="text/javascript" src="/bap/static/mobile/js/iscroll.js"></script> -->
	    </#if>

	<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
		<script src="/bap/static/mobile/js/html5shiv.js"></script>
		<script src="/bap/static/mobile/js/respond.min.js"></script>
    <![endif]-->
</#macro>

<#macro mobilecss>

		<#if Session['touchScreen']?? && Session['touchScreen']??>
			<link rel="stylesheet" type="text/css" href="/bap/static/res/touchScreen.css" />
		<#else>
			<link rel="stylesheet" type="text/css" href="/bap/static/res/mobile.css" />
		</#if>

</#macro>
<#macro mobileList>
	<script type="text/javascript" src="/bap/static/core/config.js"></script>
	<script type="text/javascript" src="/bap/static/core/jquery.js"></script>
	<script type="text/javascript" src="/bap/static/core/${getCurrent('lang')}/cui-core.js"></script>
	<script type="text/javascript" src="/bap/static/select/${getCurrent('lang')}/select.js"></script>
</#macro>

<#macro vueMobileBase>
	<script type="text/javascript" src="/bap/static/vuemobile/js/manifest.js"></script>
	<script type="text/javascript" src="/bap/static/vuemobile/js/vendor.js"></script>
</#macro>

<#macro vueMobileEdit>
	<@vueMobileBase />
	<link href="/bap/static/vuemobile/css/edit.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/bap/static/vuemobile/js/edit.js"></script>
</#macro>

<#macro vueMobileList>
	<@vueMobileBase />
	<link href="/bap/static/vuemobile/css/itemList.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/bap/static/vuemobile/js/itemList.js"></script>
</#macro>

<#macro vueMobilePosition>
	<@vueMobileBase />
	<link href="/bap/static/vuemobile/css/position.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/bap/static/vuemobile/js/position.js"></script>
</#macro>

<#macro vueMobileStaff>
	<@vueMobileBase />
	<link href="/bap/static/vuemobile/css/staff.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/bap/static/vuemobile/js/staff.js"></script>
</#macro>

<#macro vueMobileDepartment>
	<@vueMobileBase />
	<link href="/bap/static/vuemobile/css/department.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/bap/static/vuemobile/js/department.js"></script>
</#macro>
<#macro vueMobileUser>
	<@vueMobileBase />
	<link href="/bap/static/vuemobile/css/user.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/bap/static/vuemobile/js/user.js"></script>
</#macro>

<#-- 新bap暂时引用 -->
<#macro vueMobileBaseNew>
	<script type="text/javascript" src="/greenDill/mobile-static/js/manifest.js"></script>
	<script type="text/javascript" src="/greenDill/mobile-static/js/vendor.js"></script>
</#macro>

<#macro vueMobilePositionNew>
	<@vueMobileBaseNew />
	<link href="/greenDill/mobile-static/css/position.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/greenDill/mobile-static/js/position.js"></script>
</#macro>

<#macro vueMobileStaffNew>
	<@vueMobileBaseNew />
	<link href="/greenDill/mobile-static/css/staff.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/greenDill/mobile-static/js/staff.js"></script>
</#macro>

<#macro vueMobileDepartmentNew>
	<@vueMobileBaseNew />
	<link href="/greenDill/mobile-static/css/department.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/greenDill/mobile-static/js/department.js"></script>
</#macro>
<#macro vueMobileUserNew>
	<@vueMobileBaseNew />
	<link href="/greenDill/mobile-static/css/user.css?v=${bundle.version}" rel="stylesheet" />
	<script type="text/javascript" src="/greenDill/mobile-static/js/user.js"></script>
</#macro>





<#-- Head -->
<#macro head product=false,cssreset=false,cssfonts=false,cssgrids=false,cssbase=false,frameset=true,panel=true,dialog=true,loading=true,errorbar=true,listmenu=true,overlay=false,tabview=true,treeview=true,datepicker=true,datatable=true,mneclient=true,select=true,popupmenu=true,fullcalendar=true,weekcalendar=true,texteditor=true>
	<script type="text/javascript" src="/bap/static/res/${getCurrent('lang')}/oldmain.js"></script>
	<link rel="stylesheet" type="text/css" href="/bap/static/res/oldmain.css" />
	<link rel='stylesheet' type='text/css' href='/bap/static/css/defaultSkin.css' />
	<#if getConfigProperty("platform/bap/basic/bap.theme")?? && getConfigProperty("platform/bap/basic/bap.theme")!="default">
	<link rel='stylesheet' type='text/css' href='/bap/static/css/${getConfigProperty("platform/bap/basic/bap.theme")}Skin.css}' />
	</#if>
	<#include 'cui_head_skin.ftl'>
	<#include 'cui_head_font.ftl'>
	<#include 'cui_head_international.ftl'>
</#macro>

<#macro adpSkin>
	<!--<link rel="stylesheet" type="text/css" href="/bap/entity/skins/grey/main.css" />-->
	<meta name="skin-placeholder" id="skin-placeholder" />
	<script>
		(function(){
			var themeMap = {
				default: 'blue',
				dark: 'grey'
			};
			var defaultSkin = 'dark';
			try {
				var personalTheme = localStorage.getItem('personalTheme');
				personalTheme = JSON.parse(personalTheme);
				defaultSkin = personalTheme.theme;
			} catch (err){}
			var holder = document.getElementById('skin-placeholder');
			var t=document.createElement("link");
			t.type="text/css";
			t.rel="stylesheet"
			t.href="/bap/static/entity/skins/"+ themeMap[defaultSkin]  +"/main.css";
			holder.parentNode.insertBefore(t, holder.nextSibling);
		}());
	</script>
</#macro>
