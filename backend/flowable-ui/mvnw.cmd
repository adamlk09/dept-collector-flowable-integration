@echo off
setlocal

set MAVEN_CACHE=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12
set MVN_BIN=

for /d %%i in ("%MAVEN_CACHE%\*") do (
    if exist "%%i\bin\mvn.cmd" (
        set MVN_BIN=%%i\bin\mvn.cmd
        goto :found
    )
)

echo [mvnw] Maven 3.9.12 not found in %MAVEN_CACHE%
echo [mvnw] Run once: mvn wrapper:wrapper  OR install Maven and add to PATH
exit /b 1

:found
"%MVN_BIN%" %*
endlocal
