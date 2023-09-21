@echo off
cd /d %~dp0../compiler/src
"C:\Program Files\7-Zip\7z.exe" a -tzip -mx5 -aoa -uq0 ../../comp.zip .
pause