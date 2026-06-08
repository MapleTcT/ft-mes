<#-- =======charts Plugin ======== -->
<#macro charts id="",width="500px",height="330px",chartsClick="",chartsDblClick="",type="",url="",data="",renderover="",beforeLoad="",chartTheme="macarons">
	<#assign type = type >	
	<div id="${id}" style="width: ${width!};height:${height!};"></div>
	<script type="text/javascript">
		var myChart_${id} = new Object(); 
		myChart_${id}.chart = echarts.init($("div[id='${id}']")[0],'${chartTheme!}');
		<#if beforeLoad?length gt 0 >
		${beforeLoad!}();
		</#if>
		var options_${id} = {
			<#nested>
		};
		<#if url?length gt 0>
		var refreshCallBack = "";
		myChart_${id}.chartRefresh = function(url,data,efunction,refreshBeforeLoad){
			var yValue_${id} ;
			var xValue_${id} ;
			var pieValue_${id} ;
			var pieName_${id} ;
			var refreshUrl;
			var refreshData;
			if( undefined != typeof(url) && url !=null && url != ""){
				refreshUrl =  url;
				if( undefined != efunction && null != efunction  && "" != efunction ){
					refreshCallBack = efunction;
				}
				if( undefined != refreshBeforeLoad && null != refreshBeforeLoad  && "" != refreshBeforeLoad ){
					eval(refreshBeforeLoad + '()');
				}
			}else{
				refreshUrl = '${url!}';
			}
			if( undefined != typeof(data) && data !=null && data != ""){
				refreshData = data;
			}else{
				<#if data?length gt 0>
				refreshData = '${data!}';
				</#if>
			}
			CUI.ajax({
				type: "POST",
				url: refreshUrl ,	
				data:  refreshData,	
				success: function(msg){
					if(msg !=null && msg != undefined && msg.res !=null && msg.res != undefined){
						if(options_${id} == null){
							options_${id} = myChart_${id}.chart.getOption();
						}
						pieValue_${id} = msg.res.pieValue;
						pieName_${id} = msg.res.pieName;
						if(pieValue_${id} != null && pieValue_${id} != undefined && pieName_${id} != null && pieName_${id} != undefined){
							var seriesLength = pieValue_${id}.split(";").length;	//饼图个数
							for(var j=0;j<seriesLength;j++){
								var pieValueLength = (pieValue_${id}.split(";"))[j].split(",").length;
								var pieValue_${id}_temp = (pieValue_${id}.split(";"))[j].split(",");
								var pieName_${id}_temp = (pieName_${id}.split(";"))[j].split(",");
								for(var i=0;i<pieValueLength;i++){
									options_${id}.series[j].data[i] ={'name':pieName_${id}_temp[i], 'value':pieValue_${id}_temp[i]};
								}
								pieValueLength = null;
								pieValue_${id}_temp = null;
								pieName_${id}_temp = null;
							}
						}else if(null != options_${id}.xAxis && undefined != options_${id}.xAxis){
							//只有type='category'时，才在这里进行赋值
							var xAxisType = options_${id}.xAxis[0].type ;
							var yAxisType = options_${id}.yAxis[0].type ;
							xValue_${id} = msg.res.xValue;
							yValue_${id} = msg.res.yValue;
							if(xAxisType == "category"){			
								if(xValue_${id} != null && xValue_${id} != undefined){				
									var xValueSplit = xValue_${id}.split(";");
									for(var i=0;i<xValueSplit.length;i++){
										options_${id}.xAxis[i].data = xValueSplit[i].split(",");
									}
									xValueSplit = null;
								}
								if(yValue_${id} != null && yValue_${id} != undefined){
									var yValueLength = yValue_${id}.split(";").length;						
									for(var i=0;i<yValueLength;i++){
										options_${id}.series[i].data = (yValue_${id}.split(";")[i]).split(",");
									}
									yValueLength = null;
								}
							}else if(yAxisType == "category"){
								if(yValue_${id} != null && yValue_${id} != undefined){
									var yValueLength = yValue_${id}.split(";").length;						
									for(var i=0;i<yValueLength;i++){
										options_${id}.yAxis[i].data = (yValue_${id}.split(";")[i]).split(",");
									}
									yValueLength = null;
								}
								if(yValue_${id} != null && yValue_${id} != undefined){
									var yValueLength = yValue_${id}.split(";").length;						
									for(var i=0;i<yValueLength;i++){
										options_${id}.series[i].data = (xValue_${id}.split(";")[i]).split(",");
									}
									yValueLength = null;
								}
							}else{
								if(xAxisType == "time"){
									var yValueLength = yValue_${id}.split(";").length;
									for(var i=0;i<yValueLength;i++){
										var seriesDataLength = (yValue_${id}.split(";")[i]).split(",").length;
										var xValueData = (xValue_${id}.split(";")[i]).split(",");
										var yValueData = (yValue_${id}.split(";")[i]).split(",");
										for(var j=0;j<seriesDataLength;j++){
											options_${id}.series[i].data[j] = [new Date(xValueData[j]),yValueData[j]];
										}
										seriesDataLength = null;
									}
									yValueLength = null;
								}else if(yAxisType == "time"){
									var yValueLength = yValue_${id}.split(";").length;
									for(var i=0;i<yValueLength;i++){
										var seriesDataLength = (yValue_${id}.split(";")[i]).split(",").length;
										var xValueData = (xValue_${id}.split(";")[i]).split(",");
										var yValueData = (yValue_${id}.split(";")[i]).split(",");
										for(var j=0;j<seriesDataLength;j++){
											options_${id}.series[i].data[j] = [xValueData[j],new Date(yValueData[j])];
										}
										seriesDataLength = null;
									}
									yValueLength = null;
								}else{
									var yValueLength = yValue_${id}.split(";").length;
									for(var i=0;i<yValueLength;i++){
										var seriesDataLength = (yValue_${id}.split(";")[i]).split(",").length;
										var xValueData = (xValue_${id}.split(";")[i]).split(",");
										var yValueData = (yValue_${id}.split(";")[i]).split(",");
										for(var j=0;j<seriesDataLength;j++){
											options_${id}.series[i].data[j] = [xValueData[j],yValueData[j]];
										}
										seriesDataLength = null;
									}
									yValueLength = null;
								}
							}
							//解决IE8下，数据后面多了一个分号，被IE8认为那个undefined也是一个元素造成的问题								
							for(var i=0;i<options_${id}.xAxis.length;i++){
								if(null == options_${id}.xAxis[i] || undefined == options_${id}.xAxis[i]){
									options_${id}.xAxis.pop();
								}
							}
							for(var i=0;i<options_${id}.yAxis.length;i++){
								if(null == options_${id}.yAxis[i] || undefined == options_${id}.yAxis[i]){
									options_${id}.yAxis.pop();
								}
							}								
						}	
						//解决IE8下，数据后面多了一个分号，被IE8认为那个undefined也是一个元素造成的问题								
						if(null != options_${id}.series && undefined != options_${id}.series){
							for(var i=0;i<options_${id}.series.length;i++){
								if(null == options_${id}.series[i] || undefined == options_${id}.series[i]){
									options_${id}.series.pop();
								}
							}
						}
						if(null != options_${id}.dataZoom && undefined != options_${id}.dataZoom){
							for(var i=0;i<options_${id}.dataZoom.length;i++){
								if(null == options_${id}.dataZoom[i] || undefined == options_${id}.dataZoom[i]){
									options_${id}.dataZoom.pop();
								}
							}
						}			
						myChart_${id}.chart.setOption(options_${id});
						<#if renderover?length gt 0 >
						${renderover!}();
						</#if>
						if( undefined != refreshCallBack && null != refreshCallBack  && "" != refreshCallBack ){
							eval(refreshCallBack + '()');
						}
						options_${id} = null;
						xValue_${id} = null;
						yValue_${id} = null;
						pieValue_${id} = null;
						pieName_${id} = null;
					}
				}
			});
		};
		myChart_${id}.chartRefresh();
		<#else>
			myChart_${id}.chart.setOption(options_${id});
			options_${id} = null;
		</#if>
		
	myChart_${id}.click = function(efunction) {
		myChart_${id}.chart.off('click');
		myChart_${id}.chart.on('click', efunction);
	};

	myChart_${id}.dblclick = function(efunction) {
		myChart_${id}.chart.off('dblclick');
		myChart_${id}.chart.on('dblclick', efunction);
	};
	
	myChart_${id}.mousedown = function(efunction){
		myChart_${id}.chart.off('mousedown');
		myChart_${id}.chart.on('mousedown', efunction);
	};
	
	myChart_${id}.mouseover = function(efunction){
		myChart_${id}.chart.off('mouseover');
		myChart_${id}.chart.on('mouseover', efunction);
	};
	
	myChart_${id}.mouseout = function(efunction){
		myChart_${id}.chart.off('mouseout');
		myChart_${id}.chart.on('mouseout', efunction);
	};
	
	myChart_${id}.mouseup = function(efunction){
		myChart_${id}.chart.off('mouseup');
		myChart_${id}.chart.on('mouseup', efunction);
	};
	myChart_${id}.setOption = function(option){
		myChart_${id}.chart.setOption(option);
	};
	myChart_${id}.getOption = function(){
		return myChart_${id}.chart.getOption();
	};
	myChart_${id}.getWidth = function(){
		return myChart_${id}.chart.getWidth();
	};
	myChart_${id}.getHeight = function(){
		return myChart_${id}.chart.getHeight();
	};
	myChart_${id}.chartResize = function(beforeLoad,afterLoad,chartTheme){
		//解决ie8下resize失效的bug
		if( undefined != beforeLoad && null != beforeLoad  && "" != beforeLoad ){
			eval(beforeLoad+"()");
		}
		if($.browser.msie && ($.browser.version =='8.0' || $.browser.version =='7.0')){
			var currentOption_${id} = myChart_${id}.chart.getOption(); 
			myChart_${id}.chart.clear();
			$("#${id}").children().remove();				
			if( undefined == chartTheme || null == chartTheme || "" == chartTheme ){
				chartTheme = "macarons";
			}
			myChart_${id}.chart = echarts.init($("div[id='${id}']")[0],chartTheme);
			myChart_${id}.chart.setOption(currentOption_${id});
			currentOption_${id} = null;
		}else{
			myChart_${id}.chart.resize();
		}
		if( undefined != afterLoad && null != afterLoad  && "" != afterLoad ){
			eval(afterLoad+"()");
		}
	};
	
	myChart_${id}.printImg = function(){
		//如果ie8,则该功能失效
		var agent = navigator.userAgent.toLowerCase() ;
		var regStr_ie = /msie [\d.]+;/gi ;
		if(agent.indexOf("msie") > 0){
			var verinfo = (agent.match(regStr_ie)+"").replace(/[^0-9.]/ig,""); 
			if(verinfo == '7.0' || verinfo == '8.0'){
				CUI.Dialog.alert("${getText('foundation.print.echarts.noSupportIE8')}");
				return;
			}
		}
		var chartHeight = myChart_${id}.chart.getHeight()+"px";
		var chartWidth = myChart_${id}.chart.getWidth()+"px";
		printWindow = window.open();		
		printWindow.document.write('<!DOCTYPE html><html><head></head><body><img alt="${getText('foundation.print.echarts.noSupportIE8')}" width="'+chartWidth+'" height="'+chartHeight+'" src="'+myChart_${id}.chart.getDataURL()+'"/></body></html>');
		printWindow.print();
        printWindow.close();	//打印结束之后，关闭当前页面
	};
	<#if chartsClick?length gt 0>
		myChart_${id}.click(${chartsClick!''});
	</#if>
	<#if chartsDblClick?length gt 0>
		myChart_${id}.dblclick(${chartsDblClick!''});
	</#if>
	</script>	
