<#if parameters.text??>
	<div class="cui-main-title-l">
		<span>${parameters.text}</span>
		<span id="localLoadPanel"></span>
	</div>
</#if>
<script type="text/javascript">
//localFlag：用于指定loadpanel的类型，默认为true
var localLoadPanelWidget = null;
function createLoadPanel(localFlag,containerEl){
	try{
		if(localFlag == undefined) localFlag = true;
		
		if(localFlag == false){
			containerEl = (containerEl == undefined) ? window : containerEl;
			containerLoadPanelWidget = new CUI.loading({container:containerEl});
		}else{
			if(localLoadPanelWidget == null) localLoadPanelWidget = new CUI.loading({local:localFlag,prevEl:'localLoadPanel',paddingLeft:${parameters.paddingLeft}});
		}
	}catch(e){}

}

//关闭的时候，由于无法判断目前出现的loadpanel类型，因此local和container两者都去关闭一下，并将错误捕获
function closeLoadPanel(){
	try{
		localLoadPanelWidget.close();
		localLoadPanelWidget = null;
	}catch(e){}
	try{
		containerLoadPanelWidget.close();
	}catch(e){}
}

</script>