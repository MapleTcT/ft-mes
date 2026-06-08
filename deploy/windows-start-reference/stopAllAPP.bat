@echo off
for /f "tokens=2" %%a in ('tasklist ^|find /i "supfusionApp-"') do taskkill /F /PID %%a
