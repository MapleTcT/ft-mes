<#-- ==================================== Load Panel ==================================== -->
<#--为了兼容老版本，增加newType参数，true需要生成div，如果是自定义代码就不生成（原先生成在页面里） -->
<#macro loadpanel text="",paddingLeft=0 newType=false>
	<#if text!="">
		<div class="cui-main-title-l">
			<span>${text}</span>
			<span id="localLoadPanel"></span>
		</div>
	</#if>
	<#if newType>
		<div id="load_mask_first" class="load_mask_first"></div>
		<iframe id="load_iframe_ie_first" class="load_iframe_ie" style="z-index: 199; display: block;"></iframe>
		<div id="loading_wrap_first" class="loading_wrap_first">
			<div class="loading_process">
				<div class="loading_msg_first">${getText("foundation.common.data.waiting")}</div>
			</div>
		</div>
	</#if>
	<script type="text/javascript">
	//localFlag：用于指定loadpanel的类型，默认为true
	var localLoadPanelWidget = null;
	function createLoadPanel(localFlag,containerEl,_config){
		try{
			if(localFlag == undefined) localFlag = true;
			if(localFlag == false){
				//containerEl = (containerEl == undefined) ? window : containerEl;
				if(!_config) {
					_config = {container:containerEl/*,opacity:50,bgColor:"#666666"*/};
				} else {
					_config.container = containerEl;
				}
				if(!window.containerLoadPanelWidget){
					window.containerLoadPanelWidget = new CUI.loading(_config);
			    }
			}else{
				if(localLoadPanelWidget == null) localLoadPanelWidget = new CUI.loading({local:localFlag,prevEl:'localLoadPanel',paddingLeft:${paddingLeft}});
			}
		}catch(e){}
	}
	//关闭的时候，由于无法判断目前出现的loadpanel类型，因此local和container两者都去关闭一下，并将错误捕获
	function closeLoadPanel(){
		try{
			localLoadPanelWidget.close();
			localLoadPanelWidget = null;
		}catch(e){localLoadPanelWidget = null;}
		try{
			window.containerLoadPanelWidget.close();
			window.containerLoadPanelWidget = null;
		}catch(e){window.containerLoadPanelWidget = null;}
	}
	</script>
</#macro>