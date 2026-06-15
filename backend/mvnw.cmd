@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script for Windows
@REM ----------------------------------------------------------------------------
@echo off

SET MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties
SET MAVEN_USER_HOME=%USERPROFILE%\.m2
SET MAVEN_WRAPPER_JAR=%MAVEN_USER_HOME%\wrapper\dists\maven-wrapper.jar

@REM Try to locate JAVA_HOME
IF NOT "%JAVA_HOME%"=="" GOTO foundJava
FOR /F "tokens=*" %%i IN ('where javac 2^>NUL') DO SET JAVAC=%%i
IF NOT "%JAVAC%"=="" (
    FOR %%i IN ("%JAVAC%") DO SET JAVA_BIN_DIR=%%~dpi
    SET JAVA_HOME=%JAVA_BIN_DIR%..
)
:foundJava

IF NOT "%JAVA_HOME%"=="" (
    SET JAVA_CMD=%JAVA_HOME%\bin\java.exe
) ELSE (
    SET JAVA_CMD=java.exe
)

@REM Download wrapper jar if missing
IF EXIST "%MAVEN_WRAPPER_JAR%" GOTO runWrapper
IF NOT EXIST "%MAVEN_USER_HOME%\wrapper\dists" MKDIR "%MAVEN_USER_HOME%\wrapper\dists"
FOR /F "tokens=2 delims==" %%i IN ('findstr "wrapperUrl" "%MAVEN_WRAPPER_PROPERTIES%"') DO SET WRAPPER_URL=%%i
powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%MAVEN_WRAPPER_JAR%'" 2>NUL

:runWrapper
IF EXIST "%MAVEN_WRAPPER_JAR%" (
    "%JAVA_CMD%" %MAVEN_OPTS% -classpath "%MAVEN_WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
) ELSE (
    WHERE mvn >NUL 2>&1
    IF %ERRORLEVEL% EQU 0 (
        mvn %*
    ) ELSE (
        echo ERROR: Maven not found. Install it from https://maven.apache.org/install.html
        exit /b 1
    )
)
