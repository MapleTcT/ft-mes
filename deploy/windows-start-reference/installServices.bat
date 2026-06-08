@echo off

set bap_home=%~dp0..\

setx SUPOS_SUPOS_APP_TENANT_ID "dt" /m

call %bap_home%\Commands\windows\installSupfusionNacos.bat
net start supfusion-nacos

call %bap_home%\Commands\windows\installSupfusionNginx.bat

call %bap_home%\Commands\windows\installSupfusionZK.bat



call %bap_home%\Commands\windows\installSupfusionKeycloak.bat

call %bap_home%\Commands\windows\installSupfusionRedis.bat

call %bap_home%\Commands\windows\installSupfusionKafka.bat

call %bap_home%\Commands\windows\installSupfusionIam.bat

call %bap_home%\Commands\windows\installSupfusionGateway.bat

call %bap_home%\Commands\windows\installSupfusionI18n.bat

call %bap_home%\Commands\windows\installSupfusionSysmanagement.bat

call %bap_home%\Commands\windows\installSupfusionBasicmanagement.bat

call %bap_home%\Commands\windows\installSupfusionOrgmanagement.bat

call %bap_home%\Commands\windows\installSupfusionConfiguration.bat

call %bap_home%\Commands\windows\installSupfusionOperatetools.bat

call %bap_home%\Commands\windows\installSupfusionBaseService.bat

call %bap_home%\Commands\windows\installSupfusionFlow.bat

call %bap_home%\Commands\windows\installSupfusionNotificationAdmin.bat

call %bap_home%\Commands\windows\installSupfusionNotificationApiServer.bat

call %bap_home%\Commands\windows\installSupfusionNotificationEngine.bat

call %bap_home%\Commands\windows\installSupfusionFileServer.bat

call %bap_home%\Commands\windows\installSupfusionWebsocket.bat

call %bap_home%\Commands\windows\installSupfusionTaskScheduler.bat

call %bap_home%\Commands\windows\installSupfusionLicense.bat

call %bap_home%\Commands\windows\installSupfusionCustomProperty.bat

call %bap_home%\Commands\windows\installSupfusionNotificationApp.bat

call %bap_home%\Commands\windows\installSupfusionNotificationMobile.bat

call %bap_home%\Commands\windows\installSupfusionNotificationSmsJincang.bat