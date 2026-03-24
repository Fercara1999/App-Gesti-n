#!/bin/bash

# Script para descargar Maven y ejecutar la aplicación

echo "=================================================="
echo "  Instalando Maven..."
echo "=================================================="

# Crear directorio para Maven
mkdir -p ./tools

# Descargar Maven si no existe
if [ ! -f "./tools/apache-maven-3.9.6/bin/mvn" ]; then
    echo "Descargando Apache Maven 3.9.6..."
    cd ./tools
    wget -q https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz || curl -L https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -o apache-maven-3.9.6-bin.tar.gz
    tar -xzf apache-maven-3.9.6-bin.tar.gz
    rm apache-maven-3.9.6-bin.tar.gz
    cd ..
fi

export PATH="./tools/apache-maven-3.9.6/bin:$PATH"

echo ""
echo "=================================================="
echo "  Compilando y ejecutando..."
echo "=================================================="
echo ""

mvn clean javafx:run
