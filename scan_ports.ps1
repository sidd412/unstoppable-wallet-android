$server = "103.214.169.20"
$ports = 17000..17200
foreach ($p in $ports) {
    $t = Test-NetConnection -ComputerName $server -Port $p -WarningAction SilentlyContinue
    if ($t.TcpTestSucceeded) {
        Write-Host "OPEN: $p"
    }
}
write-host "Scan complete"
