<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<@head/>
<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.position.select')}</title>
</head>
<body>
</#if>
<@loadpanel></@loadpanel>
<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="crossCompanyFlag" name="crossCompanyFlag" value="${(crossCompanyFlag)?string('true','false')}" />
<input type="hidden" id="assignPosition_callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" />
<input type="hidden" id="assignPosition_companyId" name="companyId" value="${getCurrent('companyId')}" />
<input type="hidden" id="assignPosition_companyName" name="assignPosition_companyName" value="${getCurrent('companyName')}" />
<input type="hidden" id="assignPostion_companyCode" name="assignPostion_companyCode" value="${getCurrent('companyCode')}" />
<@errorbar id="assignPositionListFrameErrorBar" />
<@frameset id="positionFrameset" border="0">
	<@frame id="positionFrameset_west" region="west" width=200 offsetH=6 resize=true  style="overflow:auto;">
		<#if crossCompanyFlag??&&crossCompanyFlag>
			<style type="text/css">
				#assignPositionTree1{position:relative;top:23px;}
				#positionFrameset_west{position:relative;}
			</style>
		 	<div class="tree-companylist">
	        	<div class="tree-companylist-son">
					<#assign l = companyList>
					<@listmenu list=l id="position_companyList" listName="name" listKey="id"   value="${getCurrent('companyName')}" onclick="foundation.assignPosition.changeCompany"  cssStyle="top:4px;left:50px;" />
				</div>
			</div>
		</#if>
		<@tree id="assignPositionTree1" dataUrl="/msService/ec/foundation/position/listChildren" rootName="${getText('foundation.position.list')}"  autoExpand=false
		     callback="{onClick:function(event,treeId,node){foundation.assignPosition.showPositionInfo(node);}}"/>
	</@frame>
	<@frame id="positionFramest_center" region="center" offsetH=6 resize=true>
		<div style="width:100%;height:100%;">
		   <div id="unassignPositionArea" style="float:left;height:95%;">
		        <div style="margin-left:15px;margin-top:6px;"><font color="#026AA1"><b>${getHtmlText('foundation.position.assignPositionListFrame.specified_position')}</b></font></div>
		        <div id="mneClient_position" style="margin:6px 5px 5px 15px;">
		        	<@mneclient view=viewFlag name="position.name" id="position_name" url="/foundation/position/common/poistionListFrame" classStyle="cui-noborder-input"  type="Position" multiple=false clicked=false isPrecise=true onkeyupfuncname='foundation.assignPosition.positionInfoCallback(obj)'/>
		        </div>
		        <div id="unassignedPositionRegion" style="width:95%;height:91%;border:1px solid #0059B3;overflow-y:auto;display:inline-block;margin-left:15px;">
		          <table id="unassignedPositionContent" style="border:0;width:100%;">
		             <tr id="positionContent_subordinate">
		              <td valign="top" colspan="2" align="right" style="width:100%;height:10px;border-bottom:1px dashed gray;padding-left:1px;" ><input type="checkbox" id="unassignPosition_selectFlag" onclick="foundation.assignPosition.selectAll(this)"/><font color="#026AA1"><b><label class="cui-label-font">${getHtmlText('foundation.staff.havingDownPosition')}</label></b></font></td>
		             </tr>
		             <tr id="unassignedPositionContent_blank">
		             </tr>
		          </table>
		        </div>
		  </div>
		  <div id="assignedPositionArea" style="float:left;height:95%;">
			      <div style="margin-left:15px;margin-top:6px;">
				      <div style="float:left;width:70%"><font color="#026AA1"><b>${getHtmlText('foundation.position.posed')}</b></font></div>
				      <#if !viewFlag>
				      <div style="float:right;margin-right:8px;" onclick='foundation.assignPosition.delSelectedPosition()'><span class="cui-btn-del" style="display:block;padding-left:20px;padding-right:2px;height:15px;cursor: pointer;">${getHtmlText('foundation.assignPosition.del')}</span></div>
				      <#else>
				      <div style="float:right;margin-right:8px;"></div>
				      </#if>
			      </div>
			      <div id="assignedPositionRegion" style="clear:both;width:95%;border:1px solid #0059B3;overflow-y:auto;display:inline-block;margin-left:15px;margin-right:8px;margin-top:6px">
			          <table id="assignedPositionContent" style="border:0;width:95%;">
			             <tr id="assignedPositionContent_subordinate">
			              	<td valign="top" style="width:70%;height:20px;border-bottom:1px dashed gray;padding-left:8px;" ><input type="checkbox" id="assignedPositionDel_selectFlag" onclick="foundation.assignPosition.selectAll(this)"/></td>
			              	<td valign="top" align="center" style="height:20px;border-bottom:1px dashed gray;"><font color="#026AA1"><b>${getHtmlText('foundation.staff.havingDownPosition')}</b></font></td>
			             </tr>
			             <#if positionList??>
							<#list positionList as assignedPosition>
									 <tr id="assignedPosition_${assignedPosition.id}" name="${assignedPosition.name}" psId="${assignedPosition.id}" code="${assignedPosition.code}" haveflag="false" layrec="${assignedPosition.layRec}">
									   	<td valign="top" style="height:20px;padding-left:8px;" >
									   		<input type="checkbox" id="assignedPositionDel_selectFlag_${assignedPosition.id}" onclick="foundation.assignPosition.clearAll(this)"/><label class="cui-label-font" style="padding-left:6px" >${assignedPosition.name}</label>
									   	</td>
			                            <td valign="top" align="center" style="height:20px;padding-right:1px;">
			                                <span id="assignedPosition_haveFlag_${assignedPosition.id}" style="display:block;height:15px;"></span>
			                            </td>
									 </tr>
							</#list>
						</#if>
						<tr></tr>
			        </table>
		        </div>
		</div>
    </div>
    </@frame>
	<#if (Parameters.openType)?default('page') != 'dialog'>
		<@frame id="position_Button" region="south" height=28>
			 <div align="right" style="margin-right:20px;position:absolute;bottom:5px;right:0;z-index:100;">
			 	<#if !viewFlag>
			 	<@querybutton isCustomize=true onclick="foundation.assignPosition.assignPositionSave()" customizeName="foundation.common.save"/>
			 	</#if>
			 	<@querybutton isCustomize=true onclick="foundation.assignPosition.closePage()" customizeName="common.button.cancel"/>
			 </div>
		</@frame>
	</#if>
