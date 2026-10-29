$policyPath = "C:\GH\CS\policy.json"
$policy = Get-Content $policyPath -Raw | ConvertFrom-Json

$binding = New-Object -TypeName PSObject -Property @{
    members = @("serviceAccount:play-verifier@credstor-backend.iam.gserviceaccount.com")
    role = "roles/androidpublisher.user"
}

$policy.bindings += $binding
$policy | ConvertTo-Json -Depth 10 | Out-File -FilePath "C:\GH\CS\policy2.json" -Encoding utf8
Write-Host "Policy updated successfully"
Get-Content "C:\GH\CS\policy2.json"
