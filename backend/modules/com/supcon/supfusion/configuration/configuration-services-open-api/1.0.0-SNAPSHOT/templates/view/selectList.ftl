	<#if (ev.configMap)??  && (ev.configMap.layout)??>
		<#assign configMap = (ev.configMap.layout)>
		<#if (configMap.sections)??>
		<#list (configMap.sections) as section>
			
			<#if (section.regionType)?? && (section.regionType)=='BUTTON'>
				<#assign serviceName = section.serviceName!''>
				<#if section.cells??>
					<#list (section.cells) as operateButton>
					<#if operateButton.ispermission?? && operateButton.ispermission>
						<li cellCode="${ecCodeInit()}" id="${(operateButton.id)!}" namekey="${i18KeyReplaceMap[operateButton.namekey]}" showname="${((operateButton.showname)!)?html}" buttonstyle="${((operateButton.buttonstyle)!)?html}"
						<#if (operateButton.useInMore)??>
							useInMore="${(operateButton.useInMore)?string('true','false')}" 
						<#else>
							useInMore="false" 
						</#if>
						<#if (operateButton.funcname)??>
							funcname="${((operateButton.funcname)!)?html}" 
						</#if>
						<#if (operateButton.funcbody)??>
							funcbody="${((operateButton.funcbody)!)?html}"
						</#if>
						<#if (operateButton.operatetype)??>
							operatetype="${((operateButton.operatetype)!)?html}"
						</#if>
						<#if (operateButton.viewselect)??>
							viewselect="${((operateButton.viewselect)!)?html}"
						</#if>
						<#if (operateButton.opentype)??>
							opentype="${((operateButton.opentype)!)?html}"
						</#if>
						<#if (operateButton.iscallback)??>
							iscallback="${(operateButton.iscallback)?string('true','false')}"
						</#if>
						<#if (operateButton.iscustomfunc)??>
							iscustomfunc="${(operateButton.iscustomfunc)?string('true','false')}"
						</#if>
						<#if (operateButton.ispermission)??>
							ispermission="${(operateButton.ispermission)?string('true','false')}"
						</#if>
						<#if (operateButton.operateurl)??>
							operateurl="${((operateButton.operateurl)!)?html}"
						</#if>
						<#if (operateButton.isconfirm)??>
							isconfirm="${(operateButton.isconfirm)?string('true','false')}"
						</#if>
						<#if (operateButton.isHide)??>
							isHide="${(operateButton.isHide)?string('true','false')}"
						</#if>
						<#if (operateButton.confirmcontent)??>
							confirmcontent="${((operateButton.confirmcontent)!)?html}"
						</#if>
						<#if (operateButton.selectType)??>
							selectType="${((operateButton.selectType)!false)?string('true','false')}"
						</#if>
						<#if (operateButton.scriptCode)??>
							scriptCode="${((operateButton.scriptCode)!)?html}"
						</#if>
						<#if (operateButton.isPublished)??>
							isPublished="${(operateButton.isPublished)?string('true','false')}"
						</#if>
						<#if (ev.view.name)??>
							permissionFromName="${((ev.view.name)!)?html}" 
						</#if>
						<#if (ev.view.code)??>
							permissionFromCode="${((ev.view.code)!)?html}" 
						</#if>
						 class="button_design_ul_li" onmousedown="listec.selectListLi(this)" <#if !(operateButton.operatetype)?? || ((operateButton.operatetype)?? && (operateButton.operatetype) != 'SEPARATE')>ondblclick="listec.modifyOperateButton()"></#if><dd><#if (operateButton.operatetype)?? && (operateButton.operatetype) == 'SEPARATE'><input type="button" style="width:30px" value="|"><#else><input type="button" class="Dialog_button btn_pointer" value="${getText('${(i18KeyReplaceMap.get(operateButton.namekey!))!(operateButton.showname)!}')}"></#if></dd></li>
					</#if>
					</#list>
				</#if>
				<#break>
			</#if>
			</ul>
		</#list>
		</#if>
	</#if>