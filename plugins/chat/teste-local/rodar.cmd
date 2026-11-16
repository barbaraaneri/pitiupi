@echo off
rem Abre o teste local do Jogo da Memoria (dois jogadores no mesmo computador, sem rede).
rem Antes, rode uma vez: scripts\pitiupi.cmd run --all   (gera pitiupi\dist\pitiupi.jar)
setlocal
cd /d "%~dp0..\..\.."
set "CORE=pitiupi\dist\pitiupi.jar"
if not exist "%CORE%" (
  echo ERRO: %CORE% nao existe. Rode antes: scripts\pitiupi.cmd run --all
  exit /b 1
)
rem Usa o java da mesma pasta do javac (JDK 17), e nao um Java 8 que esteja antes no PATH.
set "JBIN="
for /f "delims=" %%J in ('where javac 2^>nul') do if not defined JBIN set "JBIN=%%~dpJ"
if not defined JBIN (
  echo ERRO: javac nao encontrado. Instale o JDK 17: winget install Microsoft.OpenJDK.17
  exit /b 1
)
set "OUT=%TEMP%\pitiupi-teste-local"
if exist "%OUT%" rmdir /s /q "%OUT%"
"%JBIN%javac" -encoding UTF-8 -cp "%CORE%" -sourcepath plugins\chat\src -d "%OUT%" plugins\chat\teste-local\TesteJogoLocal.java
if errorlevel 1 exit /b 1
start "" "%JBIN%java" -cp "%OUT%;%CORE%" TesteJogoLocal
