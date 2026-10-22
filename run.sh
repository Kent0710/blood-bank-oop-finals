#!/usr/bin/env bash
# ------------------------------------------------------------------
# Blood Bank Management System - Compilation & Execution Script (Unix)
# ------------------------------------------------------------------

echo "================================================================="
echo "   Compiling Blood Bank Management System (Java OOP/GoF)...     "
echo "================================================================="

# Clean old class files and recompile
rm -f *.class
javac *.java

if [ $? -eq 0 ]; then
    echo "[SUCCESS] Compilation successful! Starting system in 1 second..."
    sleep 1
    java Main
else
    echo "[ERROR] Compilation failed. Please inspect syntax errors above."
    exit 1
fi
