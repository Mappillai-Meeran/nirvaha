@echo off
TITLE Nirvaha Platform Startup Launcher
echo =========================================================================
echo               NIRVAHA -- FROM REPORT TO RESOLUTION
echo     Trust-Aware Civic Intelligence Platform for Verified Issue Resolution
echo =========================================================================
echo.
echo Launching Nirvaha Spring Boot Backend on http://localhost:8080 ...
start "Nirvaha Backend (Spring Boot)" cmd /k "cd /d %~dp0nirvaha-backend && mvnw.cmd spring-boot:run"

echo.
echo Launching Nirvaha React Frontend on http://localhost:5173 ...
start "Nirvaha Frontend (Vite)" cmd /k "cd /d %~dp0nirvaha-frontend && npm run dev"

echo.
echo =========================================================================
echo Services launching in separate windows!
echo - Frontend Citizen Portal & Command Center: http://localhost:5173
echo - Backend REST API:                        http://localhost:8080
echo =========================================================================
echo.
pause
