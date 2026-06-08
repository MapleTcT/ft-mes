<#-- ======= 增强型布局实体配置中还原 ======== -->
<#macro extraView layouts="" parLayoutmethod="" parHaslefttopfixeds="" parHasrightbottomfixeds="" parNums="" >	
	<#if layouts?? && layouts.layoutProperties?? >
		<#if layouts.layoutProperties.islefttopfixed??><#local islefttopfixed=layouts.layoutProperties.islefttopfixed?string("true","false") ></#if>
		<#if layouts.layoutProperties.isrightbottomfixed??><#local isrightbottomfixed=layouts.layoutProperties.isrightbottomfixed?string("true","false") ></#if>
		<#if layouts.layoutProperties.nums??><#local nums=layouts.layoutProperties.nums ></#if>
		
		<#if layouts.layoutProperties.layoutname??><#local layoutname=layouts.layoutProperties.layoutname ></#if>
		<#if layouts.layoutProperties.parlayoutname??><#local parlayoutname=layouts.layoutProperties.parlayoutname ></#if>
		<#if layouts.layoutProperties.layoutmethod??><#local layoutmethod=layouts.layoutProperties.layoutmethod ></#if>
		<#if layouts.layoutProperties.layoutContent??><#local layoutContent=layouts.layoutProperties.layoutContent ></#if>
		<#if layouts.layoutProperties.ranksType??><#local ranksType=layouts.layoutProperties.ranksType ></#if>
		<#if layouts.layoutProperties.isborder??><#local isborder=layouts.layoutProperties.isborder ></#if>
		<#if layouts.layoutProperties.isreadonly??><#local isreadonly=layouts.layoutProperties.isreadonly ></#if>
		<#if layouts.layoutProperties.isreadonlyBak??><#local isreadonlyBak=layouts.layoutProperties.isreadonlyBak ></#if>
		<#if layouts.layoutProperties.ptRealTimeLoad??><#local ptRealTimeLoad=layouts.layoutProperties.ptRealTimeLoad ></#if>
		<#if layouts.layoutProperties.tabViewIndex??><#local tabViewIndex=layouts.layoutProperties.tabViewIndex ></#if>
		<#if layouts.layoutProperties.cssstyle??><#local cssstyle=layouts.layoutProperties.cssstyle ></#if>
		<#if layouts.layoutProperties.layno??><#local layno=layouts.layoutProperties.layno ></#if>
		<#if layouts.layoutProperties.isShow??><#local isShow=layouts.layoutProperties.isShow ></#if>
		
		<#if layouts.layoutProperties.fix_h??><#local fix_h=layouts.layoutProperties.fix_h><#else><#local fix_h="100"></#if>
		<#if layouts.layoutProperties.fix_w??><#local fix_w=layouts.layoutProperties.fix_w><#else><#local fix_w="200"></#if>
		<#if layouts.layoutProperties.ratio_h??><#local ratio_h=layouts.layoutProperties.ratio_h><#else><#local ratio_h="100"></#if>
		<#if layouts.layoutProperties.ratio_w??><#local ratio_w=layouts.layoutProperties.ratio_w><#else><#local ratio_w="100"></#if>
		
		<#if parLayoutmethod??>
			<#if parLayoutmethod=='row'>
			<script type="text/javascript">
			//1
			$(function(){
				var width = $("[layoutname='${parlayoutname!}']").width();
				var height = $("[layoutname='${parlayoutname!}']").height();
				$("[layoutname='${layoutname!}']").height(height - 6);
				<#if ranksType?? && ranksType=='panel' >
				var isleftTopFixed= "${parHaslefttopfixeds!}";						
				var isrightBottomFixed= "${parHasrightbottomfixeds!}" ;						
				if(null != isleftTopFixed && undefined != isleftTopFixed && isleftTopFixed == "true"){
					width = width - CUI(".lr-fixed:first",$("[layoutname='${parlayoutname!}']")).width() - 6;
	 			}
				if(null != isrightBottomFixed && undefined != isrightBottomFixed && isrightBottomFixed == "true"){
					width = width - CUI(".lr-fixed:last",$("[layoutname='${parlayoutname!}']")).width() - 6;
	 			}
				$("[layoutname='${layoutname!}']").width(parseInt(width*${ratio_w!}/100) - 6);
				$("[layoutname='${layoutname!}']").addClass("row-fixed");
				$("[layoutname='${layoutname!}']").attr("ratio_w","${ratio_w!}");
				<#elseif ranksType?? && ranksType=='fix' >
				$("[layoutname='${layoutname!}']").addClass("lr-fixed");
				$("[layoutname='${layoutname!}']").attr("fix_w","${fix_w!}");
				if (parseInt(${fix_w!}) >= width - 6) {
	 				$("[layoutname='${layoutname!}']").width(parseInt(width / 2));
	 			}else{
	 				$("[layoutname='${layoutname!}']").width(parseInt(${fix_w!}) - 6);
	 			}
				</#if>
				$(window).resize(function(){
					var width = $("[layoutname='${parlayoutname!}']").width();
					var height = $("[layoutname='${parlayoutname!}']").height();
					$("[layoutname='${layoutname!}']").height(height - 6);
					<#if ranksType?? && ranksType=='panel' >
					var ratio_w = $("[layoutname='${layoutname!}']").attr("ratio_w")?$("[layoutname='${layoutname!}']").attr("ratio_w"):"${ratio_w!}";
					var isleftTopFixed= "${parHaslefttopfixeds!}";						
					var isrightBottomFixed= "${parHasrightbottomfixeds!}" ;						
					if(null != isleftTopFixed && undefined != isleftTopFixed && isleftTopFixed == "true"){
						width = width - CUI(".lr-fixed:first",$("[layoutname='${parlayoutname!}']")).width() - 6;
		 			}
					if(null != isrightBottomFixed && undefined != isrightBottomFixed && isrightBottomFixed == "true"){
						width = width - CUI(".lr-fixed:last",$("[layoutname='${parlayoutname!}']")).width() - 6;
		 			}
					$("[layoutname='${layoutname!}']").height(height - 6);
					$("[layoutname='${layoutname!}']").width(parseInt(width*${ratio_w!}/100) - 6);
					$("[layoutname='${layoutname!}']").addClass("row-fixed");
					$("[layoutname='${layoutname!}']").attr("ratio_w",ratio_w);
					<#elseif ranksType?? && ranksType=='fix' >
					$("[layoutname='${layoutname!}']").addClass("lr-fixed");
					$("[layoutname='${layoutname!}']").attr("fix_w","${fix_w!}");
					if (parseInt(${fix_w!}) >= width - 6) {
		 				$("[layoutname='${layoutname!}']").width(parseInt(width / 2));
		 			}else{
		 				$("[layoutname='${layoutname!}']").width(parseInt(${fix_w!}) - 6);
		 			}
					</#if>
				});
			});
			</script>
			<#if layoutmethod?? && layoutmethod !='container'  >
			<div class="ui-droppable layout <#if ranksType?? && ranksType=='fix'>lr-fixed</#if>" parlayoutname="${parlayoutname!}" layoutname="${layoutname!}" layoutmethod="${layoutmethod}" nums="${nums!}" isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}" islefttopfixed="${islefttopfixed!}" isrightbottomfixed="${isrightbottomfixed!}" style="overflow:hidden;<#if ranksType?? && ranksType=='fix' && fix_w?? >width:${fix_w!}px;</#if>">
				<div class="confIcon" style="position:absolute;">
					<div class="widget-configure-menu">&nbsp;</div>
				</div>
				<#if layouts.layout?? >
					<#list layouts.layout as layouts >
						<@extraView layouts=layouts parLayoutmethod=layoutmethod parHaslefttopfixeds=islefttopfixed parHasrightbottomfixeds=isrightbottomfixed >
						</@extraView>
					</#list>
				<#elseif layouts.tabs?? >
					<ul class="layout-tabs"></ul>
					<#list layouts.tabs as tabs >
						<@extraView layouts=tabs parLayoutmethod=layoutmethod parNums=layouts.layoutProperties.nums  parHaslefttopfixeds=islefttopfixed parHasrightbottomfixeds=isrightbottomfixed  >
						</@extraView>
					</#list>
				</#if>
			</div>
			<#elseif layoutmethod?? && layoutmethod =='container'>
			<div class="ui-droppable layout-common <#if ranksType?? && ranksType=='fix'>lr-fixed</#if>" parlayoutname="${parlayoutname!}" layoutContent="${layoutContent!}" layoutname="${layoutname!}" layoutmethod="${layoutmethod}" <#if layouts.layoutProperties.url?? >url="${layouts.layoutProperties.url!}"</#if> <#if layouts.layoutProperties.showmethod?? >showmethod="${layouts.layoutProperties.showmethod!}"</#if> nums="${nums!}" isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if>  <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}" islefttopfixed="${islefttopfixed!}" isrightbottomfixed="${isrightbottomfixed!}" style="overflow-y:auto;<#if ranksType?? && ranksType=='fix' && fix_w?? >width:${fix_w!}px;</#if>" >
				<div class="confIcon" style="position:absolute;">
					<div class="widget-configure-menu">&nbsp;</div>
				</div>
				<@extraViewContainer layouts=layouts.sections layoutProperties=layouts.layoutProperties >
				</@extraViewContainer>
			</div>
			<#else>
			<div class="ui-droppable layout-common <#if ranksType?? && ranksType=='fix'>lr-fixed</#if>" parlayoutname="${parlayoutname!}" layoutContent="${layoutContent!}" layoutname="${layoutname!}" layoutmethod="${layoutmethod!}" <#if layouts.layoutProperties.url?? >url="${layouts.layoutProperties.url!}"</#if> <#if layouts.layoutProperties.showmethod?? >showmethod="${layouts.layoutProperties.showmethod!}"</#if> nums="${nums!}" isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}" islefttopfixed="${islefttopfixed!}" isrightbottomfixed="${isrightbottomfixed!}" <#if ranksType?? && ranksType=='fix' && fix_w?? >style="width:${fix_w!}px;"</#if>>
				<div class="confIcon" style="position:absolute;">
					<div class="widget-configure-menu">&nbsp;</div>
				</div>
			</div>
			</#if>
			<#elseif parLayoutmethod=='column'>
			<script type="text/javascript">
			//2
			$(function(){
				var width = $("[layoutname='${parlayoutname!}']").width();
				var height = $("[layoutname='${parlayoutname!}']").height();
				$("[layoutname='${layoutname!}']").width(width - 6);
				<#if ranksType?? && ranksType=='panel' >
				var isleftTopFixed="${parHaslefttopfixeds!}";						
				var isrightBottomFixed="${parHasrightbottomfixeds!}";						
				if(null != isleftTopFixed && undefined != isleftTopFixed && isleftTopFixed == "true"){
					height = height - CUI(".tb-fixed:first",$("[layoutname='${parlayoutname!}']")).height() - 6;
	 			}
				if(null != isrightBottomFixed && undefined != isrightBottomFixed && isrightBottomFixed == "true"){
					height = height - CUI(".tb-fixed:last",$("[layoutname='${parlayoutname!}']")).height() - 6;
	 			}
				$("[layoutname='${layoutname!}']").height(parseInt(height*${ratio_h!}/100) - 6);
				$("[layoutname='${layoutname!}']").addClass("column-fixed");
				$("[layoutname='${layoutname!}']").attr("ratio_h","${ratio_h!}");
				<#elseif ranksType?? && ranksType=='fix' >
				$("[layoutname='${layoutname!}']").addClass("tb-fixed");
				$("[layoutname='${layoutname!}']").attr("fix_h","${fix_h!}");
				if (parseInt(${fix_h!}) >= height - 6) {
	 				$("[layoutname='${layoutname!}']").height(parseInt(height / 2));
	 			}else{
	 				$("[layoutname='${layoutname!}']").height(parseInt(${fix_h!}) - 6);
	 			}
				</#if>
				$(window).resize(function(){
					var width = $("[layoutname='${parlayoutname!}']").width();
					var height = $("[layoutname='${parlayoutname!}']").height();
					$("[layoutname='${layoutname!}']").width(width - 6);
					<#if ranksType?? && ranksType=='panel' >
					var ratio_h = $("[layoutname='${layoutname!}']").attr("ratio_h")? $("[layoutname='${layoutname!}']").attr("ratio_h"):"${ratio_h!}";
					var isleftTopFixed="${parHaslefttopfixeds!}";						
					var isrightBottomFixed="${parHasrightbottomfixeds!}";						
					if(null != isleftTopFixed && undefined != isleftTopFixed && isleftTopFixed == "true"){
						height = height - CUI(".tb-fixed:first",$("[layoutname='${parlayoutname!}']")).height() - 6;
		 			}
					if(null != isrightBottomFixed && undefined != isrightBottomFixed && isrightBottomFixed == "true"){
						height = height - CUI(".tb-fixed:last",$("[layoutname='${parlayoutname!}']")).height() - 6;
		 			}
					$("[layoutname='${layoutname!}']").width(width - 6);
					$("[layoutname='${layoutname!}']").height(parseInt(height*${ratio_h!}/100) - 6);
					$("[layoutname='${layoutname!}']").addClass("column-fixed");
					$("[layoutname='${layoutname!}']").attr("ratio_h",ratio_h);
					<#elseif ranksType?? && ranksType=='fix' >
					$("[layoutname='${layoutname!}']").addClass("tb-fixed");
					$("[layoutname='${layoutname!}']").attr("fix_h","${fix_h!}");
					if (parseInt(${fix_h!}) >= height - 6) {
		 				$("[layoutname='${layoutname!}']").height(parseInt(height / 2));
		 			}else{
		 				$("[layoutname='${layoutname!}']").height(parseInt(${fix_h!}) - 6);
		 			}
					</#if>
				});
			});
			</script>
			<#if layoutmethod?? && layoutmethod !='container'  >
			<div class="ui-droppable layout <#if ranksType?? && ranksType=='fix'>tb-fixed</#if>" parlayoutname="${parlayoutname!}" layoutname="${layoutname!}" layoutmethod="${layoutmethod}" nums="${nums!}"  isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}"  islefttopfixed="${islefttopfixed!}" isrightbottomfixed="${isrightbottomfixed!}" style="overflow:hidden;<#if ranksType?? && ranksType=='fix' && fix_h?? >height:${fix_h!}px;</#if>">
				<div class="confIcon" style="position:absolute;">
					<div class="widget-configure-menu">&nbsp;</div>
				</div> 
				<#if layouts.layout?? >
					<#list layouts.layout as layouts >
						<@extraView layouts=layouts parLayoutmethod=layoutmethod parHaslefttopfixeds=islefttopfixed parHasrightbottomfixeds=isrightbottomfixed >
						</@extraView>
					</#list>
				<#elseif layouts.tabs?? >
					<ul class="layout-tabs"></ul>
					<#list layouts.tabs as tabs >
						<@extraView layouts=tabs parLayoutmethod=layoutmethod parNums=layouts.layoutProperties.nums  parHaslefttopfixeds=islefttopfixed parHasrightbottomfixeds=isrightbottomfixed  >
						</@extraView>
					</#list>
				</#if>
			<#elseif layoutmethod?? && layoutmethod =='container'>
			<div class="ui-droppable layout-common <#if ranksType?? && ranksType=='fix'>tb-fixed</#if>" parlayoutname="${parlayoutname!}" layoutname="${layoutname!}" layoutmethod="${layoutmethod}" layoutContent="${layoutContent!}" <#if layouts.layoutProperties.url?? >url="${layouts.layoutProperties.url!}"</#if>  <#if layouts.layoutProperties.showmethod?? >showmethod="${layouts.layoutProperties.showmethod!}"</#if> nums="${nums!}"  isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}"  islefttopfixed="${islefttopfixed!}" isrightbottomfixed="${isrightbottomfixed!}"  style="overflow-y:auto;<#if ranksType?? && ranksType=='fix' && fix_h?? >height:${fix_h!}px;</#if>" >
				<div class="confIcon" style="position:absolute;">
					<div class="widget-configure-menu">&nbsp;</div>
				</div>
				<@extraViewContainer layouts=layouts.sections layoutProperties=layouts.layoutProperties >
				</@extraViewContainer>
			<#else>
			<div class="ui-droppable layout-common <#if ranksType?? && ranksType=='fix'>tb-fixed</#if>" parlayoutname="${parlayoutname!}" layoutname="${layoutname!}" layoutmethod="${layoutmethod!}" layoutContent="${layoutContent!}" <#if layouts.layoutProperties.url?? >url="${layouts.layoutProperties.url!}"</#if> <#if layouts.layoutProperties.showmethod?? >showmethod="${layouts.layoutProperties.showmethod!}"</#if>  nums="${nums!}"  isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}"  islefttopfixed="${islefttopfixed!}" isrightbottomfixed="${isrightbottomfixed!}" <#if ranksType?? && ranksType=='fix' && fix_h?? >style="height:${fix_h!}px;"</#if> >
				<div class="confIcon" style="position:absolute;">
					<div class="widget-configure-menu">&nbsp;</div>
				</div>
			</#if>
			</div>
			<#elseif parLayoutmethod=='tab'>
				<#assign layoutId=layouts.id>
				<script type="text/javascript">
				//3
				$(function(){
					var width = $("[layoutname='${parlayoutname!}']").width();
					var height = $("[layoutname='${parlayoutname!}']").height();
					var tabWidth= parseInt((width-2)/${parNums!1})-2; //平均 得到页签的宽度
					var $ulObj = $("[layoutname='${layoutname!}']").prevAll("ul.layout-tabs");
					var tabsContentLi = '<li paneid="${layoutId!}" layno="${layno!}" namekey="${layouts.namekey}" style="height:33px;width:'+tabWidth+'px;" ><span class="tabs-span">${getText(layouts.namekey!)}</span><a class="tabs-close" title="关闭页签"></a></li>';
					if($ulObj.find("[paneid='${layoutId!}']").size() == 0){
						$ulObj.append(tabsContentLi);
					}					
					$("[layoutname='${layoutname!}']").height(height - $ulObj.outerHeight(true) - 6);
					$("[layoutname='${layoutname!}']").width(width - 6);
					//页面加载的时候，默认模拟点击第一个页签
					var $firstLi = $("[layoutname='${parlayoutname!}'] > ul").children("li:first");
					$firstLi.addClass("tab-active");
					$("#"+$firstLi.attr("paneid")).css("display","block");
					$firstLi.trigger('click');
					$(window).resize(function(){
						var width = $("[layoutname='${parlayoutname!}']").width();
						var height = $("[layoutname='${parlayoutname!}']").height();
						var tabWidth= parseInt((width-2)/${parNums!1})-2; //平均 得到页签的宽度
						var $ulObj = $("[layoutname='${layoutname!}']").prevAll("ul.layout-tabs");
						$("[layoutname='${layoutname!}']").height(height - $ulObj.outerHeight(true) - 6);
						$("[layoutname='${layoutname!}']").width(width - 6);
					});
				});
				</script>
				<#if layoutmethod?? && layoutmethod == 'container' >
					<div class="tab-pane layout-common ui-droppable" layoutmethod="${layoutmethod!}"  layoutcontent="${layoutContent!}" parlayoutname="${parlayoutname!}" layno="${layno!}" layoutname="${layoutname!}" id="${layoutId!}" isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if layouts.layoutProperties.url?? >url="${layouts.layoutProperties.url!}"</#if>  <#if layouts.layoutProperties.showmethod?? >showmethod="${layouts.layoutProperties.showmethod!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}" style="overflow-y:auto;" >
				<#elseif layoutmethod?? && layoutmethod != 'container' >
					<div class="tab-pane layout ui-droppable" layoutmethod="${layoutmethod!}" parlayoutname="${parlayoutname!}" layno="${layno!}" layoutname="${layoutname!}" id="${layoutId!}" nums="${nums!}" isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}" islefttopfixed="${islefttopfixed!}" isrightbottomfixed="${isrightbottomfixed!}" style="overflow:hidden;">
				<#else>
					<div class="tab-pane layout-common ui-droppable" layoutcontent="${layoutContent!}" parlayoutname="${parlayoutname!}" layno="${layno!}" layoutname="${layoutname!}" id="${layoutId!}" isborder="${isborder!}" <#if isreadonly??>isreadonly="${isreadonly!}"</#if> <#if isreadonlyBak??>isreadonlyBak="${isreadonlyBak!}"</#if> <#if ptRealTimeLoad??>ptRealTimeLoad="${ptRealTimeLoad!}"</#if> <#if tabViewIndex??>tabViewIndex="${tabViewIndex!}"</#if> <#if cssstyle??>cssstyle="${cssstyle!}"</#if> layno="${layno!}" >	
				</#if>
					<div class="confIcon" style="position:absolute;">
						<div class="widget-configure-menu">&nbsp;</div>
					</div>
					<#if layouts.layout?? >
						<#list layouts.layout as layouts >
							<@extraView layouts=layouts parLayoutmethod=layoutmethod parHaslefttopfixeds=islefttopfixed parHasrightbottomfixeds=isrightbottomfixed >
							</@extraView>
						</#list>
					<#elseif layouts.tabs?? >
						<ul class="layout-tabs"></ul>
						<#list layouts.tabs as tabs >
							<@extraView layouts=tabs parLayoutmethod=layoutmethod parNums=layouts.layoutProperties.nums  parHaslefttopfixeds=islefttopfixed parHasrightbottomfixeds=isrightbottomfixed  >
							</@extraView>
						</#list>
					<#elseif layouts.sections?? >
						<@extraViewContainer layouts=layouts.sections layoutProperties=layouts.layoutProperties >
						</@extraViewContainer>
					<#elseif layouts.layoutProperties?? && layouts.layoutProperties.layoutContent?? && layouts.layoutProperties.layoutContent == 'webFrame' >
						<div class='urlBox'><img src='/bap/static/images/urlImg.png' /></div>
					<#elseif layouts.layoutProperties?? && layouts.layoutProperties.layoutContent?? && layouts.layoutProperties.layoutContent == 'searchWidget' >
						<@extraViewContainer layouts=layouts layoutProperties=layouts.layoutProperties >
						</@extraViewContainer>
					<#elseif layouts.layoutProperties?? && layouts.layoutProperties.layoutContent?? && layouts.layoutProperties.layoutContent == 'echarts' >
						<@extraViewContainer layouts=layouts layoutProperties=layouts.layoutProperties >
						</@extraViewContainer>
					</#if>
				</div>
			<#elseif parLayoutmethod=='container'>
				<#if layouts?? && layouts.layoutProperties?? >
					<@extraViewContainer layouts=layouts layoutProperties=layouts.layoutProperties >
					</@extraViewContainer>
				</#if>
			</#if>		
		</#if>
	</#if>
