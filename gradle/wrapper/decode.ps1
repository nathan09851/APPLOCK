$b64 = Get-Content (Join-Path $PSScriptRoot "wrapper-base64.txt") -Raw
$bytes = [Convert]::FromBase64String($b64.Trim())
$outPath = Join-Path $PSScriptRoot "gradle-wrapper.jar"
[System.IO.File]::WriteAllBytes($outPath, $bytes)
Write-Host "Written $($bytes.Length) bytes to $outPath"
