@echo off

set bap_home=%~dp0..\

call stopServices.bat

call startServices.bat
