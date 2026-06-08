title supfusion.gateway
@echo off
rem Copyright 1993-2020 ZHEJIANG SUPCON
set BASE_DIR=%~dp0
set JAVA_HOME=%BASE_DIR%\..\..\assembly\jdk1.8
if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\supfusion-gateway.exe"

set "JAVA_OPT=%JAVA_OPT% -Xms512m -Xmx512m -Xmn256m -Dwork.dir=%BASE_DIR%\..\..\..\logs\gateway -Dintegration.supos.enabled=false -Dspring.cloud.nacos.config.enabled=true -Dspring.cloud.nacos.config.server-addr=127.0.0.1:8848 -Dspring.cloud.nacos.config.group=prod -Dspring.cloud.nacos.discovery.group=prod -Dspring.cloud.nacos.discovery.server-addr=127.0.0.1:8848 -Dspring.redis.host=127.0.0.1"

set "JAVA_OPT=%JAVA_OPT% -jar %BASE_DIR%\supos-gateway.jar"

"%JAVA%" %JAVA_OPT% supfusion.gateway %*
