<#if parameters.type == 'basic'>
<script type="text/javascript">
	var pager = {};
	pager.CurrentPage = "1";
	pager.TotalPages = "5";
	pager.TotalList = "100";
	pager.PageList = "20";
	function enterPress(event,inputObj){
	  if(event.keyCode == 13){	  
	   // DTobj.setRequestData('pageSize='+inputObj.value);
	  }
	}
	function pageChoose(thisobj,pagesize){
	  var PageNo = thisobj.value;      
	  //  DTobj.setRequestData('pageNo='+PageNo+'&pageSize='+pagesize);         
	}
	function pagenav(ev,page){
		//this.setRequestData('pageNo='+page+'&pageSize='+pager.pageSize);
    }
</script>

<#--<table>
  <thead>
    <tr>
      <#list parameters.columns as col>
        <th>${col.label}</th>
      </#list>
    </tr>
  </thead>
  <tbody>
    <#if parameters.pager?? && parameters.pager.result??>
    <#list parameters.pager.result as data>
      <tr>
        <#list parameters.columns as col>
          <td>data[col.key]</td>
        </#list>
      </tr>
    </#list>
    </#if>
  </tbody>
</table>
<div>${parameters.pager.pageNo}</div>-->

<div class="basictable-wrapper" id="${parameters.id}" >
	<div class="basictable-snt">序号</div>
	<div class="basictable-sn">		
		<div class="basictable-snbg"></div>		
	</div>
	<div class="basictable-hd">
		<table cellspacing="0" cellpadding="0">
			<thead>
				<#list parameters.columns as col>
			        <th style="border-right:1px solid #FFF;height:23px;line-height:23px;white-space:nowrap;">${col.label}</th>
			    </#list>
			</thead>
		</table>
	</div>
	<div class="basictable-bd" style="height:348px;"> 
		<table>
			<#if parameters.pager?? && parameters.pager.result??>
		    <#list parameters.pager.result as data>
		      <tr>
		        <#list parameters.columns as col>
		          <td>data[col.key]</td>
		        </#list>
		      </tr>
		    </#list>
		    </#if>
		</table>
	</div>
	<div class="paginatorbar">
		<div class="paginatorbar-pageoperate"><a href="#" class="set-button">操作</a></div>
      <div class="paginatorbar-pageinfo">
	    <div class="total-pagecount"><span>共</span><em id="PageLink_TotalList"></em><span>条</span></div>
	    <div class="display-pagecount">
	      <span>每页显示</span>
	      <input type="text" id="PageLink_PageList" value=""  onkeypress="enterPress(event,this)"></input>
	      <span>条</span>
	    </div>
	    <div class="current-pagecount">
	      <select id="PageLink_PageSelect" onchange="pageChoose(this,pager.TotalPages)"></select>
	      <span>/<em id="PageLink_TotalPages"></em>页</span>
	    </div>
	    <div class="turn-pagecount">
	      <span class="firstpage" onclick="pagenav(event,1)"></span>
	      <span class="prevpage" onclick="pagenav(event,2)">上一页</span>
	      <span class="nextpage" onclick="pagenav(event,3)">下一页</span>
	      <span class="lastpage" onclick="pagenav(event,pager.TotalPages)"></span>
	    </div>
	  </div>
    </div>
	<div class="overlayerWrap">
		<div class='overlayer-title'><span>X</span></div>
		<div class='overlayer-content' >
			内容内容内容内容内容内内容内容内容内容内容内容内容
		</div>
	</div>
</div>

