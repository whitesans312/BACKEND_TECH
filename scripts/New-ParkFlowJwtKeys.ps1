param([string]$EnvFile = ".env.parkflow")
$ErrorActionPreference = 'Stop'

$envPath = [System.IO.Path]::GetFullPath($EnvFile)
if (-not (Test-Path -LiteralPath $envPath)) {
    $templatePath = Join-Path $PSScriptRoot '..\.env.parkflow.example'
    Copy-Item -LiteralPath $templatePath -Destination $envPath
}

$openssl = Get-Command openssl -ErrorAction SilentlyContinue
if (-not $openssl) {
    $gitOpenSsl = 'C:\Program Files\Git\usr\bin\openssl.exe'
    if (Test-Path -LiteralPath $gitOpenSsl) { $opensslPath = $gitOpenSsl }
    else { throw 'No se encontró OpenSSL. Instálalo o agrega openssl.exe al PATH.' }
} else { $opensslPath = $openssl.Source }

$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("parkflow-rsa-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir | Out-Null
$privatePem = Join-Path $tempDir 'private.pem'
$privateDer = Join-Path $tempDir 'private.der'
$publicDer = Join-Path $tempDir 'public.der'
try {
    & $opensslPath genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out $privatePem
    if ($LASTEXITCODE -ne 0) { throw 'OpenSSL no pudo generar la llave privada.' }
    & $opensslPath pkcs8 -topk8 -nocrypt -in $privatePem -outform DER -out $privateDer
    if ($LASTEXITCODE -ne 0) { throw 'OpenSSL no pudo codificar la llave privada PKCS#8.' }
    & $opensslPath pkey -in $privatePem -pubout -outform DER -out $publicDer
    if ($LASTEXITCODE -ne 0) { throw 'OpenSSL no pudo generar la llave pública.' }
    $privateB64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($privateDer))
    $publicB64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($publicDer))
    $contents = [System.IO.File]::ReadAllText($envPath)
    $contents = [regex]::Replace($contents, '(?m)^IDENTITY_JWT_PRIVATE_KEY_B64=.*$', "IDENTITY_JWT_PRIVATE_KEY_B64=$privateB64")
    $contents = [regex]::Replace($contents, '(?m)^IDENTITY_JWT_PUBLIC_KEY_B64=.*$', "IDENTITY_JWT_PUBLIC_KEY_B64=$publicB64")
    [System.IO.File]::WriteAllText($envPath, $contents, [System.Text.UTF8Encoding]::new($false))
    Write-Host "Llaves creadas en $envPath. Mantén ese archivo privado y fuera de Git."
} finally {
    Remove-Item -LiteralPath $privatePem,$privateDer,$publicDer -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $tempDir -Force -ErrorAction SilentlyContinue
}
