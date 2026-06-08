<style type="text/css">
	.table-td-label{
		width:50px;
		border-bottom:1px solid #000;
		border-right:1px solid #000;
		line-height: 20px;
		padding: 4px;
		text-align: center;
	}
	.table-td-label-last, .table-td-line-last{
		border-bottom:0;
	}
	.opinions-content{
		border:1px solid #000000;
		border-bottom:0;
		border-left:0 \9\0;
		overflow-y: hidden;
	}
	.opinions-content table{
		float: left;
		 width: 806px; 
		/*width: 788px;*/
	}
	.opinions-label{
		width:155px;
		height:20px;
		border:1px solid #000;
		margin:6px auto 0;
		border-radius:1px;
	}
	.opinions-tab{
		display:inline-block;
		float:left;
		width:77px;
		height:20px;
		line-height:20px;
		text-align: center;
		color:#93acbb;
		cursor: pointer;
	}
	.opinions-tab-current{
		background:#dbf2ff;
		color:#227fb4;
	}
	.opinions-icon-th-list{
		background:url( '/bap/static/css/buttons.png' ) no-repeat  10px -221px ;
		_background:url( '/bap/static/css/buttons.gif' ) no-repeat  10px -221px ;
	}
	.opinions-icon-list{
		background:url( '/bap/static/css/buttons.png' ) no-repeat  10px -281px ;
		_background:url( '/bap/static/css/buttons.gif' ) no-repeat  10px -281px ;
	}
	.opinions-icon-th-list-current{
		background:url( '/bap/static/css/buttons.png' ) no-repeat  10px -250px ;
		_background:url( '/bap/static/css/buttons.gif' ) no-repeat  10px -250px ;
	}
	.opinions-icon-list-current{
		background:url( '/bap/static/css/buttons.png' ) no-repeat  10px -315px;
		_background:url( '/bap/static/css/buttons.gif' ) no-repeat  10px -315px ;
	}
	.opinions-table-td{
		border-left:1px solid #000;
		border-bottom:1px solid #000;
		height:29px;
	}
	.opinions-table-td-outcome{
		float:left;
		padding:9px;
		/*background-color:#efefef;*/
		border-radius:0.5em;
		display:inline-block;
		margin-right:80px;
		color:#000;
		text-align:left;
		min-width:85px;
	}
	.opinions-table-td-outcome span{
	   float:left;
	   white-space:nowrap;
	
	}
	.opinions-table-td-data{
	    height:29px;
	    color:#777;
	    text-align:left;
	    font-size:0.8em;
	    float:left;
	    min-width:180px;
	}
	.cl_result{
    	width:12%;
		heigh:29px;
		padding-left:30px;
		border-bottom:1px solid #000;
	}
	.last{
	    border-bottom:1px solid #000;
	}
	.table-td-line {
		border-bottom: 1px solid #000;
		line-height: 20px;
		padding: 4px 0;
		text-align: left;
	}
	
	<#if enableSimpleDealInfo?? && enableSimpleDealInfo>
	#dealInfoContain tr, #dealInfoContain table{
		page-break-inside:avoid;
	}
	</#if>
</style>
<@s.hidden name="expandDealInof" id="${preName!}expandDealInof"></@s.hidden>
<@s.hidden name="dlTableInfoId" id="${preName!}dlTableInfoId"></@s.hidden>
<@s.hidden name="dealDataUrl" id="${preName!}dealDataUrl"></@s.hidden>
<!--
<div class="opinions-title dealinfo-head">
	<strong>${getText('ec.view.dealadvice')}</strong>
</div>
 -->
<div style="width:98%;text-align:left;">
	<h1><span>${getText('ec.view.dealadvice')}</span></h1>
	<hr>