</@frameset>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript"  language="javascript">
(function(){
   CUI.ns("foundation.assignPosition");
   //页面布局函数
   foundation.assignPosition.layout=function(){
   		var west_width=CUI("#positionFrameset_west").attr('width');
   		<#if (Parameters.openType)?default('page') != 'dialog'>
   			var whole_width= $(window).width();//页面的宽度
   		<#else>
   			var whole_width=700;//Dialog的宽度
   		</#if>
   		var content_width=whole_width-west_width-40;
   		CUI("#unassignPositionArea").width(content_width/2);
   		<#if (Parameters.openType)?default('page') != 'dialog'>
   			if(YAHOO.env.ua.ie == 6){
   				CUI("#assignedPositionArea").width(content_width/2+35);
   			}
   			else{
   				CUI("#assignedPositionArea").width(content_width/2+40);
   			}
   		<#else>
   			CUI("#assignedPositionArea").width(content_width/2+10);
   		</#if>
   		setTimeout(function(){
   			var mnClient_height=CUI("#mneClient_position").height();
   			var unassignedRegion_height=CUI("#unassignedPositionRegion").height();
   			CUI("#assignedPositionRegion").height(parseInt(mnClient_height)+parseInt(unassignedRegion_height)+5);
   		},50);
   		<#if (Parameters.openType)?default('page') != 'dialog'>
   			if(window.navigator.userAgent.indexOf('MSIE ') > -1){
	   			setTimeout(function(){
	   				var height=$(window).height();
					CUI("#unassignedPositionRegion").height(height-100);
					CUI("#assignedPositionRegion").height(height-75);
				},50);
			}
   		</#if>
   }
   foundation.assignPosition.layout();
   $(window).resize(function(){foundation.assignPosition.layout();});
   //页面初始化，主要是刷新“已指定岗位”区域中的含下级数据（将“已指定岗位”区域中已经存在数据中“含下级”部分用新数据覆盖）
   foundation.assignPosition.initPage=function(){
	     var existsInfo = '${assignPositonIds!}';
		 var objArr = existsInfo.split(';');
		 var chekFlag=CUI("span[id^='assignedPosition_haveFlag_']", CUI('#assignedPositionContent'));
		 chekFlag.each(function(i){
			var objMe = this;
			var obj = CUI(this).parent().parent();
			CUI.each(objArr, function(i,value){
				if(value==(obj.attr('psId')+',true')) {
					CUI(objMe).addClass('cui-gray-hook');
					obj.attr('haveflag', 'true');
					return false;
				}
			});
		 });
   }
   //切换公司
   foundation.assignPosition.changeCompany=function(oSelect){
      if(oSelect!=null&&oSelect!=undefined){
         setTimeout(function(){
         	// 修改root节点
			CUI("#assignPosition_companyId").val(oSelect.getAttribute("key"));
			var code=oSelect.getAttribute("code");
			CUI("#assignPosition_companyCode").val(code);
			CUI("#assignPosition_companyName").val(oSelect.innerHTML);
			assignPositionTree1.getNodes()[0].name = oSelect.innerHTML;
			assignPositionTree1.updateNode(assignPositionTree1.getNodes()[0], true);
			var url = "/msService/ec/foundation/position/listChildren?companyId="+ oSelect.getAttribute("key");
			assignPositionTree1.setting.async.url=url;
			assignPositionTree1.reAsyncChildNodes(assignPositionTree1.getNodes()[0], "refresh");
			//if(closeLoadPanel){closeLoadPanel(false);}
            <#--var url = "/msService/ec/foundation/position/listChildren?companyId="+ oSelect.getAttribute("key");
			assignPositionTree1.refresh(null,oSelect.innerHTML,url);
			CUI("#assignPosition_companyId").val(oSelect.getAttribute("key"));
			var code=oSelect.getAttribute("code");
			CUI("#assignPosition_companyCode").val(code);
			CUI("#assignPosition_companyName").val(oSelect.innerHTML);
			//assignPositionTree1.setRootName(oSelect.innerHTML);-->
         },0);
      }
   }
   //全选功能
   foundation.assignPosition.selectAll=function(obj){
        var id=CUI(obj).attr("id");
        CUI("input[id^='"+id+"_']").each(function(){
            CUI(this).prop('checked',obj.checked);
            var id=CUI(this).attr("id");
			var arr=id.split("_");
			if(arr[0]=='unassignPosition'){
			    CUI(this).parent().parent().attr('haveflag',obj.checked);
			 }
		})
   }
   //根据“含下级”checkbox的选中状态，刷新其所在行的hasFlag数据并且全选联动
   foundation.assignPosition.refreshHasFlag=function(obj){
      var id=CUI(obj).attr("id");
      var arr=id.split("_");
      if(arr[0]=='unassignPosition'){
        CUI(obj).parent().parent().attr('haveflag',obj.checked);
      }
      //改变全选checkBox的选中状态
      var cbNumber=CUI("input[id^='unassignPosition_selectFlag_']").length;
      var checkedNumber=CUI("input[id^='unassignPosition_selectFlag_']:checked").length;
      if(cbNumber==checkedNumber){
      	if(CUI("#unassignPosition_selectFlag")&&!(CUI("#unassignPosition_selectFlag").prop('checked'))){
          		CUI("#unassignPosition_selectFlag").prop('checked',true);
        }
      }
      else{
      	if(CUI("#unassignPosition_selectFlag")&&CUI("#unassignPosition_selectFlag").prop('checked')){
          		CUI("#unassignPosition_selectFlag").prop('checked',false);
        }
      }
   }
   //添加相应记录行到“待指定岗位”区域,type为了判断是obj来源于Tree还是助词码，其中1表示Tree,2表示助词码
   foundation.assignPosition.addPositionTr=function(obj,type){
	        var layer='';
	        if(type==2){
		         CUI.ajax({
		           type:'POST',
		           async:false,
		           url:'/ec/foundation/position/info?id='+obj.id,
		           success:function(res){
		              layer=res.layRec;
		           }
		        });
	        }
	        else{
	           layer=obj.layRec;
	        }
          var appendContent="<tr id=unassignedPositionContentTr_"+obj.id+" name="+obj.name+" psId="+obj.id+" code="+obj.code+" layrec="+layer+" haveflag='true'>";
          appendContent+="<td valign='top' style='height:20px;padding-left:8px;'><span id=unassignedPositionDel_"+obj.id+" class='cui-close' style='cursor:pointer;float:left;' onclick='foundation.assignPosition.delPositionInfo(this)'/><span style='float:left;'>"+obj.name+"</span></td>";
          appendContent+="<td valign='top' align='center' style='padding-right:2px;height:20px'><input id=unassignPosition_selectFlag_"+obj.id+" value="+obj.id+" type='checkbox' checked onclick='foundation.assignPosition.refreshHasFlag(this)'/></td>";
          appendContent+="</tr>";
          CUI("#unassignedPositionContent_blank").before(appendContent);
          if(CUI("#unassignPosition_selectFlag")&&!(CUI("#unassignPosition_selectFlag").prop('checked'))){
          		CUI("#unassignPosition_selectFlag").prop('checked',true);
          }
    }
   //点击树时向“待指定岗位”区域中添加相应的数据
   foundation.assignPosition.showPositionInfo=function(node){
   	   <#if !viewFlag>
       if(node!=null&& node!=undefined&&node.id!=-1){
           if(foundation.assignPosition.isExist(node,1)){
              foundation.assignPosition.addPositionTr(node,1);
           }
           assignPositionTree1.cancelSelectedNode();
       }
       else{
          //CUI.Dialog.alert("${getHtmlText('foundation.position.checkselected')}");
          assignPositionListFrameErrorBarWidget.showMessage("${getText('foundation.position.checkselected')}","f");
       }
       </#if>
    } 
   //助词码回车时向“待指定岗位”区域中添加相应的数据
   foundation.assignPosition.positionInfoCallback=function(obj){
       if(obj!=null && obj != undefined && obj[0].id != undefined && obj[0].id != null){
          if(foundation.assignPosition.isExist(obj[0],2)){
             foundation.assignPosition.addPositionTr(obj[0],2);
          }
          CUI('#position_name').val('');
       }
    }
    //判断岗位信息在“待指定岗位”区域中是否存在,type为了判断是obj来源于Tree还是助词码，其中1表示Tree,2表示助词码
    foundation.assignPosition.isExist=function(obj,type){
        var layer='';
        if(type==2){
	         CUI.ajax({
	           type:'POST',
	           async:false,
	           url:'/ec/foundation/position/info?id='+obj.id,
	           success:function(res){
	              layer=res.layRec;
	           }
	        });
        }
        else{
           layer=obj.layRec;
        }
        var flag=1;
        CUI("tr[id^='unassignedPositionContentTr_']").each(function(){
           var id=CUI(this).attr('id');
           var arr=id.split("_");
           var layRec=CUI(this).attr('layrec');
           if(arr[1]==obj.id){
             // CUI.Dialog.alert("${getHtmlText('foundation.assignPosition.tip')}");
              assignPositionListFrameErrorBarWidget.showMessage("${getText('foundation.assignPosition.tip')}","f");
              flag=0;
              return false;
           }
           if(layer.indexOf(arr[1]+"-")!=-1&&CUI(this).attr('haveflag')=='true'){
              //CUI.Dialog.alert("${getHtmlText('foundation.assignPosition.parentExist')}");
              assignPositionListFrameErrorBarWidget.showMessage("${getText('foundation.assignPosition.parentExist')}","f");
              flag=0;
              return false;
           }
           if(layRec.indexOf(obj.id+"-")!=-1){
              CUI(this).css("backgroundColor","#FCD6D6");
              if(!confirm("${getText('foundation.assignPosition.haveSubPosition')}")){
                   flag=0;
                   return false;
              }
           }
        });
        CUI("tr[id^='assignedPosition_']").each(function(){
           var id=CUI(this).attr('id');
           var arr=id.split("_");
           var layRec=CUI(this).attr('layrec');
           if(arr[1]==obj.id){
              //CUI.Dialog.alert("${getHtmlText('foundation.assignPosition.tip')}");
              assignPositionListFrameErrorBarWidget.showMessage("${getText('foundation.assignPosition.tip')}","f");
              flag=0;
              return false;
           }
           if(layer.indexOf(arr[1]+"-")!=-1&&CUI(this).attr('haveflag')=='true'){
              //CUI.Dialog.alert("${getHtmlText('foundation.assignPosition.parentExist')}");
               assignPositionListFrameErrorBarWidget.showMessage("${getText('foundation.assignPosition.parentExist')}","f");
              flag=0;
              return false;
           }
           if(layRec.indexOf(obj.id+"-")!=-1){
               CUI(this).css("backgroundColor","#FCD6D6");
              if(!confirm("${getText('foundation.assignPosition.haveSubPosition')}")){
                   flag=0;
                   return false;
              }
           }
        });
        if(flag==0) return false;
        return true;
    }
    //点出“删除”图标时删除相应的行（待指定岗位区域）
    foundation.assignPosition.delPositionInfo=function(obj){
       var id=CUI(obj).attr('id');
       var arr=id.split("_");
       CUI("#unassignedPositionContentTr_"+arr[1]).remove();
    }
    //点击“删除”图标时删除“已指定岗位”区域中被选中的行
    foundation.assignPosition.delSelectedPosition=function(){
       if(foundation.assignPosition.checkSelectAny()){
          CUI.Dialog.confirm("${getHtmlText('foundation.assignStaff.confrimDel')}",function(){
               CUI("input[id^='assignedPositionDel_selectFlag_']").each(function(){
	               if(CUI(this).attr('checked')=='checked'){
	                   CUI(this).parent().parent().remove();
	               }
               })
          });
        } 
    }
    //确认是否选中的相关要删除的记录行
    foundation.assignPosition.checkSelectAny=function(){
       var flag=0;
       CUI("input[id^='assignedPositionDel_selectFlag_']").each(function(){
          if(CUI(this).attr('checked')=='checked'){
              flag=1;
              return false;
          }
       })
       if(flag==1) return true;
       else{
       	 assignPositionListFrameErrorBarWidget.showMessage("${getText('foundation.assignPosition.checkedSelect')}","f");
          //CUI.Dialog.alert("${getHtmlText('foundation.assignPosition.checkedSelect')}");
          return false;
       }
    }
    //指定岗位保存
    foundation.assignPosition.assignPositionSave=function(dialog){
       if(foundation.assignPosition.positionInfoIsNull()){
            CUI.Dialog.confirm("${getHtmlText('foundation.assignPosition.nullChecked')}", function(){
                var arrObj=new Array();
                //当指定的岗位为空时，取消当前行指定岗位的选择状态
                <#if (Parameters.openType)?default('page') != 'dialog'>
		   	  		eval("opener." + CUI("#assignPosition_callBackFuncName").val()+ "(arrObj)");
		   	  		top.opener.focus();
					CUI.closeWindow();
		   	  <#else>
		      		eval(CUI("#assignPosition_callBackFuncName").val()+ "(arrObj)");
		      		dialog.close();
		   	  </#if> 
                
           });
            
       }
       else{
		   var arrObj=new Array();
		   var companyId=CUI("#assignPosition_companyId").val();
		   var companyCode=CUI("#assignPosition_companyCode").val();
		   //获取“待指定岗位”区域中的岗位信息
		   CUI("tr[id^='unassignedPositionContentTr_']",CUI("#unassignedPositionContent")).each(function(){
		        var oPosition=new Object();
		        oPosition.positionName=CUI(this).attr("name");
				oPosition.positionID=CUI(this).attr("psId");
				oPosition.positionCode=CUI(this).attr("code");
				oPosition.haveFalg=CUI(this).attr("haveflag");
				oPosition.companyId=companyId;
				oPosition.companyCode=companyCode;
				arrObj.push(oPosition);
		   });
		   //获取“已指定岗位”区域中的岗位信息
		   CUI("tr[id^='assignedPosition_']",CUI('#assignedPositionContent')).each(function(){
		        var oPosition=new Object();
		        oPosition.positionName=CUI(this).attr("name");
				oPosition.positionID=CUI(this).attr("psId");
				oPosition.positionCode=CUI(this).attr("code");
				oPosition.haveFalg=CUI(this).attr("haveflag");
				oPosition.companyId=companyId;
				oPosition.companyCode=companyCode;
				arrObj.push(oPosition);
		   });
		   try{
		   	  <#if (Parameters.openType)?default('page') != 'dialog'>
		   	  		eval("opener." + CUI("#assignPosition_callBackFuncName").val()+ "(arrObj)");
		   	  <#else>
		      		eval(CUI("#assignPosition_callBackFuncName").val()+ "(arrObj)");
		   	  </#if>
		      assignPositionListFrameErrorBarWidget.show("${getHtmlText('foundation.add.success')}","s");
		      <#if (Parameters.openType)?default('page') != 'dialog'>
		      		setTimeout(function(){
						try {
							top.opener.focus();
							CUI.closeWindow();
						}catch(e){}
					},1000);
		      <#else>
				      setTimeout(function(){
				        if(dialog.isShow !== -1){
				           dialog.close();
				        }
				      },1000)
		      </#if>
		   }
		   catch(e){
		      assignPositionListFrameErrorBarWidget.show("${getHtmlText('foundation.add.failure')}","f");
		   }
       }
	   
    }
    foundation.assignPosition.closePage=function(){
    	top.opener.focus();
		CUI.closeWindow();
    }
    //判断指定岗位的信息是否为空，空则为true
    foundation.assignPosition.positionInfoIsNull=function(){
       if(CUI("tr[id^='unassignedPositionContentTr_']",CUI("#unassignedPositionContent")).length<=0&&CUI("tr[id^='assignedPosition_']",CUI('#assignedPositionContent')).length<=0){
           return true;
       }
       else{
          return false;
       }
    }
    //当取消checkbox的选中状态时，将全选的checkbox的选中状态取消
    foundation.assignPosition.clearAll=function(obj){
       /*var id=CUI(obj).attr("id");
       var ids=id.split("_");
       var allId=ids[0]+"_"+ids[1];
       if(!CUI(obj).checked){
          CUI("#"+allId).prop('checked',false);
       }*/
       var cbNumber=CUI("input[id^='assignedPositionDel_selectFlag_']").length;
       var checkedNumber=CUI("input[id^='assignedPositionDel_selectFlag_']:checked").length;
       if(cbNumber==checkedNumber){
       		if(CUI("#assignedPositionDel_selectFlag")&&!(CUI("#assignedPositionDel_selectFlag").prop('checked'))){
       			CUI("#assignedPositionDel_selectFlag").prop('checked',true);
       		}
       }
       else{
       		if(CUI("#assignedPositionDel_selectFlag")&&CUI("#assignedPositionDel_selectFlag").prop('checked')){
       			CUI("#assignedPositionDel_selectFlag").prop('checked',false);
       		}
       }
    }
    //执行页面初始化操作
    foundation.assignPosition.initPage();
})();
</script>