</#macro>

<#-- ======= 增强型布局Container 展现 ======== -->
<#macro extraViewContainer layouts="" layoutProperties="" >
	<#if layoutProperties??>
		<#if layoutProperties.layoutname??>
			<#local layoutname = layoutProperties.layoutname >
		<#else>
			<#local layoutname = "" >
		</#if>
		<#if layoutProperties.echartCode??>
			<#local echartCode = layoutProperties.echartCode >
		<#else>
			<#local echartCode = "" >
		</#if>
		<#if layoutProperties.echartType??>
			<#local echartType = layoutProperties.echartType >
		<#else>
			<#local echartType = "" >
		</#if>
		<#if layoutProperties.layertype??>
			<#local layertype = layoutProperties.layertype >
		<#else>
			<#local layertype = "" >
		</#if>
		<#if layoutProperties.layerpropertycode??>
			<#local layerpropertycode = layoutProperties.layerpropertycode >
		<#else>
			<#local layerpropertycode = "" >
		</#if>
		<#if layoutProperties.layerName??>
			<#local layerName = layoutProperties.layerName >
		<#else>
			<#local layerName = "" >
		</#if>
		<#if layoutProperties.layerNameKey??>
			<#local layerNameKey = layoutProperties.layerNameKey >
		<#else>
			<#local layerNameKey = "" >
		</#if>
	<#else>
		<#local layoutname = "" >
		<#local echartCode = "" >
		<#local echartType = "" >
	</#if>
	<#if layoutProperties?? && layoutProperties.layoutContent?? >
		<#if layoutProperties.layoutContent=='section'>
			<#list layouts as secatt>
				<#assign sectionCode = secatt.sectionCode!ecCodeInit('section')>
				<#if (secatt.pageConfig.colNum)??>
					<#assign sectionColNum = secatt.pageConfig.colNum>
				<#else>
					<#assign sectionColNum = defaultColNum>
				</#if>
				<ul class="form_design_ul <#if ((secatt.customSection)!false)>custom_section</#if>" sectionCode="${sectionCode!}" onclick="ec.selectsection(this)" colwidth="${(secatt.pageConfig.colwidth)!}" colNum="${sectionColNum}" name="${(secatt.name)!}" isborder="${(secatt.isborder)!1}" cssstyle="${(secatt.cssstyle)!''}">
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
						
						<#if (element.element.isMobile)?? && (element.element.isMobile) ><#--mobile视图-->
							<#if (element.element.showType)?? &&(element.element.showType) == 'LABEL'>
								<#if (propCode)?? && propertyMap?? && propertyMap[propCode]??>
									<#assign property = propertyMap[propCode] >
								<li class="form_design_ul_li" isMobile="${((element.element.isMobile)!true)?string}" isTitle="${((element.element.isTitle)!true)?string}" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" key="${(element.element.key)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" propertyCode="${(element.element.propertyCode)!}" complex="${(element.element.complex)?string("true","false")}" <#if currentProperty=='undefined'>propnullable="${(property.nullable!false)?string('true','false')}"</#if> nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((property.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if>  <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}" showFormat="${(element.element.showFormat)!}"
						propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(element.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}" sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.element.funcname)!}" funcbody="${((element.element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" 
						<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' 
						<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"</#if> hide=${(element.element.isHidden!false)?string("true","false")} textalign="${(element.element.textalign)!'center'}" showFormatFunc="${(element.element.showFormatFunc?html)!}"
						<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" <#--convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
						isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
						acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
						</#if> <#if (element.element.layerType)??>layerType=${element.element.layerType!}</#if>>
								<#else>
								<li class="form_design_ul_li" isMobile="${((element.element.isMobile)!true)?string}" isTitle="${((element.element.isTitle)!true)?string}" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" key="${(element.element.key)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" propertyCode="${(element.element.propertyCode)!}" complex="${(element.element.complex)?string("true","false")}" nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((element.element.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if>   <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}"  showFormat="${(element.element.showFormat)!}"
						 sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.element.funcname)!}" funcbody="${((element.element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}"  hide=${(element.element.isHidden!false)?string("true","false")} textalign="${(element.element.textalign)!'center'}" showFormatFunc="${(element.element.showFormatFunc?html)!}"
						<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' 
						<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"</#if>
						<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" <#--convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
						isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}" 
						acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
						</#if> <#if (element.element.layerType)??>layerType=${element.element.layerType!}</#if>>
								</#if>
								<dd >${getText('${((element.element.namekey)!element.element.name)!}')}</dd>
							    <#--mobile自定义字段-->
                            <#elseif (element.element.customSection)?? &&(element.element.customSection)>
                                <li class="form_design_ul_li" isMobile="${((element.element.isMobile)!true)?string}" isTitle="${((element.element.isTitle)!true)?string}" cellCode="${cellCode!}" ondblclick="ec.cellProperty()" onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" key="${(element.element.key)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" propertyCode="${(element.element.propertyCode)!}" complex="${(element.element.complex)?string("true","false")}" nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((element.element.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if>   <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}"  showFormat="${(element.element.showFormat)!}" namekey="${(element.element.namekey)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" isCustom="${(element.element.isCustom!false)?string("true","false")}"
                                    customsection="true"
                                    customModelCode="${(element.element.customModelCode)!}"
                                    propertyLayRec="${(element.element.propertyLayRec)!}"
                                    modelNameInternational="${(element.element.modelNameInternational)!}">
                                <dd >${getText('${((element.element.namekey)!element.element.name)!}')}&nbsp;[${(element.element.modelNameInternational)!}]</dd>
							<#else>
								<#if (propCode)?? && propCode!='' && propertyMap?? && propertyMap[propCode]??>
									<#assign property = propertyMap[propCode] >
								<li class="form_design_ul_li" isMobile="${((element.element.isMobile)!true)?string}" cellCode="${cellCode!}" name='${(element.element.key)!}' displayDefaultType="${(element.element.displayDefaultType)!}" columnType="${property.type!}" openPending=<#if !element.element.openPending?? && element.element.key?? && element.element.key == 'tableNo'>true<#else>${(element.element.openPending!false)?string("true","false")}</#if> colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" isCount=${(element.element.isCount!false)?string("true","false")} isTotal=${(element.element.isTotal!false)?string("true","false")} namekey="${(element.element.namekey)!}" key='${(element.element.key)!}' showFormatFunc="${(element.element.showFormatFunc?html)!}" <#if element.element.multable??>multable=${(property.multable!false)?string("true","false")}<#else>multable=false</#if> <#if element.element.isOrderBy??>isOrderBy=${(element.element.isOrderBy)?string("true","false")}<#else>isOrderBy=false</#if> sortType='${(element.element.sortType)!}' seqNumber='${(element.element.seqNumber)!}' <#if element.element.isLink??>isLink=${(element.element.isLink)?string("true","false")}</#if> assModels='${(element.element.assModels)!}' linkView='${(element.element.linkView)!}' assModelCode='${(element.element.assModelCode)!}' modelCode='${(element.element.modelCode)!}' mnecode='${(property.isUsedMneCode!false)?string('true','false')}' showType='${(property.fieldType)!}' layRec='${(element.element.layRec)!}' formulas="${((element.element.formulas)!'')?html}" textalign="${(element.element.textalign)!'center'}" decimalNum='${(property.decimalNum)!}'
																	fill='${(property.fillcontent)!}'  gns="${(element.element.gns)!}" us="${(element.element.us)!}" ids="${(element.element.ids)!}" ss="${(element.element.ss)!}" sns="${(element.element.sns)!}"
																	precisionHasChanged="${(element.element.precisionHasChanged!false)?string('true','false')}" precision="<#if (element.element.precisionHasChanged!false)?string('true','false')=='true'>${element.element.precision!}<#else>${property.decimalNum!}</#if>" propPrecision="${property.decimalNum!}" 
																	showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}" showFormat="<#if (element.element.showFormatHasChanged!false)?string('true','false')=='true'>${element.element.showFormat!}<#else>${property.format!}</#if>" propShowFormat="${property.format!}"
																 	isTreeNode="${(element.element.isTreeNode!false)?string("true","false")}" 
																 	assPropertyName="${(element.element.assPropertyName)!}"
															 		funcname="${(element.element.funcname!'')?html}"
																	funcbody="${(element.element.funcbody!'')?html}"
																	cssstyle="${(element.element.cssstyle )!}"
																	seniorSystemcode="${(element.element.seniorSystemcode!false)?string("true","false")}"
																	isCustom="${(element.element.isCustom!false)?string("true","false")}" 
																 	<#if (element.element.ass)?? && element.element.ass?has_content><#list element.element.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(element.element.ass)[asstar]!}'</#list></#if>
																 	propertyCode='${(element.element.propertyCode)!}' 
																 	hide=${(element.element.isHidden!false)?string("true","false")} 
																 	style="width: ${(element.element.width)!100}px"
																 	proWidth=${(element.element.width)!100} 
																 	ondblclick="ec.cellProperty()" 
																 	onmousedown="ec.chooseLi(this)">
								<#else>
								<li class="form_design_ul_li" isMobile="${((element.element.isMobile)!true)?string}" cellCode="${cellCode!}" name='${(element.element.key)!}' displayDefaultType="${(element.element.displayDefaultType)!}" openPending=<#if !element.element.openPending?? && element.element.key?? && element.element.key == 'tableNo'>true<#else>${(element.element.openPending!false)?string("true","false")}</#if> colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" isCount=${(element.element.isCount!false)?string("true","false")} isTotal=${(element.element.isTotal!false)?string("true","false")} namekey="${(element.element.namekey)!}" key='${(element.element.key)!}' <#if element.element.multable??>multable=${(element.element.multable)?string("true","false")}<#else>multable=false</#if> <#if element.element.isOrderBy??>isOrderBy=${(element.element.isOrderBy)?string("true","false")}<#else>isOrderBy=false</#if> sortType='${(element.element.sortType)!}' seqNumber='${(element.element.seqNumber)!}' <#if element.element.isLink??>isLink=${(element.element.isLink)?string("true","false")}</#if> assModels='${(element.element.assModels)!}' linkView='${(element.element.linkView)!}' assModelCode='${(element.element.assModelCode)!}' modelCode='${(element.element.modelCode)!}' mnecode='${(element.element.mnecode!false)?string('true','false')}' showType='${(element.element.showType)!}' layRec='${(element.element.layRec)!}' formulas="${((element.element.formulas)!'')?html}" textalign="${(element.element.textalign)!'center'}" decimalNum='${(element.element.decimalNum)!}' gns="${(element.element.gns)!}" us="${(element.element.us)!}" ids="${(element.element.ids)!}" ss="${(element.element.ss)!}" sns="${(element.element.sns)!}"
																	fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index &gt; 0>,</#if>"${fe}":<#if (element.element.showType) == 'ENUMERATE' && fe == 'fillContent'>{<#list (element.element.fill)[fe]?keys as ne><#if ne_index &gt; 0>,</#if>"${ne}":"${((element.element.fill)[fe][ne])?html}"</#list>}<#else>"${((element.element.fill)[fe])?html}"</#if></#list></#if>}'
																 	<#if (element.element.assoFlag)!false>
																 	assoFlag="true"
																 	assoConfig='${(element.element.assoConfig)!}'
																 	</#if>
																 	<#if (element.element.customSection)!false>
																 	customsection="true"
																 	customModelCode="${(element.element.customModelCode)!}"
																 	propertyLayRec="${(element.element.propertyLayRec)!}"
																 	modelNameInternational="${(element.element.modelNameInternational)!}"
																 	</#if>
																 	code="${(element.element.code)!''}"
																 	precisionHasChanged="${(element.element.precisionHasChanged!false)?string('true','false')}"
																 	columnType="${(element.element.columnType)!}"
																 	showFormat="${(element.element.showFormat)!}"
																 	isTreeNode="${(element.element.isTreeNode!false)?string("true","false")}"
																 	isCustom="${(element.element.isCustom!false)?string("true","false")}" 
																 	assPropertyName="${(element.element.assPropertyName)!}"
																 	seniorSystemcode="${(element.element.seniorSystemcode!false)?string("true","false")}"
																 	funcname ="${(element.element.funcname!'')?html}"
																	funcbody="${(element.element.funcbody!'')?html}"
																	cssstyle="${(element.element.cssstyle )!}"
																 	<#if (element.element.ass)?? && element.element.ass?has_content><#list element.element.ass?keys as asstar><#if asstar == 'tar'>assTar<#else>assOrg</#if>='${(element.element.ass)[asstar]!}'</#list></#if>
																 	propertyCode='${(element.element.propertyCode)!}' 
																 	hide=${(element.element.isHidden!false)?string("true","false")}
																	style="width: ${(element.element.width)!100}px"						 	 
																 	proWidth=${(element.element.width)!100} 
																 	ondblclick="ec.cellProperty()" 
																 	onmousedown="ec.chooseLi(this)">
								</#if>
								<dd>
									<#if  (element.element.showType)??>
									<#if (element.element.showType) == 'TEXTAREA' || (element.element.showType) == 'RICHTEXT'>
									<textarea style="color:gray">${ckname!}</textarea>
									<#elseif (element.element.showType) == 'DATE' || (element.element.showType) == 'DATETIME' || (element.element.showType) == 'TIME'>
									<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-calpick' />
									<#elseif (element.element.showType) == 'TEXTFIELD' || (element.element.showType) == 'BAPCODE' || (element.element.showType) == 'SUMMARY' ||  (element.element.showType) == 'PASSWORDFIELD' ||  (element.element.showType) == 'OUTDATA' || (element.element.showType) == 'COLOR'>
									<input type="text" style="color:gray;width:100%;" value="${ckname!}"/>
									<#elseif (element.element.showType) == 'MULTFILES'>
									<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-search-click' />
									<#elseif (element.element.showType) == 'SELECTCOMP' || (element.element.showType) == 'LAYERSELECTCOMP'>
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
										<table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td0">${getHtmlText('ec.view.num')}</td><td class="datagird_th_td1">${getHtmlText('ec.view.modelfield')}</td></tr></thead><tbody><tr><td class="datagird_tb_td0">1</td><td class="datagird_tb_td1"></td></tr></tbody></table>
									<#elseif (element.element.showType) == 'OFFICE'>
										<table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td1">${getHtmlText("office文档控件")}</td></tr></thead><tbody><tr><td class="datagird_tb_td1"></td></tr></tbody></table>
									<#elseif (element.element.showType) == 'line-bar'>
										<div class="echarts" style="width:99%;height:96%;position:relative;">
											<img src="/bap/static/ec/images/line-bar.gif" style="width:100%; height:100%;" />
										</div>
									</#if>
									</#if>
								</dd>
							</#if>
							<div></div>
						</li>
						
						<#else><#--pc视图-->
						
							<#if (element.element.showType) == 'LABEL'>
								<#if (propCode)?? && propertyMap?? && propertyMap[propCode]??>
									<#assign property = propertyMap[propCode] >
								<li class="form_design_ul_li" isTitle="${((element.element.isTitle)!true)?string}" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" key="${(element.element.key)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" propertyCode="${(element.element.propertyCode)!}" complex="${(element.element.complex)?string("true","false")}" <#if currentProperty=='undefined'>propnullable="${(property.nullable!false)?string('true','false')}"</#if> nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((property.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if>  <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}" showFormat="${(element.element.showFormat)!}"
						propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(element.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}" sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" 
						<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' 
						<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"</#if>
						<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" <#--convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
						isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
						acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
						</#if>>
								<#else>
								<li class="form_design_ul_li" isTitle="${((element.element.isTitle)!true)?string}" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" key="${(element.element.key)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" propertyCode="${(element.element.propertyCode)!}" complex="${(element.element.complex)?string("true","false")}" nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((element.element.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if>   <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}"  showFormat="${(element.element.showFormat)!}"
						 sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" 
						<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' 
						<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"</#if>
						<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" <#--convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
						isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
						acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
						</#if>>
								</#if>
								<dd >${getText('${((element.element.namekey)!element.element.name)!}')}</dd>
							<#else>
								<#if (propCode)?? && propCode!='' && propertyMap?? && propertyMap[propCode]??>
									<#assign property = propertyMap[propCode] >
								<li class="form_design_ul_li" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" defaultValue="<#if element.element.defaultValueHasChanged!false>${(element.element.defaultValue!)?string}<#else>${(property.defaultValue!)?string}</#if>" propDefaultValue="${(property.defaultValue)!}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" complex="${(element.element.complex)?string("true","false")}" nullable="<#if (element.element.nullable)?? && element.element.nullable>true<#else>false</#if>" <#if currentProperty=='undefined'>propnullable="${(property.nullable!true)?string('true','false')}" </#if> multable="${((property.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if> <#if (element.element.autoresize)??>autoresize="${(element.element.autoresize)?string('true','false')}"</#if>  <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol!false)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" isCustom="${(element.element.isCustom!false)?string('true','false')}" 
								  showType="<#if propCodes?size gt 1 || element.element.showTypeHasChanged!false>${(element.element.showType)!}<#else>${(property.fieldType!)}</#if>" showFormat="<#if propCodes?size gt 1 || element.element.showFormatHasChanged!false>${(element.element.showFormat)!}<#else>${(property.format!)}</#if>"  gns="${(element.element.gns)!}" us="${(element.element.us)!}" ids="${(element.element.ids)!}" ss="${(element.element.ss)!}" sns="${(element.element.sns)!}"
									<#if currentProperty?? && currentProperty?string != 'undefined'>
										objectPropertyNullable = "${currentProperty.nullable?string('true', 'false')}" 
									</#if>
						propShowType="${(property.fieldType!)}" propShowFormat="${(property.format!)}" showTypeHasChanged="${(element.element.showTypeHasChanged!false)?string('true','false')}" showFormatHasChanged="${(element.element.showFormatHasChanged!false)?string('true','false')}"  maxLength = "${property.maxLength!}"
						precision="<#if element.element.precisionHasChanged!false>${element.element.precision!}<#else>${(property.decimalNum!)}</#if>" propPrecision="${property.decimalNum!}" precisionHasChanged="${(element.element.precisionHasChanged!false)?string('true','false')}" 
									<#if (element.validate)??>validates = "<#list element.validate as validate><#if validate_index gt 0>~~~~</#if>${validate.type}<#if validate.param??><#list (validate.param)?keys as paramKey>~~${paramKey}~${validate.param[paramKey]?html}<#if paramKey=='errorMsg'>~~international_errorMsg_showName~${getText("${validate.param[paramKey]}")}</#if></#list></#if></#list>"</#if>
						sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(property.type)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" propertyCode="${(element.element.propertyCode)!}" picWidth="${(element.element.picWidth)!}" picHeight="${(element.element.picHeight)!}"
						<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' <#if property.fillcontent??>fill='${property.fillcontent!''?html}'</#if> 
						<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"</#if>
						<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" <#--convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
						isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
						acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
						</#if> <#if (element.element.layerType)??>layerType=${element.element.layerType!}</#if>>
								<#else>
								<li class="form_design_ul_li" cellCode="${cellCode!}" <#if (element.element.showType)?has_content>ondblclick="ec.cellProperty()" </#if> onmousedown="ec.chooseLi(this)" name="${(element.element.name)!}" couple="${((element.element.couple)!false && (!element.element.complex))?string('true','false')}" defaultValue="${(element.element.defaultValue!)?string}" propDefaultValue="${(element.element.defaultValue!)?string}" defaultValueHasChanged="${(element.element.defaultValueHasChanged!false)?string('true','false')}" complex="${(element.element.complex)?string("true","false")}" nullable="${((element.element.nullable)!false)?string('true','false')}" multable="${((element.element.multable)!false)?string('true','false')}" <#if (element.element.isgroup)??>isgroup="${(element.element.isgroup)?string('true','false')}"</#if> <#if (element.element.autoresize)??>autoresize="${(element.element.autoresize)?string('true','false')}"</#if> <#if (element.element.iscontrol)??>iscontrol="${(element.element.iscontrol)?string('true','false')}"</#if>  isreadonly="${(element.element.readonly)?string('true','false')}" showType="${(element.element.showType)!}" isCustom="${(element.element.isCustom!false)?string('true','false')}" gns="${(element.element.gns)!}" us="${(element.element.us)!}" ids="${(element.element.ids)!}" ss="${(element.element.ss)!}" sns="${(element.element.sns)!}"
									<#if currentProperty?? && currentProperty?string != 'undefined'>
										objectPropertyNullable = "${currentProperty.nullable?string('true', 'false')}" 
									</#if>
									<#if (element.validate)??>validates = "<#list element.validate as validate><#if validate_index gt 0>~~~~</#if>${validate.type}<#if validate.param??><#list (validate.param)?keys as paramKey>~~${paramKey}~${validate.param[paramKey]?html}<#if paramKey=='errorMsg'>~~international_errorMsg_showName~${getText("${validate.param[paramKey]}")}</#if></#list></#if></#list>"</#if>
						sourcepropertyname="${(element.sourcepropertyname)!}" topAssName="${(element.topAssName)!}" showFormat="${(element.element.showFormat)!}" callbackbody="${((element.callbackbody)!'')?html}" callbackname="${(element.callbackname)!}" funcname="${(element.funcname)!}" funcbody="${((element.funcbody)!'')?html}" selectCompType="${(element.element.selectCompType)!}" cssstyle="${((element.cssstyle)!'')?html}" cssstyle_container="${((element.cssstyle_container)!'')?html}" referenceview="${(element.element.referenceview)!}" multallowview="${((element.element.multallowview)!false)?string('true','false')}" allowview="${((element.element.allowview)!false)?string('true','false')}" allowviewcode="${(element.element.allowviewcode)!}" allowmultviewselect="${(element.element.allowmultviewselect)!}" modelcode="${(element.element.modelcode)!}" columnType="${(element.element.columnType)!}" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}" namekey="${(element.element.namekey)!}" propertyCode="${(element.element.propertyCode)!}"
						<#if (element.element.elalign)?has_content>elalign="${element.element.elalign}"</#if> <#if (element.element.refCondition)?has_content>refCondition="${(element.element.refCondition)?html}"</#if> isrefselect="${((element.element.isrefselect)!false)?string('true','false')}" mneenable="${((element.element.mneenable)!false)?string('true','false')}" staffselecttype="${(element.element.staffselecttype)!''}" directasso='${(element.element.directasso)!""}' indirectasso='${(element.element.indirectasso)!""}' displayfield='${(element.element.displayfield)!""}' fill='{<#if (element.element.fill)?has_content><#list (element.element.fill)?keys as fe><#if fe_index gt 0>,</#if>"${fe}":<#if (element.element.fill.fillType)?has_content && (element.element.fill.fillType?string) == '4' && fe == 'fillContent'>{<#if element.element.fill.fillOrder?has_content><#list element.element.fill.fillOrder?split(",") as ne><#if ne_index gt 0>,</#if>"${(ne)!}":"${(((element.element.fill.fillContent)[ne])!)?html}"</#list><#else><#list (element.element.fill.fillContent)?keys as ne><#if ne_index gt 0>,</#if>"${ne!}":"${(((element.element.fill.fillContent)[ne])!)?html}"</#list></#if>}<#else>"${((element.element.fill)[fe])?html}"</#if></#list></#if>}' 
						<#if (element.element.showType) == 'DATAGRID'>targetPropertyCode="${(element.element.targetPropertyCode)!}" DataGridCode="${(element.element.DataGridCode)!}" hideKey="${(element.element.hideKey)!}"</#if>
						<#if (element.element.showType) == 'OUTDATA'>outdata_database_url="${(element.element.outdata_database_url)!}" outdata_username="${(element.element.outdata_username)!}" outdata_password="${(element.element.outdata_password)!}" outdata_sql="${(element.element.outdata_sql)!}"</#if>
						<#if (element.element.showType) == 'OFFICE'>convertPdfOnLine="${((element.element.convertPdfOnLine)!true)?string('true','false')}" downloadDoc="${((element.element.downloadDoc)!false)?string('true','false')}" <#--convertPdf="${((element.element.convertPdf)!false)?string('true','false')}"--> isCreateNew="${((element.element.isCreateNew)!false)?string('true','false')}" isNoCopy="${((element.element.isNoCopy)!false)?string('true','false')}" isOfficeSign="${((element.element.isOfficeSign)!false)?string('true','false')}" isOfficeHandSign="${((element.element.isOfficeHandSign)!false)?string('true','false')}" saveTemplate="${((element.element.saveTemplate)!false)?string('true','false')}" openEmptyDoc="${((element.element.openEmptyDoc)!true)?string('true','false')}" signUrl="${(element.element.signUrl)!}" handSignUrl="${(element.element.handSignUrl)!}"
						isRevision="${((element.element.isRevision)!false)?string('true','false')}" showRevision="${((element.element.showRevision)!false)?string('true','false')}" hideRevision="${((element.element.hideRevision)!false)?string('true','false')}" getRevisions="${((element.element.getRevisions)!false)?string('true','false')}"
						acceptRevisions="${((element.element.acceptRevisions)!false)?string('true','false')}" officeNotNull="${((element.element.officeNotNull)!true)?string('true','false')}" officePrint="${((element.element.officePrint)!false)?string('true','false')}" officeOpenType="${(element.element.officeOpenType)!}" officeSaveType="${(element.element.officeSaveType)!}"
						</#if> <#if (element.element.layerType)??>layerType=${element.element.layerType!}</#if>>
								</#if>
								<dd>
									<#if  (element.element.showType)??>
									<#if (element.element.showType) == 'TEXTAREA' || (element.element.showType) == 'RICHTEXT'>
									<textarea style="color:gray">${ckname!}</textarea>
									<#elseif (element.element.showType) == 'DATE' || (element.element.showType) == 'DATETIME' || (element.element.showType) == 'TIME'>
									<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-calpick' />
									<#elseif (element.element.showType) == 'TEXTFIELD' || (element.element.showType) == 'BAPCODE' || (element.element.showType) == 'SUMMARY' ||  (element.element.showType) == 'PASSWORDFIELD' ||  (element.element.showType) == 'OUTDATA' || (element.element.showType) == 'COLOR'>
									<input type="text" style="color:gray;width:100%;" value="${ckname!}"/>
									<#elseif (element.element.showType) == 'MULTFILES'>
									<input type="text" style="color:gray;width:100%;height: 20px;line-height: 20px;margin-right: -5px;" value="${ckname!}"/><input type='button' class='cui-search-click' />
									<#elseif (element.element.showType) == 'SELECTCOMP' || (element.element.showType) == 'LAYERSELECTCOMP'>
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
										<table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td0">${getHtmlText('ec.view.num')}</td><td class="datagird_th_td1">${getHtmlText('ec.view.modelfield')}</td></tr></thead><tbody><tr><td class="datagird_tb_td0">1</td><td class="datagird_tb_td1"></td></tr></tbody></table>
									<#elseif (element.element.showType) == 'OFFICE'>
										<table cellspacing="0" cellpadding="0" style="margin-left:10px;width:98%"><thead><tr><td class="datagird_th_td1">${getHtmlText("office文档控件")}</td></tr></thead><tbody><tr><td class="datagird_tb_td1"></td></tr></tbody></table>
									<#elseif (element.element.showType) == 'line-bar'>
										<div class="echarts" style="width:99%;height:96%;position:relative;">
											<img src="/bap/static/ec/images/line-bar.gif" style="width:100%; height:100%;" />
										</div>
									</#if>
									</#if>
								</dd>
							</#if>
							<div></div>
						</li>
					</#if>
					<#else>
						<li class="form_design_ul_li" cellCode="${cellCode!}" onmousedown="ec.chooseLi(this)" colspan="${(element.colspan)!1}" rowspan="${(element.rowspan)!1}"><dd></dd><div></div></li>
					</#if>
					</#list>
					</#if>
				</ul>
			</#list>
		<#elseif layoutProperties.layoutContent=='echarts'>
			<div class="echarts" style="width:99%;height:96%;position:relative;">
				<img src="/bap/static/ec/images/line-bar.gif" style="width:100%; height:100%;" />
			</div>
			<script type="text/javascript">
			//4
			$(function(){
				var $echarts = $("[layoutname='${layoutname!}']");
				$echarts.css("background-color", "#ececec").attr("layoutContent","echarts");
				$echarts.attr('echartcode', '${echartCode!}');
				$echarts.attr('echarttype', '${echartType!}');
				$echarts.unbind('dblclick').dblclick(function(){
					ec.echartsConfig($echarts, true);
				});
			});
			</script>
		<#elseif layoutProperties.layoutContent=='datagrid'>
			<h3 style="margin-top:5px;font-weight:bolder;text-align:center;"></h3>
			<table class="dataGridTable" cellspacing="0" cellpadding="0" name="DataGrid-${layoutname!}" style="margin:0 auto;width:98%">
				<thead>
					<tr>
						<td class="datagird_th_td0">${getHtmlText('ec.view.num')}</td>
						<#local listPt = false>
						<#list layouts as secatt>
							<#if secatt.listPT?? && secatt.listPT>
								<#local listPt = true>
							</#if>
							<#if secatt.datagridCode?? && secatt.datagridCode?has_content>
								<#local datagridCode = secatt.datagridCode>
							</#if>
						</#list>
						<td class="datagird_th_td1">
						<#if datagridCode??>
							<#if orgPropertyMap?has_content && orgPropertyMap[datagridCode]?has_content>
								${getText('${orgPropertyMap[datagridCode].model.name}')}[${getText('${orgPropertyMap[datagridCode].displayName}')}]
							<#elseif targetModelMap?has_content && targetModelMap[datagridCode]?has_content>
								${getText('${targetModelMap[datagridCode].name}')}
							</#if>
						</#if>
						 （<#if listPt?? && !listPt>DataGrid<#else>DataTable</#if>-${layoutname!}） </td>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td class="datagird_tb_td0">1</td>
						<td class="datagird_tb_td1"></td>
					</tr>
				</tbody>
			</table>
			<script type="text/javascript">
			//5
			$(function(){
				var $datagrid = $("[layoutname='${layoutname!}']");
				$datagrid.css("background-color", "#ececec").attr("layoutContent","datagrid");
				<#list layouts as secatt>
					$datagrid.attr('columntype',"DATAGRID").attr('showtype',"DATAGRID");
					<#if secatt.formLayoutName??>$datagrid.attr('formLayoutName',"${secatt.formLayoutName?string}");</#if>
					<#if secatt.complex??>$datagrid.attr('complex',"${secatt.complex?string}");</#if>
					<#if secatt.isborder??>$datagrid.attr('isborder',"${secatt.isborder}");</#if>
					<#if secatt.cssstyle??>$datagrid.attr('cssstyle',"${secatt.cssstyle}");</#if>
					<#if secatt.listPT??>$datagrid.attr('listPT',"${secatt.listPT?string}");</#if>
					<#if secatt.regionType??>$datagrid.attr('regionType',"${secatt.regionType}");</#if>
					<#if secatt.datagridCode??>$datagrid.attr('datagridCode',"${secatt.datagridCode}");</#if>
					<#if secatt.cells?? > 
						<#list secatt.cells as cell >
							<#if cell.cellCode?? >$datagrid.attr('cellCode',"${cell.cellCode}");</#if>
							<#if cell.hideKey??>$datagrid.attr("hideKey","${cell.hideKey!}");</#if>
							<#if cell.targetPropertyCode??>$datagrid.attr("targetPropertyCode","${cell.targetPropertyCode!}");</#if>
							<#if cell.targetModelCode??>$datagrid.attr("targetModelCode","${cell.targetModelCode!}");</#if>
						</#list>
					</#if>
				</#list>
				$datagrid.unbind('dblclick').dblclick(function(){
					ec.layoutProperty($(this));
				});
			});
			</script>
		<#elseif layoutProperties.layoutContent=='searchWidget'>
			<script type="text/javascript">
			//6
				$(function(){
					var $searchWidget = $("[layoutname='${layoutname!}']");
			<#if layoutProperties.datagridCode??>
				<#assign dgCodes= "" >
				<#assign dgCodesHasStart = false>
				<#list layoutProperties.datagridCode as code>
					<#if dgCodesHasStart>
						<#assign dgCodes = dgCodes + "," >		
					</#if>
					<#assign dgCodes = dgCodes + code.code >
					<#assign dgCodesHasStart = true >
				</#list>
					$searchWidget.removeAttr("dgCodes").attr("dgCodes","${dgCodes}");
			</#if>
			<#if layoutProperties.targetmodelcode??>
					$searchWidget.removeAttr("targetmodelcode").attr("targetmodelcode","${layoutProperties.targetmodelcode}");
			</#if>
			<#if layoutProperties.targetmodelname??>
					$searchWidget.removeAttr("targetmodelname").attr("targetmodelname","${layoutProperties.targetmodelname}");
					$searchWidget.removeAttr("targetmodelnametext").attr("targetmodelnametext","${getText(layoutProperties.targetmodelname)}");
			</#if>			
			<#if layoutProperties.fqjCode??>
					$searchWidget.removeAttr("fqjcode").attr("fqjcode","${layoutProperties.fqjCode}");
			</#if>
			<#if layoutProperties.aqjCode??>
					$searchWidget.removeAttr("aqjcode").attr("aqjcode","${layoutProperties.aqjCode}");
			</#if>
				});
			</script>
			<div class="searchBox">
				<div style="width:100%;height:100%;">
					<div class="search-config-wrapper">
						<button id="dcbutton" class="search-config-button" onclick="ec.dataClassific(this)" title="${getText('ec.view.list.dcsetclick')}">${getHtmlText('ec.view.list.dcset')}</button>
					</div>
					<div class="search-config-wrapper">
						<button id="fqsbutton" class="search-config-button" onclick="ec.fastQuerySetting(this)" title="${getText('ec.view.list.fastsetclick')}">${getHtmlText('ec.view.list.fastset')}</button>
					</div>
					<div class="search-config-wrapper">
						<button id="advbutton" class="search-config-button" onclick="ec.advQuerySetting(this)" title="${getText('ec.view.list.advsetclick')}">${getHtmlText('ec.view.list.advset')}</button>
					</div>
					<div class="search-config-wrapper">
						<button class="search-config-button" onclick="ec.dataAssocSetting(this)" title="数据关联配置">数据关联配置</button>
					</div>
				</div>
			</div>
		<#elseif layoutProperties.layoutContent=='webFrame'>
			<div class='urlBox'><img src='/bap/static/images/urlImg.png' /></div>
		<#elseif layoutProperties.layoutContent=='layer'>
			<div class="echarts" style="width:99%;height:96%;position:relative;">
				<img src="/bap/static/ec/images/map.png" style="width:100%; height:100%;" />
			</div>
			<#if layoutProperties?? && layoutProperties.cellCode??>
				<#local cellCode = layoutProperties.cellCode >
			<#else>
				<#local cellCode = "" >
			</#if>
			<script type="text/javascript">
			//7
			$(function(){
				var $layer = $("[layoutname='${layoutname!}']");
				$layer.css("background-color", "#ececec").attr("layoutContent","layer");
				$layer.attr('layertype', '${layertype!}');
				$layer.attr('layerpropertycode', '${layerpropertycode!}');
				$layer.attr('layerName', '${layerName!}');
				$layer.attr('layerNameKey', '${layerNameKey!}');
				$layer.attr('cellCode', '${cellCode!}');
				$layer.unbind('dblclick').dblclick(function(){
					ec.layerConfig($layer, true);
				});
			});
			</script>
		</#if> 
	</#if>
</#macro>