<script type="text/javascript">
	YAHOO.util.Event.onDOMReady(function() {
	CUI.BasicTable = function(sId, oConfigs){
		this.id = sId;
		this._configs = oConfigs || {};
		this.otable = document.getElementById(this.id);
		this.sWidth = this.otable.clientWidth;
		this._init();
	}
	CUI.BasicTable.prototype =  {
      _init: function(){
       var basictablewrapper = document.getElementById(this.id);
        var basictablebd = this.otable.children[3];
        var basictablesn = this.otable.children[1];   
        var basictablesnbg = basictablesn.children[0];
        var basictablehd = this.otable.children[2];
        var overlayerWrap = this.otable.children[5];
        if(this._configs.height){
          this.otable.style.height = parseInt(this._configs.height) +"px";
          basictablesn.style.height = parseInt(this._configs.height) - 60 +  "px";
          basictablebd.style.height = parseInt(this._configs.height) - 43 + "px";
        }else{
            var ancestor = basictablewrapper.parentNode;
            if (ancestor && ancestor.tagName != 'BODY') {
             if(YAHOO.env.ua.ie == "6"){
               var sorHeight = parseInt(ancestor.clientHeight) - 200;
             }else{
               var sorHeight = parseInt(ancestor.clientHeight) - 238;
             }
            }else {
                var sorHeight = parseInt(document.documentElement.clientHeight) - 60;
               
            }
            basictablesn.style.height = parseInt(sorHeight) - 60 + "px";
            basictablebd.style.height = parseInt(sorHeight) - 43 + "px";
            basictablewrapper.style.height = parseInt(sorHeight) - 0 + "px";
        }
        if(YAHOO.env.ua.ie == "6"){
         basictablehd.style.width = parseInt(this.sWidth) - 40 + "px";
         basictablebd.style.width = parseInt(this.sWidth) - 40 + "px";
        }
        basictablebd.onscroll = function(){
          basictablehd.scrollLeft = basictablebd.scrollLeft;
          basictablesn.scrollTop = basictablebd.scrollTop;
        }
        if(this.otable.children[4]){
          var overlayon = this.otable.children[4].children[0].children[0];
          overlayon.onclick = function(){
          overlayerWrap.style.display = "block";
          overlayerWrap.style.left = "6px";
          overlayerWrap.style.bottom = "20px";
          var bwHeight = parseInt(basictablewrapper.clientHeight) - 46;
          var owHeight = parseInt(overlayerWrap.clientHeight);
          if( owHeight > bwHeight){
            overlayerWrap.style.height = bwHeight + "px";
            overlayerWrap.children[1].style.height = bwHeight - 38 + "px";
            overlayerWrap.children[1].style.overflowY = "scroll";
          }}
        }
        if(this.otable.children[5]){
          var overlayoff = this.otable.children[5].children[0].children[0];
          overlayoff.onclick = function(){
            overlayerWrap.style.display = "none";
          }
        }
        var ipt= document.getElementById('PageLink_PageList'); 
        ipt.value = pager.PageList;
        var iem= document.getElementById('PageLink_TotalList'); 
        iem.innerHTML = pager.TotalList;
		var sel= document.getElementById('PageLink_PageSelect'); 
		for(var i = 1; i <= pager.TotalPages;i++){
		      sel.options[sel.length] = new Option(i,i,false,true);     
		}
		sel.value = "1";
		for(var i=0;i<sel.length;i++){
		  if(sel.options[i].value == sel.value){
			    sel.options[i].selected=true;
			    break;
		    }
		}
		var tem= document.getElementById('PageLink_TotalPages'); 
        tem.value = pager.TotalPages;
      },
      resizetable: function(){
          this.otable.children[1].style.height = parseInt(this.otable.clientHeight) - 60 + "px";
          this.otable.children[3].style.height = parseInt(this.otable.clientHeight) - 43 + "px";
      }
      
    } 
    
    var bt =  new CUI.BasicTable("${parameters.id}"); 
    }) 
</script>


<#else>

