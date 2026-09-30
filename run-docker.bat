@ECHO OFF
SETLOCAL

SET APP_NAME=webshell
SET HARBOR_URL=harbor.tai.it/webshell
SET COMPOSE_CMD=docker compose -f docker\docker-compose.yml

SET JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75
set SPRING_PROFILES_ACTIVE=prod

set "APP_VERSION="

for /f "usebackq delims=" %%V in (`call mvn -q help:evaluate "-Dexpression=project.version" "-DforceStdout" 2^>nul`) do (
    set "APP_VERSION=%%V"
)

if defined APP_VERSION (
    echo Project version: %APP_VERSION%
) else (
    echo ERROR: cannot read version from pom.xml
)

REM Check parameter
IF "%~1"=="" (
    echo Usage: %~nx0 {build^|push^|build_start^|start^|stop^|restart^|purge^|tail^|update}
    GOTO END
)

IF /I "%~1"=="build" (
    CALL :build
    GOTO :END
)

IF /I "%~1"=="push" (
    CALL :push
    GOTO END
)

IF /I "%~1"=="build_start" (
    CALL :build
    CALL :start "%~2"
    CALL :tail
    GOTO END
)

IF /I "%~1"=="start" (
    CALL :start "%~2"
    CALL :tail
    GOTO END
)

IF /I "%~1"=="stop" (
    CALL :down
    GOTO END
)

IF /I "%~1"=="restart" (
    CALL :down
    CALL :start "%~2"
    CALL :tail
    GOTO END
)

IF /I "%~1"=="purge" (
    CALL :down
    CALL :purge
    GOTO END
)

IF /I "%~1"=="tail" (
    CALL :tail
    GOTO END
)

IF /I "%~1"=="update" (
    CALL :update
    GOTO END
)

echo Usage: %~nx0 {build^|build_start^|start^|stop^|restart^|purge^|tail^|update}

:END
ENDLOCAL
EXIT /B %ERRORLEVEL%

:build
docker build -t %APP_NAME%:%APP_VERSION% . -f docker/Dockerfile
docker tag %APP_NAME%:%APP_VERSION% %APP_NAME%:latest
EXIT /B %ERRORLEVEL%

:push
docker tag %APP_NAME%:%APP_VERSION% %APP_NAME%:latest
docker tag %APP_NAME% %HARBOR_URL%/%APP_NAME%:%APP_VERSION%
docker push %HARBOR_URL%/%APP_NAME%:%APP_VERSION%
EXIT /B %ERRORLEVEL%

:start
%COMPOSE_CMD% up -d
EXIT /B %ERRORLEVEL%

:down
%COMPOSE_CMD% down
EXIT /B %ERRORLEVEL%

:tail
%COMPOSE_CMD% logs -f
EXIT /B %ERRORLEVEL%

:update
docker build -t %APP_NAME%:%APP_VERSION% . -f docker/Dockerfile
%COMPOSE_CMD% stop %APP_NAME%
%COMPOSE_CMD% rm -f %APP_NAME%
%COMPOSE_CMD% up -d --no-build %APP_NAME%
%COMPOSE_CMD% logs -f
EXIT /B %ERRORLEVEL%

:off
%COMPOSE_CMD% stop %APP_NAME%
EXIT /B %ERRORLEVEL%

:purge
%COMPOSE_CMD% down --rmi local --remove-orphans
EXIT /B %ERRORLEVEL%