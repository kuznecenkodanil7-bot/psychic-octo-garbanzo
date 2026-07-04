@echo off
echo Building RaidMine MiniBaritone...
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle не найден. Самый простой способ: загрузи проект на GitHub и запусти Actions - Build MiniBaritone 1.21.11.
  echo Либо установи Gradle и Java 21, затем запусти build.bat снова.
  pause
  exit /b 1
)
gradle build --stacktrace
pause
