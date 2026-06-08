title supfusion.i18n
@echo off
rem Copyright 1993-2020 ZHEJIANG SUPCON
set BASE_DIR=%~dp0
set JAVA_HOME=%BASE_DIR%\..\..\assembly\jdk1.8
if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\supfusion-i18n.exe"

set "JAVA_OPT=%JAVA_OPT% -Xms512m -Xmx512m -Xmn256m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9979 -Dwork.dir=%BASE_DIR%\..\..\..\logs\i18n -Dspring.profiles.active=prod -Dfile.encoding=utf-8 -Dspring.cloud.nacos.config.enabled=true"

set "JAVA_OPT=%JAVA_OPT% -jar %DEBUG_OPTS% %BASE_DIR%\supfusion-i18n.jar"

"%JAVA%" %JAVA_OPT% supfusion.i18n %*