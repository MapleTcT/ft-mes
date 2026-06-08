<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<@head/>
<link href="/bap/static/foundation/css/style.css" rel="stylesheet" type="text/css" charset="utf-8"/>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.specialPermissionList.select')}</title>
</head>
<body>
<@loadpanel></@loadpanel>
<@s.hidden name="closePage"/>
<@s.hidden id="data"/>
<@s.hidden id="userId"  value="${userId!}"/>
<@s.hidden id="roleId" value="${roleId!}"  />
<@s.hidden id="associateName" value="${associateName!}"  />
<@s.hidden id="associateType" value="${associateType!}"  />
<@s.hidden id="associatePropertyCode" value="${associateCode!}"  />
<@s.hidden id="specialPermissionCode"  value="${specialPermissionCode!}"/>
<@s.hidden id="operateId"  value="${operateId!}"/>
<@s.hidden id="isTree"  value="${(isTree!false)?string('true','false')}"/>
<@s.hidden id="type"  value="${type!}"/>
<@s.hidden id="isAssigned" value="${(isAssigned!false)?string('true','false')}"  />
<@s.hidden id="pkPropertyCode"  value="${(pkProperty.code)!}"/>
<@s.hidden id="pkPropertyColumnType"  value="${(pkProperty.type)!}"/>
<@s.hidden id="targetModelCode"  value="${targetModelCode!}"/>
<@errorbar id="assignSpecialPermissionListFrameErrorBar" />
<div  style="width:100%;height:100%"  >
	<input type="hidden"  id="changed"  value="0"  />
	<input type="hidden"  id="type"  value="${type!}"  />
	<#assign editable=false />
	<#assign nodeType="checkbox" />
	<#assign checkBoxEditable=true />
	<#assign hideOperateBar=false />
	<#assign url="" />
	<#if type??&&type=="SYSTEMCODE" > 
	<#assign editable=true/>  
	<#else>
    <#assign editable=false  />
    </#if> 	
    <#if userId??>
    <#assign url="/msService/ec/specialPermission/showUShowInfo?specialPermissionCode=${specialPermissionCode}&userId=${userId!}&operateId=${operateId}&isAssigned=${(isAssigned!false)?string('true','false')}&isTree=${(isTree!false)?string('true','false')}&type=${type!}&isFromRole=${(isFromRole!false)?string('true','false')}" />
    </#if>
    <#if roleId??>
    <#assign url="/msService/ec/specialPermission/showRShowInfo?specialPermissionCode=${specialPermissionCode}&roleId=${roleId!}&operateId=${operateId}&isAssigned=${(isAssigned!false)?string('true','false')}&isTree=${(isTree!false)?string('true','false')}&type=${type!}" />
    </#if>	
    <#if isPermissionQuery??>
    	<#if isPermissionQuery>
    		<#assign hideOperateBar=true />
    		<#assign editable=false  />
    		<#assign nodeType="boolean" />
    		<#assign checkBoxEditable=false />
    	</#if>
    </#if>									
	<@datatable  exportExcel=false editableStrFlag="${(editable!false)?string('true','false')}" withoutConfigTable=true postData="&records.pageSize=65536" dtPage="records" id="assignSpecialPermissionList${formatSpecilPermissionCode!}"  hidekey="['LAYREC']"   transMethod="post" paginator=false firstLoad=true       dataUrl="${url!}"   >
    	<#if hideOperateBar??><#if !hideOperateBar><@operatebar operates="code:rolePermission_assignStaff_add||iconcls:add||name:${getHtmlText('foundation.assignStaff.add')}||onclick:foundation.specialPermissionList.openConfigDialog();code:rolePermission_assignStaff_del||iconcls:del||name:${getHtmlText('foundation.assignStaff.del')}||onclick:foundation.specialPermissionList.delRow()" operateType="noPower" resultType="json"/></#if></#if>
   		<#if type??&&type=="OBJECT" >
	   		<@datacolumn  key="ID"  textalign="center"  label="${getHtmlText(pkProperty.displayName)}"    hiddenCol=true  />  
	   		<@datacolumn  key="CODE"  textalign="center"  label="${getHtmlText(bussinessProperty.displayName)}"    />  
	   		<@datacolumn  key="TITLE"  textalign="center"  label="${getHtmlText(mainDisplayProperty.displayName)}"  />
	   		<#if isTree??&&isTree >
	   			<@datacolumn key="ISINCLUDESUB"   type="${nodeType!}"  editable=false  textalign="center" label="${getHtmlText('ec.specialpermission.isIncludeSub')}"   selectCol=true  />
	   		</#if>
    	<#else>
	    	<@datacolumn columnName="${(bussinessProperty.columnName)!}" key="ID"  textalign="center"  label="${getHtmlText('ec.specialpermission.code')}"   hiddenCol=true   /> 
			<#if isTree??&&isTree >
	   			<@systemCodeColumn onchange="sysTestColumnChange(oColumn,nRow,sFieldName,'${(systemCodeType)!}','${(systemEntityCode)!}')"  code="${(systemEntityCode)!}" textalign="center"   systemCodeType="${(systemCodeType)!}"   systemEntityCode="${(systemEntityCode)!}"   systemCodeUrl="/msService/ec/systemCode/singleCodeTree?systemEntityCode=${(systemEntityCode)!}"  key="TITLE.id" seniorSystemCode=isSeniorSystemCode editable=true type="selectcomp" notnull=false    label="${getHtmlText('ec.specialpermission.code')}" width=100  />
	   			<@datacolumn key="ISINCLUDESUB"   type="${nodeType!}"  editable=false textalign="center" label="${getHtmlText('ec.specialpermission.isIncludeSub')}"   selectCol=true  />
	   		<#else>
	   			<@systemCodeColumn onchange="sysTestColumnChange(oColumn,nRow,sFieldName,'${(systemCodeType)!}','${(systemEntityCode)!}')"  code="${(systemEntityCode)!}" textalign="center"   systemCodeType="${(systemCodeType)!}"   systemEntityCode="${(systemEntityCode)!}"   systemCodeUrl="/msService/ec/systemCode/singleCodeTree?systemEntityCode=${(systemEntityCode)!}"  key="TITLE" seniorSystemCode=isSeniorSystemCode editable=true type="selectcomp" notnull=false    label="${getHtmlText('ec.specialpermission.code')}" width=100  />
	   		</#if>
    	</#if>
    </@datatable>
