<#-- Head -->
<#macro infoset userId, companyId, entity=-1 , readOnly=false>
	<#assign viewType = 'create'>
	<#if entity != -1>
	<#assign viewType = 'edit'>
	</#if>
	<#assign infoSets=getInfoset(entity,userId,companyId)>
	<#assign stateCollection=infoSets[0]>
	<#assign recordCollection=infoSets[1]>
	
	<#assign entityId=infoSets[2]>
	<#assign systemCodeCollection=infoSets[3]>
	<#assign companyId=getCurrent('company').id>
	
	<#assign recordColtId='staffs_infoset_recordcolt'>
	
	<#assign stateColtId='staffs_infoset_statecolt'>
	<#assign userPermisition = checkUserOperatePower('/foundation/user/add') >
	<#assign allowEmptyPsw = getConfigProperty('platform/bap/security/bap.allow.empty.password')>
	
<style type="text/css">
.infoset_top{height:40px;background:url("/bap/static/css/edit_20120821.png") 0 -1094px repeat-x;}
.infoset_top div ul{margin:0;padding:0;list-style:none;}
.infoset_top div ul li{float:left;color:#FFF;line-height:30px;margin-left:5px;}
.infoset_top div ul li a{color:#FFF;text-decoration:none;padding:4px 6px 2px;}
.infoset_top div ul li a:hover{background-color:#FFF6C3;color:#000;border:1px solid #FDC51A;padding:3px 5px 2px;}
.infoset_top div ul li.dot{line-height:30px;}
.info_icon{width:18px;height:30px;cursor:pointer}
.inco_icon_first{margin-top: 6px;background-image:url('/bap/static/css/first_grep.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.inco_icon_prev{margin-top: 6px;background-image:url('/bap/static/css/prev_grep.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.inco_icon_next{margin-top: 6px;background-image:url('/bap/static/css/next_grep.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.inco_icon_last{margin-top: 6px;background-image:url('/bap/static/css/last_grep.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.inco_icon_first_hover{margin-top: 6px;background-image:url('/bap/static/css/first_blue.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.inco_icon_prev_hover{margin-top: 6px;background-image:url('/bap/static/css/prev_blue.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.inco_icon_next_hover{margin-top: 6px;background-image:url('/bap/static/css/next_blue.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.inco_icon_last_hover{margin-top: 6px;background-image:url('/bap/static/css/last_blue.png');_background-image:url('/bap/static/css/sprite_20121010.gif');background-repeat:no-repeat;}
.infoTable td{}
.infoTable .textfield{
  border: 1px solid #c0d8f0;
  height: 19px;
  width: 100%;
  line-height:19px;
  padding-left:3PX;
}
.infoTable .textfield_disabled{
  border: 1px solid #909090;
  height: 19px;
  width: 100%;
  line-height:19px;
}
.infoTable .textarea{
  border: 1px solid #A5C7EF;
}
.etv-navset .etv-content{
  overflow-y:hidden!important;
}
.infoTable .textfield_readonly{
  border: 1px solid #909090;
  height: 18px;
  width: 100%;
  line-height:18px;
}
.infoTable.co.textfield_required{
color:#FF0000;
}
.etv-navset .etv-content{
  overflow-y:hidden!important;
}
.infoTable .textfield_err{border:#DB0000 1px solid;background-color:#FCD6D6;}
.etv-navset .etv-content {overflow-y:scroll;}
#staffs_infoset_statecolt{width:100% !important}
#staffs_infoset_statecolt .etv-content{width:100% !important}
</style>
<script type="text/javascript">
    
   function _edit(){
      //editStaffForm();
   }
 
  function _saveAndClose(){
  		_save("close");
  }
  
  function _saveAndNew(){
  		_save("new");
  }
  
  function _save(type){
  
    var data = {};
     
    var formData = CUI('#StaffSubmitForm').serializeArray();

    var hasErr = false;
    var tip = CUI('#_infoset_err_tip');
    tip.empty();
     

   var companyType=CUI('#companyType').val();
   var companyId=CUI('#companyId').val();
       if($("select[name='staff.workStatus.id']") && $("select[name='staff.workStatus.id']").length > 0) {
	       var staff_workStatus=CUI.trim(CUI("select[name='staff.workStatus.id']").val());
	       if(staff_workStatus=="" || staff_workStatus==null){
	         CUI("div[id^='staff_workStatus_id__']:first").addClass('textfield_err');
	         //showErrorField(CUI("select[name='staff.workStatus.id']"));
	         tip.append("<li><@s.text name='foundation.staff.workSatus.notnull'/></li>");
	         hasErr = true;
	       } else{
	       		CUI("div[id^='staff_workStatus_id__']:first").removeClass('textfield_err');
	       }
       }
       //if($("input[name='staff.birthday']") && $("input[name='staff.birthday']").length > 0) {
	   //    var staff_birthday=CUI.trim(CUI("input[name='staff.birthday']").val());
	   //    if(staff_birthday=="" || staff_birthday==null) {
	   //    	 //CUI("input[name='staff.birthday']").addClass('textfield_err');
	   //    	 showErrorField(CUI("input[name='staff.birthday']"));
	   //    	 tip.append("<li><@s.text name='foundation.staff.birthday.notnull'/></li>");
	   //    	 hasErr = true;
	   //    } else{
	   //    		if(!CUI("input[name='staff.birthday']").validate()){
	   //    			showErrorField(CUI("input[name='staff.birthday']"));
	   //    		}
	   //    		else{
	   //    			removeErrorField(CUI("input[name='staff.birthday']"));
	   //    		}	
	   //    }
       //}
       if($("select[name='staff.mainPositionId']") && $("select[name='staff.mainPositionId']").length > 0) {
	       var staff_mainPositionId=CUI.trim(CUI("select[name='staff.mainPositionId']").val());
	       if(staff_mainPositionId=="" || staff_mainPositionId==null){
	       	 //showErrorField(CUI("select[name='staff.mainPositionId']"));
	       	 CUI("div[id^='staff_mainPositionId__']:first").addClass('textfield_err');
	         tip.append("<li><@s.text name='foundation.staff.mainPositionName.notnull'/></li>");
	         hasErr = true;
	       } else{
	       		 CUI("div[id^='staff_mainPositionId__']").parents('div.fix-input:first').removeClass('textfield_err');
	       }
       }
       //if($("select[name='staff.ygxs.id']") && $("select[name='staff.ygxs.id']").length > 0) {
	   //    var staff_ygxs=CUI.trim(CUI("select[name='staff.ygxs.id']").val());
	   //    if(staff_ygxs=="" || staff_ygxs==null){
	   //    	 //showErrorField(CUI("select[name='staff.ygxs.id']"));
	   //    	 CUI("div[id^='staff_ygxs_id__']:first").addClass('textfield_err');
	   //      tip.append("<li><@s.text name='foundation.staff.ygxs.notnull'/></li>");
	   //      hasErr = true;
	   //    } else{
	   //    		CUI("div[id^='staff_ygxs_id__']:first").removeClass('textfield_err');
	   //    }
       //}
       
       if($("input[name='staff.mobile']") && $("input[name='staff.mobile']").length > 0) {
	       var staff_mobile=CUI.trim(CUI("input[name='staff.mobile']").val());
	       removeErrorField(CUI("input[name='staff.mobile']"))
	       if(staff_mobile=="" || staff_mobile==null) {
	       	 //
	       	 //showErrorField(CUI("input[name='staff.birthday']"));
	       	 //tip.append("<li><@s.text name='foundation.staff.birthday.notnull'/></li>");
	       	 //hasErr = true;
	       } else{
	       		if(typeof isMobile == 'function'){
	       			var result = isMobile(staff_mobile);
	       			if(!result){
	       				showErrorField(CUI("input[name='staff.mobile']"));
	       				tip.append("<li><@s.text name='foundation.staff.mobile.error'/></li>");
	       				hasErr = true;
	       			}
	       		}
	       }
       }
       
       if($("input[name='staff.sort']") && $("input[name='staff.sort']").length > 0) {
	       var staff_sort=CUI.trim(CUI("input[name='staff.sort']").val());
	       if(staff_sort=="" || staff_sort==null) {
	       	 
	       } else{
	       		if(typeof isInteger == 'function'){
	       			var result = isInteger(staff_sort);
	       			if(!result){
	       				CUI("input[name='staff.sort']").addClass('textfield_err');
	       				tip.append("<li><@s.text name='foundation.staff.num2'/></li>");
	       				hasErr = true;
	       			}
	       		}
	       }
       }
       
    if(!CUI.trim(CUI("#staffCodeID").val())){
      showErrorField(CUI("#staffCodeID"))
      //CUI("#staffCodeID").addClass('textfield_err');
      tip.append("<li>" + CUI("#staffCodeID").parent().prev('td').text() + "<@s.text name='foundation.staff.code.required'/></li>");
      CUI("#staffCodeID").focus();
      hasErr = true;
    }else{
      var patrn=/^[\w\.]+$/; 
      if (!patrn.exec(CUI("#staffCodeID").val())){
        //CUI("#staffCodeID").addClass('textfield_err');
       	showErrorField(CUI("#staffCodeID"));
        tip.append("<li><@s.text name='foundation.staff.code.format'/></li>");
        CUI("#staffCodeID").focus();
        hasErr = true;
      }else{
       //CUI("#staffCodeID").removeClass('textfield_err');
       removeErrorField(CUI("#staffCodeID"));
      }
    }
    if(!CUI.trim(CUI("#StaffSubmitForm_staff_name").val())){
      //CUI("#StaffSubmitForm_staff_name").addClass('textfield_err');
      showErrorField(CUI("#StaffSubmitForm_staff_name"));
      tip.append("<li>" + CUI("#StaffSubmitForm_staff_name").parent().prev('td').text() + "<@s.text name='foundation.staff.name.required'/></li>");
      CUI("#StaffSubmitForm_staff_name").focus();
      hasErr = true;
    } else if(!CUI("#StaffSubmitForm_staff_name").val().match(/^[a-zA-Z0-9_\u4e00-\u9fa5\-]{1,}$/gi)){
      showErrorField(CUI("#StaffSubmitForm_staff_name"));
      tip.append("<li>" + CUI("#StaffSubmitForm_staff_name").parent().prev('td').text() + "<@s.text name='foundation.staff.name.format'/></li>");
      CUI("#StaffSubmitForm_staff_name").focus();
      hasErr = true;
    }else{
      //CUI("#StaffSubmitForm_staff_name").removeClass('textfield_err');
      removeErrorField(CUI("#StaffSubmitForm_staff_name"));
    }
    if(CUI.trim(CUI("#StaffSubmitForm_staff_email").val())){
   
      //var patrn1=/^\w{1,}@\w+(\.\w+)+$/;
      
 	if(!CUI("#StaffSubmitForm_staff_email").val().match(/\b(^[_A-Za-z0-9-]+(\.[_A-Za-z0-9-]+)*@([A-Za-z0-9-])+(\.[A-Za-z0-9-]+)*((\.[A-Za-z0-9]{2,})|(\.[A-Za-z0-9]{2,}\.[A-Za-z0-9]{2,}))$)\b/gi)){ 
        //CUI("#StaffSubmitForm_staff_email").addClass('textfield_err');
        showErrorField(CUI("#StaffSubmitForm_staff_email"));
        tip.append("<li><@s.text name='foundation.company.email.checkemail'/></li>");
        CUI("#StaffSubmitForm_staff_email").focus();
        hasErr = true;
      }else{
      	//CUI("#StaffSubmitForm_staff_email").removeClass('textfield_err');
      	removeErrorField(CUI("#StaffSubmitForm_staff_email"));
      }
    }
    if(!CUI.trim(CUI('[name="staff.sex.id"]').val())){
      CUI("div[id^='staff_sex_id__']:first").addClass('textfield_err');
      //showErrorField(CUI('[name="staff.sex.id"]'));
      tip.append("<li><@s.text name='foundation.staff.sex.required'/></li>");
      hasErr = true;
    }else{
      //CUI("#StaffSubmitForm_staff_name").removeClass('textfield_err');
      CUI("div[id^='staff_sex_id__']:first").removeClass('textfield_err');
      removeErrorField(CUI('[name="staff.sex.id"]'));
    }
    
    CUI('input',CUI('#infoSet_content')).each(function(index){

      var o = $(this);
      if(o.attr('nullable') == '0'){
        if(!CUI.trim(o.val())){
          //o.addClass('textfield_err');
          showErrorField(o);
          if(o.attr('checkDate')&&o.attr('checkDate')=='true'){
          	tip.append("<li>" + o.parent().parent().parent().prev('td').text() + "<@s.text name='foundation.staff.field.required'/></li>");
         
          }else{
          	tip.append("<li>" + o.parent().parent().prev('td').text() + "<@s.text name='foundation.staff.field.required'/></li>");
         
          }
         o.focus();
          hasErr = true;
        }else{
          //o.removeClass('textfield_err');
          removeErrorField(o);
        }
      }
      if(o.attr('fieldType')&&o.attr('fieldType')=='TEXT'&&o.attr('maxLen') && o.val().length > o.attr('maxLen')){
        //o.addClass('textfield_err');
        showErrorField(o);
        tip.append("<li>" + o.parent().parent().prev("td").text() + "<@s.text name='foundation.staff.field.maxLength'/>" + o.attr("maxLen") + "!</li>");
        o.focus();
        hasErr = true;
      }
      if(o.attr('fieldType')&&o.attr('fieldType')=='INTEGER'){
        if(CUI.trim(o.val())){
          if(!isInteger(o.val())){
            //o.addClass('textfield_err');
            showErrorField(o);
            tip.append("<li>" + o.parent().parent().prev('td').text() + "<@s.text name='foundation.staff.field.requiredInt'/></li>");
            o.focus();
            hasErr = true;
          }
        }
        
      }
      if(o.attr('fieldType')&&o.attr('fieldType')=='DECIMAL'){
        if(CUI.trim(o.val())){
          if(!isDecimal(o.val())){
            //o.addClass('textfield_err');
            showErrorField(o);
            tip.append("<li>" + o.parent().parent().prev('td').text() + "<@s.text name='foundation.staff.field.requiredDECIMAL'/></li>");
            o.focus();
            hasErr = true;
          }
         
          if(!hasErr){
          
          	var valueObj=o.val()+"";
			var decLength=o.attr("decimaldigits");
			var index=valueObj.indexOf(".");
			
			if(index>-1){
				valueObj=valueObj+"00000000000";
				valueObj=valueObj.substring(0,parseInt(index,10)+parseInt(decLength,10)+1);
				valueObj=valueObj.replace ('.','');
			}else{
				valueObj=parseInt(valueObj,10)*Math.pow(10, parseInt(decLength,10))
			
			}
			
			var intObj=parseInt(valueObj,10);
			intObj=intObj+"";
	        if(o.attr('maxLen')<intObj.length ){
	        	 //o.addClass('textfield_err');
	        	 showErrorField(o);
			     tip.append("<li>" + o.parent().parent().prev('td').text() + "<@s.text name='foundation.staff.field.maxIntLength'/>" + o.attr('maxLen') + ",<@s.text name='foundation.staff.field.maxDECIMALLength'/>"+decLength+"!</li>");
			     o.focus();
			     hasErr = true;
	        }
          }
          	
        }
     
      }
      if(o.attr('checkDate')&&o.attr('checkDate')=='true'){
        if(CUI.trim(o.val())){
          if(!isDate(o.val())){
            //o.addClass('textfield_err');
            showErrorField(o);
            tip.append("<li>" + o.parent().parent().parent().prev('td').text() + "<@s.text name='foundation.staff.field.requiredDate'/></li>");
            o.focus();
            hasErr = true;
          }
        }
      }
      
    });
   var mess="";
    <#if recordCollection?has_content>
    <#list recordCollection?keys as recordKey>
      <#assign recs = recordCollection[recordKey] />
      <#assign cols = recs[0] />
      <#assign table = recs[2] />
       <#assign decimalIndex = 0 />
      if(DT_${table['TARGET_TABLE']}){
      	DT_${table['TARGET_TABLE']}.validator.validateRules = {
       <#list cols as col>
	       	<#if col["TYPE"]?lower_case =='decimal'>
	       		<#if decimalIndex gt 0>,</#if>
	   				"${col["CODE"]?upper_case}":[
	   							{"validatorType":"custom","jsCode":${table['TARGET_TABLE']}_${col["CODE"]?upper_case}_jsCode,"message":"<@s.text name="foundation.staff.field.maxIntLength"/>${col["FIELD_LENGTH"]},<@s.text name="foundation.staff.field.maxDECIMALLength"/>${col["DECIMAL_DIGITS"]}!"}
	   							
	   				]
	   			 <#assign decimalIndex = decimalIndex+1 />
	   		</#if>
	   		<#if col["TYPE"]?lower_case =='text'>
	   			<#if decimalIndex gt 0>,</#if>
	   			"${col["CODE"]?upper_case}":[
	   							{"validatorType":"stringlength","minLength":"","maxLength":${col["FIELD_LENGTH"]?default(0)},"message":"<@s.text name="foundation.staff.field.maxLength"/>${col["FIELD_LENGTH"]}!"}			
	   			]
	   			<#assign decimalIndex = decimalIndex+1 />
	   		</#if>
   		</#list>
   		};
        var errarMessage=DT_${table['TARGET_TABLE']}.validateTable();
        if(errarMessage!=null&&errarMessage!=""){
        	mess+=errarMessage;
        	
        }
        var d = DT_${table['TARGET_TABLE']}._DT.getEditData();
      
        CUI.each(d,function(index,item){
          CUI.each(item,function(i,it){
            if(i!='rowHtmlObj'){
              data['__infoset__${table['TARGET_TABLE']}__' + index + '__' + i] = it;
            }
          });
        });
        if(CUI("#__infoset__delRowIDS__${table['TARGET_TABLE']}").val()!=""){
          data['__infoset__${table['TARGET_TABLE']}__1__delRowIDS']=CUI("#__infoset__delRowIDS__${table['TARGET_TABLE']}").val();
        }
        
      }
    </#list>
    </#if> 
   if(mess!=""){
   		staffEditDialogErrorBarWidget.show(mess);
        return false;
   }
   <#if (userPermisition?? && userPermisition?string=='true')>
   var userName = $("#userName").val();
   if(userName && "" != userName) {
		var reg = /^[a-zA-Z0-9_\u4e00-\u9fa5\-]{1,}$/;
		var name = userName.match(reg);
		var errFlag = false;
		if(null == name){
			hasErr = true;
			showErrorField(CUI("#userName"));
			tip.append("<li>用户名不合法！</li>");
		}else{
			removeErrorField(CUI("#userName"));
		}
		<#if (allowEmptyPsw?? && allowEmptyPsw?string=='false')>
        var userPassword = $("#userPassword").val();
		if(!userPassword || "" == userPassword) {
			showErrorField(CUI("#userPassword"));
			errFlag = true;
			hasErr = true;
			tip.append("<li>密码不能为空！</li>");
		}
		if((userPassword && "" != userPassword) && userPassword.length < 8) {
			showErrorField(CUI("#userPassword"));
			errFlag = true;
			hasErr = true;
			tip.append("<li>密码字符个数不小于8位！</li>");
		}
		if(!errFlag){
			removeErrorField(CUI("#userPassword"));
		}
		errFlag = false;
		var userRePassword = $("#userRePassword").val();
		if(!userRePassword || "" == userRePassword) {
			showErrorField(CUI("#userRePassword"));
			errFlag = true;
			hasErr = true;
			tip.append("<li>确认密码不能为空！</li>");
		}
		if((userRePassword && "" != userRePassword) && userRePassword.length < 8) {
			showErrorField(CUI("#userRePassword"));
			errFlag = true;
			hasErr = true;
			tip.append("<li>确认密码字符个数不小于8位！</li>");
		}
		if((userPassword && "" != userPassword && userPassword.length >= 8) && (userRePassword && "" != userRePassword && userRePassword.length >= 8) && (userPassword!=userRePassword)) {
			showErrorField(CUI("#userRePassword"));
			errFlag = true;
			hasErr = true;
			tip.append("<li>确认密码不一致，请重新输入！</li>");
		}
		if(!errFlag){
			removeErrorField(CUI("#userRePassword"));
		}
		</#if>
   }
   </#if>
   if(hasErr){
      staffEditDialogErrorBarWidget.showMessage(tip.html());
      return false;
    }else{
     	staffEditDialogErrorBarWidget.close();
    }
    CUI.each(formData,function(index,item){
      data[item['name']] = item['value'];
    });
   if(CUI.Dialog){ CUI.Dialog.toggleAllButton('StaffSubmitForm',null,true);}
    _t_save(data,type);
    
  }
  function isInteger(obj){   
           
        reg=/^[-+]?\d+$/;    
        if(!reg.test(obj)){   
             return false;     
        }else{   
            return true;
        }   
    }   
    function isDecimal(obj){   
           
        reg= /^\d+\.*\d*$/;    
       
        if(!reg.test(obj)){   
             return false;     
        }else{   
            return true;
        }   
    }   
     function isDate(obj){   
        reg=/^(\d{4})(-|\/)(\d{2})\2(\d{2})$/;    
        if(!reg.test(obj)){   
             return false;     
        }else{   
            return true;
        }   
       return true;
    }   
    

    function showSpecifiedStaff(action){
      var currentStaffId = window.opener.foundation.staff.getSpecifiedStaffId(action);
      if(currentStaffId == -1){
      	CUI.Dialog.alert("${getHtmlText('foundation.staff.firstRecord')}");
        return false;
      }
      if(currentStaffId == -2){
      	CUI.Dialog.alert("${getHtmlText('foundation.staff.lastRecord')}");
        return false;
      }
      
      var href = window.location.href;
      var newhref=href.split('?');
    //href = href.replace(/staffId=/g,"");
    window.location.href = newhref[0] + "?staffId=" + currentStaffId;
    }
  function checkUpLoadFileBefore(){
    if(CUI("#infosetEntityID").val() == ""||CUI("#infosetEntityID").val() =='0'||CUI("#infosetEntityID").val() =='-1'){
      CUI.Dialog.alert("${getHtmlText('foundation.staff.savefirst')}");
      return false;
    }
  }
  function upLoadFile(){
  }
</script>
<div id="_infoset_err_tip" style="display:none;"></div>
<div class="infoset_top"  style="">
 <div class="edit-head">
    <div class="edit-menubar">
		<div class="fl" id="top_buttonbar">
		<#if !readOnly >
			<a class='cui-btn-new' onclick="_saveAndClose()"><span class="save"></span>${getHtmlText("foundation.staff.saveAndClose")}</a>
			<a class='cui-btn-new' onclick="_saveAndNew()"><span class="save"></span>${getHtmlText("foundation.staff.saveAndNew")}</a>
			<a class='cui-btn-new' onclick="_save()"><span class="save"></span>${getHtmlText("foundation.staff.save")}</a>
			
			<a class='cui-btn-new' id="infoset-fileupload-btn"><span class="save"></span><@uploadfile height=480 width=850 maxSize=10485760 namespace="staffFile" callback="upLoadFile" acceptType="all" onClickBefore="checkUpLoadFileBefore" multi=true linkId="${entityId?default('').toString()}" type="com.supcon.supfusion.base.entities.Staff.attachement" look="label" text="${getText('foundation.staff.infoset.attachment')}" ></@uploadfile></a>
			<script type="text/javascript">
				$('#infoset-fileupload-btn').click(function(e){
					if( e.target.id != '_uploadLabel' ){
						$( '#_uploadLabel' ).trigger( 'click' );
					}
				})
			</script>
		</#if>
		     
		    <a class='cui-btn-new' onclick="location.reload()"><span class="save"></span>${getHtmlText("foundation.staff.reflash")}</a>
		     
			<a class='cui-btn-new' onclick="CUI.closeWindow()"><span class="close"></span>${getHtmlText("foundation.staff.closeWin")}</a> 
		
		</div>

		<div style="float:right;padding-right:20px;"  id="info_page_order">
		    <ul>
		      <li class="info_icon inco_icon_first" title="<@s.text name='foundation.staff.firstPage'/>" onclick="showSpecifiedStaff('first')"></li><li>&#160;</li>
		      <li class="info_icon inco_icon_prev"  title="<@s.text name='foundation.staff.upPage'/>" onclick="showSpecifiedStaff('previous')"></li><li>&#160;</li>
		      <li class="info_icon inco_icon_next"  title="<@s.text name='foundation.staff.savefirst.nextPage'/>" onclick="showSpecifiedStaff('next')"></li><li>&#160;</li>
		      <li class="info_icon inco_icon_last"  title="<@s.text name='foundation.staff.lastPage'/>" onclick="showSpecifiedStaff('last')"></li>
		    </ul>
	  	</div>
	 </div>
</div>
  
</div>
<#assign enableCreateStaffIncludeUser = getConfigProperty("platform/bap/basic/bap.staff.create.include.user")!'false' >
<form id="_infoset">
<div id="${stateColtId}" class="etv-navset" >
  <ul class="etv-nav">
    <#if stateCollection?has_content>
      <#list stateCollection?keys as stateKey>
        <li>${getHtmlText(stateKey)}</li>
      </#list>
    </#if>
    <#if (userPermisition?? && userPermisition?string=='true')>
    <#if !staffId??>
    	<#if (enableCreateStaffIncludeUser?? && enableCreateStaffIncludeUser?string=='true')>
    		<li>
    			<span>用户信息</span>
    		</li>
    	</#if>
    </#if>
    </#if>
  </ul>
  <div class="etv-content" id="infoSet_content">
    <#if stateCollection?has_content>
      <#list stateCollection?keys as stateKey>
        <#assign res_flag = stateCollection[stateKey] />
        <#assign table_flag = res_flag[2] />
        <div style="overflow-y: auto; overflow-x: hidden;position: relative;">
        <#if table_flag["TARGET_TABLE"]=="BASE_STAFF">
          <#assign base_recs = stateCollection[stateKey] />
          <#assign base_cols = base_recs[0] />
          <#assign b_stateMap = base_recs[1] />
          <#assign b_table = base_recs[2] />
          <#assign nameEditable = getConfigProperty("platform/bap/basic/bap.organization.editable")!'true' >
          <table class="infoTable cui-fd-infotable" id="editInfo" cellpadding="0" cellspacing="0" width="98%">
            <tr>
              <td class="lab <#if !staff.code?exists>cui-lmust </#if>" style="width:13%;">
             	<@s.text name="foundation.staff.code"/>
              </td>
              <td style="width:20%;">
              <#if staff.code?exists>
              	<div class="fix-input-readonly"><@s.textfield  readonly="true"  name="staff.code"  id="staffCodeID" cssClass="cui-noborder-input" /></div>
              <#else>
             	 <div class="fix-input"><@s.textfield   maxLen="80" fieldType="TEXT"  name="staff.code"  id="staffCodeID" cssClass="cui-noborder-input" /></div>
              </#if>
              </td>
              <td class="lab  cui-lmust" style="width:13%;"><@s.text name="foundation.staff.name"/></td>
              <td  style="width:20%;">
               <#if readOnly || companyType =='ORGANIZATION'>
             	 <div class="fix-input-readonly"> <@s.textfield    name="staff.name"  cssClass="cui-noborder-input" readonly="true"/></div>
              <#else>
              	<#if staff.code?exists>
	              	<#if (nameEditable?? && nameEditable?string=='true')>
	              	<div class="fix-input-readonly"> <@s.textfield    name="staff.name"  cssClass="cui-noborder-input" readonly="true"/></div>
	              	<#else>
              	 	<div class="fix-input"> <@s.textfield  maxLen="80" fieldType="TEXT" name="staff.name"  cssClass="cui-noborder-input"/></div>
	              	</#if>
	            <#else>
	             <div class="fix-input"> <@s.textfield  maxLen="80" fieldType="TEXT" name="staff.name"  cssClass="cui-noborder-input"/></div>
	            </#if>
  	           </#if>
              </td>
              <td colspan="2" rowspan="7" >
              <div style=" margin:0px; padding:0px;position:relative;" >
                <div id="staff_imageShow" style="position:relative;*top:0;left:40%;margin-bottom:2px;margin-bottom:2px;width: 120px; height: 140px;border: 1px solid black;text-align:center;">
                  <@s.if test="staff.imagePath!=null">                  
                  <img height="140" id="staffImageID" align="center" width="120" src="${staff.imagePath}" namespace="staffImage" />    
                  </@s.if>
                  <@s.else>
                  <img height="140" id="staffImageID" align="center" width="120" src="/bap/static/foundation/images/defaultstaffimage.gif" namespace="staffImage" />
                  </@s.else>
                </div>
                <div style="position:relative;*top:0;left:50%;">
                  <#if  readOnly ||companyType =='ORGANIZATION'>
                   
                  <#else>
                     <@uploadfile maxSize=2097152 namespace="staffImage" height=150 width=150 callback="upLoadImage" acceptType="JPG,BMP,GIF,PNG" onClickBefore="checkUpLoadBefore" multi=false linkId="${(staff.id)!}" type="com.supcon.supfusion.base.entities.Staff.image" look="button" text="${getText('foundation.staff.infoset.uploadPhoto')}" />
                 </#if>
                </div>
              </div>
              </td>
            </tr>
            <tr>
              <td class="lab cui-lmust"><@s.text   name="foundation.staff.xzzg"/></td>
              <td >
         		 <#if readOnly || companyType =='ORGANIZATION'>  
         		 <div class="fix-input-readonly"><@s.textfield   name="staffMainPosition" value="${(staff.mainPosition.name)!}" cssClass="cui-noborder-input" readonly="true"/></div>
                 
	               <#else>
	                  <@s.select id="mainPositionId" name="staff.mainPositionId" emptyOption="false" cssStyle="width:100%;height:24px" templateDir="template" theme="simple" list="%{positionMap}"/>
	              </#if>
	           </td>
	          <td class="lab cui-lmust"><@s.text   name="foundation.infoSetCol.staffstatuse_nature"/></td>
              <td >
               <#if readOnly || companyType =='ORGANIZATION'>  
               		<div class="fix-input-readonly"><@s.textfield   name="staff_workStatus" value="${getText((staff.workStatus.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
               <#else>       
                   <@systemcode name="staff.workStatus.id" viewType=viewType code="STAFFSTATUSE_NATURE"  value="${(staff.workStatus.id)!}" deValue="STAFFSTATUSE_NATURE/STAFFSTATUTS_02" cssStyle="width:100%;height:18px"/>
              </#if>
              	
              </td>
            </tr>
            <tr>
              <td class="lab cui-lmust"><@s.text name="foundation.staff.sex"/></td>
              <td >
              <#if readOnly || companyType =='ORGANIZATION'>
              	<div class="fix-input-readonly"><@s.textfield   name="staff_sex"  value="${getText((staff.sex.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
               <#else>
               <@systemcode name="staff.sex.id" code="SEX_NATURE" viewType=viewType value="${(staff.sex.id)!}" cssStyle="width:100%;height:18px" />
              </#if>   
              </td>
              <td class="lab"><@s.text   name="foundation.staff.marriage"/></td>
              <td >
                <#if readOnly || companyType =='ORGANIZATION'>
                	<div class="fix-input-readonly"> <@s.textfield   name="staff_marriage" value="${getText((staff.marriage.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
               <#else>
                <@systemcode name="staff.marriage.id" viewType=viewType code="MARRIAGE_NATURE" value="${(staff.marriage.id)!}" cssStyle="width:100%;height:18px"/>
              </#if> 
              </td>
            </tr>
            <#if isThreeRoles>
	            <tr>
	            	<td class="lab"><@s.text name="foundation.infoSetColumn.SECURITYCLASS"/></td>
	              	<td >
	              		<#if readOnly || companyType =='ORGANIZATION'>
              				<div class="fix-input-readonly"><@s.textfield   name="staff_securityClass"  value="${getText((staff.securityClass.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
               			<#else>
               				<@systemcode name="staff.securityClass.id" viewType=viewType code="SECURITY_CLASS"  value="${(staff.securityClass.id)!}" deValue="" cssStyle="width:100%;height:18px"/>
	              		</#if>
                   	</td>
	            </tr>
			</#if>
            <tr>
              <td class="lab"><@s.text   name="foundation.staff.foreignLanguage"/></td>
              <td >
                  <#if readOnly || companyType =='ORGANIZATION'>
                 <div class="fix-input-readonly"><@s.textfield   name="staff.foreignLanguage" cssClass="cui-noborder-input" readonly="true"/></div>
                 <#else> 
                <div class="fix-input"> <@s.textfield maxLen="80" fieldType="TEXT"  name="staff.foreignLanguage" cssClass="cui-noborder-input" /></div>
                 </#if>
              </td>
              <td class="lab"><@s.text   name="foundation.staff.computeLevel"/></td>
              <td >
                 <#if readOnly || companyType =='ORGANIZATION'>
               <div class="fix-input-readonly"> <@s.textfield   name="staff.computeLevel" cssClass="cui-noborder-input" readonly="true"/></div>
                <#else>
               <div class="fix-input"> <@s.textfield maxLen="80" fieldType="TEXT"  name="staff.computeLevel" cssClass="cui-noborder-input" /></div>
                </#if> 
              </td>
            </tr>
            <tr>
              <td class="lab"><@s.text    name="foundation.staff.birthday"/></td>
              <td  >
               <#if readOnly || companyType =='ORGANIZATION'>
                  <@datepicker name="staff.birthday" id="staff.birthday" value="${(staff.birthday?date)!}"  view=true></@datepicker>
               <#else>       
                   <@datepicker name="staff.birthday" id="staff.birthday" value="${(staff.birthday?date)!}"></@datepicker>
              </#if> 
              </td>
              <td class="lab"><@s.text   name="foundation.staff.IDCard"/></td>
              <td >
                <#if readOnly || companyType =='ORGANIZATION'>
                <div class="fix-input-readonly"><@s.textfield   name="staff.idCard" cssClass="cui-noborder-input"  readonly="true" /> </div>
                <#else>
                <div class="fix-input"><@s.textfield   name="staff.idCard" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
                </#if> 
              </td>
            </tr>
            <tr>
              <td class="lab"><@s.text   name="foundation.staff.educational"/></td>
              <td >
              <#if readOnly || companyType =='ORGANIZATION'>  
              	<div class="fix-input-readonly"> <@s.textfield   name="staff_educational" value="${getText((staff.educational.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
               <#else>       
                <@systemcode name="staff.educational.id" viewType=viewType code="EDUCATIONAL_NATURE" value="${(staff.educational.id)!}" cssStyle="width:100%;height:18px"/>
              </#if> 
              </td>
              <td class="lab"><@s.text   name="foundation.staff.degree"/></td>
              <td >
              <#if readOnly || companyType =='ORGANIZATION'>  
              	<div class="fix-input-readonly"><@s.textfield   name="staff_degree" value="${getText((staff.degree.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
               <#else>       
               <@systemcode name="staff.degree.id" viewType=viewType code="DEGREE_NATURE" value="${(staff.degree.id)!}" cssStyle="width:100%;height:18px"/>
              </#if> 
              </td>
              </tr>
            <tr>
              
              <td class="lab"><@s.text   name="foundation.staff.school"/></td>
              <td >
               <#if readOnly || companyType =='ORGANIZATION'> 
               <div class="fix-input-readonly"><@s.textfield   name="staff.school" cssClass="cui-noborder-input" readonly="true"/></div>
               <#else>
                <div class="fix-input"><@s.textfield maxLen="80" fieldType="TEXT"  name="staff.school" cssClass="cui-noborder-input" /></div>
               </#if> 
              </td>
              <td class="lab"><@s.text    name="foundation.staff.biyeTime"/></td>
              <td >
               <#if readOnly || companyType =='ORGANIZATION'>  
               		
                    <@datepicker name="staff.biyeTime" id="staff.biyeTime"  cssStyle="border:1px solid #B1B1B1;border:0px;" value="${(staff.biyeTime?date)!}" view=true></@datepicker>
               <#else>       
                    <@datepicker name="staff.biyeTime" id="staff.biyeTime" value="${(staff.biyeTime?date)!}" ></@datepicker> 
              </#if> 
              </td>
              </tr>
            <tr>
              <td class="lab"><@s.text   name="foundation.staff.profession"/></td>
              <td >
              <#if readOnly || companyType =='ORGANIZATION'>
                <div class="fix-input-readonly"> <@s.textfield   name="staff.profession" cssClass="cui-noborder-input"  readonly="true"/></div>
                <#else>
                 <div class="fix-input"><@s.textfield  maxLen="80" fieldType="TEXT" name="staff.profession" cssClass="cui-noborder-input"/></div>
               </#if> 
              </td>   
              <td class="lab"><@s.text   name="foundation.staff.politicsInfo"/></td>
              <td >
               <#if readOnly || companyType =='ORGANIZATION'> 
               		<div class="fix-input-readonly"><@s.textfield   name="staff_politicsInfo.id" value="${getText((staff.politicsInfo.value)!)}" cssClass="cui-noborder-input" readonly="true"/> </div>
               <#else>       
                  <@systemcode name="staff.politicsInfo.id" viewType=viewType code="POLITICSINFO_NATURE" value="${(staff.politicsInfo.id)!}" cssStyle="width:100%;height:18px"/>
              </#if> 
             
              </td>
              <td class="lab" style="width:13%;"><@s.text   name="foundation.staff.nativePlace"/></td>
              <td  style="width:20%;">
                <#if readOnly || companyType =='ORGANIZATION'>
                 <div class="fix-input-readonly"><@s.textfield   name="staff.nativePlace" cssClass="cui-noborder-input"  readonly="true"/></div>
                <#else>
               <div class="fix-input"> <@s.textfield  maxLen="80" fieldType="TEXT" name="staff.nativePlace" cssClass="cui-noborder-input" /></div>
               </#if> 
              </td>
            </tr>
            <tr>
              <td class="lab"><@s.text   name="foundation.staff.dangandi"/></td>
              <td>
              
                <#if readOnly || companyType =='ORGANIZATION'>
                <div class="fix-input-readonly"> <@s.textfield   name="staff.dangandi" cssClass="cui-noborder-input" readonly="true"/></div>
                <#else>
                <div class="fix-input"><@s.textfield maxLen="80" fieldType="TEXT"  name="staff.dangandi" cssClass="cui-noborder-input" /></div>
               </#if> 

              </td>
              <td class="lab"><@s.text   name="foundation.staff.hukoudi"/></td>
              <td>
               <#if readOnly || companyType =='ORGANIZATION'>
               <div class="fix-input-readonly"><@s.textfield   name="staff.hukoudi" cssClass="cui-noborder-input"   readonly="true"/> </div>    
                <#else>
               <div class="fix-input"><@s.textfield maxLen="80" fieldType="TEXT"  name="staff.hukoudi" cssClass="cui-noborder-input"/></div>
               </#if> 
              
              </td>
              <td class="lab"><@s.text   name="foundation.staff.hukouxingzhi"/></td>
              <td >
              <#if readOnly || companyType =='ORGANIZATION'>
              		<div class="fix-input-readonly"><@s.textfield   name="staff_hukouxingzhi" value="${getText((staff.hukouxingzhi.value)!)}" cssClass="cui-noborder-input" readonly="true"/>  </div>
               <#else>       
                  <@systemcode name="staff.hukouxingzhi.id" viewType=viewType code="HUKOUXZ_NATURE" value="${(staff.hukouxingzhi.id)!}" cssStyle="width:100%;height:18px" />
              </#if>
               
              </td>
            </tr>
            <tr>
             <td class="lab"><@s.text   name="foundation.staff.height"/></td>
              <td >
                 <#if readOnly || companyType =='ORGANIZATION'>
                 <div class="fix-input-readonly"><@s.textfield   name="staff.height" cssClass="cui-noborder-input"  readonly="true"/> </div>
                <#else>
                 <div class="fix-input"><@s.textfield    name="staff.height" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
                </#if> 
              </td>
              <td class="lab"><@s.text   name="foundation.infoSetCol.srtffleader"/></td>
              <td >
              	<@s.hidden     name="staff.leaderStaffId" id="leaderstaffid" value="${(staff.leaderStaffId)!}"/>
               <#if readOnly || companyType =='ORGANIZATION'>  
                   	<@selector pageType="selectStaff" name="staff.leaderName" clearFunc='$("input[name$=leaderStaffId]").val("");$("input[name$=leaderName]").val("");' value="${(staff.leaderName)!}"  id="leaderstaffname"  onclick="selectleaderStaffInfo" cssClass="cui-noborder-input" view=true readOnly=true></@selector>
               <#else>       
                   	<@selector pageType="selectStaff" name="staff.leaderName" clearFunc='$("input[name$=leaderStaffId]").val("");$("input[name$=leaderName]").val("");' value="${(staff.leaderName)!}"  id="leaderstaffname"  onclick="selectleaderStaffInfo"  cssClass="cui-noborder-input" readOnly=true ></@selector>
              </#if>
              
              </td>
              <td class="lab"><@s.text   name="foundation.infoSetCol.srtffhighleader"/></td>
              <td >
              	<@s.hidden     name="staff.higherLeaderStaffId" id="higherleaderstaffid" value="${(staff.higherLeaderStaffId)!}"/>
               <#if readOnly || companyType =='ORGANIZATION'>  
                 <@selector pageType="selectStaff" name="staff.higherLeaderName" clearFunc='$("input[name$=higherLeaderStaffId]").val("");$("input[name$=higherLeaderName]").val("");' value="${(staff.higherLeaderName)!}" id="higherleaderstaffname" onclick="selecthigherleaderStaffInfo"  cssClass="cui-noborder-input" view=true readOnly=true></@selector>
               <#else>       
                  <@selector pageType="selectStaff" name="staff.higherLeaderName" clearFunc='$("input[name$=higherLeaderStaffId]").val("");$("input[name$=higherLeaderName]").val("");' value="${(staff.higherLeaderName)!}" readOnly=true id="higherleaderstaffname" onclick="selecthigherleaderStaffInfo"  cssClass="cui-noborder-input" ></@selector>
              </#if>
              
              </td>
            </tr>
            <tr>
              <td class="lab"><@s.text   name="foundation.staff.ygxs"/></td>
              <td  >
               <#if readOnly || companyType =='ORGANIZATION'>  
               		<div class="fix-input-readonly"><@s.textfield   name="staff_ygxs" value="${getText((staff.ygxs.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
                 
               <#else>       
                <@systemcode   name="staff.ygxs.id" code="YGXS" viewType=viewType value="${(staff.ygxs.id)!}" cssStyle="width:100%;height:18px"  />
              </#if>
              	
              </td>
              <td class="lab"><@s.text    name="foundation.staff.nation"/></td>
              <td >
                <#if readOnly || companyType =='ORGANIZATION'>
                	<div class="fix-input-readonly"> <@s.textfield   name="staff_nation" value="${getText((staff.nation.value)!)}" cssClass="cui-noborder-input" readonly="true"/></div>
               <#else>
               <@systemcode name="staff.nation.id" code="NATION_NATURE" viewType=viewType value="${(staff.nation.id)!}" cssStyle="width:100%;height:18px" />
              </#if> 
              </td>
	          <td class="lab"><@s.text   name="foundation.staff.sortitem"/></td>
              <td >
         		 <#if readOnly>  
         		 	<div class="fix-input-readonly"><@s.textfield   name="staffSort" value="${(staff.sort)!}" cssClass="cui-noborder-input" readonly="true"/></div>
	             <#else>
	             	<div class="fix-input"><@s.textfield  maxLen="80" fieldType="TEXT" name="staff.sort" value="${(staff.sort)!}" cssClass="cui-noborder-input"/></div>
	             </#if>
	           </td>
              <td class="lab">&#160;</td>
              <td>
              </td>
            </tr>
            <#assign b_col_count = 0 />
            <#if companyType =='ORGANIZATION'>  
            <td class="lab cui-lmust"><@s.text   name="foundation.staff.orcposition"/></td>
	            <td >
	                  <@s.select id="orcmainPositionId" cssClass="edit-select" name="stafforcmainPositionId"   emptyOption="true" cssStyle="width:100%;height:24px" templateDir="template" theme="simple" list="%{orcpositionMap}"/>
	           </td>
	           </#if>
            <#list base_cols as b_col>
              <#if b_col['SYSTEMDEFAULT'] == 0>
              <input type="hidden" name="__infoset__TARGET_FK_NAME" value="${b_table['TARGET_FK_NAME']}" />
              <input type="hidden" id="infosetEntityID" name="__infoset__${b_table['TARGET_FK_NAME']}" value="${entityId}" />
              <input type="hidden" name="__infoset__${b_table['TARGET_TABLE']}__ID" value="${b_stateMap['ID']?default('')}" />
	            
               <td style="width:13%;" class="lab <#if b_col['NULLABLE'] == 0&&!readOnly>cui-lmust</#if>">
                  ${b_col['NAME']}
                  <#assign b_col_count = b_col_count + 1 />
               </td>
              
               <#if b_col['DISPLAY_TYPE'] == 'TEXTFIELD'>
                  <td class="<#if b_col['NULLABLE'] == 0&&!readOnly> textfield_required</#if>">
                 	 <div class="<#if !b_col['_editable'] || readOnly ||(companyType =='ORGANIZATION'&& b_col['COMPANY_ID'] != companyId) >fix-input-readonly<#else>fix-input</#if>">
                  		<input type="text" nullable="${b_col['NULLABLE']}" fieldType="${b_col['TYPE']}" decimalDigits="${b_col['DECIMAL_DIGITS']?default('2')}"  maxLen="${b_col['FIELD_LENGTH']?default('1500')}" name="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}" value="${b_stateMap[b_col['CODE']]?default('')}" class="cui-noborder-input" <#if !b_col['_editable'] || readOnly ||(companyType =='ORGANIZATION'&& b_col['COMPANY_ID'] != companyId) > readonly="true"  </#if>  />            
					</div>
                  </td>
                  <#assign b_col_count = b_col_count + 1 />
               </#if>    
                  
                <#if b_col['DISPLAY_TYPE'] == 'DATE'>
                <td style="width:20%;" class="co<#if b_col['NULLABLE'] == 0 &&!readOnly> textfield_required</#if>">
                <#if !b_col['_editable'] || readOnly ||(companyType =='ORGANIZATION'&& b_col['COMPANY_ID'] != companyId) >
                <@datepicker id="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}" name="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}" value="${b_stateMap[b_col['CODE']]?default('')}" cssStyle="width:100%;height:24px"    view=true ></@datepicker>
                <#else>
                <@datepicker id="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}" nullable="${b_col['NULLABLE']}" name="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}" value="${b_stateMap[b_col['CODE']]?default('')}" cssStyle="width:100%;height:24px"  ></@datepicker>  
                </#if>
                </td>
                <#assign b_col_count = b_col_count + 1 />
               
                </#if>
                
                
                <#if b_col['DISPLAY_TYPE'] == 'SELECT'>
                  <td class="co<#if b_col['NULLABLE'] == 0 &&!readOnly> textfield_required</#if>">
                   <#if !b_col['_editable'] || readOnly ||(companyType =='ORGANIZATION'&& b_col['COMPANY_ID'] != companyId)>
                   
                   <@systemcode cssStyle="width:100%;height:18px" name="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}"  code="${b_col['SYSTEM_CODE_TYPE']}"  value="${b_stateMap[b_col['CODE']]?default('')}"  view=true />
                   <#else>
                    <@systemcode cssStyle="width:100%;height:18px" name="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}"  code="${b_col['SYSTEM_CODE_TYPE']}"  value="${b_stateMap[b_col['CODE']]?default('')}"  />
                   </#if>
                   </td>
                  <#assign b_col_count = b_col_count + 1 />
                </#if>
                
                <#if b_col['DISPLAY_TYPE'] == 'TEXTAREA'>
                  <td colspan="${6 - (b_col_count % 6)}" class="co<#if b_col['NULLABLE']== 0&&!readOnly > textfield_required</#if>">
                  <textarea name="__infoset__${b_table['TARGET_TABLE']}__${b_col['CODE']}"   class="textfield<#if !b_col['_editable']&&!readOnly> textfield_readonly</#if>"   style="height:60px;width:100%;" <#if !col['_editable']||readOnly||(companyType =='ORGANIZATION'&& b_col['COMPANY_ID'] != companyId) >readonly="readonly"  </#if>>${b_stateMap[b_col['NAME']]?default('')}</textarea>
                  </td>
                  <#assign b_col_count = b_col_count + 6 - (b_col_count % 6) />
                </#if>
                </td>
                <#if !b_col_has_next && (b_col_count) % 6 != 0>
                <#list 1..((6 - (b_col_count % 6))/2) as x>
                <td class="lab">&nbsp;</td>
                <td class="co">&nbsp;</td>
                </#list>
                </#if>
                 <#if readOnly || companyType =='ORGANIZATION'>  
                <#if b_col_count == 4 && b_col_has_next></tr><tr></#if>
                </#if>
                <#if (b_col_count) % 6 == 0 && b_col_has_next></tr><tr></#if>
                </#if>
              </#list>
            <tr>
				<td class="lab">
					<@s.text name="foundation.staff.memo"/>
				</td>
				<td colspan="5">
					<div class="fix-input"><@s.textarea rows="5" style="height:50px" name="staff.memo"  id="staffMemo" cssClass="cui-noborder-input" /></div>
				</td>
				<td class="lab">&nbsp;</td>
                <td class="co">&nbsp;</td>
			</tr>
          </table>
        <#else>
        
          <table class="cui-fd-infotable infoTable" style="width:96%;"  cellspacing="0" cellpadding="0">
              <#assign recs = stateCollection[stateKey] />
              <#assign cols = recs[0] />
              <#assign stateMap = recs[1] />
              <#assign table = recs[2] />
              <input type="hidden" name="__infoset__TARGET_FK_NAME" value="${table['TARGET_FK_NAME']}" />
              <input type="hidden" id="infosetEntityID" name="__infoset__${table['TARGET_FK_NAME']}" value="${entityId}" />
              
              <input type="hidden" name="__infoset__${table['TARGET_TABLE']}__ID" value="${stateMap['ID']?default('')}" />
              <#if table_flag["TARGET_TABLE"]=='BASE_LINKINFO'>
                <tr>
                  <td class="lab" style="width:13%;"><@s.text   name="foundation.staff.email"/></td>
                  <td style="width:20%;">
                 <#if readOnly || companyType =='ORGANIZATION'>
                 
                 <div class="fix-input-readonly"><@s.textfield   name="staff.email" cssClass="cui-noborder-input" readonly="true"/></div>
                 <#else>
               
                <div class="fix-input"> <@s.textfield   name="staff.email" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
                 </#if> 
                  </td>
                  <td class="lab" style="width:13%;"><@s.text   name="foundation.staff.mobile"/></td>
                  <td style="width:20%;">
                <#if readOnly || companyType =='ORGANIZATION'>
               <div class="fix-input-readonly"> <@s.textfield   name="staff.mobile" cssClass="cui-noborder-input" readonly="true" /></div>
                <#else>
               <div class="fix-input"> <@s.textfield   name="staff.mobile" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
                </#if> </td>
                
                  <td class="lab" style="width:13%;"><@s.text   name="foundation.staff.jabber"/></td>
                  <td style="width:20%;">   
                 <#if readOnly || companyType =='ORGANIZATION'>
                <div class="fix-input-readonly"> <@s.textfield   name="staff.jabber" cssClass="cui-noborder-input" readonly="true" /></div>
                 <#else>
                <div class="fix-input"> <@s.textfield   name="staff.jabber" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
                 </#if> 
                  </td>
                </tr>
              </#if>
              <tr>
              <#assign col_count = 0 />
              <#list cols as col>
              <#if col['SYSTEMDEFAULT'] == 0>
              
              <td style="width:13%;" class="lab <#if col['NULLABLE'] == 0&&!readOnly>cui-lmust</#if>">
                ${col['NAME']}
                <#assign col_count = col_count + 1 />
              </td>
              
              <#if col['DISPLAY_TYPE'] == 'TEXTFIELD'>
                  <td style="width:20%;" class="<#if col['NULLABLE'] == 0&&!readOnly> textfield_required</#if>">
                  <div class="fix-input<#if  !col['_editable'] || readOnly ||(companyType =='ORGANIZATION' && col['COMPANY_ID'] != companyId)>-readonly</#if>"><input type="text" nullable="${col['NULLABLE']}" fieldType="${col['TYPE']}" decimalDigits="${col['DECIMAL_DIGITS']?default('2')}"  maxLen="${col['FIELD_LENGTH']?default('1500')}" name="__infoset__${table['TARGET_TABLE']}__${col['CODE']}" value="${stateMap[col['CODE']]?default('')}" class="cui-noborder-input"   <#if  !col['_editable'] || readOnly ||(companyType =='ORGANIZATION' && col['COMPANY_ID'] != companyId)> readonly="true"  </#if> /></div>
                  </td>
                  <#assign col_count = col_count + 1 />
               </#if>    
                  
              <#if col['DISPLAY_TYPE'] == 'DATE' >
                <td style="width:20%;" class="<#if col['NULLABLE'] == 0 &&!readOnly> textfield_required</#if>">
                <#if  !col['_editable'] || readOnly ||(companyType =='ORGANIZATION' && col['COMPANY_ID'] != companyId)> 
                <@datepicker  name="__infoset__${table['TARGET_TABLE']}__${col['CODE']}" id="__infoset__${table['TARGET_TABLE']}__${col['CODE']}" value="${stateMap[col['CODE']]?default('')}"   view=true></@datepicker>
                <#else>
                 <@datepicker  name="__infoset__${table['TARGET_TABLE']}__${col['CODE']}" nullable="${col['NULLABLE']}" id="__infoset__${table['TARGET_TABLE']}__${col['CODE']}" value="${stateMap[col['CODE']]?default('')}"  ></@datepicker>
                </#if>
                </td>
                 <#assign col_count = col_count + 1 />
              </#if>
              
              <#if col['DISPLAY_TYPE'] == 'DATETIME'>
                <td style="width:20%;" class="<#if col['NULLABLE'] == 0 &&!readOnly> textfield_required</#if>">
                <input type="text" fieldType="${col['TYPE']}" decimalDigits="${col['DECIMAL_DIGITS']?default('2')}" maxLen="${col['FIELD_LENGTH']?default('100')}" name="__infoset__${table['TARGET_TABLE']}__${col['CODE']}" value="${stateMap[col['CODE']]?default('')}" class="textfield<#if !col['_editable']&&!readOnly> textfield_readonly</#if>"  <#if !col['_editable']||readOnly||(companyType =='ORGANIZATION'&& b_col['COMPANY_ID'] != companyId) >readonly="true"</#if> />
                </td>
                <#assign col_count = col_count + 1 />
              </#if>
              
              <#if col['DISPLAY_TYPE'] == 'SELECT'>
                <td style="width:20%;" class="<#if col['NULLABLE'] == 0 &&!readOnly> textfield_required</#if>">
                 <#if !col['_editable'] || readOnly ||(companyType =='ORGANIZATION'&& col['COMPANY_ID'] != companyId)>
	               	<@systemcode cssStyle="width:100%;height:18px;position:relative;" name="__infoset__${table['TARGET_TABLE']}__${col['CODE']}"  code="${col['SYSTEM_CODE_TYPE']}"  value="${stateMap[col['CODE']]?default('')}"  view=true />
	               <#else>
	                <@systemcode cssStyle="width:100%;height:18px;position:relative;" name="__infoset__${table['TARGET_TABLE']}__${col['CODE']}"  code="${col['SYSTEM_CODE_TYPE']}"  value="${stateMap[col['CODE']]?default('')}"  />
	               </#if>
                </td>
                <#assign col_count = col_count + 1 />
              </#if>
              
              <#if col['DISPLAY_TYPE'] == 'TEXTAREA'>
                 <td colspan="${6 - (col_count % 6)}" class="co<#if col['NULLABLE']== 0&&!readOnly > textfield_required</#if>">
                 <textarea name="__infoset__${table['TARGET_TABLE']}__${col['CODE']}"   class="textfield<#if !col['_editable']&&!readOnly> textfield_readonly</#if>" style="height:60px;width:100%;"   <#if !col['_editable']||readOnly||(companyType =='ORGANIZATION'&& b_col['COMPANY_ID'] != companyId) >readonly="readonly"</#if> >${stateMap[col['NAME']]?default('')}</textarea>
                 </td>
                 <#assign col_count = col_count + 6 - (col_count % 6) />
              </#if>
              
             
              <#if !col_has_next && (col_count) % 6 != 0>
              <#list 1..((6 - (col_count % 6))/2) as x>
              
	           <td class="lab">&nbsp;</td>
              <td class="co">&nbsp;</td>
              </#list>
              </#if>
              
              <#if (col_count) % 6 == 0 && col_has_next></tr><tr></#if>
              </#if>
              </#list>
              </tr>
          </table>
          </#if>
        </div>
      </#list>
    </#if>
    <#if (userPermisition?? && userPermisition?string=='true')>
     <#if !staffId??>
        <#if (enableCreateStaffIncludeUser?? && enableCreateStaffIncludeUser?string=='true')>
        <div style="overflow-y: auto; overflow-x: hidden;position: relative;">
	     	<table class="cui-fd-infotable infoTable" style="width:96%;"  cellspacing="0" cellpadding="0">
	     		<#if (pimsSafeAccessEnabled)?? && pimsSafeAccessEnabled>
	            <tr>
	              <td class="lab" style="width:13%;"><@s.text name="foundation.user.name"/></td>
	              <td style="width:20%;">
					 <div class="fix-input"><@s.textfield name="user.name" id="userName" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
	              </td>
	              
				   <td class="lab" style="width:13%;"><@s.text name="foundation.user.remoteAccessAllowed"/></td>
				   <td style="width:20%;">
					    <input type="radio" name="user.isAllowRemoteAccess" value="true" <#if (user.isAllowRemoteAccess)?? && user.isAllowRemoteAccess>checked</#if>/>${getHtmlText('common.radio.true')}
					    <input type="radio" name="user.isAllowRemoteAccess" value="false" <#if !((user.isAllowRemoteAccess)?? && user.isAllowRemoteAccess)>checked</#if>/>${getHtmlText('common.radio.false')}
				   </td>
				 
	              <td class="lab"><@s.text name="foundation.role.hisrole"/></td>
					<td>
						<@mneclient name="user_roleIds" id="user_roleIds" iframe=true displayFieldName="name" url="/msService/ec/foundation/role/common/roleListFrame?multiSelect=true&isUserRef=true" classStyle="cui-noborder-input" type="Role" cssStyle="width:97%;float:left;" onkeyupfuncname="getuser_roleIdsMultiInfo()" multiple=true clicked=true isEdit=true ids="${(defRole.id)!}" names="${(defRole.name)!}" useDefaultVal=true funcparam="isUserRef=true" conditionfunc="retrunRoleParam()"/>
					</td>
	              
	            </tr>
				<tr id="roleTr">
				  <td class="lab" style="width:13%;"><@s.text name="foundation.user.password"/></td>
	              <td style="width:20%;">
					 <div class="fix-input"><@s.password name="user.password" id="userPassword" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
				  </td>
	              <td class="lab" style="width:13%;"><@s.text name="foundation.user.repassword"/></td>
	              <td style="width:20%;">   
					<div class="fix-input"><@s.password name="userRePassword" id="userRePassword" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
	              </td>
				</tr>
				<#else>
				<tr>
	                <td class="lab" style="width:13%;"><@s.text name="foundation.user.name"/></td>
	                <td style="width:20%;">
					   <div class="fix-input"><@s.textfield name="user.name" id="userName" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
	                </td>
	              
	                <td class="lab"><@s.text name="foundation.role.hisrole"/></td>
					<td>
						<@mneclient name="user_roleIds" id="user_roleIds" iframe=true displayFieldName="name" url="/msService/ec/foundation/role/common/roleListFrame?multiSelect=true&isUserRef=true" classStyle="cui-noborder-input" type="Role" cssStyle="width:97%;float:left;" onkeyupfuncname="getuser_roleIdsMultiInfo()" multiple=true clicked=true isEdit=true ids="${(defRole.id)!}" names="${(defRole.name)!}" useDefaultVal=true funcparam="isUserRef=true" conditionfunc="retrunRoleParam()"/>
					</td>
				 	<td class="lab" style="width:13%;"><@s.text name="foundation.user.password"/></td>
	              	<td style="width:20%;">
					 	<div class="fix-input"><@s.password name="user.password" id="userPassword" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
				  	</td>
				</tr>
				<tr id="roleTr">
					 <td class="lab" style="width:13%;"><@s.text name="foundation.user.repassword"/></td>
	              	 <td style="width:20%;">   
						<div class="fix-input"><@s.password name="userRePassword" id="userRePassword" cssClass="cui-noborder-input" maxlen="80" fieldtype="TEXT"/></div>
	              	 </td>
					
				</tr>
				 </#if>
				 
	        </table>
        </div>
        </#if>
      </#if>
      </#if>
  </div>
</div>
</form>
<script type="text/javascript">
<#if (userPermisition?? && userPermisition?string=='true')>
function retrunRoleParam(){
	return " CID='${getCurrent('companyId')}'"
}
</#if>
$("#mainPositionId").mSelect();
CUI(function(){
	foundation.buttonBar = new CUI.ButtonBar( { panel: '#top_buttonbar' }  );
	
    new CUI.TabView("${stateColtId}",{tabposition:'top'<#if recordCollection?has_content>,height:320</#if>});
    //通过点击首页的用户名弹出的人员信心页面，隐藏掉右上角的上下翻动按钮
    var _location = location.search;
    if(_location.indexOf('from=homePage') != -1){
    	$('#info_page_order').css('display','none');
    }
});
</script>

<#if recordCollection?has_content>

<script type="text/javascript">
CUI(function(){
  new CUI.TabView("${recordColtId}",{tabposition:'top'});
  CUI("#__infoset__BASE_WORKINFO__JGSSJ").bind('blur', function(){
   		if(CUI("#__infoset__BASE_WORKINFO__YJZZSJ").val()==""&& CUI("#__infoset__BASE_WORKINFO__JGSSJ").val()!=""){
   			var jgssj= CUI("#__infoset__BASE_WORKINFO__JGSSJ").val();
   			var arr=jgssj.split('-');
   			var year=parseInt(arr[0],10);
   			var month=parseInt(arr[1],10);
   			var day=parseInt(arr[2],10);
   			var dd=new Date(year,month-1,day);
   			dd.setMonth(dd.getMonth()+3);
   			var lMonth=dd.getMonth()+1;
   			var lday=dd.getDate();
   			if(lMonth<10){lMonth='0'+lMonth;}
   			if(lday<10){lday='0'+lday;}
   			var yjzzsj=dd.getFullYear()+"-"+lMonth+"-"+lday;
   			CUI("#__infoset__BASE_WORKINFO__YJZZSJ").val(yjzzsj);
   		}
   
   });
   CUI("#staffworkStatusid").bind('onchange', function(){
   		if(CUI("#staffworkStatusid").val()=='STAFFSTATUSE_NATURE/STAFFSTATUTS_03'){
   			CUI("#staffworkStatusid").val("");
   			CUI("#staffworkStatusid").next().children().text("");
   		}
    });
  
});
</script>
<div id="${recordColtId}" class="etv-navset">
  <ul class="etv-nav">
    <#list recordCollection?keys as recordKey>
    <#assign recs1 = recordCollection[recordKey] />
    <#assign tagTable = recs1[2] />
    <li _click="createDateTable_${tagTable['TARGET_TABLE']}('${tagTable['TARGET_TABLE']}','${recs1[0][0]["TABLE_CODE"]}')" num="${recordKey_index}">${getHtmlText(recordKey)}</li>
    </#list>
  </ul>
<div>
<div class="etv-content" style="margin-top:5px;">
  <#list recordCollection?keys as recordKey>
  <#assign recs = recordCollection[recordKey] />
  <#assign cols = recs[0] />
  <#assign table = recs[2] />

      <input type="hidden" id="__infoset__delRowIDS__${table['TARGET_TABLE']}"  />
      <div id="${recordColtId}_${recordKey_index}" style="margin:0px 4px 0px 4px;"></div>
      <script type="text/javascript">
      $(function(){
      	$(".info_icon").hover(function(){
      		//$(this).addClass("hover");
      		var class_hover=$(this).attr("class").split(' ')[1]+"_hover";
      		$(this).addClass(class_hover);
      	},
      	function(){
      		//$(this).removeClass("hover");
      		var class_hover=$(this).attr("class").split(' ')[1]+"_hover";
      		$(this).removeClass(class_hover);
      	})
      })
      var DT_${table['TARGET_TABLE']}; 
      function createDateTable_${table['TARGET_TABLE']}(tableName,tableCode){
      //  DT_${table['TARGET_TABLE']};
          var noNestColumnDefs = new Array();
		       
          var validatorConfigs=new Object();
          <#list cols as col>
          	 var ${col["CODE"]?upper_case}_validatorArr=new Array();    
            var testnode = new Object();
            testnode.key = '${col["CODE"]?upper_case}';
            testnode.label = '${getText(col["NAME"])}';
            testnode.width = '${col["FIELDWIDTH"]?default('100')}';
          
            <#if !col['_editable']||readOnly||(companyType =='ORGANIZATION'&& cols[0]['COMPANY_ID']!= companyId) >
               testnode.edit = false;
            <#else>
               testnode.edit = true;
            </#if>
            
           
          
            <#if col['NULLABLE'] == 0>
             	${col["CODE"]?upper_case}_validatorArr.push("require");
            </#if>
            <#if col["TYPE"]?lower_case == 'integer'>
            	${col["CODE"]?upper_case}_validatorArr.push("isInt");
            	//testnode.testReg=/^[-+]?\d+$/;
            	testnode.type ='${col["TYPE"]?lower_case}';
            	testnode.emptyIsEmpty = true;
            <#elseif col["TYPE"]?lower_case =='decimal'>
            	${col["CODE"]?upper_case}_validatorArr.push("isFloat");
            	//testnode.testReg=/^\d+\.*\d*$/;
            	testnode.type ='${col["TYPE"]?lower_case}';
            	testnode.emptyIsEmpty = true;
            <#elseif col["TYPE"]?lower_case =='date'>
            	${col["CODE"]?upper_case}_validatorArr.push("isDate");
            	testnode.type ='${col["TYPE"]?lower_case}';
            	//testnode.testReg=/^(\d{4})(-|\/)(\d{2})\2(\d{2})$/;
            </#if>
            if(${col["CODE"]?upper_case}_validatorArr.length>0){
            	validatorConfigs.${col["CODE"]?upper_case}=${col["CODE"]?upper_case}_validatorArr;
            }
			
            <#if col['DISPLAY_TYPE'] == 'SELECT'>
            testnode.type = 'select';
            testnode.options = {
            <#if systemCodeCollection[col['CODE']]?has_content>
            <#assign scs = systemCodeCollection[col['CODE']] />
            <#list scs?keys as systemCodeKey>
            "${systemCodeKey}" : "${scs[systemCodeKey]}"<#if systemCodeKey_has_next>,</#if>
            </#list>
            </#if>
            };
            </#if>
            noNestColumnDefs[noNestColumnDefs.length] = testnode;
            
          </#list>
         if($( '#staffs_infoset_recordcolt li.selected' ).attr('hasRequested') == undefined && ${recordKey_index} != 0  ){
        
        	 var url1 = encodeURI('/foundation/infoset/record-colt-json');
			   DT_${table['TARGET_TABLE']} = new CUI.DataGrid({
			       	 sContainerId : "${recordColtId}_${recordKey_index}",
			       	 aColumnDefs : noNestColumnDefs,
			       	 aDataSource : null,
			       	 oConfigs : {
			              ShowSetPath : '/foundation/infoset/record-colt-json?infoSetTableCode=${cols[0]["TABLE_CODE"]}&entityId=${entityId.toString()}',
			              paginator : true,
			              method : "POST",
			              editable:true,
			              postData : 'infoSetTableCode=${cols[0]["TABLE_CODE"]}&entityId=${entityId.toString()}',
			         	  dataUrl :  url1,
			              <#if !readOnly && !(cols[0]['COMPANY_ID']!=companyId && companyType=='ORGANIZATION') >
			              buttons : [{text:"<@s.text name='foundation.staff.addRow'/>",iconClass:"cui-btn-add",handler:function(event){this.addNewRow(event);}},{text:" <@s.text name='foundation.staff.deleteRow'/> ",iconClass:"cui-btn-del",handler:${table['TARGET_TABLE']}_deleteRows}],
			            </#if> 
			              hideKey : ['ID','CODE']
			                            
			         },
		  	         validatorConfig : validatorConfigs
			        
			   	 })
			   
        
        
           /* DT_${table['TARGET_TABLE']} = new CUI.DataTable("${recordColtId}_${recordKey_index}",noNestColumnDefs,null,{
              ShowSetPath : '/foundation/infoset/record-colt-json?infoSetTableCode=${cols[0]["TABLE_CODE"]}&entityId=${entityId.toString()}',
                  paginator : true,
                  method : "POST",
                  editable:true,
                  <#if !readOnly  && !(cols[0]['COMPANY_ID']!=companyId && companyType=='ORGANIZATION')> 
                  buttons : [{text:"<@s.text name='foundation.staff.addRow'/>",iconClass:"cui-btn-add",handler:function(event){this.addNewRow(event);}},{text:" <@s.text name='foundation.staff.deleteRow'/> ",iconClass:"cui-btn-del",handler:${table['TARGET_TABLE']}_deleteRows}],
                  </#if>  
                  hideKey : ['ID','CODE']
                  
            });
            
            
            var url = encodeURI('/foundation/infoset/record-colt-json?infoSetTableCode=' + tableCode + '&entityId=${entityId.toString()}');
            var oStr = 'DT_' + tableName + '.setRequestDataUrl(\'' + url + '\')';
            eval(oStr);*/
            $( '#staffs_infoset_recordcolt li.selected' ).attr('hasRequested','true');
      }
 
}
		<#list cols as col>
			<#if col["TYPE"]?lower_case =='decimal'>
				function ${table['TARGET_TABLE']}_${col["CODE"]?upper_case}_jsCode(value){
					var decLength=${col["DECIMAL_DIGITS"]};
					var maxLeng=${col["FIELD_LENGTH"]};
					var valueObj=value+"";
					var index=valueObj.indexOf(".");
					if(index>-1){
						valueObj=valueObj+"00000000000";
						valueObj=valueObj.substring(0,parseInt(index,10)+parseInt(decLength,10)+1);
						valueObj=valueObj.replace ('.','');
					}else{
						valueObj=parseInt(valueObj,10)*Math.pow(10, parseInt(decLength,10))
					
					}
					
					var intObj=parseInt(valueObj,10);
					intObj=intObj+"";
			        if(maxLeng<intObj.length ){
			        	return false;
			        }
			        return true;
				
				}
					
			</#if>
		</#list>    
        function ${table['TARGET_TABLE']}_deleteRows(){
          try{
	          var delRowIDS=CUI("#__infoset__delRowIDS__${table['TARGET_TABLE']}").val();
	          var deleteRow= DT_${table['TARGET_TABLE']}._DT.delRow()[0];
	          tepID = deleteRow.ID;
	          if(delRowIDS!=""){
	            delRowIDS+=','+tepID;
	          }else{
	            delRowIDS= tepID
	          }
	          CUI("#__infoset__delRowIDS__${table['TARGET_TABLE']}").val(delRowIDS);
	      }catch(e){
	      	CUI.Dialog.alert("${getHtmlText('foundation.infoSet.deleteRows')}");
	      }    
        }
       
       
       YUE.onDOMReady(function(){
          <#if !((staff.workStatus.id)??)>
          $('[name="staff.workStatus.id"]').setValue('STAFFSTATUSE_NATURE/STAFFSTATUTS_02');
          </#if>
          function thefirstTable(){
          var noNestColumnDefs = new Array();
          var validatorConfigs=new Object();
          <#list cols as col>
          	var ${col["CODE"]?upper_case}_validatorArr=new Array(); 
            var testnode = new Object();
            testnode.key = '${col["CODE"]?upper_case}';
            testnode.label = '${getText(col["NAME"])}';
            testnode.width = '${col["FIELDWIDTH"]?default('100')}';
            
            <#if !col['_editable']||readOnly||(companyType =='ORGANIZATION'&& cols[0]['COMPANY_ID']!= companyId) >
               testnode.edit = false;
            <#else>
               testnode.edit = true;
            </#if>
            

            <#if col['NULLABLE'] == 0>
            	${col["CODE"]?upper_case}_validatorArr.push("require");
            </#if>
            <#if col["TYPE"]?lower_case == 'integer'>
              ${col["CODE"]?upper_case}_validatorArr.push("isInt");
              testnode.type ='${col["TYPE"]?lower_case}';
              testnode.emptyIsEmpty = true;
            <#elseif col["TYPE"]?lower_case =='decimal'>
               ${col["CODE"]?upper_case}_validatorArr.push("isFloat");
              testnode.type ='${col["TYPE"]?lower_case}';
              testnode.emptyIsEmpty = true;
            <#elseif col["TYPE"]?lower_case =='date'>
              testnode.type ='${col["TYPE"]?lower_case}';
              ${col["CODE"]?upper_case}_validatorArr.push("isDate");
            </#if>
            if(${col["CODE"]?upper_case}_validatorArr.length>0){
            	validatorConfigs.${col["CODE"]?upper_case}=${col["CODE"]?upper_case}_validatorArr;
            }
            
            <#if col['DISPLAY_TYPE'] == 'SELECT'>
            testnode.type = 'select';
            testnode.options = {
            <#if systemCodeCollection[col['CODE']]?has_content>
            <#assign scs = systemCodeCollection[col['CODE']] />
            <#list scs?keys as systemCodeKey>
            "${systemCodeKey}" : "${scs[systemCodeKey]}"<#if systemCodeKey_has_next>,</#if>
           </#list>
            </#if>
            };
            </#if>
            noNestColumnDefs[noNestColumnDefs.length] = testnode;
          </#list>  
          
       if(${recordKey_index} == 0 ){
         
       var tableWrapper = YUD.getElementsByClassName("etv-content")[1].clientHeight;
       var tableHeight;
       if(YAHOO.env.ua.ie == "8"){
           tableHeight = tableWrapper - 47 + "px";
       }else if(YAHOO.env.ua.ie == "6"){
           tableHeight = tableWrapper - 46 + "px";
       }else{
           tableHeight = tableWrapper - 46 + "px";
       }
       
       var url = encodeURI('/foundation/infoset/record-colt-json');
	   DT_${table['TARGET_TABLE']} = new CUI.DataGrid({
	       	 sContainerId : "${recordColtId}_${recordKey_index}",
	       	 aColumnDefs : noNestColumnDefs,
	       	 aDataSource : null,
	       	 oConfigs : {
	              ShowSetPath : '/foundation/infoset/record-colt-json?infoSetTableCode=${cols[0]["TABLE_CODE"]}&entityId=${entityId.toString()}',
	              paginator : true,
	              method : "POST",
	              editable:true,
	              postData : 'infoSetTableCode=${cols[0]["TABLE_CODE"]}&entityId=${entityId.toString()}',
	        	  dataUrl :  url,	 
	              <#if !readOnly && !(cols[0]['COMPANY_ID']!=companyId && companyType=='ORGANIZATION') >
	              buttons : [{text:"<@s.text name='foundation.staff.addRow'/>",iconClass:"cui-btn-add",handler:function(event){this.addNewRow(event);}},{text:" <@s.text name='foundation.staff.deleteRow'/> ",iconClass:"cui-btn-del",handler:${table['TARGET_TABLE']}_deleteRows}],
	            </#if> 
	              hideKey : ['ID','CODE']
	                            
	         },
	         validatorConfig : validatorConfigs
  	      
	   	 })
       	/*
         DT_${table['TARGET_TABLE']} = new CUI.DataTable("${recordColtId}_${recordKey_index}",noNestColumnDefs,null,{
              ShowSetPath : '/foundation/infoset/record-colt-json?infoSetTableCode=${cols[0]["TABLE_CODE"]}&entityId=${entityId.toString()}',
                  paginator : true,
                  width:document.body.clientWidth - 32,
                  height:tableHeight,
                  method : "POST",
                  editable:true,
                  <#if !readOnly && !(cols[0]['COMPANY_ID']!=companyId && companyType=='ORGANIZATION') >
                  buttons : [{text:"<@s.text name='foundation.staff.addRow'/>",iconClass:"cui-btn-add",handler:function(event){this.addNewRow(event);}},{text:" <@s.text name='foundation.staff.deleteRow'/> ",iconClass:"cui-btn-del",handler:${table['TARGET_TABLE']}_deleteRows}],
                </#if> 
                  hideKey : ['ID','CODE']
                            
         });
          var url = encodeURI('/foundation/infoset/record-colt-json?infoSetTableCode=${cols[0]["TABLE_CODE"]}&entityId=${entityId.toString()}');
            var oStr = DT_${table['TARGET_TABLE']}.setRequestDataUrl(url);
		*/
           
            $( '#staffs_infoset_recordcolt li.selected' ).attr('hasRequested','true');
      } }
         thefirstTable()
       })
       
      </script>
  </#list>
</div>
</#if>
	
</#macro>