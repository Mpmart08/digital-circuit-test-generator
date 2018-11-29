@echo off
set tab=	
set /p circuit= Circuit:%tab%
set /p net= Net:%tab%%tab%
set /p value= Stuck-at-Value:%tab%
for /f %%i in ('java TestGenerator input_files/%circuit%.txt %net% %value%') do (
    set vector=%%i
)
if "%vector%"=="Fault" (
    echo. & echo Fault Undetectable
) else (
    echo. & echo Test Vector:%tab%%vector% & echo.
    java FaultSimulator input_files/%circuit%.txt %vector%
)