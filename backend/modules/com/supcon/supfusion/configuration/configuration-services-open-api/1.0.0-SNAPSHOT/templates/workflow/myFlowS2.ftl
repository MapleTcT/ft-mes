<div id="myflowRemind" class="rowItemDiv">
 <div class="mod modBody">
  <table width="100%" cellpadding = '0' cellspacing = '0'>
   <tbody>
    <tr>
     <td id="content">

      <div class="port-panes">
       <div class="tabcontent" style="display:block;">
        <#if tableinfos?exists>
        	<#if tableinfos["pending"]?exists>
        	 <ul id="remindList" class="port-list">
				<#list tableinfos["pending"] as info>
			    <li title="${info['CREATE_TIME']?string("yyyy-MM-dd")} ${info['SNAME']?default('')} ${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))}) ${info['STATUS']?default('')}" onclick="foundation.openPending('${info['URL']?default('')}')">
					<span style="width:20%;"  >${info['CREATE_TIME']?string("yyyy-MM-dd")}</span>
					<span style="width:20%;" >${info['SNAME']?default('')}</span>
					<span style="width:42%;"  >${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))})</span>
					<span style="width:18%;"  >${info['STATUS']?default('')}</span>
				</li>
			    </#list>
			  </ul>
			 <#else>
				<div align="" style="padding-top: 8px;padding-bottom: 4px; text-align: center;"> 
				${getHtmlText('foundation.workflow.nopending')}</div>
		    </#if>
		</#if>
       </div>
       <div id="myworkflowon_div" class="tabcontent" style="display:none;">
			<table style="width:100%;height:100%;text-align:center;margin-top:5px;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>
       </div>
       <div id="myworkflowover_div" class="tabcontent" style="display:none;">
			<table style="width:100%;height:100%;text-align:center;margin-top:5px;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>
       </div>
       <div id="myworkflowhandle_div" class="tabcontent" style="display:none;">
			<table style="width:100%;height:100%;text-align:center;margin-top:5px;"><tr><td><label class="datagrid-loading">${getText("common.text.loading")}</label></td></tr></table>
       </div>
      </div>

 </div>   
</div>
<script type="text/javascript" language="javascript" charset="utf-8">
//注册命名空间
CUI.ns("foundation");

