<?xml version="1.0" encoding="UTF-8"?>
<templateList>
	<#if importTemplateList?? && importTemplateList?size gt 0>
	<#list importTemplateList as importTemplate>
	<template>
		<code>${(importTemplate.code)!}</code>
		<value>
			${((importTemplate.value)!)?replace('<?xml version="1.0" encoding="UTF-8"?>', '')}
		</value>
		<version>${(importTemplate.version)!}</version>
		<ecEnv>${(importTemplate.ecEnv)!}</ecEnv>
		<projFlag>${((importTemplate.projFlag)!false)?string('true', 'false')}</projFlag>
	</template>
	</#list>
	</#if>
</templateList>	