$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\finki-code\.jdk\extract\jdk-17.0.18+8"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
.\mvnw.cmd spring-boot:run