</script>
<script type="text/javascript">
foundation.openPending =function(url){
	if(url==''||url== undefined ){
   		CUI.Dialog.alert("${getHtmlText('foundation.workflow.noview')}");
   		return false;
   	}
   	var window_height = window.screen.availHeight-63;
   	var window_width  = window.screen.availWidth-20;
   	var ShowStyle = "width="+window_width+",height="+window_height+",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
   	window.open(url,"",ShowStyle);	
};
foundation.openDealinfoURL = function(ID,entityCode,status,targetTablename){
	if(status===88){
		foundation.waittodo(ID,entityCode,status);
	}else{
		foundation.opentableinfoURL(ID,entityCode,targetTablename);
	}
};
foundation.waittodo = function(ID,entityCode,status){
	var url="/msService/ec/myWorkflow/openURL.action";
	var data="tableInfoId="+ID+"&entityCode="+entityCode+"&status="+status;
	//console.log(data);
	CUI.ajax({
	   type: "POST",
	   url: url,
	   data:data,
	   success: function(res){
		   // alert(res);
			if(res=="noview"){
					CUI.Dialog.alert("${getHtmlText('foundation.workflow.noview')}");
					return false;
				}
				var window_height = window.screen.availHeight-63;
		   		var window_width  = window.screen.availWidth-20;
		   	    var ShowStyle = "width="+window_width+",height="+window_height+",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		   		//foundation.staff.currentEditStaffId = staffListWidget.selectedRows[0].STAFFID;
		   	   // var openurl = '/foundation/staff/view/default.action?staffId='+staffListWidget.selectedRows[0].STAFFID;
		   		window.open(res,"",ShowStyle);				   				
		}
	});
};
foundation.opentableinfoURL = function(ID,entityCode,targetTablename){
	var url="/msService/ec/myWorkflow/openURL.action";
	var data="tableInfoId="+ID+"&entityCode="+entityCode+"&status=99&targetTablename="+targetTablename;
	//console.log(data);
	CUI.ajax({
	   type: "POST",
	   url: url,
	   data:data,
	   success: function(res){
		   // alert(res);
			if(res=="noview"){
					CUI.Dialog.alert("${getHtmlText('foundation.workflow.noview')}");
					return false;
				}
				var window_height = window.screen.availHeight-63;
		   		var window_width  = window.screen.availWidth-20;
		   	    var ShowStyle = "width="+window_width+",height="+window_height+",scrollbars=yes,top=0,left=0,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		   		//foundation.staff.currentEditStaffId = staffListWidget.selectedRows[0].STAFFID;
		   	   // var openurl = '/foundation/staff/view/default.action?staffId='+staffListWidget.selectedRows[0].STAFFID;
		   		window.open(res,"",ShowStyle);				   				
		}
	});
};
foundation.clickformore = function(type){
	var url="";
	if(type=='on'){
		url="/msService/ec/myWorkflow/frame.action?selectOn=true";
	}else if(type=="over"){
		url="/msService/ec/myWorkflow/frame.action?selectOver=true";
	}else if(type=="handle"){
		url="/msService/ec/myWorkflow/frame.action?selectHandle=true";
	}else{
		url="/msService/ec/myWorkflow/frame.action?selectPending=true";
	}
	
	// 判断横版
	if(  $( '#v3_main_menu' ).length > 0 ){
		$( '#v3_main_menu li[code="myFlowManageWorkBench"]' ).trigger( "click" );
		$( '#v3_menu_list li[code="my_workflow"]' ).addClass( "v3_menu_current" );	
		CUI.loadPage( { url: url } );
		return;
	}	
	
	var menuCode="my_workflow";
	var parentCode="myFlowManageWorkBench";
	var pli = $("#main-menu li[code='" + menuCode +"']").data("pli");
	if (pli) {
		var dt = pli.data("dt");
		__addRootMenu(dt, $("#item-box"), menuCode);
		location.hash='{"url":"'+url+'","code":"' + menuCode + '","parentCode":"'+parentCode+'"}'
	}
	loadPage({url:url});
};
$("li",$(".port-tabs", "#myflowRemind")).each(function(i){
   $(this).click(function(){
      $(this).addClass("current").siblings().removeClass("current");
      $('.tabcontent').eq(i).show().siblings().hide();
  });
});
foundation.pageinit = function(){
	var ws = [];
	 var portTabs = $('.port-tabs','#myflowRemind');
	    $("li",portTabs).each(function(index){
	        ws.push(this.offsetWidth+2);//2为li的margin值
	    });
	    var liW = 0;
	    var portW = $("#myflowRemind").parent().prev().width();
	 for(var i=0;i<ws.length;i++){
	  liW += ws[i];
	 }
	 liW+=18;
	 portTabs.attr('_oWidth',liW);
	 if(liW >= portW){
	  $("li",portTabs).each(function(){
	   this.style.width = parseInt(((portW-24)/$("li",portTabs).length-13),10) + 'px';
	  });
	 }
	 $(".portlet-content").each(function(){
	  $("#remindList li",this).height("34px");
	   if($.browser.msie && ($.browser.version == "6.0") && !$.support.style){
		$("#remindList li",this).width($(this).width()-18);
	   }else{
	   	$("#remindList li",this).width($(this).width() - 20);
	   }
	 });
};
$(function(){
	 foundation.pageinit();
	 $('li[id]', '.port-tabs').one('click', function(){
	 	var id=$(this).attr('id');
	 	$('#' + id + '_div').load($(this).attr('url'), null, function(){
	 		foundation.pageinit();
	 	});
	 });
})

</script>