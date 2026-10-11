<#
  KillJavaVMForWindows.ps1
  - looks for all java or javaw processes. Kills them "softly" unless -Force is given as parameter. 
  - Optional: -Pattern "mainclassname" looks for th/is pattern in the command line of the java/javaw process
    and only kills the matching processes. 
  - Optional: -Force implies a forced kill with /F /T if the soft kill does not work.
#>

param(
  [string]$Pattern = "",
  [switch]$Force = $false
)

function Log {
  param([string]$msg, [string]$level = "INFO")
  $time = (Get-Date).ToString("s")
  Write-Output "[$time] [$level] $msg"
}

# 1) Find all java/javaw Prozesses
$allJava = Get-CimInstance Win32_Process |
  Where-Object { $_.Name -match '^(java|javaw)\.exe$' -or ($_.CommandLine -and $_.CommandLine -match '\bjava\b') } |
  Select-Object ProcessId, Name, @{Name='CommandLine';Expression={if ($_.CommandLine) { $_.CommandLine } else { "<keine Kommandozeile>" }}}

if (-not $allJava) {
  Log "Did not find any java/javaw processes." "WARN"
  exit 0
}

# 2) Always list the java processes in a table and in detail 
Log "List of all running java/javaw processes:"
$allJava | Format-Table -AutoSize

Log "Each java/javaw process with details, one row per process"
foreach ($p in $allJava) {
  Write-Output ("PID: {0}  Name: {1}`nCmd: {2}`n" -f $p.ProcessId, $p.Name, $p.CommandLine)
}

# 3) if a filter pattern is provided, check which of the java processes match
if ($Pattern) {
  $matches = $allJava | Where-Object { $_.CommandLine -and $_.CommandLine -like "*$Pattern*" }
  if (-not $matches) {
    Log "None of the java processes match pattern '$Pattern'." "ERROR"
    exit 2
  }
} else {
  $matches = $allJava
}

# 4) only kill java processes if there is only one running, or multiple processes are running and
# a pattern has been provided.
if (-not $Pattern -and $allJava.Count -gt 1) {
  Log "Mehrere Java-Prozesse gefunden und kein Pattern angegeben. Automatisches Beenden wird abgebrochen, um falsches Beenden zu vermeiden." "ERROR"
  exit 3
}

# 5) Non-interaktive soft kill of the selected PIDs
$pidsToKill = $matches | Select-Object -ExpandProperty ProcessId

foreach ($pit in $pidsToKill) {
  Log "Sending soft Kill command to PID $pit"
  $out = & taskkill /PID $pit 2>&1
  Log $out
  $exit = $LASTEXITCODE
  if ($exit -ne 0) {
    Log "taskkill Exit Code $exit for PID $pit." "WARN"
    if ($Force) {
      Log "Force option applied: trying forced kill for PID $pit mit /F /T."
      $out2 = & taskkill /PID $pit /F /T 2>&1
      Log $out2
      $exit2 = $LASTEXITCODE
      if ($exit2 -ne 0) {
        Log "Forced kill of PID $pit failed (ExitCode $exit2)." "ERROR"
      } else {
        Log "Forced kill of PID $pit successful." "INFO"
      }
    } else {
      Log "Process PID $pit did not react to soft kill. Use parameter -Force, to apply forced kill. /F /T zu verwenden." "WARN"
    }
  } else {
    Log "PID $pit successfully terminated." "INFO"
  }
}

exit 0
