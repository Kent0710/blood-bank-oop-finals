@echo off
REM ------------------------------------------------------------------
REM Blood Bank Management System - Compilation & Execution Script (Win)
REM ------------------------------------------------------------------

echo =================================================================
echo    Compiling Blood Bank Management System (Java OOP/GoF)...     
echo =================================================================

del /q *.class 2>nul
javac *.java

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation successful! Starting system...
    java Main
) else (
    echo [ERROR] Compilation failed. Please check errors above.
)
pause