</#macro>
<#macro chartsTitle show=true,text="",subtext="",leftNum="",leftStr="center",topNum="",topStr="bottom",textColor="",subtextColor="",>
	title : {
		show : ${show?string},
		text: '${text!""?string}',
		subtext: '${subtext!""?string}',
		<#if leftNum?length gt 0 >
		left:${leftNum!},
		<#elseif leftStr?length gt 0 >
		left:'${leftStr!}',
		</#if>
		<#if topNum?length gt 0 >							
		top:${topNum!},
		<#elseif topStr?length gt 0 >	
		top:'${topStr!}',
		</#if>
		<#if textColor?length gt 0 >
		textStyle : {
			color : '${textColor!}',
		},
		</#if>
		<#if subtextColor?length gt 0 >
		subtextStyle : {
			color : '${subtextColor!}',
		},
		</#if>
		<#nested>
	},
</#macro>
<#macro chartsGrid show=true,leftNum=50,leftStr="",topNum=60,topStr="",rightNum=50,rightStr="",bottomNum=60,bottomStr="",borderWidth=0>
	grid : {			
		show:${show?string},
		<#if leftStr?length gt 0 >
		left : '${leftStr!}',
		<#elseif leftNum?length gt 0 >
		left : ${leftNum!},
		</#if>
		<#if topStr?length gt 0 >
		top : '${topStr!}',
		<#elseif topNum?length gt 0 >
		top : ${topNum!},
		</#if>	
		<#if rightStr?length gt 0 >
		right : '${rightStr!}',
		<#elseif rightNum?length gt 0 >
		right : ${rightNum!},
		</#if>	
		<#if bottomStr?length gt 0 >
		bottom : '${bottomStr!}',
		<#elseif bottomNum?length gt 0 >
		bottom : ${bottomNum!},
		</#if>		
		borderWidth : ${borderWidth!},
		<#nested>
	},
