<div id="birthdayRemind" class="rowItemDiv">
	<div class="mod modBody">
		<table width="100%">
			<tbody>
				<tr>
					<td height="100%" id="content">
						<ul id="remindList">
							<li><span onclick="foundation.pendingRemind.waittodo(1)" style="cursor:pointer;">${getHtmlText('ec.remind.pending.today')} <span title="${getText('ec.remind.pending.clickshow.todoList')}" class="rednum">${responseMsg['1']?default(0)}</span> ${getHtmlText('ec.remind.pending.item')}</span></li>
							<li><span onclick="foundation.pendingRemind.waittodo(7)" style="cursor:pointer;">${getHtmlText('ec.remind.pending.thisWeek')} <span title="${getText('ec.remind.pending.clickshow.todoList')}" class="rednum" >${responseMsg['7']?default(0)}</span> ${getHtmlText('ec.remind.pending.item')}</span></li>
							<li><span onclick="foundation.pendingRemind.waittodo()" style="cursor:pointer;">${getHtmlText('ec.remind.pending.all')} <span title="${getText('ec.remind.pending.clickshow.todoList')}" class="rednum" >${responseMsg['all']?default(0)}</span> ${getHtmlText('ec.remind.pending.item')}</span></li>
						</ul>
					</td>
				</tr>
			</tbody>
		</table>
	</div>			
</div>
<script type="text/javascript" language="javascript" charset="utf-8">
//注册命名空间
CUI.ns("foundation.pendingRemind");
foundation.pendingRemind.waittodo = function(type){
	var url = "/foundation/workbench/pendings.action";
	if(type=='1') {
		url+="?beginDate="+foundation.pendingRemind.dateChange(0)+"&endDate="+foundation.pendingRemind.dateChange(0);
	} else if(type=='7') {
		url+="?beginDate="+foundation.pendingRemind.dateChange(6)+"&endDate="+foundation.pendingRemind.dateChange(0);
	}
	var menuCode="mypendingWorkbench";
	var parentCode="myFlowManageWorkBench";
	// 判断横版
	if(  $( '#v3_main_menu' ).length > 0 ){
		$( '#v3_main_menu li[code="' + parentCode + '"]' ).trigger( "click" );
		$( '#v3_menu_list li[code="' + menuCode + '"]' ).addClass( "v3_menu_current" );	
		CUI.loadPage( { url: url } );
		return;
	}	
	var pli = $("#main-menu li[code='" + menuCode +"']").data("pli");
	if (pli) {
		var dt = pli.data("dt");
		__addRootMenu(dt, $("#item-box"), menuCode);
		location.hash='{"url":"'+url+'","code":"' + menuCode + '","parentCode":"'+parentCode+'"}'
	}
	loadPage({url:url});
};
foundation.pendingRemind.dateChange = function(dateAdd) {
	var a = new Date();
	a = a.valueOf();
	a = a - dateAdd * 24 * 60 * 60 * 1000;
	a = new Date(a);
	return a.getFullYear()+"-"+(a.getMonth()+1)+"-"+a.getDate();
};
</script>