title supfusion.iam
@echo off
rem Copyright 1993-2020 ZHEJIANG SUPCON
set BASE_DIR=%~dp0
set JAVA_HOME=%BASE_DIR%\..\..\assembly\jdk1.8
if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\supfusion-iam.exe"

set "JAVA_OPT=%JAVA_OPT% -Xms512m -Xmx512m -Xmn256m -Dfile.encoding=utf-8 -Dwork.dir=%BASE_DIR%\..\..\..\logs\iam -Dspring.cloud.nacos.config.enabled=true"

set "JAVA_OPT=%JAVA_OPT% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=30161 -Dsupfusion.environment=configure -jar %BASE_DIR%\supfusion-iam.jar "

"%JAVA%" %JAVA_OPT% supfusion.iam %*
