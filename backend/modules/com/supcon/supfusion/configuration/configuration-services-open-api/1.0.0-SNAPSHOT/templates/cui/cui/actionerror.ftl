<#--
/*
 * $Id: actionerror.ftl 805635 2009-08-19 00:18:54Z musachy $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
-->
<#if (actionErrors?? && actionErrors?size > 0)>
	<ul<#rt/>
 id="actionErrorMessageUL"<#rt/>
<#if parameters.cssClass??>
 class="${parameters.cssClass?html}"<#rt/>
<#else>
 class="errorMessage"<#rt/>
</#if>
<#if parameters.cssStyle??>
 style="${parameters.cssStyle?html}"<#rt/>
</#if>
>
	<#list actionErrors as error>
		<#if error?if_exists != "">
            <li><span><#if parameters.escape>${error!?html}<#else>${error!}</#if></span><#rt/></li><#rt/>
        </#if>
	</#list>
	</ul>
	
	<script type="text/javascript">
		 try {
		 	
	    	//调用window.js显示错误信息，并定位到第一个出错的field
	    	var errorMessageLayer = new CUI.Overlay({
				    el: "actionErrorMessageUL",
				    title: '错误',
					alignCenter:true,
					shadow:true
				});
			
	    	errorMessageLayer.on('renderEvent', function(){
				var checkButton = errorMessageLayer.addButton("确定");
				YAHOO.util.Event.on(checkButton, "click", function(){
					errorMessageLayer.hide();
		        })
			});
			errorMessageLayer.render();
			errorMessageLayer.show();
	    } catch (e) {
	        alert(e);
		}
	</script>
	
</#if>