</div>
<#if enableSimpleDealInfo?? && enableSimpleDealInfo>
<div class="opinions-content ${preName!}dealinfo-content"  style="margin:0 0 10px;height:auto;zoom:1;border-top:0;border-right:0;" >
	<table cellspacing="0" cellpadding="0" align="center" id="${preName!}dealTable" style="width:100%;border-collapse:collapse;border-top:1px solid #000;border-right:1px solid #000;" frame="void">
		<tbody>
		</tbody>
	</table>
</div>
<#else>
<div class="opinions-content ${preName!}dealinfo-content"  style="margin:0 0 10px;height:auto;zoom:1;border-top:1px solid #000;border-right:1px solid #000;" >
	<table cellspacing="0" cellpadding="0" align="center" id="${preName!}dealTable" style="width:100%;">
		<tbody>
		</tbody>
	</table>
</div>
</#if>

<script type="text/javascript">

	$('#${preName!}opinionsContainer span').click( function( e){
		if( $(this).hasClass('opinions-icon-th-list') ){
			$(this).addClass('opinions-icon-th-list-current');
			$('#${preName!}opinionsContainer span[type="byTime"]').removeClass('opinions-icon-list-current');
		}else{
			$(this).addClass('opinions-icon-list-current');
			$('#${preName!}opinionsContainer span[type="byTask"]').removeClass('opinions-icon-th-list-current');
		}	
		var contents = $('div.${preName!}dealinfo-content');
		contents.hide();
		var current = contents.eq(  $(this).index() )
		current.show();
		if($('tr', current).size() == 0) {
			${preName!}showDeal($(this).index(), $(this).attr('type'));
		}
		var table = $( 'table', current );
		if ( table.height() > current.height() ){
			table.width( '788px' );
		}
	})

	function ${preName!}showDeal(index, param, flag){
		var groupByTask = false;
		var secd = 0;
		var url = $("#${preName!}dealDataUrl").val();
		if(param == 'byTask') {
			url = url.substring(0, url.lastIndexOf('.')) + 'Group.action';
			groupByTask = true;
		}
		$.ajax({
			url:url,
			type: 'post',
			async: false,
			data: {"tableInfoId" : $("#${preName!}dlTableInfoId").val(), "expandDealInof" : flag, "groupByTask" : groupByTask},
			success: function(data) {
				var htmlStr = '<tbody>';
				if(data == undefined || data == null){
					//if(!flag){
						htmlStr+='<tr><td class="table-td-line table-td-line-last" style="border-bottom:1px solid #000"><div style="text-align:center;color:#000;">${getText('ec.view.none')}</div></td></tr>';
						htmlStr+='</tbody>';
						$('#${preName!}dealTable').html(htmlStr);
					//}
					return;
				}
				$('#${preName!}dealTable').html("");
				if('${(enableSimpleDealInfo!false)?string}' == 'true'){
					for(var k in data){
						var list = data[k];
						if(k == 'bap_other'){
							taskName = "${getText('foundation.dealInfo.other')}";
						}else{
							taskName = list[0][12];
						}
						var showInSimpleDealInfo = true;
						if(list[0][14] != null && !list[0][14]){
							showInSimpleDealInfo = false;
						}
						if(showInSimpleDealInfo != null && showInSimpleDealInfo){
							var n = 0;
							for(var i in list){
								if(list[i][7] == 'NORMAL' || list[i][7] == 'ENTRUST' || list[i][7] == 'FORWARD'  || list[i][7] == 'NORMAL_AND_EXPECTEDCONSIGN'|| list[i][7] == 'FORWARD_AND_EXPECTEDCONSIGN'){
									n++;
								}
							}
							if(n > 0){
								htmlStr += '<tr><td class="opinions-table-td" style="width:12%;font-weight:bold;" rowspan="' + n + '"><div style="padding:9px;text-align:right;">' + taskName + '</div></td>';
								htmlStr = createDealInfoHtml(htmlStr, list);
							}
						}
					}
					if (htmlStr == '<tbody>') {
						htmlStr += '<tr><td class="table-td-line table-td-line-last" style="border-bottom:1px solid #000"><div style="text-align:center;color:#000;">${getText('ec.view.none')}</div></td></tr>';
					}
				}else{
					if(param == 'byTime') {
						htmlStr = buildDealHtml(htmlStr, data, param);
					} else if(param == 'byTask') {
						for(var j in data) {
							var response = data[j];
							var labelStr = response[0][12];
							if(j == 'bap_other') {
								labelStr = "${getText('foundation.dealInfo.other')}";
							}
							htmlStr+='<tr class="dealinfo-tr"><td class="table-td-label" rowspan="' + (response.length) +'">' + labelStr + '</td>';
							htmlStr = buildDealHtml(htmlStr, response, param);
						}
					}
				}
				htmlStr+='</tbody>';
				$('#${preName!}dealTable').html(htmlStr);
				
				// 修改ie8下打印样式分页问题
				$('#${preName!}dealTable td').each( function(){ 
					$(this).attr( 'height' , $(this).outerHeight() ); 
				} );
				
			}
		});	
	}
	
	function buildDealHtml(htmlStr, response, param) {
		for(var i = 0;i < response.length; i++){
			var thisDay = response[i][1];
			var thisTime = "";
			var exp = "";
			
			if(thisDay){
				if(response[i][9] != null){
					secd = response[i][9];
				}
				var d = new Date(thisDay);
				
                if(d != 'NaN'){
					thisTime = d.format('yyyy-MM-dd hh:mm:ss');
					var cal = 0;
					if(i != 0){
						cal = parseInt(thisDay,10) - parseInt(secd,10);
					}
                	exp = traTimeFunc(cal);
                }
			}else{
				thisDay = '';
			}
			if(exp != ""){
				exp = "${getText('ec.view.flowstop')}" + exp;
			}else{
				exp = "${getText('ec.view.stopone')}";
			}
			var dealInfo = "${getText('common.button.save')}";
			if(response[i][5] != null && response[i][5] != "null"){
				dealInfo = response[i][5];
			}
			var taskType;
			if (param == 'byTime') {
				taskType = 	response[i][11];
			} else {
				taskType = response[i][13];
			}
			if(taskType == 5){
				dealInfo = "${getText('ec.dealInfo.confirm')}";
			}
			if(param == 'byTime') {
				htmlStr += '<tr>';
			}
			
			if (response[i][7] != null && response[i][7] == "EXPECTEDCONSIGNOR") {
				htmlStr += '<td class="table-td-line"><span id="ec_table_name" style="font-weight:bold;margin-left:10px;">'+response[i][4]+'</span>${getText('ec.flowActive.for')} <span id="ec_table_time" style="font-weight:bold;">'+thisTime+'</span>${getText('ec.flowActive.doItByExpect')}<span  style="font-weight:bold;">'+response[i][6]+'</span>';
			} else if (response[i][7] != null && response[i][7] == "ENTRUST") {
				htmlStr += '<td class="table-td-line"><span id="ec_table_name" style="font-weight:bold;margin-left:10px;">'+response[i][4]+'</span>${getText('ec.flowActive.for')}<span id="ec_table_time" style="font-weight:bold;">'+thisTime+'</span>${getText('ec.flowActive.doIt')}<span id="ec_table_opr" style="font-weight:bold;">'+response[i][0]+'</span><span >${getText('ec.flowActive.doperate')}(${getText('ec.flowActive.dai')}' + response[i][8] + ')${getText('foundation.common.dh')}</span><span>${getText('ec.view.dealeffortcontent')}</span><span style="font-weight:bold;">'+dealInfo;
			} else if (response[i][7] != null && (response[i][7] == "NORMAL_AND_EXPECTEDCONSIGN" || response[i][7] == "FORWARD_AND_EXPECTEDCONSIGN")) {
				htmlStr += '<td class="table-td-line"><span id="ec_table_name" style="font-weight:bold;margin-left:10px;">'+response[i][4]+'</span>${getText('ec.flowActive.for')}<span id="ec_table_time" style="font-weight:bold;">'+thisTime+'</span>${getText('ec.flowActive.doIt')}<span id="ec_table_opr" style="font-weight:bold;">'+response[i][0]+'</span><span >${getText('ec.flowActive.doperate')}(${getText('ec.flowActive.bingdai')}' + response[i][8] + ')${getText('foundation.common.dh')}</span><span>${getText('ec.view.dealeffortcontent')}</span><span style="font-weight:bold;">'+dealInfo;
			} else if (response[i][7] != null && response[i][7] == "CONSIGNOR") {
				htmlStr += '<td class="table-td-line"><span id="ec_table_name" style="font-weight:bold;margin-left:10px;">'+response[i][4]+'</span>${getText('ec.flowActive.for')} <span id="ec_table_time" style="font-weight:bold;">'+thisTime+'</span>${getText('ec.flowActive.doItByProxy')}<span  style="font-weight:bold;">'+response[i][6]+'</span>';
			} else if (response[i][7] != null && response[i][7] == "INVALID") {
				htmlStr += '<td class="table-td-line"><span id="ec_table_name" style="font-weight:bold;margin-left:10px;">'+response[i][4]+'</span>${getText('ec.flowActive.for')}<span id="ec_table_time" style="font-weight:bold;">'+thisTime+'</span>${getText('ec.flowActive.doIt')}<span id="ec_table_opr" style="font-weight:bold;">${getText('ec.edit.remove')}</span><span >${getText('ec.flowActive.doperate')}';
			} else {
				if (response[i][8] != null && response[i][8] != '') {
					htmlStr += '<td class="table-td-line"><span id="ec_table_name" style="font-weight:bold;margin-left:10px;">'+response[i][4]+'</span>${getText('ec.flowActive.for')}<span id="ec_table_time" style="font-weight:bold;">'+thisTime+'</span>${getText('ec.flowActive.doIt')}<span id="ec_table_opr" style="font-weight:bold;">'+response[i][0]+'</span><span >${getText('ec.flowActive.doperate')}(${getText('ec.flowActive.dai')}' + response[i][8] + ')${getText('foundation.common.dh')}</span><span>${getText('ec.view.dealeffortcontent')}</span><span style="font-weight:bold;">'+dealInfo;
				} else {
					htmlStr += '<td class="table-td-line"><span id="ec_table_name" style="font-weight:bold;margin-left:10px;">'+response[i][4]+'</span>${getText('ec.flowActive.for')}<span id="ec_table_time" style="font-weight:bold;">'+thisTime+'</span>${getText('ec.flowActive.doIt')}<span id="ec_table_opr" style="font-weight:bold;">'+response[i][0]+'</span><span >${getText('ec.flowActive.doperate')}${getText('foundation.common.dh')}</span><span>${getText('ec.view.dealeffortcontent')}</span><span style="font-weight:bold;">'+dealInfo;
				}
			}
				
			if(response[i][7] != null && (response[i][7] == "FORWARD" || response[i][7] == "FORWARD_AND_EXPECTEDCONSIGN")){
				htmlStr += '${getText('foundation.common.dh')}</span> ${getText('ec.assignStaff.Forward')}<span  style="font-weight:bold;">'+response[i][6]+'</span>${getText('foundation.common.bd')}';
			}else if(response[i][7] != null&&response[i][7] == "EXPECTEDCONSIGNOR"){
				htmlStr += '${getText('foundation.common.bd')}</span>  '
			}else if(response[i][7]!=null&&response[i][7]=="CONSIGNOR"){
				htmlStr+='${getText('foundation.common.bd')}</span>  '
			}else if(response[i][7]!=null&&response[i][7]=="INVALID"){
				htmlStr+='${getText('foundation.common.bd')}</span>  '
			}else if(response[i][6] != null&&response[i][6] != ""){
				htmlStr += '${getText('foundation.common.dh')}</span> ${getText('ec.assignStaff.assign')}<span  style="font-weight:bold;">'+response[i][6]+'</span>${getText('ec.assignStaff.deal')}${getText('foundation.common.bd')}';
			}else{
				htmlStr += '${getText('foundation.common.bd')}</span>  '
			}
			
			
			if(response[i][2] != null){
				htmlStr += '<div ><span style="padding-left:36px;float: left;">'+response[i][2].replace(/\n/g, '<br>')+'</span>';
				if(response[i][10] != null){
					htmlStr += '<div style="text-align:right"><image src="' + response[i][10] + '" /></div>'; 
				}
				htmlStr += '<span style="color:#8D8D8D; float:right; padding-right:15px; ">'+exp+'</span> </div>';
			}else{
				htmlStr += '<div ><span style="padding-left:50px;float: left;"> </span>';
				if(response[i][10] != null){
					htmlStr += '<div style="text-align:right"><image src="' + response[i][10] + '" /></div>'; 
				}
				htmlStr += '<span style="color:#8D8D8D; float:right; padding-right:15px; ">'+exp+'</span> </div>';
			}
			
			htmlStr += '</td></tr>';
			secd = thisDay;
		}
		if(response.length < 1){
			htmlStr += '<tr><td class="table-td-line table-td-line-last"><div style="text-align:center"></div></td></tr>';
		}
		return htmlStr;
	}

	function createDealInfoHtml(htmlStr, list){
		$.each(list, function(i, data){
			if(data[7] == 'NORMAL' || data[7] == 'ENTRUST' || data[7] == 'FORWARD' || data[7] == 'NORMAL_AND_EXPECTEDCONSIGN' || data[7] == 'FORWARD_AND_EXPECTEDCONSIGN'){
				var comments = '${getText("ec.dealInfo.null")}';
				if(data[2] != null && data[2] != ''){
					comments = data[2].replace(/\n/g,'<br/>');
				}
				
				var outcome = '<span>${getText("ec.dealInfo.outcome")}</span>';
				if(data[5] != null && data[5] != ''){
					outcome += data[5];
				}else if(data[13] == 5){
					outcome += '${getText("ec.dealInfo.confirm")}';
				}
				
				var sName = '';
				if(data[4] != null && data[4] != ''){
					sName = data[4];
				}
				var cTimeStr = '';
				if(data[1] != null && data[1] != ''){
					//alert(typeof(data[1]));
					var cDate = new Date(data[1]);
					if(!isNaN(cDate)){
						cTimeStr = cDate.format("yyyy-MM-dd hh:mm:ss");
					}
				}
				var imgUrl = '';
				if(data[10] != null && data[10] != ''){
					imgUrl = data[10];
				}
				htmlStr += '<td class="opinions-table-td" style="width:65%;border-right:0px;"><div style="float:left;word-break:break-all;padding:10px;text-align:left;">' + comments + '</div></td>'
				htmlStr += '<td class="cl_result"><span class="opinions-table-td-outcome">' + outcome + '</span></td>';
				if(imgUrl != null && imgUrl != ''){
					htmlStr += '<td class="last" style="text-align: left;"><img src="' + imgUrl + '" width="100" height="100" /></br><div class="opinions-table-td-data">' + cTimeStr + '</div></td></tr>';
				}else{
					htmlStr += '<td class="last" align="left" style="padding-top:20px;">${getText("ec.dealInfo.signature")}' + sName + '</br><div class="opinions-table-td-data">' + cTimeStr + '</div></td></tr>';
				}
			}
		});
		return htmlStr;
	}

	$(function(){
		${preName!}showDeal(0, <#if enableSimpleDealInfo?? && enableSimpleDealInfo>'byTask'<#else>'${dealInfoGroup!"byTime"}'</#if>,true);
	});

</script>