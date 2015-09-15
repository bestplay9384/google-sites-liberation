@echo off
set mypath=%~dp0
java -cp "%mypath%google-sites-liberation-1.0.7-SNAPSHOT-jar-with-dependencies.jar;%mypath%config" com.google.sites.liberation.util.Main %*
