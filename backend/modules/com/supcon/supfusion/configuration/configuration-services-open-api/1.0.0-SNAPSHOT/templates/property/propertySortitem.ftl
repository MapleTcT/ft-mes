<#if (Parameters.openType)?default('page') == 'frame'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.property.sortItem.edit')}</title>
<@maincss/>
<@mainjs/>
<@adpSkin />
<script type="text/javascript" src="/bap/static/ec/js/jquery.dragsort-0.4.min.js"></script>
</head>
<body id="dialog_page">
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>

<style>

.selected{
	background-color: #FFF5C3;
	
}
.unselected{
	background-color: #F9F9F9;
	
}
.initBG{
   background-color:#EFEFEF;
}
.prevbtn
{
 background:url(/bap/static/foundation/infoset/order_prev.gif) no-repeat center left;
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;
}
.nextbtn
{
 background-image:url(/bap/static/foundation/infoset/order_next.gif);
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;
}
.topbtn
{
 background-image:url(/bap/static/foundation/infoset/order_top.gif);
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;
}
.lastbtn
{
 background-image:url(/bap/static/foundation/infoset/order_last.gif);
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;

}

.disableprevbtn
{
 background-image:url(/bap/static/foundation/infoset/order_disableprev.gif);
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;
}
.disablenextbtn
{
 background-image:url(/bap/static/foundation/infoset/order_disablenext.gif);
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;
}
.disabletopbtn
{
 background-image:url(/bap/static/foundation/infoset/order_disabletop.gif);
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;
}
.disablelastbtn
{
 background-image:url(/bap/static/foundation/infoset/order_disablelast.gif);
 width: 35px;
 height: 31px;
 cursor: hand;
 border-style: none;

}
.btndiv
{
padding-top:25px;
padding-left:35px;
}
ul{padding:0px;}
.dragsort li{height:28px;line-height:28px;}
.dragsort div{height:28px;border:solid 1px #e0e0e0;text-align:center;}
.dragsort input{height:28px;line-height:28px;margin-left:5px;}
.placeHolder div{background-color:white!important;border:dashed 1px gray!important;}
</style>
<script type="text/javascript" charset="utf-8" language="javascript">

//注册命名空间
CUI.ns("ec.property");
//当选中一行移动后，其余项恢复初始化背景
//并根据移动的方式，将滚动条滚动到对应位置
ec.property.recoveryBg=function(){
	//var selectedRow=CUI('li[class="selected"]',CUI('#list'));
    var row=CUI('li',CUI('#list'));
    for(var i=0;i<row.size();i++){
       if(CUI(row[i]).attr("class")!="selected"){
           CUI(row[i]).attr("class","unselected");
       }
       //根据当前的选中行将滚动条滚到相应位置
       else{
           var index=CUI(row[i]).index();
           if(index<=8){
           	index=0;
           }else{
           	index=index-8;
           }
           CUI("#contentDiv").scrollTop(index*CUI('li[id^="propertyColOrder_"]').first().height());
       }
    }
}
//首行
ec.property.firstRow=function(){
	var selectedRow=CUI('li[class="selected"]',CUI('#list'));
	if(selectedRow.size()<1){
		CUI.Dialog.alert("${getHtmlText('foundation.infoSet.selectRow')}");
		return false;
	}
	var firstRow = CUI('li[id^="propertyColOrder_"]').first();
	//console.log(firstRow)
	if(firstRow.attr("id")==selectedRow.attr("id")){
		return false;
	}
	firstRow.before(selectedRow);
	ec.property.disableUPBtn(1);
	ec.property.disableDownBtn(0);
	ec.property.recoveryBg();
	ec.property.sortReflsh();
}

//末行
ec.property.lastRow=function(){
	var selectedRow=CUI('li[class="selected"]',CUI('#list'));
	if(selectedRow.size()<1){
		CUI.Dialog.alert("${getHtmlText('foundation.infoSet.selectRow')}");
		return false;
	}
	var lastRow = CUI('li[id^="propertyColOrder_"]').last();
	if(lastRow.attr("id")==selectedRow.attr("id")){
		return false;
	}
	lastRow.after(selectedRow);
	ec.property.disableUPBtn(0);
	ec.property.disableDownBtn(1);
	ec.property.recoveryBg();
	ec.property.sortReflsh();
}

//上一行
ec.property.upRow=function(){
	var selectedRow=CUI('li[class="selected"]',CUI('#list'));
	if(selectedRow.size()<1){
		CUI.Dialog.alert("${getHtmlText('foundation.infoSet.selectRow')}");
		return false;
	}
	var prevRow = selectedRow.prev('li');
	var index=prevRow.index();
	prevRow.before(selectedRow);
	if(index==0){
		ec.property.disableUPBtn(1);
		ec.property.disableDownBtn(0);
	}else{
		ec.property.disableDownBtn(0);
	}
	ec.property.recoveryBg();
	ec.property.sortReflsh();
}

//下一行
ec.property.downRow=function(){
	var selectedRow=CUI('li[class="selected"]',CUI('#list'));
	if(selectedRow.size()<1){
	   CUI.Dialog.alert("${getHtmlText('foundation.infoSet.selectRow')}");
		return false;
	}
	var nextRow = selectedRow.next('li');

	var index=nextRow.next().index();
	nextRow.after(selectedRow);
	
	if(index==-1){
		ec.property.disableUPBtn(0);
		ec.property.disableDownBtn(1);
	}else{
		ec.property.disableUPBtn(0);
	}
	ec.property.recoveryBg();
	ec.property.sortReflsh();
}
//序号刷新
ec.property.sortReflsh = function(){
    var lists = CUI('#list li');
	$(lists).each(function(j) {
		if (parseInt($(this).attr("data-itemIdx")) != j) {	
			$(this).attr("data-itemIdx", j);
		}
	});
}
//将选中的行变成    tr[selected="true"]
ec.property.selectRow=function(obj){
	CUI('li[class="selected"]',CUI('#list')).each(function(){
		CUI(this).prop("selected","false");
		var index=CUI(this).index();
		CUI(this).attr("class","unselected");
	});
	CUI(obj).parent().parent().prop("selected","true");
	CUI(obj).parent().parent().attr("class","selected");
	var pre_index=CUI(obj).parent().parent().prev().index();
	var next_index=CUI(obj).parent().parent().next().index();
	if(pre_index==-1){
		ec.property.disableUPBtn(1);
	}else{
		ec.property.disableUPBtn(0);
	}	
	if(next_index==-1){
		ec.property.disableDownBtn(1);
	}else{
		ec.property.disableDownBtn(0);
	}
}
ec.property.disableUPBtn=function(type){//1禁用，其他开启
	if(type=='1'){
		CUI("#upMove").attr("class","disableprevbtn");
		CUI("#firstMove").attr("class","disabletopbtn");
	}else{
		CUI("#upMove").attr("class","prevbtn");
		CUI("#firstMove").attr("class","topbtn");			
	}
}

ec.property.disableDownBtn=function(type){//1禁用，其他开启
	if(type=='1'){
		CUI("#downMove").attr("class","disablenextbtn");
		CUI("#lastMove").attr("class","disablelastbtn");
	}else{
		CUI("#downMove").attr("class","nextbtn");
		CUI("#lastMove").attr("class","lastbtn");		
	}
}

</script>
<div id="contentDiv" style="position:absolute;left:0px;top:0px;margin:5px;overflow:auto;height:98%;width:73%;display:inline-block;">
	<form id="SubmitModelColOrderForm">
		<input type="hidden" name="orderModelCol" id="orderModelID"/>
		<ul class="dragsort" id="list" style="width:100%">
		<#list propertyList as p>

				<li id="propertyColOrder_${p.code}" colid="${p.code}" style="width:100%"  selected="false" class="unselected">
					<div><input type="radio" name="gender" style="float:left;" onclick="ec.property.selectRow(this);"/>${getHtmlText('${p.displayName}')}</div>
				</li>

			</#list>
		</ul>
	</form>
</div>
<script type="text/javascript">
		$("#list").dragsort({
			dragSelector: "li", 
			dragBetween: true, 
			dragEnd: saveOrder, 
			placeHolderTemplate: "<li class='placeHolder'><div></div></li>",
			scrollSpeed: 5
		});
		function saveOrder() {
			
		};
		</script>
<div id="infoContent" style="background-color:#f8f6f7;position:absolute;right:0px;top:0px;width:25%;border: 1px solid #efefef; height: 99%;">
	<div class="btndiv"><div class="topbtn" onclick="ec.property.firstRow()" id="firstMove" ></div></div>
	<div class="btndiv"><div class="prevbtn" onclick="ec.property.upRow()" id="upMove" ></div></div>
	<div class="btndiv"><div class="nextbtn" onclick="ec.property.downRow()" id="downMove"></div></div>
	<div class="btndiv"><div class="lastbtn" onclick="ec.property.lastRow()" id="lastMove"></div></div>
</div>


<#if (Parameters.openType)?default('page') == 'frame'>
</body>
</html>
</#if>