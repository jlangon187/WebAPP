param(
    [string]$ApiBaseUrl = "http://localhost:8080/api/internal/encryption-jobs",
    [string]$WorkerKey = "",
    [string]$LockExePath = "\\nas\mods-files\lock.exe",
    [string]$RarExePath = "C:\Program Files\WinRAR\rar.exe",
    [string]$ModsRoot = "\\nas\mods-files",
    [string]$OutputRoot = "\\nas\mods-files\compras",
    [string]$TempRoot = "C:\gpbmods\jobs",
    [int]$PollIntervalSeconds = 5
)

if ([string]::IsNullOrWhiteSpace($WorkerKey)) {
    Write-Error "WorkerKey is required."
    exit 1
}

New-Item -ItemType Directory -Force -Path $TempRoot | Out-Null

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    $headers = @{ "X-Worker-Key" = $WorkerKey }
    $uri = "$ApiBaseUrl$Path"

    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 6
        return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json" -Body $json
    }

    return Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
}

function Invoke-LockForPkz {
    param(
        [string]$FilePath,
        [string]$Guid
    )

    $proc = Start-Process -FilePath $LockExePath -ArgumentList @($FilePath, $FilePath, "/guid", $Guid) -NoNewWindow -PassThru -Wait
    if ($proc.ExitCode -ne 0) {
        throw "lock.exe failed for $FilePath with exit code $($proc.ExitCode)"
    }
}

function Process-Job {
    param([object]$Job)

    $jobId = [string]$Job.id
    $guid = [string]$Job.guid
    $folder = [string]$Job.modBaseFolder

    $sourcePath = Join-Path $ModsRoot $folder
    $jobTempRoot = Join-Path $TempRoot $jobId
    $jobTempFolder = Join-Path $jobTempRoot $folder

    $outputUserFolder = Join-Path $OutputRoot $guid
    $rarName = "MOD_${folder}_${guid}.rar"
    $rarTempPath = Join-Path $jobTempRoot $rarName
    $finalRarPath = Join-Path $outputUserFolder $rarName

    try {
        Invoke-Api -Method POST -Path "/$jobId/start" | Out-Null

        if (!(Test-Path $sourcePath)) {
            throw "Source folder does not exist: $sourcePath"
        }

        New-Item -ItemType Directory -Force -Path $jobTempRoot | Out-Null
        Copy-Item -Path $sourcePath -Destination $jobTempRoot -Recurse -Force

        $pkzFiles = Get-ChildItem -Path $jobTempFolder -Filter *.pkz -File -Recurse
        if ($pkzFiles.Count -eq 0) {
            throw "No .pkz files found in copied folder: $jobTempFolder"
        }

        foreach ($pkz in $pkzFiles) {
            Write-Host "[INFO] Locking $($pkz.FullName)"
            Invoke-LockForPkz -FilePath $pkz.FullName -Guid $guid
        }

        New-Item -ItemType Directory -Force -Path $outputUserFolder | Out-Null

        $workingDir = $jobTempRoot
        $proc = Start-Process -FilePath $RarExePath -ArgumentList @("a", "-ep1", $rarTempPath, $jobTempFolder) -WorkingDirectory $workingDir -NoNewWindow -PassThru -Wait
        if ($proc.ExitCode -ne 0) {
            throw "rar.exe failed with exit code $($proc.ExitCode)"
        }

        Move-Item -Path $rarTempPath -Destination $finalRarPath -Force

        $relativePath = "compras/$guid/$rarName"
        Invoke-Api -Method POST -Path "/$jobId/complete" -Body @{ outputRelativePath = $relativePath } | Out-Null
        Write-Host "[OK] Job $jobId completed."
    }
    catch {
        $errorText = $_.Exception.Message
        Write-Host "[ERROR] Job $jobId failed: $errorText"
        Invoke-Api -Method POST -Path "/$jobId/fail" -Body @{ errorMessage = $errorText } | Out-Null
    }
    finally {
        if (Test-Path $jobTempRoot) {
            Remove-Item -Path $jobTempRoot -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "Encryption worker started..."
while ($true) {
    try {
        $job = Invoke-Api -Method POST -Path "/next"
        if ($null -ne $job -and $job.id) {
            Process-Job -Job $job
        }
        else {
            Start-Sleep -Seconds $PollIntervalSeconds
        }
    }
    catch {
        Start-Sleep -Seconds $PollIntervalSeconds
    }
}