</#macro>
<#macro chartsTooltip show=true,showContent=true,trigger="axis",triggerOn="mousemove",axisPointerShow=true,formatter="",color="",>
	tooltip : {
		show : ${show?string},
		showContent :  ${showContent?string},
		trigger : '${trigger!}',
		triggerOn : '${triggerOn!}',
		<#if axisPointerShow?string("yes","no") =="yes">
		axisPointer : {
			type : 'cross',
			lineStyle : {
				type : 'dashed',
				width : 1
			}
		},
		</#if>
		<#if formatter?length gt 0>
		formatter : '${formatter!}',
		</#if>
		<#if color?length gt 0 >
		textStyle : {
			color : '${color!}',
		},
		</#if>		
		<#nested>
	},
</#macro>
<#macro chartsLegend show=true,leftStr="center",leftNum="",topStr="top",topNum="",orient="horizontal",selectedMode=true,data="",textColor="",>
	legend : {
		show : ${show?string},
		orient : '${orient!""?string}',
		<#if leftNum?length gt 0 >
		left : ${leftNum!},
		<#elseif leftStr?length gt 0 >
		left : '${leftStr!}',
		</#if>	
		<#if topNum?length gt 0 >
		top : ${topNum!},
		<#elseif topStr?length gt 0 >
		top : '${topStr!}',
		</#if>
		selectedMode : ${selectedMode?string},
		data : [${data!""?string}],
		<#if textColor?length gt 0 >
		textStyle : {
			color : '${textColor!}',
		},
		</#if>
		<#nested>
	},