<#if parameters.noscript?default(false) == false>
	<script type="text/javascript">
	  YAHOO.util.Event.onDOMReady(function() {

		var noNestColumnDefs = new Array();
			<#list parameters.columns as col>
				var testnode = new Object();
				testnode.key = '${col.key}';
				testnode.label = '${col.label}';
				<#if col.type??>
					<#if '${col.type}' == 'integer'>
		              testnode.testReg=/^[-+]?\d+$/;
		              testnode.type ='integer';
		            <#elseif '${col.type}' =='decimal'>
		              testnode.testReg=/^\d+\.*\d*$/;
		              testnode.type ='decimal';
		            <#elseif '${col.type}' =='date'>
		              testnode.type ='date';
		              testnode.testReg=/^(\d{4})(-|\/)(\d{2})\2(\d{2})$/;
		            <#else>
		              testnode.type = '${col.type}';
		              <#if col.testReg??>testnode.testReg = '${col.testReg}';</#if>
		            </#if>
		        </#if>    
				<#if col.defaultTitle??>testnode.defaultTitle = '${col.defaultTitle}';</#if>
				<#if col.editable??>testnode.edit = ${col.editable};</#if>
				<#if col.notnull??>testnode.notnull = ${col.notnull};</#if>
				testnode.width= ${col.width};
				<#if col.options??>
				testnode.options = <#rt/>
					<#if col.optionsMap?has_content>
					{
						<#list col.optionsMap?keys as key>
							'${key}' : '${col.optionsMap[key]}'<#if key_has_next>,</#if>
						</#list>
					};
					<#else>${col.options};</#if>
				</#if>				
				<#if col.click??>testnode.colclick=${col.click};</#if>
				<#if col.checkall??>testnode.checkall='${col.checkall}';</#if>
				<#if col.checkallclick??>testnode.checkallclick=${col.checkallclick};</#if>
				<#if col.textalign??>testnode.textalign='${col.textalign}';</#if>
				<#if col.sortable?default('true') == 'true'>testnode.sortable=true;</#if>
				<#if col.sum??>testnode.sum=${col.sum};</#if>
				<#if col.stat??>testnode.stat=${col.stat};</#if>
				<#if col.fieldType??>testnode.fieldType =" ${col.fieldType}"</#if>
				<#if col.selectCompType??>testnode.selectCompType = "${col.selectCompType}"</#if>
				<#if col.funcname??>testnode.funcname = "${col.funcname}"</#if>
				<#if col.length??>testnode.length = "${col.length}"</#if>
				<#if col.callbackname??>testnode.callbackname =" ${col.callbackname}"</#if>
				noNestColumnDefs[noNestColumnDefs.length]=testnode;
			</#list>
			
			<#if parameters.showSetPath??>
				 var call = {success:handleSuccess};
			     var transaction = YAHOO.util.Connect.asyncRequest('${parameters.transMethod?default('POST')}', encodeURI('${parameters.showSetPath}'), call);   
			     function handleSuccess(o){
							 pagaData = YAHOO.lang.JSON.parse(o.responseText);
							 ${parameters.widget} = new CUI.DataTable("${parameters.id}",noNestColumnDefs, 
			    		 	 [pagaData], 
				      		 {
				              <#if parameters.height gt 0>height: ${parameters.height},</#if>
				              <#if parameters.width gt 0>width:${parameters.width},</#if>
				              <#if parameters.caption??>caption: "${parameters.caption}",</#if>
				              <#if parameters.formId??>formId: "${parameters.formId}",</#if>
				              ShowSetPath:"${parameters.showSetPath}",
				              <#if parameters.rowClick??>trclick:${parameters.rowClick},</#if>
				              <#if parameters.editable>editable:true,</#if>
				              <#if parameters.paginator?default('true') == 'true'>paginator:true,</#if>
				              <#if parameters.dblclick??>dblclick:"${parameters.dblclick}",</#if>
				              <#if parameters.hidekey??>hideKey:${parameters.hidekey},</#if>
				              <#if !parameters.complex>complex:false,</#if>
				              <#if parameters.fbuttons??>fbuttons:${parameters.fbuttons},</#if>
					          <#if parameters.buttons??>buttons:${parameters.buttons},</#if>
					          <#if parameters.custombtns??>custombtns:${parameters.custombtns},</#if>
					          <#if parameters.multiselect>multiselect:true,</#if>
					          <#if parameters.tfoot??>tfoot:${parameters.tfoot},</#if>
					          <#if parameters.autoaddrow>autoAddRow:${parameters.autoaddrow},</#if>
					          <#if parameters.dtPage??>dtPage:"${parameters.dtPage}",</#if>
					          <#if parameters.dgattribute>dbAttribute:true,</#if>
				              method:"${parameters.transMethod?default('POST')}"
				             });
				}
			<#else>
			 	${parameters.widget} = new CUI.DataTable("${parameters.id}",
					noNestColumnDefs, 
					${parameters.myDataSource}, 
			  		{
			  			<#if parameters.condition??>ClassCondition: "${parameters.condition}",</#if>
			            <#if parameters.caption??>caption: "${parameters.caption}",</#if>
			            height: ${parameters.height},
			            width:${parameters.width},
			            <#if parameters.editable>editable:true,</#if>
			            <#if parameters.hidekey??>hideKey:${parameters.hidekey},</#if>
			            <#if parameters.dblclick??>dblclick:"${parameters.dblclick}",</#if>
			            <#if parameters.fbuttons??>fbuttons:${parameters.fbuttons},</#if>
			            <#if parameters.buttons??>buttons:${parameters.buttons},</#if>
			            <#if parameters.custombtns??>custombtns:${parameters.custombtns},</#if>			            
			            <#if parameters.paginator?default('true') == 'true'>paginator:true,</#if>
			            <#if parameters.multiselect>MultiSelect:true,</#if>
			            <#if parameters.tfoot??>tfoot:${parameters.tfoot},</#if>
			            <#if !parameters.complex>complex:false,</#if>
					    <#if parameters.autoaddrow>autoAddRow:${parameters.autoaddrow},</#if>
					    <#if parameters.dgattribute>dbAttribute:true,</#if>
					    <#if parameters.dtPage??>dtPage:"${parameters.dtPage}",</#if>
			            <#if parameters.DataTableType??>responseType:"${parameters.DataTableType}",</#if>
			            method:"${parameters.transMethod?default('POST')}"
			        });
			</#if>
	    
	  });	
	</script>
	<div id="${parameters.id}" <#if parameters.cssStyle??> style="${parameters.cssStyle}"</#if> >
	</div>
	
	<#else>
	<div id="${parameters.id}" <#if parameters.cssStyle??> style="${parameters.cssStyle}"</#if> >
		<table _export="true">
			<thead>
				<tr>
				<#list  parameters.columns as col>
				<th>${col.label}</th>
				</#list>
				</tr>
			</thead>			
			<tbody>
			    <#list  parameters.valueData as row>
				 <tr>
					<#list parameters.columns as col>
					<#assign ks = col.key?split('.') />
					
					<#assign r = row />
					<#list ks as k>
					<#assign r = r[k]?default('')/>
					</#list>
					
					<td>${r}</td>
					</#list>				 	
				 </tr>
				</#list> 
			</tbody>
		</table>
	</div> 
	</#if>

	
</#if>