<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<@head/>
<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.specialPermission.select')}</title>
</head>
<body>
</#if>
<@loadpanel></@loadpanel>
<@s.hidden name="closePage"/>
<@s.hidden id="assignSpecialPermission_callBackFuncName" name="callBackFuncName" value="${callBackFuncName}"/>
<@s.hidden name="crossCompanyFlag"/>
<@s.hidden id="operateId" value="${operateId!}"  />
<@s.hidden id="userId" value="${userId!}" name="spe.user.id" />
<@s.hidden id="roleId" value="${roleId!}"  name="spe.role.id" />
<@s.hidden id="isAssigned" value="${(isAssigned!false)?string('true','false')}"  />
<@s.hidden id="isPermissionQuery" value="${(isPermissionQuery!false)?string('true','false')}"  />
<@s.hidden id="isFromRole" value="${(isFromRole!false)?string('true','false')}"  />
<@s.hidden id="assignSpecialPermission_companyId" name="companyId" value="${getCurrent('companyId')}"/>
<@s.hidden id="currentSpecialId" name="currentSpecialId" value=""/>
<@s.hidden id="assignSpecialPermission_companyName" name="companyName" value="${getCurrent('companyName')}"/>
<@s.hidden id="assignSpecialPermission_companyCode" name="companyCode" value="${getCurrent('companyCode')}"/>
<@errorbar id="assignSpecialPermissionMainFrameErrorBar" />
<@frameset id="specialPermissionFrameset" border="0">
	<@frame id="positionFrameset_west" region="west" width=200 offsetH=6 resize=true  style="overflow:auto;">
		<@tree id="assignSpecialPermissionTree1" dataUrl="/msService/ec/specialPermission/listChildren?modelCode=${modelCode}" rootName="限制对象列表"  autoExpand=false
		     callback="{onClick:function(event,treeId,node){foundation.specialPermission.changeView(node);}, customOnAsyncSuccessMethod:function(event, treeId, treeNode, msg){$('#assignSpecialPermissionTree1 ul li:eq(0) a').trigger('click');}}"  nameCol="propertyName" />
	</@frame>
	<div style="clear: both; padding-left: 200px; height: 450px; width: 530px;"  id="viewDiv">
		
	</div>