</#macro>
<#macro chartsDataZoom type="slider",xAxisIndex="0",yAxisIndex="0",filterMode="filter",start="0",end="100",zoomLock=false>
	{
		<#if type?length gt 0>
			<#if type='inside'>
				type: 'inside',
			<#elseif type='slider'>
				type: 'slider',				
			</#if>
			xAxisIndex : [${xAxisIndex!}],
			yAxisIndex : [${yAxisIndex!}],
			filterMode: '${filterMode!}',
			start : ${start},
			end : ${end},
			zoomLock : ${zoomLock?string},
			<#nested>
		</#if>		
	},
</#macro>
<#macro chartsDataZoomLabel>
	dataZoom: [
		<#nested>
	],
</#macro>
<#macro chartsToolbox show=false,orient="horizontal",dataZoomShow=false,dataViewShow=false,readOnly=false,magicTypeShow=false,restoreShow=false,saveAsImageShow=false,>
	toolbox: {
        show : ${show?string},
        orient : '${orient!}', 
        feature: {
            dataZoom : {
            	show : ${dataZoomShow?string},
            },
            dataView: {
            	show : ${dataViewShow?string},
            	readOnly: ${readOnly?string},
            },
            magicType: {
            	show : ${magicTypeShow?string},
            },
            restore: {
            	show : ${restoreShow?string},
            },
            saveAsImage: {
            	show : ${saveAsImageShow?string},
            },
            <#nested>
        }
    },
