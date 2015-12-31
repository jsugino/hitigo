@echo off

setlocal

set VER=0.0.2

if "%1"=="" goto NEXT
if "%1"=="latest" goto NEXT
set VER=%1
:NEXT

set CLS=%HOMEDRIVE%%HOMEPATH%\.m2\repository\mylib\hitigo\hitigo\%VER%\hitigo-%VER%-jar-with-dependencies.jar

echo %CLS%

"%JAVA_HOME%\bin\java.exe" -jar "%CLS%" %2 %3 %4 %5 %6 %7 %8
pause
