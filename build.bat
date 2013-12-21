@echo Building...
@echo off
del bin\*.* /Q
dir /s /B src\*.groovy > tmpsources.txt
call groovyc -d bin @tmpsources.txt
del tmpsources.txt
@echo Done!