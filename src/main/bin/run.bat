@echo off

if not "%OS%"=="Windows_NT" goto wrongOS

@setlocal

set command=%0
set progname=%~n0

set SIMPLEX_HOME=%~dp0..
set SIMPLEX_BIN=%~dp0..\bin
set SIMPLEX_LIB=%~dp0..\lib
set SIMPLEX_LOG=%~dp0..\log

if "%JAVA_HOME%"=="" goto noJavaHome
if not exist "%JAVA_HOME%"\bin\java.exe goto noJava

set JAVACMD="%JAVA_HOME%\bin\java.exe"

set LOCALCLASSPATH=%SIMPLEX_CLASSPATH%;%SIMPLEX_LIB%
FOR %%c in (%SIMPLEX_LIB%\*.jar) DO (call :append_cp %%c)
call :append_cp %SIMPLEX_LOG%

%JAVACMD% %SIMPLEX_JAVAOPTS% -Dsimplex.home="%SIMPLEX_HOME%" -Djava.util.logging.config.file="%SIMPLEX_HOME%/log/logging.properties" -cp "%LOCALCLASSPATH%" com.intalio.simplex.StandaloneServer "%SIMPLEX_HOME%"  %* 
goto end

:append_cp
set LOCALCLASSPATH=%LOCALCLASSPATH%;%1
goto end

=====================================================================
                              ERRORS
=====================================================================


:wrongOS
echo ERROR: SIMPLEX requires WindowsNT/XP. Aborting.
goto end

:noJavaHome
echo ERROR: JAVA_HOME not set! Aborting.
goto end

:noJava
echo ERROR: The Java VM (java.exe) was not found in %JAVA_HOME%\bin! Aborting
goto end

REM ================================================================
REM                             END
REM ================================================================
:end
@endlocal
