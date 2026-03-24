@echo off
cls

echo ======================================
echo  Registro de Lectura y Visualizacion
echo ======================================
echo.

REM Verificar si Maven esta instalado
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Maven no esta instalado globalmente
    echo Instalando Maven localmente...
    echo.
    
    if not exist "tools" mkdir "tools"
    cd tools
    
    if not exist "apache-maven-3.9.6" (
        echo Descargando Apache Maven 3.9.6...
        powershell -Command "[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile 'maven.zip' -Verbose"
        
        if exist "maven.zip" (
            echo Extrayendo archivo...
            powershell -Command "Expand-Archive -Path 'maven.zip' -DestinationPath '.' -Force"
            del maven.zip
        )
    )
    
    set MVN_PATH=%CD%\apache-maven-3.9.6\bin
    cd ..
    set PATH=!MVN_PATH!;!PATH!
) else (
    echo Maven detectado
)

echo.
echo ======================================
echo  Compilando y ejecutando...
echo ======================================
echo.

call mvn clean javafx:run

echo.
pause