</div>
</body>
</html>
<script type="text/javascript"  language="javascript">
(function(){
		// ie下页面可能会闪动,然后PT控件样式会乱掉,需要触发resize事件使样式恢复正常
		setTimeout( function(){
			$(window).trigger( 'resize' )
		},50)
		setTimeout( function(){
			$(window).trigger( 'resize' )
		},500)
		setTimeout( function(){
			$(window).trigger( 'resize' )
		},1000)
		
		CUI.ns("foundation.specialPermissionList");
		var  originalCode="";
		foundation.specialPermissionList.openConfigDialog=function(){
		   parent.foundation.specialPermission.openConfigDialog();
       }
       
       <#if type??&&type=="OBJECT">
       //判断数据在DT中是否已经存在
       foundation.specialPermissionList.isExist=function(obj){
          var allRows=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.getAllRows();
          for(var i=0;i<allRows.length;i++){
             if(obj.id==allRows[i].ID){
                assignSpecialPermissionListFrameErrorBarWidget.show("${getHtmlText('foundation.assignSpecialPermission.tip')}","f");
                return true;
             }
          } 
          return false; 
       }
       </#if>
       
       
       
       //系统编码判断在DT中是否已经存在
       foundation.specialPermissionList.isSystemCodeExist=function(idValue){
          var allRows=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.getAllRows();
          var count=0;
          for(var i=0;i<allRows.length;i++){
          	 <#if isTree??&&isTree&&type??&&type=="SYSTEMCODE" >
          	 	if(idValue==allRows[i].TITLE.id){
          	 <#else>
          	 	if(idValue==allRows[i].TITLE){
          	 </#if>
             	count++;
             }
          } 
          if(count>1)  {
         	assignSpecialPermissionListFrameErrorBarWidget.show("${getHtmlText('foundation.assignSpecialPermission.tip')}","f");
            return true;
          }
          return false; 
       }
       
       
       
       //删除DataTable中的数据
       foundation.specialPermissionList.delRow=function(){
          if(foundation.specialPermissionList.selectedAny()){
             CUI.Dialog.confirm("${getHtmlText('foundation.assignStaff.confrimDel')}",function(){
                assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.delRow();
             });
          }
       }
       
       //判断在dataTable是否选中了记录
       foundation.specialPermissionList.selectedAny=function(){
          var rows=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.selectedRows;
          if(rows.length<=0){
              try{
			     	top.assignSpecialPermissionMainFrameErrorBarWidget.show("${getHtmlText('foundation.assignStaff.checkedSelect')}","f");
			   } catch(e) {
			   		assignSpecialPermissionListFrameErrorBarWidget.show("${getHtmlText('foundation.assignStaff.checkedSelect')}","f");
			   }
              return false;
          }
          else{
              return true;
          }
       }
       
       
       //设置选中的编码
       foundation.specialPermissionList.changeView = function(node)  {
       			var param="";
       			if(node.targetModelCode!=""&&node.targetModelCode!=undefined)  {
       				param=param+"targetModelCode="+node.targetModelCode;
       			}
       			if(originalCode=="") {
       				originalCode=node.code;
       			}else {
       				if(node.code!=originalCode)  {
       					alert('选择不一致')
       				}
       				originalCode=node.code;
       			}
       			$("#currentSpecialCode").attr("value",node.code);
       			$("#view_source").prop("src","/msService/ec/view/specialPermissionList?"+param);
       }
       
        // 点击分类树，显示相关配置信息
		foundation.specialPermissionList.showRelateInfo=function(oNode){
			if(oNode != null && oNode != undefined){
				$('#systemModuleCode').val(oNode.code==null? '' : oNode.code);
			} else {
				$('#systemModuleCode').val('');
			}
			var url = "/msService/ec/systemCode/getSystemEntityList?moduleCode="+$('#systemModuleCode').val();
			datatable_systemEntityList.setRequestDataUrl(encodeURI(url), "pageSize="+CUI('input[name="systemEntityList_PageLink_PageCount"]').val());
		}
      
      
       
})();


//提供给父类调用的函数,填充关联数据
<#if type??&&type=="OBJECT">
function  fillDataGridValue(obj)  {
		//设置变更标志,默认以Id作为关联,如果用户设置了其他属性,但是返回参照视图中没有,则使用Id
		$("#changed").val(1);
		if(obj!=null){
	         for(var i=0;i<obj.length;i++){
	            if(!foundation.specialPermissionList.isExist(obj[i])){
	               assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.addNewRowWithValue( null, {"ID":obj[i].${(pkProperty.name)!},"CODE":obj[i].${(bussinessProperty.name)!},"TITLE":obj[i].${(mainDisplayProperty.name)!},"LAYREC":obj[i].layRec}  )
	            }
	         }
	    }
		try{
			$('body').trigger('resize');
			$('body').trigger('dialog.resize');
		} catch(e) {
			if(true) {throw e;}
		}
		parent.foundation.specialPermission.closeValueDialog();
}

</#if>


function  initFinish()  {


}

<#if type??&&type=="SYSTEMCODE">
function  fillDataGridSystemCodeValue()  {
		//设置变更标志
		var  systemcodeValueId=top.systemValue_rabbitcodeTest2id.value;
		var  systemcodeText=top.systemValue_rabbitcodeTest2id;
		assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.addNewRowWithValue( null, {"id":systemcodeValueId,"title":01}  )
		//var  form=$("#systemValue_rabbitcodeTest2id");
		//console.log(systemcodeValueId)
		//var  systemValue=$("input[name='rabbit.codeTest2.id']").val();
		
}

</#if>

//提供给父类调用的函数,保存数据
function  saveData()  {
		  var rows=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.getAllRows();
          var arrObj=new Array();
      	  //组装数据
      	  var allRows=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.getAllRows();
		  var id="";
		  var specialPermissionCode=$("#specialPermissionCode").val();
		  var userId=$("#userId").val();
		  var roleId=$("#roleId").val();
		  var operateId=$("#operateId").val();
		  var isAssigned=$("#isAssigned").val();
		  var jsonstr='['
		  for(var i=0;i<allRows.length;i++){
					if(allRows[i].ID!=null&&allRows[i].ID!=undefined)  {
		                   jsonstr+= '{'; 
		                   if(userId!=null&&userId!=""&&userId!=undefined&&userId!="undefined") 
		                   {
		                   jsonstr+="'userId':";
		                   jsonstr+="'"+userId+"'";
		                   jsonstr+=",";
		                   }else if(roleId!=null&&roleId!=""&&roleId!=undefined&&roleId!="undefined") {
		                   jsonstr+="'roleId':";
		                   jsonstr+="'"+roleId+"'";
		                   jsonstr+=",";
		                   }
		                   jsonstr+="'specialPermissionCode':";
		                   jsonstr+="'"+specialPermissionCode+"'";
		                   jsonstr+=",";
		                   jsonstr+="'operateId':";
		                   jsonstr+="'"+operateId+"'";
		                   jsonstr+=",";
		                   jsonstr+="'valueId':";
		                   jsonstr+="'"+allRows[i].ID+"'";
		                   jsonstr+=",";
		                   jsonstr+="'valueCode':";
		                   if(allRows[i].CODE!=""&&allRows[i].CODE!=undefined)  {
		                   		jsonstr+="'"+allRows[i].CODE+"'";
		                   }else  {
		                   		jsonstr+="'"+''+"'";
		                   }
		                   jsonstr+=",";
		                   jsonstr+="'valueTitle':";
		                   <#if isTree??&&isTree&&type??&&type=="SYSTEMCODE" >
		                   jsonstr+="'"+allRows[i].TITLE.id+"'";
		                   <#else>
		                   jsonstr+="'"+allRows[i].TITLE+"'";
		                   </#if>
		                   jsonstr+=",";
		                   jsonstr+="'layRec':";
		                   if(allRows[i].LAYREC!=""&&allRows[i].LAYREC!=undefined)  {
		                   		//if(allRows[i].ISINCLUDESUB!=null&&allRows[i].ISINCLUDESUB!=undefined) {
		                   			jsonstr+="'"+allRows[i].LAYREC+"'";
		                   		//}else {
		                   			// jsonstr+="'"+''+"'";
		                   		//}
		                   }else {
		                  	 jsonstr+="'"+''+"'";
		                   }
		                   jsonstr+=",";
		                   jsonstr+="'isIncludeSub':";
		                   if(allRows[i].ISINCLUDESUB!=null&&allRows[i].ISINCLUDESUB!=undefined) {
		                   	jsonstr+="'"+allRows[i].ISINCLUDESUB+"'";
		                   }else {
		                   	jsonstr+="'false'";
		                   }
		                   jsonstr +='}'
		                   if(i!=allRows.length-1){
		                    jsonstr+=','
						   }
					}
	      } 
	      jsonstr+=']';
	     
	      var xmlStr=builde_xml();
	      $("#data").val(xmlStr);
	      if(userId!=null&&userId!=""&&userId!=undefined&&userId!="undefined")  {
			      CUI.ajax({
			    			url: "/msService/ec/specialPermission/saveSpecialPermissionForUShow",
			    			type: 'post',
			    			async: false,
			    			data: {
			    				"data" : jsonstr,
			    				"userId":userId,
			    				"operateId":operateId,
			    				"specialPermissionCode":specialPermissionCode,
			    				"isAssigned":isAssigned
			    			},
			    			success: function(jsonResponse) {
			    				oResponse = jsonResponse;
			    				if(oResponse == "undefined") {
			    					alert("${getText('js.ec.corresponding.attribute')}");
			    					return false;
			    				}
			    				if(window.closeLoadPanel){closeLoadPanel();}
			    			}
			      });
	      }
	      
	       if(roleId!=null&&roleId!=""&&roleId!=undefined&&roleId!="undefined")  {
			      CUI.ajax({
			    			url: "/msService/ec/specialPermission/saveSpecialPermissionForRShow",
			    			type: 'post',
			    			async: false,
			    			data: {
			    				"data" : jsonstr,
			    				"roleId":roleId,
			    				"operateId":operateId,
			    				"specialPermissionCode":specialPermissionCode,
			    				"isAssigned":isAssigned
			    			},
			    			success: function(jsonResponse) {
			    				oResponse = jsonResponse;
			    				if(oResponse == "undefined") {
			    					alert("${getText('js.ec.corresponding.attribute')}");
			    					return false;
			    				}
			    				if(window.closeLoadPanel){closeLoadPanel();}
			    			}
			      });
	      }
}


function builde_xml() {
		  var allRows=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.getAllRows();
		  var isTree=$("#isTree").val();
          var pk=$("#specialPermissionCode").val();
          var type=$("#type").val();
          var pkPropertyColumnType=$("#pkPropertyColumnType").val();
          var pkPropertyCode=$("#pkPropertyCode").val();
          var associatePropertyCode=$("#associatePropertyCode").val();
          var targetModelCode=$("#targetModelCode").val();
		  var xmlStr = '<?xml version="1.0" encoding="UTF-8"?>';
		  xmlStr += '<objects>';
		  var idData="";
	      var layRecData="";
	      var isIncludeSubData="";
      	  for(var i=0;i<allRows.length;i++){
		 	if(allRows[i].ID!=null&&allRows[i].ID!=undefined)  {
			 	 	if(type!=null&&type=="OBJECT")  {
				 		if(pkPropertyColumnType!=""&&pkPropertyColumnType=="LONG") {
				 			//code等情况用'',id不用''
		             		idData+=","+allRows[i].ID;
		             	}else if(pkPropertyColumnType!=""&&pkPropertyColumnType=="TEXT") {
		             		idData+=",|"+allRows[i].ID+"|";
		             	}
		            }else if(type!=null&&type=="SYSTEMCODE")  {
		             	idData+=",|"+allRows[i].ID+"|";
		            }
	             	if(allRows[i].LAYREC!=null&&allRows[i].LAYREC!=undefined) {
	             		layRecData+=","+allRows[i].LAYREC;
	             	}else {
	             		layRecData+=","+"";
	             	}
	             	if(allRows[i].ISINCLUDESUB!=null&&allRows[i].ISINCLUDESUB!=undefined&&allRows[i].ISINCLUDESUB!="undefined")  {
	             		isIncludeSubData+=","+allRows[i].ISINCLUDESUB;
	             	}else {
	             		isIncludeSubData+=","+"false";
	             		
	             	}
             }
          }
          if(idData.length>0) {
	          idData=idData.substring(1);
          }else {
          	  idData='';
          }
          if(layRecData.length>0)  {
          	  layRecData=layRecData.substring(1);
          }
          if(isIncludeSubData.length>0)  {
          	  isIncludeSubData=isIncludeSubData.substring(1);
          }
          xmlStr += '<item>';
          xmlStr += '<pk><![CDATA[' + pk + ']]></pk>';
          xmlStr += '<isTree><![CDATA[' + isTree + ']]></isTree>';
          xmlStr += '<bussinessPropertyColumnType><![CDATA[' + pkPropertyColumnType + ']]></bussinessPropertyColumnType>';
          xmlStr += '<bussinessPropertyName><![CDATA[' + pkPropertyCode + ']]></bussinessPropertyName>';
          xmlStr += '<associatePropertyCode><![CDATA[' + associatePropertyCode + ']]></associatePropertyCode>';
          xmlStr += '<type><![CDATA[' + type + ']]></type>';
          xmlStr += '<targetModelCode><![CDATA[' + targetModelCode + ']]></targetModelCode>';
          xmlStr += '<idData><![CDATA[' + idData + ']]></idData>';
          if(layRecData!=""&&layRecData!=undefined&&layRecData!="undefined"&&isTree=="true")  {
               if(isIncludeSubData!=""&&isIncludeSubData!=undefined&&isIncludeSubData!="undefined"&&isTree=="true")  {
          			xmlStr += '<layRecData><![CDATA[' + layRecData + ']]></layRecData>';
          		}else {
          			xmlStr += '<layRecData><![CDATA[' + '' + ']]></layRecData>';
          		}
          }else {
           	xmlStr += '<layRecData><![CDATA[' + '' + ']]></layRecData>';
          }
          if(isIncludeSubData!=""&&isIncludeSubData!=undefined&&isIncludeSubData!="undefined"&&isTree=="true")  {
          	xmlStr += '<isIncludeSubData><![CDATA[' + isIncludeSubData + ']]></isIncludeSubData>';
          }else {
          	xmlStr += '<isIncludeSubData><![CDATA[' + '' + ']]></isIncludeSubData>';
          }
          xmlStr += '</item>';
          xmlStr += '</objects>';// section的
          return xmlStr;
}


function Init(){
       		<#--var url="/msService/ec/specialPermission/showDataInit";
       		var specialPermissionCode=$("#specialPermissionCode").val();
			var userId=$("#userId").val();
			var operateId=$("#operateId").val();
			var param="";
			param+="specialPermissionCode="+specialPermissionCode;
			param+="&userId="+userId;
			param+="&operateId="+operateId;
			url=url+param;
       		assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.load(url,initFinish);-->
       		assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.addNewRow()
}


//得到保存的值
function  getSavedValue() {
		var  value=$("#data").val();
		return  value;

}

//系统编码增加行
function  addNewRow()  {
	  assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.addNewRow()
}

function sysTestColumnChange(oColumn,nRow,sFieldName,type,entityCode){
	if(type==null||type=="")  {
		var systemcodeRowValue=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.getCellValue(nRow,sFieldName);
		if(foundation.specialPermissionList.isSystemCodeExist(systemcodeRowValue)){
				//assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.setCellValue(nRow,'ID','');
				//assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.setCellValue(nRow,'TITLE','');
				assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.delRow();
				return false;
		}
		var  selectedNode=parent.assignSpecialPermissionTree1.getSelectedNodes()[0];
		if(systemcodeRowValue!=""&&systemcodeRowValue!=undefined&&systemcodeRowValue!="undefined")  {
			assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.setCellValue(nRow,'ID',systemcodeRowValue);
		}
	}else if(type!=null&&type=="tree") {
		//树形
		var formatsFieldName=sFieldName.replace( /\./g, '_' );
		var el_id = 'sysCode_assignSpecialPermissionList${formatSpecilPermissionCode!}'+  formatsFieldName;
		try{	
			var node=eval( 'singleCodeTree' + entityCode + el_id + 'datatable_form' ).getSelectedNodes()[0]; 
			var layRec=node.layRec;
			var systemcodeRowValue=assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.getCellValue(nRow,sFieldName);
			if(foundation.specialPermissionList.isSystemCodeExist(systemcodeRowValue)){
					//assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.setCellValue(nRow,'ID','');
					//assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.setCellValue(nRow,'TITLE.ID','');
					assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.delRow();
					return false;
			}
			var  selectedNode=parent.assignSpecialPermissionTree1.getSelectedNodes()[0];
			if(systemcodeRowValue!=""&&systemcodeRowValue!=undefined&&systemcodeRowValue!="undefined")  {
				assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.setCellValue(nRow,'ID',systemcodeRowValue);
				assignSpecialPermissionList${formatSpecilPermissionCode!}Widget.setCellValue(nRow,'LAYREC',layRec);
			}
		}catch(e){
		
		}
	
	
	}
}

 
</script>
