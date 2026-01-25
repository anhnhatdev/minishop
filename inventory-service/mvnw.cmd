@echo off
setlocal
set "MAVEN_HOME=C:\Users\anh\.m2\wrapper\dists\apache-maven-3.9.6-bin\apache-maven-3.9.6"
java "-Dmaven.home=%MAVEN_HOME%" "-Dclassworlds.conf=%MAVEN_HOME%\bin\m2.conf" "-Dmaven.multiModuleProjectDirectory=%CD%" -classpath "%MAVEN_HOME%\boot\plexus-classworlds-2.7.0.jar" org.codehaus.plexus.classworlds.launcher.Launcher %*