</#macro>
<#macro chartsAxis type="",show=true,minNum="",minStr="",maxNum="",maxStr="",position="",name="",nameLocation="end",splitNumber="",data="",lineShow=true,lineOnZero=true,
	tickShow=true,tickInside=false,labelShow=true,splitLineShow=true,areaShow=false,>
	{
		show : ${show?string},
		type : '${type!}',
		<#if type=="value">
		boundaryGap : false,
		<#else>
		boundaryGap : true,
		</#if>
		<#if minNum?length gt 0 >
		min : ${minNum},	
		<#elseif minStr?length gt 0 >
		min : '${minStr!}',
		</#if>
		<#if maxNum?length gt 0 >
		max : ${maxNum},	
		<#elseif maxStr?length gt 0 >
		max : '${maxStr!}',
		</#if>
		<#if position?length gt 0 >
			position:'${position!}',
		</#if>
		name : '${name!}',
		nameLocation : '${nameLocation!}',
		<#if splitNumber?length gt 0 >
			splitNumber : ${splitNumber!},		
		</#if>
		axisLine : {
			show :${lineShow?string},
			onZero : ${lineOnZero?string},
		},
		axisTick : {
			show :  ${tickShow?string},
			inside : ${tickInside?string},
		},
		axisLabel : {
			show : ${labelShow?string},
		},
		splitLine : {
			show : ${splitLineShow?string},
		},
		splitArea : {
			show : ${areaShow?string},
		},
		data : [${data}],
		<#nested>
	},
</#macro>
<#macro chartsAxisLabel axisType="x" >
	<#if axisType?contains("x")>
		xAxis:[
			<#nested>
		],
	<#else>
		yAxis:[
			<#nested>
		],
	</#if>
	 
</#macro>
<#macro chartsSeriesLabel >
	series : [
		<#nested>
	],
