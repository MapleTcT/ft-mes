
<#if preName??&&preName=='mainFrame'>
	<@s.hidden name="expandDealInof" id="${preName!}expandDealInof"></@s.hidden>
	<@s.hidden name="dlTableInfoId" id="${preName!}dlTableInfoId"></@s.hidden>
	<@s.hidden name="dealDataUrl" id="${preName!}dealDataUrl"></@s.hidden>
	<div class="col-xs-12 col-sm-12 col-md-12 col-lg-12"> 
		<label>${getText("ec.view.dealadvice")}</label>
	</div>
	
	<div class="margin-bottom-5">
		<div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
			<div id="${preName!}dealTable" style="border:1px solid #e5e5e5;">
				<div typeName="none" style="display:none;" class="col-xs-12 col-sm-12 col-md-12 col-lg-12 chuli-info">
					<div>${getText('ec.view.none')}</div>
				</div>
				<div typeName="normal" style="display:none;" class="col-xs-12 col-sm-12 col-md-12 col-lg-12 chuli-info">
					<div class="person-info"></div><div class="remain-time"></div>
					<div class="operate-info">
						<span class="operate-time"></span>
						${getText('ec.flowActive.doIt')}
						<span class="operate-name"></span>
						${getText('ec.flowActive.doperate')}
						<span class="operate-entrust"></span>
						${getText('foundation.common.dh')}
						${getText('ec.view.dealeffortcontent')}
						<span class="operate-result"></span>
						<span class="operate-end"></span>
					</div>
					<div class="operate-info deal-info"></div>
				</div>
				<div typeName="expect" style="display:none;" class="col-xs-12 col-sm-12 col-md-12 col-lg-12 chuli-info">
					<div class="person-info"></div><div class="remain-time"></div>
					<div class="operate-info">
						<span class="operate-time"></span>
						${getText('ec.flowActive.doItByExpect')}
						<span class="operate-name"></span>
					</div>
					<div class="operate-info deal-info"></div>
				</div>
			</div>
		</div>
	</div>
	
	
	<script type="text/javascript">
		function ${preName!}showDeal(flag){
			var secd=0;
			var url=$("#${preName!}dealDataUrl").val();
			$.ajax({
				url:url,
				type: 'post',
				async: false,
				data: {"tableInfoId":$("#${preName!}dlTableInfoId").val(),"expandDealInof":flag},
				success: function(response) {
					if( !response || response.length == 0 ){			
						$('#${preName!}dealTable div[typeName="none"]').show();				
						return ;
					}
					for(var i=0;i<response.length;i++){
						var thisDay=response[i][1];
						var thisTime="";
						var exp="";
						if(thisDay){
							var d = new Date(thisDay);
							if(d != 'NaN'){
								thisTime=d.format('yyyy-MM-dd hh:mm:ss');
								var cal=0;
								if(i!=0){
									cal=parseInt(thisDay,10)-parseInt(secd,10);
								}
								exp=traTimeFunc(cal);
							}
						}else{
							thisDay='';
						}
						if(exp!=""){
							exp="${getText('ec.view.flowstop')}"+exp;
						}else{
							exp="${getText('ec.view.stopone')}";
						}
						var dealInfo="${getText('common.button.save')}";
						if(response[i][5]!=null&&response[i][5]!="null"&&response[i][5].length > 0){
							dealInfo=response[i][5];
						} else if (response[i][11] == 5) { //通知活动处理结果为确认
							dealInfo = "${getHtmlText('ec.dealInfo.confirm')}";
						}
						var div = null;
						if(response[i][7]!=null&&response[i][7]=="EXPECTEDCONSIGNOR"){
							div = $('div[typeName="expect"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][6]);
						} else if(response[i][7]!=null&&response[i][7]=="ENTRUST"){
							// 处理委托的待办
							div = $('div[typeName="normal"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][0]);
							$('.operate-entrust', div).html("(${getText('ec.flowActive.dai')}" + response[i][8] + ")");
							$('.operate-result', div).html(dealInfo);
							response[i][6] = null;
						} else if (response[i][7] != null && (response[i][7] == "NORMAL_AND_EXPECTEDCONSIGN" || response[i][7] == "FORWARD_AND_EXPECTEDCONSIGN")) {
							//处理“并代”的待办
							div = $('div[typeName="normal"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][0]);
							$('.operate-entrust', div).html("(${getText('ec.flowActive.bingdai')}" + response[i][8] + ")");
							$('.operate-result', div).html(dealInfo);
						} else{
							// 处理非委托待办
							div = $('div[typeName="normal"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][0]);
							if (response[i][8] != null && response[i][8] != '') {
								$('.operate-entrust', div).html("(${getText('ec.flowActive.dai')}" + response[i][8] + ")");
							}
							$('.operate-result', div).html(dealInfo);
						}
						if(response[i][7]!=null && (response[i][7]=="FORWARD" || response[i][7] == "FORWARD_AND_EXPECTEDCONSIGN")){
							$('.operate-end', div).html('${getText("foundation.common.dh")}</span> ${getText("ec.assignStaff.Forward")}<span  style="font-weight:bold;">'+response[i][6]+'</span>${getText("foundation.common.bd")}');
						}else if(response[i][7]!=null&&response[i][7]=="EXPECTEDCONSIGNOR"){
							$('.operate-end', div).html('${getText("foundation.common.bd")}</span>');
						}else if(response[i][6]!=null&&response[i][6]!=""){
							$('.operate-end', div).html('${getText("foundation.common.dh")}</span> ${getText("ec.assignStaff.assign")}<span  style="font-weight:bold;">'+response[i][6]+'</span>${getText("ec.assignStaff.deal")}${getText("foundation.common.bd")}');
						}else{
							$('.operate-end', div).html('${getText("foundation.common.bd")}</span>');
						}
						$('.deal-info', div).html(response[i][2]==null?'':response[i][2]);
						$('.remain-time', div).html(exp);
		
						$('#${preName!}dealTable').append(div);
						div.show();
						
						secd=thisDay;
					}
				}
			});
		}
		$(function(){
			${preName!}showDeal(true);
		});
	</script>
	
<#else>
	<style>
		.no-dealinfo-style{
			height:100%;
			text-align:center;
			font-size:30px;
			color:#c2c2c2;
		}
	</style>
	<div id="viewport-dealinfo-container">
		<@s.hidden name="expandDealInof" id="${preName!}expandDealInof"></@s.hidden>
		<@s.hidden name="dlTableInfoId" id="${preName!}dlTableInfoId"></@s.hidden>
		<@s.hidden name="dealDataUrl" id="${preName!}dealDataUrl"></@s.hidden>
		<div class="col-xs-12 col-sm-12 col-md-12 col-lg-12"> 
			<label>${getText("ec.view.dealadvice")}</label>
		</div>
		
		<div class="margin-bottom-5">
			<div class="col-xs-12 col-sm-12 col-md-12 col-lg-12">
				<div id="${preName!}dealTable" style="border:1px solid #e5e5e5;">
					<div typeName="none" style="display:none;" class="col-xs-12 col-sm-12 col-md-12 col-lg-12 chuli-info">
						<div>${getText('ec.view.none')}</div>
					</div>
					<div typeName="normal" style="display:none;" class="col-xs-12 col-sm-12 col-md-12 col-lg-12 chuli-info">
						<div class="person-info"></div><div class="remain-time"></div>
						<div class="operate-info">
							<span class="operate-time"></span>
							${getText('ec.flowActive.doIt')}
							<span class="operate-name"></span>
							${getText('ec.flowActive.doperate')}
							<span class="operate-entrust"></span>
							${getText('foundation.common.dh')}
							${getText('ec.view.dealeffortcontent')}
							<span class="operate-result"></span>
							<span class="operate-end"></span>
						</div>
						<div class="operate-info deal-info"></div>
					</div>
					<div typeName="expect" style="display:none;" class="col-xs-12 col-sm-12 col-md-12 col-lg-12 chuli-info">
						<div class="person-info"></div><div class="remain-time"></div>
						<div class="operate-info">
							<span class="operate-time"></span>
							${getText('ec.flowActive.doItByExpect')}
							<span class="operate-name"></span>
						</div>
						<div class="operate-info deal-info"></div>
					</div>
				</div>
			</div>
		</div>
	</div>
	<script type="text/javascript">
		function ${preName!}showDeal(flag){
			var secd=0;
			var url=$("#${preName!}dealDataUrl").val();
			$.ajax({
				url:url,
				type: 'post',
				async: false,
				data: {"tableInfoId":$("#${preName!}dlTableInfoId").val(),"expandDealInof":flag},
				success: function(response) {
					if( !response || response.length == 0 ){
						$( '#viewport-dealinfo-container' ).addClass( 'no-dealinfo-style' ).html( '<div id="no-dealinfo-box" style="padding-top:' + ( $( '#viewport-dealinfo-container' ).height() / 2 - 40 ) + 'px">${getText("foundation.pending.nodealinfo")}</div>' );		
						if( !CUI.isLargeScreen ) {
							$( '#no-dealinfo-box' ).css( { 'font-size': '20px', 'padding-top': $(window).height() / 2 - 50 } );
						}
						return ;
					}
					for(var i=0;i<response.length;i++){
						var thisDay=response[i][1];
						var thisTime="";
						var exp="";
						if(thisDay){
							var d = new Date(thisDay);
							if(d != 'NaN'){
								thisTime=d.format('yyyy-MM-dd hh:mm:ss');
								var cal=0;
								if(i!=0){
									cal=parseInt(thisDay,10)-parseInt(secd,10);
								}
								exp=traTimeFunc(cal);
							}
						}else{
							thisDay='';
						}
						if(exp!=""){
							exp="${getText('ec.view.flowstop')}"+exp;
						}else{
							exp="${getText('ec.view.stopone')}";
						}
						var dealInfo="${getText('common.button.save')}";
						if(response[i][5]!=null&&response[i][5]!="null"&&response[i][5].length > 0){
							dealInfo=response[i][5];
						} else if (response[i][11] == 5) { //通知活动处理结果为确认
							dealInfo = "${getHtmlText('ec.dealInfo.confirm')}";
						}
						var div = null;
						if(response[i][7]!=null&&response[i][7]=="EXPECTEDCONSIGNOR"){
							div = $('div[typeName="expect"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][6]);
						} else if(response[i][7]!=null&&response[i][7]=="ENTRUST"){
							// 处理委托的待办
							div = $('div[typeName="normal"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][0]);
							$('.operate-entrust', div).html("(${getText('ec.flowActive.dai')}" + response[i][8] + ")");
							$('.operate-result', div).html(dealInfo);
							response[i][6] = null;
						}  else if (response[i][7] != null && (response[i][7] == "NORMAL_AND_EXPECTEDCONSIGN" || response[i][7] == "FORWARD_AND_EXPECTEDCONSIGN")) {
							//处理“并代”的待办
							div = $('div[typeName="normal"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][0]);
							$('.operate-entrust', div).html("(${getText('ec.flowActive.bingdai')}" + response[i][8] + ")");
							$('.operate-result', div).html(dealInfo);
						} else{
							// 处理非委托待办
							div = $('div[typeName="normal"]', $('#${preName!}dealTable')).clone();
							div.removeAttr('typeName');
							$('.person-info', div).html(response[i][4]);
							$('.operate-time', div).html(thisTime);
							$('.operate-name', div).html(response[i][0]);
							if (response[i][8] != null && response[i][8] != '') {
								$('.operate-entrust', div).html("(${getText('ec.flowActive.dai')}" + response[i][8] + ")");
							}
							$('.operate-result', div).html(dealInfo);
						}
						if(response[i][7]!=null && (response[i][7]=="FORWARD" || response[i][7] == "FORWARD_AND_EXPECTEDCONSIGN")){
							$('.operate-end', div).html('${getText("foundation.common.dh")}</span> ${getText("ec.assignStaff.Forward")}<span  style="font-weight:bold;">'+response[i][6]+'</span>${getText("foundation.common.bd")}');
						}else if(response[i][7]!=null&&response[i][7]=="EXPECTEDCONSIGNOR"){
							$('.operate-end', div).html('${getText("foundation.common.bd")}</span>');
						}else if(response[i][6]!=null&&response[i][6]!=""){
							$('.operate-end', div).html('${getText("foundation.common.dh")}</span> ${getText("ec.assignStaff.assign")}<span  style="font-weight:bold;">'+response[i][6]+'</span>${getText("ec.assignStaff.deal")}${getText("foundation.common.bd")}');
						}else{
							$('.operate-end', div).html('${getText("foundation.common.bd")}</span>');
						}
						$('.deal-info', div).html(response[i][2]==null?'':response[i][2]);
						$('.remain-time', div).html(exp);

						$('#${preName!}dealTable').append(div);
						div.show();
					
						secd=thisDay;
					}
				}
			});
		}
		$(function(){
			${preName!}showDeal(true);
		});
	</script>
</#if>
