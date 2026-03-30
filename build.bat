@echo off
echo Building File Transfer Server Docker image...
docker build -t file-transfer-server:latest .
echo Build complete!