</@frameset>
<script>
//回调函数
function  specialPermission(obj)  {
		//console.log(obj)
	    var selectedNodes=assignSpecialPermissionTree1.getSelectedNodes();
	    var selectedNode = selectedNodes[0];
	    var  selectedId=selectedNode.code;
	    document.getElementById(selectedId).contentWindow.fillDataGridValue(obj);
}
</script>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
<script type="text/javascript"  language="javascript">
(function(){
		CUI.ns("foundation.specialPermission");
		//弹出选择实体值的框
		foundation.specialPermission.openConfigDialog=function(){
		   var selectedNodes=assignSpecialPermissionTree1.getSelectedNodes();
		   var selectedNode = selectedNodes[0];
		   var  selectedId=selectedNode.code;
		   if(selectedNode==''||selectedNode==undefined||selectedNode==null||selectedNode.length==0)  {
		   			assignSpecialPermissionMainFrameErrorBarWidget.show('选择节点!','f');//alert('选择节点');
		   			return;
		   }
		   
		   if(selectedNode.isParent)  {
		   			assignSpecialPermissionMainFrameErrorBarWidget.show('请选择子节点!','f');
		   			return;
		   }
		   if(selectedNode.type=="OBJECT")  {
		   	    //对象
			   var refViewurl=selectedNode.refViewUrl;
			   var  param="";
			   param="callBackFuncName=specialPermission&openType=frame";
			   if(refViewurl.length>0)  {
			   	   if(refViewurl.indexOf("position/posReftreeSeg")!=-1)  {
			   	   		//特殊url转换  岗位树参照转换
			   			refViewurl="/msService/ec/foundation/position/common/positionListFrame";
			   	   }
			   	   if(refViewurl.indexOf("?")!=-1)  {
			   	   		refViewurl=refViewurl+"&"+param;
			   	   }else  {
			       	    refViewurl=refViewurl+"?"+param;
			       }
			   }
	           CUI(function(){
	               foundation.specialPermission.assignValueDialog=new CUI.Dialog({
		                  title:'选择实体值',
		                  url : refViewurl,
		                  type:5,
		                  iframe:"specialPermissionIframe",
		                  closeAlways:true,
		                  modal:true,
			              dragable:true,
			              buttons:[{name:"<@s.text name='common.button.choose'/>",
			              			id:"assignStaffSave",
			              			handler:function(){
			              			if(refViewurl.indexOf("/msService/ec/foundation/staff/common/staffListFrameset")!=-1) {
			              				if( $("#staffListTab .selected").html().indexOf("${getText('foundation.department.according')}") != -1) { // 按部门
											foundation.staff.sendBackDepartmentStaffInfo();
										} else if( $("#staffListTab .selected").html().indexOf("${getText('foundation.position.according')}") != -1) { //按岗位
											foundation.staff.sendBackStaffInfo();
										} else if ( $("#staffListTab .selected").html().indexOf("${getText('foundation.staff.unassignstaffselect')}") != -1) { //未分配人员
											foundation.unassignStaff.sendBackStaffInfo();
										} else if ($('#staffListTab .selected').html().indexOf("${getText('foundation.customGroup.userDefined')}") > -1){ //自定义组
											foundation.userDefinedGroupStaff.sendBackGroupStaffList();
										}	
			              			}else if(refViewurl.indexOf("/msService/ec/foundation/department/common/departmentListFrameCustom")!=-1)  {
			              				specialPermissionIframe.specialPermission__callbackFunction();
			              			}else {
			              				//一般参照视图callBack
				              			specialPermissionIframe.specialPermission__callbackFunction();
				              			}
			              			}
			              			},
					               {name:"<@s.text name='common.button.close'/>",
					                handler:function(){this.close()}
			    			}]
	               });
	               foundation.specialPermission.assignValueDialog.show();
	           })
           }else {
           		//系统编码
           		document.getElementById(selectedId).contentWindow.addNewRow();
           }
       }
       
       foundation.specialPermission.assignSystemCodeSeleValue=function()  {
       			$("#viewSource")[0].contentWindow.fillDataGridSystemCodeValue();
       			 foundation.specialPermission.assignValueDialog.close();
       }
       
       foundation.specialPermission.closeValueDialog=function(){
       		foundation.specialPermission.assignValueDialog.close();
       }
       
       
       //判断数据在DT中是否已经存在
       foundation.specialPermission.isExist=function(obj){
          var allRows=assignSpecialPermissionListWidget.getAllRows();
          for(var i=0;i<allRows.length;i++){
             if(obj.id==allRows[i].id){
                assignSpecialPermissionListFrameErrorBarWidget.show("${getHtmlText('foundation.assignSpecialPermission.tip')}","f");
                return true;
             }
          } 
          return false; 
       }
       
       //保存
       foundation.specialPermission.save=function(dialog,checkBox){
       		  //调用每个子页面的保存函数
       		  $(checkBox).parents('tr').first().attr('isEdited', true);
       		  var arrObj = new Array();
       		  var parentRow=assignSpecialPermissionTree1.getNodes();
          	  for(var i=0;i<parentRow.length;i++){
          	  		var allRows=parentRow[0].children;
          	  		for(var j=0;j<allRows.length;j++)  {
          	  		     var  selectedId=allRows[j].code;
          	  		     if(document.getElementById(selectedId)!=null) {
					  		 document.getElementById(selectedId).contentWindow.saveData();
					  		 var xml=document.getElementById(selectedId).contentWindow.getSavedValue();
					  		 if(xml!=""&&xml!=undefined)  {
					  		 	arrObj.push(document.getElementById(selectedId).contentWindow.getSavedValue());
					  		 }
				  		 }
			  		}
	          } 
          	  //组装数据
          	  try{
	          	  eval(CUI("#assignSpecialPermission_callBackFuncName").val()+ "(arrObj,${operateId!})");
			      assignSpecialPermissionMainFrameErrorBarWidget.show("${getHtmlText('foundation.add.success')}","s");
			      setTimeout(function(){
			        if(dialog.isShow !== -1){
			           dialog.close();
			        }
			      },1000)
			      $("#changed").attr("value",0);
			   }
			   catch(e){
			   	  //console.log(e);
			      assignSpecialPermissionMainFrameErrorBarWidget.show("${getHtmlText('foundation.add.failure')}","f");
			   }
       }
       
       
       
       //切换左侧的树,刷新右侧的iframe
       foundation.specialPermission.changeView = function(node)  {
       			if(!node.isParent)  {
		       			var userId= $("[name='spe.user.id']").val();
		       			var roleId=$("[name='spe.role.id']").val(); 
		       			var operateId=$("#operateId").val();
		       			var isAssigned=$("#isAssigned").val();
		       			var isFromRole=$("#isFromRole").val();
		       			var isPermissionQuery=$("#isPermissionQuery").val();
		       			var param="";
		       			param+="operateId="+operateId;
		       			if(userId!=""&&userId!=undefined)  {
		       			param+="&userId="+userId;
		       			}
		       			if(roleId!=""&&roleId!=undefined)  {
		       			param+="&roleId="+roleId;
		       			}
		       			param+="&isAssigned="+isAssigned;
		       			param+="&isPermissionQuery="+isPermissionQuery;
		       			param+="&isFromRole="+isFromRole;
		       			var exist=false;
		       			if(node.targetModelCode!=""&&node.targetModelCode!=undefined)  {
		       					param=param+"&targetModelCode="+node.targetModelCode;
		       			}
		       			if(node.type!=""&&node.type!=undefined)  {
		       				param+="&type="+node.type;
		       			}
		       			if(node.associateName!=""&&node.associateName!=undefined)  {
		       				param+="&associateName="+node.associateName;
		       			}
		       			
		       			if(node.associateType!=""&&node.associateType!=undefined)  {
		       				param+="&associateType="+node.associateType;
		       			}
		       			if(node.associateCode!=""&&node.associateCode!=undefined)  {
		       				param+="&associateCode="+node.associateCode;
		       			}
		       			if(node.isTree!=null&&node.isTree!=undefined)  {
		       				param+="&isTree="+node.isTree;
		       			}else {
		       				param+="&isTree=false";
		       			}
		       			if(node.code!=null&&node.code!=undefined)  {
		       				param+="&specialPermissionCode="+node.code;
		       			}
		       			$("#viewDiv").find("iframe").each(
		       					function() {
		       						if($(this).prop("id")==node.code) {
		       							$(this).css("display","");
		       						}else {
		       							$(this).css("display","none");
		       						}
		       					}
		       			);
		       			
		       			$("#viewDiv").find("iframe").each(
		       					function() {
		       						if($(this).prop("id")==node.code) {
		       							exist=true;
		       							return false;
		       						}
		       					}
		       			);
		       			$("#currentSpecialId").attr("value",node.code);
		       			// 创建新 iframe
		       			if(!exist) {
			       			var iframe = $('<iframe  height="100%"   width="100%"  class="viewIframe" frameborder="0"   id="' + node.code + '" name="' + node.code + '"     src="about:blank" />' );
			       			$(iframe).prop("src","/msService/ec/view/showSpecialPermissionList?"+param);
			       			$("#viewDiv").append(iframe);
		       			}
       			
       			
       			}
       }
       
        // 点击分类树，显示相关配置信息
		foundation.specialPermission.showRelateInfo=function(oNode){
			if(oNode != null && oNode != undefined){
				$('#systemModuleCode').val(oNode.code==null? '' : oNode.code);
			} else {
				$('#systemModuleCode').val('');
			}
			var url = "/msService/ec/systemCode/getSystemEntityList?moduleCode="+$('#systemModuleCode').val();
			datatable_systemEntityList.setRequestDataUrl(encodeURI(url), "pageSize="+CUI('input[name="systemEntityList_PageLink_PageCount"]').val());
		}
      	
      	
})();
<#--
$(document).ready(function () { 
	 			  var parentRow=assignSpecialPermissionTree1.getNodes();
	          	  for(var i=0;i<parentRow.length;i++){
	          	  		//console.log(parentRow[0]);
	          	  		var allRows=parentRow[0].children;
	          	  		for(var j=0;j<allRows.length;j++)  {
	          	  		        var userId= $("[name='spe.user.id']").val();
				       			var roleId=$("[name='spe.role.id']").val(); 
				       			var operateId=$("#operateId").val();
				       			var isAssigned=$("#isAssigned").val();
				       			var isPermissionQuery=$("#isPermissionQuery").val();
				       			var param="";
				       			param+="operateId="+operateId;
				       			if(userId!=""&&userId!=undefined)  {
				       			param+="&userId="+userId;
				       			}
				       			if(roleId!=""&&roleId!=undefined)  {
				       			param+="&roleId="+roleId;
				       			}
				       			param+="&isAssigned="+isAssigned;
				       			param+="&isPermissionQuery="+isPermissionQuery;
				       			var exist=false;
				       			if(allRows[j].targetModelCode!=""&&allRows[j].targetModelCode!=undefined)  {
				       					param=param+"&targetModelCode="+allRows[j].targetModelCode;
				       			}
				       			if(allRows[j].type!=""&&allRows[j].type!=undefined)  {
				       				param+="&type="+allRows[j].type;
				       			}
				       			if(allRows[j].isTree!=null&&allRows[j].isTree!=undefined)  {
				       				param+="&isTree="+allRows[j].isTree;
				       			}
				       			if(allRows[j].code!=null&&allRows[j].id!=undefined)  {
				       				param+="&specialPermissionCode="+allRows[j].id;
				       			}
				  		}
		          } 

});-->
       function  showDialog(url)  {
		   CUI(function(){
               foundation.specialPermission.assignValueDialog=new CUI.Dialog({
	                  title:'选择实体值',
	                  url : url,
	                  type:4,
	                  modal:true,
		              dragable:true
		              
               });
               foundation.specialPermission.assignValueDialog.show();
           })
       
       }
       


</script>