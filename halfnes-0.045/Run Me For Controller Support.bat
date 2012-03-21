@ECHO OFF
echo Starting HalfNES...
java -Djava.library.path=./lib -jar HalfNES.jar %1
IF NOT EXIST .\lib\jinput.jar GOTO NoLibs
GOTO EOF
:NoLibs
echo HalfNES cannot start because the library folder is missing.
pause
:EOF
