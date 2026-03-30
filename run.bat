@echo off
echo Starting File Transfer Server...
docker-compose up -d
echo Server started at http://localhost:9090
echo View logs: docker-compose logs -f