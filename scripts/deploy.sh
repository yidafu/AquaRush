#!/bin/bash
# Deployment script for AquaRush

set -e

APP_NAME="aquarush"
APP_JAR="aquarush.jar"
DEPLOY_DIR="/opt/aquarush"
SERVICE_NAME="aquarush.service"

echo "Building application..."
./gradlew :modules:entry-module:bootJar

echo "Stopping service..."
sudo systemctl stop ${SERVICE_NAME}

echo "Backing up current version..."
if [ -f "${DEPLOY_DIR}/${APP_JAR}" ]; then
    sudo cp ${DEPLOY_DIR}/${APP_JAR} ${DEPLOY_DIR}/${APP_JAR}.backup
fi

echo "Deploying new version..."
sudo cp modules/entry-module/build/libs/*.jar ${DEPLOY_DIR}/${APP_JAR}

echo "Starting service..."
sudo systemctl start ${SERVICE_NAME}

echo "Checking service status..."
sudo systemctl status ${SERVICE_NAME}

echo "Deployment completed!"
