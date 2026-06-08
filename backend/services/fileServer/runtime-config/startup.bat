title supfusion-fileServer
@echo off
rem Copyright 1993-2020 ZHEJIANG SUPCON
set BASE_DIR=%~dp0
set JAVA_HOME=%BASE_DIR%\..\..\assembly\jdk1.8
if not exist "%JAVA_HOME%\bin\java.exe" echo Please set the JAVA_HOME variable in your environment, We need java(x64)! jdk8 or later is better! & EXIT /B 1
set "JAVA=%JAVA_HOME%\bin\supfusion-fileServer.exe"

set "JAVA_OPT=%JAVA_OPT% -Xms2048m -Xmx2048m -Xmn1024m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9599 -Dfile.encoding=utf-8 -Dwork.dir=%BASE_DIR%\..\..\..\logs\fileServer -Dspring.cloud.nacos.config.enabled=true"

set "JAVA_OPT=%JAVA_OPT% -jar %BASE_DIR%\supfusion-file-server.jar"

"%JAVA%" %JAVA_OPT% supfusion-fileServer %*