</#macro>
<#macro chartsSeries seriesType="",name="",data="",symbol="emptyCircle",smooth=true,step=false,stepStr="",barWidth="",barWidthStr="",labelShow=true,formatter="",stack="",animation=true,silent=false,legendHoverLink=true,hoverAnimation=true,selectedMode=true,selectedModeStr="",clockwise=true,startAngle="180",minAngle="10",roseType=false,roseTypeStr="",markLineSlient=true,markLineColor="",markLineType="solid",markLineData="",xAxisIndex="0",yAxisIndex="0",radius="",center="",>
	{
		name : '${name!""?string}',
		z : 0,
		silent : ${silent?string},
		animation : ${animation?string},
		label : {
			normal : {
				show : ${labelShow?string}, 
				position : 'top',
				<#if formatter?length gt 0>
				formatter : '${formatter}',
				</#if>
				textStyle : {
					fontWeight : 'bolder',
					fontSize : '12',
					fontFamily : '微软雅黑',
				}
			}				
		},
		<#if markLineData?length gt 0 >
		markLine : {
			silent : ${markLineSlient?string},
			symbol : [ 'circle', 'arrow' ],
			symbolSize : [ 2, 4 ],
			lineStyle : {
				normal : {
					<#if markLineColor?length gt 0 >
					color : '${markLineColor}',
					</#if>
					type : '${markLineType}',
				}
			},
			data : [${markLineData}],
		},
		</#if>
	<#if seriesType?length gt 0 >
		<#if seriesType == 'line'>
			type : 'line',
			stack : '${stack!}',
			symbol : '${symbol!}',
			smooth : ${smooth?string},
			<#if step = true>
			step : ${step?string},
			<#elseif stepStr?length gt 0 >
			step : '${stepStr!}',
			<#else>
			step : ${step?string},	
			</#if>
			xAxisIndex : ${xAxisIndex!},
			yAxisIndex : ${yAxisIndex!},
			data : [${data!""?string} ],
		<#elseif  seriesType == 'bar'>
			type : 'bar',
			stack : '${stack!}',
			xAxisIndex : ${xAxisIndex!},
			yAxisIndex : ${yAxisIndex!},
			<#if barWidth?length gt 0 >
			barWidth : ${barWidth!},
			<#elseif barWidthStr?length gt 0 >
			barWidth : '${barWidthStr!}',
			</#if>
			data : [${data!""?string} ],
		<#elseif  seriesType == 'pie'>
			type : 'pie',
			legendHoverLink : ${legendHoverLink?string},
			hoverAnimation : ${hoverAnimation?string},
			<#if selectedModeStr?length gt 0 >
			selectedMode : '${selectedModeStr!}',
			<#else>
			selectedMode : ${selectedMode?string},						
			</#if>
			<#if radius?length gt 0 >
			radius : ${radius!},
			</#if>
			<#if center?length gt 0 >
			center : ${center!},
			</#if>
			clockwise : ${clockwise?string},
			startAngle : ${startAngle!},
			minAngle : ${minAngle!},
			<#if roseTypeStr?length gt 0>
			roseType : '${roseTypeStr?string}',
			<#else>
			roseType : ${roseType?string},
			</#if>
			data : [${data!""?string} ],
		</#if>
	<#else>
			type : '${type!}',
		<#if type == 'line'>
			stack : '${stack!}',
			symbol : '${symbol!}',
			smooth : ${smooth?string},
			<#if step = true>
			step : ${step?string},
			<#elseif stepStr?length gt 0 >
			step : '${stepStr!}',
			<#else>
			step : ${step?string},	
			</#if>
			xAxisIndex : ${xAxisIndex!},
			yAxisIndex : ${yAxisIndex!},
			data : [${data!""?string} ],
		<#elseif  type == 'bar'>
			stack : '${stack!}',
			<#if barWidth?length gt 0 >
			barWidth : ${barWidth!},
			xAxisIndex : ${xAxisIndex!},
			yAxisIndex : ${yAxisIndex!},
			<#elseif barWidthStr?length gt 0 >
			barWidth : '${barWidthStr!}',
			</#if>
			data : [${data!""?string} ],
		<#elseif  type == 'pie'>
			legendHoverLink : ${legendHoverLink?string},
			hoverAnimation : ${hoverAnimation?string},
			<#if selectedModeStr?length gt 0 >
			selectedMode : '${selectedModeStr!}',
			<#else>
			selectedMode : ${selectedMode?string},						
			</#if>
			<#if radius?length gt 0 >
			radius : ${radius!},
			</#if>
			<#if center?length gt 0 >
			center : ${center!},
			</#if>
			clockwise : ${clockwise?string},
			startAngle : ${startAngle!},
			minAngle : ${minAngle!} ,
			<#if roseTypeStr?length gt 0>
			roseType : '${roseTypeStr?string}',
			<#else>
			roseType : ${roseType?string},
			</#if>
			data : [${data!""?string} ],
		</#if>
	</#if>
		<#nested>
	},
</#macro>
