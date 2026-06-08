<?xml version="1.0" encoding="UTF-8"?>
<workflows>
	<#if deployments?has_content>
		<#list deployments as deployment>
		<workflow>
			<name>${(deployment.name)!}</name>
			<processKey>${(deployment.processKey)!}</processKey>
			<description><![CDATA[${(deployment.description)!}]]></description>
			<processName>${(deployment.processName)!}</processName>
			<isSuspended>${(deployment.isSuspended?string)!}</isSuspended>
			<entityCode>${(deployment.entityCode)!}</entityCode>
			<processXml><![CDATA[${(deployment.processXml)!}]]></processXml>
			<entryUrl>${(deployment.entryUrl)!}</entryUrl>
			<keyDescs>${(deployment.keyDescs)!}</keyDescs>
			<requiredTime>${(deployment.requiredTime)!}</requiredTime>
			<mobilequery>${(deployment.mobilequery!false)?string}</mobilequery>
			<mobileinitiate>${(deployment.mobileinitiate!false)?string}</mobileinitiate>
			<mobileapprove>${(deployment.mobileapprove!false)?string}</mobileapprove>
			<allowInvalid>${(deployment.allowInvalid!false)?string}</allowInvalid>
			<graduallyReject>${(deployment.graduallyReject!false)?string}</graduallyReject>
			<recallAble>${(deployment.recallAble!false)?string}</recallAble>
			<recallRemainTime>${(deployment.recallRemainTime)!}</recallRemainTime>
			<mainViewViewCode>${(deployment.mainViewViewCode)!}</mainViewViewCode>
		</workflow>
		</#list>
	</#if>
</workflows>