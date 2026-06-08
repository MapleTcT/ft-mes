<?xml version="1.0" encoding="UTF-8"?>
<CustomProperty>
	<#if cpmList?? && cpmList?has_content>
	<CustomPropertyModelMappings>
		<#list cpmList as item>
		<CustomPropertyModelMapping>
			<displayName>${(item.displayName)!}</displayName>
			<fieldType>${(item.fieldType)!}</fieldType>
			<format>${(item.format)!}</format>
			<fillContent><![CDATA[${(item.fillContent)!} ]]></fillContent>
			<multable>${(item.multable?string)!}</multable>
			<seniorSystemCode>${(item.seniorSystemCode?string)!}</seniorSystemCode>
			<associatedProperty_code>${(item.associatedProperty.code)!}</associatedProperty_code>
			<associatedType>${(item.associatedType)!}</associatedType>
			<refView_code>${(item.refView.code)!}</refView_code>
			<nullable>${(item.nullable?string)!}</nullable>
			<enableCustom>${(item.enableCustom?string)!}</enableCustom>
			<property_code>${(item.property.code)!}</property_code>
			<model_code>${(item.model.code)!}</model_code>
			<description><![CDATA[${(item.description)!}]]></description>
			<sort>${(item.sort)!}</sort>
			<relatedKey>${(item.relatedKey)!}</relatedKey>
			<precision>${(item.precision)!}</precision>
		</CustomPropertyModelMapping>
		</#list>
	</CustomPropertyModelMappings>
	</#if>
	<#if cpvList?? && cpvList?has_content>
	<CustomPropertyViewMappings>
		<#list cpvList as item>
		<CustomPropertyViewMapping>
			<displayName>${(item.displayName)!}</displayName>
			<fieldType>${(item.fieldType)!}</fieldType>
			<format>${(item.format)!}</format>
			<nullable>${(item.nullable?string)!}</nullable>
			<showCustom>${(item.showCustom?string)!}</showCustom>
			<colspan>${(item.colspan)!}</colspan>
			<textareaRow>${(item.textareaRow)!}</textareaRow>
			<sort>${(item.sort)!}</sort>
			<property_code>${(item.property.code)!}</property_code>
			<propertyLayRec>${(item.propertyLayRec)!}</propertyLayRec>
			<associatedCode>${(item.associatedCode)!}</associatedCode>
			<customStyle><![CDATA[${(item.customStyle)!}]]></customStyle>
			<customScript><![CDATA[${(item.customScript)!}]]></customScript>
			<multable>${(item.multable?string)!}</multable>
			<readonly>${(item.readonly?string)!}</readonly>
			<align>${(item.align)!}</align>
			<precision>${(item.precision)!}</precision>
			<length>${(item.length)!}</length>
		</CustomPropertyViewMapping>
		</#list>
	</CustomPropertyViewMappings>
	</#if>
</CustomProperty>