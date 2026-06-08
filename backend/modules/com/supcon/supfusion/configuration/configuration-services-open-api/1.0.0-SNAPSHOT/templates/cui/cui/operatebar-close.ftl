<#if parameters.buttons?has_content>
	<#list parameters.buttons as btn>
	<a href="#" id="${btn['CODE']?default('')}" class="cui-btn mr10 ${btn['ICONCLS']?default("")}" onclick="${btn['ONCLICK']?default('')}">${btn['NAME']?default('')}</a>	
	</#list>

</#if>
<script type="text/javascript">

	function operateBarOnclickFoundation(url){
		
	    var window_height = window.screen.availHeight-63;
		var window_width  = window.screen.availWidth-20;
	    var ShowStyle = "width="+window_width+",height="+window_height+",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		window.open(url,'',ShowStyle);	
		
	}
</script>