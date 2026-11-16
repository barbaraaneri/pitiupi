# Chamado por scripts/pitiupi.sh e por scripts/pitiupi.cmd.
# Não é um comando de quem baixa o projeto.
# Não copia plugins: quem chama já deixou o JAR e o PITIUPI_HOME prontos.
param(
    [Parameter(Mandatory = $true)][string]$PitiupiHome,
    [Parameter(Mandatory = $true)][string]$Jar
)

$ErrorActionPreference = 'Stop'

function Get-JavaMajor([string]$exe) {
    # "1.8.0_421" -> 8 ; "17.0.21" -> 17
    $out = cmd /c "`"$exe`" -version 2>&1" | Out-String
    if ($out -match 'version "(\d+)(?:\.(\d+))?') {
        $major = [int]$Matches[1]
        if ($major -eq 1 -and $Matches[2]) { $major = [int]$Matches[2] }
        return $major
    }
    return 0
}

function Find-JavaExe {
    # O PATH pode ter um Java antigo (ex.: Java 8 da Oracle) antes do JDK 17.
    # Junta os candidatos e usa o primeiro que seja Java 17 ou mais novo.
    $candidates = @()
    Get-Command java -All -ErrorAction SilentlyContinue |
        Where-Object { $_.Source -notmatch '\\WindowsApps\\' } |
        ForEach-Object { $candidates += $_.Source }
    $patterns = @(
        (Join-Path $env:ProgramFiles 'Microsoft\jdk-*\bin\java.exe'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium\jdk-*\bin\java.exe'),
        (Join-Path $env:ProgramFiles 'Java\jdk-*\bin\java.exe'),
        (Join-Path ${env:ProgramFiles(x86)} 'Microsoft\jdk-*\bin\java.exe'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Microsoft\jdk-*\bin\java.exe')
    )
    foreach ($pat in $patterns) {
        Get-Item -Path $pat -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending |
            ForEach-Object { $candidates += $_.FullName }
    }
    foreach ($exe in $candidates) {
        if ((Get-JavaMajor $exe) -ge 17) { return $exe }
    }
    return $null
}

if (-not (Test-Path -LiteralPath $Jar)) {
    Write-Error "JAR ausente: $Jar"
}
if (-not (Test-Path -LiteralPath $PitiupiHome)) {
    Write-Error "PITIUPI_HOME ausente: $PitiupiHome"
}

$javaExe = Find-JavaExe
if (-not $javaExe) {
    Write-Error "Java 17 (ou mais novo) do Windows nao encontrado. No PowerShell: winget install Microsoft.OpenJDK.17 --accept-package-agreements"
}

Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -like '*pitiupi.jar*' } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }

$env:PITIUPI_HOME = $PitiupiHome
Start-Process -FilePath $javaExe -ArgumentList @('-jar', $Jar)
